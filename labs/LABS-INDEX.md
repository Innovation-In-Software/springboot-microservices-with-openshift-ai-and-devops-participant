# Labs

Log in to your **Ablaze virtual machine** first, then clone this participant repo **on that VM**. Each day has one progressive lab that continues the banking platform.

**Workstations:** TEKsystems Ablaze VMs (required) — not your personal laptop. Every student VM and the instructor VM already have **GitHub Copilot Free** in VS Code (sign in during Lab 0). **OpenShift (ARO):** classroom cluster `aro-md287` in **Central US** — required for Labs 4–5. Assigned project is `md287-<your-username>`. Workstation checklist: [Lab 0](day-00/lab0/LAB-0-GUIDE.md).

Portal: **https://my.ablazedesktop.com** (find your name, username, and password in [LAB-ACCESS.md](../LAB-ACCESS.md)). After the Windows desktop appears, clone **inside the VM**:

```powershell
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

If you are not on the VM desktop yet, start with **[Lab 0](day-00/lab0/LAB-0-GUIDE.md) Step 1**. Use **`curl.exe`** in PowerShell.

Official checkpoint worksheets: [EXERCISES-INDEX.md](EXERCISES-INDEX.md) (matches the module PDFs in `slides/`). In-slide activity answers: [practice-exercises/](practice-exercises/).

## Day 1 morning: Log in and set up the VM

- [Lab 0 — Log in to the Ablaze VM and set up the workstation](day-00/lab0/LAB-0-GUIDE.md) — before Module 1

## Day 1: Architecture, API Design, and Spring Boot

- [Lab 1 — Account Service](day-01/lab1/LAB-1-GUIDE.md)
- Exercises: [1.1](day-01/exercises/exercise-1.1-analyze-monolith.md) · [1.2](day-01/exercises/exercise-1.2-account-apis.md) · [1.3](day-01/exercises/exercise-1.3-scaffold-account.md)

## Day 2: Data and Event-Driven Communication

- [Lab 2 — Transaction Service](day-02/lab2/LAB-2-GUIDE.md)
- Exercises: [2.1](day-02/exercises/exercise-2.1-data-ownership.md) · [2.2](day-02/exercises/exercise-2.2-event-flow.md) · [2.3](day-02/exercises/exercise-2.3-idempotency-dlq.md)

## Day 3: Resilience, Security, and Testing

- [Lab 3 — Secure and Resilient Services](day-03/lab3/LAB-3-GUIDE.md)
- Exercises: [3.1](day-03/exercises/exercise-3.1-timeout-circuit-breaker.md) · [3.2](day-03/exercises/exercise-3.2-jwt-roles-scopes.md) · [3.3](day-03/exercises/exercise-3.3-security-resilience-tests.md)

## Day 4: Observability, Containers, OpenShift, and CI/CD

- [Lab 4 — Deploy the Capstone Services](day-04/lab4/LAB-4-GUIDE.md)
- Exercises: [4.1](day-04/exercises/exercise-4.1-probes-correlation.md) · [4.2](day-04/exercises/exercise-4.2-containerize-account.md) · [4.3](day-04/lab4/starter/pipeline/OWNERSHIP.md)

## Day 5: OpenShift AI, MCP, and Capstone Completion

- [Lab 5 — Risk Assessment Service](day-05/lab5/LAB-5-GUIDE.md)
- Exercises: [5.1](day-05/exercises/exercise-5.1-model-policy-disposition.md) · [5.2](day-05/exercises/exercise-5.2-policy-rules.md) · [5.3](day-05/lab5/starter/mcp-controls.md)

Work in each lab's `starter/` folder. This pack has no `solution/`.

## Per-day layout

```text
labs/day-00/lab0/     ← LAB-0-GUIDE.md, tools/ (no starter Java project)
labs/day-NN/
  README.md
  exercises/             ← checkpoint worksheets
  labN/
    LAB-N-GUIDE.md
    starter/
```
