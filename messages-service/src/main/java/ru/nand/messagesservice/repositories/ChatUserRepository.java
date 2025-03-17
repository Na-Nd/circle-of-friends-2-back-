package ru.nand.messagesservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import ru.nand.messagesservice.entities.ChatUser;

import java.util.List;
import java.util.UUID;

public interface ChatUserRepository extends CassandraRepository<ChatUser, ChatUser.ChatUserKey> {
    List<ChatUser> findByKeyChatId(UUID chatId);
}
