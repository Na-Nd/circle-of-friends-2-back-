package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.Notification;
import ru.nand.registryservice.services.NotificationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationsUserController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationsUserController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotificationsForUser(@RequestParam String username) {
        try {
            List<NotificationDTO> notifications = notificationService.getNotificationsByUsername(username);
            System.out.println(notifications.getFirst().toString());// Как выглядит //TODO deleteme
            return ResponseEntity.status(200).body(notifications);
        } catch (Exception e) {
            log.error("Ошибка при получении уведомлений для пользователя {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
