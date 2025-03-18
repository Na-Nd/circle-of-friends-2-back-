package ru.nand.registryservice.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.DTO.UserDTO;
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

    /// Получение данных аккаунта конкретного пользователя
    @GetMapping("/{username}")
    public ResponseEntity<UserDTO> getFollowerCount(@PathVariable String username) {
        try {
            log.info("Запрос от account-user-service на получение данных аккаунта пользователя {}", username);
            return ResponseEntity.status(200).body(userService.getUserByUsername(username));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение username'ов подписчиков пользователя
    @GetMapping("/{username}/followers")
    public ResponseEntity<List<String>> getFollowers(@PathVariable String username) {
        try {
            log.info("Запрос от account-user-service на получение подписчиков пользователя {}", username);
            return ResponseEntity.ok(userService.getFollowers(username));
        } catch (Exception e) {
            log.error("Ошибка получения подписчиков пользователя {}: {}", username, e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение списка username'ов подписок пользователя
    @GetMapping("/{username}/following")
    public ResponseEntity<List<String>> getFollowing(@PathVariable String username) {
        try {
            log.info("Запрос от account-user-service на получение подписок пользователя {}", username);
            return ResponseEntity.ok(userService.getFollowing(username));
        } catch (Exception e) {
            log.error("Ошибка получения подписок пользователя {}: {}", username, e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Подписка пользователя current на пользователя target (возвращаем почту пользователя target)
    @PostMapping("/{currentUsername}/follow/{targetUsername}")
    public ResponseEntity<String> followUser(@PathVariable String currentUsername, @PathVariable String targetUsername) {
        try {
            log.info("Запрос от account-user-service на подписку пользователя {} на {}", currentUsername, targetUsername);
            return ResponseEntity.status(200).body(userService.followUser(currentUsername, targetUsername));
        } catch (Exception e) {
            log.error("Ошибка подписки пользователя {} на {}: {}", currentUsername, targetUsername, e.getMessage());
            return ResponseEntity.status(400).body("Ошибка подписки: " + e.getMessage());
        }
    }

    /// Отписка пользователя current от пользователя target
    @PostMapping("/{currentUsername}/unfollow/{targetUsername}")
    public ResponseEntity<String> unfollowUser(@PathVariable String currentUsername, @PathVariable String targetUsername){
        try{
            log.info("Запрос от account-user-service на отписку пользователя {} от {}", currentUsername, targetUsername);
            userService.unfollowUser(currentUsername, targetUsername);

            return ResponseEntity.status(200).body("Вы успешно отписались от " + targetUsername);
        } catch (Exception e){
            log.error("Ошибка отписки пользователя {} от {}: {}", currentUsername, targetUsername, e.getMessage());
            return ResponseEntity.status(400).body("Ошибка отписки: " + e.getMessage());
        }
    }

    /// Получение списка всех username'ов пользователей
    @GetMapping
    public ResponseEntity<List<String>> getAllUsernames() {
        try {
            log.info("Запрос от account-user-service на получение списка пользователей");
            return ResponseEntity.ok(userService.getAllUsernames());
        } catch (Exception e) {
            log.error("Ошибка получения списка пользователей: {}", e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Обновление данных аккаунта
    @PatchMapping("/edit")
    public ResponseEntity<String> patchUser(@RequestBody String message) {
        try {
            log.info("Запрос от account-user-service на обновление данных аккаунта");

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
            return ResponseEntity.status(200).body("Данные аккаунта успешно обновлены, новый access: " + newJwt);
        } catch (Exception e) {
            log.error("Ошибка обновления данных аккаунта: {}", e.getMessage());
            return ResponseEntity.status(400).body("Ошибка обновления данных аккаунта: " + e.getMessage());
        }
    }

    /// Удаление пользователя
    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username) {
        try {
            log.info("Запрос от account-user-service на удаление аккаунта {}", username);
            userService.deleteUser(username);
            return ResponseEntity.ok("Аккаунт успешно удалён");
        } catch (Exception e) {
            log.error("Ошибка удаления аккаунта {}: {}", username, e.getMessage());
            return ResponseEntity.status(400).body("Ошибка удаления аккаунта: " + e.getMessage());
        }
    }

    /// Проверка на существование пользователя c переданным id
    /// В случае существования вернет email пользователя
    @GetMapping("/{userId}/exists")
    public ResponseEntity<String> userExists(@PathVariable int userId) {
        try{
            log.info("Принял запрос от messages-service на существование id: {}", userId);
            return ResponseEntity.status(200).body(userService.existsById(userId));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    /// Проверка на существование пользователей с переданным сетом id
    /// В случае существования всех пользователей вернет их почты
    @PostMapping("/find-users")
    public ResponseEntity<String> findUsers(@RequestBody String requestMessage){
        try{
            log.info("Запрос от group-chats-service на проверку на существование пользователей");
            return ResponseEntity.status(200).body(userService.findUsers(requestMessage));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Пользователи не найдены");
        }
    }

}
