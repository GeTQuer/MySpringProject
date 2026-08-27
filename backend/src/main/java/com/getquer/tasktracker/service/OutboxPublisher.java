package com.getquer.tasktracker.service;

import com.getquer.tasktracker.Entities.OutboxEventEntity;
import com.getquer.tasktracker.Repositories.OutboxEventRepository;
import com.getquer.tasktracker.config.KafkaTopic;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public int publishPendingEvents() {
        List<OutboxEventEntity> events =
                outboxEventRepository
                        .findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {
            publish(event);
            event.setPublishedAt(Instant.now());
        }

        return events.size();
    }

    private void publish(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(
                    KafkaTopic.TASK_EVENT_TOPIC,
                    event.getTaskId().toString(),
                    event.getPayload()
            ).get(10, TimeUnit.SECONDS);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Публикация события была прервана: "
                            + event.getEventId(),
                    exception
            );

        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Не удалось отправить событие в Kafka: "
                            + event.getEventId(),
                    exception
            );
        }
    }
}