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
import java.lang.annotation.Target;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("group_chat_users")
public class GroupChatUser { // Какие пользователи состоят в конкретном групповом чате
    @PrimaryKey
    private GroupChatUserKey key;

    @Getter
    @Setter
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class GroupChatUserKey implements Serializable {
        @PrimaryKeyColumn(name = "group_chat_id", type = PrimaryKeyType.PARTITIONED)
        private UUID groupChatId;

        @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.CLUSTERED)
        private int userId;
    }
}
