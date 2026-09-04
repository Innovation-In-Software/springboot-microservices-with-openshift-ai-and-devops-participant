package com.md287.risk.policy;

import com.md287.risk.config.Md287Properties;
import com.md287.risk.domain.Disposition;
import com.md287.risk.model.ModelScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {

    private PolicyEngine engine;

    @BeforeEach
    void setUp() {
        Md287Properties properties = new Md287Properties(
                new Md287Properties.Kafka("transactions.submitted"),
                new Md287Properties.Model("http://localhost:8090", "/v1/score", "test"),
                new Md287Properties.Policy("policy-v1", 40, new BigDecimal("1000"), 70, new BigDecimal("5000"))
        );
        engine = new PolicyEngine(properties);
    }

    @Test
    void lowScoreLowAmountApproves() {
        PolicyDecision decision = engine.decide(new BigDecimal("25.00"), ModelScore.ok(12, "md287-risk-model", "1.0.0"));
        assertThat(decision.disposition()).isEqualTo(Disposition.APPROVE);
        assertThat(decision.reason()).isEqualTo("LOW_SCORE_LOW_VALUE");
    }

    @Test
    void highScoreDeclines() {
        PolicyDecision decision = engine.decide(new BigDecimal("80.00"), ModelScore.ok(80, "md287-risk-model", "1.0.0"));
        assertThat(decision.disposition()).isEqualTo(Disposition.DECLINE);
        assertThat(decision.reason()).isEqualTo("HIGH_SCORE");
    }

    @Test
    void highValueHoldsEvenWhenScoreIsLow() {
        PolicyDecision decision = engine.decide(new BigDecimal("5000.00"), ModelScore.ok(10, "md287-risk-model", "1.0.0"));
        assertThat(decision.disposition()).isEqualTo(Disposition.HOLD);
        assertThat(decision.reason()).isEqualTo("HIGH_VALUE");
    }

    @Test
    void modelTimeoutNeverApproves() {
        PolicyDecision decision = engine.decide(new BigDecimal("25.00"), ModelScore.timeout());
        assertThat(decision.disposition()).isEqualTo(Disposition.HOLD);
        assertThat(decision.reason()).isEqualTo("MODEL_UNAVAILABLE");
    }

    @Test
    void reviewBandHolds() {
        PolicyDecision decision = engine.decide(new BigDecimal("250.00"), ModelScore.ok(55, "md287-risk-model", "1.0.0"));
        assertThat(decision.disposition()).isEqualTo(Disposition.HOLD);
        assertThat(decision.reason()).isEqualTo("REVIEW_BAND");
    }
}
