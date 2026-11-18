package com.chubb.claims.notification;

import com.chubb.claims.claim.application.ClaimNotificationTarget;
import com.chubb.claims.claim.application.ClaimService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "claims.notifications.kafka.enabled", havingValue = "true")
public class StatusNotificationConsumer {

    private final ClaimService claims;
    private final NotificationSender sender;
    private final NotificationDeliveryRepository deliveries;

    public StatusNotificationConsumer(
            ClaimService claims,
            NotificationSender sender,
            NotificationDeliveryRepository deliveries) {
        this.claims = claims;
        this.sender = sender;
        this.deliveries = deliveries;
    }

    @KafkaListener(topics = "${claims.notifications.kafka.topic}")
    @Transactional
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        if (deliveries.existsById(event.eventId())) {
            return;
        }
        ClaimNotificationTarget target = claims.notificationTarget(event.claimId());
        sender.sendStatusUpdate(event.claimId(), event.newStatus(), target, event.eventId());
        deliveries.save(new NotificationDeliveryRecord(
                event.eventId(), event.claimId(), event.newStatus(), target.channel()));
    }
}
