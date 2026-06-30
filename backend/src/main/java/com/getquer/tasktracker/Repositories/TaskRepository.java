package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.TaskStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>
{
    @Query(
            value = "SELECT t FROM TaskEntity t JOIN FETCH t.user u WHERE t.status = :status",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t WHERE t.status = :status and t.username = :username"
    )
    Page<TaskEntity> findAllByUserUsernameAndStatus(@Param("username") String username,
                                                    @Param("status") TaskStatus status,
                                                    Pageable pageable);

    @Query(
            value = "SELECT t FROM TaskEntity t JOIN FETCH t.user u WHERE u.username = :username",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t WHERE t.user.username = :username"
    )
    Page<TaskEntity> findAllByUserUsername(@Param("username") String username, Pageable pageable);

    @Query(
            value = "SELECT t FROM TaskEntity t JOIN FETCH t.user u WHERE t.status = :status",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t WHERE t.status = :status"
    )
    Page<TaskEntity> findAllByStatus(@Param("status") TaskStatus status, Pageable pageable);

    @Query(
            value = "SELECT t FROM TaskEntity t JOIN FETCH t.user",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t"
    )
    Page<TaskEntity> findAll(Pageable pageable);

    Optional<TaskEntity> findById(@Param("id") Long id);

    Optional<TaskEntity> findByIdAndUserUsername(Long id, String username);

    @Modifying
    @Transactional
    @Query("Update TaskEntity t SET t.status = 'OVERDUE'" +
            "WHERE t.status NOT IN ('DONE','CANCELLED','OVERDUE')" +
            "AND t.deadline < :now")
    void markOverdueTasks(@Param("now")LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskEntity t WHERE t.status = 'OVERDUE'" +
            "AND t.deadline < :threseholDate")
    void deleteOldOverdueTasks(@Param("threseholDate") LocalDateTime threseholDate);
}
