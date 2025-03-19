package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.Comment;
import ru.nand.registryservice.entities.DTO.PostsUserService.CommentCreateDTO;
import ru.nand.registryservice.entities.DTO.PostsUserService.CommentDTO;
import ru.nand.registryservice.entities.Post;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.repositories.CommentRepository;
import ru.nand.registryservice.repositories.PostRepository;
import ru.nand.registryservice.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ObjectMapper objectMapper;
    private final UserSessionService userSessionService;
    private final UserRepository userRepository;

    /// Добавление комментария к посту
    public String addComment(int postId, String requestMessage){
        CommentCreateDTO commentCreateDTO;
        try{
            commentCreateDTO = objectMapper.readValue(requestMessage, CommentCreateDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при десериализации данных: {}", e.getMessage());
            throw new RuntimeException("Ошибка при десериализации данных: " + e.getMessage());
        }

        // Ищем пост и пользователя
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));
        User user = userRepository.findByUsername(commentCreateDTO.getCommentAuthorUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Comment comment = Comment.builder()
                .post(post)
                .author(user)
                .text(commentCreateDTO.getCommentText())
                .dateOfCreation(LocalDateTime.now())
                .build();

        // Достаем пользователя и обновляем время активности
        userSessionService.updateLastActivityTime(user);

        commentRepository.save(comment);

        return "Комментарий добавлен";
    }

    /// Обновление комментария поста (пост не нужен, т.к. каждый комментарий уникален)
    @Transactional
    public String updateComment(int commentId, String requestMessage){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        CommentCreateDTO commentCreateDTO;
        try{
            commentCreateDTO = objectMapper.readValue(requestMessage, CommentCreateDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при десериализации данных : {}", e.getMessage());
            throw new RuntimeException("Ошибка при десериализации данных: " + e.getMessage());
        }

        if (!comment.getAuthor().getUsername().equals(commentCreateDTO.getCommentAuthorUsername())){
            throw new RuntimeException("У вас нет такого комментария");
        }

        comment.setText(commentCreateDTO.getCommentText());

        userSessionService.updateLastActivityTime(
                userRepository.findById(comment.getAuthor().getId())
                        .orElseThrow(()-> new RuntimeException("Пользователь не найден"))
        );

        commentRepository.save(comment);

        return "Комментарий обновлен";
    }

    /// Удаление комментария поста (пост не нужен, т.к. каждый комментарий уникален)
    @Transactional
    public String deleteComment(int commentId, String ownerUsername){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        if(!comment.getAuthor().getUsername().equals(ownerUsername)){
            throw new RuntimeException("У вас нет такого комментария");
        }

        userSessionService.updateLastActivityTime(
                userRepository.findById(comment.getAuthor().getId())
                        .orElseThrow(()-> new RuntimeException("Пользователь не найден"))
        );

        commentRepository.delete(comment);

        return "Комментарий удален";
    }

    /// Получение всех комментариев поста
    public List<CommentDTO> getPostComments(int postId){
        return commentRepository.findByPostId(postId).stream()
                .map(comment -> new CommentDTO(
                        comment.getId(),
                        comment.getPost().getId(),
                        comment.getAuthor().getUsername(),
                        comment.getText(),
                        comment.getDateOfCreation()
                )).collect(Collectors.toList());
    }

}
