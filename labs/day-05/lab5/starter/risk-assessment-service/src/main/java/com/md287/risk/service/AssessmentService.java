package com.md287.risk.service;

import com.md287.risk.api.dto.AssessmentResponse;
import com.md287.risk.api.dto.ReviewRequest;
import com.md287.risk.api.exception.AssessmentNotFoundException;
import com.md287.risk.api.exception.ReviewNotAllowedException;
import com.md287.risk.config.Md287Properties;
import com.md287.risk.domain.Assessment;
import com.md287.risk.domain.Disposition;
import com.md287.risk.domain.ProcessedEvent;
import com.md287.risk.domain.ReviewStatus;
import com.md287.risk.messaging.TransactionSubmittedEvent;
import com.md287.risk.model.ModelClient;
import com.md287.risk.model.ModelScore;
import com.md287.risk.policy.PolicyDecision;
import com.md287.risk.policy.PolicyEngine;
import com.md287.risk.repository.AssessmentRepository;
import com.md287.risk.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final ModelClient modelClient;
    private final PolicyEngine policyEngine;
    private final AssessmentRepository assessmentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Md287Properties properties;

    public AssessmentService(
            ModelClient modelClient,
            PolicyEngine policyEngine,
            AssessmentRepository assessmentRepository,
            ProcessedEventRepository processedEventRepository,
            Md287Properties properties
    ) {
        this.modelClient = modelClient;
        this.policyEngine = policyEngine;
        this.assessmentRepository = assessmentRepository;
        this.processedEventRepository = processedEventRepository;
        this.properties = properties;
    }

    @Transactional
    public AssessmentResponse assess(TransactionSubmittedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event ignored eventId={} transactionId={}",
                    event.eventId(), event.payload().transactionId());
            return assessmentRepository.findByTransactionId(event.payload().transactionId())
                    .map(AssessmentResponse::from)
                    .orElseThrow(() -> new AssessmentNotFoundException(event.payload().transactionId()));
        }

        TransactionSubmittedEvent.Payload payload = event.payload();
        ModelScore model = modelClient.score(
                payload.transactionId(),
                payload.accountId(),
                payload.amount(),
                payload.currency(),
                payload.type()
        );
        PolicyDecision decision = policyEngine.decide(payload.amount(), model);
        ReviewStatus reviewStatus = decision.disposition() == Disposition.HOLD
                ? ReviewStatus.PENDING
                : ReviewStatus.NONE;

        OffsetDateTime now = OffsetDateTime.now();
        Assessment assessment = new Assessment(
                nextId(),
                payload.transactionId(),
                payload.accountId(),
                payload.amount(),
                payload.currency(),
                model.modelName(),
                model.modelVersion(),
                model.score(),
                model.status(),
                properties.policy().version(),
                decision.disposition(),
                decision.reason(),
                reviewStatus,
                event.correlationId() == null ? "none" : event.correlationId(),
                event.eventId(),
                now
        );
        assessmentRepository.save(assessment);
        processedEventRepository.save(new ProcessedEvent(event.eventId(), payload.transactionId(), now));
        log.info("Assessed transactionId={} disposition={} reason={} modelStatus={}",
                payload.transactionId(), decision.disposition(), decision.reason(), model.status());
        return AssessmentResponse.from(assessment);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getByTransactionId(String transactionId) {
        return assessmentRepository.findByTransactionId(transactionId)
                .map(AssessmentResponse::from)
                .orElseThrow(() -> new AssessmentNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> listHolds() {
        return assessmentRepository.findByDisposition(Disposition.HOLD).stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional
    public AssessmentResponse review(String transactionId, ReviewRequest request) {
        if (request.decision() == Disposition.HOLD) {
            throw new ReviewNotAllowedException(transactionId, "Review must choose APPROVE or DECLINE");
        }
        Assessment assessment = assessmentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AssessmentNotFoundException(transactionId));
        if (assessment.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ReviewNotAllowedException(transactionId,
                    "Assessment " + transactionId + " is not waiting for human review");
        }
        assessment.completeReview(request.decision(), request.reason(), OffsetDateTime.now());
        log.info("Human review completed transactionId={} decision={}", transactionId, request.decision());
        return AssessmentResponse.from(assessment);
    }

    private static String nextId() {
        return "RSK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
