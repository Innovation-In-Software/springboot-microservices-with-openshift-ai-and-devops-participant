# Exercise 2.3 — Design Idempotency and DLQ Behaviour

**Module 4** (Microservices Integration) · Day 2 · **Checkpoint C**  
**Time:** 10 min · **Type:** design

**Objective:** Duplicate detection and failure routing

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Same eventId twice: second consume is a no-op (event_id already in processed_events)
2. Poison / unreadable JSON: retry then dead-letter — do not loop forever
3. A DLT message must not invent a successful money-movement outcome
4. Lab 5 Risk Assessment will be another consumer of the same topic — design for that

## Expected result

You can explain duplicate detection and DLT routing before you code them.

## Lab connection

Lab 2 uses processed_events keyed by event_id and topic transactions.submitted.DLT.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

