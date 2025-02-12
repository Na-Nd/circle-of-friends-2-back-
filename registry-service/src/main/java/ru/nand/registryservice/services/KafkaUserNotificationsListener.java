package ru.nand.registryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.Notification;
import ru.nand.registryservice.entities.User;

import java.time.LocalDateTime;

@Slf4j
@Service
public class KafkaUserNotificationsListener {
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public KafkaUserNotificationsListener(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "notifications-registry-topic", groupId = "registry-group")
    public void handleUserNotification(String message) {

        NotificationDTO notificationDTO;
        try{
            notificationDTO = new ObjectMapper().readValue(message, NotificationDTO.class);
        } catch (JsonProcessingException e){
            log.error("Ошибка десериализации сообщения: {}", e.getMessage());
            return;
        }
        log.info("Получено уведомление для пользователя: {}", notificationDTO.getUserEmail());

        User user = userService.findByEmail(notificationDTO.getUserEmail());
        if(user != null) {
            // мб modelMapper
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setText(notificationDTO.getMessage());
            notification.setCreationDate(LocalDateTime.now());

            notificationService.save(notification);
        }

    }
}
