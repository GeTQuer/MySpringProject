package com.getquer.tasktracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxPublisher outboxPublisher;

    @Scheduled(
            fixedDelayString =
                    "${outbox.publisher.fixed-delay-ms:5000}",
            initialDelayString =
                    "${outbox.publisher.initial-delay-ms:5000}"
    )
    public void publishPendingEvents() {
        try {
            int publishedCount =
                    outboxPublisher.publishPendingEvents();

            if (publishedCount > 0) {
                log.info(
                        "Published {} outbox event(s)",
                        publishedCount
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to publish pending outbox events",
                    exception
            );
        }
    }
}