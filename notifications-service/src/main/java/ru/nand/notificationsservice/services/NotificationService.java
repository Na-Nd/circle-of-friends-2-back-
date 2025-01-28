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
import ru.nand.notificationsservice.entities.DTO.NotificationDTO;
import ru.nand.notificationsservice.entities.Notification;
import ru.nand.notificationsservice.utils.JwtUtil;

import java.util.Arrays;
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


    public List<NotificationDTO> getAllNotificationsForCurrentUser(String username) {
        String url = REGISTRY_SERVICE_URL + "/api/notifications?username=" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<NotificationDTO[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, NotificationDTO[].class);
        return Arrays.asList(response.getBody());
    }
}
