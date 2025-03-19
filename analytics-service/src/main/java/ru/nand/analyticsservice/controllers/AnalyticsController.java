package ru.nand.analyticsservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.analyticsservice.entities.DTO.CreatedAccountsDTO;
import ru.nand.analyticsservice.entities.DTO.CreatedPostsDTO;
import ru.nand.analyticsservice.services.AnalyticsService;

@Slf4j
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    ///Тестовый эндпоинт
    @GetMapping("/test")
    public String test(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            String username = userDetails.getUsername();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("NO_ROLE");

            log.debug("Пользователь: {}, Роль: {}", username, role);

            return String.format("Hello, %s! Your role is %s.", username, role);
        } else {
            log.error("Пользователь не найден");
            return "Пользователь не найден";
        }
    }

    /// Получение списка созданных аккаунтов за последние N часов
    @GetMapping("/created-accounts/{hoursCount}")
    public ResponseEntity<CreatedAccountsDTO> getCreatedAccounts(@PathVariable int hoursCount){
        try{
            log.info("Запрос менеджера на получение списка созданных аккаунтов за последние {} часа (ов)", hoursCount);
            return ResponseEntity.status(200).body(analyticsService.getCreatedAccounts(hoursCount));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение N аккаунтов с наибольшим количеством подписчиков (количество упорядочено от большего к меньшему)
    @GetMapping("/popular-accounts/{accountsCount}")
    public ResponseEntity<CreatedAccountsDTO> getPopularAccounts(@PathVariable int accountsCount){
        try{
            log.info("Запрос менеджера на получение списка популярных аккаунтов");
            return ResponseEntity.status(200).body(analyticsService.getPopularAccounts(accountsCount));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение списка созданных постов за последние N часов
    @GetMapping("/created-posts/{hoursCount}")
    public ResponseEntity<CreatedPostsDTO> getCreatedPosts(@PathVariable int hoursCount){
        try{
            log.info("Запрос менеджера на получение списка созданных постов за последние {} часа (ов)", hoursCount);
            return ResponseEntity.status(200).body(analyticsService.getCreatedPosts(hoursCount));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }


}
