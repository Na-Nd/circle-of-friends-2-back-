package ru.nand.postsuserservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.postsuserservice.entities.DTO.UserDTO;
import ru.nand.postsuserservice.utils.JwtUtil;
import ru.nand.postsuserservice.utils.PostsUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostsLikesService {
    private final PostsUtil postsUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Лайк поста
    public String likePost(int postId, String username){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/like";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<String> requestEntity = new HttpEntity<>(username, headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при лайке поста с id {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при лайке поста с id {} пользователем {}: {}", postId, username, e.getMessage());
            throw new RuntimeException("Ошибка при лайке поста с id " + postId + " пользователем " + username + ": " + e.getMessage());
        }
    }

    /// Снятие лайка с поста
    public String unlikePost(int postId, String username){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/like";

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
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при снятии лайка с поста с id {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при снятии лайка с поста с id {} пользователем {}: {}", postId, username, e.getMessage());
            throw new RuntimeException("Ошибка при снятии лайка с поста с id " + postId + " пользователем " + username + ": " + e.getMessage());
        }
    }

     /// Получение списка пользователей, лайкнувших пост
    public List<UserDTO> getLikedUsers(int postId){
        String url = REGISTRY_SERVICE_URL + "/api/posts/" + postId + "/likes";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<List<UserDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при получении списка лайкнувших пользователей поста с id {}: {}", postId, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            return response.getBody();
        }catch (Exception e){
            log.warn("Ошибка при получении списка лайкнувших пользователей поста с id {}: {}", postId, e.getMessage());
            throw new RuntimeException("Ошибка при получении списка пользователей поста с id " + postId + ": " + e.getMessage());
        }
    }
}
