package com.divinelaundry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LaundryAdminApplication {
    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    public static void main(String[] args) {
        SpringApplication.run(LaundryAdminApplication.class, args);
    }
}
