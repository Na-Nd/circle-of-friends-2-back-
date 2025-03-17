package ru.nand.notificationsservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.nand.notificationsservice.entities.DTO.NotificationDTO;

@Slf4j
@Service
public class KafkaNotificationsListener {
    private final MailSenderService mailSenderService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @Autowired
    public KafkaNotificationsListener(MailSenderService mailSenderService, ObjectMapper objectMapper, NotificationService notificationService) {
        this.mailSenderService = mailSenderService;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "user-notifications-topic", groupId = "notifications-group")
    public void handleNotification(String message) throws JsonProcessingException {
        log.info("В сервис пришло уведомление");
        NotificationDTO notificationDTO = objectMapper.readValue(message, NotificationDTO.class);

        log.info("Отправка уведомления на почту пользователю {}", notificationDTO.getUserEmail());

        mailSenderService.sendMail(notificationDTO.getUserEmail(), "Уведомление от COF-2", notificationDTO.getMessage());

        notificationService.saveNotification(notificationDTO);
    }
}
