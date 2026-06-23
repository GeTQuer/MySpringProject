package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.TaskEntity;
import com.getquer.tasktracker.TaskStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long>
{
    List<TaskEntity> findAllByUserUsernameAndStatus(String username, TaskStatus status);

    List<TaskEntity> findAllByUserUsername(String username);

    List<TaskEntity> findAllByStatus(TaskStatus status);

    Optional<TaskEntity> findByIdAndUserUsername(Long id, String username);

    @Modifying
    @Transactional
    @Query("Update TaskEntity t SET t.status = 'OVERDUE'" +
            "WHERE t.status NOT IN ('DONE','CANCELLED','OVERDUE')" +
            "AND t.deadline < :now")
    void markOverdueTasks(@Param("now")LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskEntity t WHERE t.stauts = 'OVERDUE '" +
            "AND t.deadline < :threseholDate")
    void deleteOldOverdueTasks(@Param("threseholDate") LocalDateTime threseholDate);


}
