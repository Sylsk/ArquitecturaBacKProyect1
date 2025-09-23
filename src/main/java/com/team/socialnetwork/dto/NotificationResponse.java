package com.team.socialnetwork.dto;

import java.time.Instant;

import com.team.socialnetwork.entity.Notification;

public class NotificationResponse {
    private Long id;
    private SafeUser actor; // Usuario que realizó la acción
    private Notification.NotificationType type;
    private Long postId; // ID del post (si aplica)
    private String postImageUrl; // URL de imagen del post (si aplica)
    private Long commentId; // ID del comentario (si aplica)
    private String commentText; // Texto del comentario (si aplica, truncado)
    private boolean isRead;
    private Instant createdAt;

    public NotificationResponse() {}

    public NotificationResponse(Long id, SafeUser actor, Notification.NotificationType type, 
                               Long postId, String postImageUrl, Long commentId, String commentText,
                               boolean isRead, Instant createdAt) {
        this.id = id;
        this.actor = actor;
        this.type = type;
        this.postId = postId;
        this.postImageUrl = postImageUrl;
        this.commentId = commentId;
        this.commentText = commentText;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SafeUser getActor() { return actor; }
    public void setActor(SafeUser actor) { this.actor = actor; }

    public Notification.NotificationType getType() { return type; }
    public void setType(Notification.NotificationType type) { this.type = type; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getPostImageUrl() { return postImageUrl; }
    public void setPostImageUrl(String postImageUrl) { this.postImageUrl = postImageUrl; }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}