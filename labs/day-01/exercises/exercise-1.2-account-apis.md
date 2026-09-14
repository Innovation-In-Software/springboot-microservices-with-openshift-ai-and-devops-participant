# Exercise 1.2 — Design Account Service APIs and Boundaries

**Module 2** (Microservices Design) · Day 1 · **Checkpoint B**  
**Time:** 15 min · **Type:** design

**Objective:** REST resources, status transitions, OpenAPI sketch

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Resource is an account. Sketch POST / GET / PATCH and activate / freeze / close (no DELETE)
2. Status machine: PENDING → ACTIVE → FROZEN or CLOSED; freeze PENDING is 409; FROZEN can activate; retries return 200
3. List 400 / 404 / 409 cases; note OpenAPI + Actuator as Lab 1 deliverables
4. Draw the bounded context: Account owns accounts only — not transactions, not risk scores

## Expected result

A one-page Account API sketch that Lab 1 can implement without changing URLs tomorrow.

## Reference solution

After you finish your sketch, compare with [Exercise 1.2 solution](exercise-1.2-account-apis-solution.md).

## Lab connection

Use this contract in Lab 1. Do not add Customer or Notification services.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

