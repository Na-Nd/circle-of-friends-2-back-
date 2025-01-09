package ru.nand.notificationsservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailSenderService {

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

    @Autowired
    public MailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMail(String toEmail, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(toEmail);
        message.setText(text);
        message.setSubject(subject);

        log.debug("Отправил сообщение: {}", message);
        mailSender.send(message);
        // TODO Накатить логи и проверку токена как в Account-User-Service, сделать такой же тестовый эндпоинт: достать из токена eamil пользователя и кинуть ему приветственное сообщение
    }
}
