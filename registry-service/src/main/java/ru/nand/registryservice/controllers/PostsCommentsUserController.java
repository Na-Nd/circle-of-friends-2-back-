package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.CommentDTO;
import ru.nand.registryservice.services.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostsCommentsUserController {
    private final CommentService commentService;

    @Autowired
    public PostsCommentsUserController(CommentService commentService) {
        this.commentService = commentService;
    }

    /// Добавление комментария к посту
    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> addComment(@PathVariable int postId, @RequestBody String requestMessage){
        try{
            log.info("Запрос от posts-user-service на добавление комментария к посту с id {}", postId);
            return ResponseEntity.status(200).body(commentService.addComment(postId, requestMessage));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при добавлении комментария");
        }
    }

    /// Редактирование комментария по id
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> updateComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @RequestBody String requestMessage
    ) {
        try{
            log.info("Запрос от posts-user-service на обновление комментария к посту с id {}", postId);
            return ResponseEntity.status(200).body(commentService.updateComment(commentId, requestMessage));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при обновлении комментария");
        }
    }

    /// Удаление комментария по id
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @RequestBody String ownerUsername
    ){
        try{
            log.info("Запрос от posts-user-service на удаление комментария к посту с id {}", postId);
            return ResponseEntity.status(200).body(commentService.deleteComment(commentId, ownerUsername));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при удалении комментария");
        }
    }

    /// Получение всех комментариев поста
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getPostComments(@PathVariable int postId){
        try{
            log.info("Запрос от posts-user-service на получение комментариев поста с id {}", postId);
            return ResponseEntity.status(200).body(commentService.getPostComments(postId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
