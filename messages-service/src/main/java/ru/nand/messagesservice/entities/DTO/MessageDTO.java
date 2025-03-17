package ru.nand.messagesservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private UUID id;
    private int sender;
    private String text;
    private UUID chatId;
    private LocalDateTime timestamp;

    public MessageDTO(UUID id, String text, LocalDateTime timestamp) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
    }
}