# Lab 5 tools

| File | Purpose |
| --- | --- |
| `issue-jwt.py` | Classroom JWTs (`ops`, `reviewer`, `readonly`, `teller`) |
| `mock-model.py` | Stand-in OpenShift AI score endpoint on port **8090** |
| `publish-event.ps1` | Pipe a JSON file into the Lab 5 Kafka container |
| `events/` | Synthetic `TransactionSubmitted` messages |

JWT helper from Lab 3 still works for Account/Transaction. Use **this** copy when you need `risk.read` / `risk.write`.
