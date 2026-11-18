package com.chubb.claims.notification.infrastructure.kafka;

import com.chubb.claims.notification.application.ClaimStatusChangedEvent;
import com.chubb.claims.notification.infrastructure.persistence.StatusOutboxRecord;
import com.chubb.claims.notification.infrastructure.persistence.StatusOutboxRepository;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "claims.notifications.kafka.enabled", havingValue = "true")
public class StatusOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(StatusOutboxPublisher.class);
    private final StatusOutboxRepository outbox;
    private final KafkaTemplate<String, ClaimStatusChangedEvent> kafkaTemplate;
    private final String topic;

    public StatusOutboxPublisher(
            StatusOutboxRepository outbox,
            KafkaTemplate<String, ClaimStatusChangedEvent> kafkaTemplate,
            org.springframework.core.env.Environment environment) {
        this.outbox = outbox;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = environment.getRequiredProperty("claims.notifications.kafka.topic");
    }

    @Scheduled(fixedDelayString = "${claims.notifications.outbox-poll-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (StatusOutboxRecord record : outbox.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            ClaimStatusChangedEvent event = new ClaimStatusChangedEvent(
                    record.getEventId(),
                    record.getClaimId(),
                    record.getNewStatus(),
                    record.getOccurredAt());
            try {
                kafkaTemplate.send(topic, record.getClaimId().toString(), event)
                        .get(5, TimeUnit.SECONDS);
                record.markPublished();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                record.recordFailure(exception.getMessage());
                log.error("Interrupted while publishing claim status event {}", record.getEventId(), exception);
                return;
            } catch (ExecutionException | TimeoutException | KafkaException exception) {
                record.recordFailure(exception.getMessage());
                log.warn("Could not publish claim status event {}; it will be retried", record.getEventId(), exception);
            }
        }
    }
}
