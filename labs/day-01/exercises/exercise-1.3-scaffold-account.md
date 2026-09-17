# Exercise 1.3 — Scaffold Account Endpoints and Validation

**Module 3** (Spring Boot Microservices Development) · Day 1 · **Checkpoint C**  
**Time:** 15 min · **Type:** code warmup

**Objective:** Controllers, DTOs, validation rules

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Open labs/day-01/lab1/starter/account-service. TODOs are only in AccountService (create/get/update/activate/freeze/close). The controller is already mapped. DTOs have no validation annotations yet — Lab 1 Step 6 adds them.
2. Name the layers: Controller → Service → Repository → PostgreSQL (Flyway, not Hibernate DDL)
3. List validation rules you will enforce (customerId CUST- plus four digits, nickname length, legal status change). accountId is generated
4. Confirm logs will use accountId/status only — no request bodies or PII. GitHub Copilot (Free, already on this VM) can explain the starter TODOs; **review before accept** — do not skip into generated Lab 2 code.

## Expected result

You can point at the starter files Lab 1 will complete. Do not skip ahead into Lab 2.

## Lab connection

Finish the implementation in Lab 1 (starter on port 8081).

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

