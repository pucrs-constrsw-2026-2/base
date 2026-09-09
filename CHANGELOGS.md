# Grupo 07 — Change logs (human + AI handoff)

Shared board for **what changed**, **who did it**, and **what the next person should know**.  
Every group member **and** every AI session that modifies the repo should append an entry here **before or with** the commit.

Related docs:

| Doc | Role |
| --- | --- |
| `_bmad-output/specs/spec-grupo07-keycloak-oauth/` | WHAT (API / Keycloak contract) |
| `_bmad-output/planning-artifacts/epics.md` | Story backlog |
| `_bmad-output/planning-artifacts/architecture-grupo07-keycloak-oauth.md` | HOW (Keycloak + oauth only) |
| `_bmad-output/implementation-artifacts/sprint-status.yaml` | Story status |
| `RESPONSIBILITIES.md` | Who implements which epics/stories (3 tracks) |
| `AI-CONTEXT-PROMPT.md` | Paste prompt for teammate AIs |
| This file (`CHANGELOGS.md`) | WHO did WHAT, when |

---

## How to use (required pattern)

### Rules

1. **Append only** — newest entry at the **top** of the “Log entries” section (below the template). Do not rewrite older entries except to fix your own typo.
2. **One entry per work session** (or per logical PR). If an AI continues someone else’s work, start a **new** entry and mention the previous one.
3. **Never paste secrets** (client secrets, passwords, tokens). Env **names** are OK.
4. Update **`sprint-status.yaml`** when story status changes; mention that in the entry.
5. Scope note: for now we track **Keycloak / oauth** work. Professors/Postgres can get its own section later.

### Template (copy this block)

```markdown
### YYYY-MM-DD — <short title>

| Field | Value |
| --- | --- |
| Author | <Name or "AI (Cursor) for <Name>"> |
| Branch | <base branch / oauth submodule branch> |
| Stories | <e.g. 1.1, 1.2 or "planning only"> |
| Status | <done / in progress / blocked> |

**Summary**
- <1–3 bullets: what landed>

**Paths touched**
- `path/to/file` — <why>

**Decisions / notes for the next person**
- <anything they must not redo or must verify>

**Next suggested step**
- <e.g. code-review 1.1; implement 1.2>
```

### Good vs bad

| Good | Bad |
| --- | --- |
| “Bound `KEYCLOAK_SERVER_URL` in Nest ConfigModule” | “Fixed stuff” |
| “Do not invent compose; professor file already enables oauth” | Pasting `.env` secret values |
| “Story 1.1 → review in sprint-status” | Editing someone else’s log entry silently |

---

## Log entries

### 2026-09-09 — Contract review against the T1 brief (authz matrix, login contract, user listing)

| Field | Value |
| --- | --- |
| Author | AI (Claude Code) for William Klein / Grupo 07 |
| Branch | `base` `grupo07` |
| Stories | 6.4, 6.5 (contract/AC correction — no implementation) |
| Status | done |

**Summary**
- Resolved the divergence flagged in Story 6.4 Dev Notes **in favour of the professor's `constrsw.json`**, not the Moodle brief. Test oracle for 6.5 is now the hierarchical matrix.
- Recorded the brief's original disjoint table as a historical note so the group can defend the decision if questioned.
- Marked the multi-policy `applyPolicies` as **intentional configuration** — previously an implementer could have read them as "gaps" and deleted them from the professor's realm.

**Paths touched**
- `_bmad-output/specs/spec-grupo07-keycloak-oauth/keycloak-authz.md` — real permission bindings, new § "Divergence from the T1 brief", corrected matrix quick check
- `_bmad-output/specs/spec-grupo07-keycloak-oauth/SPEC.md` — CAP-5, CAP-6, constraints, assumptions
- `_bmad-output/planning-artifacts/epics.md` — FR15, FR16, NFR9, Epic 6 preamble, Story 6.4/6.5 ACs
- `_bmad-output/implementation-artifacts/6-4-resource-based-permissions.md` — AC #1, Dev Notes, Task 4
- `_bmad-output/implementation-artifacts/6-5-validate-...-services.md` — AC #1, AC #6, Task 3
- `CHANGELOGS.md` — this entry

**Decisions / notes for the next person**
- **The realm is the oracle, not the brief.** Evidence: `constrsw.json` committed by the professor 2026-09-02 (`51423d2`) for this semester, alongside `10e6838` "Ajusta nome do serviço oauth" and `1791eab` the same day; the brief's screenshots still show realm `constr-sw-2022-2` / client `grupo1` (2022 re-upload).
- Verified directly in `constrsw.json`: all three permissions are `decisionStrategy: AFFIRMATIVE`; `coordinator-permissions` applies `coordinator-policy` + `administrator-policy`; `professor-permissions` applies professor + coordinator + administrator. Effective grants: administrator → all 8; coordinator → courses, classes, lessons, reservations; professor → lessons, reservations; student → none.
- **Do not remove those extra policies.** `SPEC.md` forbids editing the professor's realm, and doing so would be "fixing" his current config to match 2022 material.
- Story 6.5 code is unaffected — it delegates to Keycloak Authorization Services, so it is correct under either matrix. Only test expectations changed.
- ⚠️ **No implementation code exists yet.** Branch `grupo07` of submodule `backend/oauth` contains only `README.md` (1 file; other groups have 11–105). The 2026-09-08 entry below reports Story 1.1 as implemented and `sprint-status.yaml` still lists `1-1` as `ready-for-dev` — confirm with the author whether that Nest scaffold was ever committed before anyone redoes it.
- Question to the professor about this divergence was drafted but **not sent** — group chose to follow the realm without waiting.

**Login contract decisions (same review)**
- **Body:** the brief's form lists `client_id` and `grant_type`; the SPEC required username+password only. Now the API **accepts and ignores** those extra fields instead of rejecting them, so a brief-shaped request still succeeds. Credentials sent to Keycloak always come from env — a client-supplied value is never trusted, used, or logged. Story 2.1 explicitly forbids `forbidNonWhitelisted` on the login DTO.
- **Status code:** login now returns **`200`**, not `201`. The brief leaves this route's `Response codes` as literal `???`, nothing is created, and Keycloak itself answers 200. ⚠️ The earlier `201` came from group-supplied material — if Juliano has a professor source that specifies 201, this is a one-line revert in `SPEC.md` / `oauth-api.md` / story 2.1.
- **`GET /users` default:** with no query string the list now returns **enabled users only**, matching the brief's "todos os usuários cadastrados e habilitados". `?enabled=false` shows disabled ones.
- **Roles routes (Epic 5) kept as-is.** They are not in the T1 brief page we hold, but the brief says "no mínimo, as seguintes rotas", and they do not conflict with the professor's realm (the four client roles they manage are the ones `constrsw.json` ships). Flagged for Juliano to confirm the source.

**Next suggested step**
- Confirm the Story 1.1 code situation; fill owner names in `RESPONSIBILITIES.md`; then start Track A (1.1 → 1.2).

### 2026-09-08 — Responsibilities split (3 tracks) + AI context prompt

| Field | Value |
| --- | --- |
| Author | AI (Cursor) for EduardoArruda / Grupo 07 |
| Branch | `base` `grupo07` |
| Stories | Planning / process only (no new story implementation) |
| Status | done |

**Summary**
- Added `RESPONSIBILITIES.md`: Tracks A (Epics 1–3), B (Epic 4), C (Epics 5–6) for assigning 3 members.
- Added `AI-CONTEXT-PROMPT.md`: copy-paste prompt for other AIs (project, architecture, BMAD, changelogs, responsibilities).

**Paths touched**
- `RESPONSIBILITIES.md` — 3-way implementation ownership
- `AI-CONTEXT-PROMPT.md` — handoff prompt for teammate AIs
- `CHANGELOGS.md` — this entry

**Decisions / notes for the next person**
- Fill owner names in `RESPONSIBILITIES.md` before coding in parallel.
- Track A should stay ahead of B/C (OA mapper + Nest foundation).

**Next suggested step**
- Assign Track A/B/C owners; each opens a chat with `AI-CONTEXT-PROMPT.md` filled in.

### 2026-09-08 — BMAD planning + Story 1.1 scaffold + story batch + Keycloak architecture

| Field | Value |
| --- | --- |
| Author | AI (Cursor) for EduardoArruda / Grupo 07 |
| Branch | `base`: `grupo07` (+ professor `main` merge); `backend/oauth`: `grupo07` |
| Stories | Planning (SPEC/epics/sprint); Story **1.1** implemented (status **review**); Stories **1.2–6.5** story files only (**ready-for-dev**) |
| Status | Planning done; 1.1 awaiting code-review; implementation of 1.2+ not started in this session’s final state |

**Summary**
- Locked Keycloak/oauth-only SPEC (`spec-grupo07-keycloak-oauth`) — professors/Postgres deferred.
- Created epics (6) + 26 stories; sprint-status; merged professor `docker-compose.yml` / `.env` / `constrsw.json` from `main`.
- Aligned plan to professor stack: Story **1.3 = verify** (oauth already enabled); Epic **6.1–6.4 = verify-first** on import.
- Fixed `.env` usability: literal `KEYCLOAK_SERVER_URL=http://keycloak:8080`, `OAUTH_INTERNAL_HOST=oauth`.
- Implemented NestJS Story **1.1** (config + URL builder + tests + README env docs) in `backend/oauth`.
- Batch-created **25** remaining BMAD story markdown files under `_bmad-output/implementation-artifacts/` (no extra app code for 1.2–6.5).
- Added this `CHANGELOGS.md` and Keycloak-only architecture doc.

**Paths touched**
- `_bmad-output/specs/spec-grupo07-keycloak-oauth/` — SPEC + companions + memlog
- `_bmad-output/planning-artifacts/requirements-grupo07-t1-oauth-professors.md` — requirements pack
- `_bmad-output/planning-artifacts/epics.md` — epics/stories
- `_bmad-output/planning-artifacts/architecture-grupo07-keycloak-oauth.md` — Keycloak architecture (new)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — tracker
- `_bmad-output/implementation-artifacts/1-1-*.md` … `6-5-*.md` — story files
- `backend/oauth/` — Nest scaffold + Keycloak settings (Story 1.1)
- `.env` — `KEYCLOAK_SERVER_URL` / `OAUTH_INTERNAL_HOST` fixes (no secret rotation)
- `CHANGELOGS.md` — this file

**Decisions / notes for the next person**
- Contract: SPEC + `oauth-api.md` + `keycloak-authz.md` win over Keycloak README JSON login curls (T1 = form-data, login **201**, field `referesh_expires_in`).
- Do **not** invent root compose/`.env`; do **not** use divergent `backend/oauth/keycloak/realm-export.json`.
- Canonical realm import: `infrastructure/dev.local/services/keycloak/constrsw.json`.
- Faster implementation: epic-sized chats OK; story files already exist — no more create-story required for 1.2–6.5.
- Next BMAD step for 1.1: `bmad-code-review` → mark **done**, then implement **1.2** (Dockerfile + `GET /health`).

**Next suggested step**
- Code-review Story 1.1; then implement Epic 1 remaining (1.2 → 1.3) from existing story files.

---
