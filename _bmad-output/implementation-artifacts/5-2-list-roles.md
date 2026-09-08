---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 5.2: List roles

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an authenticated caller,
I want to `GET /roles`,
so that I can see client roles on `oauth`.

## Acceptance Criteria

1. **Given** a valid Bearer token
   **When** the caller `GET /roles`
   **Then** the API returns `200` with a list of roles (shape documented in README), filtered to client `oauth` when binding to Keycloak

2. **Given** missing/invalid token or insufficient permission
   **When** the caller `GET /roles`
   **Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

## Tasks / Subtasks

- [ ] Task 1 — `GET /roles` client `oauth` only (AC: #1)
  - [ ] Same README representation as 5.1
  - [ ] Do not list realm roles or other clients
- [ ] Task 2 — Bearer 401/403 (AC: #2)
- [ ] Task 3 — Tests; README

## Dev Notes

### Scope (this story only)

**In scope:** list client roles on `oauth`.

Bearer rules (pin in 4.1, reuse later): missing/invalid access token → `401` OA; valid token that Keycloak or the API rejects for permission → `403` OA. Validate the caller token via UserInfo (`urls.userInfoUrl`) or token introspection — do **not** invent JWKS env names. Admin REST calls use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (typically `admin-cli` against realm `master`) plus `adminRealmUrl` — never the caller’s password. JSON fields from the brief use hyphens: `first-name`, `last-name`.

**Out of scope:** get-by-id, mutations, assign.

If 5.6 logical-delete uses a flag, list may still return inactive roles — document; Epic 6 binds **active** canonical names only.

### Current files (UPDATE vs CREATE)

| Path | Action | Notes |
| --- | --- | --- |
| `src/roles/` from 5.1 | **UPDATE** | Add `GET /roles` only |
| Bearer guard from 4.1 | **REUSE** | Do not fork auth |
| README | **UPDATE** | List response shape (same as 5.1) |
| Root compose / `.env` / `constrsw.json` | **DO NOT TOUCH** | |

### Testing requirements

- Mock Admin `GET .../clients/{id}/roles`. Assert only oauth client roles. 401/403 OA.

### Anti-patterns (will fail review)

- Listing realm roles. Returning Keycloak-only field dumps without README. New error JSON.


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

- [Source: `epics.md` — Story 5.2, FR11, FR13]
- [Source: Story 5.1 — representation; 4.1 Bearer]

## Previous story intelligence

5.1 README shape + admin client UUID lookup.

## Latest tech information

- `GET /admin/realms/{realm}/clients/{id}/roles`.

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

