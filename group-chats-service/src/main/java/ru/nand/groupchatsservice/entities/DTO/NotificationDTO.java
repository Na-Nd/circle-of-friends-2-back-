package ru.nand.groupchatsservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDTO {
    private String userEmail;
    private String message;

    private String ownerUsername;
    private LocalDateTime creationDate;

}
