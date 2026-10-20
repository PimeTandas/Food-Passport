package com.foodpassport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. @SpringBootApplication tells Spring to scan this package (and sub-packages)
 * for components like controllers, services and repositories, and wire them together.
 */
@SpringBootApplication
public class FoodPassportApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodPassportApplication.class, args);
    }
}
