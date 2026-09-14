# Exercise 2.1 — Map Data Ownership for Account vs Transaction

**Module 5** (Microservices and Data) · Day 2 · **Checkpoint A**  
**Time:** 10 min · **Type:** design

**Objective:** Separate DBs/schemas; no cross-service tables

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Account table lives only in account_db. Transaction table lives only in transaction_db
2. Transaction stores accountId as a reference, not a foreign key across services
3. Eligibility is a REST GET to Account Service, not a SQL join
4. Redis is lecture-only — not part of this capstone

## Expected result

A two-column ownership map with zero shared tables.

## Lab connection

Lab 2 uses transaction_db on host port 5434. Never join to account_db.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

