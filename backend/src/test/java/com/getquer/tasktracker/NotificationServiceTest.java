package com.getquer.tasktracker;

import com.getquer.tasktracker.Entities.NotificationEntity;
import com.getquer.tasktracker.Entities.UserEntity;
import com.getquer.tasktracker.Enums.NotificationType;
import com.getquer.tasktracker.Repositories.NotificationRepository;
import com.getquer.tasktracker.Repositories.UserRepository;
import com.getquer.tasktracker.events.TaskAssignedEventV1;
import com.getquer.tasktracker.responseDTO.NotificationDTO;
import com.getquer.tasktracker.service.NotificationService;
import com.getquer.tasktracker.util.NotificationTestDataMother;
import com.getquer.tasktracker.util.TaskTestDataMother;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String USERNAME = "employee";
    private static final Long RECIPIENT_ID = 20L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotification_ShouldSaveAndReturnTaskAssignedNotification() {
        TaskAssignedEventV1 event =
                NotificationTestDataMother.createTaskAssignedEvent(
                        100L,
                        10L,
                        RECIPIENT_ID
                );

        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> {
                    NotificationEntity notification = invocation.getArgument(0);
                    notification.setId(1L);
                    return notification;
                });

        NotificationDTO result = notificationService.createNotification(event);

        assertEquals(1L, result.id());
        assertEquals(NotificationType.TASK_ASSIGNED, result.notificationType());
        assertEquals(event.taskId(), result.taskId());
        assertEquals(event.occurredAt(), result.createdAt());
        assertFalse(result.read());
        assertNull(result.readAt());
        assertTrue(result.message().contains(event.actorUsername()));
        assertTrue(result.message().contains(event.taskTitle()));

        ArgumentCaptor<NotificationEntity> captor =
                ArgumentCaptor.forClass(NotificationEntity.class);

        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();
        assertEquals(event.eventId(), saved.getEventId());
        assertEquals(event.actorId(), saved.getActorId());
        assertEquals(event.recipientId(), saved.getRecipientId());
        assertEquals(event.taskId(), saved.getTaskId());
        assertEquals(NotificationType.TASK_ASSIGNED, saved.getType());
    }

    @Test
    void getNotifications_ShouldReturnUserNotifications() {
        UserEntity user = createRecipient();
        NotificationEntity unreadNotification =
                NotificationTestDataMother.createUnreadNotification(1L, RECIPIENT_ID);
        NotificationEntity readNotification =
                NotificationTestDataMother.createReadNotification(2L, RECIPIENT_ID);

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by("id").descending()
        );

        Page<NotificationEntity> notificationPage = new PageImpl<>(
                List.of(unreadNotification, readNotification),
                pageable,
                2
        );

        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipientId(RECIPIENT_ID, pageable))
                .thenReturn(notificationPage);

        Page<NotificationDTO> result =
                notificationService.getNotifications(USERNAME, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        NotificationDTO first = result.getContent().get(0);
        NotificationDTO second = result.getContent().get(1);

        assertEquals(unreadNotification.getId(), first.id());
        assertEquals(unreadNotification.getTaskId(), first.taskId());
        assertFalse(first.read());
        assertNull(first.readAt());

        assertEquals(readNotification.getId(), second.id());
        assertEquals(readNotification.getTaskId(), second.taskId());
        assertTrue(second.read());
        assertEquals(NotificationTestDataMother.READ_AT, second.readAt());

        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository).findByRecipientId(RECIPIENT_ID, pageable);
    }

    @Test
    void getNotifications_ShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> notificationService.getNotifications(USERNAME, 0, 20)
        );

        assertTrue(exception.getMessage().contains(USERNAME));
        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository, never())
                .findByRecipientId(any(), any());
    }

    @Test
    void getUnreadCount_ShouldReturnRepositoryCount() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(createRecipient()));
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(RECIPIENT_ID))
                .thenReturn(3L);

        Long result = notificationService.getUnreadCount(USERNAME);

        assertEquals(3L, result);
        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository)
                .countByRecipientIdAndReadAtIsNull(RECIPIENT_ID);
    }

    @Test
    void markAsRead_ShouldSetReadAtForUnreadNotification() {
        NotificationEntity notification =
                NotificationTestDataMother.createUnreadNotification(1L, RECIPIENT_ID);

        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(createRecipient()));
        when(notificationRepository.findByIdAndRecipientId(1L, RECIPIENT_ID))
                .thenReturn(Optional.of(notification));

        NotificationDTO result = notificationService.markAsRead(1L, USERNAME);

        assertTrue(result.read());
        assertNotNull(result.readAt());
        assertEquals(notification.getReadAt(), result.readAt());
        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository).findByIdAndRecipientId(1L, RECIPIENT_ID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_ShouldKeepExistingReadAt() {
        NotificationEntity notification =
                NotificationTestDataMother.createReadNotification(1L, RECIPIENT_ID);
        Instant originalReadAt = notification.getReadAt();

        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(createRecipient()));
        when(notificationRepository.findByIdAndRecipientId(1L, RECIPIENT_ID))
                .thenReturn(Optional.of(notification));

        NotificationDTO result = notificationService.markAsRead(1L, USERNAME);

        assertTrue(result.read());
        assertSame(originalReadAt, notification.getReadAt());
        assertEquals(originalReadAt, result.readAt());
        verify(notificationRepository).findByIdAndRecipientId(1L, RECIPIENT_ID);
    }

    @Test
    void markAsRead_ShouldThrowWhenNotificationDoesNotBelongToUser() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(createRecipient()));
        when(notificationRepository.findByIdAndRecipientId(99L, RECIPIENT_ID))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> notificationService.markAsRead(99L, USERNAME)
        );

        assertEquals("Notification not found", exception.getMessage());
        verify(notificationRepository).findByIdAndRecipientId(99L, RECIPIENT_ID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_ShouldUpdateAllUnreadNotificationsForUser() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.of(createRecipient()));
        when(notificationRepository.markAllAsRead(any(), any()))
                .thenReturn(3);

        notificationService.markAllAsRead(USERNAME);

        ArgumentCaptor<Instant> readAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository).markAllAsRead(
                org.mockito.ArgumentMatchers.eq(RECIPIENT_ID),
                readAtCaptor.capture()
        );
        assertNotNull(readAtCaptor.getValue());
    }

    @Test
    void markAllAsRead_ShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> notificationService.markAllAsRead(USERNAME)
        );

        verify(userRepository).findByUsername(USERNAME);
        verify(notificationRepository, never()).markAllAsRead(any(), any());
    }

    private UserEntity createRecipient() {
        return TaskTestDataMother.createTestUser(
                RECIPIENT_ID,
                USERNAME,
                "USER"
        );
    }
}
