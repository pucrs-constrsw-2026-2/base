---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 3.1: OA error envelope and mapper

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As an API caller or grader,
I want a single error shape for the oauth API,
so that every failure is diagnosable the same way.

## Acceptance Criteria

1. **Given** the oauth API produces an error response
   **When** the shared mapper serializes it
   **Then** the JSON body is `{ error_code, error_description, error_source, error_stack }`
   **And** `error_source` is `OAuthAPI` (or an equally specific oauth-API source)
   **And** `error_description` is a group-provided human message
   **And** `error_stack` is a JSON **array of objects**, chaining errors down to the root cause

2. **Given** Keycloak returned an error code
   **When** the mapper sets `error_code`
   **Then** it relays Keycloak’s code unless a brief-specific code applies

3. **Given** the failure is local (structure/validation) and not a Keycloak code
   **When** the mapper sets `error_code`
   **Then** it uses a documented brief-specific convention (e.g. `OA-400` for bad structure) so Users/Roles stories do not invent new code families
   **And** that convention is recorded in the oauth service README (or equivalent)

4. **Given** subsequent stories return HTTP errors
   **When** they use this mapper
   **Then** they must not invent a different error JSON shape

## Tasks / Subtasks

- [ ] Task 1 — Envelope type + mapper (AC: #1, #2, #3)
  - [ ] Type: `{ error_code: string, error_description: string, error_source: string, error_stack: object[] }`
  - [ ] `error_source`: `OAuthAPI`
  - [ ] `error_stack`: **array of objects** (not a string, not an array of strings)
  - [ ] Relay Keycloak `error` / `error_description` into `error_code` when present
  - [ ] Local validation: `OA-400` (and document siblings e.g. `OA-401` only if you need a local-auth code; prefer relaying KC for credential failures)
- [ ] Task 2 — Nest exception filter (AC: #1, #4)
  - [ ] Global filter (or equivalent) so later modules cannot casually return `{ message, statusCode }`
  - [ ] Do **not** require wiring `/login` and `/refresh` in this PR — that is Story 3.2. Export the mapper so 3.2 is a thin adopt
- [ ] Task 3 — README convention (AC: #3)
  - [ ] Document envelope fields + `OA-400` family + “relay Keycloak code when present”
  - [ ] Document that Epics 4–6 **must** reuse this mapper
- [ ] Task 4 — Unit tests (AC: #1–#3)
  - [ ] Local error → `OA-400`, stack is array of objects
  - [ ] KC payload `{ error: "invalid_grant" }` → `error_code` relays `invalid_grant`

## Dev Notes

### Scope (this story only)

**In scope:** shared OA mapper + documented code convention + filter infrastructure. **Not** switching login/refresh (3.2).

**Out of scope:**

| Later story | Do NOT do here |
| --- | --- |
| 3.2 | Wire `/login` and `/refresh` 400/401 to the envelope |
| 4–6 | User/role/authz routes |

`oauth-api.md` example `error_code: "OA-000"` is illustrative. Pin the family in README (`OA-400` for bad structure, as epics.md).

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| `src/errors/` or `src/oa/` | **CREATE** | DTO + mapper + filter |
| README | **UPDATE** | Envelope + code table |
| Login/refresh controllers | **DO NOT CHANGE** | 3.2 |

### File structure (target)

```
backend/oauth/src/errors/
  oa-error.ts
  oa-error.mapper.ts
  oa-error.mapper.spec.ts
  oa-exception.filter.ts
```

### Testing requirements

- Pure unit tests on the mapper. No Docker.

### Anti-patterns (will fail review)

- `error_stack` as a stringified traceback only
- Nest default `{ statusCode, message }` leaking after this story is consumed
- Per-route ad-hoc error JSON in later PRs
- Inventing `ERR_USER_*` families in Epic 4


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

- [Source: `epics.md` — Story 3.1, FR14]
- [Source: `SPEC.md` — CAP-4]
- [Source: `oauth-api.md` — Error body table]

## Previous story intelligence

Stories 2.1/2.2 may still return empty/simple errors until 3.2. 3.1 must not expand login behavior.

## Latest tech information

- Keep the filter compatible with Nest 11 `ExceptionFilter`. Do not add a new HTTP framework.

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

