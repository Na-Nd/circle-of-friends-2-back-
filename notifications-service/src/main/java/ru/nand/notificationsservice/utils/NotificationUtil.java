package ru.nand.notificationsservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.nand.notificationsservice.services.NotificationService;

@Slf4j
@Component
public class NotificationUtil {
    private final NotificationService notificationService;

    @Autowired
    public NotificationUtil(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 48*60*60*1000) // Раз в два дня
    public void clearNotifications(){
        log.info("Запуск отчистки старых уведомлений");
        long count = notificationService.clearOldNotifications();
        log.debug("Отчистка завершена, отчищено {} уведомлений", count);
    }

}
