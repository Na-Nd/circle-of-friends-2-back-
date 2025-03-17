package ru.nand.notificationsservice.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@PrimaryKeyClass
public class NotificationKey implements Serializable {
    @PrimaryKeyColumn(name = "user_email", type = PrimaryKeyType.PARTITIONED)
    private String userEmail; // Бдуем брать именно email, так как в слушатель кафки прилетает он, а не ид пользователя

    @PrimaryKeyColumn(name = "notification_id", type = PrimaryKeyType.CLUSTERED)
    private UUID id;
}
