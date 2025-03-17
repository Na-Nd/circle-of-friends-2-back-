package ru.nand.authservice.services;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entities.DTO.RegisterDTO;

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
    }

    public void sendVerificationMail(RegisterDTO registerDTO, String verificationCode){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(registerDTO.getEmail());
        message.setText("Здравствуйте, " + registerDTO.getUsername() + ", ваш код верификации: " + verificationCode);
        message.setSubject("Код верификации");

        log.debug("Отправил сообщение верификации: {}", message);
        mailSender.send(message);
    }
}