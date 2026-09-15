# Module 4 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module04_Microservices_Integration.pptx`  
**Story:** Order + Notification  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoints for this module: [Exercise 2.2](../day-02/exercises/exercise-2.2-event-flow.md) and [Exercise 2.3](../day-02/exercises/exercise-2.3-idempotency-dlq.md) (banking `TransactionSubmitted`).

---

## Exercise: When Notification Is Down

**Time:** 10 minutes

### Scenario

Notification Service is down for 10 minutes during a sale.

- **Version A:** after saving, Order calls `POST /notifications` over REST.  
- **Version B:** after saving, Order publishes `OrderCreated`; Notification consumes it.

### Solution

| | Version A (REST) | Version B (event) |
| --- | --- | --- |
| While Notification is down | The call blocks until timeout. Order must **fail the order** or **skip the email** (and likely lose it). | Order **saves and publishes**. The event **waits in the broker**. |
| Customer sees | An error, or a “success” with a missing email — both bad. | Order confirmation. Email is delayed, not lost. |
| When Notification returns | Unless you built an outbox/retry yourself, the skipped email does not come back. | Notification **catches up** on queued events. |

Version B still needs:

- **Idempotency** (replay / at-least-once delivery).
- **Lag monitoring** (how far behind is the consumer?).

### Why this is the answer

A broker turns a failing dependency into **delayed work** instead of a failed checkout. It is not magic: duplicates and lag are now your problem.

---

## Exercise: Command or Event?

**Time:** 10 minutes

### Scenario

Proposed names: `ChargeCard`, `CardCharged`, `SendWelcomeEmail`, `AccountOpened`, `ReserveStock`, `StockReserved`, `DataChanged`, `OrderShipped`.

### Solution

| Name | Kind | Notes |
| --- | --- | --- |
| `ChargeCard` | **Command** | Imperative; one handler (Payment). |
| `CardCharged` | **Event** | Past-tense fact. |
| `SendWelcomeEmail` | **Command** | Aimed at Notification. |
| `AccountOpened` | **Event** | Might go to Notification (welcome) and Analytics. |
| `ReserveStock` | **Command** | Aimed at Inventory. |
| `StockReserved` | **Event** | Fact others can react to. |
| `DataChanged` | **Vague — rename** | Says nothing. Use a specific fact, e.g. `CustomerAddressChanged`. |
| `OrderShipped` | **Event** | Might go to Notification and Loyalty. |

Commands are imperative requests to **one** handler. Events are **specific** past-tense facts for anyone who cares.

### Why this is the answer

`DataChanged` is the trap. Consumers cannot decide whether they care.
