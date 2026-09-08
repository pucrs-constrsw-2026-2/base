---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 1.3: Verify oauth runs on professor-provided compose

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a group developer,
I want the professor-provided `oauth` service to build and run our image against Keycloak,
so that graders can start the stack without a group-invented compose file.

## Acceptance Criteria

1. **Given** root `docker-compose.yml` from the professor already defines service `oauth` (build `./backend/oauth`, env from `.env`) — **already enabled; do not invent or replace compose**
   **When** this story is done
   **Then** `docker compose build oauth` (or equivalent) succeeds using the Dockerfile from Story 1.2
   **And** with external volume `constrsw-keycloak-data` created as documented by the professor, the stack can start Keycloak + oauth
   **And** the oauth container becomes healthy (`GET /health`) and can resolve Keycloak via `KEYCLOAK_SERVER_URL`
   **And** no new compose file is invented as a group deliverable

2. **Given** the professor Keycloak README / compose ops notes
   **When** the oauth README is updated
   **Then** it documents: create `constrsw-keycloak-data`, console at `:8081`, API at `:8181`, and that login contract for *our* API remains SPEC/T1 (form-data, `201`) even if the Keycloak README shows a JSON curl example

## Tasks / Subtasks

- [ ] Task 1 — Verify build against professor compose (AC: #1)
  - [ ] From repo root, with Story 1.2 Dockerfile present: `docker compose build oauth`
  - [ ] Do **not** add/uncomment/replace `docker-compose.yml`. If build fails, fix **oauth** (Dockerfile/context), not compose
- [ ] Task 2 — Verify run + health (AC: #1)
  - [ ] `docker volume create constrsw-keycloak-data` if missing (external volume already declared)
  - [ ] Start Keycloak + oauth via professor compose (`docker compose up -d` or equivalent)
  - [ ] Confirm oauth reaches `healthy` (healthcheck hits `GET /health` on `OAUTH_INTERNAL_API_PORT`)
  - [ ] From inside the oauth container (or logs), confirm `KEYCLOAK_SERVER_URL` is `http://keycloak:8080` and the hostname `keycloak` resolves on network `constrsw`
  - [ ] Do **not** implement `/login` to “prove” Keycloak — resolution + healthy is enough. Login is Story 2.1
- [ ] Task 3 — oauth README ops (AC: #2)
  - [ ] Document: `docker volume create constrsw-keycloak-data`; Keycloak console `http://localhost:8081`; oauth API `http://localhost:8181`
  - [ ] Document: **our** login (Story 2.1) is form-data/`x-www-form-urlencoded` + HTTP `201` per SPEC — **not** the Keycloak README JSON curl to `:8181/login`
  - [ ] Document import caveat: `--import-realm` only imports if the realm is absent on the volume (professor Keycloak README)
  - [ ] Do not paste secret values from `.env` or `constrsw.json`
- [ ] Task 4 — Record verification, no extra deliverables
  - [ ] Completion notes: commands run + healthy result
  - [ ] No group `docker-compose.override.yml`, no second `.env`, no committed realm export

## Dev Notes

### Scope (this story only)

**In scope:** verify professor compose builds/runs oauth; README ops (`volume`, `:8081`, `:8181`, T1 login vs Keycloak README curl). Tiny oauth README edits only.

**Out of scope:**

| Later story | Do NOT do here |
| --- | --- |
| 2.1 / 2.2 | `POST /login`, `POST /refresh` |
| 3.x | OA envelope |
| 4–5 | Users/roles Admin API |
| 6.1–6.4 | Console authz verify (needs running Keycloak; that is Epic 6, not a license to rewrite realm JSON now) |
| 6.5 | `POST /authz/validate` |

If compose/env is wrong, **stop** and report — do not invent a replacement stack.

### Current files

| Path | Action | Notes |
| --- | --- | --- |
| `backend/oauth/README.md` | **UPDATE** | Ops section only |
| `backend/oauth/Dockerfile` | **UPDATE only if verify fails** | Prefer fixing 1.2 issues here rather than forking compose |
| Root `docker-compose.yml` | **DO NOT TOUCH** | Already enabled |
| Root `.env` | **DO NOT TOUCH** | Professor-owned |
| `infrastructure/dev.local/services/keycloak/constrsw.json` | **DO NOT REPLACE** | Epic 6 verifies |

### File structure (target)

No new Nest modules. README ops headings only.

### Testing requirements

- Operational verification, not a new Jest suite (unless a 1.2 health test was missed).
- Do not add a compose-based e2e in the parent repo as a required deliverable.

### Anti-patterns (will fail review)

- Adding `docker-compose.yml` under `backend/oauth`
- Uncommenting “the real oauth service” — it is already enabled
- Changing Keycloak image/tag or Dockerfile under `infrastructure/`
- Copying Keycloak README JSON login as **our** contract
- Implementing login in this PR “while the stack is up”


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

- [Source: `epics.md` — Story 1.3, FR17, NFR12]
- [Source: `SPEC.md` — CAP-8, Assumptions, Constraints]
- [Source: `oauth-api.md` — Runtime substrate; login contract note]
- [Source: `docker-compose.yml` — oauth + keycloak + external volume]
- [Source: `infrastructure/dev.local/services/keycloak/README.md` — volume reimport, console `:8081` — login curl is **not** T1]

## Previous story intelligence

Story 1.2 must ship Dockerfile + `/health` + listen **before** this verify story is meaningful. Story 1.1 supplies env names. Keycloak README (`infrastructure/dev.local/services/keycloak/README.md`) uses JSON `POST http://localhost:8181/login` and mentions service `auth` — **superseded** for Grupo 07 by SPEC/T1 (`oauth`, form-data, `201`).

## Latest tech information

- Compose v2 `docker compose` (space) is the professor CLI. External volumes must exist before `up`.
- Keycloak 26 `start-dev --import-realm` will **not** re-import over an existing realm on `constrsw-keycloak-data`.

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

