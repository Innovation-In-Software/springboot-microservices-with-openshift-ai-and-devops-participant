# Module 10 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module10_Agentic_AI_with_Microsoft_Services.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoint in this deck: [Exercise 5.1](../day-05/exercises/exercise-5.1-model-policy-disposition.md).

---

## Exercise: Chatbot, Copilot or Agent?

**Time:** 10 minutes

### Scenario

1. FAQ bot answering fee questions  
2. Drafts replies for support staff  
3. Looks up order + shipment, answers  
4. Cancels orders when customers ask  
5. Nightly: finds stuck payments, retries  

### Solution

| # | Label | Changes state? | Control |
| --- | --- | --- | --- |
| 1 | **Chatbot** | No | Grounded answers; no tools that write. |
| 2 | **Copilot** | No (human sends) | Human always in the send path; mask PII in the draft. |
| 3 | **Agent** (read-only tools) | No | Auth to read APIs; audit tool calls. |
| 4 | **Agent** | **Yes** (cancel) | Identity, authorization, **approval**, idempotency, audit. |
| 5 | **Autonomous workflow** | **Yes** (retry payment) | Strict limits, stop condition, alerts; not an unbounded chat loop. |

The label matters less than the **actions**. Anything that changes state needs authority, limits, and audit.

---

## Exercise: Design Tools and Stop Conditions

**Time:** 12 minutes

### Scenario

Support wants an agent for “my order hasn't arrived”. It may look up orders and shipments, reship low-value orders, and request refunds.

### Solution

| Kind | Tools |
| --- | --- |
| Read | `get_order`, `get_shipment` |
| Action | `request_reship`, `request_refund` (create **requests**, do not silently ship or move money) |

**Approval:** refunds **always**; reships above a business value limit (low-value reship may be automatic **if policy says so**).

**Stop conditions (three):**

1. Request **resolved** (found / reship requested / handed to human).  
2. **Step or time limit** (e.g. 8 steps or 5 minutes).  
3. **Unclear intent or out of policy** → human.

**Workflow state:** a **workflow store** (case id, steps, correlation), **not** only the chat transcript.

---

## Exercise: Investigate a Failed Payment

**Time:** 12 minutes

### Scenario

An AI assistant investigates why a payment failed and drafts a response for the support agent.

### Solution

| Question | Answer |
| --- | --- |
| APIs | **Read only:** payment status, transaction, order. No refund/cancel tools in this design. |
| Mask | Account numbers, PAN/tokens, personal data in prompts **and** in the draft. |
| Send automatically? | **No.** A person reviews and sends. |
| Logged | Tools called, resource ids, correlation / trace id, outcome. **Never** the raw secret or full account number. |
| Transaction Service down | **Say it could not check.** Do not guess a reason. |

### Why this is the answer

Read-only tools, masked data, a human sender, full audit, honest answers when a service is down.
