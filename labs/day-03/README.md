# Day 3 Labs — Resilience, Security, and Testing

**Lab 3:** Secure and Resilient Services  
**Day outcome:** JWT-protected Account and Transaction APIs, a circuit breaker on the Account dependency, and tests that prove 401/403/503.

Run **Lab 3** as the Day 3 hands-on. Start from the Lab 3 starter (it already contains Lab 1 + Lab 2 code). Work from `%USERPROFILE%\MD287`. Start with `git pull` (Lab 3 Step 0). Do **not** clone, `mklink`, or `oc login`. Use **`curl.exe`**.

## Layout

```text
exercises/                   ← checkpoints 3.1–3.3
lab3/
  LAB-3-GUIDE.md
  tools/issue-jwt.py
  starter/account-service
  starter/transaction-service
```

| Resource | Path |
| -------- | ---- |
| Lab guide | [lab3/LAB-3-GUIDE.md](lab3/LAB-3-GUIDE.md) |
| JWT helper | [lab3/tools/issue-jwt.py](lab3/tools/issue-jwt.py) |
| Exercise 3.1 | [exercises/exercise-3.1-timeout-circuit-breaker.md](exercises/exercise-3.1-timeout-circuit-breaker.md) |
| Exercise 3.2 | [exercises/exercise-3.2-jwt-roles-scopes.md](exercises/exercise-3.2-jwt-roles-scopes.md) |
| Exercise 3.3 | [exercises/exercise-3.3-security-resilience-tests.md](exercises/exercise-3.3-security-resilience-tests.md) |
