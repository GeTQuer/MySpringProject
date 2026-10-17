package com.getquer.tasktracker;


import com.getquer.tasktracker.Entities.ReportJobEntity;
import com.getquer.tasktracker.Enums.ReportStatus;
import com.getquer.tasktracker.Repositories.ReportJobRepositorty;
import com.getquer.tasktracker.events.ReportRequestEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportRecoveryScheduler{
    private final ReportJobRepositorty repository;
    private final ApplicationEventPublisher publisher;

    @Value("${reports.recovery.stalled-timeout:PT10M}")
    private Duration stalledTimeout;

    @Scheduled(
            fixedDelayString = "${reports.recovery.fixed-delay-ms:60000}",
            initialDelayString = "${reports.recovery.initial-delay-ms:60000}"
    )
    @Transactional
    public void recoverStalledReports() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(stalledTimeout);

        List<ReportJobEntity> needToRetry = repository.findStalledReports(cutoff);

        for (ReportJobEntity job: needToRetry){
            job.setStatus(ReportStatus.RETRY_PENDING);
            job.setNextAttemptAt(now);
            job.setAttemptId(null);
            publisher.publishEvent(
                    new ReportRequestEvent(this, job.getUuid())
            );
        }

    }
}
