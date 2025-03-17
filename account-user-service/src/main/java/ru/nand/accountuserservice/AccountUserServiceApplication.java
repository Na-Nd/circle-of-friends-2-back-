package ru.nand.accountuserservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AccountUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountUserServiceApplication.class, args);
    }

}
