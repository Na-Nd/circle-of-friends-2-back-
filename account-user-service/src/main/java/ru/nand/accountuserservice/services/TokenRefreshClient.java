package ru.nand.accountuserservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchTaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.nand.sharedthings.utils.KeyGenerator;

@Slf4j
@Service
public class TokenRefreshClient {
    private final RestTemplate restTemplate;

    @Value("${interservice.secret.key}")
    private String SECRET_KEY;

    @Value("${myplug}")
    private String TOKEN_VALUE;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Value("${mysecret}")
    private String MY_SECRET;

    @Autowired
    public TokenRefreshClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String refreshToken(String expiredToken) {
        String url = REGISTRY_SERVICE_URL + "/api/auth/refresh-token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + expiredToken);
        headers.set(HEADER_NAME, KeyGenerator.generateKey(SECRET_KEY, TOKEN_VALUE));

        String secretKey = KeyGenerator.generateKey(MY_SECRET, expiredToken);
        headers.set("X-SECRET-KEY", secretKey);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Ошибка при обновлении токена: {}", e.getStatusCode());
            throw new RuntimeException("Пользователь заблокирован или токен недействителен");
        }
    }
}
