# Module 1 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module01_Microservices_Fundamentals.pptx`  
**Story:** order / checkout platform (not the banking labs)  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint for this module: [Exercise 1.1](../day-01/exercises/exercise-1.1-analyze-monolith.md).

---

## Exercise: Checkout When Payment Is Down

**Time:** 10 minutes · **How to run:** five minutes in pairs, then compare. There is no single right answer, but there are unsafe ones.

### Scenario

A customer checks out a $180 order. Order Service calls Payment Service, but Payment has been timing out for three minutes.

### Solution

| Decision | Answer |
| --- | --- |
| Behaviour | Keep the order as `PENDING_PAYMENT`. Do **not** approve without payment, and do **not** blindly retry the charge. |
| Customer sees | “We’re confirming your payment” (or equivalent). Never “Paid” and never a silent hang. |
| Reserved stock | Hold the reservation. Retry payment in the background with an **idempotency key**. Release stock after a business time limit if payment never succeeds. |
| Alert | Owning team: payment error rate **and** latency (the three-minute timeout is already a latency incident). |

Retry in the background is allowed **only** if every attempt reuses the same idempotency key so the customer is charged once.

### Unsafe answers

- Blind retry of `POST /payments` with a new request each time → double-charge.
- Approve the order without payment → lost money.
- Fail the whole checkout with no pending state → lose a recoverable sale and confuse the customer.

### Why this is the answer

Failure handling is a **business** decision made explicit in code. Resilience4j retries and breakers come later; today the point is the outcome, not the library.

---

## Exercise: Monolith or Microservices?

**Time:** 10 minutes

### Scenario

- **A.** Internal tool for five staff to approve supply requests.
- **B.** Checkout platform with seasonal spikes and six product teams.
- **C.** Brand-new subscription product whose rules change weekly.
- **D.** Fraud scoring that changes daily and needs its own scaling.

### Solution

| Case | Choice | Why | Risk of that choice |
| --- | --- | --- | --- |
| A | **Monolith** | Small team, simple domain, low traffic. Faster to build and cheaper to run. | If the tool later becomes a company-wide portal, the single deployable becomes a bottleneck. |
| B | **Microservices** | Many teams, different scaling needs, frequent independent releases. | Distributed complexity, data ownership fights, and operational load if boundaries are wrong. |
| C | **Modular monolith first** | Domain is still moving. Splitting now risks the wrong cuts. | Modules that are not actually independent, so a later extract is painful. |
| D | **Separate microservice** | Changes daily and scales differently from checkout. | Chatty sync calls from checkout into fraud if the contract is not bounded and timed out. |

### Why this is the answer

The skill is explaining **why** (team size, domain clarity, scaling), not picking a fashionable label. “Microservices everywhere” for A is a miss.

---

## Exercise: Spot the Twelve-Factor Violations

**Time:** 10 minutes

### Scenario

```java
public class OrderService {
  String dbPassword = "Prod@123";
  String payUrl =
      "https://prod-pay.shop.com";
  Map<String, Cart> carts =
      new HashMap<>();

  void save(Order o) {
    writeFile("/var/log/orders.log", o);
  }
}
```

### Solution

| Violation | Factor | Twelve-Factor rewrite |
| --- | --- | --- |
| Password in source | **3 Config** | `DB_PASSWORD` from the environment / OpenShift Secret. Never in Git. |
| Production payment URL in source | **3 Config** and **4 Backing services** | `PAYMENT_URL` from config. Treat Payment as an attached resource. |
| Carts in a local `HashMap` | **6 Processes** | Store carts in a database or shared cache. A restart or a second pod must not lose or split carts. |
| Write to `/var/log/orders.log` | **11 Logs** | Log to **stdout**. The platform (OpenShift) collects the stream. A local file dies with the pod. |

### Discussion prompt

Ask which violation would hurt first in production. Usually the hard-coded secret or the in-memory state.

### Why this is the answer

Most Twelve-Factor problems show up in a few lines of code. ConfigMaps/Secrets, stateless pods, and stdout logs are the OpenShift translation.
