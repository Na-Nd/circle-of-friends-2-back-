package ru.nand.registryservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.services.UserService;
import ru.nand.registryservice.entities.DTO.AccountPatchDTO;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class AccountUserController {
    private final UserService userService;

    @Autowired
    public AccountUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}/followers/count")
    public ResponseEntity<Integer> getFollowerCount(@PathVariable String username) {
        try {
            log.info("Запрос на получение количества подписчиков пользователя {}", username);
            return ResponseEntity.ok(userService.getFollowersCount(username));
        } catch (Exception e) {
            log.error("Ошибка получения количества подписчиков пользователя {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<String>> getFollowers(@PathVariable String username) {
        try {
            log.info("Запрос на получение подписчиков пользователя {}", username);
            return ResponseEntity.ok(userService.getFollowers(username));
        } catch (Exception e) {
            log.error("Ошибка получения подписчиков пользователя {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<String>> getFollowing(@PathVariable String username) {
        try {
            log.info("Запрос на получение подписок пользователя {}", username);
            return ResponseEntity.ok(userService.getFollowing(username));
        } catch (Exception e) {
            log.error("Ошибка получения подписок пользователя {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{currentUsername}/follow/{targetUsername}")
    public ResponseEntity<String> followUser(@PathVariable String currentUsername, @PathVariable String targetUsername) {
        try {
            log.info("Запрос на подписку пользователя {} на {}", currentUsername, targetUsername);
            userService.followUser(currentUsername, targetUsername);
            return ResponseEntity.ok("Подписка успешно оформлена");
        } catch (Exception e) {
            log.error("Ошибка подписки пользователя {} на {}: {}", currentUsername, targetUsername, e.getMessage());
            return ResponseEntity.status(500).body("Ошибка подписки: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllUsernames() {
        try {
            log.info("Запрос на получение списка пользователей");
            return ResponseEntity.ok(userService.getAllUsernames());
        } catch (Exception e) {
            log.error("Ошибка получения списка пользователей: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PatchMapping("/edit")
    public ResponseEntity<String> patchUser(@RequestBody String message) {
        try {
            log.info("Запрос на обновление данных аккаунта");

            // Десериализация входящего сообщения
            AccountPatchDTO accountPatchDTO;
            try {
                accountPatchDTO = new ObjectMapper().readValue(message, AccountPatchDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Ошибка десериализации данных: {}", e.getMessage());
                return ResponseEntity.status(400).body("Некорректный формат данных");
            }

            // Обновление данных пользователя
            String newJwt = userService.updateUser(accountPatchDTO);
            return ResponseEntity.ok("Данные аккаунта успешно обновлены, новый JWT: " + newJwt);
        } catch (Exception e) {
            log.error("Ошибка обновления данных аккаунта: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка обновления данных аккаунта: " + e.getMessage());
        }
    }


    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        try {
            log.info("Запрос на удаление аккаунта {}", username);
            userService.deleteUser(username);
            return ResponseEntity.ok("Аккаунт успешно удалён");
        } catch (Exception e) {
            log.error("Ошибка удаления аккаунта {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления аккаунта: " + e.getMessage());
        }
    }
}
