package ru.nand.accountuserservice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;
import ru.nand.accountuserservice.services.AccountService;

import java.util.List;


// TODO: Попробовать try с ресурсами

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

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
            return "User not found";
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<String> getUserByUsername(@PathVariable String username) {
        try{
            int followersCont = accountService.getFollowersCount(username);
            log.info("Пользователь: {}. Количество подписчиков: {}", username, followersCont);
            return ResponseEntity.status(200).body("Пользователь: " + username + ", количество подписчиков: " + followersCont);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }

    }

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

    @PatchMapping("/edit")
    public ResponseEntity<String> editAccount(@AuthenticationPrincipal UserDetails userDetails, @RequestBody AccountPatchRequest accountPatchRequest) {
        try{
            accountService.patchAccount(accountPatchRequest, userDetails.getUsername()); // Передаем username текущего пользователя (пригодится на случай если пользователь захочет поменять свой текущий username)
            log.info("Обновление данных аккаунта пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body("Аккаунт пользователя " + userDetails.getUsername() + " успешно обновлен");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: Не удалось обновить данные аккаунта пользователя " + userDetails.getUsername() + ", ошибка: " + e.getMessage());
        }

    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        try{
            accountService.deleteAccount(userDetails.getUsername());
            log.info("Удаление аккаунта пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body("Аккаунт пользователя " + userDetails.getUsername() + " удален");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @GetMapping("/followers")
    public ResponseEntity<?> getFollowers(@AuthenticationPrincipal UserDetails userDetails){
        try {
            List<String> followers = accountService.getFollowers(userDetails.getUsername());
            log.info("Получен список подписчиков пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(followers);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @GetMapping("/following")
    public ResponseEntity<?> getFollowing(@AuthenticationPrincipal UserDetails userDetails){
        try{
            List<String> following = accountService.getFollowing(userDetails.getUsername());
            log.info("Получен список подписок пользователя {}", userDetails.getUsername());
            return ResponseEntity.status(200).body(following);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }

    @PostMapping("/follow/{targetUsername}")
    public ResponseEntity<String> followUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String targetUsername) {
        try{
            accountService.followUser(userDetails.getUsername(), targetUsername);
            log.info("Запрос пользователя {} подписаться на {}", userDetails.getUsername(), targetUsername);
            return ResponseEntity.status(200).body("Теперь вы подписаны на пользователя " + targetUsername);
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Внутренняя ошибка сервера: " + e.getMessage());
        }
    }
}
