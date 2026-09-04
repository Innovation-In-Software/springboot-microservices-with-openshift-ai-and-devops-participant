package com.md287.account;

import com.md287.account.api.dto.CreateAccountRequest;
import com.md287.account.domain.AccountStatus;
import com.md287.account.domain.AccountType;
import com.md287.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AccountPersistenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("account_db")
            .withUsername("account")
            .withPassword("account");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountService accountService;

    @Test
    void healthIsUp() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void createRetrieveActivateAndClosePersistInPostgres() {
        var created = accountService.create(
                new CreateAccountRequest("CUST-0001", AccountType.SAVINGS, "USD", "Travel")
        );
        assertThat(created.status()).isEqualTo(AccountStatus.PENDING);

        var loaded = accountService.get(created.accountId());
        assertThat(loaded.nickname()).isEqualTo("Travel");

        var activated = accountService.activate(created.accountId());
        assertThat(activated.status()).isEqualTo(AccountStatus.ACTIVE);

        var closed = accountService.close(created.accountId());
        assertThat(closed.status()).isEqualTo(AccountStatus.CLOSED);
        assertThat(accountService.get(created.accountId()).status()).isEqualTo(AccountStatus.CLOSED);
    }
}
