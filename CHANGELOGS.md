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

### 2026-09-09 — Restrict Roles operations to administrator

| Field | Value |
| --- | --- |
| Author | Gabriel Hoppe with AI |
| Branch | `grupo07-feat/roles-attributions` |
| Stories | Review follow-up for 5.1–5.8 |
| Status | in progress; commit/story separation still deferred |

**Summary**
- Added an authorization guard requiring the `administrator` role for every Roles and user-role assignment route.
- Users without that role now receive `403` through the global OA exception filter.
- Added unit coverage for administrator access and denied non-administrator access.

**Paths touched**
- `backend/oauth/src/common/administrator-role.guard.ts` — administrator authorization rule.
- `backend/oauth/src/roles/roles.controller.ts` — apply authentication and administrator authorization guards.
- `backend/oauth/README.md` — document the administrator-only rule.

**Decisions / notes for the next person**
- The caller authorization decision is now resolved: only `administrator` may manage roles and assignments.
- The service still uses the configured admin credentials for Keycloak Admin API calls after the caller passes both guards.
- `npm test -- --runInBand` passes with 70 tests and `npm run build` passes.

**Next suggested step**
- Re-run code review, then decide whether to address the deferred branch/commit separation.

### 2026-09-09 — Review fixes for Roles & Atribuições

| Field | Value |
| --- | --- |
| Author | Gabriel Hoppe with AI |
| Branch | `grupo07-feat/roles-attributions` |
| Stories | Review follow-up for 1.2, 1.3, 5.1–5.8 |
| Status | in progress; authorization decision intentionally open |

**Summary**
- Added the global OA exception filter and expanded tests for validation, upstream failures, malformed Keycloak responses, and role mappings.
- Separated PUT replacement semantics from PATCH merge semantics and added timeout/network handling for the Keycloak Admin API.
- Kept caller authorization unchanged for now, as explicitly decided in the session; story/commit separation is also deferred.

**Paths touched**
- `backend/oauth/src/errors/` — global OA error filter and tests.
- `backend/oauth/src/roles/` — Admin API resilience, PUT/PATCH semantics, and tests.
- `backend/oauth/src/app.module.ts` — register the global filter.
- `backend/oauth/README.md` — document PUT/PATCH and OA envelope behavior.

**Decisions / notes for the next person**
- Point 1 from the review, caller authorization before admin-backed role operations, remains intentionally unresolved and must not be treated as fixed.
- Point 7, separate branch/commit per story, remains deferred for now.
- `npm test -- --runInBand` passes with 68 tests and `npm run build` passes.

**Next suggested step**
- Re-run code review focusing on the remaining authorization decision and live Keycloak role-operation behavior.

### 2026-09-09 — Story 1.3 compose verification

| Field | Value |
| --- | --- |
| Author | Gabriel Hoppe with AI |
| Branch | `grupo07-feat/roles-attributions` |
| Stories | 1.3 |
| Status | review |

**Summary**
- Built the oauth image through the professor compose and started `keycloak` plus `oauth` without changing compose or environment files.
- Confirmed the oauth container is healthy and `GET http://localhost:8181/health` returns `{"status":"ok"}`.
- Confirmed `keycloak` resolves inside the oauth container and `KEYCLOAK_SERVER_URL` is injected as `http://keycloak:8080`.

**Paths touched**
- `backend/oauth/README.md` — compose volume, console/API URLs, and import/login contract notes.
- `_bmad-output/implementation-artifacts/1-3-verify-oauth-runs-on-professor-provided-compose.md` — verification checklist and status.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — Story 1.3 moved to `review`.

**Decisions / notes for the next person**
- Docker Desktop is running; image build and compose verification passed.
- The external volume `constrsw-keycloak-data` is present. Keycloak import behavior depends on whether the realm already exists in that volume.

**Next suggested step**
- Run code review for Stories 1.2, 1.3, and 5.1–5.8.

### 2026-09-09 — Roles & Atribuições implementation

| Field | Value |
| --- | --- |
| Author | Gabriel Hoppe with AI |
| Branch | `grupo07-feat/roles-attributions` |
| Stories | 1.2, 5.1–5.8 |
| Status | review; 1.3 pending Docker verification |

**Summary**
- Implemented the Roles & Atribuições module against the Keycloak Admin API: client-role CRUD, logical delete, and user assignment/removal.
- Added the documented health endpoint and Dockerfile required by the PDF track.
- Reused the published shared Bearer guard; the professor compose, `.env`, and realm were not changed.

**Paths touched**
- `backend/oauth/src/roles/` — role routes, validation, Keycloak Admin API integration, and tests.
- `backend/oauth/src/health/` — `GET /health` and HTTP test.
- `backend/oauth/Dockerfile`, `backend/oauth/.dockerignore` — image build inputs.
- `backend/oauth/README.md` — role representation, route pinning, canonical names, and inactive flag.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — stories moved to `review`; Epic 5 moved to `in-progress`.

**Decisions / notes for the next person**
- Roles are always client roles for the configured `oauth` client; realm roles are rejected.
- Logical delete sets `attributes.inactive=true` and never calls Keycloak DELETE.
- Assign is pinned to `POST /users/:id/roles`; unassign is `DELETE /users/:id/roles/:roleId`.
- `npm test -- --runInBand` passes with 62 tests and `npm run build` passes. Docker build could not run because the local Docker daemon is unavailable.

**Next suggested step**
- Start Docker and execute Story 1.3 against the professor compose, then run code review for the Roles stories.

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
