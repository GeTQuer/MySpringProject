package com.getquer.tasktracker.Repositories;

import com.getquer.tasktracker.Entities.TaskCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

public interface TaskCommentsRepository extends JpaRepository<TaskCommentEntity,Long>{

    @Query(
            value = """
            SELECT c 
            FROM TaskCommentEntity c 
            JOIN FETCH c.author 
            WHERE c.task.id = :taskId
            """,
            countQuery = """
                 SELECT count(c) 
                 FROM TaskCommentEntity c 
                 WHERE c.task.id = :taskId
                 """
    )
    Page<TaskCommentEntity> findAllByTask_Id(@Param("taskId") Long taskId, Pageable pageable);

    Optional<TaskCommentEntity> findByIdAndAuthor_Id(
            Long commentId,
            Long authorId
    );

    boolean existsByIdAndAuthor_Id(
            Long commentId,
            Long authorId
    );

    long countByTask_Id(Long taskId);

    void deleteAllByTask_Id(Long taskId);
}