# Spring Boot Microservices with OpenShift AI and DevOps

Participant materials for the instructor-led **5-day** course: Day 1–5 slide PDFs and labs (starter projects only).

You build a single evolving **AI-Assisted Banking Transaction Risk Platform** across the week (Account → Transaction → Risk Assessment).

**Audience:** Bank of America / enterprise Java developers, backend engineers, DevOps/platform engineers, technical leads, architects  
**Format:** Instructor-led · slides · progressive labs · capstone demonstration

## Clone

```powershell
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

No GitHub login. Work in each lab's `starter/` folder.

## Technology stack

```text
Java 17/21          Spring Boot 3.x       Spring Data JPA       Spring Security
Maven               Flyway                PostgreSQL            Resilience4j
Red Hat AMQ Streams (Kafka)               OpenAPI / Swagger UI  Micrometer / Actuator
Podman              Red Hat OpenShift     OpenShift Pipelines (Tekton)
OpenTelemetry       Prometheus / Grafana  Keycloak (or client IdP)
GitHub Copilot      Red Hat OpenShift AI  MCP (conceptual + demo)
```

**Workstations:** TEKsystems Ablaze VMs (required all five days). **OpenShift (ARO):** required for Labs 4–5.

- [System requirements](docs/SYSTEM-REQUIREMENTS.md) · [Word](docs/MD287_System_Requirements.docx)
- [Delivery flowchart](docs/lab-environment-flow.svg)
- [Complete course outline](docs/MD287_Complete_Course_Outline.md)

## Slides (PDF)

| Day | Theme | Deck |
| --- | ----- | ---- |
| 1 | Architecture, API Design, and Spring Boot | [MD287_Day1_Slides.pdf](slides/MD287_Day1_Slides.pdf) |
| 2 | Data and Event-Driven Communication | [MD287_Day2_Slides.pdf](slides/MD287_Day2_Slides.pdf) |
| 3 | Resilience, Security, and Testing | [MD287_Day3_Slides.pdf](slides/MD287_Day3_Slides.pdf) |
| 4 | Observability, Containers, OpenShift, and CI/CD | [MD287_Day4_Slides.pdf](slides/MD287_Day4_Slides.pdf) |
| 5 | OpenShift AI, MCP, and Capstone Completion | [MD287_Day5_Slides.pdf](slides/MD287_Day5_Slides.pdf) |

## Labs

| Day | Lab |
| --- | --- |
| 1 | [Lab 1 — Account Service](labs/day-01/lab1/LAB-1-GUIDE.md) |
| 2 | [Lab 2 — Transaction Service](labs/day-02/lab2/LAB-2-GUIDE.md) |
| 3 | [Lab 3 — Secure and Resilient Services](labs/day-03/lab3/LAB-3-GUIDE.md) |
| 4 | [Lab 4 — Deploy the Capstone Services](labs/day-04/lab4/LAB-4-GUIDE.md) |
| 5 | [Lab 5 — Risk Assessment Service](labs/day-05/lab5/LAB-5-GUIDE.md) |

Index: [labs/LABS-INDEX.md](labs/LABS-INDEX.md)

## Capstone (built across Labs 1–5)

1. End-to-end successful transaction
2. Duplicate-event handling
3. Model endpoint failure / timeout (safe fallback)
4. Human-review routing
5. OpenShift deployment evidence
6. Rollback or recovery

© 2026 Innovation In Software Corporation
