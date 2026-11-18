package com.chubb.claims.notification.infrastructure.kafka;

import com.chubb.claims.notification.application.ClaimStatusChangedEvent;
import com.chubb.claims.notification.application.StatusNotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "claims.notifications.kafka.enabled", havingValue = "true")
public class StatusNotificationConsumer {

    private final StatusNotificationService notifications;

    public StatusNotificationConsumer(StatusNotificationService notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = "${claims.notifications.kafka.topic}")
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        notifications.onStatusChanged(event);
    }
}
