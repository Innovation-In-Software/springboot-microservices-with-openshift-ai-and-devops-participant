package com.md287.account.api.exception;

import com.md287.account.api.dto.ApiErrorResponse;
import com.md287.account.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request
    ) {
        log.info("Account lookup missed accountId={} path={}", ex.getAccountId(), request.getRequestURI());
        return error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                "Account " + ex.getAccountId() + " was not found", request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request
    ) {
        log.info("Business rule rejected code={} path={}", ex.getCode(), request.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(
            InvalidAccountStateException ex,
            HttpServletRequest request
    ) {
        log.info(
                "Rejected status change accountId={} action={} currentStatus={}",
                ex.getAccountId(),
                ex.getAction(),
                ex.getCurrentStatus()
        );
        return error(
                HttpStatus.CONFLICT,
                "INVALID_ACCOUNT_STATE",
                "Account " + ex.getAccountId() + " cannot " + ex.getAction()
                        + " while status is " + ex.getCurrentStatus(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.info("Validation failed path={} fieldCount={}", request.getRequestURI(), details.size());
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.info("Unreadable request body path={}", request.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Request body could not be read", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error path={}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<String> details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
