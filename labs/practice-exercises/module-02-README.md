# Module 2 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module02_Microservices_Design.pptx`  
**Story:** order platform  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint for this module: [Exercise 1.2](../day-01/exercises/exercise-1.2-account-apis.md) (banking Account Service — different story).

---

## Exercise: Group Features into Services

**Time:** 10 minutes

### Scenario

Features: open a customer account · view a product · add to cart · place an order · charge a card · refund a payment · reserve stock · print a shipping label · email a receipt · flag a suspicious order.

### Solution

| Service | Features | Data it owns |
| --- | --- | --- |
| **Customer** | Open a customer account | Customer profile / account records |
| **Catalog** | View a product | Product information |
| **Order** | Add to cart, place an order | Carts and orders |
| **Payment** | Charge a card, refund a payment | Charges and refunds |
| **Inventory** | Reserve stock | Stock levels and reservations |
| **Shipping** | Print a shipping label | Shipments and labels |
| **Notification** | Email a receipt | Delivery of notices (not order rows) |
| **Fraud** (optional separate) | Flag a suspicious order | Scores / flags — split if the model changes often and scales alone |

Judgment calls: the cart usually lives with **Order**. Receipt email is a **Notification** capability, not an Order table. Fraud is its own service when it changes often; otherwise it can start as a module.

### Why this is the answer

Groupings justified by **business capability and owned data**, not by technical layer (UI / backend / database).

---

## Exercise: Spot the Tight Coupling

**Time:** 10 minutes

### Scenario

1. Order Service updates the Inventory database directly.  
2. Order Service calls Inventory's API to reserve stock.  
3. Notification polls the Order database every 5 seconds.  
4. Order publishes `OrderPlaced`; Notification reacts.  
5. Order and Payment share a pricing-rules library and must release together.

### Solution

| # | Verdict | Coupling | Better design |
| --- | --- | --- | --- |
| 1 | **Bad** | Data coupling | Call Inventory's reserve API (or send a reservation command). |
| 2 | **Good** | Contract only | Keep it. Bound the call with a timeout. |
| 3 | **Bad** | Data coupling | Consume `OrderPlaced` (or similar) events. |
| 4 | **Good** | Loose, asynchronous | Keep it. Consumer must be idempotent. |
| 5 | **Bad** | Deployment coupling | Pricing rules stay with the service that owns price. Publish a fact or expose an API; do not share the library. |

### Why this is the answer

Services never own each other's databases or internal business libraries. Independence is about **evolution**, not process count.

---

## Exercise: REST or Messaging?

**Time:** 10 minutes

### Scenario

1. Check stock before accepting an order  
2. Send the order confirmation email  
3. Record an audit entry after payment  
4. Show the current order status on screen  
5. Tell Analytics an order was placed  
6. Get a fraud score before charging the card

### Solution

| Step | Style | Why | Failure you must handle |
| --- | --- | --- | --- |
| 1 Stock check | **REST** | Need the answer before accepting | Timeout + fallback (do not accept on a hung inventory) |
| 2 Confirmation email | **Event** | Can happen later; Order should not wait | Duplicates; DLQ if Notification keeps failing |
| 3 Audit after payment | **Event** | Background; possibly several listeners | Duplicates; DLQ |
| 4 Current status on screen | **REST** | User is waiting on the page | Timeout + cached/last-known or error, not a fake “shipped” |
| 5 Analytics | **Event** | Many listeners; delay is fine | Duplicates; lag monitoring |
| 6 Fraud score before charge | **REST** | Feeds an immediate money decision | Timeout + **hold / fail closed**, never auto-approve |

Rule: immediate decision → REST. Background reaction or many listeners → messaging.

---

## Exercise: Stateless or Stateful?

**Time:** 8 minutes

### Scenario

Order Service pod · PostgreSQL order database · Kafka broker · shared cart cache · in-memory `HashMap` of pending payments · fraud-scoring REST API.

### Solution

| Component | Label | Scale freely? | Notes |
| --- | --- | --- | --- |
| Order Service pod | **Stateless** | Yes | Add replicas. Durable state is not in the JVM. |
| PostgreSQL order database | **Stateful** | No (careful) | Backups, failover, not “just add pods”. |
| Kafka broker | **Stateful** | No (careful) | Topics and offsets are durable. |
| Shared cart cache | **Stateful** | Shared store | Horizontal for the *app*, not for forgetting the cache. |
| In-memory `HashMap` of pending payments | **Stateful and risky** | No | Lost on restart; each pod has a different map. **Fix:** store pending payments in the database (or a shared store). |
| Fraud-scoring REST API | **Stateless** (the API process) | Yes, if it keeps no session | Model artifacts live elsewhere. |

### Why this is the answer

Scale the stateless tier freely; protect and back up stateful components. An in-memory map of money-movement state is the classroom “never do this in a container” example.
