# Module 5 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module05_Microservices_and_Data.pptx`  
**Story:** Order Service persistence and sagas  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint for this module: [Exercise 2.1](../../labs/day-02/exercises/exercise-2.1-data-ownership.md) (banking Account vs Transaction).

---

## Exercise: Spot the Persistence Problems

**Time:** 12 minutes

### Scenario

Pull request contains:

```yaml
spring.jpa.hibernate.ddl-auto: update
spring.datasource.password: Prod#2026
```

```java
@ManyToOne
private Customer customer;

@GetMapping("/orders")
List<Order> all() {
  return orders.findAll();
}
```

### Solution

| Problem | Why it hurts in production | Fix |
| --- | --- | --- |
| `ddl-auto: update` | Hibernate mutates the live schema; no review, no rollback | Flyway (or Liquibase) migrations + `ddl-auto: validate` |
| Password in YAML | Secret in Git for anyone with the repo | Environment / OpenShift Secret |
| `@ManyToOne Customer` | Cross-service join; Order does not own Customer | Store `customerId` only; load customer via API if needed |
| Returns `Order` entities | Leaks internals, lazy-load surprises, over-exposure | Return DTOs |
| `findAll()` with no paging | Memory and latency blow up as the table grows | Accept `Pageable`; return a page |

**Bonus:** if DTO mapping touches each order's items, the list can cause **N+1** queries — fetch-join or a dedicated query.

### Why this is the answer

Schema by migration, secrets from the environment, IDs across services, DTOs out, pages for lists.

---

## Exercise: Design the Compensation

**Time:** 12 minutes

### Scenario

Saga: Create Order → Authorize Payment → Reserve Inventory → Create Shipment.

Shipping rejects the address **after** payment and inventory both succeeded.

### Solution

Compensate in **reverse order** of completed steps:

1. **Release** the reserved inventory.  
2. **Refund or void** the authorized payment.  
3. Mark the order **`CANCELLED`**, *or* pause and ask the customer for a valid address (business choice — compensation is not the only option).

Every step **and** every compensation must be **idempotent** (the same message can arrive twice).

**Customer view:** `PENDING` while the saga runs, then either a clear failure with refund/void confirmation, or a request for a new address — never a silent “success”.

### Why this is the answer

Undo the most recent completed step first. Do not invent a shipped order.

---

## Exercise: What Should We Cache?

**Time:** 10 minutes

### Scenario

Product catalog page · order status for tracking · account balance · exchange rates · fraud decision · branch and ATM list · customer dashboard summary.

### Solution

| Data | Cache? | TTL / invalidation | Harm if stale |
| --- | --- | --- | --- |
| Product catalog page | **Yes** | Long TTL; evict on catalog publish | Wrong price is possible — still usually cached, with price checks at checkout |
| Branch / ATM list | **Yes** | Long TTL | Low |
| Exchange rates | **Yes** | Minutes | Moderate; document the lag |
| Order status | **Yes, short** | Short TTL + evict on status change | Customer sees old status — annoying, usually not money-wrong |
| Dashboard summary | **Yes, short** | Short TTL + evict on change | Same |
| Account **balance** | **No** | — | Wrong spending / overdraft decision |
| **Fraud decision** | **No** | — | Approve a payment that should fail |

Ask: what is the business impact of **five minutes** of staleness? If the answer is “we move money wrong,” do not cache.

### Why this is the answer

Cache what is read often and **tolerates** staleness. Never cache what must be exact **right now**.
