package ru.nand.notificationsservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.notificationsservice.entities.DTO.NotificationDTO;
import ru.nand.notificationsservice.entities.Notification;
import ru.nand.notificationsservice.entities.NotificationKey;
import ru.nand.notificationsservice.repositories.NotificationRepository;
import ru.nand.notificationsservice.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {
    private final JwtUtil jwtUtil;
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(JwtUtil jwtUtil, NotificationRepository notificationRepository) {
        this.jwtUtil = jwtUtil;
        this.notificationRepository = notificationRepository;
    }

    public void saveNotification(NotificationDTO notificationDTO) {
        Notification notification = Notification.builder()
                .key(new NotificationKey(notificationDTO.getUserEmail(), UUID.randomUUID()))
                .message(notificationDTO.getMessage())
                .ownerUsername(notificationDTO.getOwnerUsername())
                .creationDate(LocalDateTime.now())
                .build();

        log.debug("Сохранил уведомление: {}", notification);
        notificationRepository.save(notification);
    }

    public List<NotificationDTO> getAllNotificationsForCurrentUser(String accessToken){
        String userEmail = jwtUtil.extractEmail(accessToken);
        log.debug("Извлек почту: {}", userEmail);

        return notificationRepository.findByKeyUserEmail(userEmail).stream()
                .map(notification -> new NotificationDTO(
                        notification.getKey().getUserEmail(),
                        notification.getMessage(),
                        notification.getOwnerUsername(),
                        notification.getCreationDate()
                ))
                .collect(Collectors.toList());

    }

    public long clearOldNotifications(){
        // Получаем старые уведомления, порог - неделя
        List<Notification> oldNotifications = notificationRepository.findOldNotifications(LocalDateTime.now().minusDays(7));
        log.debug("Найдено старых уведомлений: {}", oldNotifications.size());

        notificationRepository.deleteAll(oldNotifications);

        return oldNotifications.size();
    }
}
