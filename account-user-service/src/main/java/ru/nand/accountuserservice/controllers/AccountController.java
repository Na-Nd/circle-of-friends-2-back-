package ru.nand.accountuserservice.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/account")
@Slf4j
public class AccountController {

    @GetMapping
    public String test(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            String username = userDetails.getUsername();
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("NO_ROLE");

            log.info("Пользователь: {}, Роль: {}", username, role);

            return String.format("Hello, %s! Your role is %s.", username, role);
        } else {
            log.error("Пользователь не найден");
            return "User not found";
        }
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/admin")
    public String adminAccess(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        log.info("Администратор: {} получил доступ", username);

        return String.format("Welcome, admin %s!", username);
    }
}
