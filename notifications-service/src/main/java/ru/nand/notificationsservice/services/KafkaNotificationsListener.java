package ru.nand.notificationsservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.nand.notificationsservice.entities.DTO.NotificationDTO;

@Slf4j
@Service
public class KafkaNotificationsListener {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MailSenderService mailSenderService;

    @Autowired
    public KafkaNotificationsListener(KafkaTemplate<String, String> kafkaTemplate, MailSenderService mailSenderService) {
        this.kafkaTemplate = kafkaTemplate;
        this.mailSenderService = mailSenderService;
    }

    @KafkaListener(topics = "user-notifications-topic", groupId = "notifications-group")
    public void handleNotification(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        NotificationDTO notificationDTO = objectMapper.readValue(message, NotificationDTO.class);

        log.info("Отправка уведомления на почту {}", notificationDTO.getUserEmail());

        mailSenderService.sendMail(notificationDTO.getUserEmail(), "Регистрация аккаунта", notificationDTO.getMessage());

        // Теперь передаем уведомление в registry-service для сохранения
        kafkaTemplate.send("notifications-registry-topic", message);
    }
}
