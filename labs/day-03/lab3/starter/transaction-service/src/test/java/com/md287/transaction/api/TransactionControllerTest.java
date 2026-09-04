package com.md287.transaction.api;

import com.md287.transaction.api.dto.CreateTransactionRequest;
import com.md287.transaction.api.dto.TransactionResponse;
import com.md287.transaction.api.exception.AccountNotEligibleException;
import com.md287.transaction.api.exception.GlobalExceptionHandler;
import com.md287.transaction.config.CorrelationIdFilter;
import com.md287.transaction.domain.TransactionStatus;
import com.md287.transaction.domain.TransactionType;
import com.md287.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void createReturns201() throws Exception {
        when(transactionService.create(any(CreateTransactionRequest.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "lab2-test")
                        .content("""
                                {
                                  "accountId": "ACC-AABBCCDD",
                                  "amount": 25.00,
                                  "currency": "USD",
                                  "type": "DEBIT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-11111111"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void createRejectsBadAccountId() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "real-customer-99",
                                  "amount": 25.00,
                                  "currency": "USD",
                                  "type": "DEBIT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createConflictWhenAccountFrozen() throws Exception {
        when(transactionService.create(any(CreateTransactionRequest.class)))
                .thenThrow(new AccountNotEligibleException("ACC-AABBCCDD", "FROZEN"));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "ACC-AABBCCDD",
                                  "amount": 25.00,
                                  "currency": "USD",
                                  "type": "DEBIT"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ELIGIBLE"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        when(transactionService.get("TXN-MISSING"))
                .thenThrow(new com.md287.transaction.api.exception.TransactionNotFoundException("TXN-MISSING"));

        mockMvc.perform(get("/api/v1/transactions/TXN-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    private TransactionResponse sample() {
        OffsetDateTime now = OffsetDateTime.parse("2026-01-15T10:00:00Z");
        return new TransactionResponse(
                "TXN-11111111",
                "ACC-AABBCCDD",
                new BigDecimal("25.00"),
                "USD",
                TransactionType.DEBIT,
                TransactionStatus.RECEIVED,
                "lab2-test",
                "evt-1",
                now,
                now
        );
    }
}
