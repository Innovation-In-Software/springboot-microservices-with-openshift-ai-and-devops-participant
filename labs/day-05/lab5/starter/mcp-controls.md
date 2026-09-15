# Exercise 5.3 — MCP security controls (starter)

**Module 11** (MCP and Microservices) · Day 5 · **Checkpoint C**

This starter table is **already filled**. Walk each row in class. Compare with `../../solution/mcp-controls.md` when the instructor says to. Do **not** start an MCP server, Keycloak, or a Microsoft agent runtime.

If an assistant called Risk Assessment through MCP tools `get_assessment` and `submit_review`, write one control for each:

| Concern | Control you would require |
| --- | --- |
| Authentication | MCP session carries the **user** JWT (or a tightly scoped service token). No anonymous tool calls. |
| Authorization / scopes | `get_assessment` requires `risk.read`. `submit_review` requires `risk.write`. The MCP server must not use a superuser token. |
| Human-in-the-loop | `submit_review` is not auto-invoked. A human confirms HOLD → APPROVE/DECLINE. The model never posts review by itself. |
| Audit | Log tool name, synthetic `transactionId`, `correlationId`, caller subject, and outcome. Never log Bearer tokens, API keys, or PAN. |
| Data minimization | Tools return disposition, score, and policy reason — not raw prompts, full account dumps, or secrets. |

Do not expose `review` as an unattended tool. Compare with `../../solution/mcp-controls.md` when the instructor says to.
