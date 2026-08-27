package com.getquer.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity,Long> {
    Page<NotificationEntity> findByRecipientId(
            Long id,
            Pageable pageable
    );

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<NotificationEntity> findByIdAndRecipientId(Long id, Long recipientId);

    boolean existsByEventId(UUID eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                UPDATE notifications
                SET read_at = :readAt
                WHERE recipient_id = :recipientId
                  AND read_at IS NULL
                """,
            nativeQuery = true
    )
    int markAllAsRead(
            @Param("recipientId") Long recipientId,
            @Param("readAt") Instant readAt
    );

}
