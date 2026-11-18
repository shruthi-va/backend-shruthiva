package com.chubb.claims.notification;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.domain.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDeliveryRecord {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClaimStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "delivered_at", nullable = false)
    private Instant deliveredAt;

    protected NotificationDeliveryRecord() {
    }

    public NotificationDeliveryRecord(
            UUID eventId, UUID claimId, ClaimStatus status, NotificationChannel channel) {
        this.eventId = eventId;
        this.claimId = claimId;
        this.status = status;
        this.channel = channel;
        this.deliveredAt = Instant.now();
    }
}
