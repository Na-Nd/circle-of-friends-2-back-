package ru.nand.messagesservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import ru.nand.messagesservice.entities.Message;
import ru.nand.messagesservice.entities.MessageKey;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends CassandraRepository<Message, MessageKey>{
    List<Message> findByKeyChatId(UUID chatId);
}
