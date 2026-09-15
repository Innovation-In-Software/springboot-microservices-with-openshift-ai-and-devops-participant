# Lab 5 starter — Risk Assessment Service

Participant folder. Follow **[LAB-5-GUIDE.md](../LAB-5-GUIDE.md)**.

Do **all** of this **on the Ablaze VM** in `%USERPROFILE%\MD287`. Use **`curl.exe`**. **`oc login` is required** in this lab (`student01`–`student25`, project `md287-studentNN`).

Complete `PolicyEngine` and the JWT scope rules in `SecurityConfig`. `ModelClient` and `AssessmentService` are already done. Java for Account and Transaction stays in Labs 1–3. Do not copy from a `solution/` folder.

```text
starter/
  risk-assessment-service/   ← Maven module (TODOs in policy + security)
  docker-compose.yml         ← Postgres, Kafka, mock model
  openshift/                 ← ConfigMap, Secret, Deployment, Route
  mcp-controls.md            ← Exercise 5.3 (already filled — walk it)
```

Shared tools: `../tools/` (JWT, mock model, sample events).
