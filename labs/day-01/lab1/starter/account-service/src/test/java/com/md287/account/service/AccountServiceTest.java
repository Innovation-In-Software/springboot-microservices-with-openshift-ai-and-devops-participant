package com.md287.account.service;

import com.md287.account.api.dto.CreateAccountRequest;
import com.md287.account.api.dto.UpdateAccountRequest;
import com.md287.account.api.exception.AccountNotFoundException;
import com.md287.account.api.exception.BusinessRuleException;
import com.md287.account.api.exception.InvalidAccountStateException;
import com.md287.account.domain.Account;
import com.md287.account.domain.AccountStatus;
import com.md287.account.domain.AccountType;
import com.md287.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
        org.mockito.Mockito.lenient()
                .when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createStartsAccountInPendingStatus() {
        var request = new CreateAccountRequest("CUST-0001", AccountType.CHECKING, "USD", "Payroll");

        var response = accountService.create(request);

        assertThat(response.status()).isEqualTo(AccountStatus.PENDING);
        assertThat(response.accountId()).startsWith("ACC-");
        assertThat(response.customerId()).isEqualTo("CUST-0001");

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.PENDING);
    }

    @Test
    void createRejectsUnsupportedCurrency() {
        var request = new CreateAccountRequest("CUST-0001", AccountType.SAVINGS, "EUR", null);

        assertThatThrownBy(() -> accountService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void activateMovesPendingToActive() {
        Account pending = sample("ACC-11111111", AccountStatus.PENDING);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(pending));

        var response = accountService.activate("ACC-11111111");

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void activateIsIdempotentWhenAlreadyActive() {
        Account active = sample("ACC-11111111", AccountStatus.ACTIVE);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(active));

        var response = accountService.activate("ACC-11111111");

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void freezeRequiresActiveStatus() {
        Account pending = sample("ACC-11111111", AccountStatus.PENDING);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> accountService.freeze("ACC-11111111"))
                .isInstanceOf(InvalidAccountStateException.class);
    }

    @Test
    void freezeMovesActiveToFrozen() {
        Account active = sample("ACC-11111111", AccountStatus.ACTIVE);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(active));

        var response = accountService.freeze("ACC-11111111");

        assertThat(response.status()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    void closeRetainsTheRecord() {
        Account active = sample("ACC-11111111", AccountStatus.ACTIVE);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(active));

        var response = accountService.close("ACC-11111111");

        assertThat(response.status()).isEqualTo(AccountStatus.CLOSED);
        assertThat(response.closedAt()).isNotNull();
        verify(accountRepository).findById("ACC-11111111");
    }

    @Test
    void updateRejectedWhenClosed() {
        Account closed = sample("ACC-11111111", AccountStatus.CLOSED);
        when(accountRepository.findById("ACC-11111111")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> accountService.update("ACC-11111111", new UpdateAccountRequest("New name")))
                .isInstanceOf(InvalidAccountStateException.class);
    }

    @Test
    void getThrowsWhenMissing() {
        when(accountRepository.findById("ACC-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get("ACC-MISSING"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private Account sample(String id, AccountStatus status) {
        Account account = new Account(
                id,
                "CUST-0001",
                AccountType.CHECKING,
                AccountStatus.PENDING,
                "USD",
                "Payroll",
                OffsetDateTime.parse("2026-01-15T10:00:00Z")
        );
        if (status == AccountStatus.ACTIVE || status == AccountStatus.FROZEN || status == AccountStatus.CLOSED) {
            account.activate(OffsetDateTime.parse("2026-01-15T10:05:00Z"));
        }
        if (status == AccountStatus.FROZEN) {
            account.freeze(OffsetDateTime.parse("2026-01-15T10:10:00Z"));
        }
        if (status == AccountStatus.CLOSED) {
            account.close(OffsetDateTime.parse("2026-01-15T10:15:00Z"));
        }
        return account;
    }
}
