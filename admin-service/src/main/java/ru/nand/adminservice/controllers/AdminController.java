package ru.nand.adminservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.adminservice.entities.DTO.UserDTO;
import ru.nand.adminservice.services.AdminService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /// Тестовый эндпоинт
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

    /// Получение списка аккаунтов с заблокированными сессиями (типа подозрительная активность)
    @GetMapping("/blocked-accounts")
    public ResponseEntity<List<UserDTO>> getBlockedAccounts(){
        try{
            log.info("Запрос администратора на получение списка аккаунтов с подозрительной активностью");
            return ResponseEntity.status(200).body(adminService.getBlockedAccounts());
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
