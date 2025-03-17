package ru.nand.notificationsservice.repositories;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ru.nand.notificationsservice.entities.Notification;
import ru.nand.notificationsservice.entities.NotificationKey;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends CassandraRepository<Notification, NotificationKey> {
    List<Notification> findByKeyUserEmail(String userEmail);

    @Query("SELECT * FROM notifications WHERE creationdate < ?0 ALLOW FILTERING")
    List<Notification> findOldNotifications(LocalDateTime threshold);
}
