# Exercise 2.2 — Sketch TransactionSubmitted Produce/Consume Flow

**Module 4** (Microservices Integration) · Day 2 · **Checkpoint B**  
**Time:** 10 min · **Type:** design

**Objective:** Producer, consumer, correlation ID

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. After a valid REST create: persist RECEIVED, then publish TransactionSubmitted
2. Envelope: eventType, eventVersion, eventId, correlationId, occurredAt, payload
3. Copy X-Correlation-Id from the HTTP request onto the event and into logs
4. Lab 2 implements TransactionSubmitted only. Approved / HeldForReview / Declined are later; RISK_ASSESSED is internal

## Expected result

A sequence sketch: REST → DB → Kafka → consumer. Correlation ID on every hop.

## Lab connection

Lab 2 publishes and consumes TransactionSubmitted only (Compose Kafka; AMQ Streams later on OpenShift).

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

