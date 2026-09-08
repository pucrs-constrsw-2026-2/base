---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 2.2: Refresh tokens without re-entering password

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API caller,
I want to `POST /refresh` with a valid `refresh_token`,
so that I can obtain new tokens without sending my password again.

## Acceptance Criteria

1. **Given** a valid `refresh_token` issued by Keycloak for client `oauth`
   **When** the caller `POST /refresh` with `refresh_token` as `multipart/form-data` **or** `application/x-www-form-urlencoded`
   **Then** the API adds `client_id`, `client_secret`, and `grant_type=refresh_token` from env and posts to the same Keycloak token endpoint as login
   **And** the response is HTTP `200` with the same token field set as login
   **And** `referesh_expires_in` is mapped from Keycloak’s `refresh_expires_in` when Keycloak returns refresh expiry (NFR5)

2. **Given** the request is missing `refresh_token` or is not valid form-data / urlencoded
   **When** the caller `POST /refresh`
   **Then** the API returns `400`
   **And** the body may be simple or empty (OA envelope is Story 3.x)

3. **Given** the `refresh_token` is invalid or expired
   **When** the caller `POST /refresh`
   **Then** the API returns `401`
   **And** the body may be simple or empty (OA envelope is Story 3.x)

## Tasks / Subtasks

- [ ] Task 1 — `POST /refresh` (AC: #1)
  - [ ] Path **`POST /refresh`** (SPEC CAP-7 default). Not `/auth/refresh` from Keycloak README
  - [ ] Accept multipart **or** urlencoded; field `refresh_token` only from caller
  - [ ] Success HTTP **`200`** (distinct from login `201`)
- [ ] Task 2 — Reuse login token client (AC: #1)
  - [ ] Same `tokenUrl`; `grant_type=refresh_token`; add client id/secret from `KeycloakSettingsService`
  - [ ] Same response mapping as login (`referesh_expires_in` when Keycloak sends `refresh_expires_in`)
- [ ] Task 3 — 400 / 401 (AC: #2, #3)
  - [ ] Missing token / bad content-type → `400` simple or empty
  - [ ] Invalid/expired refresh → `401` simple or empty
  - [ ] OA envelope is 3.x — do not build it here unless 3.1 already landed **and** this PR would duplicate 3.2; still keep this story one-PR (refresh only)
- [ ] Task 4 — Tests + README
  - [ ] Mocked KC: success 200; missing field 400; KC invalid_grant 401
  - [ ] README: refresh contract

## Dev Notes

### Scope (this story only)

**In scope:** `POST /refresh` only, reusing the Story 2.1 token client.

**Out of scope:**

| Later story | Do NOT do here |
| --- | --- |
| 3.x | OA envelope (unless already shared — do not invent a second error shape) |
| 4–6 | Users, roles, authz |
| 2.1 | Re-litigate login 201 / JSON-vs-form |

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| Token client from 2.1 | **UPDATE** | Add refresh grant |
| Login module or new refresh controller | **UPDATE/CREATE** | Keep controllers thin |
| README | **UPDATE** | Refresh row |

### File structure (target)

Extend `keycloak-token.client.ts` with `refresh(refreshToken)`. Controller `POST /refresh`.

### Testing requirements

- Assert `grant_type=refresh_token` on the outbound form body.
- Assert success status **200** not 201.
- Same token field set as login.

### Anti-patterns (will fail review)

- Path `/auth/refresh` because Keycloak README table says so
- Success 201 “to match login”
- Re-asking username/password on refresh
- Inventing `KEYCLOAK_GRANT_TYPE` env


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

- [Source: `epics.md` — Story 2.2, FR3, NFR5, NFR6]
- [Source: `SPEC.md` — CAP-7]
- [Source: `oauth-api.md` — CAP-7 Refresh table]
- [Source: Story 2.1 — token client + field mapping]

## Previous story intelligence

Story 2.1 owns login 201 + password grant + field mapping. Reuse that mapper. Epic 3 still owns OA bodies; 2.2 may keep simple 400/401.

## Latest tech information

- Keycloak refresh grant is still the token endpoint with `grant_type=refresh_token` (OIDC). Client remains confidential (`oauth` secret from env).

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

