package com.prasadsumit.notebridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class SessionTimeConfiguration {
    @Bean
    ZoneId sessionTimeZone(@Value("${notebridge.time-zone:Asia/Kolkata}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}
