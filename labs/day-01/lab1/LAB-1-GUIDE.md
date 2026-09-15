# Lab 1 — Account Service

**Day:** 1 — Architecture, API Design, and Spring Boot  
**Capstone:** Service 1 of 3 in the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Build a working Spring Boot **Account Service** that stores accounts in PostgreSQL, exposes a versioned REST API, documents itself with OpenAPI, reports health through Actuator, and never physically deletes a closed account.

---

## What you will finish with

By the end of this lab you will have:

- A Spring Boot 3 service running on port **8081**
- A Flyway-managed `accounts` table in PostgreSQL
- Create, retrieve, and update operations
- Approved status changes: **activate**, **freeze**, **close** (no HTTP DELETE)
- Request validation and consistent error JSON
- Actuator health and Swagger UI
- Passing unit tests
- Logs that use synthetic identifiers only

Tomorrow (Lab 2) Transaction Service will call this API. Keep the contract stable.

---

## Knowledge you need (from Day 1)

Read this once before you type. Each idea shows up in a later step.

| Day 1 idea | How it appears in this lab |
| --- | --- |
| **Bounded context** | This service owns *accounts*. It does not post transactions or score risk. |
| **Database per service** | Only Account Service talks to `account_db`. Other services will get their own databases. |
| **REST resource** | The resource is an account. URLs name the resource, not an action like `/doActivate`. |
| **HTTP semantics** | `POST` create, `GET` read, `PATCH` partial update, `POST …/activate` for a business action. |
| **Idempotency** | Activating an already-ACTIVE account returns 200. Closing an already-CLOSED account returns 200. |
| **Twelve-Factor config** | Database URL, user, and password live in `application-local.yml`, not in Java code. |
| **Layered Spring Boot** | Controller → Service → Repository → PostgreSQL. |
| **Flyway** | SQL files version the schema. Hibernate does **not** create tables. |
| **Validation + errors** | Bad input → 400. Missing account → 404. Illegal status change → 409. |
| **Actuator + OpenAPI** | Health for operators. Swagger UI for humans and future consumers. |
| **Log hygiene** | Log `accountId` and `status`. Do not log request bodies, emails, or card-like numbers. |
| **GitHub Copilot Free** | Already signed in on this VM (Lab 0). Ask Copilot to *explain* tests and drafts. **Review before accept.** |

### Account status machine

New accounts start as **PENDING** (not yet usable for money movement).

```text
                  activate                 freeze
   PENDING ─────────────────► ACTIVE ──────────────► FROZEN
                                 │                      │
                                 │ close                │ activate
                                 ▼                      │
                               CLOSED ◄─────────────────┘
                                 ▲                      │
                                 └──────── close ───────┘
```

| Action | Allowed from | Result |
| --- | --- | --- |
| activate | PENDING or FROZEN | ACTIVE |
| activate | ACTIVE | ACTIVE (no error — safe to retry) |
| freeze | ACTIVE | FROZEN |
| freeze | FROZEN | FROZEN (no error — safe to retry) |
| freeze | PENDING or CLOSED | **409 Conflict** |
| close | any status except already handled | CLOSED, row **kept** |
| close | CLOSED | CLOSED (no error — safe to retry) |
| update nickname | PENDING, ACTIVE, FROZEN | updated |
| update nickname | CLOSED | **409 Conflict** |

There is **no DELETE**. Banking audit needs the closed record.

### API contract

Base URL: `http://localhost:8081`

| Method | Path | Success |
| --- | --- | --- |
| POST | `/api/v1/accounts` | 201 Created |
| GET | `/api/v1/accounts/{accountId}` | 200 OK |
| PATCH | `/api/v1/accounts/{accountId}` | 200 OK |
| POST | `/api/v1/accounts/{accountId}/activate` | 200 OK |
| POST | `/api/v1/accounts/{accountId}/freeze` | 200 OK |
| POST | `/api/v1/accounts/{accountId}/close` | 200 OK |
| GET | `/actuator/health` | 200 OK |
| GET | `/swagger-ui/index.html` | 200 OK |

Synthetic identifiers only:

- Customer: `CUST-0001`, `CUST-0002`, … (`CUST-` plus four digits)
- Account: generated for you as `ACC-` plus eight hex characters

Never use Social Security numbers, PAN/card numbers, or real emails.

---

## Environment basics (read this first)

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (press Ctrl+` to open the terminal)

**Repo root (from Lab 0):** `%USERPROFILE%\MD287` (short folder name). Every `labs/day-01/...` path below is relative to that folder. If VS Code still shows the long `...-participant` folder, rename it as in Lab 0 Step 4, then **File → Open Folder** → `%USERPROFILE%\MD287`.

| Task | How |
| --- | --- |
| Open the starter | File → Open Folder → `labs/day-01/lab1/starter/account-service` (under `%USERPROFILE%\MD287`) |
| Terminal | Ctrl+` → PowerShell |
| Working directory | Every `docker compose`, `mvn`, and `curl.exe` command in this lab is run from **`account-service`** (the folder that contains `pom.xml`, `docker-compose.yml`, and `requests/`) |
| HTTP calls | Use **`curl.exe`** (not `curl` — PowerShell aliases `curl` to something else) |
| Second terminal | In VS Code: Terminal → New Terminal. Keep the app running in the first window. |
| GitHub Copilot | **Copilot Free** is already on this VM. Sign in was Lab 0. Use it to explain and draft; **review before accept**. |

**Already completed [Lab 0](../../day-00/lab0/LAB-0-GUIDE.md) on the Ablaze VM?** Skip the clone and version checks. Stay in `%USERPROFILE%\MD287`. Confirm Docker Desktop is still running **on that VM**, `cd` to `labs\day-01\lab1\starter\account-service`, and start at Step 1. If `md287-account-db` is already **(healthy)** from Lab 0, `docker compose up -d` is a no-op. Do not continue Lab 1 on a laptop.

**You need:**

- Lab 0 **PASS** (or the same checks: clone under `labs\`, Java 21, Maven on 21, Docker engine up)
- Working directory: `account-service` (the folder with `pom.xml`)
- HTTP calls: **`curl.exe`** (not `curl` — PowerShell aliases `curl`)

If you skipped Lab 0 and do not already have the `labs` folder, clone **once** into the short folder `MD287`:

```powershell
cd $env:USERPROFILE
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

If VS Code already cloned the long repo name under `.vscode\`, rename it to `%USERPROFILE%\MD287` (Lab 0 Step 4). Do not keep two copies.

Start Docker Desktop from the Start menu and wait until it finishes starting (the whale icon in the system tray is idle, not animating). Then check once:

```powershell
cd "labs\day-01\lab1\starter\account-service"
java -version
mvn -version
docker info
```

`docker info` must print a **Server Version**. If you see `error during connect` / `docker_engine`, Docker Desktop is not ready yet — wait and retry.

The first `mvn spring-boot:run` on a machine may take several minutes while Maven downloads libraries. Lab 0 warms that cache; later runs are faster.

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 1 — Run and validate the starter

**Do this:**

1. Open the starter folder and make it your working directory:

```powershell
cd "labs\day-01\lab1\starter\account-service"
```

Confirm you see `pom.xml`, `docker-compose.yml`, and a `requests` folder.

2. Start PostgreSQL. From that folder:

```powershell
docker compose up -d
docker compose ps
```

Wait until `STATUS` includes **`(healthy)`**. Right after `up -d` it often says `(health: starting)` — wait about 10 seconds and run `docker compose ps` again.

3. Start the application (leave this terminal running):

```powershell
mvn spring-boot:run
```

4. In a **second** PowerShell window, check health:

```powershell
cd "labs\day-01\lab1\starter\account-service"
curl.exe -s http://localhost:8081/actuator/health
```

Optional — prove create is not implemented yet:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

**Expected result:**

- Maven prints `Started AccountServiceApplication`
- Health JSON includes `"status":"UP"` and `"db":{"status":"UP"...}` with `"database":"PostgreSQL"`
- The service is listening on **8081**
- Flyway may warn `No migrations found. Are your locations set up correctly?` That warning is **expected** until you add the SQL file in Step 2.
- Create/get APIs are **not** finished yet. The optional POST above returns **HTTP 500** with `"code":"INTERNAL_ERROR"` until Step 4. That is expected.

Leave the application running if you can. For later steps that change Java or SQL, stop it with `Ctrl+C` and start it again.

**Why this matters:** Spring Boot packages an embedded Tomcat, auto-configures a DataSource from the classpath, and exposes Actuator. You did not write a `server.xml` or a JDBC bootstrap class. Profiles (`local`) choose which YAML file supplies the JDBC URL.

---

### Step 2 — Apply the Flyway migration

The starter has **no** schema yet. Flyway will create the table from a versioned SQL file.

**Do this:**

1. Stop the app (`Ctrl+C`) if it is running.

2. Create this file:

   `src/main/resources/db/migration/V1__create_accounts.sql`

```sql
CREATE TABLE accounts (
    account_id   VARCHAR(36)  PRIMARY KEY,
    customer_id  VARCHAR(32)  NOT NULL,
    account_type VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    nickname     VARCHAR(80),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    closed_at    TIMESTAMPTZ
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_accounts_status ON accounts (status);
```

The file name is part of the contract:

- `V1` = version 1 (next change would be `V2__...`)
- `__` = two underscores
- `create_accounts` = a short description

3. Start the app again:

```powershell
mvn spring-boot:run
```

**Expected result:** Startup logs include lines similar to:

```text
Migrating schema "public" to version "1 - create accounts"
Successfully applied 1 migration to schema "public", now at version v1
```

(Your Flyway version may add an execution-time suffix. The important words are **applied 1 migration** and **version v1**.)

If you restart again, Flyway does **not** re-run V1. It records the version in `flyway_schema_history`.

**Why this matters:** `spring.jpa.hibernate.ddl-auto` must not create production tables. Flyway is the source of truth for schema. That is the same idea you will use in every later capstone service.

---

### Step 3 — Confirm the entity and repository

The starter already contains the JPA mapping. Your job is to **read it** and then ask Hibernate to check that it matches Flyway.

**Do this:**

1. Open `src/main/java/com/md287/account/domain/Account.java`.

   Confirm these mappings:

   | Java field | Database column |
   | --- | --- |
   | `accountId` | `account_id` (primary key) |
   | `customerId` | `customer_id` |
   | `accountType` | `account_type` (`CHECKING` or `SAVINGS`) |
   | `status` | `status` |
   | `currency` | `currency` |
   | `nickname` | `nickname` (nullable) |
   | `createdAt` / `updatedAt` / `closedAt` | timestamps |

2. Open `src/main/java/com/md287/account/repository/AccountRepository.java`.

   It extends `JpaRepository<Account, String>`. That gives you `save`, `findById`, and `existsById` without writing SQL.

3. In `src/main/resources/application.yml`, change:

```yaml
    hibernate:
      ddl-auto: none
```

to:

```yaml
    hibernate:
      ddl-auto: validate
```

4. Stop the app with `Ctrl+C` in the Maven terminal (a YAML change does not apply until restart), then start it again:

```powershell
mvn spring-boot:run
```

**Expected result:** The application starts (`Started AccountServiceApplication`). Hibernate does not print a loud “schema OK” line — success is a clean start. If Flyway and the entity disagree, startup **fails** with a schema-validation error. That is a feature: you want mismatch to be loud.

**Why this matters:** The **repository** is persistence. The **entity** is the table. The **service** (next step) is where banking rules live. Controllers should stay thin.

---

### Step 4 — Complete service-layer state transitions

This is the main coding step. Open:

`src/main/java/com/md287/account/service/AccountService.java`

Replace each `throw new UnsupportedOperationException(...)` method with the code below. Keep the constructor and the two private helpers (`requireAccount` and `nextAccountId`) — they are already written.

Helpers you already have:

- `requireAccount(id)` — loads the row or throws `AccountNotFoundException`
- `nextAccountId()` — returns `ACC-` plus eight hex characters

#### 4.1 `create` — new accounts are PENDING

```java
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
```

Notice what is **not** logged: customer email, nickname, request JSON.

#### 4.2 `get`

```java
    @Transactional(readOnly = true)
    public AccountResponse get(String accountId) {
        return AccountResponse.from(requireAccount(accountId));
    }
```

#### 4.3 `update` — nickname only, never on CLOSED

```java
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
```

You do not call `save` after a field change inside a `@Transactional` method. JPA flushes the dirty entity at commit time.

#### 4.4 `activate`

```java
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
```

#### 4.5 `freeze`

```java
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
```

#### 4.6 `close` — retain the row

```java
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
```

Restart the app after saving.

**Expected result:** The project compiles. `POST /api/v1/accounts` now returns **201** (you will prove that in Step 5).

**Why this matters:** Constructor injection (`AccountService(AccountRepository ...)`) is the Spring default for testability. `@Transactional` sets the local transaction boundary — this service never starts a two-phase commit with another database.

---

### Step 5 — Exercise the REST endpoints

The controller is already wired in `AccountController`. It is thin on purpose: it translates HTTP to method calls.

**Do this:**

Stay in `account-service/` (the folder that contains `requests/`). Run these commands **in order**. Use **`curl.exe`**.

Create (`-i` prints response headers so you can see `Location` and `X-Correlation-Id`):

```powershell
curl.exe -s -i -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab1-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

Confirm the headers include `X-Correlation-Id: lab1-demo` and `Location: http://localhost:8081/api/v1/accounts/ACC-...`.

Copy the `accountId` from the JSON body (example: `ACC-1F4E6A52`). In every command below, replace **`ACC-YOUR-ID`** with your value. Do not leave the placeholder in the URL.

Retrieve:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8081/api/v1/accounts/ACC-YOUR-ID
```

Freeze while still PENDING (should be rejected):

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/ACC-YOUR-ID/freeze
```

Activate, update, freeze, unfreeze, close:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/ACC-YOUR-ID/activate

curl.exe -s -w "`nHTTP:%{http_code}`n" -X PATCH -H "Content-Type: application/json" `
  --data-binary "@requests/update-nickname.json" `
  http://localhost:8081/api/v1/accounts/ACC-YOUR-ID

curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/ACC-YOUR-ID/freeze

curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/ACC-YOUR-ID/activate

curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/ACC-YOUR-ID/close
```

Closed records stay in the database:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8081/api/v1/accounts/ACC-YOUR-ID
```

Missing account:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8081/api/v1/accounts/ACC-MISSING
```

**Expected result:**

| Call | HTTP |
| --- | --- |
| create | **201** and `"status":"PENDING"` |
| get | **200** |
| freeze while PENDING | **409** `INVALID_ACCOUNT_STATE` |
| activate | **200** `"status":"ACTIVE"` |
| patch nickname | **200** `"nickname":"Business checking"` |
| freeze | **200** `"status":"FROZEN"` |
| activate from FROZEN | **200** `"status":"ACTIVE"` |
| close | **200** `"status":"CLOSED"` and a `closedAt` value |
| get after close | **200** (row still there) |
| get missing | **404** `ACCOUNT_NOT_FOUND` |

Error bodies look like:

```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "code": "ACCOUNT_NOT_FOUND",
  "message": "Account ACC-MISSING was not found",
  "path": "/api/v1/accounts/ACC-MISSING",
  "correlationId": "...",
  "details": []
}
```

`X-Correlation-Id` on create is echoed back. If you omit it, the service generates one. Downstream services in later labs will pass the same header.

**Why this matters:** `/api/v1` is a URL-prefix version. You can add `/api/v2` later without breaking Lab 2. Action URLs (`/activate`) are used because activate is a **state change**, not a field you PATCH blindly.

---

### Step 6 — Add validation

Right now `CreateAccountRequest` accepts any string. A real email must be rejected.

**Do this:**

Replace `CreateAccountRequest.java` with:

```java
package com.md287.account.api.dto;

import com.md287.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank
        @Pattern(regexp = "^CUST-[0-9]{4}$", message = "customerId must be a synthetic identifier such as CUST-0001")
        String customerId,

        @NotNull
        AccountType accountType,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code such as USD")
        String currency,

        @Size(max = 80)
        String nickname
) {
}
```

Replace `UpdateAccountRequest.java` with:

```java
package com.md287.account.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 80)
        String nickname
) {
}
```

The controller already has `@Valid`. That is what triggers Jakarta Validation before the service runs.

Stop the app (`Ctrl+C`) and start it again (`mvn spring-boot:run`) so the new DTO annotations load. Then:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-invalid-customer.json" `
  http://localhost:8081/api/v1/accounts

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-eur.json" `
  http://localhost:8081/api/v1/accounts
```

**Expected result:**

- Email-style `customerId` → **400** `VALIDATION_FAILED` (rejected at the DTO)
- `EUR` → **400** `UNSUPPORTED_CURRENCY` (rejected in the service after `@Pattern` allows any 3-letter code)

Two layers is intentional: the DTO guards *shape*; the service guards *banking policy*.

**Why this matters:** Standardized error JSON lives in `GlobalExceptionHandler`. Every later service should use the same `code` / `message` / `correlationId` shape so API consumers (and AI tools on Day 5) can parse failures.

---

### Step 7 — Verify health and API documentation

**Do this:**

```powershell
curl.exe -s http://localhost:8081/actuator/health
curl.exe -s -o NUL -w "info:%{http_code}`n" http://localhost:8081/actuator/info
curl.exe -s -o NUL -w "openapi:%{http_code}`n" http://localhost:8081/v3/api-docs
```

Open a browser:

[http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

(`http://localhost:8081/swagger-ui.html` also works — Springdoc serves both.)

In Swagger UI, expand `POST /api/v1/accounts` and confirm the request body shows `customerId`, `accountType`, `currency`, and `nickname`.

Confirm which Actuator endpoints are exposed:

```powershell
curl.exe -s http://localhost:8081/actuator
```

**Expected result:**

- Health is `UP` with a PostgreSQL `db` component
- `info` and `openapi` both print `200`
- Swagger UI lists the account operations (`/api/v1/accounts`, activate, freeze, close)
- `/actuator` JSON `_links` contains only **self**, **health**, **health-path**, and **info** (no `env`, `beans`, or `mappings`)

**Why this matters:** In production, management endpoints are limited and often placed on a separate port. You will wire liveness/readiness probes from this same Actuator surface on Day 4.

---

### Step 8 — Run unit tests

Tests are already in the starter. They fail until Steps 4 and 6 are done.

**Do this:**

Stop the running app (`Ctrl+C`) so the test JVM is not fighting over files. Then:

```powershell
mvn test
```

Maven may print Mockito / Byte Buddy lines such as `Dynamic loading of agents will be disallowed` or `Mockito is currently self-attaching`. Those are **warnings**, not failures. Look at the **Results** block.

GitHub Copilot (Free, already on this VM): ask Copilot to *explain* `AccountServiceTest`. Do **not** accept generated test code until you can say what each assertion proves. Review-before-accept is a course rule.

**Expected result:**

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

and `BUILD SUCCESS`.

(`AccountServiceTest` has 9 tests, `AccountControllerTest` has 4.)

**Why this matters:** Service tests use Mockito (no database). Controller tests use MockMvc (no Tomcat). That is the middle of the testing pyramid. Day 3 adds an integration test; Day 2 persistence uses the real PostgreSQL you just started.

---

### Step 9 — Review logs for sensitive data

**Do this:**

1. Start the app again (`mvn spring-boot:run`) and wait for `Started AccountServiceApplication`.
2. In the second terminal, from `account-service/`, send the invalid customer payload (it contains `john.doe@bank.com`) and then a successful create:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-invalid-customer.json" `
  http://localhost:8081/api/v1/accounts

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab1-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

3. Look at the **Maven** console (the window running `spring-boot:run`), not the curl window.

You should see lines like:

```text
correlationId=... - Validation failed path=/api/v1/accounts fieldCount=1
correlationId=lab1-demo - Created account accountId=ACC-... type=CHECKING status=PENDING
```

**Checklist — logs must NOT contain:**

- [ ] The email `john.doe@bank.com`
- [ ] Full JSON request bodies
- [ ] Card-like numbers or government identifiers
- [ ] Database passwords

**Checklist — logs MAY contain:**

- [ ] `accountId` (`ACC-...`)
- [ ] `status` and `accountType`
- [ ] `correlationId`
- [ ] HTTP path (not the body)

If you added `log.info(request.toString())` anywhere, remove it.

**Why this matters:** Day 3 hardens this with security and redaction. Day 1 establishes the habit: synthetic data in requests, minimal data in logs.

---

## Success criteria

- [ ] PostgreSQL is running from `docker compose`
- [ ] Flyway applied `V1__create_accounts`
- [ ] `POST /api/v1/accounts` creates a **PENDING** account and returns **201**
- [ ] `GET` returns the stored account
- [ ] `PATCH` updates nickname
- [ ] `activate` / `freeze` / `close` follow the status table
- [ ] Closed accounts still `GET` successfully (no physical delete)
- [ ] Illegal freeze from PENDING returns **409**
- [ ] Unknown account returns **404** with `ACCOUNT_NOT_FOUND`
- [ ] Non-synthetic `customerId` returns **400**
- [ ] `/actuator/health` is `UP`
- [ ] Swagger UI shows the API
- [ ] `mvn test` passes
- [ ] Logs have no emails, card numbers, or request bodies

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `docker info` / `docker compose` cannot connect | Start **Docker Desktop** and wait until the engine is ready, then retry. |
| `docker compose ps` still says `(health: starting)` | Wait ~10 seconds and run `docker compose ps` again before `mvn spring-boot:run`. |
| Port 5433 already in use | Another Postgres is bound to 5433. Stop it, or change the left-hand port in `docker-compose.yml` **and** in `application-local.yml`. |
| `Could not find or load` / `No such file` for `@requests/...` | You are not in `account-service/`. `cd` there so `requests\create-valid.json` exists. |
| App starts then dies with schema-validation | Flyway did not run, or `V1` does not match `Account.java`. Confirm the SQL file is under `src/main/resources/db/migration/` and is named `V1__create_accounts.sql` (two underscores). |
| `POST` returns 500 `INTERNAL_ERROR` after Step 4 | You did not restart after saving `AccountService.java`. Stop with `Ctrl+C` and run `mvn spring-boot:run` again. Before Step 4, 500 is expected. |
| `curl` output looks like PowerShell errors | Use `curl.exe`, not `curl`. |
| JSON `400 MALFORMED_JSON` | Do not paste JSON on the PowerShell command line. Use `--data-binary "@requests/create-valid.json"`. |
| Tests fail on validation | Step 6 annotations are missing. |
| `mvn test` prints Mockito / dynamic-agent warnings | Ignore them if you also see `Tests run: 13, Failures: 0` and `BUILD SUCCESS`. |
| Flyway says V1 already applied but the table is missing | You pointed at an old Docker volume. Run `docker compose down -v` then `docker compose up -d` (**this deletes local lab data**). |

---

## Clean shutdown

```powershell
# in the app terminal
Ctrl+C

docker compose down
```

Keep the database running if you will jump straight into extra practice.

---

## Optional stretch (only if you finished early)

- Add a second profile `application-qa.yml` with a different port and explain Twelve-Factor “config in the environment”.
- In Swagger UI, execute create + activate without `curl.exe`.
- Sketch how Lab 2’s Transaction Service will call `GET /api/v1/accounts/{id}` before accepting a payment.

Do **not** add balance, transfers, or Kafka here. Those belong to later days.

---

## What you built in the capstone

```text
Day 1  Account Service   ← you are here
Day 2  + Transaction Service (REST validate + Kafka events)
Day 3  + JWT, Resilience4j, tests
Day 4  + containers, OpenShift, CI/CD
Day 5  + Risk Assessment Service and OpenShift AI
```

---

© 2026 Innovation In Software Corporation
