# Module 3 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module03_Spring_Boot_Microservices_Development.pptx`  
**Story:** Order Service  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint for this module: [Exercise 1.3](../../labs/day-01/exercises/exercise-1.3-scaffold-account.md) (banking Account Service).

---

## Exercise: Choose the Starters

**Time:** 10 minutes

### Scenario

Order Service needs a REST API for `/orders`, PostgreSQL persistence, request validation, a health endpoint for OpenShift, and unit/slice tests. A teammate also wants Spring Security, Kafka, and Redis “just in case”.

### Solution

| Need | Starter | What it auto-configures |
| --- | --- | --- |
| REST `/orders` | `spring-boot-starter-web` | Spring MVC, embedded Tomcat, Jackson |
| Save in PostgreSQL | `spring-boot-starter-data-jpa` + PostgreSQL driver | `DataSource`, Hibernate, repositories |
| Request validation | `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotNull`, …) |
| Health for OpenShift | `spring-boot-starter-actuator` | `/actuator/health` (and more — expose only what you need) |
| Tests | `spring-boot-starter-test` | JUnit, Mockito, AssertJ, slice-test support |

**Leave out** Security, Kafka, and Redis until a requirement needs them. Each one switches on auto-configuration, adds startup time and attack surface. Security **locks every endpoint by default**.

### Why this is the answer

Every starter is a decision, not a shopping list.

---

## Exercise: Refactor a Fat Controller

**Time:** 12 minutes

### Scenario

```java
@PostMapping("/orders")
Order create(@RequestBody Order o) {
  if (o.getItems().isEmpty())
    return null;
  int open = jdbc.queryForObject(
    "select count(*) from orders"
    + " where customer_id = ?",
    Integer.class, o.getCustomerId());
  if (open > 5)
    throw new RuntimeException("limit");
  o.setStatus("CREATED");
  System.out.println("Saved " + o);
  return orderRepository.save(o);
}
```

### Solution — line labels

| Piece | Layer |
| --- | --- |
| `@PostMapping` / HTTP mapping | Controller |
| `if items isEmpty` / `return null` | Validation (belongs on the DTO, not `null`) |
| SQL count of open orders | Repository |
| `open > 5` | Service / domain rule |
| `setStatus("CREATED")` | Service or domain |
| `System.out.println` | Logging (use the logger; **orderId only**, not the whole entity) |
| `orderRepository.save` | Repository, called from the service |

### Rewrite

- Signature: `ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request)`
- Return **201 Created** with a DTO, never the JPA entity.
- Empty items → `@NotEmpty` on the DTO → **400**.
- Open-order limit → domain exception from the service → **409**.
- Count → `orderRepository.countByCustomerIdAndStatus(...)`.
- `return null` is never an answer.

### Why this is the answer

Thin controller, rule-owning service, query-only repository. Easier to test and change.

---

## Exercise: Why Won't It Wire?

**Time:** 10 minutes

### Scenario

Startup: “Parameter 0 of constructor in `OrderController` required a bean of type `OrderService` that could not be found.”

```java
package com.example.order;
@SpringBootApplication
public class OrderApplication { }

package com.example.services;
public class OrderService { }

package com.example.order.web;
@RestController
public class OrderController {
  @Autowired
  private OrderService service;
}
```

(The error text says constructor injection; the snippet uses field `@Autowired`. Treat both: the bean is missing either way.)

### Solution

**Two reasons `OrderService` is not a bean**

1. No stereotype (`@Service` / `@Component`). Component scanning ignores it.
2. `com.example.services` is **outside** `com.example.order`, where scanning starts from `@SpringBootApplication`.

**Fix**

- Move to `com.example.order.service`.
- Add `@Service`.
- Inject with a **constructor** into a `private final OrderService` field (drop field `@Autowired`).

**Which startup step failed:** bean creation — **before** the embedded Tomcat starts.

---

## Exercise: Which Value Wins?

**Time:** 10 minutes

### Scenario

```yaml
# application.yml
server.port: 8080
orders.max-items-per-order: 50

# application-prod.yml
server.port: 8081
orders.max-items-per-order: 20
```

- **A:** no profile  
- **B:** `prod` profile  
- **C:** `prod` profile, env `SERVER_PORT=9090`, command-line `server.port=7070`

### Solution

| Run | Port | Item limit |
| --- | --- | --- |
| A | **8080** | **50** |
| B | **8081** | **20** |
| C | **7070** | **20** |

Priority (highest first): **command line > environment variables > profile-specific file > `application.yml`**.

On run C, `server.port=7070` beats `SERVER_PORT=9090`. The item limit still comes from `application-prod.yml` (20).

**Database password:** OpenShift Secret (or equivalent) as an environment variable. **Never** a YAML file in Git.

Relaxed binding reminder if someone asks: `SERVER_PORT` maps to `server.port`, which is why it is in the fight at all — and still loses to the command line.
