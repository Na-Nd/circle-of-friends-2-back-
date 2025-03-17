package ru.nand.postsuserservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.nand.postsuserservice.entities.DTO.CommentDTO;
import ru.nand.postsuserservice.entities.requests.CommentRequest;
import ru.nand.postsuserservice.services.PostsCommentsService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
public class PostsCommentsController {
    private final PostsCommentsService postsCommentsService;

    @Autowired
    public PostsCommentsController(PostsCommentsService postsCommentsService) {
        this.postsCommentsService = postsCommentsService;
    }

    /// Добавление комментария к посту
    @PostMapping("/{postId}/comments")
    public ResponseEntity<String> addComment(
            @PathVariable int postId,
            @RequestBody CommentRequest comment,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            log.info("Пользовательский запрос на добавление комментария к посту с id {}", postId);
            return ResponseEntity.status(200).body(postsCommentsService.addComment(postId, comment, userDetails.getUsername()));
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
            @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            log.info("Пользовательский запрос на редактирование комментария к посту с id {}", postId);
            return ResponseEntity.status(200).body(postsCommentsService.updateComment(
                    postId,
                    commentId,
                    commentRequest,
                    userDetails.getUsername())
            );
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при редактировании комментария");
        }
    }

    /// Удаление комментария по id
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable int postId,
            @PathVariable int commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try{
            log.info("Пользовательский запрос на удаления комментария поста с id {}", postId);
            return ResponseEntity.status(200).body(postsCommentsService.deleteComment(postId, commentId, userDetails.getUsername()));
        } catch (Exception e){
            log.info(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления комментария");
        }
    }

    /// Получение всех комментариев поста
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getPostComments(@PathVariable int postId){
        try{
            log.info("Пользовательский запрос на получение комментариев к посту id {}", postId);
            return ResponseEntity.status(200).body(postsCommentsService.getPostComments(postId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
