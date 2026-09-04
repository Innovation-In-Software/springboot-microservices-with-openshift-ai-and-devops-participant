package com.md287.risk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.md287.risk.api.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

public final class SecurityJsonHandlers {

    private SecurityJsonHandlers() {
    }

    public static AuthenticationEntryPoint unauthorized(ObjectMapper mapper) {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) ->
                write(mapper, request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                        "Authentication is required");
    }

    public static AccessDeniedHandler forbidden(ObjectMapper mapper) {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) ->
                write(mapper, request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "The token does not have the required role or scope");
    }

    private static void write(
            ObjectMapper mapper,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY),
                List.of()
        );
        mapper.writeValue(response.getOutputStream(), body);
    }
}
