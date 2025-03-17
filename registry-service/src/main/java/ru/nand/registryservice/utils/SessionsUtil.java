package ru.nand.registryservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.nand.registryservice.services.UserSessionService;

@Slf4j
@Component
public class SessionsUtil {

    private final UserSessionService userSessionService;

    @Autowired
    public SessionsUtil(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Scheduled(fixedRate = 24*60*60*1000) // 24 Часа
    public void cleanSessions() {
        log.info("Запуск очистки INACTIVE и REVOKED сессий");
        userSessionService.handlingInactiveAndRevokedSessions();
        log.debug("Отчистка сессий завершена");
    }

    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2 Часа
    public void checkActiveSessions() {
        log.info("Запуск проверки активных сессий");
        userSessionService.handleInactiveSessions();
        log.debug("Проверка активных сессий завершена");
    }
}
