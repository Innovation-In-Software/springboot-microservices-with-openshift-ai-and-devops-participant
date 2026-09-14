# Exercise 1.1 — solution

**Module 1** · Day 1 · Checkpoint A  
**Type:** design

Use this after you finish your own sketch. Lab 1 still builds Account Service — this page is the design checkpoint only.

## Answer

### 1. Capabilities in the monolith

Typical banking monolith modules: customer **profiles**, **accounts**, **transactions** / payments, **loans**, **fraud**, **reporting**, **notifications**. They share one process, one database, and one release train.

### 2. Tight coupling / change / traffic

| Area | Why it is a problem |
| --- | --- |
| Accounts + transactions in one schema | A payment change forces an account release (and the reverse). |
| Reporting queries against live account tables | Month-end reports lock or slow money-movement paths. |
| Notifications / email in the same JVM | A mail outage or a report job can take down POST /accounts. |
| Fraud scoring in-process | Model latency becomes account-open latency. |

High-change / high-traffic: **accounts** (every new customer) and **transactions** (every money movement). Reporting is high-load but lower change.

### 3. Failure isolation

Money movement (create account, activate, later POST /transactions) must stay up if **reporting** or **notifications** fail. Extract those later; do not let a batch job share the Account Service process or `account_db`.

### 4. First extract: Account Service

This course extracts **Account Service** first because:

- It is a clear bounded context (accounts only — not transactions, not risk scores).
- Labs 2–5 consume it as a **REST eligibility** dependency (`GET` active account).
- Status (PENDING / ACTIVE / FROZEN / CLOSED) is a local invariant; other domains should not own that table.

**Not this week:** Customer Service, Redis, loans, a separate Notification service, Jenkins, Microsoft agent runtimes.

## Expected result (filled)

A short coupling list and one justified first extract: **Account Service**. Transaction and Risk come later; they call Account rather than owning its table.
