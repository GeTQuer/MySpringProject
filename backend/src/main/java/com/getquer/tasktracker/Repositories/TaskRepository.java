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
            value = "SELECT t.id FROM TaskEntity t JOIN t.user u LEFT JOIN t.department d WHERE t.status = :status AND u.username = :username AND (:departmentId IS NULL OR d.id = :departmentId)",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t JOIN t.user u LEFT JOIN t.department d WHERE t.status = :status AND u.username = :username AND (:departmentId IS NULL OR d.id = :departmentId)"
    )
    Page<Long> findAllByUserUsernameAndStatus(@Param("username") String username,
                                              @Param("status") TaskStatus status,
                                              @Param("departmentId") Long departmentId,
                                              Pageable pageable);

    @Query(
            value = "SELECT t.id FROM TaskEntity t JOIN t.user u LEFT JOIN t.department d WHERE u.username = :username AND (:departmentId IS NULL OR d.id = :departmentId)",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t JOIN t.user u LEFT JOIN t.department d WHERE u.username = :username AND (:departmentId IS NULL OR d.id = :departmentId)"
    )
    Page<Long> findAllByUserUsername(@Param("username") String username,
                                     @Param("departmentId") Long departmentId,
                                     Pageable pageable);

    @Query(
            value = "SELECT t.id FROM TaskEntity t WHERE t.status = :status",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t WHERE t.status = :status"
    )
    Page<Long> findAllByStatus(@Param("status") TaskStatus status, Pageable pageable);

    @Query(
            value = "SELECT t.id FROM TaskEntity t",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t"
    )


    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT t FROM TaskEntity t JOIN FETCH t.user WHERE t.id IN :ids")
    List<TaskEntity> findAllByIdsWithUser(@Param("ids") List<Long> ids);

    @Query("SELECT t FROM TaskEntity t WHERE t.id = :id AND t.user.username = :username AND t.department.id = :departmentId")
    Optional<TaskEntity> findByIdAndUsernameAndDepartmentId(
            @Param("id") Long id,
            @Param("username") String username,
            @Param("departmentId") Long departmentId
    );
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



    @Query(
            value = "SELECT t.id FROM TaskEntity t JOIN t.department d WHERE d.id = :id",
            countQuery = "SELECT COUNT(t) FROM TaskEntity t JOIN t.department d WHERE d.id = :id"
    )


    Page<Long> findAllTasksByDepartmentId(@Param("id") Long id, Pageable pageable);

    @Query(
            value = "SELECT t.id FROM TaskEntity t JOIN t.department d WHERE d.id = :id and t.status = :status",
            countQuery =  "SELECT COUNT(t) FROM TaskEntity t JOIN t.department d WHERE d.id = :id and t.status = :status"
    )
    Page<Long> findAllTasksByDepartmentIdAndStatus(@Param("id") Long id, @Param("status") TaskStatus status, Pageable pageable);



}
