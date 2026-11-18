package com.chubb.claims.claim.application;

import java.util.UUID;

public interface ClaimNotificationLookup {

    ClaimNotificationTarget notificationTarget(UUID claimId);
}
