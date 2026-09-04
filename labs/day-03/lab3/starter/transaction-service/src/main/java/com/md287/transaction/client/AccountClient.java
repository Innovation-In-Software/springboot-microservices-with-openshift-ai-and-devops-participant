package com.md287.transaction.client;

import com.md287.transaction.api.exception.AccountNotEligibleException;
import com.md287.transaction.api.exception.AccountServiceUnavailableException;
import com.md287.transaction.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);

    private final RestClient accountRestClient;

    public AccountClient(RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }

    // TODO Lab 3 Step 5 — add @CircuitBreaker(name = "accountService", fallbackMethod = "accountUnavailable")
    public AccountView requireActiveAccount(String accountId) {
        try {
            AccountView account = accountRestClient.get()
                    .uri("/api/v1/accounts/{accountId}", accountId)
                    .header(CorrelationIdFilter.HEADER, nullToEmpty(MDC.get(CorrelationIdFilter.MDC_KEY)))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 404) {
                            throw new AccountNotEligibleException(accountId, "NOT_FOUND");
                        }
                        throw new AccountNotEligibleException(accountId, "UNKNOWN");
                    })
                    .body(AccountView.class);

            if (account == null) {
                throw new AccountNotEligibleException(accountId, "NOT_FOUND");
            }
            log.info("Validated account accountId={} status={}", account.accountId(), account.status());
            if (!"ACTIVE".equals(account.status())) {
                throw new AccountNotEligibleException(accountId, account.status());
            }
            return account;
        } catch (AccountNotEligibleException ex) {
            throw ex;
        } catch (RestClientException ex) {
            if (ex.getCause() instanceof AccountNotEligibleException eligible) {
                throw eligible;
            }
            throw new AccountServiceUnavailableException("Account Service did not respond", ex);
        }
    }

    // TODO Lab 3 Step 5 — add fallback methods. Never return a fake ACTIVE account.

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
