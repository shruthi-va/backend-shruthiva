package com.chubb.claims.notification.infrastructure.persistence;

import com.chubb.claims.claim.domain.NotificationChannel;
import com.chubb.claims.notification.application.ClaimStatusChangedEvent;
import com.chubb.claims.notification.application.NotificationDeliveryStore;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationDeliveryStore implements NotificationDeliveryStore {

    private final NotificationDeliveryRepository records;

    public JpaNotificationDeliveryStore(NotificationDeliveryRepository records) {
        this.records = records;
    }

    @Override
    public boolean wasDelivered(UUID eventId) {
        return records.existsById(eventId);
    }

    @Override
    public void recordDelivered(ClaimStatusChangedEvent event, NotificationChannel channel) {
        records.save(new NotificationDeliveryRecord(
                event.eventId(), event.claimId(), event.newStatus(), channel));
    }
}
