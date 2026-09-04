package com.md287.transaction.service;

import com.md287.transaction.api.dto.CreateTransactionRequest;
import com.md287.transaction.api.dto.TransactionResponse;
import com.md287.transaction.api.exception.BusinessRuleException;
import com.md287.transaction.api.exception.TransactionNotFoundException;
import com.md287.transaction.client.AccountClient;
import com.md287.transaction.config.CorrelationIdFilter;
import com.md287.transaction.domain.Transaction;
import com.md287.transaction.domain.TransactionStatus;
import com.md287.transaction.messaging.TransactionSubmittedEvent;
import com.md287.transaction.messaging.TransactionSubmittedPublisher;
import com.md287.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD");

    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;
    private final TransactionSubmittedPublisher publisher;

    public TransactionService(
            AccountClient accountClient,
            TransactionRepository transactionRepository,
            TransactionSubmittedPublisher publisher
    ) {
        this.accountClient = accountClient;
        this.transactionRepository = transactionRepository;
        this.publisher = publisher;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        // TODO Lab 2 Step 5 — validate the account, save RECEIVED, publish TransactionSubmitted.
        throw new UnsupportedOperationException("TODO: implement create()");
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(String transactionId) {
        // TODO Lab 2 Step 5 — load the transaction or throw TransactionNotFoundException.
        throw new UnsupportedOperationException("TODO: implement get()");
    }

    private String currentCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }

    private String nextTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
