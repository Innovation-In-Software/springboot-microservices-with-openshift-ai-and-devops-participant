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
        // TODO Lab 1 Step 4 — create a PENDING account and save it.
        // See LAB-1-GUIDE.md Step 4.
        throw new UnsupportedOperationException("TODO: implement create()");
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountId) {
        // TODO Lab 1 Step 4 — load the account or throw AccountNotFoundException.
        throw new UnsupportedOperationException("TODO: implement get()");
    }

    @Transactional
    public AccountResponse update(String accountId, UpdateAccountRequest request) {
        // TODO Lab 1 Step 4 — update nickname only when the account is not CLOSED.
        throw new UnsupportedOperationException("TODO: implement update()");
    }

    @Transactional
    public AccountResponse activate(String accountId) {
        // TODO Lab 1 Step 4 — PENDING or FROZEN -> ACTIVE. CLOSED is not allowed. ACTIVE is idempotent.
        throw new UnsupportedOperationException("TODO: implement activate()");
    }

    @Transactional
    public AccountResponse freeze(String accountId) {
        // TODO Lab 1 Step 4 — ACTIVE -> FROZEN. FROZEN is idempotent. PENDING and CLOSED are not allowed.
        throw new UnsupportedOperationException("TODO: implement freeze()");
    }

    @Transactional
    public AccountResponse close(String accountId) {
        // TODO Lab 1 Step 4 — mark CLOSED and set closedAt. Do not delete the row.
        throw new UnsupportedOperationException("TODO: implement close()");
    }

    private Account requireAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private String nextAccountId() {
        return "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
