# Module 7 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module07_DevOps_CICD_for_Microservices.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoints in this deck: [Exercise 3.3](../day-03/exercises/exercise-3.3-security-resilience-tests.md) and [Exercise 4.3](../day-04/lab4/starter/pipeline/OWNERSHIP.md). This README covers only the **unnumbered** activities.

---

## Exercise: Design a Branching Workflow

**Time:** 10 minutes

### Scenario

Six developers own Order Service. They release several times a week and must ship an urgent fix within an hour. Today some branches live for three weeks and merges break `main`.

### Solution

| Topic | Answer |
| --- | --- |
| Model | **Short-lived feature branches** (1–2 days) or trunk-based with feature flags. Three-week branches are the problem. |
| Branch types / names | `feature/`, `bugfix/`, `hotfix/` plus a work-item id. Protected `main`. |
| Merge gates | Build, tests, vulnerability scan, **one human approval**. |
| Urgent production fix | `hotfix/` **from `main`**, same pipeline gates, **faster review — not skipped**. |
| Unfinished work | **Feature flags**, not long-lived branches. |

### Why this is the answer

A good workflow keeps branches short, gates every merge, and still allows a fast **safe** hotfix.

---

## Exercise: Choose a Release Strategy

**Time:** 10 minutes

### Scenario

- **A.** Payment Service: new fraud-check call  
- **B.** Order Service: fix a typo in an error message  
- **C.** Inventory Service: rename a DB column  
- **D.** Transfer API: new routing to a partner bank  

### Solution

| Release | Strategy | Stop signals | Rollback |
| --- | --- | --- | --- |
| A Fraud call | **Canary** | Failed payments, latency, fraud-timeouts | Repoint Route / scale canary to 0. Schema unchanged. |
| B Typo | **Rolling update** | Readiness failures (unlikely) | Previous ReplicaSet. Low risk. |
| C Rename column | **Expand-and-contract first**, then Blue/Green or rolling | Migration errors, 5xx on inventory | **Schema stays compatible**: add new column, dual-write/read, then drop old later. Traffic rollback alone cannot undo a breaking rename. |
| D Partner bank routing | **Canary** to a small share, with **approval** | Failed transfers, partner errors, duplicates | Repoint Route. Partner-side state may need a playbook. |

### Why this is the answer

Match strategy to **risk**. Plan **data** rollback before traffic rollback.

---

## Exercise: Find the Pipeline Security Problems

**Time:** 12 minutes

### Scenario

```yaml
deploy:
  runs-on: ubuntu-latest
  permissions: write-all
  env:
    DB_PASSWORD: SuperSecret123
  steps:
    - uses: some-user/deploy-action@main
    - run: docker build -t order:latest .
    - run: echo $DB_PASSWORD
    - run: ./deploy.sh prod order:latest
```

### Solution — at least five problems

| Problem | Risk | Fix |
| --- | --- | --- |
| `permissions: write-all` | Job can rewrite repo, packages, anything the token allows | Least privilege (`id-token`, `contents: read`, deploy-only) |
| Password in YAML | Secret in Git | Platform secret store; never commit |
| `echo $DB_PASSWORD` | Secret in CI logs | Masked secrets; never echo |
| `some-user/deploy-action@main` | Supply chain: `@main` moves; unknown author | Pin to a **commit SHA** after review |
| Rebuild `:latest` in deploy | Production runs an artifact no test scanned | **Promote** the already-built, scanned **digest** |
| No scan / approval gate | Vulnerable or accidental prod push | Scan + human approval before prod |

Safe shape: pin the action, inject secrets from the store, deploy the **scanned digest**, require approval, no `echo`.
