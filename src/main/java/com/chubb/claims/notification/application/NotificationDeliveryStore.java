package com.chubb.claims.notification.application;

import com.chubb.claims.claim.domain.NotificationChannel;
import java.util.UUID;

public interface NotificationDeliveryStore {

    boolean wasDelivered(UUID eventId);

    void recordDelivered(ClaimStatusChangedEvent event, NotificationChannel channel);
}
