package com.md287.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "md287")
public record Md287Properties(Kafka kafka, Model model, Policy policy) {

    public record Kafka(String submittedTopic) {
    }

    public record Model(String baseUrl, String scorePath, String apiKey) {
    }

    public record Policy(
            String version,
            int approveScoreBelow,
            BigDecimal approveAmountBelow,
            int declineScoreAtOrAbove,
            BigDecimal holdAmountAtOrAbove
    ) {
    }
}
