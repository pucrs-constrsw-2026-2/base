---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 6.2: Authorization resources with URLs

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a group developer,
I want eight Keycloak Authorization Services resources on client `oauth`,
so that permissions can target named resources with URLs.

## Acceptance Criteria

1. **Given** client `oauth` in realm `constrsw`
   **When** this story starts
   **Then** Authorization Services is **verified** enabled on client `oauth` (`authorizationServicesEnabled` or equivalent)

2. **Given** Authorization Services is enabled
   **When** this story is done
   **Then** resources are **verified** (create only gaps): `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`
   **And** each has a URL (as imported or defaults from `keycloak-authz.md`)
   **And** these URLs identify Authz resources only — this SPEC does **not** implement those domain APIs (NFR11)
   **And** the oauth README documents verification / gap-fill — not a group-owned compose or realm-JSON deliverable

## Tasks / Subtasks

- [ ] Task 1 — Authz enabled (AC: #1)
  - [ ] Confirm `authorizationServicesEnabled` on client `oauth` (present in professor export)
- [ ] Task 2 — Eight resources (AC: #2)
  - [ ] Names + URLs (`/classes`, `/courses`, …) as imported or `keycloak-authz.md` defaults
  - [ ] Gap-fill only; do not implement Nest routes for those URLs
- [ ] Task 3 — README (no compose/realm-JSON deliverable)
- [ ] Do not create policies/permissions (6.3/6.4) or validate (6.5)

## Dev Notes

### Scope (this story only)

**In scope:** verify Authz enabled + eight named resources with URLs.

**Verify-first (6.1–6.4):** professor `constrsw.json` already contains B.2-named objects. Confirm in Admin Console **after volume import**. Create/fix **only gaps**. Do **not** replace professor realm JSON or invent compose as a git deliverable. Document method (console checklist / overlay / Admin API) in oauth README.

Import caveat: `start-dev --import-realm` runs only if realm `constrsw` is **absent** on volume `constrsw-keycloak-data`. Changing JSON without recreating the volume does nothing. Recreate volume only when the team intends a full reimport (professor Keycloak README).

Import also contains `Default Resource` `/*` — leave it; do not treat it as one of the eight B.2 names.

**Out of scope:** domain microservices, 6.3–6.5.

### Anti-patterns

- Scaffolding `backend/classes`. Replacing `constrsw.json`. Using divergent `realm-export.json`.


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

- [Source: `epics.md` — Story 6.2, FR15, NFR10, NFR11]
- [Source: `keycloak-authz.md` — resource table]
- [Source: `constrsw.json` — authorizationSettings.resources]

## Previous story intelligence

Depends on 6.1 stop-condition (canonical role names). Resources can be verified in parallel in console but stories stay one-PR.

## Latest tech information

- Professor export already has the eight resources with uris `/classes` … `/students` and `authorizationServicesEnabled: true`.

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

