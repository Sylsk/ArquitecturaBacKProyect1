package com.team.socialnetwork.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.socialnetwork.dto.NotificationCountResponse;
import com.team.socialnetwork.dto.NotificationResponse;
import com.team.socialnetwork.dto.SafeUser;
import com.team.socialnetwork.entity.Comment;
import com.team.socialnetwork.entity.Notification;
import com.team.socialnetwork.entity.Post;
import com.team.socialnetwork.entity.User;
import com.team.socialnetwork.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, 
                              SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Crear y enviar notificación por WebSocket
     */
    @Transactional
    public void createAndSendNotification(User recipient, User actor, Notification.NotificationType type) {
        createAndSendNotification(recipient, actor, type, null, null);
    }

    @Transactional
    public void createAndSendNotification(User recipient, User actor, Notification.NotificationType type, Post post) {
        createAndSendNotification(recipient, actor, type, post, null);
    }

    @Transactional
    public void createAndSendNotification(User recipient, User actor, Notification.NotificationType type, 
                                        Post post, Comment comment) {
        // Evitar auto-notificaciones
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        // Verificar si ya existe esta notificación para evitar spam
        if (shouldSkipNotification(recipient.getId(), actor.getId(), type, 
                                  post != null ? post.getId() : null, 
                                  comment != null ? comment.getId() : null)) {
            return;
        }

        // Crear la notificación
        Notification notification = new Notification(recipient, actor, type, post, comment);
        notificationRepository.save(notification);

        // Convertir a DTO y enviar por WebSocket
        NotificationResponse notificationResponse = convertToResponse(notification);
        sendNotificationByWebSocket(recipient.getId(), notificationResponse);

        // También enviar actualización del contador
        sendUnreadCountUpdate(recipient.getId());
    }

    /**
     * Eliminar notificación cuando se deshace una acción (unlike, unfollow)
     */
    @Transactional
    public void removeNotification(Long recipientId, Long actorId, Notification.NotificationType type, Long postId) {
        int deleted = notificationRepository.deleteByRecipientIdAndActorIdAndTypeAndPostId(
                recipientId, actorId, type, postId);
        
        if (deleted > 0) {
            // Enviar actualización del contador
            sendUnreadCountUpdate(recipientId);
        }
    }

    @Transactional
    public void removeFollowNotification(Long recipientId, Long actorId, Notification.NotificationType type) {
        int deleted = notificationRepository.deleteByRecipientIdAndActorIdAndType(recipientId, actorId, type);
        
        if (deleted > 0) {
            // Enviar actualización del contador
            sendUnreadCountUpdate(recipientId);
        }
    }

    @Transactional
    public void removeCommentLikeNotification(Long recipientId, Long actorId, Long commentId) {
        // Método específico para eliminar notificaciones de likes en comentarios
        // Necesitamos buscar por commentId en lugar de postId
        int deleted = notificationRepository.deleteByRecipientIdAndActorIdAndTypeAndCommentId(
                recipientId, actorId, Notification.NotificationType.COMMENT_LIKE, commentId);
        
        if (deleted > 0) {
            // Enviar actualización del contador
            sendUnreadCountUpdate(recipientId);
        }
    }

    /**
     * Obtener notificaciones paginadas de un usuario
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        
        return notifications.map(this::convertToResponse);
    }

    /**
     * Contar notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        return new NotificationCountResponse(count);
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByRecipientId(userId);
        
        if (updated > 0) {
            // Enviar actualización del contador
            sendUnreadCountUpdate(userId);
            
            // Notificar que se marcaron como leídas
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, 
                    new NotificationWebSocketMessage("NOTIFICATIONS_MARKED_READ", null, 0L));
        }
        
        return updated;
    }

    /**
     * Marcar una notificación específica como leída
     */
    @Transactional
    public boolean markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsReadByIdAndRecipientId(notificationId, userId);
        
        if (updated > 0) {
            // Enviar actualización del contador
            sendUnreadCountUpdate(userId);
            return true;
        }
        
        return false;
    }

    /**
     * Limpiar notificaciones cuando se elimina contenido
     */
    @Transactional
    public void cleanupNotificationsForPost(Long postId) {
        notificationRepository.deleteByPostId(postId);
    }

    @Transactional
    public void cleanupNotificationsForComment(Long commentId) {
        notificationRepository.deleteByCommentId(commentId);
    }

    // Métodos privados de utilidad

    private boolean shouldSkipNotification(Long recipientId, Long actorId, Notification.NotificationType type, 
                                         Long postId, Long commentId) {
        return switch (type) {
            case POST_LIKE -> postId != null && 
                    notificationRepository.existsByRecipientIdAndActorIdAndTypeAndPostId(recipientId, actorId, type, postId);
            case COMMENT_LIKE -> commentId != null && 
                    notificationRepository.existsByRecipientIdAndActorIdAndTypeAndCommentId(recipientId, actorId, type, commentId);
            case FOLLOW, FOLLOW_REQUEST_APPROVED -> 
                    notificationRepository.existsByRecipientIdAndActorIdAndType(recipientId, actorId, type);
            case COMMENT -> false; // Permitir múltiples notificaciones de comentarios
        };
    }

    private void sendNotificationByWebSocket(Long userId, NotificationResponse notification) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, 
                new NotificationWebSocketMessage("NEW_NOTIFICATION", notification, null));
    }

    private void sendUnreadCountUpdate(Long userId) {
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, 
                new NotificationWebSocketMessage("UNREAD_COUNT_UPDATE", null, count));
    }

    private NotificationResponse convertToResponse(Notification notification) {
        // Convertir actor a SafeUser
        User actor = notification.getActor();
        SafeUser actorSafe = new SafeUser(
                actor.getId(),
                actor.getFullName(),
                actor.getUsername(),
                actor.getEmail(),
                actor.getCreatedAt()
        );

        // Extraer información del post si existe
        Long postId = null;
        String postImageUrl = null;
        if (notification.getPost() != null) {
            postId = notification.getPost().getId();
            postImageUrl = notification.getPost().getImage();
        }

        // Extraer información del comentario si existe
        Long commentId = null;
        String commentText = null;
        if (notification.getComment() != null) {
            commentId = notification.getComment().getId();
            commentText = truncateText(notification.getComment().getText(), 100);
        }

        return new NotificationResponse(
                notification.getId(),
                actorSafe,
                notification.getType(),
                postId,
                postImageUrl,
                commentId,
                commentText,
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // Clase interna para mensajes WebSocket
    public static class NotificationWebSocketMessage {
        private final String type;
        private final NotificationResponse notification;
        private final Long unreadCount;

        public NotificationWebSocketMessage(String type, NotificationResponse notification, Long unreadCount) {
            this.type = type;
            this.notification = notification;
            this.unreadCount = unreadCount;
        }

        // Getters
        public String getType() { return type; }
        public NotificationResponse getNotification() { return notification; }
        public Long getUnreadCount() { return unreadCount; }
    }
}