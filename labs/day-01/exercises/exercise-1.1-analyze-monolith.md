# Exercise 1.1 — Analyze a Monolithic Banking Application

**Module 1** (Microservices Fundamentals) · Day 1 · **Checkpoint A**  
**Time:** 15 min · **Type:** design

**Objective:** Spot coupling and candidate boundaries

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. List major business capabilities in the monolith (profiles, accounts, transactions, loans, fraud, reporting, notifications)
2. Mark tightly coupled areas and modules that change often or carry high traffic
3. Mark where failure should be isolated so money movement is not taken down by reporting
4. Name the first extraction candidate and say why (the course extracts Account Service)

## Expected result

A short list of coupling problems and one justified first extract: Account Service.

## Lab connection

Lab 1 builds Account Service after Modules 1–3. This checkpoint is design-only.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

