# Module 9 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module09_GitHub_Copilot_for_Spring_Boot.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint in this deck: [Exercise 5.2](../day-05/exercises/exercise-5.2-policy-rules.md).

---

## Exercise: Improve a Weak Prompt

**Time:** 10 minutes

### Scenario

Developer typed `make a transfer endpoint` and got an insecure, entity-returning controller.

### Solution

**What Copilot had to guess:** path, DTOs vs entities, validation, status codes, security/scopes, idempotency, logging hygiene, tests.

**Rewrite (example — keep this quality, wording can vary):**

> Context: Spring Boot 3 Transaction Service, Java 21, existing Account client.  
> Task: add `POST /api/v1/transfers` that accepts a validated `TransferRequest` record and returns `TransferResponse`.  
> Rules: require header `Idempotency-Key`; never return JPA entities; never log account numbers; check source account is ACTIVE via Account Service.  
> Errors: 201 created, 400 validation, 401/403 security, 404 unknown account, 409 duplicate key or conflict.  
> Output: controller, service method signatures, tests for 400 and duplicate key.

**Still check in the result:** authorization (may this caller move **this** money?) and the real business rules (limits, frozen accounts). A perfect prompt can still be wrong.

---

## Exercise: Review AI-Generated Transfer Code

**Time:** 12 minutes

### Scenario

Copilot generated `transferFunds` for Transaction Service. The PR author says “tests pass”.

### Solution

| Question | Answer |
| --- | --- |
| Business rules to check **manually** | Positive amount, sufficient funds / eligibility, account status (ACTIVE vs FROZEN), daily/limits, no self-serve bypass. Compare to the **requirement**, not to the generated code. |
| Tests to add | Insufficient funds, frozen/closed account, **duplicate Idempotency-Key**, Account Service timeout → 503 / fail closed. Generated tests are often happy-path only. |
| Security review | Object-level auth: does the caller own the source account? Scopes on the endpoint. No account numbers in logs. No `.get()` on empty `Optional` that turns a 404 into 500. |
| Transactional? | **Yes**, `@Transactional` — both updates or neither. |
| Unacceptable | Logged PANs/account numbers; swallowing errors; fake success. |

“Tests pass” proves little if the tests and the production code came from the **same prompt**.

---

## Exercise: Troubleshoot a Startup Failure

**Time:** 10 minutes

### Scenario

```
APPLICATION FAILED TO START
Parameter 0 of constructor in
  com.example.order.web.OrderController
required a bean of type
  com.example.billing.PaymentClient
that could not be found.
```

### Solution

**Prompt to Copilot:** paste the **full** error, the `@SpringBootApplication` package (`com.example.order`), and **what the merge changed** (new `PaymentClient` in `com.example.billing`). Ask for ranked hypotheses, not a blind patch.

**Likely causes**

1. `com.example.billing` is **outside** component scan (`com.example.order`).  
2. Missing `@Component` / `@Service` / `@Bean`.  
3. `@Profile` / `@ConditionalOn...` bean not active.

**Verify before changing code:** `--debug` (conditions report), active profiles, bean list. Then the **smallest** fix: move the client under the scanned tree, or `@Bean` in a `@Configuration` that *is* scanned, or enable the profile.

### Why this is the answer

Give Copilot the evidence, weigh hypotheses, **verify** the cause, then fix. Do not apply the first suggested `@ComponentScan("com.example")` of the entire company.
