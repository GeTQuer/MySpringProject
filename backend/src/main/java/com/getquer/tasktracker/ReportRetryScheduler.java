package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.ReportJobEntity;
import com.getquer.tasktracker.Enums.ReportStatus;
import com.getquer.tasktracker.Repositories.ReportJobRepositorty;
import com.getquer.tasktracker.events.ReportRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportRetryScheduler {

    private final ReportJobRepositorty repository;
    private final ApplicationEventPublisher publisher;

    @Scheduled(
            fixedDelayString = "${reports.retry.fixed-delay-ms:30000}",
            initialDelayString = "${reports.retry.initial-delay-ms:30000}"
    )
    @Transactional(readOnly = true)
    public void retryReports() {
        List<ReportJobEntity> jobs = repository
                .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        ReportStatus.RETRY_PENDING,
                        Instant.now()
                );

        for (ReportJobEntity job : jobs) {
            publisher.publishEvent(
                    new ReportRequestEvent(this, job.getUuid())
            );
        }
    }
}
