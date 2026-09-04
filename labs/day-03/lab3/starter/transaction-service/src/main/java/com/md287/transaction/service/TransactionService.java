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
        accountClient.requireActiveAccount(request.accountId());

        String currency = request.currency().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new BusinessRuleException(
                    "UNSUPPORTED_CURRENCY",
                    "Currency " + currency + " is not supported. Use USD."
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        String correlationId = currentCorrelationId();
        String eventId = UUID.randomUUID().toString();
        Transaction transaction = new Transaction(
                nextTransactionId(),
                request.accountId(),
                request.amount(),
                currency,
                request.type(),
                TransactionStatus.RECEIVED,
                correlationId,
                eventId,
                now
        );
        Transaction saved = transactionRepository.save(transaction);

        publisher.publish(new TransactionSubmittedEvent(
                TransactionSubmittedEvent.TYPE,
                TransactionSubmittedEvent.VERSION,
                eventId,
                correlationId,
                now,
                new TransactionSubmittedEvent.Payload(
                        saved.getTransactionId(),
                        saved.getAccountId(),
                        saved.getAmount(),
                        saved.getCurrency(),
                        saved.getType()
                )
        ));

        log.info("Created transaction transactionId={} accountId={} status={}",
                saved.getTransactionId(), saved.getAccountId(), saved.getStatus());
        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(String transactionId) {
        return TransactionResponse.from(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId)));
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
