package com.md287.account.api;

import com.md287.account.api.dto.AccountResponse;
import com.md287.account.api.dto.CreateAccountRequest;
import com.md287.account.api.dto.UpdateAccountRequest;
import com.md287.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{accountId}")
                .buildAndExpand(created.accountId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable String accountId) {
        return accountService.get(accountId);
    }

    @PatchMapping("/{accountId}")
    public AccountResponse update(
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return accountService.update(accountId, request);
    }

    @PostMapping("/{accountId}/activate")
    public AccountResponse activate(@PathVariable String accountId) {
        return accountService.activate(accountId);
    }

    @PostMapping("/{accountId}/freeze")
    public AccountResponse freeze(@PathVariable String accountId) {
        return accountService.freeze(accountId);
    }

    @PostMapping("/{accountId}/close")
    public AccountResponse close(@PathVariable String accountId) {
        return accountService.close(accountId);
    }
}
