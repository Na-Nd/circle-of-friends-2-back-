package ru.nand.postsuserservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.postsuserservice.entities.DTO.PostCreateDTO;
import ru.nand.postsuserservice.entities.DTO.PostDTO;
import ru.nand.postsuserservice.entities.DTO.PostUpdateDTO;
import ru.nand.postsuserservice.entities.requests.PostRequest;
import ru.nand.postsuserservice.utils.JwtUtil;
import ru.nand.postsuserservice.utils.PostsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostsService {
    private final PostsUtil postsUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Создание поста
    public String createPost(PostRequest postRequest, String ownerUsername){
        List<String> uploadedImagesUrls = new ArrayList<>();

        // Если список фотографий не пустой
        if (postRequest.getImages() != null && !postRequest.getImages().isEmpty()) {
            // Загрузка файлов поста на диск и получение их уникальных названий
            uploadedImagesUrls = postsUtil.uploadImages(postRequest.getImages());
            log.debug("Фотографии загружены, уникальные названия: {}", uploadedImagesUrls);
        }

        // Создание DTO для формирования в JSON
        PostCreateDTO postCreateDTO = PostCreateDTO.builder()
                .text(postRequest.getText())
                .tags(postRequest.getTags())
                .imagesUrls(uploadedImagesUrls)
                .ownerUsername(ownerUsername)
                .build();

        // Складываем DTO в JSON для передачи в registry-service
        String requestMessage;
        try {
            requestMessage = objectMapper.writeValueAsString(postCreateDTO);
        } catch (JsonProcessingException e){
            log.warn("Ошибка сериализации данных при создании поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных");
        }
        log.debug("Сформирован JSON для передачи в registry-service: {}", requestMessage);

        String url = REGISTRY_SERVICE_URL + "/api/posts";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса на создание поста: {}", requestMessage);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при создании поста: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при отправке запроса в registry-service на создание поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка при создании поста: " + e.getMessage());
        }
    }

    /// Получение поста по id
    public PostDTO getPostById(int postId) {
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId;
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

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при получении поста по id {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            // Десериализуем в DTO
            PostDTO postDTO = objectMapper.readValue(response.getBody(), PostDTO.class);

            // Если список уникальных имен фотографий не пустой
            if (!postDTO.getImagesUrls().isEmpty()) {
                // То загружаем изображения и добавляем их в DTO в формате Base64
                List<String> imagesBase64 = new ArrayList<>();
                for (String imageName : postDTO.getImagesUrls()) {
                    String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                    imagesBase64.add(base64Image);
                }
                postDTO.setImagesBase64(imagesBase64);
            }

            return postDTO;
        } catch (Exception e) {
            log.warn("Ошибка при получении поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении поста с id " + postId + ": " + e.getMessage());
        }
    }

    /// Удаление поста
    public String deletePostById(int postId, String postOwnerUsername){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(postOwnerUsername, headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при удалении поста с id{}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            // Получаем изображения
            List<String> filenames;
            try{
                filenames = objectMapper.readValue(response.getBody(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e){
                log.warn("Ошибка при десериализации сообщения: {}", e.getMessage());
                throw new RuntimeException("Ошибка при десериализации ответа от registry-service");
            }

            // Удаляем изображения с диска
            for(String filename : filenames){
                postsUtil.deleteFile("/imagesFolder/" + filename);
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при удалении поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при удалении поста с id: " + postId + ": " + e.getMessage());
        }
    }

    /// Редактирование своего поста (PUT)
    public String updatePost(int postId, PostRequest postRequest, String postOwnerUsername){
        // Если текущий пользователь является автором поста
        PostDTO existingPost = getPostById(postId);
        if(!existingPost.getOwnerUsername().equals(postOwnerUsername)){
            throw new RuntimeException("У вас нет такого поста");
        }

        List<String> uploadedImagesUrls = new ArrayList<>();

        // Если список фотографий в запросе на обновление не пустой
        if (postRequest.getImages() != null && !postRequest.getImages().isEmpty()) {
            // То удаляем старые изображения с диска, если они есть
            if (existingPost.getImagesUrls() != null && !existingPost.getImagesUrls().isEmpty()) {
                for(String filename : existingPost.getImagesUrls()){
                    try {
                        postsUtil.deleteFile("/imagesFolder/" + filename);
                    } catch (Exception e){
                        log.warn("Ошибка удаления старого изображения {}: {}", filename, e.getMessage());
                        throw new RuntimeException("Ошибка удаления изображения");
                    }
                }
            }

            // И кидаем на диск новые изображения из запроса, сохраняя уникальное название для передачи в registry-service
            uploadedImagesUrls = postsUtil.uploadImages(postRequest.getImages());
        }

        // Формируем DTO для отправки
        PostUpdateDTO postUpdateDTO = PostUpdateDTO.builder()
                .text(postRequest.getText())
                .tags(postRequest.getTags())
                .imagesUrls(uploadedImagesUrls)
                .ownerUsername(postOwnerUsername)
                .build();

        // Складываем DTO в JSON для передачи в registry-service
        String requestMessage;
        try {
            requestMessage = objectMapper.writeValueAsString(postUpdateDTO);
        } catch (JsonProcessingException e){
            log.warn("Ошибка сериализации данных при обновлении поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных");
        }

        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса на обновление поста, JSON: {}", requestMessage);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при обновлении поста по id {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при отправке запроса в registry-service на обновление поста: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обновлении поста: " + e.getMessage());
        }
    }

    /// Получение всех постов
    public List<PostDTO> getAllPosts(){
        String url = REGISTRY_SERVICE_URL + "/api/posts";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {} // Чтобы exchange мог обрабатывать генерк-класс List
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении всех постов: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            // Подгрузка изображений для каждого поста с переводом изображения в base64
            for(PostDTO post : posts){
                // Если список уникальных имен фотографий не пустой
                if (!post.getImagesUrls().isEmpty()) {
                    // То загружаем изображения и добавляем их в DTO в формате Base64
                    List<String> imagesBase64 = new ArrayList<>();
                    for (String imageName : post.getImagesUrls()) {
                        String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                        imagesBase64.add(base64Image);
                    }
                    post.setImagesBase64(imagesBase64);
                }
            }

            return posts;
        } catch (Exception e){
            log.warn("Ошибка при получении всех постов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получениии всех постов: " + e.getMessage());
        }
    }

    /// Получение постов конкретного пользователя
    public List<PostDTO> getPostsByAuthor(String authorUsername){
        String url = REGISTRY_SERVICE_URL + "/api/posts/user/" + authorUsername;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<PostDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении постов автора {}: {}", authorUsername, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                // Если список уникальных имен фотографий не пустой
                if (!post.getImagesUrls().isEmpty()) {
                    // То загружаем изображения и добавляем их в DTO в формате Base64
                    List<String> imagesBase64 = new ArrayList<>();
                    for (String imageName : post.getImagesUrls()) {
                        String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                        imagesBase64.add(base64Image);
                    }
                    post.setImagesBase64(imagesBase64);
                }
            }

            return posts;
        } catch (Exception e){
            log.warn("Ошибка при получении постов пользователя {}: {}", authorUsername, e.getMessage());
            throw new RuntimeException("Ошибка при получении постов пользователя " + authorUsername + ": " + e.getMessage());
        }
    }

    /// Получение постов подписок
    public List<PostDTO> getSubscriptionsPosts(String username){
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

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении постов подписок: {}", response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                // Если список уникальных имен фотографий не пустой
                if (!post.getImagesUrls().isEmpty()) {
                    // То загружаем изображения и добавляем их в DTO в формате Base64
                    List<String> imagesBase64 = new ArrayList<>();
                    for (String imageName : post.getImagesUrls()) {
                        String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                        imagesBase64.add(base64Image);
                    }
                    post.setImagesBase64(imagesBase64);
                }
            }

            return posts;
        } catch (Exception e){
            log.warn("Ошибка при получении постов подписок польователя {}: {}", username, e.getMessage());
            throw new RuntimeException("Ошибка при получении постов подписок пользователя " + username + ": " + e.getMessage());
        }
    }

    /// Поиск постов по тэгам
    public List<PostDTO> getPostsByTags(List<String> tags){
        String url = REGISTRY_SERVICE_URL + "/api/posts/search-by-tags?tags=" + String.join(",", tags);

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

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении постов по тэгам {}: {}", tags, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                // Если список уникальных имен фотографий не пустой
                if (!post.getImagesUrls().isEmpty()) {
                    // То загружаем изображения и добавляем их в DTO в формате Base64
                    List<String> imagesBase64 = new ArrayList<>();
                    for (String imageName : post.getImagesUrls()) {
                        String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                        imagesBase64.add(base64Image);
                    }
                    post.setImagesBase64(imagesBase64);
                }
            }

            return posts;
        } catch (Exception e){
            log.warn("Ошибка при получении постов по тэгам: {}: {}", tags, e.getMessage());
            throw new RuntimeException("Ошибка при получении постов по тэгам " + tags + ": " + e.getMessage());
        }
    }

    /// Получение постов по тексту
    public List<PostDTO> getPostsByText(String text){
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

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении постов по тексту {}: {}", text, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            List<PostDTO> posts = response.getBody();
            if (posts == null) return Collections.emptyList();

            for(PostDTO post : posts){
                // Если список уникальных имен фотографий не пустой
                if (!post.getImagesUrls().isEmpty()) {
                    // То загружаем изображения и добавляем их в DTO в формате Base64
                    List<String> imagesBase64 = new ArrayList<>();
                    for (String imageName : post.getImagesUrls()) {
                        String base64Image = postsUtil.downloadAndEncodeImage(imageName);
                        imagesBase64.add(base64Image);
                    }
                    post.setImagesBase64(imagesBase64);
                }
            }

            return posts;
        } catch (Exception e){
            log.warn("Ошибка при получении постов по тексту: {}: {}", text, e.getMessage());
            throw new RuntimeException("Ошибка при получении постов по тексту " + text + ": " + e.getMessage());
        }
    }


}
