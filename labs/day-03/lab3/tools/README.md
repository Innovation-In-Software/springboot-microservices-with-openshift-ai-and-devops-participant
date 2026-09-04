# Lab 3 JWT helper

Classroom-only HS256 tokens. The secret matches `md287.jwt.secret` in both services.

```powershell
python issue-jwt.py teller
python issue-jwt.py ops
python issue-jwt.py readonly
```

Paste the single line into `$TELLER` / `$OPS`. Do not commit real bank tokens. This secret is not for production.
