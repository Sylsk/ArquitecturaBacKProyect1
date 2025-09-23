package com.team.socialnetwork.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.team.socialnetwork.entity.User;
import com.team.socialnetwork.repository.UserRepository;
import com.team.socialnetwork.service.NotificationService;

@Controller
public class NotificationWSController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWSController(NotificationService notificationService, 
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Endpoint para que el cliente se suscriba a notificaciones
     * El cliente envía un mensaje a /app/notifications/subscribe
     */
    @MessageMapping("/notifications/subscribe")
    public void subscribeToNotifications(Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Enviar contador actual de notificaciones no leídas
            var unreadCount = notificationService.getUnreadCount(user.getId());
            messagingTemplate.convertAndSend("/topic/notifications/" + user.getId(), 
                    new NotificationService.NotificationWebSocketMessage(
                            "INITIAL_UNREAD_COUNT", null, unreadCount.getUnreadCount()));

            System.out.println("Usuario " + user.getUsername() + " suscrito a notificaciones WebSocket");
        } catch (RuntimeException e) {
            System.err.println("Error al suscribir usuario a notificaciones: " + e.getMessage());
        }
    }

    /**
     * Endpoint para marcar todas las notificaciones como leídas via WebSocket
     */
    @MessageMapping("/notifications/mark-all-read")
    public void markAllAsRead(Principal principal) {
        if (principal == null) {
            return;
        }

        try {
            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            notificationService.markAllAsRead(user.getId());
            
            System.out.println("Usuario " + user.getUsername() + " marcó todas las notificaciones como leídas");
        } catch (RuntimeException e) {
            System.err.println("Error al marcar notificaciones como leídas: " + e.getMessage());
        }
    }
}