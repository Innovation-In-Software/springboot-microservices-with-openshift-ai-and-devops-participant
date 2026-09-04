package com.md287.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "md287.jwt")
public record JwtProperties(String issuer, String secret) {
}
