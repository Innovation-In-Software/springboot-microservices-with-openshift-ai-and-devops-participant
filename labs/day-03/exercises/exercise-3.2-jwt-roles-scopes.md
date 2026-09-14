# Exercise 3.2 — Secure an Endpoint with JWT Roles/Scopes

**Module 12** (Security and Compliance) · Day 3 · **Checkpoint B**  
**Time:** 10 min · **Type:** code warmup

**Objective:** Roles/scopes on protected APIs

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. No Bearer token → 401. Wrong scope → 403 (not a silent 200)
2. teller: accounts.read/write. ops: transactions + accounts.read. readonly: *.read
3. Transaction Service forwards the caller's JWT when it GETs Account Service
4. Never log Authorization headers or JWT claims

## Expected result

A scope matrix you can test with issue-jwt.py teller / ops / readonly.

## Lab connection

Lab 3 uses issue-jwt.py (HMAC). Keycloak / Azure AD are lecture only.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

