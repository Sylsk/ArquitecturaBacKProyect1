# 📝 Lógica Completa: Likes y Comentarios con Notificaciones

## 🎯 Flujo General de Implementación

### 1. **LIKE A POST**

#### Frontend:
```javascript
const handlePostLike = async (postId, isCurrentlyLiked) => {
    try {
        // 1. Actualizar UI optimísticamente
        setIsLiked(!isCurrentlyLiked);
        setLikesCount(prev => isCurrentlyLiked ? prev - 1 : prev + 1);
        
        // 2. Llamar API
        if (isCurrentlyLiked) {
            await api.delete(`/posts/${postId}/likes`);
        } else {
            await api.post(`/posts/${postId}/likes`);
        }
        
        // 3. La notificación llegará automáticamente por WebSocket
        
    } catch (error) {
        // Revertir UI en caso de error
        setIsLiked(isCurrentlyLiked);
        setLikesCount(prev => isCurrentlyLiked ? prev + 1 : prev - 1);
    }
};
```

#### Backend (ya implementado):
1. `POST /posts/{id}/likes` → Guarda like + Envía notificación
2. `DELETE /posts/{id}/likes` → Elimina like + Elimina notificación
3. Notificación automática por WebSocket al autor del post

---

### 2. **LIKE A COMENTARIO**

#### Frontend:
```javascript
const handleCommentLike = async (commentId, isCurrentlyLiked) => {
    try {
        // 1. Actualizar UI optimísticamente
        setCommentLiked(!isCurrentlyLiked);
        setCommentLikesCount(prev => isCurrentlyLiked ? prev - 1 : prev + 1);
        
        // 2. Llamar API
        if (isCurrentlyLiked) {
            await api.delete(`/comments/${commentId}/likes`);
        } else {
            await api.post(`/comments/${commentId}/likes`);
        }
        
        // 3. La notificación llegará automáticamente por WebSocket
        
    } catch (error) {
        // Revertir UI en caso de error
        setCommentLiked(isCurrentlyLiked);
        setCommentLikesCount(prev => isCurrentlyLiked ? prev + 1 : prev - 1);
    }
};
```

#### Backend (ya implementado):
1. `POST /comments/{id}/likes` → Guarda like + Envía notificación
2. `DELETE /comments/{id}/likes` → Elimina like + Elimina notificación  
3. Notificación automática por WebSocket al autor del comentario

---

### 3. **COMENTAR EN POST**

#### Frontend:
```javascript
const handleCreateComment = async (postId, commentText) => {
    try {
        // 1. Validar texto
        if (!commentText.trim()) return;
        
        // 2. Llamar API
        const response = await api.post(`/posts/${postId}/comments`, {
            text: commentText.trim()
        });
        
        // 3. Actualizar lista de comentarios localmente
        setComments(prev => [...prev, response.data]);
        setCommentsCount(prev => prev + 1);
        
        // 4. Limpiar input
        setCommentText('');
        
        // 5. La notificación llegará automáticamente por WebSocket al autor del post
        
    } catch (error) {
        console.error('Error al crear comentario:', error);
    }
};
```

#### Backend (ya implementado):
1. `POST /posts/{id}/comments` → Guarda comentario + Envía notificación
2. Notificación automática por WebSocket al autor del post

---

## 🔄 Componentes Frontend Completos

### Hook para Notificaciones:
```javascript
// hooks/useNotifications.js
import { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

export const useNotifications = (userId, token) => {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const stompClientRef = useRef(null);

    useEffect(() => {
        if (!userId || !token) return;

        const socket = new SockJS('http://localhost:8080/notifications');
        const client = Stomp.over(socket);
        
        const headers = { 'Authorization': `Bearer ${token}` };

        client.connect(headers, (frame) => {
            client.subscribe(`/topic/notifications/${userId}`, (message) => {
                const data = JSON.parse(message.body);
                handleNotificationMessage(data);
            });
            
            client.send('/app/notifications/subscribe', {}, '');
            stompClientRef.current = client;
        });

        return () => client.connected && client.disconnect();
    }, [userId, token]);

    const handleNotificationMessage = (data) => {
        switch(data.type) {
            case 'NEW_NOTIFICATION':
                setNotifications(prev => [data.notification, ...prev]);
                setUnreadCount(prev => prev + 1);
                showToast(data.notification);
                break;
            case 'UNREAD_COUNT_UPDATE':
                setUnreadCount(data.unreadCount);
                break;
            case 'INITIAL_UNREAD_COUNT':
                setUnreadCount(data.unreadCount);
                break;
        }
    };

    const showToast = (notification) => {
        const messages = {
            'POST_LIKE': `${notification.actor.username} le gustó tu post`,
            'COMMENT_LIKE': `${notification.actor.username} le gustó tu comentario`,
            'COMMENT': `${notification.actor.username} comentó tu post`,
            'FOLLOW': `${notification.actor.username} te siguió`
        };
        
        console.log('🔔', messages[notification.type]);
        // Aquí integrar con tu sistema de toast (react-toastify, etc.)
    };

    return { notifications, unreadCount, stompClient: stompClientRef.current };
};
```

### Hook para Likes:
```javascript
// hooks/useLikes.js
import { useState } from 'react';
import { api } from '../services/api';

export const useLikes = () => {
    const [loading, setLoading] = useState(false);

    const togglePostLike = async (postId, isLiked) => {
        setLoading(true);
        try {
            if (isLiked) {
                await api.delete(`/posts/${postId}/likes`);
                return false;
            } else {
                await api.post(`/posts/${postId}/likes`);
                return true;
            }
        } finally {
            setLoading(false);
        }
    };

    const toggleCommentLike = async (commentId, isLiked) => {
        setLoading(true);
        try {
            if (isLiked) {
                await api.delete(`/comments/${commentId}/likes`);
                return false;
            } else {
                await api.post(`/comments/${commentId}/likes`);
                return true;
            }
        } finally {
            setLoading(false);
        }
    };

    return { togglePostLike, toggleCommentLike, loading };
};
```

### Hook para Comentarios:
```javascript
// hooks/useComments.js
import { useState } from 'react';
import { api } from '../services/api';

export const useComments = () => {
    const [loading, setLoading] = useState(false);

    const createComment = async (postId, text) => {
        setLoading(true);
        try {
            const response = await api.post(`/posts/${postId}/comments`, { text });
            return response.data;
        } finally {
            setLoading(false);
        }
    };

    const deleteComment = async (commentId) => {
        setLoading(true);
        try {
            await api.delete(`/comments/${commentId}`);
            return true;
        } finally {
            setLoading(false);
        }
    };

    return { createComment, deleteComment, loading };
};
```

### Componente Post Completo:
```jsx
// components/PostCard.jsx
import React, { useState } from 'react';
import { useLikes } from '../hooks/useLikes';
import { useComments } from '../hooks/useComments';
import CommentsList from './CommentsList';

const PostCard = ({ post, currentUser, onPostUpdate }) => {
    // Estados del post
    const [isLiked, setIsLiked] = useState(post.viewerLiked);
    const [likesCount, setLikesCount] = useState(post.likesCount);
    const [commentsCount, setCommentsCount] = useState(post.commentsCount);
    const [showComments, setShowComments] = useState(false);
    const [comments, setComments] = useState([]);
    
    // Estados del formulario de comentario
    const [commentText, setCommentText] = useState('');
    const [showCommentForm, setShowCommentForm] = useState(false);
    
    // Hooks
    const { togglePostLike, loading: likeLoading } = useLikes();
    const { createComment, loading: commentLoading } = useComments();

    const handleLikeClick = async () => {
        try {
            const newLikedState = await togglePostLike(post.id, isLiked);
            setIsLiked(newLikedState);
            setLikesCount(prev => newLikedState ? prev + 1 : prev - 1);
        } catch (error) {
            console.error('Error al dar like:', error);
        }
    };

    const handleCommentSubmit = async (e) => {
        e.preventDefault();
        if (!commentText.trim()) return;

        try {
            const newComment = await createComment(post.id, commentText);
            setComments(prev => [...prev, newComment]);
            setCommentsCount(prev => prev + 1);
            setCommentText('');
            setShowCommentForm(false);
        } catch (error) {
            console.error('Error al comentar:', error);
        }
    };

    const loadComments = async () => {
        if (comments.length === 0) {
            try {
                const response = await api.get(`/posts/${post.id}/comments`);
                setComments(response.data);
            } catch (error) {
                console.error('Error al cargar comentarios:', error);
            }
        }
        setShowComments(!showComments);
    };

    return (
        <div className="post-card">
            {/* Header del post */}
            <div className="post-header">
                <img src={post.author.profilePicture} alt={post.author.username} />
                <div>
                    <h4>{post.author.username}</h4>
                    <small>{new Date(post.createdAt).toLocaleDateString()}</small>
                </div>
            </div>

            {/* Contenido del post */}
            <div className="post-content">
                {post.description && <p>{post.description}</p>}
                {post.image && <img src={post.image} alt="Post" />}
            </div>

            {/* Acciones del post */}
            <div className="post-actions">
                <button 
                    onClick={handleLikeClick}
                    disabled={likeLoading}
                    className={`like-btn ${isLiked ? 'liked' : ''}`}
                >
                    {isLiked ? '❤️' : '🤍'} {likesCount}
                </button>
                
                <button onClick={loadComments} className="comment-btn">
                    💬 {commentsCount}
                </button>
                
                <button 
                    onClick={() => setShowCommentForm(!showCommentForm)}
                    className="add-comment-btn"
                >
                    ✏️ Comentar
                </button>
            </div>

            {/* Formulario de comentario */}
            {showCommentForm && (
                <form onSubmit={handleCommentSubmit} className="comment-form">
                    <textarea
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        placeholder="Escribe un comentario..."
                        maxLength={1000}
                    />
                    <div className="comment-form-actions">
                        <button 
                            type="submit" 
                            disabled={commentLoading || !commentText.trim()}
                        >
                            {commentLoading ? 'Enviando...' : 'Comentar'}
                        </button>
                        <button 
                            type="button"
                            onClick={() => setShowCommentForm(false)}
                        >
                            Cancelar
                        </button>
                    </div>
                </form>
            )}

            {/* Lista de comentarios */}
            {showComments && (
                <CommentsList 
                    comments={comments}
                    currentUser={currentUser}
                    onCommentsUpdate={setComments}
                />
            )}
        </div>
    );
};

export default PostCard;
```

### Componente Lista de Comentarios:
```jsx
// components/CommentsList.jsx
import React from 'react';
import CommentItem from './CommentItem';

const CommentsList = ({ comments, currentUser, onCommentsUpdate }) => {
    const handleCommentUpdate = (commentId, updatedComment) => {
        onCommentsUpdate(prev => 
            prev.map(comment => 
                comment.id === commentId ? updatedComment : comment
            )
        );
    };

    const handleCommentDelete = (commentId) => {
        onCommentsUpdate(prev => 
            prev.filter(comment => comment.id !== commentId)
        );
    };

    return (
        <div className="comments-list">
            {comments.length === 0 ? (
                <p className="no-comments">No hay comentarios aún</p>
            ) : (
                comments.map(comment => (
                    <CommentItem
                        key={comment.id}
                        comment={comment}
                        currentUser={currentUser}
                        onUpdate={handleCommentUpdate}
                        onDelete={handleCommentDelete}
                    />
                ))
            )}
        </div>
    );
};

export default CommentsList;
```

### Componente Comentario Individual:
```jsx
// components/CommentItem.jsx
import React, { useState } from 'react';
import { useLikes } from '../hooks/useLikes';
import { useComments } from '../hooks/useComments';

const CommentItem = ({ comment, currentUser, onUpdate, onDelete }) => {
    const [isLiked, setIsLiked] = useState(comment.viewerLiked);
    const [likesCount, setLikesCount] = useState(comment.likesCount);
    
    const { toggleCommentLike, loading: likeLoading } = useLikes();
    const { deleteComment, loading: deleteLoading } = useComments();

    const handleLikeClick = async () => {
        try {
            const newLikedState = await toggleCommentLike(comment.id, isLiked);
            setIsLiked(newLikedState);
            setLikesCount(prev => newLikedState ? prev + 1 : prev - 1);
        } catch (error) {
            console.error('Error al dar like al comentario:', error);
        }
    };

    const handleDeleteClick = async () => {
        if (!window.confirm('¿Estás seguro de eliminar este comentario?')) return;
        
        try {
            await deleteComment(comment.id);
            onDelete(comment.id);
        } catch (error) {
            console.error('Error al eliminar comentario:', error);
        }
    };

    return (
        <div className="comment-item">
            <div className="comment-header">
                <img src={comment.author.profilePicture} alt={comment.author.username} />
                <div>
                    <strong>{comment.author.username}</strong>
                    <small>{new Date(comment.createdAt).toLocaleDateString()}</small>
                </div>
            </div>
            
            <div className="comment-content">
                <p>{comment.text}</p>
            </div>
            
            <div className="comment-actions">
                <button 
                    onClick={handleLikeClick}
                    disabled={likeLoading}
                    className={`like-btn ${isLiked ? 'liked' : ''}`}
                >
                    {isLiked ? '❤️' : '🤍'} {likesCount}
                </button>
                
                {currentUser.id === comment.author.id && (
                    <button 
                        onClick={handleDeleteClick}
                        disabled={deleteLoading}
                        className="delete-btn"
                    >
                        🗑️ Eliminar
                    </button>
                )}
            </div>
        </div>
    );
};

export default CommentItem;
```

## 🚀 Endpoints del Backend (ya implementados)

### Posts:
- `POST /posts/{id}/likes` - Dar like a post
- `DELETE /posts/{id}/likes` - Quitar like a post  
- `GET /posts/{id}/likes/check` - Verificar si le diste like
- `GET /posts/{id}/likes/count` - Contar likes del post

### Comentarios:
- `POST /posts/{id}/comments` - Crear comentario
- `DELETE /comments/{id}` - Eliminar comentario
- `POST /comments/{id}/likes` - Dar like a comentario
- `DELETE /comments/{id}/likes` - Quitar like a comentario
- `GET /comments/{id}/likes/check` - Verificar like en comentario

### Notificaciones:
- `GET /notifications` - Obtener notificaciones paginadas
- `GET /notifications/unread-count` - Contador de no leídas
- `PATCH /notifications/mark-all-read` - Marcar todas como leídas

## 🔄 Flujo de Notificaciones WebSocket

### Eventos que recibes:
```javascript
{
    type: 'NEW_NOTIFICATION',
    notification: {
        id: 123,
        actor: { username: 'juan_perez' },
        type: 'POST_LIKE', // 'COMMENT_LIKE', 'COMMENT', 'FOLLOW'
        postId: 456,
        isRead: false,
        createdAt: '2024-01-01T10:00:00Z'
    }
}

{
    type: 'UNREAD_COUNT_UPDATE',
    unreadCount: 5
}
```

## ✅ Checklist de Implementación

### Backend (✅ Ya implementado):
- [x] Endpoints de likes en posts
- [x] Endpoints de likes en comentarios  
- [x] Endpoints de comentarios
- [x] Sistema de notificaciones WebSocket
- [x] Prevención de auto-notificaciones
- [x] Limpieza automática al eliminar contenido

### Frontend (🚧 Por implementar):
- [ ] Hook useNotifications
- [ ] Hook useLikes  
- [ ] Hook useComments
- [ ] Componente PostCard
- [ ] Componente CommentsList
- [ ] Componente CommentItem
- [ ] Integración con sistema de toast
- [ ] Manejo de estados optimista
- [ ] UI responsive

### Testing:
- [ ] Dar like a post → Verificar notificación
- [ ] Quitar like → Verificar eliminación de notificación
- [ ] Comentar post → Verificar notificación al autor
- [ ] Like a comentario → Verificar notificación al autor del comentario
- [ ] Eliminar comentario → Verificar limpieza de notificaciones

## 🎯 Resumen del Flujo

1. **Usuario hace acción** (like/comment) → API call
2. **Backend procesa** → Guarda en BD + Crea notificación
3. **WebSocket envía** → Notificación en tiempo real
4. **Frontend recibe** → Actualiza UI + Muestra toast
5. **Resultado** → Acción guardada + Notificación entregada

Este sistema garantiza que todas las acciones se guarden correctamente y las notificaciones lleguen en tiempo real a los usuarios afectados.