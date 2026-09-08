---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 3.2: OA error body on login and refresh failures

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API caller,
I want `400`/`401` from `/login` and `/refresh` to use the OA envelope,
so that token-route failures match the grader contract.

## Acceptance Criteria

1. **Given** `POST /login` fails with bad structure
   **When** the API returns `400`
   **Then** the body is the OA envelope from Story 3.1 (not empty / ad-hoc)
   **And** `error_code` follows the brief-specific convention (e.g. `OA-400`) when Keycloak was not called or did not supply a code
   **And** `error_stack` is a JSON array of objects

2. **Given** `POST /login` fails with bad credentials
   **When** the API returns `401`
   **Then** the body is the OA envelope from Story 3.1
   **And** `error_code` relays Keycloak’s code when present

3. **Given** `POST /refresh` fails with bad structure
   **When** the API returns `400`
   **Then** the body is the OA envelope from Story 3.1
   **And** `error_stack` is a JSON array of objects

4. **Given** `POST /refresh` fails with invalid or expired `refresh_token`
   **When** the API returns `401`
   **Then** the body is the OA envelope from Story 3.1
   **And** `error_code` relays Keycloak’s code when present
   **And** `error_stack` includes the upstream/root cause as objects in the array

## Tasks / Subtasks

- [ ] Task 1 — Login errors → OA (AC: #1, #2)
  - [ ] Structure failures: `400` + `OA-400` (or documented equivalent)
  - [ ] Bad credentials: `401` + relay KC `error` (typically `invalid_grant`)
  - [ ] Success path **unchanged** (`201` + token fields)
- [ ] Task 2 — Refresh errors → OA (AC: #3, #4)
  - [ ] Same mapping; success remains `200`
  - [ ] 401 stack includes upstream KC error object(s)
- [ ] Task 3 — Tests
  - [ ] Update 2.1/2.2 tests that expected empty bodies
  - [ ] Assert envelope keys + `error_stack` is array of objects
- [ ] Task 4 — No new error family
  - [ ] Do not add `LOGIN_400` codes; reuse 3.1 README convention

## Dev Notes

### Scope (this story only)

**In scope:** wire existing `/login` and `/refresh` error paths to the 3.1 mapper.

**Out of scope:** users, roles, authz, compose, changing success payloads.

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| Login/refresh controllers + token client | **UPDATE** | Throw mapped exceptions |
| 3.1 mapper/filter | **REUSE** | Do not fork a second envelope |
| Specs | **UPDATE** | Envelope assertions |

### Testing requirements

- HTTP tests for all four AC rows. Mock KC for 401 cases with a body containing `error`.

### Anti-patterns (will fail review)

- Empty 400/401 after this story
- Success responses wrapped in OA
- New code family besides README convention


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

- [Source: `epics.md` — Story 3.2, FR14]
- [Source: `oauth-api.md` — Error body; CAP-1/CAP-7 error rows]
- [Source: Stories 2.1, 2.2, 3.1]

## Previous story intelligence

3.1 delivers mapper + README. 2.1/2.2 already have status codes. This story is adopt-only.

## Latest tech information

- Keycloak password/refresh failures commonly return HTTP 401 with `error=invalid_grant`. Relay that string as `error_code`.

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

