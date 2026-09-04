package com.md287.risk.api;

import com.md287.risk.api.dto.AssessmentResponse;
import com.md287.risk.api.dto.ReviewRequest;
import com.md287.risk.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{transactionId}")
    public AssessmentResponse get(@PathVariable String transactionId) {
        return assessmentService.getByTransactionId(transactionId);
    }

    @GetMapping
    public List<AssessmentResponse> list(@RequestParam(required = false) String disposition) {
        // Classroom queue is HOLD rows waiting for human review.
        return assessmentService.listHolds();
    }

    @PostMapping("/{transactionId}/review")
    public AssessmentResponse review(
            @PathVariable String transactionId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return assessmentService.review(transactionId, request);
    }
}
