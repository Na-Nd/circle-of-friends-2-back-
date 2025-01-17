package ru.nand.notificationsservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nand.notificationsservice.services.MailSenderService;
import ru.nand.notificationsservice.utils.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private final JwtUtil jwtUtil;
    private final MailSenderService mailSenderService;

    @Autowired
    public NotificationsController(JwtUtil jwtUtil, MailSenderService mailSenderService) {
        this.jwtUtil = jwtUtil;
        this.mailSenderService = mailSenderService;
    }

    @GetMapping
    public String test(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        if(userDetails != null){
            String username = userDetails.getUsername();

            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("NO_ROLE");

            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;

            String email = jwtUtil.extractEmail(token);

            log.debug("Пользователь: {}, Роль: {}, Email: {}", username, role, email);

            String subject = "Welcome to Notification Service";
            String text = "Hello from notifications-service, " + username + "!";
            mailSenderService.sendMail(email, subject, text);

            return String.format("Hello, %s! Your role is %s, and your email is %s.", username, role, email);
        } else {
            log.error("Пользователь не найден");
            return "Пользователь не найден";
        }
    }
}
