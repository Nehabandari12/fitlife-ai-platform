package com.fitness.aiservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    /**
     * A failing event is retried 3 times, 2 seconds apart, then logged and skipped, so one bad event
     * can't block its partition. Events that can't be deserialized are skipped without retries.
     * Only the record's coordinates are logged, not its value (a person's workout data).
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (record, error) -> log.error("Giving up on {}-{} offset {} (key {}): {}",
                        record.topic(), record.partition(), record.offset(), record.key(), error.getMessage()),
                new FixedBackOff(2000L, 3));
    }
}
