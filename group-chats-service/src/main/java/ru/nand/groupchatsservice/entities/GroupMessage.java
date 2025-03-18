package ru.nand.groupchatsservice.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Table("group_messages")
public class GroupMessage {

    @PrimaryKey
    private GroupMessageKey key;

    private String text;

    private int senderId;

    private LocalDateTime timestamp;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @PrimaryKeyClass
    public static class GroupMessageKey implements Serializable {
        @PrimaryKeyColumn(name = "group_chat_id", type = PrimaryKeyType.PARTITIONED)
        private UUID groupChatId;

        @PrimaryKeyColumn(name = "message_id", type = PrimaryKeyType.CLUSTERED)
        private UUID messageId;

        @Override
        public int hashCode() {
            int result = groupChatId.hashCode();
            result = 31 * result + messageId.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            GroupMessageKey that = (GroupMessageKey) obj;

            if (!groupChatId.equals(that.groupChatId)) return false;
            return messageId.equals(that.messageId);
        }
    }
}