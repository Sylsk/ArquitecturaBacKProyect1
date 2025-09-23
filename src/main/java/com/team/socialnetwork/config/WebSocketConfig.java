package com.team.socialnetwork.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.team.socialnetwork.security.JwtService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Crear interceptor común para autenticación JWT
        HandshakeInterceptor jwtInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) throws Exception {
                
                System.out.println("🔍 WebSocket handshake attempt from: " + request.getRemoteAddress());
                System.out.println("🔍 URI: " + request.getURI());
                
                String token = null;
                
                // Método 1: Buscar en header Authorization
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                    System.out.println("🔍 Token encontrado en header Authorization");
                } 
                // Método 2: Buscar en query parameter
                else {
                    String query = request.getURI().getQuery();
                    if (query != null && query.contains("token=")) {
                        String[] pairs = query.split("&");
                        for (String pair : pairs) {
                            String[] keyValue = pair.split("=");
                            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                                token = keyValue[1];
                                System.out.println("🔍 Token encontrado en query parameter");
                                break;
                            }
                        }
                    }
                }
                
                if (token != null) {
                    try {
                        String email = jwtService.extractSubject(token);
                        if (jwtService.isTokenValid(token, email)) {
                            // Crear Principal con el email del usuario autenticado
                            Principal userPrincipal = () -> email;
                            attributes.put("principal", userPrincipal);
                            System.out.println("✅ WebSocket authenticated for user: " + email);
                            return true;
                        } else {
                            System.err.println("❌ Invalid token for user: " + email);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ JWT validation failed: " + e.getMessage());
                        return false;
                    }
                } else {
                    System.err.println("❌ No token found in Authorization header or query parameter");
                }
                return false; // Rechazar conexión sin token válido
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler,
                                       Exception ex) {
                // no hace nada
            }
        };

        // Endpoint para chat
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtInterceptor)
                .withSockJS();
        
        // Endpoint para notificaciones
        registry.addEndpoint("/notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/user");
    }
}
