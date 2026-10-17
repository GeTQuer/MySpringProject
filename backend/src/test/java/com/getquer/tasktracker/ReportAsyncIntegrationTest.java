package com.getquer.tasktracker;

import com.getquer.tasktracker.events.ReportRequestEvent;
import com.getquer.tasktracker.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReportAsyncIntegrationTest {
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean ReportService reportService;

    @Test
    void processorRunsAfterCommitInConfiguredExecutor() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        when(reportService.tryStartAttempt(jobId)).thenReturn(Optional.of(attemptId));
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            completed.countDown();
            return null;
        }).when(reportService).markCompleted(eq(jobId), eq(attemptId), any());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(new ReportRequestEvent(this, jobId));
            verifyNoInteractions(reportService);
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS), "Report processor did not complete");
        assertTrue(threadName.get().startsWith("task-events-"));
        verify(reportService).tryStartAttempt(jobId);
        verify(reportService).buildReport(jobId);
    }

    @Test
    void rollbackDoesNotLaunchProcessor() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(new ReportRequestEvent(this, UUID.randomUUID()));
            status.setRollbackOnly();
        });

        verifyNoInteractions(reportService);
    }
}
