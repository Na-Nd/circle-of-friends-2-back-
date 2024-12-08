package ru.nand.authservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Override
    public void run(String... args) {
        logger.info("Application has started successfully :)");
    }
}
