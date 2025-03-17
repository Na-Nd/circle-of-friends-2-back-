package ru.nand.messagesservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
@Table("chat_users")
public class ChatUser {
    @PrimaryKey
    private ChatUserKey key;

    @Getter
    @Setter
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class ChatUserKey implements Serializable {
        @PrimaryKeyColumn(name = "chat_id", type = PrimaryKeyType.PARTITIONED)
        private UUID chatId;

        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.CLUSTERED)
        private int userId;
    }
}
