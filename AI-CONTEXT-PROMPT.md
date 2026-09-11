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
- STATUS: all 26 stories are IMPLEMENTED and merged into grupo07; 254 tests pass. What is left is verifying against a RUNNING Keycloak (story 1.3) — nothing has touched a live one yet.
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
- Story files: `_bmad-output/implementation-artifacts/<story-key>.md` (all at `review`: merged, not yet formally code-reviewed)
- Sprint: `_bmad-output/implementation-artifacts/sprint-status.yaml`
- BMAD Method was used for SPEC → epics → sprint → story files. Implementation is DONE; do not recreate epics/SPEC or re-implement a story unless asked.
- T1 rules that win over Keycloak README curls: login form-data (not JSON), success 200, field spelling referesh_expires_in, OA error envelope for failures after Epic 3.
- Login accepts extra form fields client_id/grant_type (brief lists them) but ignores them; credentials always from env.

## Team process files (must follow)
1. CHANGELOGS.md (repo root) — after your session, APPEND a new entry at the TOP using the template in that file. No secrets.
2. RESPONSIBILITIES.md (repo root) — owners and what each delivered:
   - Juliano Chies: planning (SPEC, epics, story files)
   - William Klein: config foundation, Bearer guard, shared Admin client, Epic 4 (users)
   - Gabriel Hoppe: Dockerfile, /health, Epic 5 (roles)
   - Leonardo Gemin: Epics 2, 3, 6 (login/refresh, OA errors, authz validate)
3. Update sprint-status.yaml when story status changes.
4. After merging inside backend/oauth, update the submodule pointer in the base repo — merging there does NOT move it, and a stale pointer builds an image from old code.

## Code conventions you must follow
- Inject KeycloakSettingsService; never read process.env in a feature module. The /auth URL rule lives there.
- Inject KeycloakAdminClient (src/common) for Admin API work; do not write a second one. It owns the admin token, caching, timeout and failure mapping.
- Errors use the shared OA envelope from src/errors; error_stack redacts tokens and passwords.
- An unreachable Keycloak answers 503, not 401, everywhere.
- Tests must assert the status, not merely that something was thrown. `rejects.toMatchObject({})` matches ANY object and once hid a broken assertion across eighteen cases.
- Integration tests must call invalidateToken() on KeycloakAdminClient between cases, or a cached token leaks across them.

## What I want you to do in THIS chat
- Member name: <name>
- Task: <fill in before sending>
- Work inside backend/oauth on grupo07, on a branch named grupo07-feat/<name> or grupo07-fix/<name>.
- Do not invent root docker-compose.yml or .env.
- When finishing: update sprint-status if a story changed state, add a CHANGELOGS.md entry, and update the submodule pointer if the oauth branch moved.

## Hard MUST NOTs
- No professors/Postgres work
- No committing secrets
- No promoting divergent realm-export.json
- No local role-matrix for authz (Keycloak Authorization Services is the engine). The realm's matrix is HIERARCHICAL — administrator reaches all eight resources, coordinator four, professor two, student none — and differs from the brief's table. The realm wins; see keycloak-authz.md.
```

---

## How members should use this

1. Open `AI-CONTEXT-PROMPT.md`.  
2. Copy the fenced prompt.  
3. Fill `Member name` and `Task`.  
4. Paste into a **new** Agent chat.  
5. Point at the next story file if useful, e.g. “Start with story 4.1”.
