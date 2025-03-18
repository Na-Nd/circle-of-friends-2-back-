package ru.nand.groupchatsservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ru.nand.groupchatsservice.entities.GroupMessage;

import java.util.List;
import java.util.UUID;

public interface GroupMessageRepository extends CassandraRepository<GroupMessage, GroupMessage.GroupMessageKey> {
    @Query("SELECT * FROM group_messages WHERE group_chat_id = ?0")
    List<GroupMessage> findByKeyGroupChatId(UUID groupChatId);

    @Query("DELETE FROM group_messages WHERE group_chat_id = ?0")
    void deleteByKeyGroupChatId(UUID groupChatId);
}