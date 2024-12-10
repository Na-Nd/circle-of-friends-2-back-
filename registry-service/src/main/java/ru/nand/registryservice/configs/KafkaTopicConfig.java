package ru.nand.registryservice.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userRegistrationResponseTopic(){
        return TopicBuilder.name("user-registration-response-topic")
                .partitions(3)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic userLoginResponseTopic(){
        return TopicBuilder.name("user-login-response-topic")
                .partitions(3)
                .replicas(3)
                .build();
    }

    @Bean
    public NewTopic userAccountChangesResponseTopic(){
        return TopicBuilder.name("user-account-changes-response-topic")
                .partitions(3)
                .replicas(3)
                .build();
    }

}
