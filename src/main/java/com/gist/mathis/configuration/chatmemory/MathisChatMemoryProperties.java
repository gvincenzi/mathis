package com.gist.mathis.configuration.chatmemory;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mathis.chat.memory")
public class MathisChatMemoryProperties {
    private long expirationMillis;
    private String cleanupCron;
}
