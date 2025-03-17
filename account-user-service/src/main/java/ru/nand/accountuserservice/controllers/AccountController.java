package ru.nand.accountuserservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import ru.nand.accountuserservice.entities.DTO.UserDTO;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.accountuserservice.services.AccountService;

import java.util.List;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {
    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // TODO: deleteme
    /// Тестовый эндпоинт
    @GetMapping
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

    /// Получить аккаунта конкретного пользователя
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try{
            log.info("Пользовательский запрос на получение данных об аккаунта пользователя {}", username);
            return ResponseEntity.status(200).body(accountService.getUserByUsername(username));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body("Пользователь не найден");
        }
    }

    /// Получение списка всех username'ов пользователей
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(){
        try{
            List<String> usernames = accountService.getAllUsernames();
            log.info("Был получен список пользователей");
            return ResponseEntity.status(200).body(usernames);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: "+ e.getMessage());
        }

    }

    /// Изменение аккаунта
    @PatchMapping("/edit")
    public ResponseEntity<String> editAccount(@AuthenticationPrincipal UserDetails userDetails, @RequestBody AccountPatchRequest accountPatchRequest) {
        try{
            log.info("Пользовательский запрос на изменение данных аккаунта {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(accountService.editAccount(userDetails.getUsername(), accountPatchRequest));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при изменении данных аккаунта");
        }
    }

    /// Подтверждение почты для изменения
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String code,
            @RequestHeader("Authorization") String authHeader
    ) {
        try{
            log.info("Пользовательский запрос на подтверждение почты {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(accountService.verifyEmail(userDetails.getUsername(), code, authHeader));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при подтверждении почты");
        }
    }

    /// Удаление аккаунта
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        try{
            accountService.deleteAccount(userDetails.getUsername());
            log.info("Удаление аккаунта пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body("Аккаунт пользователя " + userDetails.getUsername() + " удален");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления аккаунта");
        }
    }

    /// Получение списка подписчиков текущего пользователя
    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers(@AuthenticationPrincipal UserDetails userDetails){
        try {
            List<String> followers = accountService.getFollowers(userDetails.getUsername());
            log.info("Получен список подписчиков пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(followers);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка получения списка подписчиков пользователя");
        }
    }

    /// Получение списка подписок текущего пользователя
    @GetMapping("/following")
    public ResponseEntity<?> getFollowing(@AuthenticationPrincipal UserDetails userDetails){
        try{
            List<String> following = accountService.getFollowing(userDetails.getUsername());
            log.info("Получен список подписок пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(following);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка получения списка подписок пользователя");
        }
    }

    /// Подписка на пользователя
    @PostMapping("/follow/{targetUsername}")
    public ResponseEntity<String> followUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String targetUsername) {
        try{
            log.info("Запрос пользователя {} подписаться на {}", userDetails.getUsername(), targetUsername);
            accountService.followUser(userDetails.getUsername(), targetUsername);

            return ResponseEntity.status(200).body("Теперь вы подписаны на пользователя " + targetUsername);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка подписки на пользователя");
        }
    }

    /// Отписка от пользователя
    @DeleteMapping("/follow/{targetUsername}")
    public ResponseEntity<String> unfollowUser(@PathVariable String targetUsername, @AuthenticationPrincipal UserDetails userDetails) {
        try{
            log.info("Запрос пользователя {} на отписку от {}", userDetails.getUsername(), targetUsername);
            accountService.unfollowUser(userDetails.getUsername(), targetUsername);

            return ResponseEntity.status(200).body("Вы отписались от пользователя " + targetUsername);
        } catch (Exception e){
            log.error("Ошибка при отписке: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка отписки от пользователя");
        }
    }

}
