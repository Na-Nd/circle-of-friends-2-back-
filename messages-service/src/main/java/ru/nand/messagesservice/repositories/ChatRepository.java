package ru.nand.messagesservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import ru.nand.messagesservice.entities.Chat;
import java.util.UUID;

public interface ChatRepository extends CassandraRepository<Chat, UUID> {

}
