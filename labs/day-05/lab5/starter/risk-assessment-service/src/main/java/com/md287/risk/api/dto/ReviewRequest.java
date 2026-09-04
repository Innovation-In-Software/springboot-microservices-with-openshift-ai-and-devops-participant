package com.md287.risk.api.dto;

import com.md287.risk.domain.Disposition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull Disposition decision,
        @NotBlank @Size(max = 200) String reason
) {
}
