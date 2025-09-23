package com.team.socialnetwork.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at DESC"),
                @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read")
        })
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_recipient"))
    private User recipient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_actor"))
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_notification_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", foreignKey = @ForeignKey(name = "fk_notification_comment"))
    private Comment comment;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean not null default false")
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructores
    public Notification() {}

    public Notification(User recipient, User actor, NotificationType type) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
    }

    public Notification(User recipient, User actor, NotificationType type, Post post) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.post = post;
    }

    public Notification(User recipient, User actor, NotificationType type, Post post, Comment comment) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.post = post;
        this.comment = comment;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Enum para tipos de notificaciones
    public enum NotificationType {
        FOLLOW,
        POST_LIKE,
        COMMENT_LIKE,
        COMMENT,
        FOLLOW_REQUEST_APPROVED
    }
}