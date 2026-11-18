package com.chubb.claims.notification;

import com.chubb.claims.claim.application.ClaimNotificationTarget;
import com.chubb.claims.claim.domain.ClaimStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DemoNotificationSender.class);

    @Override
    public void sendStatusUpdate(
            UUID claimId,
            ClaimStatus status,
            ClaimNotificationTarget target,
            UUID idempotencyKey) {
        log.info(
                "DEMO notification only (not delivered): channel={}, claimId={}, status={}, eventId={}",
                target.channel(),
                claimId,
                status,
                idempotencyKey);
    }
}
