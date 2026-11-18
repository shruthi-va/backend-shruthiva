package com.chubb.claims.notification;

import com.chubb.claims.claim.domain.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_status_outbox", indexes = {
        @Index(name = "idx_status_outbox_pending", columnList = "published_at, occurred_at")
})
public class StatusOutboxRecord {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 40)
    private ClaimStatus newStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected StatusOutboxRecord() {
    }

    public StatusOutboxRecord(UUID claimId, ClaimStatus newStatus) {
        this.eventId = UUID.randomUUID();
        this.claimId = claimId;
        this.newStatus = newStatus;
        this.occurredAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = error == null ? "Kafka publish failed." : error.substring(0, Math.min(error.length(), 1000));
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getClaimId() {
        return claimId;
    }

    public ClaimStatus getNewStatus() {
        return newStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
