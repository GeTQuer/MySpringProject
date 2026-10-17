package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.ReportJobEntity;
import com.getquer.tasktracker.Enums.ReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportJobRepositorty extends JpaRepository<ReportJobEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM ReportJobEntity j WHERE j.uuid = :jobId")
    Optional<ReportJobEntity> findByIdForUpdate(
            @Param("jobId") UUID jobId
    );

    List<ReportJobEntity>
    findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            ReportStatus status,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT j FROM ReportJobEntity j
    WHERE j.finishedAt IS NULL
      AND (
          (j.status = 'PENDING' AND j.createdAt < :cutoff)
          OR
          (j.status = 'PROCESSING' AND j.startedAt < :cutoff)
      )
    """)
    List<ReportJobEntity> findStalledReports(
            @Param("cutoff") Instant cutoff
    );
}
