package com.chubb.claims.claim.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claims_claimant", columnList = "claimant_username"),
        @Index(name = "idx_claims_status", columnList = "status"),
        @Index(name = "idx_claims_assignee", columnList = "assigned_staff_username")
})
public class ClaimRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claimant_username", nullable = false, length = 100)
    private String claimantUsername;

    @Column(name = "incident_description", nullable = false, length = 4000)
    private String incidentDescription;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal claimedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 10)
    private NotificationChannel notificationChannel;

    @Column(name = "notification_address", nullable = false, length = 254)
    private String notificationAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClaimStatus status;

    @Column(name = "assigned_staff_username", length = 100)
    private String assignedStaffUsername;

    @Column(name = "information_request", length = 2000)
    private String informationRequest;

    @Column(name = "additional_information", length = 4000)
    private String additionalInformation;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_recommendation", length = 20)
    private AssessmentRecommendation assessmentRecommendation;

    @Column(name = "assessment_notes", length = 2000)
    private String assessmentNotes;

    @Column(name = "decision_reason", length = 2000)
    private String decisionReason;

    @Column(name = "approved_settlement_amount", precision = 19, scale = 2)
    private BigDecimal approvedSettlementAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<ClaimStatusHistory> statusHistory = new ArrayList<>();

    protected ClaimRecord() {
    }

    public ClaimRecord(
            String claimantUsername,
            String incidentDescription,
            LocalDate incidentDate,
            BigDecimal claimedAmount,
            String currency,
            NotificationChannel notificationChannel,
            String notificationAddress) {
        this.claimantUsername = claimantUsername;
        this.incidentDescription = incidentDescription;
        this.incidentDate = incidentDate;
        this.claimedAmount = claimedAmount;
        this.currency = currency;
        this.notificationChannel = notificationChannel;
        this.notificationAddress = notificationAddress;
        this.status = ClaimStatus.SUBMITTED;
        recordStatusChange(null, ClaimStatus.SUBMITTED, claimantUsername, "Claim submitted");
    }

    public void assignTo(String staffUsername) {
        this.assignedStaffUsername = staffUsername;
    }

    public void requestInformation(String message) {
        this.informationRequest = message;
        this.additionalInformation = null;
    }

    public void provideInformation(String response) {
        this.additionalInformation = response;
    }

    public void recordAssessment(AssessmentRecommendation recommendation, String notes) {
        this.assessmentRecommendation = recommendation;
        this.assessmentNotes = notes;
    }

    public void recordDecision(String reason, BigDecimal settlementAmount) {
        this.decisionReason = reason;
        this.approvedSettlementAmount = settlementAmount;
    }

    public void transitionTo(ClaimStatus newStatus, String changedBy, String reason) {
        ClaimStatus previousStatus = this.status;
        this.status = newStatus;
        recordStatusChange(previousStatus, newStatus, changedBy, reason);
    }

    private void recordStatusChange(
            ClaimStatus previousStatus, ClaimStatus newStatus, String changedBy, String reason) {
        this.statusHistory.add(
                new ClaimStatusHistory(this, previousStatus, newStatus, changedBy, reason));
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getClaimantUsername() {
        return claimantUsername;
    }

    public String getIncidentDescription() {
        return incidentDescription;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public BigDecimal getClaimedAmount() {
        return claimedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public String getNotificationAddress() {
        return notificationAddress;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public String getAssignedStaffUsername() {
        return assignedStaffUsername;
    }

    public String getInformationRequest() {
        return informationRequest;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public AssessmentRecommendation getAssessmentRecommendation() {
        return assessmentRecommendation;
    }

    public String getAssessmentNotes() {
        return assessmentNotes;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public BigDecimal getApprovedSettlementAmount() {
        return approvedSettlementAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ClaimStatusHistory> getStatusHistory() {
        return List.copyOf(statusHistory);
    }
}
