# Module 12 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module12_Security_and_Compliance.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint in this deck: [Exercise 3.2](../../labs/day-03/exercises/exercise-3.2-jwt-roles-scopes.md).

---

## Exercise: 401 or 403?

**Time:** 8 minutes

### Scenario

`GET /api/v1/accounts/ACC-5001` is owned by customer `CUST-0007`.

- **A.** No `Authorization` header  
- **B.** Expired token  
- **C.** Valid token, scope `transactions.read`  
- **D.** Valid token, `accounts.read`, `sub` `CUST-0009`  
- **E.** Valid token, `accounts.read`, `sub` `CUST-0007`  
- **F.** Token signed by an unknown issuer  

### Solution

| Request | Status | Authn vs authz |
| --- | --- | --- |
| A | **401** | Authentication — we don't know who you are |
| B | **401** | Authentication — expired |
| C | **403** | Authorization — wrong **scope** |
| D | **403** or **404** | Authorization — not the owner. Some APIs return 404 so they do not confirm the account exists. |
| E | **200** | Authenticated and authorized |
| F | **401** | Authentication — untrusted issuer |

**Logging:** subject, resource, decision. **Never** the `Authorization` header or raw token.

Rule: **401** = we don't know who you are. **403** = we know, and the answer is no.

---

## Exercise: Find the Secret Leaks

**Time:** 10 minutes

### Scenario

```
application-prod.yml:
  gateway.api-key: sk_live_9f2c...
Dockerfile:
  ENV DB_PASSWORD=Payments#2026
PaymentService.java:
  log.info("Calling gateway with {}", headers)
ci.yml:
  run: echo $REGISTRY_TOKEN
```

### Solution

| Leak | Who can see it | Fix | Rotate now? |
| --- | --- | --- | --- |
| API key in Git | Anyone with repo (and forks/history) | Secret store; purge from history if needed | **Yes** |
| Password in image layer | Anyone who can pull the image | Runtime OpenShift Secret; rebuild without `ENV` | **Yes** |
| Headers logged | Anyone with log access | Mask `Authorization` and secrets; structured logs | **Yes** if tokens appeared in logs |
| Token echoed in CI | Anyone with CI log access | Masked secrets; never `echo` | **Yes** |

Removing the line is not enough. **Rotate everything already exposed.**

---

## Exercise: Map Threats to Controls

**Time:** 10 minutes

### Scenario

1. Changing `accountId` shows others' data  
2. Response includes full card number  
3. No limit on `POST /transfers`  
4. Error returns the SQL statement  
5. Retrying a timeout sends money twice  
6. CORS allows any origin  

### Solution

| # | Threat | Control | Where |
| --- | --- | --- | --- |
| 1 | Broken **object-level authorization** (BOLA/IDOR) | Ownership check: this caller may access **this** account | **Service** (not only the gateway) |
| 2 | Sensitive **data exposure** | Masked DTO — last4 / token, never full PAN | Service response mapping |
| 3 | Abuse / flooding | **Rate limit** | **Gateway** (and app limits as defence in depth) |
| 4 | Information leak | Safe error handler; log SQL internally | `@ControllerAdvice` / exception handler |
| 5 | Replay / duplicate money movement | `Idempotency-Key` | Service (and client must send it) |
| 6 | CSRF-ish / data theft via browser | CORS **allow-list** of trusted origins | Gateway or Spring CORS config |

### Why this is the answer

Each common threat has a known control **and a right layer**. Gateway rate limits do not replace service ownership checks.
