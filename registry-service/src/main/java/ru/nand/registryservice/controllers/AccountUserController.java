package ru.nand.registryservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.registryservice.entities.User;
import ru.nand.registryservice.services.UserService;
import ru.nand.sharedthings.DTO.AccountPatchDTO;

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
        try{
            log.info("Запрос на получение количества подписчиков пользователя {}", username);
            return ResponseEntity.status(200).body(userService.getFollowersCount(username));
        } catch (Exception e){
            log.error("Ошибка получения количества подписчиков: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllUsernames(){
        try{
            log.info("Запрос на получение списка пользователей");
            return ResponseEntity.status(200).body(userService.getAllUsernames());
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PatchMapping("/edit")
    public ResponseEntity<String> patchUser(@RequestBody AccountPatchDTO accountPatchDTO){
        try{
            log.info("Запрос на обновление данных аккаунта");
            userService.updateUser(accountPatchDTO);
            return ResponseEntity.status(200).body("Данные аккаунта успешно обновлены");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка обновления данных аккаунта ");
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username){
        try {
            log.info("Запрос на удаление аккаунта");
            userService.deleteUser(username);
            return ResponseEntity.status(200).body("Аккаунт успешно удален");
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(500).body("Ошибка удаления аккаунта: " + e.getMessage());
        }
    }
}
