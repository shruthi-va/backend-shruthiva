package com.chubb.claims.claim.application;

import static com.chubb.claims.claim.api.ClaimDtos.*;

import com.chubb.claims.claim.domain.AssessmentRecommendation;
import com.chubb.claims.claim.domain.ClaimRecord;
import com.chubb.claims.claim.domain.ClaimRepository;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.ClaimStatusHistory;
import com.chubb.claims.claim.domain.FinalDecision;
import com.chubb.claims.claim.domain.NotificationChannel;
import com.chubb.claims.notification.StatusOutboxRecord;
import com.chubb.claims.notification.StatusOutboxRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

    private static final Set<ClaimStatus> STAFF_QUEUE_STATUSES = Set.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.IN_REVIEW,
            ClaimStatus.AWAITING_INFORMATION,
            ClaimStatus.SETTLEMENT_IN_PROGRESS);
    private static final Set<ClaimStatus> TERMINAL_STATUSES = Set.of(
            ClaimStatus.SETTLED,
            ClaimStatus.REJECTED);
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern E164_PATTERN =
            Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private final ClaimRepository claims;
    private final StatusOutboxRepository outbox;

    public ClaimService(ClaimRepository claims, StatusOutboxRepository outbox) {
        this.claims = claims;
        this.outbox = outbox;
    }

    @Transactional
    public ClaimantClaimResponse submit(SubmitClaimRequest request, String claimantUsername) {
        validateNotificationAddress(request.notificationChannel(), request.notificationAddress());
        ClaimRecord claim = new ClaimRecord(
                claimantUsername,
                request.incidentDescription().trim(),
                request.incidentDate(),
                request.claimedAmount(),
                request.currency(),
                request.notificationChannel(),
                request.notificationAddress().trim());
        return toClaimantResponse(claims.save(claim));
    }

    @Transactional(readOnly = true)
    public List<ClaimantClaimResponse> listClaimantClaims(String claimantUsername) {
        return claims.findAllByClaimantUsernameOrderByCreatedAtDesc(claimantUsername)
                .stream()
                .map(ClaimService::toClaimantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimantClaimResponse getClaimantClaim(UUID claimId, String claimantUsername) {
        ClaimRecord claim = claims.findByIdAndClaimantUsername(claimId, claimantUsername)
                .orElseThrow(ClaimExceptions.NotFound::new);
        return toClaimantResponse(claim);
    }

    @Transactional(readOnly = true)
    public InternalClaimResponse getInternalClaim(UUID claimId) {
        return toInternalResponse(findClaim(claimId));
    }

    @Transactional(readOnly = true)
    public InternalClaimResponse getStaffClaim(UUID claimId, String staffUsername) {
        ClaimRecord claim = findClaim(claimId);
        if (claim.getAssignedStaffUsername() != null
                && !staffUsername.equals(claim.getAssignedStaffUsername())) {
            throw new AccessDeniedException("This claim is assigned to another staff member.");
        }
        return toInternalResponse(claim);
    }

    @Transactional(readOnly = true)
    public List<InternalClaimResponse> getStaffWorkQueue(String staffUsername) {
        return claims.findWorkQueue(staffUsername, STAFF_QUEUE_STATUSES)
                .stream()
                .map(ClaimService::toInternalResponse)
                .toList();
    }

    @Transactional
    public InternalClaimResponse pickUp(UUID claimId, String staffUsername) {
        ClaimRecord claim = findClaim(claimId);
        if (claim.getStatus() == ClaimStatus.IN_REVIEW
                && staffUsername.equals(claim.getAssignedStaffUsername())) {
            return toInternalResponse(claim);
        }
        requireStatus(claim, ClaimStatus.SUBMITTED, "Only submitted claims can be picked up.");
        claim.assignTo(staffUsername);
        transition(claim, ClaimStatus.IN_REVIEW, staffUsername, "Claim picked up for review");
        return toInternalResponse(claim);
    }

    @Transactional
    public InternalClaimResponse requestInformation(
            UUID claimId, String staffUsername, String message) {
        ClaimRecord claim = findClaim(claimId);
        requireAssignee(claim, staffUsername);
        requireStatus(claim, ClaimStatus.IN_REVIEW, "Information can only be requested while a claim is in review.");
        claim.requestInformation(message.trim());
        transition(claim, ClaimStatus.AWAITING_INFORMATION, staffUsername, "Information requested");
        return toInternalResponse(claim);
    }

    @Transactional
    public ClaimantClaimResponse provideInformation(
            UUID claimId, String claimantUsername, String information) {
        ClaimRecord claim = findClaimForClaimant(claimId, claimantUsername);
        requireStatus(claim, ClaimStatus.AWAITING_INFORMATION, "This claim is not awaiting information.");
        claim.provideInformation(information.trim());
        transition(claim, ClaimStatus.IN_REVIEW, claimantUsername, "Claimant provided additional information");
        return toClaimantResponse(claim);
    }

    @Transactional
    public InternalClaimResponse assess(
            UUID claimId,
            String staffUsername,
            AssessmentRecommendation recommendation,
            String notes) {
        ClaimRecord claim = findClaim(claimId);
        requireAssignee(claim, staffUsername);
        requireStatus(claim, ClaimStatus.IN_REVIEW, "Only claims in review can be assessed.");
        claim.recordAssessment(recommendation, notes.trim());
        transition(claim, ClaimStatus.PENDING_MANAGER_DECISION, staffUsername, "Assessment submitted for manager decision");
        return toInternalResponse(claim);
    }

    @Transactional
    public InternalClaimResponse decide(
            UUID claimId,
            String managerUsername,
            FinalDecision decision,
            String reason,
            BigDecimal settlementAmount) {
        ClaimRecord claim = findClaim(claimId);
        requireStatus(claim, ClaimStatus.PENDING_MANAGER_DECISION, "This claim is not awaiting a manager decision.");
        validateDecision(claim, decision, settlementAmount);
        claim.recordDecision(reason.trim(), settlementAmount);
        ClaimStatus nextStatus = decision == FinalDecision.REJECT
                ? ClaimStatus.REJECTED
                : ClaimStatus.SETTLEMENT_IN_PROGRESS;
        transition(claim, nextStatus, managerUsername, "Manager finalized the claim decision");
        return toInternalResponse(claim);
    }

    @Transactional
    public InternalClaimResponse completeSettlement(UUID claimId, String staffUsername) {
        ClaimRecord claim = findClaim(claimId);
        requireAssignee(claim, staffUsername);
        requireStatus(claim, ClaimStatus.SETTLEMENT_IN_PROGRESS, "Only approved settlements can be completed.");
        transition(claim, ClaimStatus.SETTLED, staffUsername, "Settlement completed");
        return toInternalResponse(claim);
    }

    @Transactional(readOnly = true)
    public ManagerDashboard managerDashboard() {
        List<ClaimRecord> allClaims = claims.findAll();
        Map<ClaimStatus, Long> counts = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            if (!TERMINAL_STATUSES.contains(status)) {
                counts.put(status, 0L);
            }
        }

        Map<String, BigDecimal> exposure = new HashMap<>();
        for (ClaimRecord claim : allClaims) {
            if (!TERMINAL_STATUSES.contains(claim.getStatus())) {
                counts.computeIfPresent(claim.getStatus(), (status, count) -> count + 1);
                exposure.merge(claim.getCurrency(), claim.getClaimedAmount(), BigDecimal::add);
            }
        }
        return new ManagerDashboard(counts, exposure, teamWorkloadFrom(claims.findAllByAssignedStaffUsernameIsNotNull()));
    }

    @Transactional(readOnly = true)
    public List<StaffWorkload> teamWorkload() {
        return teamWorkloadFrom(claims.findAllByAssignedStaffUsernameIsNotNull());
    }

    @Transactional(readOnly = true)
    public ClaimNotificationTarget notificationTarget(UUID claimId) {
        ClaimRecord claim = findClaim(claimId);
        return new ClaimNotificationTarget(claim.getNotificationChannel(), claim.getNotificationAddress());
    }

    private void transition(
            ClaimRecord claim, ClaimStatus nextStatus, String changedBy, String reason) {
        if (claim.getStatus() == nextStatus) {
            throw new ClaimExceptions.Conflict("The claim already has that status.");
        }
        claim.transitionTo(nextStatus, changedBy, reason);
        outbox.save(new StatusOutboxRecord(claim.getId(), nextStatus));
    }

    private ClaimRecord findClaim(UUID claimId) {
        return claims.findById(claimId).orElseThrow(ClaimExceptions.NotFound::new);
    }

    private ClaimRecord findClaimForClaimant(UUID claimId, String claimantUsername) {
        return claims.findByIdAndClaimantUsername(claimId, claimantUsername)
                .orElseThrow(ClaimExceptions.NotFound::new);
    }

    private static void requireAssignee(ClaimRecord claim, String staffUsername) {
        if (!staffUsername.equals(claim.getAssignedStaffUsername())) {
            throw new AccessDeniedException("Only the assigned claims staff member can update this claim.");
        }
    }

    private static void requireStatus(ClaimRecord claim, ClaimStatus expected, String message) {
        if (claim.getStatus() != expected) {
            throw new ClaimExceptions.Conflict(message);
        }
    }

    private static void validateDecision(
            ClaimRecord claim, FinalDecision decision, BigDecimal settlementAmount) {
        if (decision == FinalDecision.SETTLE) {
            if (settlementAmount == null
                    || settlementAmount.signum() <= 0
                    || settlementAmount.compareTo(claim.getClaimedAmount()) > 0) {
                throw new ClaimExceptions.InvalidRequest(
                        "A settlement decision requires a positive amount no greater than the claimed amount.");
            }
        } else if (settlementAmount != null) {
            throw new ClaimExceptions.InvalidRequest(
                    "A settlement amount must not be supplied for a rejection.");
        }
    }

    private static void validateNotificationAddress(
            NotificationChannel channel, String address) {
        boolean valid = switch (channel) {
            case EMAIL -> EMAIL_PATTERN.matcher(address).matches();
            case SMS -> E164_PATTERN.matcher(address).matches();
        };
        if (!valid) {
            throw new ClaimExceptions.InvalidRequest(
                    channel == NotificationChannel.EMAIL
                            ? "Provide a valid email address for email notifications."
                            : "Provide a phone number in E.164 format for SMS notifications.");
        }
    }

    private static List<StaffWorkload> teamWorkloadFrom(List<ClaimRecord> assignedClaims) {
        Map<String, long[]> totals = new HashMap<>();
        for (ClaimRecord claim : assignedClaims) {
            long[] counts = totals.computeIfAbsent(claim.getAssignedStaffUsername(), ignored -> new long[2]);
            if (TERMINAL_STATUSES.contains(claim.getStatus())) {
                counts[1]++;
            } else {
                counts[0]++;
            }
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new StaffWorkload(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static ClaimantClaimResponse toClaimantResponse(ClaimRecord claim) {
        List<StatusHistoryEntry> history = claim.getStatusHistory().stream()
                .map(entry -> new StatusHistoryEntry(
                        entry.getPreviousStatus(),
                        entry.getNewStatus(),
                        null,
                        null,
                        entry.getChangedAt()))
                .toList();
        return new ClaimantClaimResponse(
                claim.getId(),
                claim.getIncidentDescription(),
                claim.getIncidentDate(),
                claim.getClaimedAmount(),
                claim.getCurrency(),
                claim.getStatus(),
                claim.getInformationRequest(),
                claim.getAdditionalInformation(),
                claim.getDecisionReason(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                history);
    }

    private static InternalClaimResponse toInternalResponse(ClaimRecord claim) {
        List<StatusHistoryEntry> history = claim.getStatusHistory().stream()
                .map(ClaimService::toStatusHistoryEntry)
                .toList();
        return new InternalClaimResponse(
                claim.getId(),
                claim.getClaimantUsername(),
                claim.getIncidentDescription(),
                claim.getIncidentDate(),
                claim.getClaimedAmount(),
                claim.getCurrency(),
                claim.getStatus(),
                claim.getAssignedStaffUsername(),
                claim.getInformationRequest(),
                claim.getAdditionalInformation(),
                claim.getAssessmentRecommendation(),
                claim.getAssessmentNotes(),
                claim.getDecisionReason(),
                claim.getApprovedSettlementAmount(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                history);
    }

    private static StatusHistoryEntry toStatusHistoryEntry(ClaimStatusHistory entry) {
        return new StatusHistoryEntry(
                entry.getPreviousStatus(),
                entry.getNewStatus(),
                entry.getChangedBy(),
                entry.getReason(),
                entry.getChangedAt());
    }
}
