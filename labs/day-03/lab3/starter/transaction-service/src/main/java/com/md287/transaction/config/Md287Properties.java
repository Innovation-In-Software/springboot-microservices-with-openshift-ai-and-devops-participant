package com.md287.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "md287")
public record Md287Properties(
        AccountService accountService,
        Kafka kafka
) {
    public record AccountService(String baseUrl) {
    }

    public record Kafka(String submittedTopic) {
    }
}
