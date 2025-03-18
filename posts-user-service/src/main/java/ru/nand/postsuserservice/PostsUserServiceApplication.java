package ru.nand.postsuserservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PostsUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostsUserServiceApplication.class, args);
    }

}
