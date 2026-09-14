# Day 4 Labs — Observability, Containers, OpenShift, and CI/CD

**Lab 4:** Deploy the Capstone Services  
**Day outcome:** Services observable, containerized, and **deployed to the pre-provisioned OpenShift project**, plus a completed prepared pipeline (SBOM, scan, signature).

Run **Lab 4** as the Day 4 hands-on. Stop Lab 1–3 Compose stacks first — they share ports 8081, 8082, 5433, 5434, and 9092.

## Layout

```text
exercises/               ← checkpoints 4.1–4.2
lab4/
  LAB-4-GUIDE.md
  tools/                 ← SBOM, scan gate, JWT request bodies
  starter/               ← Containerfiles, Compose, OpenShift YAML, Tekton
  solution/              ← instructor reference
```

| Resource | Path |
| -------- | ---- |
| Lab guide | [lab4/LAB-4-GUIDE.md](lab4/LAB-4-GUIDE.md) |
| Starter | [lab4/starter/](lab4/starter/) |
| Solution | [lab4/solution/](lab4/solution/) |
| Exercise 4.1 | [exercises/exercise-4.1-probes-correlation.md](exercises/exercise-4.1-probes-correlation.md) |
| Exercise 4.2 | [exercises/exercise-4.2-containerize-account.md](exercises/exercise-4.2-containerize-account.md) |
| Exercise 4.3 | [lab4/starter/pipeline/OWNERSHIP.md](lab4/starter/pipeline/OWNERSHIP.md) |
