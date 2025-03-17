package ru.nand.postsuserservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.nand.postsuserservice.entities.DTO.UserDTO;
import ru.nand.postsuserservice.services.PostsLikesService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsLikesController {
    private final PostsLikesService postsLikesService;

    @Autowired
    public PostsLikesController(PostsLikesService postsLikesService) {
        this.postsLikesService = postsLikesService;
    }

    /// Лайк поста
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePost(@PathVariable int postId, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            log.info("Пользовательский запрос на лайк поста с id: {}", postId);
            return ResponseEntity.status(200).body(postsLikesService.likePost(postId, userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при лайке поста");
        }
    }

    /// Снятие лайка с поста
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<String> unlikePost(@PathVariable int postId, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            log.info("Пользовательский запрос на снятие лайка с поста с id: {}", postId);
            return ResponseEntity.status(200).body(postsLikesService.unlikePost(postId, userDetails.getUsername()));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при снятии лайка с поста");
        }
    }

    /// Получение списка пользователей, лайкнувших пост
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<UserDTO>> getLikedUsers(@PathVariable int postId){
        try{
            log.info("Пользовательский запрос на получение списка username'ов пользователей, лайкнувших пост с id: {}", postId);
            return ResponseEntity.status(200).body(postsLikesService.getLikedUsers(postId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
