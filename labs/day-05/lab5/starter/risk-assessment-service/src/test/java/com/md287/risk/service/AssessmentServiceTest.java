package com.md287.risk.service;

import com.md287.risk.api.dto.AssessmentResponse;
import com.md287.risk.api.dto.ReviewRequest;
import com.md287.risk.api.exception.ReviewNotAllowedException;
import com.md287.risk.config.Md287Properties;
import com.md287.risk.domain.Assessment;
import com.md287.risk.domain.Disposition;
import com.md287.risk.domain.ModelStatus;
import com.md287.risk.domain.ReviewStatus;
import com.md287.risk.messaging.TransactionSubmittedEvent;
import com.md287.risk.model.ModelClient;
import com.md287.risk.model.ModelScore;
import com.md287.risk.policy.PolicyEngine;
import com.md287.risk.repository.AssessmentRepository;
import com.md287.risk.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private ModelClient modelClient;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private AssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        Md287Properties properties = new Md287Properties(
                new Md287Properties.Kafka("transactions.submitted"),
                new Md287Properties.Model("http://localhost:8090", "/v1/score", "test"),
                new Md287Properties.Policy("policy-v1", 40, new BigDecimal("1000"), 70, new BigDecimal("5000"))
        );
        assessmentService = new AssessmentService(
                modelClient,
                new PolicyEngine(properties),
                assessmentRepository,
                processedEventRepository,
                properties
        );
        lenient().when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void duplicateEventDoesNotCallModelAgain() {
        TransactionSubmittedEvent event = event("evt-1", "TXN-11111111", new BigDecimal("25.00"));
        when(processedEventRepository.existsById("evt-1")).thenReturn(true);
        when(assessmentRepository.findByTransactionId("TXN-11111111")).thenReturn(Optional.of(sampleHold()));

        AssessmentResponse response = assessmentService.assess(event);

        assertThat(response.transactionId()).isEqualTo("TXN-11111111");
        verify(modelClient, never()).score(any(), any(), any(), any(), any());
    }

    @Test
    void modelTimeoutStoresHold() {
        TransactionSubmittedEvent event = event("evt-2", "TXN-22222222", new BigDecimal("25.00"));
        when(processedEventRepository.existsById("evt-2")).thenReturn(false);
        when(modelClient.score(any(), any(), any(), any(), any())).thenReturn(ModelScore.timeout());

        AssessmentResponse response = assessmentService.assess(event);

        assertThat(response.disposition()).isEqualTo(Disposition.HOLD);
        assertThat(response.policyReason()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(response.modelStatus()).isEqualTo(ModelStatus.TIMEOUT);
        assertThat(response.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        ArgumentCaptor<Assessment> captor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDisposition()).isEqualTo(Disposition.HOLD);
    }

    @Test
    void reviewRejectsWhenNotPending() {
        when(assessmentRepository.findByTransactionId("TXN-33333333")).thenReturn(Optional.of(sampleApproved()));
        assertThatThrownBy(() -> assessmentService.review(
                "TXN-33333333", new ReviewRequest(Disposition.DECLINE, "manual decline")))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    private static TransactionSubmittedEvent event(String eventId, String transactionId, BigDecimal amount) {
        return new TransactionSubmittedEvent(
                "TransactionSubmitted",
                "1",
                eventId,
                "lab5-demo",
                OffsetDateTime.now(),
                new TransactionSubmittedEvent.Payload(transactionId, "ACC-AABBCCDD", amount, "USD", "DEBIT")
        );
    }

    private static Assessment sampleHold() {
        return new Assessment(
                "RSK-HOLD0001", "TXN-11111111", "ACC-AABBCCDD", new BigDecimal("25.00"), "USD",
                "md287-risk-model", "1.0.0", 55, ModelStatus.OK, "policy-v1",
                Disposition.HOLD, "REVIEW_BAND", ReviewStatus.PENDING, "lab5-demo", "evt-1", OffsetDateTime.now()
        );
    }

    private static Assessment sampleApproved() {
        return new Assessment(
                "RSK-APPR0001", "TXN-33333333", "ACC-AABBCCDD", new BigDecimal("25.00"), "USD",
                "md287-risk-model", "1.0.0", 12, ModelStatus.OK, "policy-v1",
                Disposition.APPROVE, "LOW_SCORE_LOW_VALUE", ReviewStatus.NONE, "lab5-demo", "evt-3", OffsetDateTime.now()
        );
    }
}
