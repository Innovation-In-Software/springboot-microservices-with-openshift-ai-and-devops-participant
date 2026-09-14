# Exercise 5.2 — Write Policy Rules for Approve / Hold / Decline

**Module 9** (GitHub Copilot for Spring Boot) · Day 5 · **Checkpoint B**  
**Time:** 15 min · **Type:** code

**Objective:** Synthetic approve/hold/decline thresholds

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. First match wins: model not OK → HOLD MODEL_UNAVAILABLE
2. Amount ≥ 5000 → HOLD HIGH_VALUE. Score ≥ 70 → DECLINE HIGH_SCORE
3. Score < 40 and amount < 1000 → APPROVE LOW_SCORE_LOW_VALUE. Else HOLD REVIEW_BAND
4. Copilot may draft the if/else — review before accept; tests must prove the table

## Expected result

The policy-v1 table written in Java, with tests for each row including model failure.

## Lab connection

Implement PolicyEngine in Lab 5 Step 3 using the policy-v1 table in LAB-5-GUIDE.md.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

