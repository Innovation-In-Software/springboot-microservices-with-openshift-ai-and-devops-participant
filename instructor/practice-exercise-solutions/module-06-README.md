# Module 6 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module06_Resilient_High_Performance_Microservices.pptx`  
**Story:** checkout / banking APIs as named on the slide  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint for this module: [Exercise 3.1](../../labs/day-03/exercises/exercise-3.1-timeout-circuit-breaker.md).

---

## Exercise: Retry or Not?

**Time:** 10 minutes

### Scenario

1. `GET` exchange rate → 503  
2. `POST` transfer → timeout, **no** idempotency key  
3. `POST` transfer → timeout, **with** key  
4. `PUT` profile → 400 validation  
5. `GET` balance → connection reset  
6. `POST` payment → 429, `Retry-After: 2`

### Solution

| # | Retry? | Detail |
| --- | --- | --- |
| 1 Exchange rate 503 | **Retry** | Safe read, transient. Backoff + jitter. A few attempts. |
| 2 Transfer timeout, no key | **Do not retry blindly** | Timeout does not say whether money moved. **Check status** (or use a key next time). |
| 3 Transfer timeout, with key | **Retry** | Same `Idempotency-Key` → server returns the original result. |
| 4 Profile 400 | **Fail fast** | Request must change. Retrying the same body loops forever. |
| 5 Balance connection reset | **Retry** | Safe read, transient. Backoff + jitter. |
| 6 Payment 429 | **Wait 2 s, retry once** (or as `Retry-After` says) | Honour the server. Do not hammer. |

### Why this is the answer

Retry transient failures on **safe or keyed** operations. Fail fast on validation. Never retry an unkeyed money write.

---

## Exercise: Diagnose a Slow Dependency

**Time:** 12 minutes

### Scenario

Since 10:05, checkout p95 is 4.8 s and 3% of requests fail with 5xx. Transaction Service calls Fraud Detection during payment. Fraud is slow and starting to time out.

### Solution

1. **First signal:** a **distributed trace** of a slow checkout. The fraud span will dominate latency. Logs with the same `traceId` show the timeouts. Metrics only show the symptom (p95, 5xx).
2. **Settings:** timeout **inside** the checkout budget; **no blind retries** on the payment write; **circuit breaker** on the fraud call so callers fail fast when Fraud is down.
3. **Fallback:** `PENDING_REVIEW` / hold. **Never auto-approve** without a fraud check.
4. **Watch:** fraud p95, timeout count, breaker state, count of payments pending review; `traceId` on every log line.

### Why this is the answer

Contain the dependency without inventing a safe money outcome.

---

## Exercise: Define SLOs for Banking APIs

**Time:** 10 minutes

### Scenario

Account balance API · Transfer API · Fraud check API · Statement download · Rewards lookup.

### Solution (direction — numbers can vary if justified)

| API | SLI (example) | SLO direction | Strictness |
| --- | --- | --- | --- |
| Balance | Successful responses / all requests; p95 latency | **99.9%**, p95 **< 300 ms** | High — customer-facing, used constantly |
| Transfer | Success + **no duplicate transfers** | **99.95%** availability **and** correctness (idempotency) | **Highest** — money movement |
| Fraud check | p95 of the check | p95 **< 700 ms** (so checkout still fits a budget) | High for latency; fail **closed** if unavailable |
| Statement download | p95 time to produce | p95 **< 3 s** | Relaxed |
| Rewards lookup | Availability | **99.5%** | Least critical |

Ranking: Transfer > Balance ≈ Fraud latency > Statements > Rewards.

Ask what each target **costs**, and what the **error budget** allows each month.

---

## Exercise: Plan a Failure Experiment

**Time:** 10 minutes

### Scenario

The team claims Order Service survives a slow Payment Service. Test it in staging this afternoon.

### Solution

| Piece | Answer |
| --- | --- |
| Hypothesis | Checkout p95 stays under **2 s**; orders go **`PENDING`** (not false success, not a cascade). |
| Fault | Add **~3 s latency to 10%** of Payment calls (staging only, limited time). |
| Measure | Checkout p95, error rate, breaker state, count of pending orders. |
| Stop rule | **Abort if error rate > 2%** (or another number agreed **before** you start). |
| After | Record results, fix what broke, add an automated test, **rerun**. |

### Why this is the answer

Chaos is a **controlled experiment**: hypothesis, small blast radius, pre-agreed stop rule. Nobody improvises during the “incident”.
