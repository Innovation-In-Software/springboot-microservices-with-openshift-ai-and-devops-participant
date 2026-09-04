package com.md287.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "md287.jwt")
public record JwtProperties(String issuer, String secret) {
}
