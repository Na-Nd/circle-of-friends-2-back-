package ru.nand.authservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nand.authservice.entities.DTO.RegisterDTO;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, RegisterDTO> registerDTORedisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, RegisterDTO> registerDTORedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.registerDTORedisTemplate = registerDTORedisTemplate;
    }

    /// Сохранение значения с TTL
    public void saveWithExpiration(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Сохранено значение в Redis: Ключ: {}, Значение: {}, TTL: {} {}", key, value, timeout, unit);
    }

    /// Получение значения по ключу
    public Object get(String key) {
        Object object = redisTemplate.opsForValue().get(key);
        log.debug("Получено значение из Redis: Ключ: {}, Значение: {}", key, object);
        return object;
    }

    /// Получение объекта по ключу
    public RegisterDTO getRegisterDTO(String key) {
        RegisterDTO registerDTO = registerDTORedisTemplate.opsForValue().get(key);
        log.debug("Получен объект RegisterDTO из Redis. Ключ: {}, Значение: {}", key, registerDTO);
        return registerDTO;
    }

    /// Удаление значения по ключу
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("Удалено значение из Redis. Ключ: {}", key);
    }
}