package ru.nand.messagesservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.messagesservice.entities.*;
import ru.nand.messagesservice.entities.DTO.ChatDTO;
import ru.nand.messagesservice.entities.DTO.MessageDTO;
import ru.nand.messagesservice.repositories.ChatRepository;
import ru.nand.messagesservice.repositories.ChatUserRepository;
import ru.nand.messagesservice.repositories.MessageRepository;
import ru.nand.messagesservice.repositories.UserChatRepository;
import ru.nand.messagesservice.utils.JwtUtil;
import ru.nand.messagesservice.utils.MessagesUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatsService {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserChatRepository userChatRepository;
    private final ChatUserRepository chatUserRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final MessagesUtil messagesUtil;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    /// Создание чата
    public ChatDTO createChat(int userId1, int userId2) {
        // Проверяем, существуют ли пользователи
        String user1Email = userExists(userId1);
        String user2Email = userExists(userId2);

        // Проверяем, существует ли уже чат между этими пользователями
        UUID existingChatId = findExistingChat(userId1, userId2);
        if (existingChatId != null) {
            return new ChatDTO(existingChatId, Set.of(userId1, userId2));
        }

        // Создаем новый чат
        Chat chat = new Chat(UUID.randomUUID(), LocalDateTime.now());
        chatRepository.save(chat);

        // Уведомления пользователям
        messagesUtil.sendNotification(user1Email, "Создан чат с пользователем " + userId2);
        messagesUtil.sendNotification(user2Email, "Создан чат с пользователем " + userId1);

        // Сохраняем связи в user_chats и chat_users
        userChatRepository.save(new UserChat(new UserChat.UserChatKey(userId1, chat.getChatId())));
        userChatRepository.save(new UserChat(new UserChat.UserChatKey(userId2, chat.getChatId())));

        chatUserRepository.save(new ChatUser(new ChatUser.ChatUserKey(chat.getChatId(), userId1)));
        chatUserRepository.save(new ChatUser(new ChatUser.ChatUserKey(chat.getChatId(), userId2)));

        log.debug("Создан чат для пользователей с ID {} и {}", userId1, userId2);

        return new ChatDTO(chat.getChatId(), Set.of(userId1, userId2));
    }

    /// Поиск существующего чата между двумя пользователями
    private UUID findExistingChat(int userId1, int userId2) {
        // Получаем все чаты для первого пользователя
        List<UserChat> user1Chats = userChatRepository.findByKeyUserId(userId1);

        // Получаем все чаты для второго пользователя
        List<UserChat> user2Chats = userChatRepository.findByKeyUserId(userId2);

        // Ищем общий чат
        for (UserChat user1Chat : user1Chats) {
            for (UserChat user2Chat : user2Chats) {
                if (user1Chat.getKey().getChatId().equals(user2Chat.getKey().getChatId())) {
                    return user1Chat.getKey().getChatId(); // Возвращаем ID общего чата
                }
            }
        }

        return null; // Если общий чат не найден
    }

    /// Отправка сообщения
    public void sendMessage(int senderId, int targetUserId, MessageDTO messageDTO) {
        // Находим или создаем чат
        ChatDTO chatDTO = createChat(senderId, targetUserId);

        // Создаем сообщение
        MessageKey messageKey = MessageKey.builder()
                .chatId(chatDTO.getId())
                .messageId(UUID.randomUUID())
                .build();

        Message message = Message.builder()
                .key(messageKey)
                .text(messageDTO.getText())
                .senderId(senderId) // Указываем отправителя
                .timestamp(LocalDateTime.now())
                .build();

        log.debug("Сообщение отправлено в чат {}", chatDTO.getId());
        messageRepository.save(message);

        // Уведомление получателю сообщения (для получения почты получателя используем метод userExists() как костыль)
        messagesUtil.sendNotification(userExists(targetUserId), "Новое сообщение от пользователя " + senderId);
    }

    /// Получение сообщений по ID чата
    public List<MessageDTO> getMessagesByChatId(UUID chatId, int currentUserId) {
        // Получаем список участников чата
        Set<Integer> participants = getChatParticipants(chatId);

        // Проверяем, является ли текущий пользователь участником чата
        if (!participants.contains(currentUserId)) {
            throw new RuntimeException("Доступ запрещен: вы не являетесь участником этого чата");
        }

        // Получаем сообщения из чата
        List<Message> messages = messageRepository.findByKeyChatId(chatId);

        return messages.stream()
                .map(message -> new MessageDTO(
                        message.getKey().getMessageId(),
                        message.getSenderId(), // sender (ID отправителя)
                        message.getText(),
                        chatId,
                        message.getTimestamp()
                ))
                .collect(Collectors.toList());
    }

    /// Список участников чата
    public Set<Integer> getChatParticipants(UUID chatId) {
        return chatUserRepository.findByKeyChatId(chatId).stream()
                .map(chatUser -> chatUser.getKey().getUserId())
                .collect(Collectors.toSet());
    }

    /// Получение всех чатов для текущего пользователя
    public List<ChatDTO> getAllChatsForUser(int userId) {
        // Получаем все чаты для пользователя
        List<UserChat> userChats = userChatRepository.findByKeyUserId(userId);

        return userChats.stream()
                .map(userChat -> {
                    UUID chatId = userChat.getKey().getChatId();

                    // Получаем всех участников чата
                    List<UserChat> chatParticipants = userChatRepository.findByKeyChatId(chatId);
                    Set<Integer> userIds = chatParticipants.stream()
                            .map(participant -> participant.getKey().getUserId())
                            .collect(Collectors.toSet());

                    return new ChatDTO(chatId, userIds);
                })
                .collect(Collectors.toList());
    }


    /// Проверка на существование пользователя с возвратом его почты
    private String userExists(int userId) {
        String url = REGISTRY_SERVICE_URL + "/api/users/" + userId + "/exists";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            log.info("Отправка запроса на существование id: {}", userId);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()){
                throw new RuntimeException("Неуспешный ответ от registry-service: " + response.getStatusCode());
            }

            log.debug("Пользователь с id {} существует, получен email: {}", userId, response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при проверке существования пользователя с ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при проверке существования пользователя с id " + userId + ": " + e.getMessage());
        }
    }
}