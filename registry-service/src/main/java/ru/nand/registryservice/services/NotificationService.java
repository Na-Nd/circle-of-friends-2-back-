package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.Notification;
import ru.nand.registryservice.repositories.NotificationRepository;

import java.util.List;

@Slf4j
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void save(Notification notification) {
        log.debug("Сохранение уведомления {} в БД", notification);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByUsername(String username) {
        log.debug("Получение уведомлений для пользователя {}", username);
        return notificationRepository.findByUserUsername(username);
    }
}
