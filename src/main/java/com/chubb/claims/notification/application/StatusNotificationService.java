package com.chubb.claims.notification.application;

import com.chubb.claims.claim.application.ClaimNotificationLookup;
import com.chubb.claims.claim.application.ClaimNotificationTarget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusNotificationService {

    private final ClaimNotificationLookup claimLookup;
    private final NotificationSender sender;
    private final NotificationDeliveryStore deliveries;

    public StatusNotificationService(
            ClaimNotificationLookup claimLookup,
            NotificationSender sender,
            NotificationDeliveryStore deliveries) {
        this.claimLookup = claimLookup;
        this.sender = sender;
        this.deliveries = deliveries;
    }

    @Transactional
    public void onStatusChanged(ClaimStatusChangedEvent event) {
        if (deliveries.wasDelivered(event.eventId())) {
            return;
        }
        ClaimNotificationTarget target = claimLookup.notificationTarget(event.claimId());
        sender.sendStatusUpdate(event.claimId(), event.newStatus(), target, event.eventId());
        deliveries.recordDelivered(event, target.channel());
    }
}
