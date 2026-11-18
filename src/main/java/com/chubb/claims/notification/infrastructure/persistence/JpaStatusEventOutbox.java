package com.chubb.claims.notification.infrastructure.persistence;

import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.notification.application.StatusEventOutbox;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStatusEventOutbox implements StatusEventOutbox {

    private final StatusOutboxRepository records;

    public JpaStatusEventOutbox(StatusOutboxRepository records) {
        this.records = records;
    }

    @Override
    public void recordStatusChange(UUID claimId, ClaimStatus newStatus) {
        records.save(new StatusOutboxRecord(claimId, newStatus));
    }
}
