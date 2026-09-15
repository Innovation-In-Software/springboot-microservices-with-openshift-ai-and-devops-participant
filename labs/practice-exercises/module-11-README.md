# Module 11 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module11_MCP_and_Microservices.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint in this deck: [Exercise 5.3](../day-05/lab5/starter/mcp-controls.md).

---

## Exercise: Tool, Resource or Prompt?

**Time:** 8 minutes

### Scenario

1. Current status of an order  
2. The refund policy document  
3. “Investigate a failed payment” steps  
4. Start a refund  
5. The error-code catalog  
6. Reserve stock for an order  

### Solution

| # | Kind | State-changing? | Human approval? |
| --- | --- | --- | --- |
| 1 Order status | **Tool** (read) | No | No |
| 2 Refund policy | **Resource** | No | No |
| 3 Investigation steps | **Prompt** (template) | No | No |
| 4 Start a refund | **Tool** (action) | **Yes** | **Always** |
| 5 Error-code catalog | **Resource** | No | No |
| 6 Reserve stock | **Tool** (action) | **Yes** | By policy (limits may allow auto within bounds) |

Live data and actions → tools. Reference content → resources. Reusable workflows → prompts.

---

## Exercise: MCP or REST?

**Time:** 8 minutes

### Scenario

- **A.** Mobile app showing order history  
- **B.** Support copilot answering questions  
- **C.** Payment Service calling Order  
- **D.** IDE agent helping ops engineers  
- **E.** Nightly reconciliation batch  

### Solution

| Client | Protocol | Why |
| --- | --- | --- |
| A Mobile app | **REST** | Fixed, known calls |
| B Support copilot | **MCP** | AI client discovering tools |
| C Payment → Order | **REST** | Service-to-service, stable contract |
| D IDE agent | **MCP** | Discovery + descriptions in the IDE |
| E Nightly batch | **REST** | Programmatic, scheduled, known endpoints |

**Same either way:** both paths hit the **same Order API**, same authorization and validation. **No database shortcuts** for MCP.

---

## Exercise: Map APIs to Tools

**Time:** 10 minutes

### Scenario

```
GET  /api/v1/orders/{id}
POST /api/v1/orders/{id}/cancel
GET  /api/v1/payments/{id}
POST /api/v1/payments/{id}/refunds
GET  /api/v1/inventory/{sku}
DELETE /api/v1/admin/orders
GET  /actuator/metrics
```

### Solution

| Tool | Maps to | Kind |
| --- | --- | --- |
| `get_order` | `GET .../orders/{id}` | Read |
| `get_payment` | `GET .../payments/{id}` | Read |
| `check_stock` | `GET .../inventory/{sku}` | Read |
| `cancel_order` | `POST .../cancel` | State-changing, **scoped** |
| `request_refund` | `POST .../refunds` | State-changing, **approval** |

**Keep out**

- `DELETE /api/v1/admin/orders` — bulk admin, not an agent business capability.  
- `GET /actuator/metrics` — operational, not a customer-support tool.

Small set of well-named **business** tools. No kitchen-sink wrappers.

---

## Exercise: Review a Risky Tool

**Time:** 10 minutes

### Scenario

```
name: run_payment_operation
description: Does payment stuff
input: { operation: string, sql: string }
auth: shared service account (payments admin)
effect: executes immediately
```

### Solution — problems

| Problem | Risk |
| --- | --- |
| Vague name and description | Model cannot choose when to call it |
| Free-form `sql` | Injection; **bypasses Payment Service rules** |
| Free-form `operation` | Anything goes |
| Shared payments-admin account | No least privilege; no “who asked” |
| Executes immediately | No approval on money movement |

**Redesign:** `get_payment` (read, caller identity, strict schema) + `request_refund` (creates a request, human approval, no SQL). Drop `run_payment_operation`.
