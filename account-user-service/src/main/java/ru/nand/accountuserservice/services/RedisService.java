package ru.nand.accountuserservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.nand.accountuserservice.entities.requests.AccountPatchRequest;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, AccountPatchRequest> accountPatchRequestRedisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, AccountPatchRequest> accountPatchRequestRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.accountPatchRequestRedisTemplate = accountPatchRequestRedisTemplate;
    }

    /// Сохранение значения с TTL
    public void saveWithExpiration(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Сохранено значение в Redis. Ключ: {}, Значение: {}, TTL: {} {}", key, value, timeout, unit);
    }

    /// Получение значения по ключу
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        log.debug("Получено значение из Redis. Ключ: {}, Значение: {}", key, value);
        return value;
    }

    /// Получение объекта по ключу
    public AccountPatchRequest getAccountPatchRequest(String key) {
        AccountPatchRequest value = accountPatchRequestRedisTemplate.opsForValue().get(key);
        log.debug("Получен объект AccountPatchRequest из Redis. Ключ: {}, Значение: {}", key, value);
        return value;
    }

    /// Удаление значения по ключу
    public void delete(String key) {
        redisTemplate.delete(key);
        log.debug("Удалено значение из Redis. Ключ: {}", key);
    }
}