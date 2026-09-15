# Spring Boot Microservices with OpenShift AI and DevOps

Participant materials for the instructor-led **5-day** course: module slide PDFs and labs.

You build a single evolving **AI-Assisted Banking Transaction Risk Platform** across the week (Account → Transaction → Risk Assessment).

**Audience:** Bank of America / enterprise Java developers, backend engineers, DevOps/platform engineers, technical leads, architects  
**Format:** Instructor-led · slides · progressive labs · capstone demonstration

## Clone

```powershell
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

No GitHub login for clone. Sign in to **GitHub Copilot Free** in VS Code on the VM during Lab 0. Work in each lab's `starter/` folder.

## Technology stack

```text
Java 21             Spring Boot 3.x       Spring Data JPA       Spring Security
Maven               Flyway                PostgreSQL            Resilience4j
Kafka               OpenAPI / Swagger UI  Micrometer / Actuator
Docker Desktop      Red Hat OpenShift     OpenShift Pipelines
GitHub Copilot      OpenShift AI          MCP
```

**Workstations:** TEKsystems Ablaze VMs (required all five days). Every student VM and the instructor VM already have **GitHub Copilot Free** in VS Code (sign in during Lab 0). **OpenShift (ARO):** required for Labs 4–5.

**Your login:** find your name in [LAB-ACCESS.md](LAB-ACCESS.md) (Ablaze portal, username, passwords, OpenShift project). Then start [Lab 0](labs/day-00/lab0/README.md) **on that VM**, not on a laptop.

## Slides (PDF)

Module decks: [slides/README.md](slides/README.md) (Course Introduction, Modules 1–12, Capstone).

Per-module reading packs (PDF): [slides/supplementary material](slides/supplementary%20material/README.md).

## Labs

Index: [labs/LABS-INDEX.md](labs/LABS-INDEX.md) · Checkpoints: [labs/EXERCISES-INDEX.md](labs/EXERCISES-INDEX.md) · In-slide activity answers: [labs/practice-exercises/](labs/practice-exercises/)

| Day | Lab | Exercises |
| --- | --- | --- |
| 1 morning | [Lab 0 — Environment Setup](labs/day-00/lab0/LAB-0-GUIDE.md) | — |
| 1 | [Lab 1 — Account Service](labs/day-01/lab1/LAB-1-GUIDE.md) | [1.1](labs/day-01/exercises/exercise-1.1-analyze-monolith.md) ([solution](labs/day-01/exercises/exercise-1.1-analyze-monolith-solution.md)) · [1.2](labs/day-01/exercises/exercise-1.2-account-apis.md) ([solution](labs/day-01/exercises/exercise-1.2-account-apis-solution.md)) · [1.3](labs/day-01/exercises/exercise-1.3-scaffold-account.md) |
| 2 | [Lab 2 — Transaction Service](labs/day-02/lab2/LAB-2-GUIDE.md) | [2.1](labs/day-02/exercises/exercise-2.1-data-ownership.md) · [2.2](labs/day-02/exercises/exercise-2.2-event-flow.md) · [2.3](labs/day-02/exercises/exercise-2.3-idempotency-dlq.md) |
| 3 | [Lab 3 — Secure and Resilient Services](labs/day-03/lab3/LAB-3-GUIDE.md) | [3.1](labs/day-03/exercises/exercise-3.1-timeout-circuit-breaker.md) · [3.2](labs/day-03/exercises/exercise-3.2-jwt-roles-scopes.md) · [3.3](labs/day-03/exercises/exercise-3.3-security-resilience-tests.md) |
| 4 | [Lab 4 — Deploy the Capstone Services](labs/day-04/lab4/LAB-4-GUIDE.md) | [4.1](labs/day-04/exercises/exercise-4.1-probes-correlation.md) · [4.2](labs/day-04/exercises/exercise-4.2-containerize-account.md) · [4.3](labs/day-04/lab4/starter/pipeline/OWNERSHIP.md) |
| 5 | [Lab 5 — Risk Assessment Service](labs/day-05/lab5/LAB-5-GUIDE.md) | [5.1](labs/day-05/exercises/exercise-5.1-model-policy-disposition.md) · [5.2](labs/day-05/exercises/exercise-5.2-policy-rules.md) · [5.3](labs/day-05/lab5/starter/mcp-controls.md) |

## Capstone (built across Labs 1–5)

1. End-to-end successful transaction
2. Duplicate-event handling
3. Model endpoint failure / timeout (safe fallback)
4. Human-review routing
5. OpenShift deployment evidence
6. Rollback or recovery

© 2026 Innovation In Software Corporation
