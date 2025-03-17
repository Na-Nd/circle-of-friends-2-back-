package ru.nand.postsuserservice.controllers;

import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.nand.postsuserservice.entities.DTO.PostDTO;
import ru.nand.postsuserservice.entities.requests.PostRequest;
import ru.nand.postsuserservice.services.PostsService;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsController {

    private final PostsService postsService;

    @Autowired
    public PostsController(PostsService postsService) {
        this.postsService = postsService;
    }

    /// Создание поста
    @PostMapping("/create")
    public ResponseEntity<?> createPost(@ModelAttribute PostRequest postRequest, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Пользовательский запрос на создание поста");
            if (postRequest.getImages() == null) {
                postRequest.setImages(new ArrayList<>());
            }

            return ResponseEntity.status(200).body(postsService.createPost(postRequest, userDetails.getUsername()));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при создании поста");
        }
    }

    /// Получение поста по id (изображение base64)
    @GetMapping("/id/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable int postId) {
        try {
            log.info("Пользовательский запрос на получение поста с id: {}", postId);
            return ResponseEntity.status(200).body(postsService.getPostById(postId));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Пост не найден");
        }
    }

    /// Получение изображения поста по id как бинарный файл
    @GetMapping("/id/{postId}/binImg")
    public ResponseEntity<?> getPostImgById(@PathVariable int postId) {
        try {
            log.info("Пользовательский запрос на получение изображения поста с id: {}", postId);
            PostDTO postDTO = postsService.getPostById(postId);

            // Если есть фотографии
            if (postDTO.getImages() != null && !postDTO.getImages().isEmpty()) {
                // Возвращаем первое изображение
                byte[] imageBytes = postDTO.getImages().getFirst();
                String contentType = URLConnection.guessContentTypeFromName(postDTO.getImagesUrls().getFirst());

                return ResponseEntity.status(200)
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + postDTO.getImagesUrls().getFirst() + "\"")
                        .body(new InputStreamResource(new ByteArrayInputStream(imageBytes)));
            } else { // Если изображений нет, вернем просто DTO
                return ResponseEntity.status(200).body(postDTO);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Пост не найден");
        }
    }

    /// Удаление поста
    @DeleteMapping("/id/{postId}")
    public ResponseEntity<?> deletePostById(@PathVariable int postId, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            log.info("Пользовательский запрос на удаление поста с id: {}", postId);
            return ResponseEntity.status(200).body(postsService.deletePostById(postId, userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления поста");
        }
    }

    /// Редактирование своего поста
    @PutMapping("/id/{postId}")
    public ResponseEntity<String> updatePost(
            @PathVariable int postId,
            @ModelAttribute PostRequest postRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try{
            log.info("Пользовательский запрос на редактирование поста с id: {}", postId);
            if (postRequest.getImages() == null) {
                postRequest.setImages(new ArrayList<>());
            }

            return ResponseEntity.status(200).body(postsService.updatePost(postId, postRequest, userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка редактирования поста");
        }
    }

    /// Получение всех постов
    @GetMapping()
    public ResponseEntity<?> getAllPosts(){
        try{
            log.info("Пользовательский запрос на получение всех постов");
            return ResponseEntity.status(200).body(postsService.getAllPosts());
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Посты не найдены");
        }
    }

    /// Получение постов текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(@AuthenticationPrincipal UserDetails userDetails){
        try{
            log.info("Пользовательский запрос на получение собственных постов");
            return ResponseEntity.status(200).body(postsService.getPostsByAuthor(userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Ваши посты не найдены");
        }
    }

    /// Получение постов конкретного пользователя
    @GetMapping("/author/{author}")
    public ResponseEntity<?> getPostsByAuthor(@PathVariable String author){
        try{
            log.info("Пользовательский запрос на получение постов автора: {}", author);
            return ResponseEntity.status(200).body(postsService.getPostsByAuthor(author));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Посты автора " + author + " не найдены");
        }
    }

    /// Получение постов подписок
    @GetMapping("/subscriptions")
    public ResponseEntity<?> getSubscriptionsPosts(@AuthenticationPrincipal UserDetails userDetails){
        try{
            log.info("Пользовательский запрос на получение постов подписок пользователя: {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(postsService.getSubscriptionsPosts(userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Посты подписок не найдены");
        }
    }

    /// Получение постов по тэгам
    @GetMapping("/search-by-tags")
    public ResponseEntity<?> getPostsByTags(@RequestParam List<String> tags){
        try {
            return ResponseEntity.status(200).body(postsService.getPostsByTags(tags));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Посты не найдены");
        }
    }

    /// Получение постов по тексту
    @GetMapping("/search-by-text")
    public ResponseEntity<?> getPostsByText(@RequestBody String text){
        try{
            log.info("Пользовательский запрос на получение постов по тексту: {}", text);
            return ResponseEntity.status(200).body(postsService.getPostsByText(text));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Посты не найдены");
        }
    }

}
