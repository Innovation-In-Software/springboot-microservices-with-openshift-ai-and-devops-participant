# Exercise 5.1 — Map Model Call → Policy → Disposition

**Module 10** (Agentic AI with Microsoft Services) · Day 5 · **Checkpoint A**  
**Time:** 10 min · **Type:** design

**Objective:** Governance boundary clarity

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Model returns a score (and metadata). It does not approve, hold, or decline money movement
2. PolicyEngine is deterministic Java. HOLD routes to a human with risk.write
3. Timeout or 5xx → HOLD MODEL_UNAVAILABLE — never auto-approve
4. Workbenches / KServe stay lecture — you call HTTP :8090 or MD287_MODEL_ROUTE

## Expected result

A three-box map: Model (score) → Policy (disposition) → Human review (HOLD only).

## Lab connection

Lab 5 ModelClient returns a score. Java PolicyEngine decides APPROVE / HOLD / DECLINE.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

