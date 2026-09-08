---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 6.5: Validate resource access via Keycloak Authorization Services

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API caller,
I want to `POST /authz/validate` with a Bearer token and a resource name,
so that I learn whether that token is permitted — decided by Keycloak, not a local role matrix.

## Acceptance Criteria

1. **Given** a valid access token and JSON `{ "resource": "<name>" }` where name is one of the eight resources
   **When** the caller `POST /authz/validate` with `Authorization: Bearer {{access_token}}`
   **Then** the API calls **Keycloak Authorization Services** (token/permission evaluation against configured resources/policies)
   **And** Keycloak Authorization Services is the **decision engine**
   **And** the B.2 matrix is the **test oracle only** — not a local allow/deny implementation

2. **Given** Keycloak permits access
   **When** validate runs
   **Then** the API returns `200`

3. **Given** Keycloak denies access
   **When** validate runs
   **Then** the API returns `403` with the OA envelope

4. **Given** the token is missing or invalid
   **When** the caller `POST /authz/validate`
   **Then** the API returns `401` with the OA envelope (Bearer rules from Story 4.1)

5. **Given** the body is malformed or `resource` is not one of the eight names
   **When** the caller `POST /authz/validate`
   **Then** the API returns `400` with the OA envelope (`error_code` e.g. `OA-400` if not relaying Keycloak)

6. **Given** the B.2 matrix
   **When** tests run against professor Keycloak
   **Then** at least one permitted pair returns `200` and at least one forbidden pair returns `403`
   **And** outcomes match: administrator → `resources`, `rooms`, `professors`, `students`; coordinator → `courses`, `classes`; professor → `lessons`, `reservations`; student → **none**

## Tasks / Subtasks

- [ ] Task 1 — `POST /authz/validate` (AC: #1–#5)
  - [ ] Bearer + JSON `{ "resource": "<name>" }`
  - [ ] Unknown name / bad JSON → `400` OA
  - [ ] Missing/invalid token → `401` OA (reuse 4.1 guard)
  - [ ] Call KC Authorization Services (UMA ticket / permission evaluation on the token endpoint, or Authz HTTP API). **No** `if (role===administrator) return 200`
- [ ] Task 2 — Map KC permit/deny → `200` / `403` OA (AC: #2, #3)
- [ ] Task 3 — Tests (AC: #6)
  - [ ] Unit tests mock KC decision endpoint (prove no local matrix)
  - [ ] At least one live or documented lab check: permitted `200` + forbidden `403` matching B.2
  - [ ] Student → none (403 on any of the eight)
- [ ] Task 4 — README: path, body, eight names, “decision engine = Keycloak”
- [ ] Do not edit realm JSON as the implementation of allow/deny

## Dev Notes

### Scope (this story only)

**In scope:** validate API only. 6.1–6.4 must already be verified.

Bearer rules (pin in 4.1, reuse later): missing/invalid access token → `401` OA; valid token that Keycloak or the API rejects for permission → `403` OA. Validate the caller token via UserInfo (`urls.userInfoUrl`) or token introspection — do **not** invent JWKS env names. Admin REST calls use `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` (typically `admin-cli` against realm `master`) plus `adminRealmUrl` — never the caller’s password. JSON fields from the brief use hyphens: `first-name`, `last-name`.

Suggested KC call (document actual): token endpoint `grant_type=urn:ietf:params:oauth:grant-type:uma-ticket` with `audience=` the professor client id `oauth` and `permission=` the resource name using the caller’s access token. Use `urls.tokenUrl` (no `/auth` insert).

**Out of scope:** implementing classes/courses/… services; local switch on role names; inventing compose.

If live outcomes diverge because of extra `applyPolicies` (see 6.4 note), document and align running authz config — still **no** local matrix.


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

- [Source: `epics.md` — Story 6.5, FR16, NFR9, NFR11]
- [Source: `SPEC.md` — CAP-6]
- [Source: `keycloak-authz.md` — CAP-6 validate + matrix]
- [Source: `oauth-api.md` — OA error body]
- [Source: Stories 4.1 Bearer, 3.1 mapper, 6.1–6.4 verify]

## Previous story intelligence

Requires 6.1–6.4 verify, 4.1 Bearer, 3.1 OA, 2.1 login to mint tokens for lab tests. Professor test users: `admin@pucrs.br` / coordinator / professor / student (Keycloak README). Tokens must carry **client** roles on `oauth` for policies `oauth/<role>`.

## Latest tech information

- Keycloak 26 Authorization Services: UMA ticket grant on the token endpoint is the usual evaluation API. Do not use JWKS-only local RBAC.

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

