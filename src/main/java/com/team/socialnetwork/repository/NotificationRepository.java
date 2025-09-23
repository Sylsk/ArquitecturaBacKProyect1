package com.team.socialnetwork.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.team.socialnetwork.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Obtener notificaciones paginadas de un usuario ordenadas por fecha (más recientes primero)
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    
    // Obtener notificaciones de un usuario con límite (para WebSocket)
    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    
    // Contar notificaciones no leídas de un usuario
    long countByRecipientIdAndIsReadFalse(Long recipientId);
    
    // Marcar todas las notificaciones de un usuario como leídas
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);
    
    // Marcar una notificación específica como leída
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :recipientId")
    int markAsReadByIdAndRecipientId(@Param("notificationId") Long notificationId, @Param("recipientId") Long recipientId);
    
    // Verificar si existe una notificación específica para evitar duplicados
    boolean existsByRecipientIdAndActorIdAndTypeAndPostId(Long recipientId, Long actorId, 
                                                         Notification.NotificationType type, Long postId);
    
    // Verificar si existe una notificación específica para comentarios
    boolean existsByRecipientIdAndActorIdAndTypeAndCommentId(Long recipientId, Long actorId, 
                                                            Notification.NotificationType type, Long commentId);
    
    // Verificar si existe una notificación de follow
    boolean existsByRecipientIdAndActorIdAndType(Long recipientId, Long actorId, 
                                                 Notification.NotificationType type);
    
    // Eliminar notificaciones cuando se quita un like o follow
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.actor.id = :actorId " +
           "AND n.type = :type AND n.post.id = :postId")
    int deleteByRecipientIdAndActorIdAndTypeAndPostId(@Param("recipientId") Long recipientId, 
                                                     @Param("actorId") Long actorId,
                                                     @Param("type") Notification.NotificationType type, 
                                                     @Param("postId") Long postId);
    
    // Eliminar notificaciones de follow
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.actor.id = :actorId AND n.type = :type")
    int deleteByRecipientIdAndActorIdAndType(@Param("recipientId") Long recipientId, 
                                           @Param("actorId") Long actorId,
                                           @Param("type") Notification.NotificationType type);
    
    // Eliminar notificaciones de comment like
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.actor.id = :actorId " +
           "AND n.type = :type AND n.comment.id = :commentId")
    int deleteByRecipientIdAndActorIdAndTypeAndCommentId(@Param("recipientId") Long recipientId, 
                                                        @Param("actorId") Long actorId,
                                                        @Param("type") Notification.NotificationType type, 
                                                        @Param("commentId") Long commentId);
    
    // Eliminar notificaciones relacionadas a un post específico (cuando se elimina el post)
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.post.id = :postId")
    int deleteByPostId(@Param("postId") Long postId);
    
    // Eliminar notificaciones relacionadas a un comentario específico (cuando se elimina el comentario)
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.comment.id = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);
    
    // Limpiar notificaciones antiguas (opcional, para mantenimiento)
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteOldNotifications(@Param("threshold") java.time.Instant threshold);
}