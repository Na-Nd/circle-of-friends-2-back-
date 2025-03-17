package ru.nand.accountuserservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.nand.accountuserservice.entities.DTO.NotificationDTO;

@Slf4j
@Component
public class AccountUtil {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public AccountUtil(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    /// Отправка формирование уведомления и отправка в notifications-service посредством кафки
    public void sendNotification(String targetUserEmail, String notificationMessage){
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .userEmail(targetUserEmail)
                .message(notificationMessage)
                .build();

        try{
            String message = objectMapper.writeValueAsString(notificationDTO);
            kafkaTemplate.send("user-notifications-topic", message);
            log.debug("Уведомление отправлено в notifications-service");
        } catch (JsonProcessingException e) {
            log.error("Ошибка при сериализации уведомления: {}", e.getMessage());
        }
    }
}
