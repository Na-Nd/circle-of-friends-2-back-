package ru.nand.messagesservice.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

/// Составной первичный ключ для сущности Message
@Getter
@Setter
@Builder
@PrimaryKeyClass
public class MessageKey implements Serializable {
    @PrimaryKeyColumn(name = "chat_id", type = PrimaryKeyType.PARTITIONED)
    private UUID chatId;

    @PrimaryKeyColumn(name = "message_id", type = PrimaryKeyType.CLUSTERED)
    private UUID messageId;

    @Override
    public int hashCode() {
        int result = chatId.hashCode();
        result = 31 * result + messageId.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;

        MessageKey that = (MessageKey) obj;

        if(!chatId.equals(that.chatId)) return false;

        return messageId.equals(that.messageId);
    }
}
