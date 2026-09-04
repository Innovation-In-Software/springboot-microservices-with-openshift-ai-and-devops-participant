package com.md287.risk.policy;

import com.md287.risk.config.Md287Properties;
import com.md287.risk.domain.Disposition;
import com.md287.risk.model.ModelScore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PolicyEngine {

    private final Md287Properties.Policy policy;

    public PolicyEngine(Md287Properties properties) {
        this.policy = properties.policy();
    }

    public PolicyDecision decide(BigDecimal amount, ModelScore model) {
        // TODO Lab 5 Step 3 — implement the policy table from LAB-5-GUIDE.md.
        // MODEL_UNAVAILABLE must HOLD. Never APPROVE when the model is not OK.
        throw new UnsupportedOperationException("TODO Lab 5 Step 3 — PolicyEngine.decide");
    }
}
