package ru.nand.postsuserservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.postsuserservice.entities.DTO.CommentCreateDTO;
import ru.nand.postsuserservice.entities.DTO.CommentDTO;
import ru.nand.postsuserservice.entities.requests.CommentRequest;
import ru.nand.postsuserservice.utils.JwtUtil;
import ru.nand.postsuserservice.utils.PostsUtil;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostsCommentsService {
    private final PostsUtil postsUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Добавление комментария
    public String addComment(int postId, CommentRequest commentRequest, String commentAuthor){
        // Формируем DTO для отправки
        CommentCreateDTO commentCreateDTO = new CommentCreateDTO(commentRequest.getCommentText(), commentAuthor);

        String requestMessage;
        try{
            requestMessage = objectMapper.writeValueAsString(commentCreateDTO);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при сериализации данных запроса: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сериализации данных запроса: " + e.getMessage());
        }

        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса на создание комментария: {}", requestMessage);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при создании комментария: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при добавлении комментария к посту {} пользователем {}: {}", postId, commentAuthor, e.getMessage());
            throw new RuntimeException("Ошибка при добавлении комментария к посту " + postId + " пользователем " + commentAuthor + ": " + e.getMessage());
        }
    }

    /// Редактирование комментария (PUT)
    public String updateComment(int postId, int commentId, CommentRequest commentRequest, String commentAuthor){
        // Формируем DTO для отправки
        CommentCreateDTO commentCreateDTO = new CommentCreateDTO(commentRequest.getCommentText(), commentAuthor);

        String requestMessage;
        try{
            requestMessage = objectMapper.writeValueAsString(commentCreateDTO);
        } catch (JsonProcessingException e) {
            log.warn("Ошибка при сериализации данных запроса : {}", e.getMessage());
            throw new RuntimeException("Ошибка при сериализации данных запроса: " + e.getMessage());
        }

        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments/" + commentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса на обновление комментария: {}", requestMessage);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при обновлении комментария: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при обновлении комментария с id {} пользователем {}: {}", commentId, commentAuthor, e.getMessage());
            throw new RuntimeException("Ошибка при обновлении комментария с id " + commentId + " пользователем " + commentAuthor + ": " + e.getMessage());
        }
    }

    /// Удаление комментария
    public String deleteComment(int postId, int commentId, String username){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments/" + commentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при удалении комментария: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при удалении комментария с id {} пользователем {}: {}", commentId, username, e.getMessage());
            throw new RuntimeException("Ошибка при удалении комментария: " + commentId + " пользователем " + username + ": " + e.getMessage());
        }
    }

    /// Получение всех комментариев поста
    public List<CommentDTO> getPostComments(int postId){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<CommentDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {} // Чтобы exchange мог обрабатывать генерк-класс List
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении всех комментариев поста {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<CommentDTO> comments = response.getBody();
            if (comments == null) return Collections.emptyList();

            return comments;
        } catch (Exception e){
            log.warn("Ошибка при получении комментариев поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при получении комментариев поста с id " + postId + ": " + e.getMessage());
        }
    }
}
