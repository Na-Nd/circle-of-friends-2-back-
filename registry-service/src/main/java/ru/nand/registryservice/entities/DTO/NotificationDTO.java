package ru.nand.registryservice.entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String userEmail;
    private String message;

    private String ownerUsername;
    private LocalDateTime creationDate;

}
