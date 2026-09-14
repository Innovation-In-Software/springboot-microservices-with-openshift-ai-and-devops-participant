# Exercise 3.1 — Configure Timeout and Circuit Breaker

**Module 6** (Resilient, High-Performance Microservices) · Day 3 · **Checkpoint A**  
**Time:** 10 min · **Type:** code warmup

**Objective:** Resilience4j on Account dependency

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Find the RestClient timeouts (connect 2s / read 3s) in the Lab 3 Transaction starter
2. Circuit breaker: 4 failed calls / 50% open it; wait 10s; later calls fail fast. 409 does not trip it
3. Safe fallback: POST /api/v1/transactions returns 503 ACCOUNT_SERVICE_UNAVAILABLE — no Kafka event
4. Bulkhead and rate limiting stay conceptual — do not install extra libraries for them

## Expected result

You can state the fallback rule: never invent a successful financial outcome.

## Lab connection

Lab 3 wraps AccountClient.requireActiveAccount. Fallback is 503, never a fake ACTIVE account.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

