package ru.nand.groupchatsservice.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.nand.groupchatsservice.entities.DTO.GroupChatDTO;
import ru.nand.groupchatsservice.entities.DTO.GroupMessageDTO;
import ru.nand.groupchatsservice.entities.GroupChat;
import ru.nand.groupchatsservice.entities.GroupChatUser;
import ru.nand.groupchatsservice.entities.GroupMessage;
import ru.nand.groupchatsservice.entities.UserGroupChat;
import ru.nand.groupchatsservice.entities.requests.CreateGroupMessageRequest;
import ru.nand.groupchatsservice.entities.requests.CreateGroupRequest;
import ru.nand.groupchatsservice.entities.requests.UpdateGroupMessageRequest;
import ru.nand.groupchatsservice.repositories.GroupChatRepository;
import ru.nand.groupchatsservice.repositories.GroupChatUserRepository;
import ru.nand.groupchatsservice.repositories.GroupMessageRepository;
import ru.nand.groupchatsservice.repositories.UserGroupChatRepository;
import ru.nand.groupchatsservice.utils.GroupChatsUtil;
import ru.nand.groupchatsservice.utils.JwtUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatsService {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final GroupChatsUtil groupChatsUtil;
    private final GroupChatRepository groupChatRepository;
    private final GroupChatUserRepository groupChatUserRepository;
    private final UserGroupChatRepository userGroupChatRepository;
    private final GroupMessageRepository groupMessageRepository;

    @Value("${registry.service.url}")
    private String REGISTRY_SERVICE_URL;

    @Value("${interservice.header.name}")
    private String HEADER_NAME;

    /// Создание группового чата
    public String createGroup(String authBearer, String groupOwnerUsername, CreateGroupRequest createGroupRequest) {
        try {
            // Проверяем существуют ли пользователи, и если существуют - возвращаем их почту для создания уведомления
            Set<String> usersEmails = usersExists(createGroupRequest.getUsersIds());

            // Достаем id текущего пользователя из токена и кладем в сет
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("Получен id текущего пользователя - создателя группы: {}", currentUserId);
            createGroupRequest.getUsersIds().add(currentUserId);

            // Проверяем существует ли такая группа
            UUID existingGroupChatId = findExistingGroupChat(createGroupRequest.getUsersIds());
            if (existingGroupChatId != null) {
                throw new RuntimeException("Такая группа уже существует");
            }

            // Создаем группу
            GroupChat groupChat = new GroupChat(
                    UUID.randomUUID(),
                    createGroupRequest.getGroupName(),
                    currentUserId,
                    LocalDateTime.now()
            );

            // Сохраняем группу
            groupChatRepository.save(groupChat);

            // Сохраняем связи в GroupChatUser и UserGroupChat
            for (Integer userId : createGroupRequest.getUsersIds()) {
                GroupChatUser groupChatUser = new GroupChatUser();
                groupChatUser.setKey(new GroupChatUser.GroupChatUserKey(groupChat.getGroupChatId(), userId));
                groupChatUserRepository.save(groupChatUser);

                UserGroupChat userGroupChat = new UserGroupChat();
                userGroupChat.setKey(new UserGroupChat.UserGroupChatKey(userId, groupChat.getGroupChatId()));
                userGroupChatRepository.save(userGroupChat);
            }

            // Уведомления участникам
            for (String email : usersEmails) {
                groupChatsUtil.sendNotification(email, "Пользователь " + groupOwnerUsername + " создал групповой чат");
            }

            return "Групповой чат создан";
        } catch (Exception e) {
            log.warn("Ошибка при создании группового чата: {}", e.getMessage());
            throw new RuntimeException("Ошибка при создании группового чата: " + e.getMessage());
        }
    }

    /// Проверка на существование пользователей и возврат их почт если пользователи существуют
    private Set<String> usersExists(Set<Integer> usersIds) {
        String requestMessage;
        try {
            requestMessage = objectMapper.writeValueAsString(usersIds);
        } catch (Exception e) {
            log.warn("Ошибка при сериализации данных сета пользователей: {}", e.getMessage());
            throw new RuntimeException("Ошибка сериализации данных");
        }
        log.debug("JSON для передачи в registry-service: {}", requestMessage);

        String url = REGISTRY_SERVICE_URL + "/api/users/find-users";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_NAME, "Bearer " + jwtUtil.generateInterServiceJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestMessage, headers);
        log.debug("Отправка запроса в registry-service на : {}", requestMessage);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Неуспешный ответ от registry-service при проверке пользователей с id {} на существование: {}", usersIds, response.getStatusCode());
                throw new RuntimeException("Неуспешный ответ от registry-service при проверке пользователей на существование");
            }

            Set<String> usersEmails;
            try {
                usersEmails = objectMapper.readValue(response.getBody(), new TypeReference<Set<String>>() {});
            } catch (Exception e) {
                log.warn("Ошибка при десериализации данных ответа: {}", e.getMessage());
                throw new RuntimeException("Ошибка при десериализации данных ответа");
            }

            return usersEmails;
        } catch (Exception e) {
            log.warn("Ошибка при проверке пользователей на существование: {}", e.getMessage());
            throw new RuntimeException("Ошибка при проверке пользователей " + usersIds + " на существование: " + e.getMessage());
        }
    }

    /// Проверка на чат, где есть только все переданные пользователи
    private UUID findExistingGroupChat(Set<Integer> usersIds) {
        if (usersIds.isEmpty()) {
            return null;
        }

        // Берем первого пользователя из списка
        Integer firstUserId = usersIds.iterator().next();

        // Находим все групповые чаты, в которых участвует первый пользователь
        Set<UUID> groupChats = userGroupChatRepository.findByKeyUserId(firstUserId)
                .stream()
                .map(userGroupChat -> userGroupChat.getKey().getGroupChatId())
                .collect(Collectors.toSet());

        // Для каждого группового чата проверяем, что в нем участвуют все пользователи из списка
        for (UUID groupChatId : groupChats) {
            Set<Integer> usersInGroupChat = groupChatUserRepository.findByKeyGroupChatId(groupChatId)
                    .stream()
                    .map(groupChatUser -> groupChatUser.getKey().getUserId())
                    .collect(Collectors.toSet());

            if (usersInGroupChat.containsAll(usersIds)) {
                return groupChatId;
            }
        }

        return null;
    }

    /// Получение сообщений группового чата
    public List<GroupMessageDTO> getGroupMessages(UUID groupChatId, String authBearer) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При получении сообщений группы {} получен id текущего пользователя: {}", groupChatId, currentUserId);

            // Проверяем, что пользователь состоит в групповом чате
            UserGroupChat.UserGroupChatKey userGroupChatKey = new UserGroupChat.UserGroupChatKey(currentUserId, groupChatId);
            boolean isUserInGroup = userGroupChatRepository.existsById(userGroupChatKey);

            if (!isUserInGroup) {
                log.warn("Пользователь с id {} не состоит в групповом чате с id {}", currentUserId, groupChatId);
                throw new RuntimeException("Пользователь не состоит в этом групповом чате");
            }

            // Получаем сообщения из группового чата
            List<GroupMessage> groupMessages = groupMessageRepository.findByKeyGroupChatId(groupChatId);

            // Преобразуем сообщения в DTO
            List<GroupMessageDTO> groupMessageDTOs = groupMessages.stream()
                    .map(message -> GroupMessageDTO.builder()
                            .messageId(message.getKey().getMessageId())
                            .groupChatId(message.getKey().getGroupChatId())
                            .messageText(message.getText())
                            .senderId(message.getSenderId())
                            .timestamp(message.getTimestamp())
                            .build())
                    .collect(Collectors.toList());

            return groupMessageDTOs;
        } catch (Exception e) {
            log.warn("Ошибка при получении сообщений из группового чата: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении сообщений из группового чата: " + e.getMessage());
        }
    }

    /// Добавление сообщения в групповой чат
    public String addGroupMessage(UUID groupChatId, String authBearer, CreateGroupMessageRequest messageRequest) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При добавлении сообщения в группу {} получен id текущего пользователя: {}", groupChatId, currentUserId);

            // Проверяем, что пользователь состоит в групповом чате
            UserGroupChat.UserGroupChatKey userGroupChatKey = new UserGroupChat.UserGroupChatKey(currentUserId, groupChatId);
            boolean isUserInGroup = userGroupChatRepository.existsById(userGroupChatKey);

            if (!isUserInGroup) {
                log.warn(" Пользователь с id {} не состоит в групповом чате с id {}", currentUserId, groupChatId);
                throw new RuntimeException("Пользователь не состоит в этом групповом чате");
            }

            // Создаем новое сообщение
            GroupMessage groupMessage = GroupMessage.builder()
                    .key(new GroupMessage.GroupMessageKey(groupChatId, UUID.randomUUID())) // Генерируем новый messageId
                    .text(messageRequest.getMessageText())
                    .senderId(currentUserId)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Сохраняем сообщение в Cassandra
            groupMessageRepository.save(groupMessage);

            return "Сообщение успешно добавлено в групповой чат";
        } catch (Exception e) {
            log.warn("Ошибка при добавлении сообщения в групповой чат: {}", e.getMessage());
            throw new RuntimeException("Ошибка при добавлении сообщения в групповой чат: " + e.getMessage());
        }
    }

    /// Получение групповых чатов переданного пользователя
    public List<GroupChatDTO> getGroupChats(String authBearer) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При получении групповых чатов получен id текущего пользователя: {}", currentUserId);

            // Получаем список чатов, в которых состоит текущий пользователь
            List<UserGroupChat> userGroupChats = userGroupChatRepository.findByKeyUserId(currentUserId);

            List<GroupChatDTO> groupChatDTOs = new ArrayList<>();

            for (UserGroupChat userGroupChat : userGroupChats) {
                UUID groupChatId = userGroupChat.getKey().getGroupChatId();

                GroupChat groupChat = groupChatRepository.findById(groupChatId)
                        .orElseThrow(() -> new RuntimeException("Групповой чат не найден"));

                // Получаем список участников группового чата
                List<GroupChatUser> groupChatUsers = groupChatUserRepository.findByKeyGroupChatId(groupChatId);
                Set<Integer> participants = groupChatUsers.stream()
                        .map(groupChatUser -> groupChatUser.getKey().getUserId())
                        .collect(Collectors.toSet());

                GroupChatDTO groupChatDTO = GroupChatDTO.builder()
                        .groupChatId(groupChatId)
                        .groupChatName(groupChat.getGroupName())
                        .participants(participants)
                        .build();

                // Добавляем в список для результата
                groupChatDTOs.add(groupChatDTO);
            }

            return groupChatDTOs;
        } catch (Exception e) {
            log.warn("Ошибка при получении групповых чатов: {}", e.getMessage());
            throw new RuntimeException("Ошибка при получении групповых чатов: " + e.getMessage());
        }
    }

    /// Удаление группового чата владельцем
    public String deleteGroupChat(UUID groupChatId, String authBearer) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При удалении группового чата {} получен id текущего пользователя: {}", groupChatId, currentUserId);

            // Получаем групповой чат по его id
            GroupChat groupChat = groupChatRepository.findById(groupChatId)
                    .orElseThrow(() -> new RuntimeException("Групповой чат не найден"));

            // Проверяем, что текущий пользователь является создателем чата
            if (groupChat.getCreatorId() != currentUserId) {
                log.warn("Пользователь с id {} не является создателем группового чата с id {}", currentUserId, groupChatId);
                throw new RuntimeException("Только создатель чата может удалить его");
            }

            // Удаляем все сообщения и связи
            groupMessageRepository.deleteByKeyGroupChatId(groupChatId);
            groupChatUserRepository.deleteByKeyGroupChatId(groupChatId);
            userGroupChatRepository.deleteByKeyGroupChatId(groupChatId);

            // Удаляем сам групповой чат
            groupChatRepository.delete(groupChat);

            return "Групповой чат успешно удален";
        } catch (Exception e) {
            log.warn("Ошибка при удалении группового чата: {}", e.getMessage());
            throw new RuntimeException("Ошибка при удалении группового чата: " + e.getMessage());
        }
    }

    /// Удаление сообщения из группового чата отправителем сообщения
    public String deleteGroupMessage(UUID groupChatId, UUID messageId, String authBearer) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При удалении сообщения {} получен id текущего пользователя: {}", messageId, currentUserId);

            // Получаем сообщение по его id
            GroupMessage groupMessage = groupMessageRepository.findById(new GroupMessage.GroupMessageKey(groupChatId, messageId))
                    .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

            // Проверяем, что текущий пользователь является отправителем сообщения
            if (groupMessage.getSenderId() != currentUserId) {
                log.warn("Пользователь с id {} не является отправителем сообщения с id {}", currentUserId, messageId);
                throw new RuntimeException("Только отправитель сообщения может удалить его");
            }

            // Удаляем сообщение
            groupMessageRepository.delete(groupMessage);

            return "Сообщение успешно удалено";
        } catch (Exception e) {
            log.warn("Ошибка при удалении сообщения: {}", e.getMessage());
            throw new RuntimeException("Ошибка при удалении сообщения: " + e.getMessage());
        }
    }

    /// Обновление сообщения отправителем
    public String updateGroupMessage(UUID groupChatId, UUID messageId, UpdateGroupMessageRequest messageRequest, String authBearer) {
        try {
            // Достаем id текущего пользователя из токена
            int currentUserId = jwtUtil.extractUserId(authBearer.substring(7));
            log.debug("При обновлении сообщения {} получен id текущего пользователя: {}", messageId, currentUserId);

            // Получаем сообщение по его id
            GroupMessage groupMessage = groupMessageRepository.findById(new GroupMessage.GroupMessageKey(groupChatId, messageId))
                    .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

            // Проверяем, что текущий пользователь является отправителем сообщения
            if (groupMessage.getSenderId() != currentUserId) {
                log.warn(" Пользователь с id {} не является отправителем сообщения с id {}", currentUserId, messageId);
                throw new RuntimeException("Только отправитель сообщения может обновить его");
            }

            // Обновляем текст сообщения
            groupMessage.setText(messageRequest.getUpdatedMessageText());

            // Сохраняем обновленное сообщение
            groupMessageRepository.save(groupMessage);

            return "Сообщение успешно обновлено";
        } catch (Exception e) {
            log.warn("Ошибка при обновлении сообщения: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обновлении сообщения: " + e.getMessage());
        }
    }
}
