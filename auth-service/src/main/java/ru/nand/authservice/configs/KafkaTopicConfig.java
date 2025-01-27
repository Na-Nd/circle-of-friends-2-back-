package ru.nand.authservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    // Топик для регистрации
    @Bean
    public NewTopic userRegistrationTopic(){
        return TopicBuilder.name("user-registration-topic")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2")) // Минимальное количество синхронизированных реплик)
                .build();
    }

    // Топик для логина
    @Bean
    public NewTopic userLoginTopic(){
        return TopicBuilder.name("user-login-topic")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }

    // Топик для уведомлений
    @Bean
    public NewTopic userNotificationsTopic(){
        return TopicBuilder.name("user-notifications-topic")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
}
