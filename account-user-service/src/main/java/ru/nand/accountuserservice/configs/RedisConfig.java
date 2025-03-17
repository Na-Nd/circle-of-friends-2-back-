package ru.nand.accountuserservice.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.nand.accountuserservice.entities.DTO.UserDTO;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;

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
    public RedisTemplate<String, AccountPatchRequest> accountPatchRequestRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AccountPatchRequest> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // StringRedisSerializer для ключей
        template.setKeySerializer(new StringRedisSerializer());

        // Jackson2JsonRedisSerializer для значений
        Jackson2JsonRedisSerializer<AccountPatchRequest> serializer = new Jackson2JsonRedisSerializer<>(AccountPatchRequest.class);
        template.setValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedisTemplate<String, UserDTO> userDTORedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, UserDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // StringRedisSerializer для ключей
        template.setKeySerializer(new StringRedisSerializer());

        // Jackson2JsonRedisSerializer для значений
        Jackson2JsonRedisSerializer<UserDTO> serializer = new Jackson2JsonRedisSerializer<>(UserDTO.class);
        template.setValueSerializer(serializer);

        return template;
    }
}