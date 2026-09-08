# Lab 4 starter — Deploy the Capstone Services

Participant folder. Follow **[LAB-4-GUIDE.md](../LAB-4-GUIDE.md)**.

You write Containerfiles and probe/resource YAML here. Java stays in Lab 3 starter — do not copy those trees.

```text
starter/
  account-service/Containerfile      ← TODO: USER + EXPOSE 8081
  transaction-service/Containerfile  ← TODO: USER + EXPOSE 8082
  docker-compose.yml                 ← full stack (build context = Lab 3 starter)
  openshift/                         ← ConfigMap, Secret, Deployment (probes TODO)
  pipeline/                          ← prepared Tekton YAML + ownership worksheet
```

Shared tools (SBOM, scan gate) live in `../tools/`.
