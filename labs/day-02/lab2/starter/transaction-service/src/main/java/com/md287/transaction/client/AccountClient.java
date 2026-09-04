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

    public AccountView requireActiveAccount(String accountId) {
        // TODO Lab 2 Step 4 — GET Account Service and require status ACTIVE.
        throw new UnsupportedOperationException("TODO: implement requireActiveAccount()");
    }
}
