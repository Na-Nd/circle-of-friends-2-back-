package ru.nand.postsuserservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.postsuserservice.entities.DTO.CommentDTO;
import ru.nand.postsuserservice.entities.DTO.PostDTO;
import ru.nand.postsuserservice.entities.requests.PostRequest;
import ru.nand.postsuserservice.entities.requests.PostUpdateRequest;
import ru.nand.postsuserservice.utils.JwtUtil;
import org.springframework.core.ParameterizedTypeReference;
import ru.nand.postsuserservice.utils.PostsUtil;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PostsService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final PostsUtil postsUtil;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Autowired
    public PostsService(RestTemplate restTemplate, JwtUtil jwtUtil, PostsUtil postsUtil) {
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.postsUtil = postsUtil;
    }

    // Добавление поста
    public String createPost(PostRequest postRequest) throws RuntimeException {
        String imageName = postsUtil.saveImage(postRequest.getImage());

        PostDTO postDTO = new PostDTO(postRequest.getText(), postRequest.getTags(), postRequest.getAuthor(), imageName, null);
        String requestMessage;
        try {
            requestMessage = new ObjectMapper().writeValueAsString(postDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации данных запроса для создания поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        String url = REGISTRY_SERVICE_URL + "/api/posts";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса на создание поста, ДТО: {}", postDTO);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Ошибка при отправке данных о посте в registry-service: {}", e.getMessage());
            throw new RuntimeException("Ошибка при отправке данных о посте: " + e.getMessage());
        }

        return imageName;
    }

    // Получение конкретного поста по id
    public PostDTO getPostById(int id) throws RuntimeException {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            PostDTO postDTO = new ObjectMapper().readValue(response.getBody(), PostDTO.class);

            postDTO.setImageBase64(postsUtil.encodeImageToBase64(postDTO.getFilename()));

            return postDTO;
        } catch (Exception e) {
            log.error("Ошибка при получении поста из registry-service: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении поста: " + e.getMessage());
        }
    }

    // Получение всех постов
    public List<PostDTO> getAllPosts() throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {} // Чтобы exchange мог обрабатывать генерк-класс List
            );

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            posts.forEach(post -> post.setImageBase64(postsUtil.encodeImageToBase64(post.getFilename())));

            return posts;
        } catch (Exception e) {
            log.error("Ошибка при получении всех постов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении постов");
        }
    }

    // Получение постов пользователя
    public List<PostDTO> getPostsByAuthor(String author) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/user/" + author;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {} // Чтобы exchange мог обрабатывать генерик-класс List
            );

            List<PostDTO> posts = response.getBody();
            if(posts == null) return Collections.emptyList();

            posts.forEach(post -> post.setImageBase64(postsUtil.encodeImageToBase64(post.getFilename())));

            return posts;
        } catch (Exception e){
            log.error("Ошибка при получении постов пользователя {}: {}", author, e.getMessage());
            throw new RuntimeException("Ошибка при получении постов пользователя");
        }
    }

    // Обновление поста
    public void updatePost(int postId, PostUpdateRequest postUpdateRequest, UserDetails userDetails) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId;

        // Проверяем автора
        PostDTO existingPost = getPostById(postId);
        if (!existingPost.getAuthor().equals(userDetails.getUsername())) {
            throw new RuntimeException("Вы не можете редактировать чужой пост");
        }

        String newFilename = null;
        if (postUpdateRequest.getImage() != null && !postUpdateRequest.getImage().isEmpty()) {
            postsUtil.deleteImage(existingPost.getFilename());
            newFilename = postsUtil.saveImage(postUpdateRequest.getImage());
        }

        PostDTO updatedPost = new PostDTO(
                postUpdateRequest.getText() != null ? postUpdateRequest.getText() : existingPost.getText(),
                postUpdateRequest.getTags() != null ? postUpdateRequest.getTags() : existingPost.getTags(),
                existingPost.getAuthor(),
                newFilename != null ? newFilename : existingPost.getFilename(),
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<PostDTO> requestEntity = new HttpEntity<>(updatedPost, headers);

        log.debug("Отправка PATCH-запроса в registry-service: {}", updatedPost);
        log.debug("Заголовки запроса: {}", headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Ошибка при обновлении поста: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обновлении поста");
        }
    }


    // Удаление поста
    public void deletePost(int postId, UserDetails userDetails) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId;

        // Проверяем автора на клиентской стороне
        PostDTO existingPost = getPostById(postId);
        if (!existingPost.getAuthor().equals(userDetails.getUsername())) {
            throw new RuntimeException("Вы не можете удалить чужой пост");
        }

        postsUtil.deleteImage(existingPost.getFilename());

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e){
            log.error("Ошибка при удалении поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка при удалении поста");
        }
    }

    // Получение поста по тэгам
    public List<PostDTO> getPostsByTags(List<String> tags) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/search?tags=" + String.join(",", tags);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}
            );

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            for (PostDTO post : posts) {
                post.setImageBase64(postsUtil.encodeImageToBase64(post.getFilename()));
            }

            return posts;
        } catch (Exception e) {
            log.error("Ошибка при поиске постов по тегам: {}", e.getMessage());
            throw new RuntimeException("Ошибка при поиске постов");
        }
    }

    // Получения поста по тексту
    public List<PostDTO> getPostsByText(String text) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/search-by-text?text=" + text;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            List<PostDTO> posts = response.getBody();

            if(posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                post.setImageBase64(postsUtil.encodeImageToBase64(post.getFilename()));
            }

            return posts;
        } catch (Exception e) {
            log.error("Ошибка при получении поста по тексту: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении постов");
        }
    }

    // Получить посты подписок
    public List<PostDTO> getSubscriptionPosts(String username) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/subscriptions?username=" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            List<PostDTO> posts = response.getBody();
            if(posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                post.setImageBase64(postsUtil.encodeImageToBase64(post.getFilename()));
            }

            return posts;
        } catch (Exception e){
            log.error("Ошибка при получении постов подписок: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении постов подписок");
        }
    }

    // Лайк поста
    public void likePost(int postId, String username) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/like";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);

        try{
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        } catch (Exception e){
            log.error("Ошибка при лайке поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при лайке поста: " + e.getMessage());
        }
    }

    // Снятие лайка с поста
    public void unlikePost(int postId, String username) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/like";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);

        try{
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        } catch (Exception e){
            log.error("Ошибка при удалении лайка с поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при снятии лайка: " + e.getMessage());
        }
    }

    // Получение кол-ва лайков
    public int getLikesCount(int postId) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/likes/count";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        try{
            ResponseEntity<Integer> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Integer.class
            );

            return response.getBody();
        } catch (Exception e){
            log.error("Ошибка при получении количества лайков поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при получении количества лайков поста: " + e.getMessage());
        }
    }

    // Получение списка лайкнувших
    public List<String> getLikedUsers(int postId) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/likes";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        try{
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            return response.getBody();
        } catch (Exception e){
            log.error("Ошибка получения списка лайкнувших пост с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка получения списка лайкнувших: " + e.getMessage());
        }
    }

    // Добавить комментарий
    public void addComment(int postId, String username, String text) {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        CommentDTO request = new CommentDTO(username, text);
        String requestBody;
        try {
            requestBody = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации CommentRequest: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        } catch (Exception e) {
            log.error("Ошибка при добавлении комментария: {}", e.getMessage());
            throw new RuntimeException("Ошибка при добавлении комментария");
        }
    }

    // Редактировать комментарий
    public void editComment(int postId, int commentId, String username, String newText) {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments/" + commentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        CommentDTO request = new CommentDTO(username, newText);
        String requestBody;
        try {
            requestBody = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации CommentUpdateRequest: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
        } catch (Exception e) {
            log.error("Ошибка при редактировании комментария: {}", e.getMessage());
            throw new RuntimeException("Ошибка при редактировании комментария");
        }
    }

    // Удалить комментарий
    public void deleteComment(int postId, int commentId, String username) {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments/" + commentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        CommentDTO request = new CommentDTO();
        request.setUsername(username);
        String requestBody;
        try {
            requestBody = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации CommentDeleteRequest: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных запроса");
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        } catch (Exception e) {
            log.error("Ошибка при удалении комментария: {}", e.getMessage());
            throw new RuntimeException("Ошибка при удалении комментария");
        }
    }

    // Получить комментарии с пагинацией
    public List<CommentDTO> getComments(int postId, int page, int size) {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/comments?page=" + page + "&size=" + size;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<CommentDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<List<CommentDTO>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при получении комментариев: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении комментариев");
        }
    }

}
