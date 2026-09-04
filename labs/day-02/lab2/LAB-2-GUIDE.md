# Lab 2 — Transaction Service

**Day:** 2 — Data and Event-Driven Communication  
**Capstone:** Service 2 of 3 in the AI-Assisted Banking Transaction Risk Platform  
**Time:** 90–120 minutes  
**Difficulty:** Beginner

**Objective:** Build a Spring Boot **Transaction Service** with its own PostgreSQL database. Validate accounts through REST, publish and consume `TransactionSubmitted` on Kafka, carry a correlation ID, ignore duplicate events, and confirm failed messages land on a dead-letter topic.

---

## What you will finish with

- Transaction Service on port **8082**
- Its **own** database (`transaction_db` on host port **5434**) — no shared tables with Account Service
- REST call to Account Service before money movement is accepted
- A `TransactionSubmitted` event on Kafka topic `transactions.submitted`
- An idempotent consumer that marks the row **SUBMITTED**
- Poison messages on `transactions.submitted.DLT`
- Passing unit tests and logs that use synthetic IDs only

Lab 5 will add Risk Assessment as the long-term consumer of this event. Today you consume it in Transaction Service so you can practice produce, consume, duplicates, and DLQ without a third service.

---

## Knowledge you need (from Day 2)

| Day 2 idea | How it appears in this lab |
| --- | --- |
| **Database per service** | Account data stays in `account_db`. Transaction rows live only in `transaction_db`. |
| **Flyway** | You version the transaction schema. Hibernate does not create tables. |
| **Synchronous REST** | Before insert, GET the account. Only **ACTIVE** accounts may transact. |
| **Asynchronous Kafka** | After insert, publish `TransactionSubmitted`. The consumer updates status later. |
| **Event envelope** | `eventType`, `eventVersion`, `eventId`, `correlationId`, `occurredAt`, `payload`. |
| **Correlation ID** | Header `X-Correlation-Id` is copied onto the event and into logs. |
| **Idempotent consumer** | Table `processed_events` keyed by `event_id`. A replay is a no-op. |
| **Dead-letter topic** | After 2 retries, unreadable/poison messages go to `transactions.submitted.DLT`. |
| **No distributed transaction** | You do **not** update Account Service's database. Eventual consistency is enough for this step. |

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

Synthetic IDs only: `ACC-` + 8 hex, `TXN-` + 8 hex, amounts in USD.

---

## Environment basics (read this first)

**Demonstration environment:** Windows 10/11 · PowerShell in VS Code (press Ctrl+` to open the terminal)

| Task | How |
| --- | --- |
| Open the starter | File → Open Folder → `labs/day-02/lab2/starter/transaction-service` |
| Terminal | Ctrl+` → PowerShell |
| HTTP calls | Use **`curl.exe`** (not `curl` — PowerShell aliases `curl` to something else) |

**You need:**

- Completed **Lab 1 Account Service** running on **8081** (your finished lab in `labs/day-01/lab1/starter/account-service`)
- Java 21
- Maven 3.9+
- Docker Desktop running (PostgreSQL **and** Kafka)

Two terminals for Account Service (compose + app) stay up. Lab 2 uses two more (compose + app).

---

## Steps from the training slides

Follow in order.

### Step 1 — Run and validate the starter

**Do this:**

1. Confirm Account Service is healthy:

```powershell
curl.exe -s http://localhost:8081/actuator/health
```

You need `"status":"UP"`. If not, start Lab 1 postgres and `mvn spring-boot:run` in the Account Service folder.

2. Open the Lab 2 starter:

`labs/day-02/lab2/starter/transaction-service`

3. Start **this service's** PostgreSQL **and** Kafka (topics are created for you):

```powershell
docker compose up -d
docker compose ps
```

Wait until `md287-transaction-db` is healthy and `md287-kafka-init` has exited.

4. Start the app:

```powershell
mvn spring-boot:run
```

5. In another PowerShell window:

```powershell
curl.exe -s http://localhost:8082/actuator/health
```

**Expected result:** Health is `UP`. Create is **not** finished yet. `POST /api/v1/transactions` should fail until Step 5.

**Why this matters:** Broker, topics, and retry/DLT wiring are **pre-provisioned**. You focus on produce, consume, correlation, and duplicates — not on installing Kafka.

---

### Step 2 — Apply the Flyway migration

The starter has no schema yet.

**Do this:**

1. Stop the app (`Ctrl+C`).

2. Create `src/main/resources/db/migration/V1__create_transactions.sql`:

```sql
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
```

Two underscores in `V1__`. `processed_events` is the inbox for idempotency.

3. Start the app again (`mvn spring-boot:run`).

**Expected result:** Logs similar to `Migrating schema "public" to version "1 - create transactions"`.

---

### Step 3 — Confirm entity, repository, and schema match

**Do this:**

1. Open `domain/Transaction.java` and `domain/ProcessedEvent.java`. Confirm fields match the SQL.

2. Repositories already extend `JpaRepository`. You get `save` and `findById` for free.

3. In `application.yml` set:

```yaml
      ddl-auto: validate
```

(replace `none`). Restart.

**Expected result:** Startup succeeds. Mismatch fails loudly.

**Why this matters:** Transaction Service never joins to `accounts`. It only stores an `account_id` it learned from REST.

---

### Step 4 — Validate the account with REST

Open `client/AccountClient.java`. Replace `requireActiveAccount` (and add the helper) with:

```java
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
```

Add `import org.springframework.web.client.RestClientException;` if the IDE asks. You can remove the unused `ResourceAccessException` import.

Base URL comes from `md287.account-service.base-url` (`http://localhost:8081`). Timeouts are already in `RestClientConfig`.

**Expected result:** The class compiles. You will prove the 409 path in Step 8.

**Why this matters:** This is **synchronous** integration. Account Service remains the owner of account status.

---

### Step 5 — Persist the transaction

Open `service/TransactionService.java`. Replace `create` and `get`:

```java
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
```

Keep the helpers that are already in the class.

Restart after Step 6 as well — publish still throws until then.

**Why this matters:** The local `@Transactional` covers **this** database only. Publishing Kafka is not a two-phase commit. If the broker is slow, Lab 3 will add resilience. The outbox pattern is taught conceptually; you do not build it today.

---

### Step 6 — Publish `TransactionSubmitted`

Open `messaging/TransactionSubmittedPublisher.java`. Replace `publish`:

```java
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
```

Add `import java.util.concurrent.ExecutionException;`, `TimeoutException`, and `TimeUnit`.

The topic name is already `transactions.submitted`. The event **key** is `eventId` so retries stay on one partition. `.get(...)` waits until the broker acknowledges the record so a down broker fails the REST call instead of leaving a silent RECEIVED row.

Restart the app.

**Do not POST a transaction yet.** The consumer still throws until Step 7, and those messages would go to the dead-letter topic.

---

### Step 7 — Consume, correlate, and ignore duplicates

Open `messaging/TransactionSubmittedConsumer.java`. Replace `consume`:

```java
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
```

Kafka retries plus DLT are already in `KafkaConfig` (2 retries, 500 ms apart).

Restart.

**Why this matters:** At-least-once delivery means the same `eventId` can arrive twice. The inbox table makes the consumer **idempotent**.

---

### Step 8 — Exercise REST, then dead-letter routing

Create an **ACTIVE** account in Account Service if you do not have one (Lab 1: POST create, then POST activate). Copy the `accountId` (`ACC-` + 8 hex).

Edit `requests/create-valid.json` and put that `accountId` in.

From the Transaction Service folder:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  -H "X-Correlation-Id: lab2-demo" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

Copy `transactionId`. Wait two seconds, then:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" http://localhost:8082/api/v1/transactions/TXN-YOUR-ID
```

**Expected:** first call **201** `"status":"RECEIVED"`; GET soon after **200** `"status":"SUBMITTED"`. Logs show the same `correlationId=lab2-demo`.

Pending/frozen/closed account:

```powershell
# freeze the account in Account Service, then:
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-valid.json" `
  http://localhost:8082/api/v1/transactions
```

**Expected:** **409** `ACCOUNT_NOT_ELIGIBLE`. Activate the account again when you are done.

Invalid synthetic id:

```powershell
curl.exe -s -w "`nHTTP:%{http_code}`n" `
  -H "Content-Type: application/json" `
  --data-binary "@requests/create-invalid-account.json" `
  http://localhost:8082/api/v1/transactions
```

**Expected:** **400** after you add validation in Step 9. If you have not added annotations yet, do Step 9 first.

**Dead-letter check** — send garbage to the real topic (this does not go through your REST API):

```powershell
'{not-json' | docker exec -i md287-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic transactions.submitted
```

Wait a few seconds, then:

```powershell
docker exec md287-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transactions.submitted.DLT --from-beginning --timeout-ms 8000
```

**Expected:** the poison payload appears on the DLT (raw text `{not-json`, or a wrapped/encoded form). App logs show retries, then recovery. Your good transactions are unchanged.

---

### Step 9 — Validation, health, docs, tests, log review

Replace `CreateTransactionRequest` with the validated record:

```java
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
```

Add the jakarta.validation imports. Restart.

```powershell
curl.exe -s http://localhost:8082/actuator/health
curl.exe -s -o NUL -w "openapi:%{http_code}`n" http://localhost:8082/v3/api-docs
```

Open [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html).

Stop the app, then:

```powershell
mvn test
```

**Expected:** tests pass (service, consumer, controller).

Optional Copilot: ask it to **explain** `TransactionSubmittedConsumer`. Do not accept generated Kafka code until you can say what happens on a duplicate `eventId`.

Log checklist:

- [ ] Logs contain `transactionId`, `eventId`, `status`, `correlationId`
- [ ] Logs do **not** contain PAN/card numbers, emails, or full request bodies
- [ ] Duplicate line says `Duplicate event ignored`

---

## Success criteria

- [ ] Account Service (8081) and Transaction Service (8082) both healthy
- [ ] Flyway V1 applied on `transaction_db` only
- [ ] POST create on an ACTIVE account returns 201 RECEIVED
- [ ] GET later shows SUBMITTED
- [ ] Non-ACTIVE account returns 409
- [ ] Correlation ID from the request appears in logs and on the event
- [ ] Duplicate `eventId` is ignored
- [ ] Poison Kafka message reaches `transactions.submitted.DLT`
- [ ] `mvn test` passes
- [ ] Logs stay synthetic and small

---

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Account health down | Lab 1 docker compose + `mvn spring-boot:run` on 8081 |
| Kafka container restarting | Wait 20s; `docker compose logs kafka` |
| `ACCOUNT_SERVICE_UNAVAILABLE` | Wrong base URL or Account Service not running |
| Status stuck on RECEIVED | Consumer exception in logs; topic name `transactions.submitted` |
| DLT empty | Wait for 2 retries (~1s+); use `--from-beginning` |
| Port 8082 in use | Stop another process, or change `server.port` |

```powershell
Ctrl+C
docker compose down
```

Leave Account Service running if you will continue practicing.

---

## Optional stretch

- Sketch why a **transactional outbox** would help if Kafka is down after the SQL commit.
- Draw who produces/consumes `TransactionApproved` in Lab 5 (Risk Assessment — not you today).

Do **not** add JWT, circuit breakers, or Redis here. Those are later days.

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
