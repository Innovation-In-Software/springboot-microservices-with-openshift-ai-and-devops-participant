# MD287 classroom lab access

**Course:** BAC Virtual MD287 · 14–18 September 2026  
**Instructor:** Gurinderjeet Kaur (Dr. G)

Find **your name** in the table. Use that Ablaze login all five days. OpenShift is **Day 4–5 only**.

| Item | Value |
| ---- | ----- |
| Ablaze portal | https://my.ablazedesktop.com |
| Ablaze logins | `MSMICR26-01` … `MSMICR26-25` (one per person) |
| Ablaze password (every seat) | `TEKmsmicr26!` |

---

## 1. How to log on (Day 1)

1. Open **https://my.ablazedesktop.com**
2. Username = the **Ablaze login** next to your name (example: `MSMICR26-01`)
3. Password = `TEKmsmicr26!` (same for every seat)
4. Wait for the Windows desktop (`student.VLAB`)
5. Clone this participant repo **on the VM** and work in each lab `starter/` folder:

```powershell
git clone https://github.com/Innovation-In-Software/springboot-microservices-with-openshift-ai-and-devops-participant.git MD287
cd MD287
```

One person per login. Two people on the same Ablaze account will kick each other off.

---

## 2. Ablaze + OpenShift assignment

Shared OpenShift password for every user below: `Tr@ininG2026`

| # | Name | Ablaze login | OpenShift user | OpenShift project |
| - | ---- | ------------ | -------------- | ----------------- |
| 1 | Amit Pathak | `MSMICR26-01` | `student01` | `md287-student01` |
| 2 | Carlos Rivera | `MSMICR26-02` | `student02` | `md287-student02` |
| 3 | Chitra Sasidharan | `MSMICR26-03` | `student03` | `md287-student03` |
| 4 | Damian Jerez | `MSMICR26-04` | `student04` | `md287-student04` |
| 5 | Donald Nigro | `MSMICR26-05` | `student05` | `md287-student05` |
| 6 | Ehsan Khan | `MSMICR26-06` | `student06` | `md287-student06` |
| 7 | Himabindu Araveeti | `MSMICR26-07` | `student07` | `md287-student07` |
| 8 | Jang (Jo) Yang | `MSMICR26-08` | `student08` | `md287-student08` |
| 9 | Joy Ding | `MSMICR26-09` | `student09` | `md287-student09` |
| 10 | Krishna Chaitanya Pulagam | `MSMICR26-10` | `student10` | `md287-student10` |
| 11 | Manojna Devisetty | `MSMICR26-11` | `student11` | `md287-student11` |
| 12 | Navya Pingili | `MSMICR26-12` | `student12` | `md287-student12` |
| 13 | Pradyothana Surireddy | `MSMICR26-13` | `student13` | `md287-student13` |
| 14 | Purva Rajwade | `MSMICR26-14` | `student14` | `md287-student14` |
| 15 | Ranjit Gampa | `MSMICR26-15` | `student15` | `md287-student15` |
| 16 | Rekha Mishra | `MSMICR26-16` | `student16` | `md287-student16` |
| 17 | Richard Yao | `MSMICR26-17` | `student17` | `md287-student17` |
| 18 | Ron Wiggins | `MSMICR26-18` | `student18` | `md287-student18` |
| 19 | Shireesha Rachakonda | `MSMICR26-19` | `student19` | `md287-student19` |
| 20 | Sridevi Neelam | `MSMICR26-20` | `student20` | `md287-student20` |
| 21 | Sundari Lakshminarayanan | `MSMICR26-21` | `student21` | `md287-student21` |
| 22 | Swapna Kethireddy | `MSMICR26-22` | `student22` | `md287-student22` |
| 23 | Thanga Velayutham | `MSMICR26-23` | `student23` | `md287-student23` |
| 24 | Waqas Shahid | `MSMICR26-24` | `student24` | `md287-student24` |
| 25 | Yiqun Chen | `MSMICR26-25` | `student25` | `md287-student25` |

---

## 3. OpenShift (Days 4–5)

Same cluster for the whole class. Do **not** run `oc login` on Days 1–3.

| Field | Value |
| ----- | ----- |
| API server | `https://api.aro-md287.centralus.aroapp.io:6443/` |
| Console | `https://console-openshift-console.apps.aro-md287.centralus.aroapp.io/` |
| Registry | `default-route-openshift-image-registry.apps.aro-md287.centralus.aroapp.io` |
| Model Route | `http://md287-risk-model-<project>.apps.aro-md287.centralus.aroapp.io` |

Example for Amit Pathak (`student01`):

```powershell
oc login https://api.aro-md287.centralus.aroapp.io:6443/ --username student01 --password "Tr@ininG2026"
oc project md287-student01
```

Model Route for that project:

`http://md287-risk-model-md287-student01.apps.aro-md287.centralus.aroapp.io`

Cluster teardown is **19 September 2026**. Do not stop or deallocate ARO VMs during class.
