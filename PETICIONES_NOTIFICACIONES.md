# 📡 Guía Completa: Peticiones para Sistema de Notificaciones

## 🔗 1. CONEXIÓN WEBSOCKET

### Configuración Inicial del Cliente:
```javascript
// 1. Instalar dependencias
npm install sockjs-client stompjs

// 2. Importar librerías
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
```

### Establecer Conexión:
```javascript
// 1. Crear conexión SockJS
const socket = new SockJS('http://localhost:8080/notifications');
const client = Stomp.over(socket);

// 2. Headers con autenticación JWT (⚠️ OBLIGATORIO)
const headers = {
    'Authorization': 'Bearer ' + userToken
};

// 3. Conectar con autenticación
client.connect(headers, (frame) => {
    console.log('✅ Conectado a notificaciones:', frame);
    
    // 4. Suscribirse al canal personal del usuario
    client.subscribe(`/topic/notifications/${userId}`, (message) => {
        const data = JSON.parse(message.body);
        handleNotificationMessage(data);
    });
    
    // 5. Enviar mensaje de suscripción al servidor
    client.send('/app/notifications/subscribe', {}, '');
    
}, (error) => {
    console.error('❌ Error de conexión WebSocket:', error);
});
```

### Manejo de Errores de Conexión:
```javascript
// Reconexión automática
const connectWithRetry = () => {
    let retryCount = 0;
    const maxRetries = 5;
    
    const attemptConnection = () => {
        if (retryCount >= maxRetries) {
            console.error('❌ Máximo de reintentos alcanzado');
            return;
        }
        
        const socket = new SockJS('http://localhost:8080/notifications');
        const client = Stomp.over(socket);
        
        client.connect(headers, onConnectSuccess, (error) => {
            retryCount++;
            console.log(`🔄 Reintento ${retryCount}/${maxRetries}`);
            setTimeout(attemptConnection, 2000 * retryCount);
        });
    };
    
    attemptConnection();
};
```

---

## 📨 2. PETICIONES HTTP QUE GENERAN NOTIFICACIONES

### **Like a Post**

#### Dar Like:
```http
POST http://localhost:8080/posts/{postId}/likes
Headers:
    Authorization: Bearer {token}
    Content-Type: application/json

Response: 200 OK
{
    "message": "Like agregado exitosamente"
}
```

#### Quitar Like:
```http
DELETE http://localhost:8080/posts/{postId}/likes
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Like eliminado exitosamente"
}
```

#### Verificar Like:
```http
GET http://localhost:8080/posts/{postId}/likes/check
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "liked": true
}
```

#### Contar Likes:
```http
GET http://localhost:8080/posts/{postId}/likes/count
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "count": 25
}
```

### **Like a Comentario**

#### Dar Like:
```http
POST http://localhost:8080/comments/{commentId}/likes
Headers:
    Authorization: Bearer {token}
    Content-Type: application/json

Response: 200 OK
{
    "message": "Like en comentario agregado"
}
```

#### Quitar Like:
```http
DELETE http://localhost:8080/comments/{commentId}/likes
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Like en comentario eliminado"
}
```

### **Comentarios**

#### Crear Comentario:
```http
POST http://localhost:8080/posts/{postId}/comments
Headers:
    Authorization: Bearer {token}
    Content-Type: application/json

Body:
{
    "text": "¡Excelente post! Me encanta el contenido."
}

Response: 201 Created
{
    "id": 123,
    "text": "¡Excelente post! Me encanta el contenido.",
    "author": {
        "id": 456,
        "username": "juan_perez",
        "profilePicture": "http://..."
    },
    "post": {
        "id": 789
    },
    "likesCount": 0,
    "viewerLiked": false,
    "createdAt": "2024-01-01T10:00:00Z"
}
```

#### Obtener Comentarios de un Post:
```http
GET http://localhost:8080/posts/{postId}/comments?page=0&size=10
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "content": [
        {
            "id": 123,
            "text": "Gran post!",
            "author": {...},
            "likesCount": 5,
            "viewerLiked": true,
            "createdAt": "2024-01-01T10:00:00Z"
        }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "number": 0,
    "size": 10
}
```

#### Eliminar Comentario:
```http
DELETE http://localhost:8080/comments/{commentId}
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Comentario eliminado exitosamente"
}
```

### **Seguimientos**

#### Seguir Usuario:
```http
POST http://localhost:8080/users/{userId}/follow
Headers:
    Authorization: Bearer {token}
    Content-Type: application/json

Response: 200 OK
{
    "message": "Usuario seguido exitosamente"
}
```

#### Dejar de Seguir:
```http
DELETE http://localhost:8080/users/{userId}/follow
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Dejaste de seguir al usuario"
}
```

---

## 📥 3. MENSAJES WEBSOCKET QUE RECIBES

### **Nueva Notificación:**
```javascript
{
    "type": "NEW_NOTIFICATION",
    "notification": {
        "id": 123,
        "type": "POST_LIKE",
        "message": "juan_perez le gustó tu post",
        "actor": {
            "id": 456,
            "username": "juan_perez",
            "name": "Juan Pérez",
            "profilePicture": "http://example.com/profile.jpg"
        },
        "postId": 789,
        "commentId": null,
        "isRead": false,
        "createdAt": "2024-01-01T10:00:00Z"
    }
}
```

### **Contador de No Leídas Actualizado:**
```javascript
{
    "type": "UNREAD_COUNT_UPDATE",
    "unreadCount": 7
}
```

### **Contador Inicial al Conectarse:**
```javascript
{
    "type": "INITIAL_UNREAD_COUNT",
    "unreadCount": 3
}
```

### **Tipos de Notificaciones:**
```javascript
// Tipos disponibles:
"POST_LIKE"               // Alguien le dio like a tu post
"COMMENT_LIKE"           // Alguien le dio like a tu comentario  
"COMMENT"                // Alguien comentó tu post
"FOLLOW"                 // Alguien te siguió
"FOLLOW_REQUEST_APPROVED" // Aceptaron tu solicitud de seguimiento
```

---

## 🔍 4. GESTIÓN DE NOTIFICACIONES

### **Obtener Lista de Notificaciones:**
```http
GET http://localhost:8080/notifications?page=0&size=20&sort=createdAt,desc
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "content": [
        {
            "id": 123,
            "type": "POST_LIKE",
            "message": "maria_garcia le gustó tu post",
            "actor": {
                "id": 789,
                "username": "maria_garcia",
                "name": "María García",
                "profilePicture": "http://..."
            },
            "postId": 456,
            "commentId": null,
            "isRead": false,
            "createdAt": "2024-01-01T15:30:00Z"
        }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
}
```

### **Contar Notificaciones No Leídas:**
```http
GET http://localhost:8080/notifications/unread-count
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "unreadCount": 7
}
```

### **Marcar Todas como Leídas:**
```http
PATCH http://localhost:8080/notifications/mark-all-read
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Todas las notificaciones marcadas como leídas",
    "updatedCount": 7
}
```

### **Eliminar Notificaciones Antiguas:**
```http
DELETE http://localhost:8080/notifications/cleanup?days=30
Headers:
    Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Notificaciones eliminadas",
    "deletedCount": 15
}
```

---

## ⚡ 5. IMPLEMENTACIÓN COMPLETA EN JAVASCRIPT

### Clase Principal de Notificaciones:
```javascript
class NotificationService {
    constructor(token, userId, baseUrl = 'http://localhost:8080') {
        this.token = token;
        this.userId = userId;
        this.baseUrl = baseUrl;
        this.stompClient = null;
        this.notifications = [];
        this.unreadCount = 0;
        this.listeners = [];
    }

    // === WEBSOCKET CONNECTION ===
    
    connect() {
        return new Promise((resolve, reject) => {
            const socket = new SockJS(`${this.baseUrl}/notifications`);
            this.stompClient = Stomp.over(socket);
            
            const headers = { 'Authorization': `Bearer ${this.token}` };

            this.stompClient.connect(headers, (frame) => {
                console.log('✅ Conectado a notificaciones');
                
                // Suscribirse a notificaciones personales
                this.stompClient.subscribe(`/topic/notifications/${this.userId}`, (message) => {
                    this.handleMessage(JSON.parse(message.body));
                });
                
                // Enviar suscripción
                this.stompClient.send('/app/notifications/subscribe', {}, '');
                
                resolve(frame);
            }, (error) => {
                console.error('❌ Error de conexión:', error);
                reject(error);
            });
        });
    }

    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
            console.log('🔌 Desconectado de notificaciones');
        }
    }

    // === MESSAGE HANDLING ===
    
    handleMessage(data) {
        switch(data.type) {
            case 'NEW_NOTIFICATION':
                this.notifications.unshift(data.notification);
                this.unreadCount++;
                this.showToast(data.notification);
                this.notifyListeners('newNotification', data.notification);
                break;
                
            case 'UNREAD_COUNT_UPDATE':
                this.unreadCount = data.unreadCount;
                this.notifyListeners('unreadCountUpdate', data.unreadCount);
                break;
                
            case 'INITIAL_UNREAD_COUNT':
                this.unreadCount = data.unreadCount;
                this.notifyListeners('initialUnreadCount', data.unreadCount);
                break;
        }
    }

    showToast(notification) {
        const messages = {
            'POST_LIKE': `${notification.actor.username} le gustó tu post`,
            'COMMENT_LIKE': `${notification.actor.username} le gustó tu comentario`,
            'COMMENT': `${notification.actor.username} comentó tu post`,
            'FOLLOW': `${notification.actor.username} te siguió`,
            'FOLLOW_REQUEST_APPROVED': `${notification.actor.username} aceptó tu solicitud`
        };
        
        const message = messages[notification.type] || 'Nueva notificación';
        console.log('🔔', message);
        
        // Integrar con sistema de toast (react-toastify, etc.)
        if (window.showToast) {
            window.showToast(message, 'info');
        }
    }

    // === HTTP REQUESTS ===
    
    async makeRequest(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        };
        
        const response = await fetch(`${this.baseUrl}${url}`, {
            ...defaultOptions,
            ...options
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        return response.json();
    }

    // === POSTS ACTIONS ===
    
    async likePost(postId) {
        try {
            await this.makeRequest(`/posts/${postId}/likes`, { method: 'POST' });
            return true;
        } catch (error) {
            console.error('Error al dar like al post:', error);
            throw error;
        }
    }

    async unlikePost(postId) {
        try {
            await this.makeRequest(`/posts/${postId}/likes`, { method: 'DELETE' });
            return true;
        } catch (error) {
            console.error('Error al quitar like del post:', error);
            throw error;
        }
    }

    async checkPostLike(postId) {
        try {
            const result = await this.makeRequest(`/posts/${postId}/likes/check`);
            return result.liked;
        } catch (error) {
            console.error('Error al verificar like del post:', error);
            return false;
        }
    }

    async getPostLikesCount(postId) {
        try {
            const result = await this.makeRequest(`/posts/${postId}/likes/count`);
            return result.count;
        } catch (error) {
            console.error('Error al obtener conteo de likes:', error);
            return 0;
        }
    }

    // === COMMENTS ACTIONS ===
    
    async commentPost(postId, text) {
        try {
            const result = await this.makeRequest(`/posts/${postId}/comments`, {
                method: 'POST',
                body: JSON.stringify({ text })
            });
            return result;
        } catch (error) {
            console.error('Error al comentar post:', error);
            throw error;
        }
    }

    async likeComment(commentId) {
        try {
            await this.makeRequest(`/comments/${commentId}/likes`, { method: 'POST' });
            return true;
        } catch (error) {
            console.error('Error al dar like al comentario:', error);
            throw error;
        }
    }

    async unlikeComment(commentId) {
        try {
            await this.makeRequest(`/comments/${commentId}/likes`, { method: 'DELETE' });
            return true;
        } catch (error) {
            console.error('Error al quitar like del comentario:', error);
            throw error;
        }
    }

    async deleteComment(commentId) {
        try {
            await this.makeRequest(`/comments/${commentId}`, { method: 'DELETE' });
            return true;
        } catch (error) {
            console.error('Error al eliminar comentario:', error);
            throw error;
        }
    }

    async getPostComments(postId, page = 0, size = 10) {
        try {
            const result = await this.makeRequest(`/posts/${postId}/comments?page=${page}&size=${size}`);
            return result;
        } catch (error) {
            console.error('Error al obtener comentarios:', error);
            throw error;
        }
    }

    // === FOLLOW ACTIONS ===
    
    async followUser(userId) {
        try {
            await this.makeRequest(`/users/${userId}/follow`, { method: 'POST' });
            return true;
        } catch (error) {
            console.error('Error al seguir usuario:', error);
            throw error;
        }
    }

    async unfollowUser(userId) {
        try {
            await this.makeRequest(`/users/${userId}/follow`, { method: 'DELETE' });
            return true;
        } catch (error) {
            console.error('Error al dejar de seguir usuario:', error);
            throw error;
        }
    }

    // === NOTIFICATIONS MANAGEMENT ===
    
    async getNotifications(page = 0, size = 20) {
        try {
            const result = await this.makeRequest(`/notifications?page=${page}&size=${size}&sort=createdAt,desc`);
            return result;
        } catch (error) {
            console.error('Error al obtener notificaciones:', error);
            throw error;
        }
    }

    async getUnreadCount() {
        try {
            const result = await this.makeRequest('/notifications/unread-count');
            return result.unreadCount;
        } catch (error) {
            console.error('Error al obtener conteo de no leídas:', error);
            return 0;
        }
    }

    async markAllAsRead() {
        try {
            const result = await this.makeRequest('/notifications/mark-all-read', { method: 'PATCH' });
            this.unreadCount = 0;
            this.notifyListeners('unreadCountUpdate', 0);
            return result;
        } catch (error) {
            console.error('Error al marcar como leídas:', error);
            throw error;
        }
    }

    async cleanupOldNotifications(days = 30) {
        try {
            const result = await this.makeRequest(`/notifications/cleanup?days=${days}`, { method: 'DELETE' });
            return result;
        } catch (error) {
            console.error('Error al limpiar notificaciones:', error);
            throw error;
        }
    }

    // === EVENT LISTENERS ===
    
    addListener(event, callback) {
        this.listeners.push({ event, callback });
    }

    removeListener(event, callback) {
        this.listeners = this.listeners.filter(l => l.event !== event || l.callback !== callback);
    }

    notifyListeners(event, data) {
        this.listeners
            .filter(l => l.event === event)
            .forEach(l => l.callback(data));
    }

    // === GETTERS ===
    
    getNotifications() {
        return this.notifications;
    }

    getUnreadCountLocal() {
        return this.unreadCount;
    }

    isConnected() {
        return this.stompClient && this.stompClient.connected;
    }
}
```

### Uso de la Clase:
```javascript
// Inicializar servicio
const notificationService = new NotificationService(userToken, userId);

// Conectar
await notificationService.connect();

// Agregar listeners
notificationService.addListener('newNotification', (notification) => {
    console.log('Nueva notificación:', notification);
    updateNotificationUI(notification);
});

notificationService.addListener('unreadCountUpdate', (count) => {
    console.log('Contador actualizado:', count);
    updateBadgeCount(count);
});

// Usar las funciones
await notificationService.likePost(123);
await notificationService.commentPost(123, '¡Excelente!');
await notificationService.followUser(456);

// Gestionar notificaciones
const notifications = await notificationService.getNotifications(0, 10);
await notificationService.markAllAsRead();

// Desconectar al salir
window.addEventListener('beforeunload', () => {
    notificationService.disconnect();
});
```

---

## 🎯 6. EJEMPLO DE INTEGRACIÓN CON REACT

### Hook personalizado:
```javascript
// hooks/useNotifications.js
import { useState, useEffect, useRef } from 'react';

export const useNotifications = (token, userId) => {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [connected, setConnected] = useState(false);
    const serviceRef = useRef(null);

    useEffect(() => {
        if (!token || !userId) return;

        const service = new NotificationService(token, userId);
        serviceRef.current = service;

        // Configurar listeners
        service.addListener('newNotification', (notification) => {
            setNotifications(prev => [notification, ...prev]);
            setUnreadCount(prev => prev + 1);
        });

        service.addListener('unreadCountUpdate', (count) => {
            setUnreadCount(count);
        });

        service.addListener('initialUnreadCount', (count) => {
            setUnreadCount(count);
        });

        // Conectar
        service.connect()
            .then(() => setConnected(true))
            .catch(error => {
                console.error('Error al conectar notificaciones:', error);
                setConnected(false);
            });

        // Cleanup
        return () => {
            service.disconnect();
            setConnected(false);
        };
    }, [token, userId]);

    const likePost = async (postId) => {
        return serviceRef.current?.likePost(postId);
    };

    const commentPost = async (postId, text) => {
        return serviceRef.current?.commentPost(postId, text);
    };

    const markAllAsRead = async () => {
        return serviceRef.current?.markAllAsRead();
    };

    return {
        notifications,
        unreadCount,
        connected,
        likePost,
        commentPost,
        markAllAsRead,
        service: serviceRef.current
    };
};
```

### Componente React:
```jsx
// components/NotificationProvider.jsx
import React, { createContext, useContext } from 'react';
import { useNotifications } from '../hooks/useNotifications';

const NotificationContext = createContext();

export const useNotificationContext = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotificationContext debe usarse dentro de NotificationProvider');
    }
    return context;
};

export const NotificationProvider = ({ children, token, userId }) => {
    const notificationData = useNotifications(token, userId);

    return (
        <NotificationContext.Provider value={notificationData}>
            {children}
        </NotificationContext.Provider>
    );
};
```

---

## 🚨 7. MANEJO DE ERRORES Y CASOS ESPECIALES

### Errores Comunes y Soluciones:

#### **Error 401 - Unauthorized:**
```javascript
// Token expirado o inválido
if (error.status === 401) {
    // Redirigir a login
    window.location.href = '/login';
}
```

#### **Error 403 - Forbidden:**
```javascript
// Sin permisos para la acción
if (error.status === 403) {
    showError('No tienes permisos para realizar esta acción');
}
```

#### **Error de Conexión WebSocket:**
```javascript
// Reconexión automática
const reconnectWebSocket = () => {
    setTimeout(() => {
        console.log('🔄 Intentando reconectar...');
        notificationService.connect();
    }, 3000);
};
```

#### **Validaciones del Frontend:**
```javascript
// Validar antes de enviar
const validateComment = (text) => {
    if (!text || text.trim().length === 0) {
        throw new Error('El comentario no puede estar vacío');
    }
    if (text.length > 1000) {
        throw new Error('El comentario es demasiado largo');
    }
    return text.trim();
};

const validatePostId = (postId) => {
    if (!postId || isNaN(postId)) {
        throw new Error('ID de post inválido');
    }
    return parseInt(postId);
};
```

---

## ✅ 8. CHECKLIST DE IMPLEMENTACIÓN

### **Backend (Ya implementado):**
- [x] Endpoints de likes en posts
- [x] Endpoints de likes en comentarios
- [x] Endpoints de comentarios
- [x] Endpoints de seguimientos
- [x] Sistema de notificaciones WebSocket
- [x] Autenticación JWT en WebSocket
- [x] Prevención de auto-notificaciones
- [x] Limpieza automática de notificaciones

### **Frontend (Por implementar):**
- [ ] Instalar dependencias (sockjs-client, stompjs)
- [ ] Crear clase NotificationService
- [ ] Implementar hooks de React
- [ ] Crear contexto de notificaciones
- [ ] Componentes de UI para notificaciones
- [ ] Sistema de toast/alertas
- [ ] Manejo de errores
- [ ] Reconexión automática
- [ ] Persistencia local (localStorage)
- [ ] Optimizaciones de rendimiento

### **Testing:**
- [ ] Conexión WebSocket exitosa
- [ ] Autenticación JWT correcta
- [ ] Like a post → Notificación recibida
- [ ] Unlike post → Notificación eliminada
- [ ] Comentar post → Notificación al autor
- [ ] Like a comentario → Notificación al autor
- [ ] Seguir usuario → Notificación recibida
- [ ] Marcar como leídas → Contador actualizado
- [ ] Reconexión tras pérdida de conexión
- [ ] Manejo de errores HTTP

---

## 🎯 9. RESUMEN EJECUTIVO

### **Flujo Principal:**
1. **Conectar WebSocket** con token JWT
2. **Suscribirse** al canal personal `/topic/notifications/{userId}`
3. **Realizar acciones** (like, comment, follow) vía HTTP
4. **Recibir notificaciones** automáticamente por WebSocket
5. **Actualizar UI** en tiempo real

### **Puntos Críticos:**
- ✅ **Token JWT obligatorio** en todas las peticiones
- ✅ **UserId correcto** en suscripción WebSocket
- ✅ **Conectar antes** de realizar acciones
- ✅ **Manejo de errores** y reconexión
- ✅ **UI responsiva** con estados optimistas

### **Beneficios del Sistema:**
- 🚀 **Tiempo real**: Notificaciones instantáneas
- 🔒 **Seguridad**: Autenticación JWT completa
- ⚡ **Performance**: WebSocket eficiente
- 🎯 **Escalabilidad**: Sistema modular y extensible
- 🛡️ **Robustez**: Manejo completo de errores

Este documento contiene toda la información necesaria para implementar exitosamente el sistema de notificaciones. ¡Cada petición está documentada con ejemplos completos y casos de uso reales!