package com.chubb.claims.notification.application;

import com.chubb.claims.claim.domain.ClaimStatus;
import java.util.UUID;

public interface StatusEventOutbox {

    void recordStatusChange(UUID claimId, ClaimStatus newStatus);
}
