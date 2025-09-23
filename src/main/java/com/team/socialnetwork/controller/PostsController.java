package com.team.socialnetwork.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.socialnetwork.dto.CreateCommentRequest;
import com.team.socialnetwork.dto.CreatePostRequest;
import com.team.socialnetwork.dto.PostDetailResponse;
import com.team.socialnetwork.dto.PostResponse;
import com.team.socialnetwork.entity.Comment;
import com.team.socialnetwork.entity.Post;
import com.team.socialnetwork.entity.PostLike;
import com.team.socialnetwork.entity.User;
import com.team.socialnetwork.repository.CommentRepository;
import com.team.socialnetwork.repository.PostLikeRepository;
import com.team.socialnetwork.repository.PostRepository;
import com.team.socialnetwork.repository.UserRepository;
import com.team.socialnetwork.service.NotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
public class PostsController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationService notificationService;

    public PostsController(PostRepository postRepository, UserRepository userRepository,
                           CommentRepository commentRepository, PostLikeRepository postLikeRepository,
                           NotificationService notificationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<com.team.socialnetwork.dto.MessageResponse> create(Authentication authentication,
                                               @Valid @RequestBody CreatePostRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        Post post = new Post(request.getDescription(), request.getImage(), author);
        postRepository.save(post);
        return ResponseEntity.ok(new com.team.socialnetwork.dto.MessageResponse("Post created successfully"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/{postId}/comments")
    public ResponseEntity<com.team.socialnetwork.dto.MessageResponse> createComment(Authentication authentication,
                                                                                   @org.springframework.web.bind.annotation.PathVariable Long postId,
                                                                                   @Valid @RequestBody CreateCommentRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));

        // Enforce privacy: if post's author is private, only the author themselves or their followers can comment
        User postAuthor = post.getAuthor();
        if (postAuthor.isPrivate() && !postAuthor.getId().equals(author.getId()) && !author.getFollowing().contains(postAuthor)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        Comment comment = new Comment(request.getText(), post, author);
        commentRepository.save(comment);
        
        // Crear notificación para el autor del post (si no es el mismo que comenta)
        if (!postAuthor.getId().equals(author.getId())) {
            notificationService.createAndSendNotification(
                    postAuthor, author, com.team.socialnetwork.entity.Notification.NotificationType.COMMENT, post, comment);
        }
        
        return ResponseEntity.ok(new com.team.socialnetwork.dto.MessageResponse("Comment created successfully"));
    }

    // List comments for a post (no userId required)
    @org.springframework.web.bind.annotation.GetMapping("/{postId}/comments")
    public ResponseEntity<java.util.List<com.team.socialnetwork.dto.CommentResponse>> listCommentsForPost(
            Authentication authentication,
            @org.springframework.web.bind.annotation.PathVariable Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        java.util.List<com.team.socialnetwork.entity.Comment> comments = commentRepository.findByPostId(postId);
        java.util.List<com.team.socialnetwork.dto.CommentResponse> resp = comments.stream()
                .map(c -> new com.team.socialnetwork.dto.CommentResponse(c.getId(), c.getCreatedAt(), c.getText(), c.getAuthor().getUsername()))
                .toList();
        return ResponseEntity.ok(resp);
    }

    // Get post detail (with counts and viewer flags)
    @org.springframework.web.bind.annotation.GetMapping("/{postId:\\d+}")
    public ResponseEntity<PostDetailResponse> getPost(Authentication authentication,
                                                      @org.springframework.web.bind.annotation.PathVariable Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        long likesCount = postLikeRepository.countByPostId(postId);
        long commentsCount = commentRepository.findByPostId(postId).size();
        boolean viewerLiked = postLikeRepository.existsByUserIdAndPostId(viewer.getId(), postId);

        com.team.socialnetwork.dto.SafeUser authorDto = new com.team.socialnetwork.dto.SafeUser(
                post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getAuthor().getUsername(),
                post.getAuthor().getEmail(),
                post.getAuthor().getCreatedAt()
        );

        PostDetailResponse resp = new PostDetailResponse(
                post.getId(), post.getCreatedAt(), post.getDescription(), post.getImage(),
                authorDto, likesCount, commentsCount, viewerLiked
        );
        return ResponseEntity.ok(resp);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<com.team.socialnetwork.dto.MessageResponse> delete(Authentication authentication,
                                                                            @org.springframework.web.bind.annotation.PathVariable Long id) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        if (!post.getAuthor().getEmail().equals(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "You can only delete your own posts");
        }
        
        // Limpiar notificaciones relacionadas con este post antes de eliminarlo
        notificationService.cleanupNotificationsForPost(id);
        
        postRepository.delete(post);
        return ResponseEntity.ok(new com.team.socialnetwork.dto.MessageResponse("Post deleted successfully"));
    }

    @org.springframework.web.bind.annotation.GetMapping("/mine")
    public ResponseEntity<java.util.List<PostResponse>> myPosts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        java.util.List<Post> posts = postRepository.findByAuthorId(me.getId());
        java.util.List<PostResponse> resp = posts.stream()
                .map(p -> new PostResponse(p.getId(), p.getCreatedAt(), p.getDescription(), p.getImage(), p.getAuthor().getUsername()))
                .toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<java.util.Map<String, Long>> countPostLikes(Authentication authentication,
                                                                      @PathVariable Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User viewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        long count = postLikeRepository.countByPostId(postId);
        java.util.Map<String, Long> body = new java.util.HashMap<>();
        body.put("count", count);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{postId}/likes")
    public ResponseEntity<java.util.List<com.team.socialnetwork.dto.SafeUser>> listPostLikes(Authentication authentication,
                                                                                             @PathVariable Long postId,
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
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(viewer.getId()) && !viewer.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        Page<PostLike> likes = postLikeRepository.findByPostId(postId, PageRequest.of(page, size));
        java.util.List<com.team.socialnetwork.dto.SafeUser> users = likes.getContent().stream().map(l -> {
            User u = l.getUser();
            return new com.team.socialnetwork.dto.SafeUser(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{postId}/likes")
    public ResponseEntity<com.team.socialnetwork.dto.MessageResponse> likePost(Authentication authentication,
                                                                               @PathVariable Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));

        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(user.getId()) && !user.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        if (postLikeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Already liked");
        }
        postLikeRepository.save(new PostLike(user, post));
        
        // Crear notificación para el autor del post (si no es el mismo que da like)
        User postOwner = post.getAuthor();
        if (!postOwner.getId().equals(user.getId())) {
            notificationService.createAndSendNotification(
                    postOwner, user, com.team.socialnetwork.entity.Notification.NotificationType.POST_LIKE, post);
        }
        
        return ResponseEntity.ok(new com.team.socialnetwork.dto.MessageResponse("Post liked successfully"));
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<com.team.socialnetwork.dto.MessageResponse> unlikePost(Authentication authentication,
                                                                                 @PathVariable Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(user.getId()) && !user.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }
        int deleted = postLikeRepository.deleteByUserIdAndPostId(user.getId(), postId);
        if (deleted == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Not liked yet");
        }
        
        // Eliminar notificación de like si existe
        User postOwner = post.getAuthor();
        if (!postOwner.getId().equals(user.getId())) {
            notificationService.removeNotification(
                    postOwner.getId(), user.getId(), 
                    com.team.socialnetwork.entity.Notification.NotificationType.POST_LIKE, postId);
        }
        
        return ResponseEntity.ok(new com.team.socialnetwork.dto.MessageResponse("Post unliked successfully"));
    }

    @GetMapping("/{postId}/likes/check")
    public ResponseEntity<java.util.Map<String, Boolean>> checkPostLike(Authentication authentication,
                                                                        @PathVariable Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        
        // 404 if post doesn't exist
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Post not found"));
        
        // Check privacy permissions
        User author = post.getAuthor();
        if (author.isPrivate() && !author.getId().equals(user.getId()) && !user.getFollowing().contains(author)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "This account is private");
        }

        boolean isLiked = postLikeRepository.existsByUserIdAndPostId(user.getId(), postId);
        java.util.Map<String, Boolean> response = new java.util.HashMap<>();
        response.put("liked", isLiked);
        return ResponseEntity.ok(response);
    }
}
