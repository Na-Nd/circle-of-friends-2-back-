package ru.nand.groupchatsservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMessageDTO {
    private UUID messageId;

    private UUID groupChatId;

    private String messageText;

    private int senderId;

    private LocalDateTime timestamp;
}
