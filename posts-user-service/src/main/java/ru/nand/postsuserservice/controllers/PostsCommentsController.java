package ru.nand.postsuserservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.nand.postsuserservice.services.PostsService;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsCommentsController {
    private final PostsService postsService;

    @Autowired
    public PostsCommentsController(PostsService postsService) {
        this.postsService = postsService;
    }

    /// Добавление комментария к посту
    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> addComment(
            @PathVariable int postId,
            @RequestParam String text,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            postsService.addComment(postId, userDetails.getUsername(), text);
            return ResponseEntity.status(200).body("Комментарий добавлен");
        } catch (Exception e){
            log.error("Ошибка при добавления комментария: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    /// Редактирование комментария
    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> editComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @RequestParam String newText,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try{
            postsService.editComment(postId, commentId, userDetails.getUsername(), newText);
            return ResponseEntity.status(200).body("Комментарий обновлен");
        } catch (Exception e){
            log.error("Ошибка при редактировании поста: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    /// Удаление комментария
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            postsService.deleteComment(postId, commentId, userDetails.getUsername());
            return ResponseEntity.status(200).body("Комментарий удален");
        } catch (Exception e){
            log.error("Ошибка при удалении комментария: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }

    /// Получить комментарии к посту (с пагинацией, так как комментариев может быть много)
    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable int postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            return ResponseEntity.status(200).body(postsService.getComments(postId, page, size));
        } catch (Exception e){
            log.error("Ошибка при получении комментариев: {}", e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера");
        }
    }
}
