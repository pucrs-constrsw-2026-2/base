---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 4.4: Update user attributes

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an authenticated caller,
I want to `PUT /users/{{id}}` with updated attributes,
so that the Keycloak user is replaced/updated.

## Acceptance Criteria

1. **Given** a valid Bearer token and an existing user id
   **When** the caller `PUT /users/{{id}}` with allowed attributes `first-name`, `last-name`, and/or `enabled`
   **Then** the API returns `200` with an empty body
   **And** `username` is immutable via PUT (email-as-username identity)
   **And** the oauth README lists allowed PUT attributes and states that `username` is not updatable on this route

2. **Given** the id does not exist
   **When** the caller `PUT /users/{{id}}`
   **Then** the API returns `404` with the OA envelope

3. **Given** the body is invalid or includes disallowed fields (including a `username` change)
   **When** the caller `PUT /users/{{id}}`
   **Then** the API returns `400` with the OA envelope

4. **Given** missing/invalid token or insufficient permission
   **When** the caller `PUT /users/{{id}}`
   **Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

## Tasks / Subtasks

- [ ] Task 1 — `PUT /users/:id` (AC: #1)
  - [ ] Allowed: `first-name`, `last-name`, `enabled` only
  - [ ] Fetch existing user, apply allowed fields, `PUT` Admin representation (keep username/email unchanged)
  - [ ] Success `200` **empty body**
  - [ ] README: allowed attributes + username immutable
- [ ] Task 2 — Errors (AC: #2, #3, #4)
  - [ ] Unknown id → 404 OA; `username` present / extra fields / invalid types → 400 OA
- [ ] Task 3 — Tests including “username in body → 400”
- [ ] Do **not** implement password PATCH (4.5) or DELETE (4.6)

## Dev Notes

### Scope (this story only)

**In scope:** attribute PUT only (not password).

Bearer rules (pin in 4.1, reuse later): missing/invalid access token → `401` OA; valid token that Keycloak or the API rejects for permission → `403` OA. Validate the caller token via UserInfo (`urls.userInfoUrl`) or token introspection — do **not** invent JWKS env names. Admin REST calls use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (typically `admin-cli` against realm `master`) plus `adminRealmUrl` — never the caller’s password. JSON fields from the brief use hyphens: `first-name`, `last-name`.

Password changes are Story 4.5 (`PATCH` with `{ "password": "..." }`). Do not accept `password` on PUT.

### Anti-patterns

- Allowing username rename. Returning the user object on 200. Hard-delete via enabled=false confusion (disable is 4.6 DELETE).


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

- [Source: `epics.md` — Story 4.4, FR7, FR10, NFR2]
- [Source: `oauth-api.md` — PUT /users/{{id}}]
- [Source: Story 4.1 Bearer; 4.3 get]

## Previous story intelligence

GET-by-id (4.3) needed to load current Keycloak user before PUT merge.

## Latest tech information

- Admin `PUT /admin/realms/{realm}/users/{id}` replaces representation — send a full user object, not a JSON-patch.

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

