# AI context prompt — Grupo 07 (Keycloak / oauth)

**Copy everything below the line into a new Agent chat** when a group member starts work with another AI.  
Do not invent a second plan — load these files and implement the assigned track.

---

```text
You are helping ConstrSW 2026/2 Grupo 07 on the Keycloak oauth gateway.

## Project context
- Monorepo `base` (branch grupo07) with submodule `backend/oauth` (branch grupo07).
- Professor already provided root `docker-compose.yml` + `.env` + Keycloak realm import.
- Keycloak: realm `constrsw`, client `oauth`, image ~26.0.1 under `infrastructure/dev.local/services/keycloak/`.
- Console http://localhost:8081; oauth API external port 8181; create volume `constrsw-keycloak-data` before compose up.
- Our job: NestJS REST API in `backend/oauth` that consumes Keycloak (login, refresh, users, roles, authz validate, OA errors).
- Out of scope for now: professors domain, PostgreSQL, Astah, inventing compose/.env, full course mesh APIs.

## Architecture (read this)
- `_bmad-output/planning-artifacts/architecture-grupo07-keycloak-oauth.md`
Key points:
- NestJS 11 + npm; ConfigModule with ignoreEnvFile: true.
- Env: KEYCLOAK_SERVER_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET, KEYCLOAK_ADMIN*, OAUTH_INTERNAL_API_PORT.
- URL rule: {KEYCLOAK_SERVER_URL}/realms/{realm}/… — never insert /auth.
- Compose already enables service `oauth`; healthcheck is node → GET /health.
- Canonical realm: infrastructure/dev.local/services/keycloak/constrsw.json — NOT backend/oauth/keycloak/realm-export.json.

## Contract / BMAD (already done — do not redo planning)
- SPEC: `_bmad-output/specs/spec-grupo07-keycloak-oauth/SPEC.md` + `oauth-api.md` + `keycloak-authz.md`
- Epics: `_bmad-output/planning-artifacts/epics.md` (6 epics, 26 stories)
- Story files: `_bmad-output/implementation-artifacts/<story-key>.md` (all ready-for-dev except 1.1 may be review/done)
- Sprint: `_bmad-output/implementation-artifacts/sprint-status.yaml`
- BMAD Method was used for SPEC → epics → sprint → story files. Implementation is next. Prefer implementing from existing story files; do not recreate epics/SPEC unless asked.
- T1 rules that win over Keycloak README curls: login form-data (not JSON), success 201, field spelling referesh_expires_in, OA error envelope for failures after Epic 3.

## Team process files (must follow)
1. CHANGELOGS.md (repo root) — after your session, APPEND a new entry at the TOP using the template in that file. No secrets.
2. RESPONSIBILITIES.md (repo root) — implementation split into 3 tracks:
   - Track A: Epics 1–3 (foundation: Docker/health, login/refresh, OA errors)
   - Track B: Epic 4 (users + Bearer guard)
   - Track C: Epics 5–6 (roles + authz verify + POST /authz/validate)
3. Update sprint-status.yaml when story status changes.

## What I want you to do in THIS chat
- Owner track: <A | B | C>   (fill before sending)
- Member name: <name>
- Implement only stories in that track, in numeric order, using the matching story .md files.
- Work inside backend/oauth on grupo07. Do not invent root docker-compose.yml or .env.
- Do not implement other tracks’ stories unless blocked and the user explicitly expands scope.
- When finishing a story: leave checklist updates in the story file if appropriate, set sprint-status, and remind me to add a CHANGELOGS.md entry (or write it).

## Hard MUST NOTs
- No professors/Postgres work
- No committing secrets
- No promoting divergent realm-export.json
- No local role-matrix for authz (Keycloak Authorization Services is the engine; B.2 matrix is test oracle only)
```

---

## How members should use this

1. Open `AI-CONTEXT-PROMPT.md`.  
2. Copy the fenced prompt.  
3. Fill `Track` and `Member name`.  
4. Paste into a **new** Agent chat.  
5. Point at the next story file if useful, e.g. “Start with story 4.1”.
