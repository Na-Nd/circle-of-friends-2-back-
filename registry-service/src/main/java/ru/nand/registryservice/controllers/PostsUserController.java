package ru.nand.registryservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.CommentCreateDTO;
import ru.nand.registryservice.entities.DTO.CommentDTO;
import ru.nand.registryservice.entities.DTO.PostCreateDTO;
import ru.nand.registryservice.entities.DTO.PostDTO;
import ru.nand.registryservice.services.CommentService;
import ru.nand.registryservice.services.PostService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostsUserController {
    private final PostService postService;

    @Autowired
    public PostsUserController(PostService postService) {
        this.postService = postService;
    }

    /// Создание поста
    @PostMapping
    public ResponseEntity<String> createPost(@RequestBody String requestMessage){
        try{
            log.info("Запрос от posts-user-service на создание поста");
            postService.createPost(requestMessage);

            return ResponseEntity.status(200).body("Пост создан");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /// Получение поста по id
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable int postId){
        try{
            log.info("Запрос от posts-user-service на получение поста по id: {}", postId);
            return ResponseEntity.status(200).body(postService.getPostById(postId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Пост не найден");
        }
    }

    ///  Удаление поста по id
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePostById(@PathVariable int postId, @RequestBody String ownerUsername){
        try{
            log.info("Запрос от posts-user-service на удаление поста по id: {}", postId);
            return ResponseEntity.status(200).body(postService.deletePost(postId, ownerUsername)); // Если нет ошибок вернем список уникальных названий изображений
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления поста");
        }
    }

    /// Обновление поста по id
    @PutMapping("/{postId}")
    public ResponseEntity<String> updatePost(@PathVariable int postId, @RequestBody String requestMessage){
        try {
            log.info("Запрос от posts-user-service на обновление поста по id: {}", postId);
            return ResponseEntity.status(200).body(postService.updatePost(postId, requestMessage));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при обновлении поста");
        }
    }

    /// Получение всех постов (лента)
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts(){
        try{
            log.info("Запрос от posts-user-service на получение всех постов");
            return ResponseEntity.status(200).body(postService.getAllPosts());
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение постов конкретного пользователя
    @GetMapping("/user/{authorUsername}")
    public ResponseEntity<List<PostDTO>> getPostsByAuthor(@PathVariable String authorUsername){
        try{
            log.info("Запрос от posts-user-service на получение постов автора: {}", authorUsername);
            return ResponseEntity.status(200).body(postService.getPostsByAuthor(authorUsername));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение постов подписок
    @GetMapping("/subscriptions")
    public ResponseEntity<List<PostDTO>> getSubscriptionsPosts(@RequestParam String username){
        try{
            log.info("Запрос от posts-user-service на получение постов подписок пользователя {}", username);
            return ResponseEntity.status(200).body(postService.getSubscriptionsPosts(username));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение постов по тэгам
    @GetMapping("/search-by-tags")
    public ResponseEntity<List<PostDTO>> getPostsByTags(@RequestParam List<String> tags){
        try {
            log.info("Запрос от posts-user-service на получение постов по тэгам {}", tags);
            return ResponseEntity.status(200).body(postService.getPostsByTags(tags));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение постов по тексту
    @GetMapping("/search-by-text")
    public ResponseEntity<List<PostDTO>> getPostsByText(@RequestParam String text){
        try{
            log.info("Запрос от posts-user-service на получение постов по тексту: {}", text);
            return ResponseEntity.status(200).body(postService.getPostsByText(text));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
