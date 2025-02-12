package ru.nand.postsuserservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.nand.postsuserservice.entities.DTO.PostDTO;
import ru.nand.postsuserservice.entities.requests.PostRequest;
import ru.nand.postsuserservice.entities.requests.PostUpdateRequest;
import ru.nand.postsuserservice.services.PostsService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsController {
    private final PostsService postsService;

    @Autowired
    public PostsController(PostsService postsService) {
        this.postsService = postsService;
    }

    // Создание поста
    @PostMapping
    public ResponseEntity<String> createPost(
            @RequestParam("text") String text,
            @RequestParam("tags") Set<String> tags,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            PostRequest postRequest = new PostRequest(text, tags, userDetails.getUsername(), image);
            String imageName = postsService.createPost(postRequest);
            return ResponseEntity.status(200).body("Пост успешно создан с изображением: " + imageName);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при создании поста");
        }
    }

    // Получить все посты
    @GetMapping
    public ResponseEntity<?> getAllPosts(){
        try{
            return ResponseEntity.status(200).body(postsService.getAllPosts());
        } catch (Exception e){
            log.error("Ошибка получения всех постов: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @GetMapping("/id/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable int postId) {
        try{
            return ResponseEntity.status(200).body(postsService.getPostById(postId));
        } catch (RuntimeException e){
            log.error("Ошибка при получении поста: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<String> updatePost(
            @PathVariable int postId,
            @ModelAttribute PostUpdateRequest postUpdateRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try{
            postsService.updatePost(postId, postUpdateRequest, userDetails);
            return ResponseEntity.status(200).body("Пост успешно обновлен");
        } catch (RuntimeException e){
            log.error("Ошибка при обновлении поста с id {}: {}", postId, e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable int postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            postsService.deletePost(postId, userDetails);
            return ResponseEntity.status(200).body("Пост успешно удален");
        } catch (RuntimeException e){
            log.error("Ошибка при удалении поста: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(@AuthenticationPrincipal UserDetails userDetails){
        try{
            return ResponseEntity.status(200).body(postsService.getPostsByAuthor(userDetails.getUsername()));
        } catch (Exception e){
            log.error("Ошибка при получении постов текущего пользователя {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @GetMapping("/author/{author}")
    public ResponseEntity<?> getPostsByAuthor(@PathVariable String author){
        try{
            return ResponseEntity.status(200).body(postsService.getPostsByAuthor(author));
        } catch (Exception e){
            log.error("Ошибка при получении постов пользователя {}: {}", author, e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<PostDTO>> getSubscriptionPosts(@AuthenticationPrincipal UserDetails userDetails){
        try{
            return ResponseEntity.status(200).body(postsService.getSubscriptionPosts(userDetails.getUsername()));
        } catch (Exception e){
            log.error("Ошибка при получении постов подписок пользователя {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostDTO>> getPostsByTags(@RequestParam List<String> tags) {
        try {
            List<PostDTO> posts = postsService.getPostsByTags(tags);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.error("Ошибка при поиске постов по тегам: {}", e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/search-by-text")
    public ResponseEntity<List<PostDTO>> getPostsByText(@RequestParam String text){
        try{
            return ResponseEntity.status(200).body(postsService.getPostsByText(text));
        } catch (Exception e){
            log.error("Ошибка при получении постов по тексту {}: {}", text, e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

}
