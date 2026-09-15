# Lab 3 — Secure and Resilient Services

**Day:** 3 — Resilience, Security, and Testing  
**Capstone:** Harden services 1 and 2 of the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Protect Account and Transaction APIs with **JWT** (roles/scopes), add a **timeout plus circuit breaker** on the Account Service call, use a **safe fallback** (never auto-approve money movement), add tests, and confirm logs stay free of secrets.

---

## What you will finish with

- Both APIs reject callers without a Bearer token (**401**)
- Wrong scope returns **403** (not a silent 200)
- Transaction Service forwards the caller's JWT when it GETs an account
- If Account Service is down, POST `/api/v1/transactions` returns **503** `ACCOUNT_SERVICE_UNAVAILABLE` — no Kafka event, no fake ACTIVE account
- After enough failures the circuit opens; later calls fail fast
- Unit tests for security and the circuit breaker
- Logs still use synthetic ids only — no `Authorization` header, no token payload

Lab 4 will containerize these services. Keep the JWT secret in `application.yml` for this classroom only. Production would use an identity provider such as Keycloak and RSA keys.

---

## Knowledge you need (from Day 3)

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

## Environment basics

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (Ctrl+`)

| Task | How |
| --- | --- |
| Open Account Service | File → Open Folder → `labs/day-03/lab3/starter/account-service` |
| Open Transaction Service | … `starter/transaction-service` (second VS Code window is fine) |
| HTTP | **`curl.exe`**, not `curl` |
| GitHub Copilot | **Copilot Free** is already on this VM. Use it to explain and draft; **review before accept**. |

**You need:**

- Java 21, Maven 3.9+, Docker Desktop
- Open the **Day 3** starters (`labs/day-03/lab3/starter/account-service` and `transaction-service`), not yesterday's trees
- HTTP calls: **`curl.exe`** (not `curl` — PowerShell aliases `curl`)
- Tokens from `tools/issue-jwt.py` (HMAC classroom JWT)

HTTP health stays public so operators can probe without a token.

---

## Steps from the training slides

### Step 1 — Start both services and prove they are still open

The starter still **permits all requests**. That is on purpose. You will lock them next.

**Do this:**

1. In `starter/account-service`:

```powershell
docker compose up -d
mvn spring-boot:run
```

2. In `starter/transaction-service`:

```powershell
docker compose up -d
mvn spring-boot:run
```

3. Health (no token):

```powershell
curl.exe -s http://localhost:8081/actuator/health
curl.exe -s http://localhost:8082/actuator/health
```

**Expected result:** both `"status":"UP"`. Create-account without a token still works (201) until Step 2.

**Why this matters:** You always confirm the baseline before adding security. Otherwise a 401 later might mean "Postgres is down," not "JWT works."

---

### Step 2 — Protect Account Service with JWT

Dependencies and `md287.jwt.secret` are already in the starter.

Open `config/SecurityConfig.java`. Replace the filter chain with the solution pattern:

- `JwtDecoder` using HS256 and `JwtProperties.secret()`
- permit: actuator health/info, swagger
- `GET /api/v1/accounts/**` → `SCOPE_accounts.read`
- create / patch / activate / freeze / close → `SCOPE_accounts.write`
- `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`
- JSON 401/403 via `SecurityJsonHandlers` (already in the project)

Restart Account Service.

Issue a teller token (from the **lab3** folder, not inside the Maven module):

```powershell
cd labs\day-03\lab3
python tools\issue-jwt.py teller
```

Copy the printed token into a variable:

```powershell
$TELLER = "<paste token>"
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8081/api/v1/accounts
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Authorization: Bearer $TELLER" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

**Expected result:** first call **401** `UNAUTHENTICATED`. Second call **201**. Health still **200** without a token.

**Why this matters:** A resource server does not trust the network. It trusts a **signed** token.

---

### Step 3 — Protect Transaction Service the same way

Open Transaction `config/SecurityConfig.java`. Use the **same secret and issuer**. Map:

- GET `/api/v1/transactions/**` → `SCOPE_transactions.read`
- POST `/api/v1/transactions` → `SCOPE_transactions.write`

Restart Transaction Service.

```powershell
$OPS = python ..\..\..\tools\issue-jwt.py ops
# if that path is awkward, run from labs/day-03/lab3:
# $OPS = python tools\issue-jwt.py ops

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

**Expected result:** **401**. With `$TELLER` (no transaction scopes) POST should be **403**. With `$OPS` you are not done until Step 4 (Account GET also needs the relayed token).

---

### Step 4 — Relay the Bearer token to Account Service

If Transaction Service calls Account Service **without** `Authorization`, Account Service now returns 401 and the transaction fails even for a valid ops user.

`BearerTokenRelayInterceptor` is already in the project. In `RestClientConfig`, add:

```java
.requestInterceptor(new BearerTokenRelayInterceptor())
```

Restart Transaction Service.

Activate an account with `$TELLER`, put that `accountId` in `requests/create-valid.json`, then POST a transaction with `$OPS` and `X-Correlation-Id: lab3-demo`.

**Expected result:** **201** `RECEIVED`, then GET with `$OPS` becomes **SUBMITTED**. Account logs show the GET with the same correlation id. Neither log line contains `Bearer`.

**Why this matters:** This is **user-delegated** access. Transaction Service is not a superuser; it borrows the caller's token.

---

### Step 5 — Circuit breaker and safe fallback

On `AccountClient.requireActiveAccount` add:

```java
@CircuitBreaker(name = "accountService", fallbackMethod = "accountUnavailable")
```

Add fallbacks that **rethrow** `AccountNotEligibleException` (business 409 must not trip the breaker — already listed in `application.yml` `ignoreExceptions`) and that wrap everything else in `AccountServiceUnavailableException`.

Never do this:

```java
return new AccountView(accountId, "ACTIVE", "USD"); // FORBIDDEN in this lab
```

Restart. Stop Account Service (`Ctrl+C` in that terminal only). POST a transaction with `$OPS` **four times** (the breaker window is 4 calls / 50%).

**Expected result:** **503** `ACCOUNT_SERVICE_UNAVAILABLE`. No `TransactionSubmitted` publish. Logs say fallback or circuit open — still no token text.

Start Account Service again. Wait ~10s (open-state wait). A later POST should succeed once Account is healthy.

**Why this matters:** Fast failure is kinder than a 30-second hang. Inventing ACTIVE would move money against a frozen or missing account.

---

### Step 6 — Tests and log review

Copy these test classes from `solution/` if they are not in your tree:

- `account-service/.../AccountSecurityTest.java`
- `transaction-service/.../TransactionSecurityTest.java`
- `transaction-service/.../AccountCircuitBreakerTest.java`

From each service folder:

```powershell
mvn test
```

**Expected result:** tests pass (including 401, 403, and circuit OPEN → `CallNotPermittedException`).

Log checklist:

- [ ] `accountId`, `transactionId`, `correlationId` present
- [ ] No `Authorization` header
- [ ] No JWT payload (`scope`, `sub` dumps)
- [ ] 503 path does **not** log a fake ACTIVE status

GitHub Copilot (Free, already on this VM): ask it to **explain** the fallback methods. Reject any suggestion that returns a dummy ACTIVE `AccountView`.

---

## Success criteria

- [ ] No token → 401 on both business APIs
- [ ] Teller cannot POST transactions (403)
- [ ] Ops can POST a transaction for an ACTIVE account
- [ ] Token is forwarded; Account GET is authorized
- [ ] Account down → **503** `ACCOUNT_SERVICE_UNAVAILABLE`, no auto-approve
- [ ] Circuit opens after **4** failed calls; 409 FROZEN does not trip it
- [ ] `mvn test` passes on both services (includes the guided **integration** test `AccountPersistenceTest`)
- [ ] Logs stay synthetic and secret-free

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| 401 with a token | Secret mismatch; extra newline when pasting `$TELLER`; token expired (re-run `issue-jwt.py`) |
| 403 with ops token | Missing `transactions.write` or Account GET missing `accounts.read` |
| 401 from Account during POST transaction | Interceptor not registered |
| Status stuck RECEIVED | Kafka / consumer — Lab 2; this lab did not remove that path |
| Circuit never opens | Need enough **failed** calls (4). 409 FROZEN is ignored on purpose |
| Tests 401 unexpectedly | You imported `SecurityConfig` into `@WebMvcTest` without `jwt()` |

```powershell
Ctrl+C
docker compose down
```

---

## Optional stretch

- Draw user-delegated access vs a **client-credentials** service account (you did not build the second one).
- Keycloak would replace `issue-jwt.py` in an environment with an IdP.

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
