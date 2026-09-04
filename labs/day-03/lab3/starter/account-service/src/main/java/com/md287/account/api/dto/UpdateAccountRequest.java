package com.md287.account.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 80)
        String nickname
) {
}
