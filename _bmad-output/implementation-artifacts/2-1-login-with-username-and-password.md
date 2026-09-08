---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 2.1: Login with username and password

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API caller,
I want to `POST /login` with form-data `username` and `password` only,
so that I receive Keycloak tokens without sending client credentials.

## Acceptance Criteria

1. **Given** valid credentials for a user in realm `constrsw`
   **When** the caller `POST /login` with `username` and `password` only (no `client_id` / `client_secret` / `grant_type`) as `multipart/form-data` **or** `application/x-www-form-urlencoded`
   **Then** the API adds `client_id`, `client_secret`, and `grant_type=password` from env and calls `POST {keycloak-base}/realms/constrsw/protocol/openid-connect/token`
   **And** the response is HTTP `201` with JSON `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in`
   **And** `referesh_expires_in` is mapped from Keycloak’s `refresh_expires_in` (NFR5)

2. **Given** the request body is missing `username` or `password`, or is not valid form-data / urlencoded
   **When** the caller `POST /login`
   **Then** the API returns `400`
   **And** the body may be simple or empty (OA envelope is Story 3.x)

3. **Given** username/password do not authenticate at Keycloak
   **When** the caller `POST /login`
   **Then** the API returns `401`
   **And** the body may be simple or empty (OA envelope is Story 3.x)

## Tasks / Subtasks

- [ ] Task 1 — `POST /login` controller (AC: #1)
  - [ ] Accept **both** `multipart/form-data` and `application/x-www-form-urlencoded` (epics.md). JSON body is **out of contract** (Keycloak README JSON curl is wrong for T1)
  - [ ] Read only `username` and `password`. Ignore/do not require client fields from the caller
  - [ ] Success HTTP **`201`** (NFR6), not Keycloak’s `200`
- [ ] Task 2 — Keycloak password grant (AC: #1)
  - [ ] POST to `KeycloakSettingsService.urls.tokenUrl` as `application/x-www-form-urlencoded`
  - [ ] Server adds `client_id`, `client_secret` from settings, `grant_type=password`
  - [ ] Do not insert `/auth` (already handled by URL builder)
- [ ] Task 3 — Token JSON mapping (AC: #1)
  - [ ] Return `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in`
  - [ ] Map Keycloak `refresh_expires_in` → `referesh_expires_in` (brief spelling). Do not “fix” the typo
- [ ] Task 4 — 400 / 401 without OA envelope (AC: #2, #3)
  - [ ] Missing/blank username or password, or unsupported content-type → `400` (simple or empty body)
  - [ ] Keycloak invalid_grant / 401 → API `401` (simple or empty body)
  - [ ] Leave `{ error_code, error_description, error_source, error_stack }` to Epic 3
- [ ] Task 5 — Tests + README
  - [ ] Unit/HTTP tests with **mocked** Keycloak token endpoint (no live KC required)
  - [ ] Cases: urlencoded success 201 + typo field; multipart success; missing field 400; KC 401 → 401
  - [ ] README: `POST /login` contract (form only, 201, field list)
  - [ ] Do not commit professor passwords; tests use fixtures

## Dev Notes

### Scope (this story only)

**In scope:** `POST /login` + token mapping + 400/401 (non-OA). One PR.

**Out of scope:**

| Later story | Do NOT do here |
| --- | --- |
| 2.2 | `POST /refresh` |
| 3.1 / 3.2 | OA envelope (2.1 **may** use empty/simple error bodies) |
| 4–5 | Users/roles |
| 6.x | Authz |
| 1.3 | Compose edits |

Extract a small Keycloak token client so 2.2 can reuse it — but do **not** implement refresh in this PR.

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| `backend/oauth/src/config/*` | **REUSE** | `tokenUrl`, client id/secret |
| `backend/oauth/src/app.module.ts` | **UPDATE** | Register login module |
| `backend/oauth/src/login/` (or `auth/`) | **CREATE** | Controller + token client |
| `backend/oauth/README.md` | **UPDATE** | Login contract |
| Root compose / `.env` | **DO NOT TOUCH** | |

Professor test users (Keycloak README; password is lab-shared — do not put it in source): `admin@pucrs.br`, `coordinator@pucrs.br`, `professor@pucrs.br`, `student@pucrs.br`. Direct Access Grants are enabled on client `oauth` in `constrsw.json`.

### File structure (target)

```
backend/oauth/src/
  login/   # or auth/ — pick one and stick
    login.controller.ts
    keycloak-token.client.ts   # password grant now; refresh in 2.2
    login.controller.spec.ts
```

### Testing requirements

- Mock HTTP to `tokenUrl`. Assert request **body** contains `grant_type=password` and server client credentials, and does **not** require them from the caller.
- Assert response key is `referesh_expires_in` not `refresh_expires_in`.
- Do not hit live Keycloak in unit tests.

### Anti-patterns (will fail review)

- JSON login because the Keycloak README shows it
- Returning HTTP 200 on success
- Correcting `referesh_expires_in` to “proper English”
- Putting `client_secret` in the README
- Implementing OA mapper “since we have errors”
- Using `KEYCLOAK_GRANT_TYPE` from root `.env` (not injected; grant is SPEC-fixed)


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

- [Source: `epics.md` — Story 2.1, FR1, FR2, NFR4, NFR5, NFR6]
- [Source: `SPEC.md` — CAP-1, Constraints (201, referesh_expires_in, username+password only)]
- [Source: `oauth-api.md` — CAP-1 Login table]
- [Source: Story 1.1 — `tokenUrl`]

## Previous story intelligence

Stories 1.1–1.3: settings, URLs, Dockerfile/health, compose verify. `tokenUrl` already built. Login must not invent a second base URL. Epic 3 explicitly allows simple/empty 400/401 bodies on 2.1.

## Latest tech information

- Keycloak 26 token endpoint remains `{base}/realms/{realm}/protocol/openid-connect/token` (password grant / Direct Access Grants).
- Nest 11: parse urlencoded via Express (already on `@nestjs/platform-express`); multipart via built-in file interceptor or `multer` already pulled by Nest — keep it minimal.

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

