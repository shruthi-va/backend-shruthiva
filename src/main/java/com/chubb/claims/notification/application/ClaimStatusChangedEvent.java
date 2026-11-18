package com.chubb.claims.notification.application;

import com.chubb.claims.claim.domain.ClaimStatus;
import java.time.Instant;
import java.util.UUID;

public record ClaimStatusChangedEvent(
        UUID eventId,
        UUID claimId,
        ClaimStatus newStatus,
        Instant occurredAt) {
}
