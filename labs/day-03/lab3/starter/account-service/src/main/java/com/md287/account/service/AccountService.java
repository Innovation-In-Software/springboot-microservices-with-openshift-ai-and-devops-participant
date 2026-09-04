package com.md287.account.service;

import com.md287.account.api.dto.AccountResponse;
import com.md287.account.api.dto.CreateAccountRequest;
import com.md287.account.api.dto.UpdateAccountRequest;
import com.md287.account.api.exception.AccountNotFoundException;
import com.md287.account.api.exception.BusinessRuleException;
import com.md287.account.api.exception.InvalidAccountStateException;
import com.md287.account.domain.Account;
import com.md287.account.domain.AccountStatus;
import com.md287.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD");

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        String currency = request.currency().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new BusinessRuleException(
                    "UNSUPPORTED_CURRENCY",
                    "Currency " + currency + " is not supported. Use USD."
            );
        }

        Account account = new Account(
                nextAccountId(),
                request.customerId(),
                request.accountType(),
                AccountStatus.PENDING,
                currency,
                request.nickname(),
                now
        );
        Account saved = accountRepository.save(account);
        log.info("Created account accountId={} type={} status={}",
                saved.getAccountId(), saved.getAccountType(), saved.getStatus());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountId) {
        return AccountResponse.from(requireAccount(accountId));
    }

    @Transactional
    public AccountResponse update(String accountId, UpdateAccountRequest request) {
        Account account = requireAccount(accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(accountId, account.getStatus().name(), "update");
        }
        account.updateNickname(request.nickname(), OffsetDateTime.now());
        log.info("Updated account attributes accountId={} status={}", accountId, account.getStatus());
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse activate(String accountId) {
        Account account = requireAccount(accountId);
        if (account.getStatus() == AccountStatus.ACTIVE) {
            return AccountResponse.from(account);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException(accountId, account.getStatus().name(), "activate");
        }
        account.activate(OffsetDateTime.now());
        log.info("Activated account accountId={} status={}", accountId, account.getStatus());
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse freeze(String accountId) {
        Account account = requireAccount(accountId);
        if (account.getStatus() == AccountStatus.FROZEN) {
            return AccountResponse.from(account);
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException(accountId, account.getStatus().name(), "freeze");
        }
        account.freeze(OffsetDateTime.now());
        log.info("Froze account accountId={} status={}", accountId, account.getStatus());
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse close(String accountId) {
        Account account = requireAccount(accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            return AccountResponse.from(account);
        }
        account.close(OffsetDateTime.now());
        log.info("Closed account accountId={} status={} retainedForAudit=true",
                accountId, account.getStatus());
        return AccountResponse.from(account);
    }

    private Account requireAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private String nextAccountId() {
        return "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
