# Exercise 3.3 — Add Security and Resilience Tests

**Module 7** (DevOps and CI/CD) · Day 3 · **Checkpoint C**  
**Time:** 10 min · **Type:** code warmup

**Objective:** Unit + one integration test

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Unit-test 401/403 on a protected Account or Transaction endpoint
2. Unit-test circuit-open → `CallNotPermittedException` (fail-fast). HTTP 503 `ACCOUNT_SERVICE_UNAVAILABLE` is the REST handler — the unit test does not call MockMvc
3. Name AccountPersistenceTest — Testcontainers Postgres 16; Docker must be running for mvn test
4. Testcontainers is required for that integration test, not optional awareness

## Expected result

A short test list that Lab 3 can tick. Coverage of security and the fallback rule.

## Lab connection

Lab 3 adds unit tests plus one guided integration test (AccountPersistenceTest).

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

