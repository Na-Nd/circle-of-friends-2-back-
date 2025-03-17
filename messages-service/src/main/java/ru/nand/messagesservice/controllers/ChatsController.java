package ru.nand.messagesservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.nand.messagesservice.entities.DTO.ChatDTO;
import ru.nand.messagesservice.entities.DTO.MessageDTO;
import ru.nand.messagesservice.services.ChatsService;
import ru.nand.messagesservice.utils.JwtUtil;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatsController {
    private final ChatsService chatsService;
    private final JwtUtil jwtUtil;

    @Autowired
    public ChatsController(ChatsService chatsService, JwtUtil jwtUtil) {
        this.chatsService = chatsService;
        this.jwtUtil = jwtUtil;
    }

    /// Создание чата
    @PostMapping("/create/{targetUserId}")
    public ResponseEntity<?> createChat(
            @PathVariable int targetUserId,
            @RequestHeader(name = "Authorization") String accessToken
    ){
        try{
            accessToken = accessToken.substring(7);

            log.info("Запрос на создание чата от пользователя {} с пользователем {}", jwtUtil.extractUserId(accessToken), targetUserId);
            ChatDTO chatDTO = chatsService.createChat(jwtUtil.extractUserId(accessToken), targetUserId);

            return ResponseEntity.status(200).body(chatDTO);
        } catch (RuntimeException e){
            log.error("Ошибка при создании чата: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при создании чата: " + e.getMessage());
        }
    }

    /// Отправка сообщения пользователю и создание чата с ним, если чат не создан
    @PostMapping("/{targetUserId}")
    public ResponseEntity<String> sendMessage(
            @PathVariable int targetUserId,
            @RequestBody MessageDTO messageDTO,
            @RequestHeader(name = "Authorization") String accessToken
    ) {
        try {
            accessToken = accessToken.substring(7);
            int senderId = jwtUtil.extractUserId(accessToken);

            log.info("Запрос на отправку сообщения от пользователя {} к пользователю {}", senderId, targetUserId);
            chatsService.sendMessage(senderId, targetUserId, messageDTO);

            return ResponseEntity.status(200).body("Сообщение отправлено");
        } catch (RuntimeException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    /// Получение сообщений по ID чата
    @GetMapping("/messages/{chatId}")
    public ResponseEntity<?> getMessagesByChatId(
            @PathVariable UUID chatId,
            @RequestHeader("Authorization") String accessToken
    ) {
        try {
            accessToken = accessToken.substring(7);
            int currentUserId = jwtUtil.extractUserId(accessToken);

            log.info("Запрос на получение сообщений для чата с ID: {}", chatId);
            List<MessageDTO> messages = chatsService.getMessagesByChatId(chatId, currentUserId);

            return ResponseEntity.status(200).body(messages);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении сообщений: {}", e.getMessage());
            return ResponseEntity.status(404).body("У вас нет такого чата");
        }
    }

    /// Получение всех чатов для текущего пользователя
    @GetMapping("/all")
    public ResponseEntity<List<ChatDTO>> getAllChats(@RequestHeader(name = "Authorization") String accessToken){
        try{
            accessToken = accessToken.substring(7);

            log.info("Запрос на получение всех чатов для пользователя с id: {}", jwtUtil.extractUserId(accessToken));
            List<ChatDTO> chats = chatsService.getAllChatsForUser(jwtUtil.extractUserId(accessToken));

            return ResponseEntity.status(200).body(chats);
        } catch (RuntimeException e){
            log.error("Ошибка при получении чатов: {}", e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
}
