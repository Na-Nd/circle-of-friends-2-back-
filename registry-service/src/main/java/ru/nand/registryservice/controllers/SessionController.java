package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.registryservice.entities.ENUMS.STATUS;
import ru.nand.registryservice.entities.UserSession;
import ru.nand.registryservice.services.UserSessionService;

@Slf4j
@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final UserSessionService userSessionService;

    @Autowired
    public SessionController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @GetMapping("/active")
    public ResponseEntity<Boolean> isSessionActive(@RequestHeader(name = "Authorization") String accessToken) {
        log.info("Пришел запрос на проверку сессии для access токена");
        try {
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }

            // Поиск сессии по access токену
            UserSession session = userSessionService.getSessionByAccessToken(accessToken);

            // Проверка статуса
            boolean isActive = session.getStatus() == STATUS.ACTIVE;
            if (isActive) {
                log.info("Сессия активна");
            } else {
                log.info("Сессия неактивна");
            }

            return ResponseEntity.status(200).body(isActive);
        } catch (Exception e) {
            log.error("Ошибка при проверке активности сессии: {}", e.getMessage());
            return ResponseEntity.status(500).body(false);
        }
    }
}
