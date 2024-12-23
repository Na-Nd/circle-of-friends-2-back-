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

    private static final String SECRET_KEY = "myinterservicekey";
    private static final String TOKEN_VALUE = "myTokenValue";
    private static final String HEADER_NAME = "account-user-service";
    private static final String REGISTRY_SERVICE_URL = "http://localhost:8081";

    @Autowired
    public TokenRefreshClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String refreshToken(String expiredToken) {
        String url = REGISTRY_SERVICE_URL + "/api/auth/refresh-token";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + expiredToken);
        headers.set(HEADER_NAME, KeyGenerator.generateKey(SECRET_KEY, TOKEN_VALUE));

        String secretKey = KeyGenerator.generateKey("myKey", expiredToken);
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

//    public String refreshToken (String expiredToken){
//        String url = "http://localhost:8081/api/auth/refresh-token";
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + expiredToken);
//
//        String secretKey = KeyGenerator.generateKey("myKey", expiredToken);
//        headers.set("X-SECRET-KEY", secretKey);
//        System.out.println("Получился secretKey: " + secretKey);
//        System.out.println("expiredToken: " + expiredToken);
//        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
//
//        try{
//            ResponseEntity<String> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.POST,
//                    requestEntity,
//                    String.class
//            );
//            return response.getBody();
//        } catch (HttpClientErrorException e){
//            log.error("Ошибка при обновлении токена: {}", e.getStatusCode());
//            throw new RuntimeException("Пользователь заблокирован или токен недействителен");
//        }
//    }
}
