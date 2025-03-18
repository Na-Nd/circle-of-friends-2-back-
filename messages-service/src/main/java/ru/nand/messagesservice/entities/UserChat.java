package ru.nand.messagesservice.entities;

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
@Table("user_chats")
public class UserChat { // В каких чатах состоит конкретный пользователь
    @PrimaryKey
    private UserChatKey key;

    @Getter
    @Setter
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class UserChatKey implements Serializable {
        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED)
        private int userId;

        @PrimaryKeyColumn(name = "chat_id", type = PrimaryKeyType.CLUSTERED)
        private UUID chatId;

    }
}
