package ru.nand.analyticsservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.analyticsservice.entities.DTO.CreatedAccountsDTO;
import ru.nand.analyticsservice.entities.DTO.CreatedPostsDTO;
import ru.nand.analyticsservice.utils.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Получение списка созданных аккаунтов за последние N часов
    public CreatedAccountsDTO getCreatedAccounts(int hoursCount){
        String url = REGISTRY_SERVICE_URL + "/api/users/created-accounts/" + hoursCount;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении списка созданных аккаунтов");
                throw new RuntimeException("Неуспешный ответ от registry-service при получении списка созданных аккаунтов");
            }

            return objectMapper.readValue(response.getBody(), CreatedAccountsDTO.class);
        } catch (Exception e){
            log.warn("Ошибка при получении списка созданных аккаунтов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении списка созданных аккаунтов: " + e.getMessage());
        }
    }

    /// Получение N аккаунтов с наибольшим количеством подписчиков (количество упорядочено от большего к меньшему)
    public CreatedAccountsDTO getPopularAccounts(int accountsCount){
        String url = REGISTRY_SERVICE_URL + "/api/users/popular-accounts/" + accountsCount;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении списка популярных аккаунтов");
                throw new RuntimeException("Неуспешный ответ от registry-service при получении списка популярных аккаунтов");
            }

            return objectMapper.readValue(response.getBody(), CreatedAccountsDTO.class);
        } catch (Exception e){
            log.warn("Ошибка при получении списка популярных аккаунтов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении списка популярных аккаунтов: " + e.getMessage());
        }
    }

    /// Получение списка созданных постов за последние N часов
    public CreatedPostsDTO getCreatedPosts(int hoursCount){
        String url = REGISTRY_SERVICE_URL + "/api/posts/created-posts/" + hoursCount;
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

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при получении списка постов");
                throw new RuntimeException("Неуспешный ответ от registry-service при получении списка постов");
            }

            return objectMapper.readValue(response.getBody(), CreatedPostsDTO.class);
        } catch (Exception e){
            log.warn("Ошибка при получении списка постов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении списка популярных аккаунтов: " + e.getMessage());
        }
    }
}
