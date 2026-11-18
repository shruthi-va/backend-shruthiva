package com.chubb.claims.claim.api;

import com.chubb.claims.claim.domain.AssessmentRecommendation;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.FinalDecision;
import com.chubb.claims.claim.domain.NotificationChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClaimDtos {

    private ClaimDtos() {
    }

    public record SubmitClaimRequest(
            @NotBlank @Size(max = 4000) String incidentDescription,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal claimedAmount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull NotificationChannel notificationChannel,
            @NotBlank @Size(max = 254) String notificationAddress) {
    }

    public record InformationRequest(
            @NotBlank @Size(max = 2000) String message) {
    }

    public record InformationResponse(
            @NotBlank @Size(max = 4000) String information) {
    }

    public record AssessmentRequest(
            @NotNull AssessmentRecommendation recommendation,
            @NotBlank @Size(max = 2000) String notes) {
    }

    public record ManagerDecisionRequest(
            @NotNull FinalDecision decision,
            @NotBlank @Size(max = 2000) String reason,
            @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal settlementAmount) {
    }

    public record ClaimantClaimResponse(
            UUID id,
            String incidentDescription,
            LocalDate incidentDate,
            BigDecimal claimedAmount,
            String currency,
            ClaimStatus status,
            String informationRequest,
            String additionalInformation,
            String decisionReason,
            Instant createdAt,
            Instant updatedAt,
            List<StatusHistoryEntry> statusHistory) {
    }

    public record InternalClaimResponse(
            UUID id,
            String claimantUsername,
            String incidentDescription,
            LocalDate incidentDate,
            BigDecimal claimedAmount,
            String currency,
            ClaimStatus status,
            String assignedStaffUsername,
            String informationRequest,
            String additionalInformation,
            AssessmentRecommendation assessmentRecommendation,
            String assessmentNotes,
            String decisionReason,
            BigDecimal approvedSettlementAmount,
            Instant createdAt,
            Instant updatedAt,
            List<StatusHistoryEntry> statusHistory) {
    }

    public record StatusHistoryEntry(
            ClaimStatus previousStatus,
            ClaimStatus newStatus,
            String changedBy,
            String reason,
            Instant changedAt) {
    }

    public record StaffWorkload(
            String staffUsername,
            long openClaims,
            long completedClaims) {
    }

    public record ManagerDashboard(
            Map<ClaimStatus, Long> outstandingClaimsByStatus,
            Map<String, BigDecimal> claimedAmountExposureByCurrency,
            List<StaffWorkload> teamWorkload) {
    }
}
