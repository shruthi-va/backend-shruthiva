package com.chubb.claims.notification;

import com.chubb.claims.claim.application.ClaimNotificationTarget;
import com.chubb.claims.claim.domain.ClaimStatus;
import java.util.UUID;

public interface NotificationSender {

    void sendStatusUpdate(
            UUID claimId,
            ClaimStatus status,
            ClaimNotificationTarget target,
            UUID idempotencyKey);
}
