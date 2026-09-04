package com.md287.risk.policy;

import com.md287.risk.domain.Disposition;

public record PolicyDecision(Disposition disposition, String reason) {
}
