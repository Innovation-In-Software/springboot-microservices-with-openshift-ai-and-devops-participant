package com.md287.transaction.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountView(
        String accountId,
        String status,
        String currency
) {
}
