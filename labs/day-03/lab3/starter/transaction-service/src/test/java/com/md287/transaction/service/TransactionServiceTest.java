package com.md287.transaction.service;

import com.md287.transaction.api.dto.CreateTransactionRequest;
import com.md287.transaction.api.exception.AccountNotEligibleException;
import com.md287.transaction.api.exception.BusinessRuleException;
import com.md287.transaction.client.AccountClient;
import com.md287.transaction.client.AccountView;
import com.md287.transaction.domain.Transaction;
import com.md287.transaction.domain.TransactionStatus;
import com.md287.transaction.domain.TransactionType;
import com.md287.transaction.messaging.TransactionSubmittedEvent;
import com.md287.transaction.messaging.TransactionSubmittedPublisher;
import com.md287.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountClient accountClient;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionSubmittedPublisher publisher;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountClient, transactionRepository, publisher);
        org.mockito.Mockito.lenient()
                .when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPersistsReceivedAndPublishesEvent() {
        when(accountClient.requireActiveAccount("ACC-AABBCCDD"))
                .thenReturn(new AccountView("ACC-AABBCCDD", "ACTIVE", "USD"));

        var response = transactionService.create(new CreateTransactionRequest(
                "ACC-AABBCCDD", new BigDecimal("25.00"), "USD", TransactionType.DEBIT));

        assertThat(response.status()).isEqualTo(TransactionStatus.RECEIVED);
        assertThat(response.transactionId()).startsWith("TXN-");
        assertThat(response.eventId()).isNotBlank();

        ArgumentCaptor<TransactionSubmittedEvent> captor = ArgumentCaptor.forClass(TransactionSubmittedEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TransactionSubmitted");
        assertThat(captor.getValue().payload().accountId()).isEqualTo("ACC-AABBCCDD");
    }

    @Test
    void createRejectsFrozenAccount() {
        doThrow(new AccountNotEligibleException("ACC-AABBCCDD", "FROZEN"))
                .when(accountClient).requireActiveAccount("ACC-AABBCCDD");

        assertThatThrownBy(() -> transactionService.create(new CreateTransactionRequest(
                "ACC-AABBCCDD", new BigDecimal("25.00"), "USD", TransactionType.DEBIT)))
                .isInstanceOf(AccountNotEligibleException.class);
        verify(publisher, never()).publish(any());
    }

    @Test
    void createRejectsUnsupportedCurrency() {
        when(accountClient.requireActiveAccount("ACC-AABBCCDD"))
                .thenReturn(new AccountView("ACC-AABBCCDD", "ACTIVE", "USD"));

        assertThatThrownBy(() -> transactionService.create(new CreateTransactionRequest(
                "ACC-AABBCCDD", new BigDecimal("25.00"), "EUR", TransactionType.DEBIT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USD");
        verify(publisher, never()).publish(any());
    }
}
