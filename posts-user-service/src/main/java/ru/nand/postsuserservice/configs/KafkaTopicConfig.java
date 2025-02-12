package ru.nand.postsuserservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicConfig {
    // Топик для уведомлений подписчиков о новых постах их подписок
    @Bean
    public NewTopic postsNotificationsTopic(){
        return TopicBuilder.name("user-account-changes")
                .partitions(3)
                .replicas(3)
                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
}
