# Lab 3 — Secure and Resilient Services

**Day:** 3 — Resilience, Security, and Testing  
**Capstone:** Harden services 1 and 2 of the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Protect Account and Transaction APIs with **JWT** (roles/scopes), add a **timeout plus circuit breaker** on the Account Service call, use a **safe fallback** (never auto-approve money movement), add tests, and confirm logs stay free of secrets.

---

## What you will finish with

By the end of this lab you will have:

- Both APIs reject callers without a Bearer token (**401**)
- Wrong scope returns **403** (not a silent 200)
- Transaction Service forwards the caller's JWT when it GETs an account
- If Account Service is down, POST `/api/v1/transactions` returns **503** `ACCOUNT_SERVICE_UNAVAILABLE` — no Kafka event, no fake ACTIVE account
- After enough failures the circuit opens; later calls fail fast
- Unit tests for security and the circuit breaker
- Logs that still use synthetic ids only — no `Authorization` header, no token payload

Lab 4 will containerize these services. Keep the JWT secret in `application.yml` for this classroom only. Production would use an identity provider such as Keycloak and RSA keys.

---

## Knowledge you need (from Day 3)

Read this once before you type. Each idea shows up in a later step.

| Day 3 idea | How it appears in this lab |
| --- | --- |
| **Resource server** | Each API validates a JWT. It does not log users in with a form. |
| **Scopes** | `accounts.read` / `accounts.write` and `transactions.read` / `transactions.write`. |
| **Workload vs user** | The human caller holds the JWT. Transaction Service **relays** that token to Account Service. |
| **Timeout** | RestClient already uses a 2s connect / 3s read timeout. That is the timeout. |
| **Circuit breaker** | Resilience4j wraps `AccountClient.requireActiveAccount`. Opens after **4** failed calls (50%); wait **10s**. |
| **Safe fallback** | Fallback throws 503. It must **not** return a synthetic ACTIVE account. |
| **409 does not trip it** | FROZEN or missing account is `ACCOUNT_NOT_ELIGIBLE`; listed in `ignoreExceptions`. |
| **Log redaction** | Log `accountId` and `status`. Never log Bearer tokens or JWT claims. |
| **GitHub Copilot Free** | Already on this VM. Ask Copilot to *explain* fallbacks. Reject any dummy ACTIVE `AccountView`. **Review before accept.** |

### Who can call what

| Token (from `tools/issue-jwt.py`) | Account API | Transaction API |
| --- | --- | --- |
| `teller` — `accounts.read accounts.write` | create / activate / freeze / close / get | **403** on POST |
| `ops` — `accounts.read transactions.read transactions.write` | GET account | POST and GET transactions |
| `readonly` — `*.read` only | GET | GET |
| no token | **401** | **401** |

### Fallback rule (memorize this)

When Account Service is slow, down, or the circuit is open:

```text
  POST /api/v1/transactions
           │
           ▼
  AccountClient ──timeout / 5xx / circuit open──► 503 ACCOUNT_SERVICE_UNAVAILABLE
           │                                         no row (or no publish)
           │                                         never "assume ACTIVE"
           ▼
        ACTIVE? ──yes──► persist RECEIVED and publish
```

---

## Environment basics (read this first)

Do **all** of this **on the Ablaze VM**. Your laptop is only the browser. Do **not** run `oc login`. Copy **one block at a time**. Do not paste two commands on the same line. Do **not** paste this whole guide (or a chat) into the terminal.

**Repo root (from Lab 0):** `%USERPROFILE%\MD287`. Example: `C:\Users\student.VLAB\MD287`. Do **not** clone. Do **not** run `mklink`. If the prompt shows the long `.vscode\...-participant` path, that is the same repo — `cd` to `MD287` before git commands.

Work in the **Day 3 starters**, not yesterday's Lab 1 / Lab 2 folders:

- `labs\day-03\lab3\starter\account-service`
- `labs\day-03\lab3\starter\transaction-service`

Those trees already contain finished Lab 1 and Lab 2 Java. You add JWT, token relay, the circuit breaker, and tests. Do **not** copy from a `solution/` folder.

| Task | How |
| --- | --- |
| Folder | **File → Open Folder** → `%USERPROFILE%\MD287` |
| Terminal A | **Account Service** (`mvn spring-boot:run` on **8081**). Leave it until the step says stop. |
| Terminal B | **Transaction Service** (`mvn spring-boot:run` on **8082**). Leave it until the step says stop. |
| Stop an app | **Ctrl+C**. If Maven prints `Terminate batch job (Y/N)?`, type **`Y`** and Enter. |
| Port 8081 or 8082 already in use | Leftover Lab 1 / Lab 2 `mvn spring-boot:run`. Click that tab, **Ctrl+C**, then **`Y`**. See Troubleshooting. |
| Terminal C | **Terminal → New Terminal**. All `curl.exe`, `python tools\issue-jwt.py`, and `$TELLER` / `$OPS` commands. |
| HTTP calls | **`curl.exe`** (not `curl`) |
| GitHub Copilot | Signed in during Lab 0. Review before accept. |

HTTP health stays public so operators can probe without a token.

If Lab 0 is not done, stop and finish it first. Lab 1 and Lab 2 ideas are already in these starters.

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 0 — Pull the latest repo

Get the latest Lab 3 guide and starter from GitHub. Do **not** clone. Do **not** run `mklink`.

```powershell
cd $env:USERPROFILE\MD287
git pull
```

**Expected:** `Already up to date.` or a Fast-forward. Prompt ends with `\MD287>`.

Then: **File → Open Folder** → `%USERPROFILE%\MD287` if it is not already open.

`git pull` only works **inside** the repo. Do not run it from `C:\Users\student.VLAB`.

### Step 1 — Start both Day 3 starters and prove they are still open

The starter still **permits all requests**. That is on purpose. You will lock them next.

**Do this:**

1. If Lab 1 or Lab 2 is still running on **8081** / **8082**, stop those Maven windows (**Ctrl+C**, then **`Y`**). You will start the **Day 3** folders instead. Leave Docker Desktop running. Repeating `docker compose up -d` is OK if `md287-account-db` / `md287-transaction-db` / `md287-kafka` are already **(healthy)**.

2. **Terminal A** — Account Service:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
docker compose up -d
Start-Sleep -Seconds 15
docker compose ps
mvn spring-boot:run
```

Wait until `STATUS` includes **`(healthy)`** on port **5433**, then wait for `Started AccountServiceApplication`. Leave this window running.

3. **Terminal B** — Transaction Service:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
docker compose up -d
Start-Sleep -Seconds 25
docker compose ps
mvn spring-boot:run
```

Wait until `md287-transaction-db` and `md287-kafka` are **(healthy)** and `md287-kafka-init` has exited, then wait for `Started TransactionServiceApplication`. Leave this window running.

4. **Terminal C** — health (no token):

```powershell
curl.exe -s http://localhost:8081/actuator/health
curl.exe -s http://localhost:8082/actuator/health
```

Optional — prove create is still open:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

**Expected result:**

- Both health JSON include `"status":"UP"`
- Create-account without a token still returns **HTTP:201** until Step 2

**Why this matters:** You always confirm the baseline before adding security. Otherwise a 401 later might mean "Postgres is down," not "JWT works."

---

### Step 2 — Protect Account Service with JWT

Dependencies and `md287.jwt.secret` are already in the starter.

**Do this:**

1. Stop Account Service (**Ctrl+C**, then **`Y`**). Leave Transaction Service running.

2. Replace `SecurityConfig.java`:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
@'
package com.md287.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAuthority("SCOPE_accounts.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/**").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/activate").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/freeze").hasAuthority("SCOPE_accounts.write")
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts/*/close").hasAuthority("SCOPE_accounts.write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(SecurityJsonHandlers.unauthorized(objectMapper))
                        .accessDeniedHandler(SecurityJsonHandlers.forbidden(objectMapper))
                )
                .build();
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\account\config\SecurityConfig.java"
Select-String -Path "src\main\java\com\md287\account\config\SecurityConfig.java" -Pattern "TODO|permitAll\(\)|JwtDecoder"
```

Need `JwtDecoder` and **no** `TODO`. Health/Swagger `permitAll` is expected. Business APIs must not be `anyRequest().permitAll()`.

3. Start Account Service again in **Terminal A**:

```powershell
mvn spring-boot:run
```

4. **Terminal C** — issue a teller token from the **lab3** folder (not inside the Maven module):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3"
$TELLER = (python tools\issue-jwt.py teller).Trim()
"teller length=$($TELLER.Length)"
```

Need a length around **200** (one JWT line). If this printed a Python traceback, raise a hand. Keep this Terminal C window for the rest of the lab so `$TELLER` stays set.

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8081/api/v1/accounts/ACC-MISSING

cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts

curl.exe -s http://localhost:8081/actuator/health
```

**Expected result:**

- GET without a token → **401** `UNAUTHENTICATED`
- POST with `$TELLER` → **201** `"status":"PENDING"`
- Health still **200** without a token

**Why this matters:** A resource server does not trust the network. It trusts a **signed** token.

---

### Step 3 — Protect Transaction Service the same way

Use the **same secret and issuer**.

**Do this:**

1. Stop Transaction Service (**Ctrl+C**, then **`Y`**). Leave Account Service running.

2. Replace Transaction `SecurityConfig.java`:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
@'
package com.md287.transaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/transactions/**").hasAuthority("SCOPE_transactions.read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions").hasAuthority("SCOPE_transactions.write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(SecurityJsonHandlers.unauthorized(objectMapper))
                        .accessDeniedHandler(SecurityJsonHandlers.forbidden(objectMapper))
                )
                .build();
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\config\SecurityConfig.java"
Select-String -Path "src\main\java\com\md287\transaction\config\SecurityConfig.java" -Pattern "TODO|JwtDecoder|transactions.write"
```

Need `JwtDecoder` and `SCOPE_transactions.write`. No `TODO`.

3. Start Transaction Service again in **Terminal B**:

```powershell
mvn spring-boot:run
```

4. **Terminal C** — issue ops, then prove 401 / 403. Re-issue teller if this is a **new** terminal (variables do not survive a closed window):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3"
$TELLER = (python tools\issue-jwt.py teller).Trim()
$OPS = (python tools\issue-jwt.py ops).Trim()
"teller length=$($TELLER.Length) ops length=$($OPS.Length)"
```

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

**Expected result:**

- No token → **401** `UNAUTHENTICATED`
- `$TELLER` (account scopes only) → **403** `ACCESS_DENIED`

Do **not** expect **201** with `$OPS` yet. Account GET also needs the relayed token (Step 4).

---

### Step 4 — Relay the Bearer token to Account Service

If Transaction Service calls Account Service **without** `Authorization`, Account Service now returns 401 and the transaction fails even for a valid ops user.

`BearerTokenRelayInterceptor` is already in the project. You only register it.

**Do this:**

1. Stop Transaction Service (**Ctrl+C**, then **`Y`**). Leave Account Service running.

2. Replace `RestClientConfig.java`:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
@'
package com.md287.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient accountRestClient(RestClient.Builder builder, Md287Properties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder
                .baseUrl(properties.accountService().baseUrl())
                .requestFactory(factory)
                .requestInterceptor(new BearerTokenRelayInterceptor())
                .build();
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\config\RestClientConfig.java"
Select-String -Path "src\main\java\com\md287\transaction\config\RestClientConfig.java" -Pattern "TODO|BearerTokenRelayInterceptor"
```

Need the interceptor line and **no** `TODO`.

3. Start Transaction Service again in **Terminal B**:

```powershell
mvn spring-boot:run
```

4. **Terminal C** — create and activate an account with `$TELLER`, then POST a transaction with `$OPS`.

If `$TELLER` / `$OPS` are empty, re-run the `issue-jwt.py` block from Step 3.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab3-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

Need **HTTP:201**. Copy **your** `accountId`. Do **not** call `ACC-YOUR-ID`.

```powershell
$accountId = "ACC-AABBCCDD"
```

Change the value to **your** create response, then activate:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -X POST http://localhost:8081/api/v1/accounts/$accountId/activate
```

Need **HTTP:200** `"status":"ACTIVE"`.

Point the transaction request file at that account:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
@"
{
  "accountId": "$accountId",
  "amount": 25.00,
  "currency": "USD",
  "type": "DEBIT"
}
"@ | Set-Content -Encoding ascii "requests\create-valid.json"
Get-Content "requests\create-valid.json"
```

Confirm the JSON shows **your** `ACC-...` id.

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab3-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

Need **HTTP:201** `"status":"RECEIVED"`. Copy **your** `transactionId`.

```powershell
$txnId = "TXN-00000000"
```

Change the value to **your** create response. Wait two seconds, then GET:

```powershell
Start-Sleep -Seconds 2
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  http://localhost:8082/api/v1/transactions/$txnId
```

Need **HTTP:200** `"status":"SUBMITTED"` (wait and GET again if it is still `RECEIVED`).

Look at **Terminal A** and **Terminal B**. Logs should show the same `correlationId=lab3-demo`. Neither log line contains `Bearer` or `eyJ`.

**Why this matters:** This is **user-delegated** access. Transaction Service is not a superuser; it borrows the caller's token.

---

### Step 5 — Circuit breaker and safe fallback

On `AccountClient.requireActiveAccount` add `@CircuitBreaker` and fallbacks that **rethrow** business 409 and wrap everything else in `AccountServiceUnavailableException`.

Never do this:

```java
return new AccountView(accountId, "ACTIVE", "USD"); // FORBIDDEN in this lab
```

**Do this:**

1. Stop Transaction Service (**Ctrl+C**, then **`Y`**). Leave Account Service running until the curl loop says to stop it.

2. Replace `AccountClient.java`:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
@'
package com.md287.transaction.client;

import com.md287.transaction.api.exception.AccountNotEligibleException;
import com.md287.transaction.api.exception.AccountServiceUnavailableException;
import com.md287.transaction.config.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountUnavailable")
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
                        if (status == 401 || status == 403) {
                            throw new AccountServiceUnavailableException(
                                    "Account Service rejected the caller's token", null);
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
        } catch (AccountNotEligibleException | AccountServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            if (ex.getCause() instanceof AccountNotEligibleException eligible) {
                throw eligible;
            }
            throw new AccountServiceUnavailableException("Account Service did not respond", ex);
        }
    }

    private AccountView accountUnavailable(String accountId, AccountNotEligibleException ex) {
        throw ex;
    }

    private AccountView accountUnavailable(String accountId, AccountServiceUnavailableException ex) {
        throw ex;
    }

    private AccountView accountUnavailable(String accountId, CallNotPermittedException ex) {
        log.warn("Account circuit open accountId={}", accountId);
        throw new AccountServiceUnavailableException("Account Service circuit is open", ex);
    }

    private AccountView accountUnavailable(String accountId, Throwable ex) {
        log.warn("Account Service fallback accountId={} reason={}", accountId, ex.getClass().getSimpleName());
        throw new AccountServiceUnavailableException("Account Service is temporarily unavailable", ex);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\client\AccountClient.java"
Select-String -Path "src\main\java\com\md287\transaction\client\AccountClient.java" -Pattern "TODO|CircuitBreaker|AccountView\(accountId"
```

Need `@CircuitBreaker` and **no** `TODO`. Must **not** construct a fake `AccountView`.

3. Start Transaction Service again in **Terminal B**:

```powershell
mvn spring-boot:run
```

4. Stop **Account Service only** (**Terminal A**: **Ctrl+C**, then **`Y`**). Leave Transaction Service running.

5. **Terminal C** — POST a transaction with `$OPS` **four times** (the breaker window is 4 calls / 50%). Re-issue `$OPS` if needed. Stay in the transaction-service folder so `requests\create-valid.json` exists:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
1..4 | ForEach-Object {
  Write-Host "POST $_"
  curl.exe -s -w "`nHTTP:%{http_code}`n" `
    -H "Authorization: Bearer $OPS" `
    -H "Content-Type: application/json" `
    --data-binary "@requests/create-valid.json" `
    http://localhost:8082/api/v1/transactions
}
```

**Expected result:** each call **503** `ACCOUNT_SERVICE_UNAVAILABLE`. No `TransactionSubmitted` publish. **Terminal B** logs fallback or `Account circuit open` — still no token text.

6. Start Account Service again in **Terminal A**:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
mvn spring-boot:run
```

Wait for `Started AccountServiceApplication`, then wait **~10 seconds** (open-state wait). **Terminal C**:

```powershell
Start-Sleep -Seconds 12
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $OPS" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab3-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

Need **HTTP:201** once Account is healthy and the breaker is closed / half-open success.

**Why this matters:** Fast failure is kinder than a 30-second hang. Inventing ACTIVE would move money against a frozen or missing account.

---

### Step 6 — Tests and log review

Add the security and circuit-breaker tests. They are **not** in the starter. Paste them here. Do **not** copy from a `solution/` folder.

**Do this:**

1. Stop **both** apps (**Ctrl+C**, then **`Y`** in Terminal A and Terminal B) so tests are not fighting over **8081** / **8082**. Leave Docker running (`AccountPersistenceTest` starts its **own** Postgres container).

2. Write Account security tests:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
@'
package com.md287.account.api;

import com.md287.account.api.exception.GlobalExceptionHandler;
import com.md287.account.config.CorrelationIdFilter;
import com.md287.account.config.SecurityConfig;
import com.md287.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "md287.jwt.issuer=md287-lab",
        "md287.jwt.secret=md287-lab-only-hmac-secret-32bytes!"
})
class AccountSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "CUST-0001",
                                  "accountType": "CHECKING",
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void getWithWriteScopeOnlyReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/ACC-AABBCCDD")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_accounts.write"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
'@ | Set-Content -Encoding ascii "src\test\java\com\md287\account\api\AccountSecurityTest.java"
```

3. Run Account tests:

```powershell
mvn test
```

**Expected:**

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
```

and `BUILD SUCCESS`.

(9 service + 4 controller + 2 persistence + 2 security.) `AccountPersistenceTest` uses Testcontainers. If Docker cannot pull `postgres:16-alpine`, those 2 tests error — raise a hand; do not skip JWT.

Mockito / Byte Buddy lines are **warnings**. Look at the **Results** block.

4. Write Transaction security and circuit-breaker tests:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
New-Item -ItemType Directory -Force -Path "src\test\java\com\md287\transaction\client" | Out-Null
@'
package com.md287.transaction.api;

import com.md287.transaction.api.exception.GlobalExceptionHandler;
import com.md287.transaction.config.CorrelationIdFilter;
import com.md287.transaction.config.SecurityConfig;
import com.md287.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "md287.jwt.issuer=md287-lab",
        "md287.jwt.secret=md287-lab-only-hmac-secret-32bytes!"
})
class TransactionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "ACC-AABBCCDD",
                                  "amount": 25.00,
                                  "currency": "USD",
                                  "type": "DEBIT"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createWithReadScopeReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_transactions.read")))
                        .content("""
                                {
                                  "accountId": "ACC-AABBCCDD",
                                  "amount": 25.00,
                                  "currency": "USD",
                                  "type": "DEBIT"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
'@ | Set-Content -Encoding ascii "src\test\java\com\md287\transaction\api\TransactionSecurityTest.java"

@'
package com.md287.transaction.client;

import com.md287.transaction.api.exception.AccountServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountCircuitBreakerTest {

    @Test
    void circuitOpensAfterRepeatedFailuresAndDoesNotInventSuccess() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("accountService", config);

        Supplier<String> alwaysDown = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            throw new AccountServiceUnavailableException("Account Service did not respond", null);
        });

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(alwaysDown::get).isInstanceOf(AccountServiceUnavailableException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(alwaysDown::get).isInstanceOf(CallNotPermittedException.class);
    }
}
'@ | Set-Content -Encoding ascii "src\test\java\com\md287\transaction\client\AccountCircuitBreakerTest.java"
```

5. Run Transaction tests:

```powershell
mvn test
```

**Expected:**

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

and `BUILD SUCCESS`.

(3 service + 4 controller + 3 consumer + 2 security + 1 circuit breaker.)

GitHub Copilot (Free, already on this VM): ask it to **explain** the fallback methods. Reject any suggestion that returns a dummy ACTIVE `AccountView`. Review-before-accept is a course rule.

**Checklist — logs must NOT contain:**

- [ ] `Authorization` header or the word `Bearer` plus a token
- [ ] JWT payload dumps (`scope`, `sub`, strings starting `eyJ`)
- [ ] A fake ACTIVE status on the 503 path
- [ ] Emails, card-like numbers, or full request bodies

**Checklist — logs MAY contain:**

- [ ] `accountId`, `transactionId`, `correlationId`
- [ ] `Account circuit open` / fallback class name
- [ ] HTTP path (not the body)

---

## Success criteria

- [ ] No token → **401** on both business APIs
- [ ] Teller cannot POST transactions (**403**)
- [ ] Ops can POST a transaction for an ACTIVE account (**201**)
- [ ] Token is forwarded; Account GET is authorized
- [ ] Account down → **503** `ACCOUNT_SERVICE_UNAVAILABLE`, no auto-approve
- [ ] Circuit opens after **4** failed calls; 409 FROZEN does not trip it
- [ ] `mvn test` passes on both services (Account includes `AccountPersistenceTest`)
- [ ] Logs stay synthetic and secret-free

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `Terminate batch job (Y/N)?` | Type **`Y`** and Enter. |
| `Port 8081 is already in use` / `Port 8082 is already in use` | Leftover Lab 1 / Lab 2 / Lab 3 Maven. **Ctrl+C**, then **`Y`**. If you cannot find it, run the free-port block below. |
| 401 with a token | Secret mismatch; extra newline when pasting `$TELLER`; token expired (re-run `issue-jwt.py`); you used `curl` not `curl.exe`. |
| `$TELLER` empty / length huge | You are not in `labs\day-03\lab3`, or Python printed a traceback. Re-run `python tools\issue-jwt.py teller` alone. |
| 403 with ops token | Missing `transactions.write`, or Account GET missing `accounts.read` on that token. Use `python tools\issue-jwt.py ops`. |
| 401 from Account during POST transaction | Interceptor not registered in `RestClientConfig`. Re-do Step 4. |
| Status stuck RECEIVED | Kafka / consumer — same as Lab 2. Confirm `md287-kafka` is healthy. |
| Circuit never opens | Need enough **failed** calls (**4**) with Account **stopped**. 409 FROZEN is ignored on purpose. |
| 503 after Account is back | Wait **10s** (open-state wait) then retry. Confirm Terminal A shows `Started AccountServiceApplication`. |
| `ACCOUNT_NOT_ELIGIBLE` | Account is not ACTIVE. Create + activate with `$TELLER` and update `requests\create-valid.json`. |
| Tests 401 unexpectedly | You imported `SecurityConfig` into `@WebMvcTest` without `jwt()`. Use the Step 6 test files as pasted. |
| `AccountPersistenceTest` errors | Docker engine must be running. Testcontainers pulls `postgres:16-alpine`. |
| Maven `Nothing to compile` | Truncated paste. Re-run the `Select-String` check for that step. |
| PSReadLine crash / huge paste | Copy **one** command block only. Do not paste a chat. |
| `git pull`: not a git repository | `cd $env:USERPROFILE\MD287` then `git pull`. |
| `curl` output looks like PowerShell errors | Use `curl.exe`. |

Free port **8081** or **8082**:

```powershell
foreach ($port in 8081, 8082) {
  $p = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
  if ($p) { Stop-Process -Id $p -Force; "Stopped PID $p on $port" } else { "$port is free" }
}
```

---

## Clean shutdown

Stop the apps (**Ctrl+C**, then **`Y`**). **Leave** Postgres and Kafka running if you will continue practicing. Do **not** run `oc login`.

```powershell
# Optional — only if you are done for the day and the instructor says to stop Docker:
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\transaction-service"
docker compose down
cd "$env:USERPROFILE\MD287\labs\day-03\lab3\starter\account-service"
docker compose down
```

---

## Optional stretch (only if you finished early)

- Draw user-delegated access vs a **client-credentials** service account (you did not build the second one).
- Keycloak would replace `issue-jwt.py` in an environment with an IdP.
- Freeze the account with `$TELLER`, POST a transaction with `$OPS`, and confirm **409** (not 503) — that must **not** open the circuit.

Do **not** add OpenShift, containers, or OpenShift AI here.

---

## What you built in the capstone

```text
Day 1  Account Service
Day 2  + Transaction Service
Day 3  + JWT, Resilience4j, tests  ← you are here
Day 4  + containers, OpenShift, CI/CD
Day 5  + Risk Assessment Service and OpenShift AI
```

---

© 2026 Innovation In Software Corporation
