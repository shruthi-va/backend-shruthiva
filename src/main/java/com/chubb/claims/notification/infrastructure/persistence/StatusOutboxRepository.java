package com.chubb.claims.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusOutboxRepository extends JpaRepository<StatusOutboxRecord, UUID> {

    List<StatusOutboxRecord> findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();

    long countByPublishedAtIsNull();
}
