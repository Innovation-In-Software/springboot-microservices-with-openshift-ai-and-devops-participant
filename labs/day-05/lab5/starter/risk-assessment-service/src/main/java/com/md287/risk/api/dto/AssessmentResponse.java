package com.md287.risk.api.dto;

import com.md287.risk.domain.Assessment;
import com.md287.risk.domain.Disposition;
import com.md287.risk.domain.ModelStatus;
import com.md287.risk.domain.ReviewStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AssessmentResponse(
        String assessmentId,
        String transactionId,
        String accountId,
        BigDecimal amount,
        String currency,
        String modelName,
        String modelVersion,
        Integer modelScore,
        ModelStatus modelStatus,
        String policyVersion,
        Disposition disposition,
        String policyReason,
        ReviewStatus reviewStatus,
        Disposition reviewerDecision,
        String reviewerReason,
        String correlationId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getAssessmentId(),
                assessment.getTransactionId(),
                assessment.getAccountId(),
                assessment.getAmount(),
                assessment.getCurrency(),
                assessment.getModelName(),
                assessment.getModelVersion(),
                assessment.getModelScore(),
                assessment.getModelStatus(),
                assessment.getPolicyVersion(),
                assessment.getDisposition(),
                assessment.getPolicyReason(),
                assessment.getReviewStatus(),
                assessment.getReviewerDecision(),
                assessment.getReviewerReason(),
                assessment.getCorrelationId(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }
}
