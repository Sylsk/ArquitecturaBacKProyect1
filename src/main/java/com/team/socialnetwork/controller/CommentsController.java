package com.team.socialnetwork.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.socialnetwork.dto.MessageResponse;
import com.team.socialnetwork.dto.SafeUser;
import com.team.socialnetwork.entity.Comment;
import com.team.socialnetwork.entity.CommentLike;
import com.team.socialnetwork.entity.User;
import com.team.socialnetwork.repository.CommentLikeRepository;
import com.team.socialnetwork.repository.CommentRepository;
import com.team.socialnetwork.repository.UserRepository;
import com.team.socialnetwork.service.NotificationService;

@RestController
@RequestMapping("/comments")
public class CommentsController {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationService notificationService;

    public CommentsController(CommentRepository commentRepository,
                              UserRepository userRepository,
                              CommentLikeRepository commentLikeRepository,
                              NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.notificationService = notificationService;
    }

    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(Authentication authentication,
                                                        @PathVariable Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));

        // Privacy gate: if the post author is private, only the author themselves or their followers can act
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        // Ownership: only the comment author can delete their own comment
        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }
        
        // Limpiar notificaciones relacionadas con este comentario antes de eliminarlo
        notificationService.cleanupNotificationsForComment(commentId);
        
        // Remove likes first to avoid FK constraint issues (no cascade mapping on comment likes from entities)
        commentLikeRepository.deleteByCommentId(commentId);
        commentRepository.delete(comment);
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully"));
    }

    @Transactional(readOnly = true)
    @GetMapping("/{commentId}/likes/count")
    public ResponseEntity<Map<String, Long>> countCommentLikes(Authentication authentication,
                                                               @PathVariable Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        long count = commentLikeRepository.countByCommentId(commentId);
        Map<String, Long> body = new HashMap<>();
        body.put("count", count);
        return ResponseEntity.ok(body);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{commentId}/likes")
    public ResponseEntity<List<SafeUser>> listCommentLikes(Authentication authentication,
                                                           @PathVariable Long commentId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        Page<CommentLike> likes = commentLikeRepository.findByCommentId(commentId, PageRequest.of(page, size));
        List<SafeUser> users = likes.getContent().stream().map(cl -> {
            User u = cl.getUser();
            return new SafeUser(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(users);
    }

    @Transactional
    @PostMapping("/{commentId}/likes")
    public ResponseEntity<MessageResponse> likeComment(Authentication authentication,
                                                      @PathVariable Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(user.getId()) && !user.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        if (commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Already liked");
        }
        commentLikeRepository.save(new CommentLike(user, comment));
        
        // Crear notificación para el autor del comentario (si no es el mismo que da like)
        User commentAuthor = comment.getAuthor();
        if (!commentAuthor.getId().equals(user.getId())) {
            notificationService.createAndSendNotification(
                    commentAuthor, user, 
                    com.team.socialnetwork.entity.Notification.NotificationType.COMMENT_LIKE, 
                    comment.getPost(), comment);
        }
        
        return ResponseEntity.ok(new MessageResponse("Comment liked successfully"));
    }

    @Transactional
    @DeleteMapping("/{commentId}/likes")
    public ResponseEntity<MessageResponse> unlikeComment(Authentication authentication,
                                                        @PathVariable Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        // 404 if comment doesn't exist
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(user.getId()) && !user.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        int deleted = commentLikeRepository.deleteByUserIdAndCommentId(user.getId(), commentId);
        if (deleted == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Not liked yet");
        }
        
        // Eliminar notificación de like en comentario si existe
        User commentAuthor = comment.getAuthor();
        if (!commentAuthor.getId().equals(user.getId())) {
            notificationService.removeCommentLikeNotification(
                    commentAuthor.getId(), user.getId(), commentId);
        }
        
        return ResponseEntity.ok(new MessageResponse("Comment unliked successfully"));
    }

    @GetMapping("/{commentId}/likes/check")
    public ResponseEntity<Map<String, Boolean>> checkCommentLike(Authentication authentication,
                                                                @PathVariable Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        
        // 404 if comment doesn't exist
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));
        
        // Check privacy permissions
        User postAuthor = comment.getPost().getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(user.getId()) && !user.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        boolean isLiked = commentLikeRepository.existsByUserIdAndCommentId(user.getId(), commentId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("liked", isLiked);
        return ResponseEntity.ok(response);
    }
}
