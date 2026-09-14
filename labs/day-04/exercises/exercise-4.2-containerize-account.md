# Exercise 4.2 — Containerize Account Service

**Module 8** (Containerization with OpenShift) · Day 4 · **Checkpoint B**  
**Time:** 10 min · **Type:** ops warmup

**Objective:** Secure non-root image

This is the official OUTLINE.md checkpoint. Complete it before or as directed in the day's lab. Do not substitute TEKsystems 13-lab exercises (Customer Service, Redis, Jenkins, Microsoft AI, MCP server).

---

## Do this

1. Multi-stage build: Maven compile in builder, JRE runtime in final image
2. USER md287 (non-root). OpenShift also sets runAsNonRoot: true
3. Immutable tag 1.0.0 — not :latest
4. Images are built from your Lab 3 starter context — do not recopy Java trees

## Expected result

You can explain why the runtime user is not root before you run docker build.

## Lab connection

Lab 4 Step 3 recap: multi-stage Containerfile, USER md287, tag md287/account-service:1.0.0.

## Success criteria

- [ ] Talked through every task above
- [ ] Artifact (sketch, map, or filled worksheet) is ready for the instructor to review
- [ ] Scope stays on Account / Transaction / Risk Assessment — no extra services

