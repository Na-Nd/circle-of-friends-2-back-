package ru.nand.registryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.nand.registryservice.entities.DTO.NotificationDTO;
import ru.nand.registryservice.entities.Notification;
import ru.nand.registryservice.repositories.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper; // Чтобы преобразовать сущность в DTO

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    public void save(Notification notification) {
        log.debug("Сохранение уведомления {} в БД", notification);
        notificationRepository.save(notification);
    }

    public List<NotificationDTO> getNotificationsByUsername(String username) {
        // Получим уведомления и преобразуем их в DTO
        log.debug("Получение уведомлений для пользователя {}", username);
        return notificationRepository.findByUserUsername(username)
                .stream()
                .map(notification -> modelMapper.map(notification, NotificationDTO.class))
                .collect(Collectors.toList());
    }
}
