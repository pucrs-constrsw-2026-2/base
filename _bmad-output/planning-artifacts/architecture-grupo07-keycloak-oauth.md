# Architecture — Grupo 07 Keycloak / oauth (lean)

**Status:** Keycloak + oauth gateway only. Professors / Postgres / other domains are **out of scope** until the group reopens that track.  
**Contract (WHAT):** `_bmad-output/specs/spec-grupo07-keycloak-oauth/`  
**Backlog:** `_bmad-output/planning-artifacts/epics.md`  
**Handoff log:** `CHANGELOGS.md` (repo root)

This document is the **HOW** spine for the identity gateway. It does not replace the SPEC.

---

## 1. Purpose

Provide a Dockerized **NestJS** REST API in `backend/oauth` that:

1. Talks to **Keycloak** (professor stack) for tokens, users, roles, and Authorization Services.
2. Exposes the T1/SPEC routes graders call (`/login`, `/refresh`, `/users`, `/roles`, `/authz/validate`, OA errors).
3. Does **not** invent root `docker-compose.yml` / `.env` — plugs into the professor files.

---

## 2. Runtime topology

```text
Postman / caller
       │
       ▼
 oauth (:8181 → :3001)     NestJS in backend/oauth
       │
       │  KEYCLOAK_SERVER_URL=http://keycloak:8080
       ▼
 keycloak (:8081 console, :8080 internal)
       │
       └── import: infrastructure/dev.local/services/keycloak/constrsw.json
           volume: constrsw-keycloak-data (external)
```

| Piece | Source of truth |
| --- | --- |
| Compose + env | Repo root `docker-compose.yml`, `.env` (professor) |
| Realm | `infrastructure/dev.local/services/keycloak/constrsw.json` |
| API code + Dockerfile | Submodule `backend/oauth` branch `grupo07` |
| Healthcheck | Compose: `node` → `GET /health` on `OAUTH_INTERNAL_API_PORT` |

**Ops:** create volume `constrsw-keycloak-data` before first `docker compose up`. Reimport realm only after volume remove (see Keycloak README).

---

## 3. Technology choices

| Concern | Choice | Why |
| --- | --- | --- |
| Language | TypeScript / Node | Professor oauth healthcheck is Node |
| Framework | **NestJS 11** + npm | Epic/Story 1.2; peer Nest references |
| Config | `@nestjs/config`, `ignoreEnvFile: true` | Compose injects env; no baked secrets |
| IdP | Keycloak **26.0.1** (professor image) | Provided stack |
| Realm / client | `constrsw` / `oauth` | Professor import + SPEC |
| Tests | Jest | Unit/HTTP with mocked Keycloak where possible |

---

## 4. Configuration (env names)

Injected into the `oauth` container (bind these; never hard-code secrets):

- `KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`
- `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`
- `OAUTH_INTERNAL_*` (listen on `OAUTH_INTERNAL_API_PORT`, typically `3001`)

**URL rule:** `{KEYCLOAK_SERVER_URL}/realms/{realm}/…` — do **not** insert `/auth`. If the base already contains `/auth`, use as-is.

Present in root `.env` but **not** passed to oauth by compose (do not require): `KEYCLOAK_GRANT_TYPE`, `KEYCLOAK_TOKEN_ALGORITHM`, host/port split vars except via `KEYCLOAK_SERVER_URL`.

---

## 5. Application shape (target modules)

As stories land, keep roughly:

| Area | Responsibility | Stories |
| --- | --- | --- |
| `config/` | Typed settings + Keycloak URL builder | 1.1 |
| `health/` | `GET /health` | 1.2 |
| `auth/` | `POST /login`, `POST /refresh` | 2.x |
| `errors/` | OA envelope mapper | 3.x |
| `users/` | User CRUD + soft-delete + Bearer guard | 4.x |
| `roles/` | Role CRUD + assign/unassign | 5.x |
| `authz/` | `POST /authz/validate` via KC Authz Services | 6.5 |
| Dockerfile | Image for professor compose | 1.2–1.3 |

**Invariants:**

- Login success **201**; refresh **200**; preserve `referesh_expires_in` spelling.
- Client credentials only server-side.
- Soft-delete user = disable in Keycloak (`204`).
- Authz decisions = **Keycloak Authorization Services**, not a local role matrix (B.2 matrix = test oracle).
- Bearer rules owned by Story 4.1; reused afterward.
- Assign/unassign paths pinned in README (default `POST/DELETE /users/{{id}}/roles…`).

---

## 6. Keycloak model (imported)

From professor `constrsw.json` (verify in Admin Console after import):

- Realm roles / client roles on `oauth`: `administrator`, `coordinator`, `professor`, `student`
- Authorization Services **enabled** on client `oauth`
- Eight resources: `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`
- Policies named for B.2; import may also contain extras (e.g. `student-policy`) — document gaps; do not replace professor realm JSON as a group deliverable
- Test users `*@pucrs.br` (password only in professor docs / `.env` admin — never commit passwords into code)

**Do not use** local divergent `backend/oauth/keycloak/realm-export.json` as runtime truth.

---

## 7. Explicit non-goals (this architecture)

- Professors domain API, PostgreSQL, Astah
- Inventing or owning root compose/`.env`
- Full mesh services (classes, courses, …) as implemented APIs
- UI / `keycloak-js` / browser calling Keycloak directly
- Prometheus / Mongo stacks for T1 oauth

---

## 8. Evolution

When professors work starts, add a sibling architecture doc (or a new section) — do not overload this file with domain ER diagrams. Keep identity gateway rules here stable.
