package ru.nand.authservice.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.nand.authservice.entities.DTO.RegisterDTO;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // StringRedisSerializer для ключей
        template.setKeySerializer(new StringRedisSerializer());

        // Jackson2JsonRedisSerializer для значений
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedisTemplate<String, RegisterDTO> registerDTORedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, RegisterDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // StringRedisSerializer для ключей
        template.setKeySerializer(new StringRedisSerializer());

        // Jackson2JsonRedisSerializer для значений
        Jackson2JsonRedisSerializer<RegisterDTO> serializer = new Jackson2JsonRedisSerializer<>(RegisterDTO.class);
        template.setValueSerializer(serializer);

        return template;
    }

    @Bean
    public Jackson2JsonRedisSerializer<RegisterDTO> registerDTOSerializer() {
        return new Jackson2JsonRedisSerializer<>(RegisterDTO.class);
    }
}