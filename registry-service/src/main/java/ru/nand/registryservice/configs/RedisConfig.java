//package ru.nand.registryservice.configs;
//
//import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//
//import java.time.Duration;
//
//@Configuration
//public class RedisConfig {
//    @Bean
//    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
//        return (builder) -> builder
//                .withCacheConfiguration("userCache",
//                        RedisCacheConfiguration.defaultCacheConfig()
//                                .entryTtl(Duration.ofMinutes(30)));
//    }
//}
