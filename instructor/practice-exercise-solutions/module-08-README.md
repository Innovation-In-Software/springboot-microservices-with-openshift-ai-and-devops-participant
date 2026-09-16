# Module 8 — Practice exercise solutions

**Deck:** `decks/pptx_new/MD287_Module08_Containerization_with_OpenShift.pptx`  
Attempt the slide activity first, then compare your answers here.

Official numbered checkpoints in this deck: [Exercise 4.2](../../labs/day-04/exercises/exercise-4.2-containerize-account.md) and [Exercise 4.1](../../labs/day-04/exercises/exercise-4.1-probes-correlation.md).

---

## Exercise: Review a Containerfile

**Time:** 10 minutes

### Scenario

```dockerfile
FROM maven:latest
COPY . /app
WORKDIR /app
ENV DB_PASSWORD=Secret123
RUN mvn package
EXPOSE 8080
CMD mvn spring-boot:run
```

### Solution

| Problem | Risk | Improved approach |
| --- | --- | --- |
| `maven:latest` | Unreproducible; surprise CVEs | Pin a version (and digest in prod) |
| Maven + source in the runtime image | Huge image, extra attack surface | **Multi-stage**: build in Maven, runtime JRE/UBI only |
| `ENV DB_PASSWORD=...` | Secret in **image history** | OpenShift Secret at **runtime** |
| Runs as root (default) | Container breakout blast radius | `USER 1001` (or the course `md287` / `runAsUser: 100` pattern) |
| `COPY .` first | Any file change busts the dependency cache | Copy `pom.xml` (and wrappers), `mvn dependency:go-offline`, then copy sources |
| `CMD mvn spring-boot:run` | Recompiles at startup; still needs Maven | `java -jar` the packaged JAR |
| No `.dockerignore` implied | Secrets and `target/` copied in | Ignore build junk and local env files |

Sketch:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd -u 1001 app
USER 1001
COPY --from=build /app/target/order-service.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Password still comes from the platform, not the file.

---

## Exercise: Why Can't Users Reach the API?

**Time:** 10 minutes

### Scenario

Account Service is deployed. Users error on the Route URL.

```
$ oc get pods
account-service-7c9d  0/1  Running  0  4m

$ oc get endpoints account-service
account-service   <none>
```

### Solution — check in order

1. **Route** → 2. **Service** → 3. **Endpoints** → 4. **Pod readiness** → 5. **Events** (`oc describe pod`) → 6. **Logs**.

| Clue | Meaning |
| --- | --- |
| `0/1` Running | Container process is up but **not Ready**. Probes are failing. |
| Endpoints `<none>` | Service has **nowhere to send traffic**. Route will fail even if DNS looks fine. |

**Likely causes**

1. **Readiness probe failing** — bad DB URL, missing Secret, app not listening on the probed port. Fix config; confirm `/actuator/health` (or the lab probe path) returns 200.  
2. **Service selector ≠ pod labels** — Service never selects the pod. Align labels.

Confirm with `oc describe pod` (probe failures in Events) and `oc logs` (the exception).

### Why this is the answer

**Running is not Ready.** Follow the traffic path; do not restart the pod as the first move.
