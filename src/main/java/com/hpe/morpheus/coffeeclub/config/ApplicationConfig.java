package com.hpe.morpheus.coffeeclub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {

    /** Injected everywhere a date is needed so that tests can pin "today". */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
