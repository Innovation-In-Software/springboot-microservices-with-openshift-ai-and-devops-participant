package com.md287.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @Column(name = "assessment_id", nullable = false, length = 36)
    private String assessmentId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "model_name", length = 80)
    private String modelName;

    @Column(name = "model_version", length = 40)
    private String modelVersion;

    @Column(name = "model_score")
    private Integer modelScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_status", nullable = false, length = 20)
    private ModelStatus modelStatus;

    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition", nullable = false, length = 20)
    private Disposition disposition;

    @Column(name = "policy_reason", nullable = false, length = 40)
    private String policyReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_decision", length = 20)
    private Disposition reviewerDecision;

    @Column(name = "reviewer_reason", length = 200)
    private String reviewerReason;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Assessment() {
    }

    public Assessment(
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
            String correlationId,
            String eventId,
            OffsetDateTime createdAt
    ) {
        this.assessmentId = assessmentId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.modelScore = modelScore;
        this.modelStatus = modelStatus;
        this.policyVersion = policyVersion;
        this.disposition = disposition;
        this.policyReason = policyReason;
        this.reviewStatus = reviewStatus;
        this.correlationId = correlationId;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public Integer getModelScore() {
        return modelScore;
    }

    public ModelStatus getModelStatus() {
        return modelStatus;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public Disposition getReviewerDecision() {
        return reviewerDecision;
    }

    public String getReviewerReason() {
        return reviewerReason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getEventId() {
        return eventId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void completeReview(Disposition decision, String reason, OffsetDateTime now) {
        this.reviewerDecision = decision;
        this.reviewerReason = reason;
        this.disposition = decision;
        this.reviewStatus = ReviewStatus.COMPLETED;
        this.updatedAt = now;
    }
}
