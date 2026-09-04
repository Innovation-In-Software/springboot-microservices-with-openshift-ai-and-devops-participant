package com.md287.account.api;

import com.md287.account.api.dto.AccountResponse;
import com.md287.account.api.dto.CreateAccountRequest;
import com.md287.account.api.exception.AccountNotFoundException;
import com.md287.account.api.exception.GlobalExceptionHandler;
import com.md287.account.api.exception.InvalidAccountStateException;
import com.md287.account.config.CorrelationIdFilter;
import com.md287.account.domain.AccountStatus;
import com.md287.account.domain.AccountType;
import com.md287.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createReturns201AndLocation() throws Exception {
        AccountResponse created = sample("ACC-AABBCCDD", AccountStatus.PENDING);
        when(accountService.create(any(CreateAccountRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "lab1-test-1")
                        .content("""
                                {
                                  "customerId": "CUST-0001",
                                  "accountType": "CHECKING",
                                  "currency": "USD",
                                  "nickname": "Payroll"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/accounts/ACC-AABBCCDD"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.accountId").value("ACC-AABBCCDD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createRejectsNonSyntheticCustomerId() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "john.doe@bank.com",
                                  "accountType": "CHECKING",
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getReturns404ForUnknownAccount() throws Exception {
        when(accountService.get("ACC-MISSING")).thenThrow(new AccountNotFoundException("ACC-MISSING"));

        mockMvc.perform(get("/api/v1/accounts/ACC-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void freezeConflictReturns409() throws Exception {
        when(accountService.freeze("ACC-AABBCCDD"))
                .thenThrow(new InvalidAccountStateException("ACC-AABBCCDD", "PENDING", "freeze"));

        mockMvc.perform(post("/api/v1/accounts/ACC-AABBCCDD/freeze"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_STATE"));
    }

    private AccountResponse sample(String id, AccountStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-01-15T10:00:00Z");
        return new AccountResponse(
                id,
                "CUST-0001",
                AccountType.CHECKING,
                status,
                "USD",
                "Payroll",
                now,
                now,
                null
        );
    }
}
