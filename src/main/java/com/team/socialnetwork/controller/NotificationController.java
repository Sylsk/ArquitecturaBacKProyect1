package com.team.socialnetwork.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.socialnetwork.dto.MessageResponse;
import com.team.socialnetwork.dto.NotificationCountResponse;
import com.team.socialnetwork.dto.NotificationResponse;
import com.team.socialnetwork.entity.User;
import com.team.socialnetwork.repository.UserRepository;
import com.team.socialnetwork.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Obtener notificaciones paginadas del usuario autenticado
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        User user = getUserFromAuth(authentication);
        
        // Validar tamaño de página
        if (size > 50) {
            size = 50; // Limitar el tamaño máximo para rendimiento
        }

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                user.getId(), page, size);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Obtener contador de notificaciones no leídas
     */
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        User user = getUserFromAuth(authentication);
        NotificationCountResponse count = notificationService.getUnreadCount(user.getId());

        return ResponseEntity.ok(count);
    }

    /**
     * Marcar todas las notificaciones como leídas
     */
    @PatchMapping("/mark-all-read")
    public ResponseEntity<MessageResponse> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        User user = getUserFromAuth(authentication);
        int updated = notificationService.markAllAsRead(user.getId());

        return ResponseEntity.ok(new MessageResponse(updated + " notifications marked as read"));
    }

    /**
     * Marcar una notificación específica como leída
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<MessageResponse> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        User user = getUserFromAuth(authentication);
        boolean success = notificationService.markAsRead(user.getId(), notificationId);

        if (!success) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Notification not found");
        }

        return ResponseEntity.ok(new MessageResponse("Notification marked as read"));
    }

    // Método auxiliar para obtener usuario autenticado
    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
    }
}