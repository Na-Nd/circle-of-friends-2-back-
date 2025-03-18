package ru.nand.groupchatsservice.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.nand.groupchatsservice.entities.DTO.GroupChatDTO;
import ru.nand.groupchatsservice.entities.DTO.GroupMessageDTO;
import ru.nand.groupchatsservice.entities.requests.CreateGroupMessageRequest;
import ru.nand.groupchatsservice.entities.requests.CreateGroupRequest;
import ru.nand.groupchatsservice.entities.requests.UpdateGroupMessageRequest;
import ru.nand.groupchatsservice.services.GroupChatsService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/groupchats")
public class GroupChatsController {
    private final GroupChatsService groupChatsService;

    @Autowired
    public GroupChatsController(GroupChatsService groupChatsService) {
        this.groupChatsService = groupChatsService;
    }

    /// Создание группового чата
    @PostMapping("/create")
    public ResponseEntity<String> createGroupChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(name = "Authorization") String authBearer,
            @Valid @RequestBody CreateGroupRequest createGroupRequest,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getFieldError().getDefaultMessage());
        }

        try {
            log.info("Пользовательский запрос на создание группового чата, создатель: {}", userDetails.getUsername());
            return ResponseEntity.ok(groupChatsService.createGroup(authBearer, userDetails.getUsername(), createGroupRequest));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка создания группы");
        }
    }

    /// Просмотр группового чата (групповых сообщений)
    @GetMapping("/{groupChatID}/messages")
    public ResponseEntity<List<GroupMessageDTO>> getGroupMessages(@PathVariable UUID groupChatID, @RequestHeader(name = "Authorization") String authBearer) {
        try{
            log.info("Пользовательски запрос на просмотр гуппового чата {}", groupChatID);
            return ResponseEntity.status(200).body(groupChatsService.getGroupMessages(groupChatID, authBearer));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Получение групповых чатов для текущего пользователя
    @GetMapping()
    public ResponseEntity<List<GroupChatDTO>> getGroupChats(@RequestHeader(name = "Authorization") String authBearer){
        try{
            log.info("Пользовательский запрос на получение списка групповых чатов");
            return ResponseEntity.status(200).body(groupChatsService.getGroupChats(authBearer));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }

    /// Удаление группового чата владельцем чата
    @DeleteMapping("/{groupChatId}")
    public ResponseEntity<String> deleteGroupChat(@PathVariable UUID groupChatId, @RequestHeader(name = "Authorization") String authBearer) {
        try{
            log.info("Пользовательский запрос на удаление группового чата {}", groupChatId);
            return ResponseEntity.status(200).body(groupChatsService.deleteGroupChat(groupChatId, authBearer));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при удалении группового чата");
        }
    }

    /// Добавление сообщения в групповой чат
    @PostMapping("/{groupChatId}/messages")
    public ResponseEntity<String> addGroupMessage(
            @PathVariable UUID groupChatId,
            @RequestHeader(name = "Authorization") String authBearer,
            @RequestBody CreateGroupMessageRequest messageRequest
    ) {
        try{
            log.info("Пользовательский запрос на добавление сообщения {} в групповой чат {}", messageRequest.getMessageText(), groupChatId);
            return ResponseEntity.status(200).body(groupChatsService.addGroupMessage(groupChatId, authBearer, messageRequest));
        } catch (Exception e){
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при добавлении сообщения");
        }
    }

    /// Удаление сообщения из группового чата отправителем сообщения
    @DeleteMapping("/{groupChatId}/messages/{messageId}")
    public ResponseEntity<String> deleteGroupMessage(
            @PathVariable UUID groupChatId,
            @PathVariable UUID messageId,
            @RequestHeader(name = "Authorization") String authBearer
    ) {
        try {
            log.info("Пользовательский запрос на удаление группового сообщения {} из чата {}", messageId, groupChatId);
            return ResponseEntity.status(200).body(groupChatsService.deleteGroupMessage(groupChatId, messageId, authBearer));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при удалении сообщения");
        }
    }

    /// Обновление сообщения отправителем сообщения
    @PutMapping("/{groupChatId}/messages/{messageId}")
    public ResponseEntity<String> updateGroupMessage(
            @PathVariable UUID groupChatId,
            @PathVariable UUID messageId,
            @RequestBody UpdateGroupMessageRequest messageRequest,
            @RequestHeader(name = "Authorization") String authBearer
    ) {
        try {
            log.info("Пользовательский запрос на обновление сообщения {} в групповом чате {}", messageId, groupChatId);
            return ResponseEntity.status(200).body(groupChatsService.updateGroupMessage(groupChatId, messageId, messageRequest, authBearer));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(400).body("Ошибка при обновлении сообщения");
        }
    }
}
