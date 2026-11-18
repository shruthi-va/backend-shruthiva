package com.chubb.claims.notification.application;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.application.ClaimNotificationTarget;
import java.util.UUID;

public interface NotificationSender {

    void sendStatusUpdate(
            UUID claimId,
            ClaimStatus status,
            ClaimNotificationTarget target,
            UUID idempotencyKey);
}
