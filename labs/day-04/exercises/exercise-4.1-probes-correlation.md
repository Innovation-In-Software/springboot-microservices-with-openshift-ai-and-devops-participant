# Exercise 4.1 — Add Health Probes and Correlation Logging

**Module 8** (Containerization with OpenShift) · Day 4 · **Checkpoint A**  
**Time:** 10 min · **Type:** code warmup

**Objective:** Operability basics

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Liveness vs readiness vs startup — which Actuator paths, and why startup exists
2. X-Correlation-Id on the request must appear as correlationId= in logs
3. Health stays public; /actuator/metrics still needs a JWT
4. Tracing (OpenTelemetry) is a demonstration — you do not stand up Jaeger

## Expected result

You can tell liveness from readiness and find a correlation id in a log line.

## Lab connection

Lab 4 verifies liveness/readiness and correlationId= in logs, then the same probes on OpenShift.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

