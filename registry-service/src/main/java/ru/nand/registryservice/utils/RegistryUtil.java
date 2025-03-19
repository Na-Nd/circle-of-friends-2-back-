package ru.nand.registryservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.nand.registryservice.entities.DTO.AnalyticsService.CreatedAccountsDTO;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.DTO.UserDTO;
import ru.nand.registryservice.entities.User;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RegistryUtil {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RegistryUtil(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
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

    /// Обогащение списка из UserDTO
    public String enrichUserDTOs(List<User> topUsers) throws JsonProcessingException {
        List<UserDTO> userDTOs = topUsers.stream()
                .map(user -> UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .subscribersCount(user.getSubscribers().size())
                        .subscriptionsCount(user.getSubscriptions().size())
                        .postsCount(user.getPosts().size())
                        .build())
                .collect(Collectors.toList());

        CreatedAccountsDTO createdAccountsDTO = CreatedAccountsDTO.builder()
                .accountsCount(userDTOs.size())
                .createdAccounts(userDTOs)
                .build();

        return objectMapper.writeValueAsString(createdAccountsDTO);
    }
}
