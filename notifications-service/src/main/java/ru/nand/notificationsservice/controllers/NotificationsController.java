package ru.nand.notificationsservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.notificationsservice.entities.DTO.NotificationDTO;
import ru.nand.notificationsservice.services.NotificationService;
import ru.nand.notificationsservice.utils.JwtUtil;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    @Autowired
    public NotificationsController(JwtUtil jwtUtil, NotificationService notificationService) {
        this.jwtUtil = jwtUtil;
        this.notificationService = notificationService;
    }

    /// Получить уведомления текущего пользователя
    @GetMapping
    public ResponseEntity<?> getAllNotifications(@RequestHeader(name = "Authorization") String accessToken) {
        try {
            accessToken = accessToken.substring(7);

            List<NotificationDTO> userNotifications = notificationService.getAllNotificationsForCurrentUser(accessToken);
            return ResponseEntity.status(200).body(userNotifications);
        } catch (Exception e) {
            log.error("Ошибка при получении списка уведомлений для пользователя {}: {}", jwtUtil.extractUsername(accessToken), e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при получении уведомлений: " + e.getMessage());
        }
    }
}
