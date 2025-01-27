package ru.nand.notificationsservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.notificationsservice.entities.Notification;
import ru.nand.notificationsservice.utils.JwtUtil;

import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }


    public List<Notification> getAllNotificationsForCurrentUser(String username) throws RuntimeException{
        String url = REGISTRY_SERVICE_URL + "/api/notifications?username=" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try{
            ResponseEntity<Notification[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Notification[].class
            );
            return List.of(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException(e); // Передадим исключение и поймаем в контроллере
        }
    }
}
