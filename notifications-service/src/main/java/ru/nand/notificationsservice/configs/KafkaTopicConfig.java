package ru.nand.notificationsservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    // Топик для отправки уведомлений в registry-service с последующим сохранением
    @Bean
    public NewTopic notificationsRegistryTopic(){
        return TopicBuilder.name("notifications-registry-topic")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
}
