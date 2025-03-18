package ru.nand.groupchatsservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ru.nand.groupchatsservice.entities.GroupChatUser;

import java.util.List;
import java.util.UUID;

public interface GroupChatUserRepository extends CassandraRepository<GroupChatUser, GroupChatUser.GroupChatUserKey> {
    @Query("SELECT * FROM group_chat_users WHERE group_chat_id = ?0")
    List<GroupChatUser> findByKeyGroupChatId(UUID groupChatId);

    @Query("DELETE FROM group_chat_users WHERE group_chat_id = ?0")
    void deleteByKeyGroupChatId(UUID groupChatId);
}
