package ru.nand.groupchatsservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ru.nand.groupchatsservice.entities.UserGroupChat;

import java.util.List;
import java.util.UUID;

public interface UserGroupChatRepository extends CassandraRepository<UserGroupChat, UserGroupChat.UserGroupChatKey> {
    @Query("SELECT * FROM user_group_chats WHERE user_id = ?0")
    List<UserGroupChat> findByKeyUserId(int userId);

    @Query("DELETE FROM user_group_chats WHERE group_chat_id = ?0")
    void deleteByKeyGroupChatId(UUID groupChatId);
}
