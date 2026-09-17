# Lab 2 — Transaction Service

**Day:** 2 — Data and Event-Driven Communication  
**Capstone:** Service 2 of 3 in the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Build a Spring Boot **Transaction Service** with its own PostgreSQL database. Validate accounts through REST, publish and consume `TransactionSubmitted` on Kafka, carry a correlation ID, ignore duplicate events, and confirm failed messages land on a dead-letter topic.

---

## What you will finish with

By the end of this lab you will have:

- Transaction Service running on port **8082**
- Its **own** database (`transaction_db` on host port **5434**) — no shared tables with Account Service
- A REST call to Account Service **before** a transaction is accepted
- A `TransactionSubmitted` event on Kafka topic `transactions.submitted`
- An idempotent consumer that marks the row **SUBMITTED**
- Poison messages on `transactions.submitted.DLT`
- Passing unit tests
- Logs that use synthetic identifiers only

Lab 5 will add Risk Assessment as the long-term consumer of this event. Today you consume it in Transaction Service so you can practice produce, consume, duplicates, and DLQ without a third service.

---

## Knowledge you need (from Day 2)

Read this once before you type. Each idea shows up in a later step.

| Day 2 idea | How it appears in this lab |
| --- | --- |
| **Database per service** | Account data stays in `account_db`. Transaction rows live only in `transaction_db`. |
| **Flyway** | You version the transaction schema. Hibernate does **not** create tables. |
| **Synchronous REST** | Before insert, GET the account. Only **ACTIVE** accounts may transact. |
| **Asynchronous Kafka** | After insert, publish `TransactionSubmitted`. The consumer updates status later. |
| **Event envelope** | `eventType`, `eventVersion`, `eventId`, `correlationId`, `occurredAt`, `payload`. |
| **Correlation ID** | Header `X-Correlation-Id` is copied onto the event and into logs. |
| **Idempotent consumer** | Table `processed_events` keyed by `event_id`. A replay is a no-op. |
| **Dead-letter topic** | After 2 retries, unreadable/poison messages go to `transactions.submitted.DLT`. |
| **No distributed transaction** | You do **not** update Account Service's database. Eventual consistency is enough for this step. |
| **GitHub Copilot Free** | Already on this VM. Ask Copilot to *explain* the consumer. **Review before accept.** |

### Status flow

```text
  REST create
      │
      ▼
  RECEIVED  ──publish──►  Kafka  transactions.submitted
      │                         │
      │                         ▼
      └── consumer ────────── SUBMITTED
                              (duplicate eventId → ignore)
```

| HTTP / event | Result |
| --- | --- |
| Account missing or not ACTIVE | **409** `ACCOUNT_NOT_ELIGIBLE` — no row, no event |
| Valid ACTIVE account | **201**, status **RECEIVED**, event published |
| Consumer handles event | status becomes **SUBMITTED** |
| Same `eventId` again | consumer logs duplicate and stops |
| Garbage JSON on the topic | retries, then **DLT** |

### API contract

Base URL: `http://localhost:8082`

| Method | Path | Success |
| --- | --- | --- |
| POST | `/api/v1/transactions` | 201 Created |
| GET | `/api/v1/transactions/{transactionId}` | 200 OK |
| GET | `/actuator/health` | 200 OK |
| GET | `/swagger-ui/index.html` | 200 OK |

Synthetic identifiers only:

- Account: `ACC-` plus eight hex characters (from Lab 1)
- Transaction: generated for you as `TXN-` plus eight hex characters
- Amounts in **USD**

Never use Social Security numbers, PAN/card numbers, or real emails.

---

## Environment basics (read this first)

Do **all** of this **on the Ablaze VM**. Your laptop is only the browser. Do **not** run `oc login`. Copy **one block at a time**. Do not paste two commands on the same line. Do **not** paste this whole guide (or a chat) into the terminal.

**Repo root (from Lab 0):** `%USERPROFILE%\MD287`. Example: `C:\Users\student.VLAB\MD287`. Do **not** clone. Do **not** run `mklink`. If the prompt shows the long `.vscode\...-participant` path, that is the same repo — `cd` to `MD287` before git commands.

| Task | How |
| --- | --- |
| Folder | **File → Open Folder** → `%USERPROFILE%\MD287` |
| Terminal A | Lab 1 **Account Service** (`mvn spring-boot:run` on **8081**). Leave it running all of Lab 2. |
| Terminal B | Lab 2 **Transaction Service** (`mvn spring-boot:run` on **8082**). Leave it until the step says stop. |
| Stop an app | **Ctrl+C**. If Maven prints `Terminate batch job (Y/N)?`, type **`Y`** and Enter. Wait for the `*>` prompt. |
| Port 8081 or 8082 already in use | A leftover `mvn spring-boot:run` is still listening. Click that terminal, **Ctrl+C**, then **`Y`**. See Troubleshooting if you cannot find it. |
| Terminal C | **Terminal → New Terminal**. All `curl.exe` and Kafka `docker exec` commands. |
| HTTP calls | **`curl.exe`** (not `curl`) |
| GitHub Copilot | Signed in during Lab 0. Review before accept. |

**You need Lab 1 finished.** Account Service must be **UP** on **8081** with PostgreSQL `md287-account-db` **(healthy)** on **5433**. Work in **your** Lab 1 starter (`labs\day-01\lab1\starter\account-service`). Do **not** copy from a `solution/` folder.

If Lab 0 or Lab 1 is not done, stop and finish it first.

---

## Steps from the training slides

Follow these steps in order. Finish one step before starting the next.

### Step 0 — Pull the latest repo

Get the latest Lab 2 guide and starter from GitHub. Do **not** clone. Do **not** run `mklink`.

```powershell
cd $env:USERPROFILE\MD287
git pull
```

**Expected:** `Already up to date.` or a Fast-forward. Prompt ends with `\MD287>`.

Then: **File → Open Folder** → `%USERPROFILE%\MD287` if it is not already open.

`git pull` only works **inside** the repo. Do not run it from `C:\Users\student.VLAB`. If clone/`mklink` says the folder already exists, ignore that and stay in `MD287`.

### Step 1 — Confirm Account Service, then start the Transaction starter

**Do this:**

1. **Terminal C.** Confirm Account Service is healthy:

```powershell
curl.exe -s http://localhost:8081/actuator/health
```

Need `"status":"UP"` and a PostgreSQL `db` component.

If that fails, start Lab 1 in **Terminal A** (leave it running):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
docker compose up -d
Start-Sleep -Seconds 10
docker compose ps
mvn spring-boot:run
```

Wait for `Started AccountServiceApplication`, then retry the health curl. Lab 1 accounts from yesterday (including **CLOSED** ones) can stay in the database. You will open a **new ACTIVE** account in Step 8.

2. **Terminal B.** Open the Lab 2 starter and start **this** service's PostgreSQL **and** Kafka:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
```

Confirm you see `pom.xml`, `docker-compose.yml`, and a `requests` folder.

```powershell
docker compose up -d
Start-Sleep -Seconds 25
docker compose ps
```

Wait until `STATUS` includes **`(healthy)`** for `md287-transaction-db` (port **5434**) and `md287-kafka`. `md287-kafka-init` should have **exited** (topics created). If Kafka still says `(health: starting)`, wait and run `docker compose ps` again.

3. Start Transaction Service in **Terminal B** (leave it running):

```powershell
mvn spring-boot:run
```

4. **Terminal C:**

```powershell
curl.exe -s http://localhost:8082/actuator/health
```

**Expected result:**

- Maven prints `Started TransactionServiceApplication`
- Health JSON includes `"status":"UP"`
- The service is listening on **8082**
- Flyway may warn `No migrations found. Are your locations set up correctly?` That warning is **expected** until you add the SQL file in Step 2.
- Create/get APIs are **not** finished yet. `POST /api/v1/transactions` returns **HTTP 500** with `"code":"INTERNAL_ERROR"` until Step 5. That is expected.

Leave both applications running if you can. For later steps that change Java or SQL, stop **only Transaction Service** (**Ctrl+C**, then **`Y`**) and start it again. Keep Account Service on **8081**.

**Why this matters:** Broker, topics, and retry/DLT wiring are **pre-provisioned**. You focus on produce, consume, correlation, and duplicates — not on installing Kafka. Compose for Lab 2 does **not** start Account's database; that stays on **5433**.

---

### Step 2 — Apply the Flyway migration

The starter has **no** schema yet. Flyway creates the tables from a versioned SQL file.

**Do this:**

1. Stop Transaction Service: **Ctrl+C**, then **`Y`** if asked. Leave Account Service running.

2. Create the migration (filename is `V1__create_transactions.sql` — two underscores):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
New-Item -ItemType Directory -Force -Path "src\main\resources\db\migration" | Out-Null
@'
CREATE TABLE transactions (
    transaction_id VARCHAR(36) PRIMARY KEY,
    account_id     VARCHAR(36) NOT NULL,
    amount         NUMERIC(18, 2) NOT NULL,
    currency       VARCHAR(3)  NOT NULL,
    type           VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(64) NOT NULL,
    event_id       VARCHAR(64) NOT NULL UNIQUE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_status ON transactions (status);

CREATE TABLE processed_events (
    event_id       VARCHAR(64) PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL
);
'@ | Set-Content -Encoding ascii "src\main\resources\db\migration\V1__create_transactions.sql"
Get-Content "src\main\resources\db\migration\V1__create_transactions.sql"
```

`processed_events` is the inbox for idempotency. Confirm both `CREATE TABLE` blocks printed.

3. Start the app again in **Terminal B** (leave it running):

```powershell
mvn spring-boot:run
```

**Expected result:** Logs include:

```text
Migrating schema "public" to version "1 - create transactions"
Successfully applied 1 migration to schema "public", now at version v1
Started TransactionServiceApplication
```

If you restart later, Flyway does **not** re-run V1.

**Why this matters:** `spring.jpa.hibernate.ddl-auto` must not create production tables. Flyway is the source of truth for schema. This migration runs against **`transaction_db` only**.

---

### Step 3 — Confirm the entity and repository

The starter already contains the JPA mapping. Read it, then ask Hibernate to check that it matches Flyway.

**Do this:**

1. Open `src\main\java\com\md287\transaction\domain\Transaction.java` and confirm:

   | Java field | Database column |
   | --- | --- |
   | `transactionId` | `transaction_id` (primary key) |
   | `accountId` | `account_id` (string only — **no** FK to Account's database) |
   | `amount` / `currency` / `type` | money movement fields |
   | `status` | `RECEIVED` then `SUBMITTED` |
   | `correlationId` / `eventId` | tracing and dedupe |
   | `createdAt` / `updatedAt` | timestamps |

2. Open `src\main\java\com\md287\transaction\domain\ProcessedEvent.java`. It maps `processed_events` (`event_id` primary key).

3. Open `TransactionRepository.java` and `ProcessedEventRepository.java`. Both extend `JpaRepository`. You get `save` and `findById` for free.

4. Stop Transaction Service: **Ctrl+C**, then **`Y`** if asked.

5. Switch Hibernate to validate:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
(Get-Content "src\main\resources\application.yml") -replace "ddl-auto: none", "ddl-auto: validate" | Set-Content -Encoding ascii "src\main\resources\application.yml"
Select-String -Path "src\main\resources\application.yml" -Pattern "ddl-auto"
```

Need `ddl-auto: validate`.

6. Start the app again in **Terminal B** (leave it running):

```powershell
mvn spring-boot:run
```

**Expected result:** `Started TransactionServiceApplication` with no schema-validation error. Hibernate does not print a loud “schema OK” line — a clean start is success.

**Why this matters:** Transaction Service never joins to `accounts`. It only stores an `account_id` it learned from REST.

---

### Step 4 — Validate the account with REST

This is the **synchronous** hop. Stop Transaction Service (**Ctrl+C**, then **`Y`** if asked) and replace `AccountClient.java`.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
@'
package com.md287.transaction.client;

import com.md287.transaction.api.exception.AccountNotEligibleException;
import com.md287.transaction.api.exception.AccountServiceUnavailableException;
import com.md287.transaction.config.CorrelationIdFilter;
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

    public AccountView requireActiveAccount(String accountId) {
        try {
            AccountView account = accountRestClient.get()
                    .uri("/api/v1/accounts/{accountId}", accountId)
                    .header(CorrelationIdFilter.HEADER, nullToEmpty(MDC.get(CorrelationIdFilter.MDC_KEY)))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new AccountNotEligibleException(accountId, "NOT_FOUND");
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
        } catch (AccountNotEligibleException ex) {
            throw ex;
        } catch (RestClientException ex) {
            if (ex.getCause() instanceof AccountNotEligibleException eligible) {
                throw eligible;
            }
            throw new AccountServiceUnavailableException("Account Service did not respond", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\client\AccountClient.java"
Select-String -Path "src\main\java\com\md287\transaction\client\AccountClient.java" -Pattern "requireActiveAccount|TODO"
```

Need the method body (no `TODO`). Base URL comes from `md287.account-service.base-url` (`http://localhost:8081`). Timeouts are already in `RestClientConfig`.

Start the app again in **Terminal B**:

```powershell
mvn spring-boot:run
```

**Expected result:** Compiles and prints `Started TransactionServiceApplication`. You prove the **409** path in Step 8. Do **not** POST a transaction yet.

**Why this matters:** This is **synchronous** integration. Account Service remains the owner of account status.

---

### Step 5 — Persist the transaction

Stop Transaction Service (**Ctrl+C**, then **`Y`** if asked) and replace `TransactionService.java`.

Keep this behavior:

- `create` — GET Account (must be ACTIVE), save **RECEIVED**, publish `TransactionSubmitted`
- `get` — load or 404
- only **USD**
- log `transactionId`, `accountId`, and `status` only (no request JSON)

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
@'
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
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\service\TransactionService.java"
Select-String -Path "src\main\java\com\md287\transaction\service\TransactionService.java" -Pattern "TODO"
```

Need **no** `TODO` hits.

Start the app again in **Terminal B**:

```powershell
mvn spring-boot:run
```

**Expected result:** Compiles and prints `Started TransactionServiceApplication`. Publish still throws until Step 6. Do **not** POST a transaction yet.

**Why this matters:** The local `@Transactional` covers **this** database only. Publishing Kafka is not a two-phase commit. If the broker is slow, Lab 3 will add resilience. The outbox pattern is taught conceptually; you do not build it today.

---

### Step 6 — Publish `TransactionSubmitted`

Stop Transaction Service (**Ctrl+C**, then **`Y`** if asked) and replace `TransactionSubmittedPublisher.java`.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
@'
package com.md287.transaction.messaging;

import com.md287.transaction.config.Md287Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class TransactionSubmittedPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionSubmittedPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public TransactionSubmittedPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            Md287Properties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().submittedTopic();
    }

    public void publish(TransactionSubmittedEvent event) {
        try {
            kafkaTemplate.send(topic, event.eventId(), event).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing TransactionSubmitted", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to publish TransactionSubmitted", ex);
        }
        log.info("Published {} eventId={} transactionId={}",
                event.eventType(), event.eventId(), event.payload().transactionId());
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\messaging\TransactionSubmittedPublisher.java"
Select-String -Path "src\main\java\com\md287\transaction\messaging\TransactionSubmittedPublisher.java" -Pattern "TODO"
```

Need **no** `TODO` hits. The topic name is already `transactions.submitted`. The event **key** is `eventId` so retries stay on one partition. `.get(...)` waits until the broker acknowledges the record so a down broker fails the REST call instead of leaving a silent RECEIVED row.

Start the app again in **Terminal B**:

```powershell
mvn spring-boot:run
```

**Do not POST a transaction yet.** The consumer still throws until Step 7, and those messages would go to the dead-letter topic.

---

### Step 7 — Consume, correlate, and ignore duplicates

Stop Transaction Service (**Ctrl+C**, then **`Y`** if asked) and replace `TransactionSubmittedConsumer.java`.

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
@'
package com.md287.transaction.messaging;

import com.md287.transaction.config.CorrelationIdFilter;
import com.md287.transaction.domain.ProcessedEvent;
import com.md287.transaction.domain.Transaction;
import com.md287.transaction.repository.ProcessedEventRepository;
import com.md287.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class TransactionSubmittedConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionSubmittedConsumer.class);

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionRepository transactionRepository;

    public TransactionSubmittedConsumer(
            ProcessedEventRepository processedEventRepository,
            TransactionRepository transactionRepository
    ) {
        this.processedEventRepository = processedEventRepository;
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "${md287.kafka.submitted-topic}")
    @Transactional
    public void consume(TransactionSubmittedEvent event) {
        if (event == null || event.eventId() == null || event.payload() == null
                || event.payload().transactionId() == null) {
            throw new IllegalArgumentException("Poison TransactionSubmitted event");
        }

        String previous = MDC.get(CorrelationIdFilter.MDC_KEY);
        MDC.put(CorrelationIdFilter.MDC_KEY, event.correlationId());
        try {
            if (processedEventRepository.existsById(event.eventId())) {
                log.info("Duplicate event ignored eventId={} transactionId={}",
                        event.eventId(), event.payload().transactionId());
                return;
            }

            Transaction transaction = transactionRepository.findById(event.payload().transactionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown transactionId " + event.payload().transactionId()));

            transaction.markSubmitted(OffsetDateTime.now());
            processedEventRepository.save(new ProcessedEvent(
                    event.eventId(),
                    transaction.getTransactionId(),
                    OffsetDateTime.now()
            ));
            log.info("Consumed {} eventId={} transactionId={} status={}",
                    event.eventType(), event.eventId(), transaction.getTransactionId(), transaction.getStatus());
        } finally {
            if (previous == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, previous);
            }
        }
    }
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\messaging\TransactionSubmittedConsumer.java"
Select-String -Path "src\main\java\com\md287\transaction\messaging\TransactionSubmittedConsumer.java" -Pattern "TODO"
```

Need **no** `TODO` hits. Kafka retries plus DLT are already in `KafkaConfig` (2 retries, 500 ms apart).

Start the app again in **Terminal B** (leave it running):

```powershell
mvn spring-boot:run
```

**Expected result:** `Started TransactionServiceApplication`. You prove **201** and **SUBMITTED** in Step 8.

**Why this matters:** At-least-once delivery means the same `eventId` can arrive twice. The inbox table makes the consumer **idempotent**.

---

### Step 8 — Exercise REST, then dead-letter routing

You need an **ACTIVE** account. Lab 1 accounts that you **closed** will **409**. Create a **new** one.

**Terminal C.** Stay in folders as written. Use **`curl.exe`**. Do **not** call `/api/v1/transactions/TXN-YOUR-ID` — that literal string is not a transaction and returns **404**.

1. Create and activate an account (Account Service on **8081**):

```powershell
cd "$env:USERPROFILE\MD287\labs\day-01\lab1\starter\account-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab2-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8081/api/v1/accounts
```

Need **HTTP:201** and `"status":"PENDING"`. Copy **your** `accountId` from the JSON.

```powershell
$accountId = "ACC-AABBCCDD"
```

Change the value to **your** create response, then activate:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/$accountId/activate
```

Need **HTTP:200** and `"status":"ACTIVE"`.

2. Point the Lab 2 request file at that account:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
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

Confirm the JSON shows **your** `ACC-...` id, not `ACC-AABBCCDD` unless that really is your id.

3. Create a transaction:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab2-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

Need **HTTP:201** and `"status":"RECEIVED"`. Copy **your** `transactionId` (`TXN-` plus eight hex).

```powershell
$txnId = "TXN-00000000"
```

Change the value to **your** create response. Wait two seconds, then GET:

```powershell
Start-Sleep -Seconds 2
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8082/api/v1/transactions/$txnId
```

Need **HTTP:200** and `"status":"SUBMITTED"`. If it is still `RECEIVED`, wait two more seconds and GET again (the consumer is asynchronous). Look at **Terminal B** for `correlationId=lab2-demo` on create, publish, and consume lines.

4. Frozen account must be rejected (no new row, no event):

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/$accountId/freeze

curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

Need **HTTP:409** and `"code":"ACCOUNT_NOT_ELIGIBLE"`. Activate again when you are done:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" -X POST http://localhost:8081/api/v1/accounts/$accountId/activate
```

5. **Dead-letter check** — send garbage to the real topic (this does **not** go through your REST API). Do **not** paste `{not-json` in PowerShell (`{` starts a script block). Use this payload:

```powershell
"not-json" | docker exec -i md287-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic transactions.submitted
```

Wait a few seconds, then:

```powershell
docker exec md287-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transactions.submitted.DLT --from-beginning --timeout-ms 8000
```

**Expected result:**

| Call | HTTP / Kafka |
| --- | --- |
| create on ACTIVE account | **201** `"status":"RECEIVED"` |
| GET after ~2s | **200** `"status":"SUBMITTED"` |
| create on FROZEN account | **409** `ACCOUNT_NOT_ELIGIBLE` |
| poison on `transactions.submitted` | payload appears on `transactions.submitted.DLT` |

App logs (Terminal B) show retries, then recovery. Your good transaction is unchanged. Duplicate `eventId` is covered by unit tests in Step 9 (`Duplicate event ignored`).

**Why this matters:** `/api/v1` is a URL-prefix version. GET may still show RECEIVED for a moment — that is eventual consistency, not a bug. DLT is for operators, not a silent drop.

---

### Step 9 — Validation, health, docs, tests, log review

Right now `CreateTransactionRequest` accepts any string. A non-synthetic account id must be rejected at the DTO.

**Do this:**

1. Stop Transaction Service: **Ctrl+C**, then **`Y`** if asked. Leave Account Service running.

2. Replace the DTO:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
@'
package com.md287.transaction.api.dto;

import com.md287.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank
        @Pattern(regexp = "^ACC-[0-9A-F]{8}$", message = "accountId must be a synthetic identifier such as ACC-AABBCCDD")
        String accountId,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code such as USD")
        String currency,

        @NotNull
        TransactionType type
) {
}
'@ | Set-Content -Encoding ascii "src\main\java\com\md287\transaction\api\dto\CreateTransactionRequest.java"
Select-String -Path "src\main\java\com\md287\transaction\api\dto\CreateTransactionRequest.java" -Pattern "TransactionType|Pattern"
```

Need the `TransactionType` import **and** the `@Pattern` on `accountId`. If either is missing, paste the block again (do not save a truncated record).

3. Start the app again in **Terminal B**:

```powershell
mvn spring-boot:run
```

4. **Terminal C** — invalid synthetic id, then health and OpenAPI:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-invalid-account.json" `
  http://localhost:8082/api/v1/transactions

curl.exe -s http://localhost:8082/actuator/health
curl.exe -s -o NUL -w "info:%{http_code}`n" http://localhost:8082/actuator/info
curl.exe -s -o NUL -w "openapi:%{http_code}`n" http://localhost:8082/v3/api-docs
curl.exe -s http://localhost:8082/actuator
```

Invalid `accountId` (`real-customer-99`) → **400** `VALIDATION_FAILED`. `info` and `openapi` both print `200`. Actuator `_links` should contain only **self**, **health**, **health-path**, and **info**.

Open a browser:

[http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

(`http://localhost:8082/swagger-ui.html` also works.) Expand `POST /api/v1/transactions` and confirm the request body shows `accountId`, `amount`, `currency`, and `type`.

5. Stop Transaction Service (**Ctrl+C**, then **`Y`**) so the test JVM is not fighting over port **8082**. Leave Account Service and Docker (Postgres + Kafka) running. Then:

```powershell
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
mvn test
```

Maven may print Mockito / Byte Buddy lines such as `Dynamic loading of agents will be disallowed`. Those are **warnings**, not failures. Look at the **Results** block.

**Expected:**

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

and `BUILD SUCCESS`.

(`TransactionServiceTest` has 3 tests, `TransactionSubmittedConsumerTest` has 3, `TransactionControllerTest` has 4.)

GitHub Copilot (Free, already on this VM): ask Copilot to *explain* `TransactionSubmittedConsumer`. Do **not** accept generated Kafka code until you can say what happens on a duplicate `eventId`. Review-before-accept is a course rule.

6. Start the app again if you want to re-check logs, then look at **Terminal B** (not the curl window).

**Checklist — logs must NOT contain:**

- [ ] Emails, PAN/card-like numbers, or government identifiers
- [ ] Full JSON request bodies
- [ ] Database passwords

**Checklist — logs MAY contain:**

- [ ] `transactionId` (`TXN-...`) and `accountId` (`ACC-...`)
- [ ] `eventId`, `status`, `correlationId`
- [ ] HTTP path (not the body)
- [ ] `Duplicate event ignored` (from tests / a replay)

If you added `log.info(request.toString())` anywhere, remove it.

**Why this matters:** Two layers is intentional: the DTO guards *shape*; `AccountClient` guards *banking policy* (ACTIVE only). Day 3 hardens resilience; Day 2 establishes the event habit.

---

## Success criteria

- [ ] Account Service (8081) and Transaction Service (8082) both healthy
- [ ] Flyway applied `V1__create_transactions` on `transaction_db` only
- [ ] `POST /api/v1/transactions` on an ACTIVE account returns **201** **RECEIVED**
- [ ] `GET` later shows **SUBMITTED**
- [ ] Non-ACTIVE account returns **409** `ACCOUNT_NOT_ELIGIBLE`
- [ ] Non-synthetic `accountId` returns **400**
- [ ] Correlation ID from the request appears in logs
- [ ] Duplicate `eventId` is ignored (unit test)
- [ ] Poison Kafka message reaches `transactions.submitted.DLT`
- [ ] `/actuator/health` is `UP`
- [ ] Swagger UI shows the API
- [ ] `mvn test` passes
- [ ] Logs have no emails, card numbers, or request bodies

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `Terminate batch job (Y/N)?` | Type **`Y`** and Enter. That is how Windows Maven finishes after Ctrl+C. |
| Account health down | Lab 1 `docker compose` + `mvn spring-boot:run` in `labs\day-01\lab1\starter\account-service`. |
| `git pull`: not a git repository | You are in the home folder. `cd $env:USERPROFILE\MD287` then `git pull`. |
| `destination path 'MD287' already exists` | Do not clone. You already have the repo. Run Step 0. |
| `TRANSACTION_NOT_FOUND` for `TXN-YOUR-ID` | That string is a placeholder. Use the `transactionId` from your **201** body in `$txnId`. |
| `ACCOUNT_NOT_ELIGIBLE` on create | The account is not **ACTIVE** (PENDING, FROZEN, CLOSED, or missing). Create + activate a **new** Lab 1 account and update `requests\create-valid.json`. |
| `ACCOUNT_SERVICE_UNAVAILABLE` | Account Service is not on **8081**, or `md287.account-service.base-url` is wrong. |
| Status stuck on RECEIVED | Consumer exception in **Terminal B**; wait 2s and GET again; confirm Kafka is healthy and topic is `transactions.submitted`. |
| DLT empty | Wait for 2 retries (~1s+); use `--from-beginning`; PowerShell `{` is a script block — send `"not-json"`, not `{not-json`. |
| Kafka container restarting | Wait 20s; `docker compose logs kafka` from the Lab 2 starter folder. |
| `docker compose ps` still says `(health: starting)` | Wait ~10 seconds and run `docker compose ps` again before `mvn spring-boot:run`. |
| `Port 8081 is already in use` / `Port 8082 is already in use` | Leftover `mvn spring-boot:run`. Click that Maven tab: **Ctrl+C**, then **`Y`**. If you cannot find it, run the free-port block below. |
| Maven `Nothing to compile` after a Java step | The file did not change on disk (often a truncated paste). Re-run the `Select-String` check for that step, then start the app again. |
| `CreateTransactionRequest` missing `TransactionType` | The here-string was cut off. Paste the **full** Step 9 DTO block. |
| PSReadLine crash / huge paste | You pasted a whole chat into the terminal. Copy **one** command block only. |
| `Could not find or load` / `No such file` for `@requests/...` | You are not in the folder that contains `requests\`. `cd` to the `account-service` or `transaction-service` folder named in that step. |
| `curl` output looks like PowerShell errors | Use `curl.exe`, not `curl`. |
| JSON `400 MALFORMED_JSON` | Do not paste JSON on the PowerShell command line. Use `--data-binary "@requests/create-valid.json"`. |
| Tests fail on validation | Step 9 annotations are missing. |
| `mvn test` prints Mockito / dynamic-agent warnings | Ignore them if you also see `Tests run: 10, Failures: 0` and `BUILD SUCCESS`. |
| Flyway says V1 already applied but the table is missing | You pointed at an old Docker volume. Run `docker compose down -v` then `docker compose up -d` from the **Lab 2** starter (**this deletes local Lab 2 data**, not Lab 1). |

Free port **8081** or **8082** if a leftover Maven process is still listening:

```powershell
foreach ($port in 8081, 8082) {
  $p = (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1).OwningProcess
  if ($p) { Stop-Process -Id $p -Force; "Stopped PID $p on $port" } else { "$port is free" }
}
```

---

## Clean shutdown

Stop **Transaction Service** only (**Ctrl+C**, then **`Y`** if asked). **Leave** Lab 1 Account Service, `md287-account-db` (**5433**), `md287-transaction-db` (**5434**), and Kafka running if you will continue practicing. Do **not** run `oc login`.

```powershell
# Optional — only if you are done for the day and the instructor says to stop Lab 2 Docker:
cd "$env:USERPROFILE\MD287\labs\day-02\lab2\starter\transaction-service"
docker compose down
```

That stops Lab 2 Postgres and Kafka. It does **not** stop Lab 1's `md287-account-db`.

---

## Optional stretch (only if you finished early)

- Sketch why a **transactional outbox** would help if Kafka is down after the SQL commit.
- Draw who produces/consumes `TransactionApproved` in Lab 5 (Risk Assessment — not you today).
- In Swagger UI, execute create without `curl.exe` (account must still be ACTIVE).

Do **not** add JWT, circuit breakers, Redis, or balance here. Those belong to later days.

---

## What you built in the capstone

```text
Day 1  Account Service
Day 2  + Transaction Service  ← you are here (REST + TransactionSubmitted)
Day 3  + JWT, Resilience4j, tests
Day 4  + containers, OpenShift, CI/CD
Day 5  + Risk Assessment Service and OpenShift AI
```

---

© 2026 Innovation In Software Corporation
