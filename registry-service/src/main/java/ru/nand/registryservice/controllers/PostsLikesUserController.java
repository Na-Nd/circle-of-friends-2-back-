package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.UserDTO;
import ru.nand.registryservice.services.LikeService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostsLikesUserController {
    private final LikeService likeService;

    @Autowired
    public PostsLikesUserController(LikeService likeService) {
        this.likeService = likeService;
    }

    /// Установка лайка на пост
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable int postId, @RequestBody String username){
        try {
            log.warn("Запрос от posts-user-service на лайк поста с id {}", postId);
            return ResponseEntity.status(200).body(likeService.likePost(postId, username));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при лайке поста");
        }
    }

    /// Снятие лайка с поста
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<String> unlikePost(@PathVariable int postId, @RequestBody String username){
        try {
            log.info("Запрос от posts-user-service на снятии лайка с поста с id {}", postId);
            return ResponseEntity.status(200).body(likeService.unlikePost(postId, username));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при снятии лайка с поста");
        }
    }

    /// Получение списка пользователей, лайкнувших пост
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<UserDTO>> getLikedUsers(@PathVariable int postId){
        try{
            log.info("Запрос от posts-user-service на получение списка пользователей, лайкнувших пост с id {}", postId);
            return ResponseEntity.status(200).body(likeService.getLikedUsers(postId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
