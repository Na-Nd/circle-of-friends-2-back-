package ru.nand.messagesservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ru.nand.messagesservice.entities.UserChat;

import java.util.List;
import java.util.UUID;

public interface UserChatRepository extends CassandraRepository<UserChat, UserChat.UserChatKey> {
    List<UserChat> findByKeyUserId(int userId);

    @Query("SELECT * FROM user_chats WHERE chat_id = ?0 ALLOW FILTERING") // Менее эффективный, так как ищем по кластерному ключу
    List<UserChat> findByKeyChatId(UUID chatId);
}
