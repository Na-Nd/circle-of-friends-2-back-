package ru.nand.messagesservice.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Table("messages")
public class Message {
    @PrimaryKey
    private MessageKey key; // Составной первичный ключ

    private String text;
    private int senderId; // Отправитель
    private LocalDateTime timestamp;
}
