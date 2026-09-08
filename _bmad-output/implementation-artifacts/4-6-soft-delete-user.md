---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 4.6: Soft-delete user

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an authenticated caller,
I want to `DELETE /users/{{id}}`,
so that the user is disabled in Keycloak rather than hard-deleted.

## Acceptance Criteria

1. **Given** a valid Bearer token and an existing enabled user
   **When** the caller `DELETE /users/{{id}}`
   **Then** the API disables the user in Keycloak (does not hard-delete)
   **And** the API returns `204` with an empty body

2. **Given** a valid Bearer token and a user who is already disabled
   **When** the caller `DELETE /users/{{id}}`
   **Then** the API returns `204` (idempotent: user remains disabled; never hard-deleted)

3. **Given** the id does not exist
   **When** the caller `DELETE /users/{{id}}`
   **Then** the API returns `404` with the OA envelope

4. **Given** a malformed id
   **When** the caller `DELETE /users/{{id}}`
   **Then** the API returns `400` with the OA envelope

5. **Given** missing/invalid token or insufficient permission
   **When** the caller `DELETE /users/{{id}}`
   **Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

## Tasks / Subtasks

- [ ] Task 1 — Soft delete (AC: #1, #2)
  - [ ] `PUT` user with `enabled: false` (or equivalent). **Never** `DELETE /admin/realms/.../users/{id}`
  - [ ] Already disabled → still `204`
  - [ ] HTTP **`204` empty body**
- [ ] Task 2 — 404 / 400 / auth (AC: #3–#5)
- [ ] Task 3 — Tests: enabled→disabled; already disabled; missing; malformed; 401/403
- [ ] README: DELETE = disable (NFR7)

## Dev Notes

### Scope (this story only)

**In scope:** disable-on-DELETE only.

Bearer rules (pin in 4.1, reuse later): missing/invalid access token → `401` OA; valid token that Keycloak or the API rejects for permission → `403` OA. Validate the caller token via UserInfo (`urls.userInfoUrl`) or token introspection — do **not** invent JWKS env names. Admin REST calls use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (typically `admin-cli` against realm `master`) plus `adminRealmUrl` — never the caller’s password. JSON fields from the brief use hyphens: `first-name`, `last-name`.

**Out of scope:** role logical-delete (5.6), hard-delete, compose.

### Anti-patterns

- Keycloak hard-delete. `200` with body. Treating already-disabled as 404.


### Canonical contract

Load-bearing sources (do not treat architecture spine or Keycloak README curls as the API contract):

- `_bmad-output/specs/spec-grupo07-keycloak-oauth/SPEC.md`
- `_bmad-output/specs/spec-grupo07-keycloak-oauth/oauth-api.md`
- `_bmad-output/specs/spec-grupo07-keycloak-oauth/keycloak-authz.md`
- `_bmad-output/planning-artifacts/epics.md` (this story’s ACs)

Spine artifacts (`closed-cras`, `KEYCLOAK_JWKS_URL`, `POST /v1/auth/login`, client `bff`, `backend/oauth` as “catalog/faixas”) are **out of scope**. Professor env names win.

### Implementation target

- Submodule `backend/oauth` on branch `grupo07`. NestJS **11.x** + npm (Story 1.1). Work **inside** the submodule; do not implement in the parent repo root.
- Reuse `KeycloakSettingsService` / `buildKeycloakUrls` — do not scatter `process.env` or invent `KEYCLOAK_URL` / `KEYCLOAK_BASE_URL` / `KEYCLOAK_JWKS_URL`.
- **MUST NOT** invent, edit, uncomment, or replace root `docker-compose.yml` or root `.env`.
- **MUST NOT** use or commit `backend/oauth/keycloak/realm-export.json` (diverges: `funcionario`/`coordenador`, authz off). Canonical import is professor `infrastructure/dev.local/services/keycloak/constrsw.json`.

### Professor stack (already present)

Root compose **already enables** service `oauth` (`build.context: ./backend/oauth`, `dockerfile: Dockerfile`, image `constrsw/oauth`, network `constrsw`, `depends_on: keycloak` healthy).

Injected into the oauth container:

`KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `OAUTH_INTERNAL_PROTOCOL`, `OAUTH_INTERNAL_HOST`, `OAUTH_INTERNAL_API_PORT`, `OAUTH_INTERNAL_DEBUG_PORT`, `OAUTH_INTERNAL_METRICS_PORT`.

Verified names (never copy secret **values** into code/README):

- `KEYCLOAK_SERVER_URL=http://keycloak:8080` (no `/auth`)
- `KEYCLOAK_REALM=constrsw`, `KEYCLOAK_CLIENT_ID=oauth`
- External API `OAUTH_EXTERNAL_API_PORT` → **8181**; internal listen `OAUTH_INTERNAL_API_PORT` → **3001**
- Keycloak image **26.0.1**, console `:8081`, volume `constrsw-keycloak-data`, import `constrsw.json`

Not injected into oauth (do not require): `KEYCLOAK_GRANT_TYPE`, `KEYCLOAK_TOKEN_ALGORITHM`, `KEYCLOAK_INTERNAL_*`, `KEYCLOAK_EXTERNAL_*`. Grants are SPEC-fixed (`password` / `refresh_token`).

### Architecture compliance

Canonical contract: `_bmad-output/specs/spec-grupo07-keycloak-oauth/` (`SPEC.md`, `oauth-api.md`, `keycloak-authz.md`). NFR1 (implement in `backend/oauth`), NFR2 (realm/client defaults), NFR3 (`/auth` rule), NFR12 (do not invent compose/.env), NFR13 (credentials from provided `.env`, Keycloak 26.x).

### Project Structure Notes

- Parent repo: `constrsw-2026-2` branch `grupo07`. Implementation lives in submodule `backend/oauth` (remote `oauth.git`, branch `grupo07`).
- Professor stack already on this tree: root `docker-compose.yml`, root `.env`, `infrastructure/dev.local/services/keycloak/`.
- Story 1.1 (status `review`) already: Nest 11 scaffold, `ConfigModule.forRoot({ isGlobal: true, ignoreEnvFile: true, load: [keycloakConfig] })`, `KeycloakSettingsService`, URL builder (`tokenUrl`, `userInfoUrl`, `adminRealmUrl`). `src/main.ts` bootstraps Nest but **does not listen**. No Dockerfile, no `GET /health`, no login.


### References

- [Source: `epics.md` — Story 4.6, FR9, FR10, NFR7]
- [Source: `oauth-api.md` — DELETE /users/{{id}}]
- [Source: `SPEC.md` — Constraints soft-delete]

## Previous story intelligence

Reuse 4.3 get + 4.4 PUT enabled. NFR7.

## Latest tech information

- Soft-delete is disable; Admin DELETE user is hard-delete — forbidden here.

## Git intelligence summary

Parent HEAD `1791eab` (`Ajusta compose`) — professor stack, not oauth app code. Submodule HEAD `48b1723` (`feat: setando e testando branch`) — README stub at commit; Story 1.1 lives in the working tree. Do not “match” other remotes that use `KEYCLOAK_URL`.

## Project context reference

No `project-context.md` in this repo. Follow SPEC + epics + this story.

## Story completion status

Ultimate context engine analysis completed — comprehensive developer guide created.

**Status:** ready-for-dev

### Discovery (create-story)

- Loaded `{epics_content}` from `_bmad-output/planning-artifacts/epics.md`
- Loaded contract from `_bmad-output/specs/spec-grupo07-keycloak-oauth/` (SPEC.md, oauth-api.md, keycloak-authz.md)
- Architecture spine present but **out of scope** — negative constraint only
- No UX requirements (HTTP API)
- No `project-context.md`
- Previous story intelligence: Story 1.1 file (review) plus later stories in this batch as listed in Dev Notes

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

