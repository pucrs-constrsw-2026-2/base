---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 1.2: Dockerfile for the oauth API image

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a group developer,
I want a Dockerfile in `backend/oauth` that builds the API image,
so that the professor compose can run our service as a container.

## Acceptance Criteria

1. **Given** the oauth source lives in submodule `backend/oauth` on the group branch
   **When** `docker build` is run against that Dockerfile
   **Then** an image is produced that starts the oauth API process
   **And** the Dockerfile is added in `backend/oauth` (not at repo root as a substitute for compose)
   **And** the image includes a minimal runnable stub (NestJS preferred unless professor mandated Spring) exposing `GET /health` returning HTTP 200 — required by the professor compose healthcheck (`node` + `/health` on `OAUTH_INTERNAL_API_PORT`)
   **And** the stack choice is documented in the oauth README

2. **Given** the image is built
   **When** the container starts with professor-provided env vars
   **Then** the process receives Keycloak URL, realm, client id/secret, and related params from the environment (Story 1.1)
   **And** the group does not add a root `docker-compose.yml` as a deliverable

## Tasks / Subtasks

- [ ] Task 1 — Dockerfile in `backend/oauth` (AC: #1, #2)
  - [ ] Add `backend/oauth/Dockerfile` (multi-stage Nest build → `node dist/main` is fine)
  - [ ] Runtime image **must include `node`**: professor healthcheck is `CMD node -e "require('http').get('http://127.0.0.1:'+(process.env.OAUTH_INTERNAL_API_PORT)+'/health',...)"`
  - [ ] Add `.dockerignore` (`node_modules`, `dist`, `.env`, `.env.*`, coverage, `keycloak/`)
  - [ ] Do **not** add a root Dockerfile or a group compose file
- [ ] Task 2 — Listen + `GET /health` (AC: #1)
  - [ ] `src/main.ts`: `app.listen` on `OAUTH_INTERNAL_API_PORT` from `KeycloakSettingsService` (professor value `3001`). Bind `0.0.0.0` so Docker healthcheck/port-map work
  - [ ] If port is unset locally, default `3001` (compose injects it in the container)
  - [ ] `GET /health` → HTTP 200 (tiny JSON body OK; do not add Prometheus/metrics just because compose maps a metrics port)
  - [ ] Do **not** implement `/login`, OA errors, users, roles, or authz
- [ ] Task 3 — Prove env still flows from process env (AC: #2)
  - [ ] Container must not read a baked-in `.env`. Story 1.1 `ignoreEnvFile: true` stays
  - [ ] Do not `ENV KEYCLOAK_CLIENT_SECRET=...` in the Dockerfile
- [ ] Task 4 — Tests + README (AC: #1)
  - [ ] HTTP test: `GET /health` returns 200 (supertest; set required env in `beforeAll` like Story 1.1 tests)
  - [ ] README: document NestJS 11, Dockerfile location, `GET /health`, listen port `OAUTH_INTERNAL_API_PORT`
  - [ ] Leave compose verify (`docker compose build/up`, volume, `:8081`/`:8181`) to Story 1.3

## Dev Notes

### Scope (this story only)

**In scope:** Dockerfile + `.dockerignore` + HTTP listen + `GET /health` + README stack/health notes.

**Out of scope (do not steal later stories):**

| Later story | Do NOT do here |
| --- | --- |
| 1.3 | `docker compose build/up`; invent/edit compose; ops README (volume, `:8081`, `:8181`) as the verify story |
| 2.x | `POST /login`, `POST /refresh` |
| 3.x | OA error envelope |
| 4–5 | Admin API user/role CRUD |
| 6.x | Authz validate / realm JSON edits |

`docker compose build oauth` is the **acceptance** of 1.3, not a required gate to merge 1.2 if the image builds via `docker build -f backend/oauth/Dockerfile backend/oauth`. Prefer also mentioning that compose will use this Dockerfile unchanged.

### Current files (UPDATE vs CREATE)

| Path | Action | Notes |
| --- | --- | --- |
| `backend/oauth/src/main.ts` | **UPDATE** | Today: `NestFactory.create` then exit path with **no listen**. Must listen |
| `backend/oauth/src/app.module.ts` | **UPDATE** | Register health controller |
| `backend/oauth/README.md` | **UPDATE** | Keep Grupo 07 header + env section; add stack + `/health` |
| `backend/oauth/Dockerfile` | **CREATE** | Only Dockerfile this story ships |
| `backend/oauth/.dockerignore` | **CREATE** | Must ignore `.env` and `keycloak/` |
| Root `docker-compose.yml` / `.env` | **DO NOT TOUCH** | Already has oauth enabled |
| `backend/oauth/keycloak/` | **DO NOT USE** | Divergent export |

### File structure (target)

```
backend/oauth/
  Dockerfile                 # NEW
  .dockerignore              # NEW
  src/main.ts                # UPDATE — listen on OAUTH_INTERNAL_API_PORT
  src/health/                # NEW — GET /health
  README.md                  # UPDATE — NestJS + health
```

### Testing requirements

- Jest + supertest already in `package.json`. No live Docker required in CI for this story (that is 1.3).
- Health test must set `KEYCLOAK_SERVER_URL` + `KEYCLOAK_CLIENT_SECRET` or AppModule fails fast (Story 1.1).

### Anti-patterns (will fail review)

- Spring Boot “because other groups” — compose healthcheck is **node**
- Distroless/scratch without `node` (healthcheck cannot run)
- Listening only on `127.0.0.1` inside the container
- Hard-coding secrets in Dockerfile
- Editing compose to “make healthcheck easier”
- Implementing login “so the stub is more complete”


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

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.2, FR17, NFR1]
- [Source: `SPEC.md` — CAP-8]
- [Source: `oauth-api.md` — Runtime substrate (NestJS stub + GET /health)]
- [Source: `docker-compose.yml` — service `oauth` build + healthcheck]
- [Source: Story 1.1 — env binding, `ignoreEnvFile`, listen port already bound]

## Previous story intelligence

Story 1.1 (`1-1-load-keycloak-settings-from-professor-environment.md`, status **review**): Nest 11 + config + URL builder + tests. `main.ts` does **not** listen. Typed settings already bind `OAUTH_INTERNAL_API_PORT`. Reuse `KeycloakSettingsService.oauth.internalApiPort`. Do not re-bind env under new names.

## Latest tech information

- Official Node images remain the paved path for Nest 11 (`node:22-alpine` or `node:20-alpine`). Keep `npm ci` / `nest build` in a builder stage; run `node dist/main`.
- Compose healthcheck uses Node’s `http` module, not curl/wget — do not switch the image to a JRE.

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

