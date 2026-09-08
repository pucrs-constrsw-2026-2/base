---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 1.1: Load Keycloak settings from professor environment

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a group developer,
I want the oauth process to read Keycloak URL, realm, client id, client secret, and related params from the professor-provided environment,
so that the API can target realm `constrsw` / client `oauth` without hard-coded secrets or an invented `.env`.

## Acceptance Criteria

1. **Given** the professor-provided `.env` (or equivalent process environment) supplies at least `KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` (and related `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` / `OAUTH_*` as needed)
   **When** the oauth service starts
   **Then** it uses those values for Keycloak calls (realm default `constrsw`, client default `oauth`)
   **And** it does not commit alternate secrets or invent a group-owned `.env` as a deliverable
   **And** config binding documents these professor env names in the oauth README

2. **Given** the Keycloak base URL is present (`KEYCLOAK_SERVER_URL`, e.g. `http://keycloak:8080`)
   **When** the service builds Keycloak request URLs
   **Then** it uses `{keycloak-base}/realms/{realm}/…` without inserting `/auth`
   **And** if the provided base URL already includes `/auth`, it uses that base as-is (NFR3)

## Tasks / Subtasks

- [ ] Task 1 — Scaffold NestJS in submodule `backend/oauth` (AC: #1)
  - [ ] Keep existing `README.md` member header; do not wipe it
  - [ ] `package.json` + TypeScript Nest app (`src/main.ts`, `src/app.module.ts`) using npm
  - [ ] Add `.gitignore` covering `node_modules/`, `dist/`, `.env`, `.env.*` (except do **not** add an `.env.example` deliverable)
  - [ ] Work **inside** the submodule on branch `grupo07`; do not implement in the parent repo root
- [ ] Task 2 — Bind professor env names via `@nestjs/config` (AC: #1)
  - [ ] `ConfigModule.forRoot({ isGlobal: true, ignoreEnvFile: true, load: [keycloakConfig] })` — process env only (compose injects vars)
  - [ ] Required: `KEYCLOAK_SERVER_URL`, `KEYCLOAK_CLIENT_SECRET` — fail fast on boot if missing/blank
  - [ ] `KEYCLOAK_REALM` default `constrsw`; `KEYCLOAK_CLIENT_ID` default `oauth` when unset
  - [ ] Bind compose-injected related vars: `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `OAUTH_INTERNAL_API_PORT` (number; professor value is `3001`)
  - [ ] Optional bind if present: other `OAUTH_INTERNAL_*` already injected by compose
  - [ ] Typed accessor (e.g. `KeycloakSettings` / factory return) — no `process.env` scattered in future feature modules
- [ ] Task 3 — Keycloak URL builder (AC: #2)
  - [ ] Trim trailing slashes on `KEYCLOAK_SERVER_URL`; join `{base}/realms/{realm}/…`
  - [ ] **Never** insert `/auth`; if base already contains `/auth`, keep it
  - [ ] Expose at least: `tokenUrl`, `userInfoUrl`; also `adminRealmUrl` (`{base}/admin/realms/{realm}`) for Epics 4–5 — do **not** call Admin API in this story
- [ ] Task 4 — Unit tests for config + URL builder (AC: #1, #2)
  - [ ] Base `http://keycloak:8080` → token `http://keycloak:8080/realms/constrsw/protocol/openid-connect/token` (no `/auth`)
  - [ ] Trailing slash on base does not produce `//realms`
  - [ ] Base already ending with `/auth` → `…/auth/realms/{realm}/…`
  - [ ] Missing `KEYCLOAK_SERVER_URL` or `KEYCLOAK_CLIENT_SECRET` throws at load
  - [ ] Unset realm/client id resolve to `constrsw` / `oauth`
- [ ] Task 5 — README env contract (AC: #1)
  - [ ] Document professor env **names** (never secret **values**)
  - [ ] Document URL `/auth` rule, defaults, `ignoreEnvFile`, “do not invent compose/.env”
  - [ ] Leave Dockerfile, `/health`, compose ops (`:8081`/`:8181`/volume) for Stories 1.2 / 1.3

## Dev Notes

### Scope (this story only)

`backend/oauth` is a git submodule (`grupo07`) with **only** a stub README plus an **untracked** `keycloak/` folder. There is no Nest/Spring app, no `package.json`, no Dockerfile.

**In scope:** Nest scaffold + typed config from professor env + URL builder + tests + README env section.

AC #1 “uses those values for Keycloak calls” means the typed settings + URL builder are the **only** source later HTTP clients will use. Do **not** call Keycloak (token/Admin/authz) in this story. Prove it with bootstrap load + unit tests.

**Out of scope (do not steal later stories):**

| Later story | Do NOT do here |
| --- | --- |
| 1.2 | Dockerfile; `GET /health`; listen-port wiring as the healthcheck target (binding `OAUTH_INTERNAL_API_PORT` is OK) |
| 1.3 | `docker compose build/up`; invent/edit/uncomment root compose; ops README (volume, `:8081`, `:8181`) |
| 2.x | `POST /login`, `POST /refresh` |
| 3.x | OA error envelope |
| 4–5 | Admin API user/role CRUD |
| 6.x | Authz validate / realm JSON edits |

Root `docker-compose.yml` **already enables** service `oauth`. Story 1.3 is **verify-only**. Do not uncomment, replace, or invent compose.

### Runtime contract (professor compose — read-only)

[Source: `docker-compose.yml` service `oauth`]

Compose **already injects** into the oauth container:

```
KEYCLOAK_SERVER_URL
KEYCLOAK_REALM
KEYCLOAK_CLIENT_ID
KEYCLOAK_CLIENT_SECRET
KEYCLOAK_ADMIN
KEYCLOAK_ADMIN_PASSWORD
OAUTH_INTERNAL_PROTOCOL
OAUTH_INTERNAL_HOST
OAUTH_INTERNAL_API_PORT
OAUTH_INTERNAL_DEBUG_PORT
OAUTH_INTERNAL_METRICS_PORT
```

Verified professor names (do **not** copy values into code or README):

- `KEYCLOAK_SERVER_URL=http://keycloak:8080` (no `/auth`) — [Source: SPEC.md Assumptions; professor `.env`]
- `KEYCLOAK_REALM=constrsw`, `KEYCLOAK_CLIENT_ID=oauth`
- External API port name `OAUTH_EXTERNAL_API_PORT` → **8181**; internal listen `OAUTH_INTERNAL_API_PORT` → **3001**
- Keycloak image **26.0.1** (professor Dockerfile), console `:8081`, realm import `infrastructure/dev.local/services/keycloak/constrsw.json`

Present in root `.env` but **NOT** injected into oauth: `KEYCLOAK_GRANT_TYPE`, `KEYCLOAK_TOKEN_ALGORITHM`, `KEYCLOAK_INTERNAL_*`, `KEYCLOAK_EXTERNAL_*`. Do not require them. Login/refresh grants are SPEC-fixed (`password` / `refresh_token`), not env-driven.

### Current files (UPDATE vs CREATE)

| Path | Action | Notes |
| --- | --- | --- |
| `backend/oauth/README.md` | **UPDATE** | Keep “Grupo 07” members block; add env-binding section |
| `backend/oauth/src/**` | **CREATE** | Nest app + `config/` |
| `backend/oauth/package.json` and Nest toolchain | **CREATE** | npm |
| `backend/oauth/.gitignore` | **CREATE** | Must ignore `.env` |
| `backend/oauth/keycloak/` (untracked `realm-export.json`) | **DO NOT USE** | Diverges from professor B.2 (`funcionario`/`coordenador`, authz off). Canonical import is professor `constrsw.json`. Do not promote or commit as runtime config |
| Root `docker-compose.yml` | **DO NOT TOUCH** | Already has oauth enabled |
| Root `.env` | **DO NOT TOUCH** | Professor-owned; never commit secrets from it into oauth |

`README.md` today is only the service title + member names. Preserve that header.

### Forbidden env / library names

Do **not** bind or document these as the Keycloak base URL:

- `KEYCLOAK_URL` / `KEYCLOAK_BASE_URL` (other groups; not professor)
- `KEYCLOAK_JWKS_URL` (architecture spine BFF — out of this SPEC)
- Spine realm `closed-cras`, spine login `POST /v1/auth/login`, client `bff`

Professor names win. [Source: `oauth-api.md` Runtime substrate; epics FR17]

### URL construction (NFR3)

Keycloak 26 (Quarkus) paths **omit** `/auth`:

| Use | Path |
| --- | --- |
| Token (Epic 2) | `{base}/realms/{realm}/protocol/openid-connect/token` |
| UserInfo | `{base}/realms/{realm}/protocol/openid-connect/userinfo` |
| Admin REST (Epics 4–5, later) | `{base}/admin/realms/{realm}` |

Issuer in professor Keycloak README: `http://keycloak:8080/realms/constrsw` — confirms no `/auth`.

Algorithm: `strip trailing /` then `base + '/realms/' + realm + rest`. If `KEYCLOAK_SERVER_URL` is already `http://host:8080/auth`, result is `http://host:8080/auth/realms/...`. Never `replace` or always-append `/auth`.

Old brief path `/auth/realms/...` in `requirements-grupo07-t1-oauth-professors.md` A.1 is **superseded** by NFR3 / SPEC.

### NestJS / config libraries

- Stack: **NestJS 11.x** + `@nestjs/config` + npm. Epic 1.2 / `oauth-api.md` prefer Nest because professor healthcheck is `node` against `/health`. Spring would fail that healthcheck.
- `@nestjs/config`: `ignoreEnvFile: true` so Nest does **not** look for `backend/oauth/.env` (that would become an invented deliverable). Runtime env comes from compose (and from the shell in local tests).
- Custom `load` factory returning a nested object (e.g. `{ keycloak: { serverUrl, realm, clientId, clientSecret, admin, adminPassword }, oauth: { internalApiPort } }`).
- Fail-fast in the factory or `validationSchema` if required vars missing. Nest 12 Standard Schema/Zod is optional; a factory throw is enough and works on Nest 11.
- Do not add `dotenv` as a reason to commit `.env`. Tests set `process.env` in `beforeEach`.

Local `npm run start` for this story: export the professor **names** in the shell (or a **gitignored** personal file — not a group deliverable). Do not add `.env.example` with secrets.

### Architecture compliance

Canonical contract: `_bmad-output/specs/spec-grupo07-keycloak-oauth/` (`SPEC.md` CAP-8, `oauth-api.md` runtime substrate). Architecture spine is **out of scope** for this SPEC run — do not follow spine env names, realm `closed-cras`, or `backend/oauth` as “catalog/faixas” instead of the T1 gateway.

NFRs for this story: NFR1 (implement in `backend/oauth`), NFR2 (realm/client defaults), NFR3 (`/auth` rule), NFR12 (do not invent compose/.env), NFR13 (credentials from provided `.env`, Keycloak 26.x).

### File structure (target)

```
backend/oauth/
  README.md                 # UPDATE — env names + URL rule
  package.json              # NEW
  tsconfig.json             # NEW (Nest CLI)
  nest-cli.json             # NEW
  .gitignore                # NEW — ignore .env, node_modules, dist
  src/
    main.ts                 # NEW — bootstrap only; no /health (1.2)
    app.module.ts           # NEW — ConfigModule.forRoot(...)
    config/
      keycloak.config.ts    # NEW — factory from process.env
      keycloak-urls.ts      # NEW — join base + realm paths
      keycloak-urls.spec.ts # NEW — or test/ equivalent
```

`main.ts` may bootstrap Nest so “when the service starts” is real. Do **not** add `GET /health` here. Listening on `OAUTH_INTERNAL_API_PORT` can wait for 1.2 if tests cover config load without listen.

### Testing requirements

- Jest from Nest scaffold.
- Pure unit tests for URL join + missing-required-env. No Docker, no live Keycloak, no compose (that is 1.3).
- Do not assert secret values from professor `.env`. Use fixtures like `serverUrl=http://keycloak:8080`, `clientSecret=test-secret`.

### Anti-patterns (will fail review)

- Creating `backend/oauth/.env` or root `.env.example` as a tracked deliverable
- Hard-coding `KEYCLOAK_CLIENT_SECRET` or admin password in source/README
- Editing `docker-compose.yml` or professor `.env`
- Using `KEYCLOAK_URL` because another group’s branch did
- Inserting `/auth` “to match the old brief”
- Committing `backend/oauth/keycloak/realm-export.json` as the realm of record
- Implementing Dockerfile, `/health`, or login “while we’re here”
- Copying architecture-spine `KEYCLOAK_JWKS_URL` / `closed-cras`

### Project Structure Notes

- Parent repo: `constrsw-2026-2` branch `grupo07`. Implementation lives in submodule `backend/oauth` (remote `oauth.git`, branch `grupo07`).
- Professor stack already on this tree: root `docker-compose.yml`, root `.env`, `infrastructure/dev.local/services/keycloak/`.
- No sibling backend has a ConfigModule to copy; all other `backend/*` are stubs.

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` — Epic 1, Story 1.1, FR17, NFR2/NFR3/NFR12/NFR13]
- [Source: `_bmad-output/specs/spec-grupo07-keycloak-oauth/SPEC.md` — CAP-8, Constraints, Assumptions]
- [Source: `_bmad-output/specs/spec-grupo07-keycloak-oauth/oauth-api.md` — Runtime substrate, Keycloak reference]
- [Source: `docker-compose.yml` — service `oauth` environment + healthcheck (healthcheck is 1.2/1.3)]
- [Source: `infrastructure/dev.local/services/keycloak/README.md` — issuer `http://keycloak:8080/realms/constrsw`]
- [Source: NestJS Configuration — `ConfigModule.forRoot({ ignoreEnvFile: true })`](https://docs.nestjs.com/techniques/configuration)
- [Source: Keycloak 26 OIDC token path `{base}/realms/{realm}/protocol/openid-connect/token`](https://www.keycloak.org/securing-apps/oidc-layers)

## Previous story intelligence

None. This is the first story in Epic 1. No prior implementation-artifacts story files.

## Git intelligence summary

Parent recent commits are professor stack, not oauth app code: `Ajusta compose`, `Ajusta nome do serviço oauth`, `Cria assets do keycloak em dev.local`, `Cria compose e env`.

Submodule `backend/oauth` HEAD `48b1723` (`feat: setando e testando branch`) — README stub only. Untracked `keycloak/` is local noise, not professor truth.

Do not “match” other remotes that use `KEYCLOAK_URL`.

## Latest tech information

- NestJS **11.x** is the course-aligned Node paved path; Nest **12** exists (Standard Schema / Zod for `validationSchema`). Prefer Nest 11 + factory fail-fast unless the CLI you use scaffolds 12 — then keep `ignoreEnvFile: true` either way.
- Keycloak **26** Admin/OIDC URLs omit `/auth`. Professor image is **26.0.1** (not spine’s 26.7.2). Use professor compose/image; do not change Keycloak Dockerfile.
- `@nestjs/config` still uses dotenv internally; `ignoreEnvFile: true` disables file load so process env (compose) is the only source.

## Project context reference

No `project-context.md` in this repo. Follow SPEC + epics + this story.

## Story completion status

Ultimate context engine analysis completed — comprehensive developer guide created.

**Status:** ready-for-dev

### Discovery (create-story)

- Loaded `{epics_content}` from `_bmad-output/planning-artifacts/epics.md`
- Loaded contract `{spec}` from `_bmad-output/specs/spec-grupo07-keycloak-oauth/` (SPEC.md, oauth-api.md, keycloak-authz.md)
- Architecture spine present but **out of scope** for this SPEC — used only as a negative constraint (wrong env/realm names)
- No UX requirements (HTTP API)
- No `project-context.md`
- No previous story file

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

