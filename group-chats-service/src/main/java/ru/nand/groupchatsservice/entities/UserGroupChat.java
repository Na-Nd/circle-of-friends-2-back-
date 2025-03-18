package ru.nand.groupchatsservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("user_group_chats")
public class UserGroupChat { // В каких группах состоит конкретный пользователь
    @PrimaryKey
    private UserGroupChatKey key;

    @Getter
    @Setter
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class UserGroupChatKey implements Serializable {
        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED)
        private int userId;

        @PrimaryKeyColumn(name = "group_chat_id", type = PrimaryKeyType.CLUSTERED)
        private UUID groupChatId;
    }
}
