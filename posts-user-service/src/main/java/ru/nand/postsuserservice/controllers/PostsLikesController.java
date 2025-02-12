package ru.nand.postsuserservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.nand.postsuserservice.services.PostsService;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsLikesController {
    private final PostsService postsService;

    @Autowired
    public PostsLikesController(PostsService postsService) {
        this.postsService = postsService;
    }

    /// Лайк поста
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(
            @PathVariable int postId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try{
            postsService.likePost(postId, userDetails.getUsername());
            return ResponseEntity.status(200).body("Лайк к посту добавлен");
        } catch (Exception e){
            log.error("Ошибка при лайке поста с id {}: {}", postId, e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    /// Удаление лайка с поста
    public ResponseEntity<String> unlikePost(
            @PathVariable int postId,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try {
            postsService.unlikePost(postId, userDetails.getUsername());
            return ResponseEntity.status(200).body("Лайк с поста удален");
        } catch (Exception e){
            log.error("Ошибка при удалении лайка с поста id {}: {}", postId, e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    /// Получить кол-во лайков на посте
    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<Integer> getLikesCount(@PathVariable int postId) {
        try {
            return ResponseEntity.ok(postsService.getLikesCount(postId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(0);
        }
    }

    /// Получить список имен пользователей лайкнувших пост
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<String>> getLikedUsers(@PathVariable int postId) {
        try {
            return ResponseEntity.ok(postsService.getLikedUsers(postId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}
