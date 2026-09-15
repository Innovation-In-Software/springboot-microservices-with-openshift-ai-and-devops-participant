# Capstone — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Capstone_Putting_It_All_Together.pptx`  
Attempt the slide activity first, then compare your answers here.

The official **Capstone Demonstration** slide is a 30-minute demo, not an activity key. Numbered Day 5 checkpoints: [5.1](../day-05/exercises/exercise-5.1-model-policy-disposition.md), [5.2](../day-05/exercises/exercise-5.2-policy-rules.md), [5.3](../day-05/lab5/starter/mcp-controls.md).

This README covers the unnumbered activities. The first two use the **order** story; “Why Are Risk Assessments Missing?” uses the **banking** platform the labs actually deploy.

---

## Exercise: Draw the Service Boundaries

**Time:** 10 minutes

### Scenario

```
orders, order_items tables
payments, refunds tables
stock_levels, reservations tables
shipments table
Order Service reads stock_levels
Shipping updates orders.status
```

### Solution

| Tables | Owning service |
| --- | --- |
| `orders`, `order_items` | **Order** |
| `payments`, `refunds` | **Payment** |
| `stock_levels`, `reservations` | **Inventory** |
| `shipments` | **Shipping** |

**Two boundary violations**

1. Order Service **reads `stock_levels`** → data coupling. **Fix:** call Inventory's reserve/check **API** (or a reservation command).  
2. Shipping **updates `orders.status`** → writes another service's table. **Fix:** Shipping publishes **`ShipmentDispatched`** (or similar); **Order Service** updates its own status.

One owner per table. Other services ask through APIs or react to events.

---

## Exercise: Trace a Failed Payment

**Time:** 12 minutes

### Scenario

`ORD-1045` is created. Then:

1. Payment call times out after 2s  
2. Retry succeeds — or did it charge twice?  
3. Payment Service goes down for 5 minutes  
4. `OrderCreated` is delivered twice  
5. A poison message keeps failing  

### Solution

| Step | Order state | Control | Customer sees |
| --- | --- | --- | --- |
| 1 Timeout | `PAYMENT_PENDING` | Client timeout (2s) | Payment being confirmed — not paid |
| 2 Retry | Still one charge | **Same Idempotency-Key** | Charged once |
| 3 Payment down 5 min | Status **unknown / pending**; circuit **open** | Circuit breaker, fail fast | Not a false success |
| 4 Duplicate `OrderCreated` | One reservation | Processed-event store / idempotent consumer | Unchanged |
| 5 Poison message | Unchanged on the happy path | **DLQ + alert** | Ops notified; customer not lied to |

Every failure has a designed outcome. **None** of them is a made-up success.

---

## Exercise: Why Are Risk Assessments Missing?

**Time:** 10 minutes · **Story:** deployed **banking** platform (Lab 5)

### Scenario

On OpenShift, transactions submit successfully, but `GET /api/v1/assessments/{transactionId}` returns **404** for every new one.

### Solution — follow the event

Check **in this order**:

1. **Was `TransactionSubmitted` published?** Evidence: Transaction Service logs + **correlation / trace id** on the submit.  
2. **Broker reachable**; consumer group **`risk-assessment-service`** (or the lab's actual group). Evidence: consumer lag, broker connectivity.  
3. **Risk pod Ready?** Logs show handler errors? Evidence: `oc get pods`, `oc logs`.  
4. **ConfigMap topic name** and **Secret** values actually set (wrong topic looks exactly like “no assessments”).  
5. **401/403 on the lookup** can look like missing data if the client swallows the body — confirm the GET with a valid token.

Start at the source, then broker, then consumer, then config, then the read API.

---

## Exercise: Production Readiness Review

**Time:** 10 minutes

### Scenario

Team checklist:

```
✓ mvn verify passes
✓ Image pushed as :latest
✓ Route readiness returns 200
✓ JWT scopes on every endpoint
  DB password in the ConfigMap
  No alert on the HOLD backlog
  Rollback never tried
```

### Solution

| Item | Mark | Fix |
| --- | --- | --- |
| `mvn verify` passes | Ready (necessary, not sufficient) | Keep it |
| Image `:latest` | **Risky** | Versioned tag **and digest**; promote that digest |
| Route readiness 200 | Ready for “it starts” | Still need probes on the real paths |
| JWT scopes on every endpoint | Ready direction | Confirm object-level checks too; some endpoints may be public health only |
| DB password in ConfigMap | **Blocking** | Move to **Secret**, **rotate** the password (it leaked) |
| No alert on HOLD backlog / DLQ | **Risky / blocking for ops** | Alert on HOLD backlog and DLQ depth |
| Rollback never tried | **Risky** | Rehearse `oc rollout undo` in staging |

**Decision: no-go** until blockers are fixed (at least: Secret + rotation; versioned digest; a rollback rehearsal and HOLD/DLQ alerts before you call it production).

Ready means secure, traceable, observable, and recoverable — not just “it starts”.
