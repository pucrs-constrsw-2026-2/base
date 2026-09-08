---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 4.1: Create user

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an authenticated caller,
I want to `POST /users` with email username, password, and name,
so that a Keycloak user is created and I get its id.

## Acceptance Criteria

1. **Given** a valid Bearer access token
   **When** the caller `POST /users` with JSON `username` (email), `password`, `first-name`, `last-name`
   **Then** the API creates the user in Keycloak and returns `201` with `{ id, username, first-name, last-name, enabled }`
   **And** `id` is taken from Keycloak’s `Location` header

2. **Given** `username` is not a valid email per RFC 5322, or the body is missing required fields
   **When** the caller `POST /users`
   **Then** the API returns `400` with the OA envelope (`error_code` e.g. `OA-400` if not relaying Keycloak)

3. **Given** that `username` already exists
   **When** the caller `POST /users`
   **Then** the API returns `409` with the OA envelope

4. **Given** the Authorization header is missing or the token is invalid
   **When** the caller `POST /users`
   **Then** the API returns `401` with the OA envelope

5. **Given** a valid token that is not permitted to create users
   **When** the caller `POST /users`
   **Then** the API returns `403` with the OA envelope

## Tasks / Subtasks

- [ ] Task 1 — Shared Bearer guard (AC: #4, #5) — **owned here, reused by 4.2–4.6 and 5.x**
  - [ ] Require `Authorization: Bearer {{access_token}}`
  - [ ] Invalid/missing → `401` OA envelope (Story 3.1 mapper)
  - [ ] Document in README how `403` is decided (Keycloak Admin API 403, and/or missing required client role). Do not invent a local B.2 resource matrix
- [ ] Task 2 — Admin API client (AC: #1)
  - [ ] Obtain admin token using `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (do not invent new env names)
  - [ ] `POST {adminRealmUrl}/users` with Keycloak representation (`username`, `email`, `firstName`, `lastName`, `enabled`, credentials)
  - [ ] Parse **`Location`** for `id` (UUID). Do not wait for a JSON body id
  - [ ] Map inbound JSON hyphens: `first-name` → `firstName`, `last-name` → `lastName`
  - [ ] Username = email (NFR2)
- [ ] Task 3 — Validation + duplicates (AC: #2, #3)
  - [ ] RFC 5322 email check on `username` (reuse a documented regex; brief-level, not a full RFC library required)
  - [ ] Missing fields → `400` `OA-400`
  - [ ] Keycloak 409 / “User exists with same username” → API `409` OA
- [ ] Task 4 — Tests + README
  - [ ] Mock Admin API + UserInfo: 201 Location; 400 email; 409; 401; 403
  - [ ] README: `POST /users` body/response; Bearer; hyphenated fields
  - [ ] Do **not** implement GET/PUT/PATCH/DELETE users in this PR

## Dev Notes

### Scope (this story only)

**In scope:** Bearer guard + `POST /users` + Admin create + OA errors.

Bearer rules (pin in 4.1, reuse later): missing/invalid access token → `401` OA; valid token that Keycloak or the API rejects for permission → `403` OA. Validate the caller token via UserInfo (`urls.userInfoUrl`) or token introspection — do **not** invent JWKS env names. Admin REST calls use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (typically `admin-cli` against realm `master`) plus `adminRealmUrl` — never the caller’s password. JSON fields from the brief use hyphens: `first-name`, `last-name`.

**Out of scope:**

| Later story | Do NOT do here |
| --- | --- |
| 4.2–4.6 | list/get/put/patch/delete |
| 5.x | roles |
| 6.x | authz validate / realm JSON |

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| `urls.adminRealmUrl` / `userInfoUrl` | **REUSE** | Story 1.1 |
| OA mapper | **REUSE** | Epic 3 |
| `src/users/` | **CREATE** | Controller + admin client + guard |
| README | **UPDATE** | Users create + Bearer |

Admin token URL is typically `{base}/realms/master/protocol/openid-connect/token` with `client_id=admin-cli` (public) + resource-owner admin user. If professor stack differs, document — do not add `KEYCLOAK_URL`. Never insert `/auth`.

### File structure (target)

```
backend/oauth/src/authz-guard/   # or common/bearer.guard.ts
backend/oauth/src/users/
  users.controller.ts            # POST only in this PR
  users.mapper.ts                # first-name hyphens
  keycloak-admin.client.ts
```

### Testing requirements

- Mock Keycloak. Assert Location parsing. Do not commit `.env` secrets.

### Anti-patterns (will fail review)

- JSON field `firstName` on the **public** API (brief uses `first-name`)
- Hard-delete or skipping Location
- New error JSON besides OA
- Using `realm-export.json`


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

- [Source: `epics.md` — Story 4.1, FR4, FR10, NFR2]
- [Source: `SPEC.md` — CAP-2]
- [Source: `oauth-api.md` — POST /users]
- [Source: Story 1.1 — adminRealmUrl; Story 3.1 — OA mapper]

## Previous story intelligence

Epic 3 mapper must exist. `adminRealmUrl` already built. Do not call Admin API with the user’s access token unless you have proven that token has realm-management — professor design is server-side `KEYCLOAK_ADMIN`.

## Latest tech information

- Keycloak 26 Admin: `POST /admin/realms/{realm}/users` returns 201 + Location `.../users/{id}`.
- Duplicate username typically 409.

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

