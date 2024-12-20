package ru.nand.accountuserservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public TokenRefreshClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String refreshToken (String expiredToken){
        String url = "http://localhost:8081/api/auth/refresh-token";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + expiredToken);

        // TODO: заменить на ключ из env
        String secretKey = KeyGenerator.generateKey("myKey", expiredToken);
        headers.set("X-SECRET-KEY", secretKey);
        System.out.println("Получился secretKey: " + secretKey);
        System.out.println("expiredToken: " + expiredToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e){
            log.error("Ошибка при обновлении токена: {}", e.getStatusCode());
            throw new RuntimeException("Пользователь заблокирован или токен недействителен");
        }
    }
}
