package ru.nand.groupchatsservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.nand.groupchatsservice.entities.DTO.NotificationDTO;


@Slf4j
@Component
public class GroupChatsUtil {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroupChatsUtil(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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
