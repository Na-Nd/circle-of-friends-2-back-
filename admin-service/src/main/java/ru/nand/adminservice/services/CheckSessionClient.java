package ru.nand.adminservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.adminservice.utils.JwtUtil;

@Slf4j
@Service
public class CheckSessionClient {
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Autowired
    public CheckSessionClient(JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    /// Проверка активности сессии по access токену пользователя
    public boolean isSessionActive(String accessToken){
        log.info("Проверка сессии по access токену");
        String url = REGISTRY_SERVICE_URL + "/api/session/active";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Запрос к registry-service для проверки статуса сессии");
        try{
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Boolean.class
            );

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e){
            log.error("Ошибка при проверке статуса сессии: {}", e.getMessage());
            throw new RuntimeException("Ошибка при проверке статуса сессии: " + e.getMessage());
        }
    }
}
