package com.chubb.claims.notification;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "claims.notifications.kafka.enabled", havingValue = "true")
public class KafkaNotificationConfiguration {

    @Bean
    NewTopic claimStatusChangedTopic(@Value("${claims.notifications.kafka.topic}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }
}
