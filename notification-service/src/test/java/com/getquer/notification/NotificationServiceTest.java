package com.getquer.notification;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long RECIPIENT_ID = 10L;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getNotificationsReturnsRecipientPage() {
        NotificationEntity unread = notification(1L, null);
        NotificationEntity read = notification(2L, Instant.parse("2026-08-27T10:00:00Z"));

        when(notificationRepository.findByRecipientId(eq(RECIPIENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(read, unread)));

        Page<NotificationDTO> result =
                notificationService.getNotifications(RECIPIENT_ID, 0, 20);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().get(0).read());
        assertFalse(result.getContent().get(1).read());
        verify(notificationRepository)
                .findByRecipientId(eq(RECIPIENT_ID), any(Pageable.class));
    }

    @Test
    void getUnreadCountUsesRecipientId() {
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(RECIPIENT_ID))
                .thenReturn(3L);

        assertEquals(3L, notificationService.getUnreadCount(RECIPIENT_ID));
        verify(notificationRepository)
                .countByRecipientIdAndReadAtIsNull(RECIPIENT_ID);
    }

    @Test
    void markAsReadChangesOnlyOwnedNotification() {
        NotificationEntity entity = notification(5L, null);
        when(notificationRepository.findByIdAndRecipientId(5L, RECIPIENT_ID))
                .thenReturn(Optional.of(entity));

        NotificationDTO result = notificationService.markAsRead(5L, RECIPIENT_ID);

        assertTrue(result.read());
        assertNotNull(result.readAt());
        assertNotNull(entity.getReadAt());
        verify(notificationRepository).findByIdAndRecipientId(5L, RECIPIENT_ID);
    }

    @Test
    void markAsReadRejectsForeignOrMissingNotification() {
        when(notificationRepository.findByIdAndRecipientId(99L, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> notificationService.markAsRead(99L, RECIPIENT_ID)
        );
    }

    @Test
    void markAllAsReadPassesRecipientAndTimestamp() {
        notificationService.markAllAsRead(RECIPIENT_ID);

        ArgumentCaptor<Instant> timestamp = ArgumentCaptor.forClass(Instant.class);
        verify(notificationRepository).markAllAsRead(eq(RECIPIENT_ID), timestamp.capture());
        assertNotNull(timestamp.getValue());
    }

    private NotificationEntity notification(Long id, Instant readAt) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setEventId(UUID.randomUUID());
        entity.setRecipientId(RECIPIENT_ID);
        entity.setActorId(2L);
        entity.setTaskId(7L);
        entity.setType(NotificationType.TASK_ASSIGNED);
        entity.setTitle("Вам назначена новая задача");
        entity.setMessage("manager назначил вам задачу");
        entity.setCreatedAt(Instant.parse("2026-08-27T09:00:00Z"));
        entity.setReadAt(readAt);
        return entity;
    }
}
