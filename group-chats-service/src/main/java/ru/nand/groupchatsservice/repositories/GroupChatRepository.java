package ru.nand.groupchatsservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import ru.nand.groupchatsservice.entities.GroupChat;

import java.util.UUID;

public interface GroupChatRepository extends CassandraRepository<GroupChat, UUID> {
}
