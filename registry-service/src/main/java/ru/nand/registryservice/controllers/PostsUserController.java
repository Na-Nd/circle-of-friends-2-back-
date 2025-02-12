package ru.nand.registryservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.CommentDTO;
import ru.nand.registryservice.entities.DTO.PostDTO;
import ru.nand.registryservice.services.PostService;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostsUserController {
    private final PostService postService;

    @Autowired
    public PostsUserController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<String> createPost(@RequestBody String message){
        try{
            PostDTO postDTO;
            postDTO = new ObjectMapper().readValue(message, PostDTO.class);

            postService.createPost(postDTO);
            return ResponseEntity.status(200).body("Пост успешно создан");
        } catch (Exception e){
            log.error("Ошибка: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при создании поста");
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable int postId){
        try{
            return ResponseEntity.status(200).body(postService.getPostById(postId));
        } catch (RuntimeException e){
            log.error("Ошибка при получении поста: {}", e.getMessage());
            return ResponseEntity.status(404).body("Пост не найден");
        }
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<String> updatePost(@PathVariable int postId, @RequestBody PostDTO postDTO){
        try{
            postService.updatePost(postId, postDTO);
            return ResponseEntity.status(200).body("Пост успешно обновлен");
        } catch (RuntimeException e){
            log.error("Ошибка при обновлении поста: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при обновлении поста");
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(@PathVariable int postId) {
        try {
            postService.deletePost(postId);
            return ResponseEntity.status(200).body("Пост успешно удален");
        } catch (RuntimeException e) {
            log.error("Ошибка при удалении поста: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при удалении поста");
        }
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts(){
        try{
            return ResponseEntity.status(200).body(postService.getAllPosts());
        } catch (Exception e){
            log.error("Ошибка при получении всех постов: {}", e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/user/{author}")
    public ResponseEntity<List<PostDTO>> getPostsByAuthor(@PathVariable String author){
        try{
            return ResponseEntity.status(200).body(postService.getPostsByAuthor(author));
        } catch (Exception e){
            log.error("Ошибка при получении постов пользователя {}: {}", author, e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<PostDTO>> getSubscriptionPosts(@RequestParam String username){
        try{
            return ResponseEntity.status(200).body(postService.getSubscriptionPosts(username));
        } catch (Exception e){
            log.error("Ошибка при получении постов подписок: {}", e.getMessage());
            return ResponseEntity.status(404).body(Collections.emptyList());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostDTO>> getPostsByTags(@RequestParam List<String> tags){
        try{
            return ResponseEntity.status(200).body(postService.getPostsByTags(tags));
        } catch (Exception e){
            log.error("Ошибка при поиске постов по тегам: {}", e.getMessage());
            return ResponseEntity.status(404).body(Collections.emptyList());
        }
    }

    @GetMapping("/search-by-text")
    public ResponseEntity<List<PostDTO>> getPostsByText(@RequestParam String text){
        try{
            return ResponseEntity.status(200).body(postService.getPostsByText(text));
        } catch (Exception e){
            log.error("Ошибка при поиске постов по тексту: {}", e.getMessage());
            return ResponseEntity.status(404).body(Collections.emptyList());
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable int postId, @RequestBody String username){
        try{
            postService.likePost(postId, username);
            return ResponseEntity.status(200).body("Лайк к посту добавлен");
        } catch (Exception e){
            log.error("Ошибка при лайке поста с id {}: {}", postId, e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при лайке поста");
        }
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<String> unlikePost(@PathVariable int postId, @RequestBody String username){
        try{
            postService.unlikePost(postId, username);
            return ResponseEntity.status(200).body("Лайк удален");
        } catch (Exception e){
            log.error("Ошибка при удалении лайка с поста id {}: {}", postId, e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при удалении лайка с поста");
        }
    }

    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<Integer> getLikesCount(@PathVariable int postId) {
        try {
            return ResponseEntity.status(200).body(postService.getLikesCount(postId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(0);
        }
    }

    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<String>> getLikedUsers(@PathVariable int postId) {
        try {
            return ResponseEntity.status(200).body(postService.getLikedUsers(postId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> addComment(@PathVariable int postId, @RequestBody CommentDTO commentDTO) {
        try{
            postService.addComment(postId, commentDTO);
            return ResponseEntity.status(200).body("Комментарий добавлен");
        } catch (Exception e){
            log.error("Ошибка при добавлении комментария: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка добавления комментария");
        }
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> editComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @RequestBody CommentDTO commentDTO
    ) {
        try{
            postService.editComment(commentId, commentDTO);
            return ResponseEntity.status(200).body("Комментарий обновлен");
        } catch (Exception e){
            log.error("Ошибка при редактировании комментария: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка редактирования комментария");
        }
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @RequestParam String username
    ){
        try{
            postService.deleteComment(commentId, username);
            return ResponseEntity.status(200).body("Комментарий удален");
        } catch (Exception e){
            log.error("Ошибка при удалении комментария: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при удалении комментария");
        }
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable int postId, @RequestParam int page, @RequestParam int size){
        try{
            return ResponseEntity.status(200).body(postService.getComments(postId, page, size));
        } catch (Exception e){
            log.error("Ошибка при получении комментариев: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при получении комментариев");
        }
    }

}
