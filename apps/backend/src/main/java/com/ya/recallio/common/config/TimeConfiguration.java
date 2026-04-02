package com.ya.recallio.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a shared clock so time-based services stay deterministic in tests.
 */
@Configuration
public class TimeConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
