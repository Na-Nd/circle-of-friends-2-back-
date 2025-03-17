package ru.nand.notificationsservice.entities;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
@Table("notifications")
public class Notification {

    @PrimaryKey
    private NotificationKey key;

    private String message;

    private String ownerUsername;

    private LocalDateTime creationDate;
}
