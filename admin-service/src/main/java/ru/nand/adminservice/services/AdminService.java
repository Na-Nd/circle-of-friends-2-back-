package ru.nand.adminservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.adminservice.entities.DTO.UserDTO;
import ru.nand.adminservice.utils.JwtUtil;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    /// Получение списка аккаунтов с заблокированными сессиями (типа подозрительная активность)
    public List<UserDTO> getBlockedAccounts(){
        String url = REGISTRY_SERVICE_URL + "/api/users/blocked-accounts";
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
                log.warn("Неуспешный ответ от registry-service при получении списка аккаунтов с заблокированными сессиями");
                throw new RuntimeException("Неуспешный ответ от registry-service при получении списка аккаунтов с заблокированными сессиями");
            }

            return objectMapper.readValue(response.getBody(), new TypeReference<List<UserDTO>>() {});
        } catch (Exception e){
            log.warn("Ошибка при получении списка аккаунтов с заблокированными сессиями: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении списка аккаунтов с заблокированными сессиями: " + e.getMessage());
        }
    }

    /// Блокировка аккаунта пользователя
    public String blockUser(int userId){
        String url = REGISTRY_SERVICE_URL + "/api/users/ban/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if(!response.getStatusCode().is2xxSuccessful()){
                log.warn("Неуспешный ответ от registry-service при блокировке аккаунта пользователя {}", userId);
                throw new RuntimeException("Неуспешный ответ от registry-service при получении списка аккаунтов с заблокированными сессиями");
            }

            return response.getBody();
        } catch (Exception e){
            log.warn("Ошибка при блокировке аккаунта пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при блокировке аккаунта пользователя: " + e.getMessage());
        }
    }
}
