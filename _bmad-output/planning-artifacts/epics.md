---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories, step-04-final-validation]
inputDocuments:
  - _bmad-output/specs/spec-grupo07-keycloak-oauth/SPEC.md
  - _bmad-output/specs/spec-grupo07-keycloak-oauth/oauth-api.md
  - _bmad-output/specs/spec-grupo07-keycloak-oauth/keycloak-authz.md
---

# constrsw-2026-2 - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for **Grupo 07 Keycloak OAuth API** (`backend/oauth`), decomposing CAP-1 through CAP-8 from `SPEC-grupo07-keycloak-oauth` and companions `oauth-api.md` / `keycloak-authz.md` into implementable stories.

**Scope of this run:** Keycloak/oauth identity gateway only. Professors domain API, PostgreSQL, Astah, architecture spine, UX, and full-mesh FRs are out of scope. Professor-provided `docker-compose.yml` and `.env` are the runtime substrate — inventing compose is not a deliverable.

Traceability source (not a second source of truth): `_bmad-output/planning-artifacts/requirements-grupo07-t1-oauth-professors.md` — section C is excluded.

## Requirements Inventory

### Functional Requirements

FR1: A caller can `POST /login` with form-data `username` and `password` only (no client credentials). The API adds `client_id`, `client_secret`, and `grant_type=password` from env and calls Keycloak `POST {keycloak-base}/realms/constrsw/protocol/openid-connect/token`. (CAP-1)

FR2: Successful login returns HTTP `201` and JSON `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in` (brief spelling). Invalid structure → `400`; bad credentials → `401`. (CAP-1)

FR3: A caller can `POST /refresh` with form-data `refresh_token`. The API adds `client_id`, `client_secret`, and `grant_type=refresh_token` and posts to the same Keycloak token endpoint. Success `200` returns the same token field set as login (including `referesh_expires_in` if Keycloak returns refresh expiry). Invalid structure → `400`; invalid/expired refresh → `401`. (CAP-7)

FR4: An authenticated caller can `POST /users` with JSON `username` (email), `password`, `first-name`, `last-name`. Success `201` returns `{ id, username, first-name, last-name, enabled }` where `id` is taken from Keycloak `Location`. Invalid structure or username failing RFC 5322 email validation → `400`; duplicate username → `409`. (CAP-2)

FR5: An authenticated caller can `GET /users` and receive `200` with a list of `{ id, username, first-name, last-name, enabled }`, optionally filtered by `?enabled=true|false`. (CAP-2)

FR6: An authenticated caller can `GET /users/{{id}}` and receive `200` with the same user object shape; missing user → `404`. (CAP-2)

FR7: An authenticated caller can `PUT /users/{{id}}` with updated user attributes; success `200` empty body; missing user → `404`. (CAP-2)

FR8: An authenticated caller can `PATCH /users/{{id}}` with `{ "password": "..." }`; success `200` empty body; missing user → `404`. (CAP-2)

FR9: An authenticated caller can `DELETE /users/{{id}}` as a soft delete (disable the user in Keycloak, not hard-delete); success `204` empty body; missing user → `404`. (CAP-2)

FR10: All `/users` routes require `Authorization: Bearer {{access_token}}`. Missing/invalid token → `401`; insufficient permission → `403`; bad structure → `400`. (CAP-2)

FR11: An authenticated caller can manage roles via `POST /roles`, `GET /roles`, `GET /roles/{{id}}`, `PUT /roles/{{id}}`, `PATCH /roles/{{id}}`, and `DELETE /roles/{{id}}`. `DELETE` is a logical delete (mark inactive in Keycloak, not hard-delete). (CAP-3)

FR12: An authenticated caller can assign a role to a user and unassign a role from a user. Path naming is left to implementation and must be documented in the oauth service README when chosen. (CAP-3)

FR13: Role routes return `401`/`403` for missing/unauthorized token, `404` for missing entity, and `400` for bad payload. Target client role names for authz alignment on client `oauth`: `administrator`, `coordinator`, `professor`, `student`. (CAP-3)

FR14: Every oauth API error response uses body `{ error_code, error_description, error_source, error_stack }` with `error_source` such as `OAuthAPI`; `error_code` relays Keycloak’s code unless a brief-specific code applies; `error_stack` chains to the root cause. (CAP-4)

FR15: Realm `constrsw` client `oauth` exposes the authorization model: client roles `administrator`, `coordinator`, `professor`, `student`; eight resources with URLs (`classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`); Role policies `administrator-policy`, `coordinator-policy`, `professor-policy`, `student-policy` bound to the matching client role (filter by client `oauth`); resource permissions `administrator-permissions` (resources, rooms, professors, students), `coordinator-permissions` (courses, classes), `professor-permissions` (lessons, reservations) — each with `decisionStrategy: AFFIRMATIVE` and the **multi-policy** bindings shipped in the professor's `constrsw.json` (`coordinator-permissions` also applies `administrator-policy`; `professor-permissions` also applies `coordinator-policy` and `administrator-policy`). Those extra bindings are intentional and **must not** be removed. Configuration is verified on the professor-imported realm. (CAP-5)

FR16: A caller can `POST /authz/validate` with `Authorization: Bearer {{access_token}}` and JSON `{ "resource": "<name>" }` (one of the eight resource names). The API must call **Keycloak Authorization Services** (not a local role matrix). `200` when permitted, `403` when forbidden, `401` if token missing/invalid, `400` for unknown resource name or bad structure. Outcomes must match the **realm-derived** matrix (hierarchical, from the `AFFIRMATIVE` multi-policy bindings in FR15): administrator → all eight resources; coordinator → courses, classes, lessons, reservations; professor → lessons, reservations; student → none. The T1 brief's disjoint table is **not** the oracle — see `keycloak-authz.md` § "Divergence from the T1 brief". (CAP-6)

FR17: A Dockerfile in `backend/oauth` builds the API image; the professor-provided root `docker-compose.yml` already defines/enables the `oauth` service — the group verifies it builds and runs against professor `.env` (does not invent compose). Runtime reads Keycloak settings from professor env var names (`KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, and related `KEYCLOAK_*` / `OAUTH_*`). (CAP-8)

### NonFunctional Requirements

NFR1: Implement on the group branch inside submodule `backend/oauth`; add the Dockerfile there.

NFR2: Realm is `constrsw`; client is `oauth` (confidential); email is username.

NFR3: Prefer Keycloak 26 URL style without `/auth` (`…/realms/{realm}/…`). If the professor base URL includes `/auth`, use that base instead. Verify on first bring-up.

NFR4: Login body from clients is username + password only; client credentials stay server-side.

NFR5: Preserve response field spelling `referesh_expires_in` as in the brief.

NFR6: Login success status is `201` (T1), not Keycloak’s native `200`. Refresh success status is `200`.

NFR7: Soft-delete users (disable) and logically delete roles; do not hard-delete unless the brief requires otherwise.

NFR8: Authz roles are **client roles** on `oauth`; filter by client when binding policies. Canonical names are `administrator`, `coordinator`, `professor`, `student`. If a future realm import diverges, stop and realign before coding policies.

NFR9: CAP-6 must evaluate via Keycloak Authorization Services. The **realm-derived** matrix in `keycloak-authz.md` is the expected-outcome oracle for tests, not a local decision engine and not the brief's disjoint table.

NFR10: Authorization resources, policies, permissions, and the validate endpoint are in MVP for this SPEC.

NFR11: The eight resource URLs identify Keycloak Authorization Services resources; they do not imply implementing domain APIs (classes, courses, lessons, rooms, and so on).

NFR12: `.env` and `docker-compose.yml` are professor-provided. Plug in — do not invent compose as a group deliverable.

NFR13: Assume modern Keycloak (≈26.x, e.g. `quay.io/keycloak/keycloak`) with realm import for `constrsw`/`oauth`, not historical `jboss/keycloak`. Client secret and Admin API credentials for user/role CRUD come from the provided `.env`.

NFR14: Out of scope: professors domain API, PostgreSQL, Astah schemas/examples; architecture spine artifacts; full course mesh services; Prometheus, MongoDB, or unrelated observability; custom BMAD agents.

### Additional Requirements

- No starter/greenfield application template is specified in this SPEC; work proceeds in existing submodule `backend/oauth`.
- Keycloak token endpoint: `POST {keycloak-base}/realms/{realm}/protocol/openid-connect/token` (form urlencoded). UserInfo reference: `GET {keycloak-base}/realms/{realm}/protocol/openid-connect/userinfo` with Bearer token.
- Exact URL strings on the eight Keycloak resources may follow a professor template if one ships with compose; otherwise use stable path-like URLs from `keycloak-authz.md` (`/classes`, `/courses`, `/lessons`, `/professors`, `/reservations`, `/resources`, `/rooms`, `/students`).
- CAP-5 objects are configured or verified **on top of** the professor-imported realm; do not treat owning realm JSON / compose as a git deliverable.
- Assign/unassign role path naming is implementation-defined and must be documented in the oauth service README when chosen.
- CAP-6 path/body default: `POST /authz/validate` with Bearer token and JSON `{ "resource": "<name>" }`.
- CAP-7 path default: `POST /refresh` with form-data `refresh_token`.
- `student` may exist as a client role without a dedicated permission set in B.2 (`student` → no allowed resources in the validate matrix).

### UX Design Requirements

None. This SPEC is an HTTP API with no UI; UX `DESIGN.md` / `EXPERIENCE.md` are excluded from this run.

### FR Coverage Map

FR1: Epic 2 - Login exchanges username/password for Keycloak tokens (no client credentials from caller)
FR2: Epic 2 - Login returns 201 and token fields including `referesh_expires_in`; 400/401 on failure
FR3: Epic 2 - Refresh tokens via `POST /refresh` with 200 success and 400/401 on failure
FR4: Epic 4 - Create user (`POST /users`) with RFC 5322 and duplicate handling
FR5: Epic 4 - List users (`GET /users`) with optional `enabled` filter
FR6: Epic 4 - Get user by id (`GET /users/{{id}}`)
FR7: Epic 4 - Update user attributes (`PUT /users/{{id}}`)
FR8: Epic 4 - Change user password (`PATCH /users/{{id}}`)
FR9: Epic 4 - Soft-delete user (`DELETE /users/{{id}}` disables in Keycloak)
FR10: Epic 4 - Bearer auth on all user routes (`401`/`403`/`400`)
FR11: Epic 5 - Roles CRUD including logical delete
FR12: Epic 5 - Assign and unassign roles to users (paths documented in README)
FR13: Epic 5 - Role route auth/error codes and canonical client role names
FR14: Epic 3 - Uniform OA error body on every oauth API error
FR15: Epic 6 - B.2 authz model on realm `constrsw` client `oauth`
FR16: Epic 6 - `POST /authz/validate` via Keycloak Authorization Services
FR17: Epic 1 - Dockerfile + verify wiring into professor-provided compose/`.env`

## Epic List

### Epic 1: Run oauth on the professor Keycloak stack
The API image builds from `backend/oauth` and runs against the professor-provided compose/`.env` (already on `main`; oauth service enabled). Verify wiring — no invented compose.
**FRs covered:** FR17

### Epic 2: Sign in and renew access
A caller gets Keycloak tokens with username/password only, then refreshes without re-entering the password. Login `201`, refresh `200`, `referesh_expires_in` preserved.
**FRs covered:** FR1, FR2, FR3

### Epic 3: Surface uniform oauth error diagnostics
Every error returns `{ error_code, error_description, error_source, error_stack }` so clients and graders see a consistent OA body. Token routes are the first consumers; later APIs reuse the same mapper.
**FRs covered:** FR14

### Epic 4: Administer Keycloak users
Authenticated callers create, list (optional `enabled`), get, update, change password, and soft-delete users, with Bearer `401`/`403`, RFC 5322 `400`, and duplicate `409`.
**FRs covered:** FR4, FR5, FR6, FR7, FR8, FR9, FR10

### Epic 5: Administer roles and assignments
Authenticated callers CRUD roles (logical delete) and assign/unassign roles to users; paths documented in the oauth README.
**FRs covered:** FR11, FR12, FR13

### Epic 6: Authorize and validate resource access
B.2 authz largely present in professor `constrsw.json` — **verify/gap-fill** (6.1–6.4); implement `POST /authz/validate` via Keycloak Authorization Services (6.5).
**FRs covered:** FR15, FR16

## Epic 1: Run oauth on the professor Keycloak stack

The API image builds from `backend/oauth` and runs against the professor-provided compose/`.env` (realm `constrsw`, client `oauth`). Professor stack is already on `main` (`docker-compose.yml`, `.env`, `infrastructure/dev.local/services/keycloak/constrsw.json`). No invented compose.

**FRs covered:** FR17  
**NFRs:** NFR1, NFR2, NFR3, NFR12, NFR13

### Story 1.1: Load Keycloak settings from professor environment

As a group developer,
I want the oauth process to read Keycloak URL, realm, client id, client secret, and related params from the professor-provided environment,
So that the API can target realm `constrsw` / client `oauth` without hard-coded secrets or an invented `.env`.

**Acceptance Criteria:**

**Given** the professor-provided `.env` (or equivalent process environment) supplies at least:
`KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`
(and related `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` / `OAUTH_*` as needed)
**When** the oauth service starts
**Then** it uses those values for Keycloak calls (realm default `constrsw`, client default `oauth`)
**And** it does not commit alternate secrets or invent a group-owned `.env` as a deliverable
**And** config binding documents these professor env names in the oauth README

**Given** the Keycloak base URL is present (`KEYCLOAK_SERVER_URL`, e.g. `http://keycloak:8080`)
**When** the service builds Keycloak request URLs
**Then** it uses `{keycloak-base}/realms/{realm}/…` without inserting `/auth`
**And** if the provided base URL already includes `/auth`, it uses that base as-is (NFR3)

### Story 1.2: Dockerfile for the oauth API image

As a group developer,
I want a Dockerfile in `backend/oauth` that builds the API image,
So that the professor compose can run our service as a container.

**Acceptance Criteria:**

**Given** the oauth source lives in submodule `backend/oauth` on the group branch
**When** `docker build` is run against that Dockerfile
**Then** an image is produced that starts the oauth API process
**And** the Dockerfile is added in `backend/oauth` (not at repo root as a substitute for compose)
**And** the image includes a minimal runnable stub (NestJS preferred unless professor mandated Spring) exposing `GET /health` returning HTTP 200 — required by the professor compose healthcheck (`node` + `/health` on `OAUTH_INTERNAL_API_PORT`)
**And** the stack choice is documented in the oauth README

**Given** the image is built
**When** the container starts with professor-provided env vars
**Then** the process receives Keycloak URL, realm, client id/secret, and related params from the environment (Story 1.1)
**And** the group does not add a root `docker-compose.yml` as a deliverable

### Story 1.3: Verify oauth runs on professor-provided compose

As a group developer,
I want the professor-provided `oauth` service to build and run our image against Keycloak,
So that graders can start the stack without a group-invented compose file.

**Acceptance Criteria:**

**Given** root `docker-compose.yml` from the professor already defines service `oauth` (build `./backend/oauth`, env from `.env`) — **already enabled; do not invent or replace compose**
**When** this story is done
**Then** `docker compose build oauth` (or equivalent) succeeds using the Dockerfile from Story 1.2
**And** with external volume `constrsw-keycloak-data` created as documented by the professor, the stack can start Keycloak + oauth
**And** the oauth container becomes healthy (`GET /health`) and can resolve Keycloak via `KEYCLOAK_SERVER_URL`
**And** no new compose file is invented as a group deliverable

**Given** the professor Keycloak README / compose ops notes
**When** the oauth README is updated
**Then** it documents: create `constrsw-keycloak-data`, console at `:8081`, API at `:8181`, and that login contract for *our* API remains SPEC/T1 (form-data, `201`) even if the Keycloak README shows a JSON curl example

## Epic 2: Sign in and renew access

A caller gets Keycloak tokens with username/password only, then refreshes without re-entering the password. Login `201`, refresh `200`, `referesh_expires_in` preserved.

**FRs covered:** FR1, FR2, FR3  
**NFRs:** NFR4, NFR5, NFR6

Epic 3 owns the uniform OA error body. Stories 2.1 and 2.2 may return simple or empty bodies on `400`/`401`.

### Story 2.1: Login with username and password

As an API caller,
I want to `POST /login` with form-data `username` and `password` only,
So that I receive Keycloak tokens without sending client credentials.

**Acceptance Criteria:**

**Given** valid credentials for a user in realm `constrsw`
**When** the caller `POST /login` with `username` and `password` only (no `client_id` / `client_secret` / `grant_type`) as `multipart/form-data` **or** `application/x-www-form-urlencoded`
**Then** the API adds `client_id`, `client_secret`, and `grant_type=password` from env and calls `POST {keycloak-base}/realms/constrsw/protocol/openid-connect/token`
**And** the response is HTTP `201` with JSON `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in`
**And** `referesh_expires_in` is mapped from Keycloak’s `refresh_expires_in` (NFR5)

**Given** the request body is missing `username` or `password`, or is not valid form-data / urlencoded
**When** the caller `POST /login`
**Then** the API returns `400`
**And** the body may be simple or empty (OA envelope is Story 3.x)

**Given** username/password do not authenticate at Keycloak
**When** the caller `POST /login`
**Then** the API returns `401`
**And** the body may be simple or empty (OA envelope is Story 3.x)

### Story 2.2: Refresh tokens without re-entering password

As an API caller,
I want to `POST /refresh` with a valid `refresh_token`,
So that I can obtain new tokens without sending my password again.

**Acceptance Criteria:**

**Given** a valid `refresh_token` issued by Keycloak for client `oauth`
**When** the caller `POST /refresh` with `refresh_token` as `multipart/form-data` **or** `application/x-www-form-urlencoded`
**Then** the API adds `client_id`, `client_secret`, and `grant_type=refresh_token` from env and posts to the same Keycloak token endpoint as login
**And** the response is HTTP `200` with the same token field set as login
**And** `referesh_expires_in` is mapped from Keycloak’s `refresh_expires_in` when Keycloak returns refresh expiry (NFR5)

**Given** the request is missing `refresh_token` or is not valid form-data / urlencoded
**When** the caller `POST /refresh`
**Then** the API returns `400`
**And** the body may be simple or empty (OA envelope is Story 3.x)

**Given** the `refresh_token` is invalid or expired
**When** the caller `POST /refresh`
**Then** the API returns `401`
**And** the body may be simple or empty (OA envelope is Story 3.x)

## Epic 3: Surface uniform oauth error diagnostics

Every error returns `{ error_code, error_description, error_source, error_stack }` so clients and graders see a consistent OA body. Token routes are the first consumers; later APIs reuse the same mapper.

**FRs covered:** FR14

### Story 3.1: OA error envelope and mapper

As an API caller or grader,
I want a single error shape for the oauth API,
So that every failure is diagnosable the same way.

**Acceptance Criteria:**

**Given** the oauth API produces an error response
**When** the shared mapper serializes it
**Then** the JSON body is `{ error_code, error_description, error_source, error_stack }`
**And** `error_source` is `OAuthAPI` (or an equally specific oauth-API source)
**And** `error_description` is a group-provided human message
**And** `error_stack` is a JSON **array of objects**, chaining errors down to the root cause

**Given** Keycloak returned an error code
**When** the mapper sets `error_code`
**Then** it relays Keycloak’s code unless a brief-specific code applies

**Given** the failure is local (structure/validation) and not a Keycloak code
**When** the mapper sets `error_code`
**Then** it uses a documented brief-specific convention (e.g. `OA-400` for bad structure) so Users/Roles stories do not invent new code families
**And** that convention is recorded in the oauth service README (or equivalent)

**Given** subsequent stories return HTTP errors
**When** they use this mapper
**Then** they must not invent a different error JSON shape

### Story 3.2: OA error body on login and refresh failures

As an API caller,
I want `400`/`401` from `/login` and `/refresh` to use the OA envelope,
So that token-route failures match the grader contract.

**Acceptance Criteria:**

**Given** `POST /login` fails with bad structure
**When** the API returns `400`
**Then** the body is the OA envelope from Story 3.1 (not empty / ad-hoc)
**And** `error_code` follows the brief-specific convention (e.g. `OA-400`) when Keycloak was not called or did not supply a code
**And** `error_stack` is a JSON array of objects

**Given** `POST /login` fails with bad credentials
**When** the API returns `401`
**Then** the body is the OA envelope from Story 3.1
**And** `error_code` relays Keycloak’s code when present

**Given** `POST /refresh` fails with bad structure
**When** the API returns `400`
**Then** the body is the OA envelope from Story 3.1
**And** `error_stack` is a JSON array of objects

**Given** `POST /refresh` fails with invalid or expired `refresh_token`
**When** the API returns `401`
**Then** the body is the OA envelope from Story 3.1
**And** `error_code` relays Keycloak’s code when present
**And** `error_stack` includes the upstream/root cause as objects in the array

## Epic 4: Administer Keycloak users

Authenticated callers create, list (optional `enabled`), get, update, change password, and soft-delete users, with Bearer `401`/`403`, RFC 5322 `400`, and duplicate `409`.

**FRs covered:** FR4, FR5, FR6, FR7, FR8, FR9, FR10  
**NFRs:** NFR2, NFR7

Stories 4.2–4.6 reuse Bearer `401`/`403` rules from Story 4.1 (same `Authorization: Bearer` check and OA envelope). They do not redefine the auth mechanism.

### Story 4.1: Create user

As an authenticated caller,
I want to `POST /users` with email username, password, and name,
So that a Keycloak user is created and I get its id.

**Acceptance Criteria:**

**Given** a valid Bearer access token
**When** the caller `POST /users` with JSON `username` (email), `password`, `first-name`, `last-name`
**Then** the API creates the user in Keycloak and returns `201` with `{ id, username, first-name, last-name, enabled }`
**And** `id` is taken from Keycloak’s `Location` header

**Given** `username` is not a valid email per RFC 5322, or the body is missing required fields
**When** the caller `POST /users`
**Then** the API returns `400` with the OA envelope (`error_code` e.g. `OA-400` if not relaying Keycloak)

**Given** that `username` already exists
**When** the caller `POST /users`
**Then** the API returns `409` with the OA envelope

**Given** the Authorization header is missing or the token is invalid
**When** the caller `POST /users`
**Then** the API returns `401` with the OA envelope

**Given** a valid token that is not permitted to create users
**When** the caller `POST /users`
**Then** the API returns `403` with the OA envelope

### Story 4.2: List users

As an authenticated caller,
I want to `GET /users` with an optional `enabled` filter,
So that I can see Keycloak users in the oauth object shape.

**Acceptance Criteria:**

**Given** a valid Bearer token
**When** the caller `GET /users`
**Then** the API returns `200` with a list of `{ id, username, first-name, last-name, enabled }`

**Given** a valid Bearer token
**When** the caller `GET /users?enabled=true` or `?enabled=false`
**Then** the list contains only users matching that enabled state

**Given** `enabled` is present but not `true` or `false`
**When** the caller `GET /users`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `GET /users`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 4.3: Get user by id

As an authenticated caller,
I want to `GET /users/{{id}}`,
So that I can read one user’s oauth representation.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing user id
**When** the caller `GET /users/{{id}}`
**Then** the API returns `200` with `{ id, username, first-name, last-name, enabled }`

**Given** the id does not exist
**When** the caller `GET /users/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** a malformed id
**When** the caller `GET /users/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `GET /users/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 4.4: Update user attributes

As an authenticated caller,
I want to `PUT /users/{{id}}` with updated attributes,
So that the Keycloak user is replaced/updated.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing user id
**When** the caller `PUT /users/{{id}}` with allowed attributes `first-name`, `last-name`, and/or `enabled`
**Then** the API returns `200` with an empty body
**And** `username` is immutable via PUT (email-as-username identity)
**And** the oauth README lists allowed PUT attributes and states that `username` is not updatable on this route

**Given** the id does not exist
**When** the caller `PUT /users/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** the body is invalid or includes disallowed fields (including a `username` change)
**When** the caller `PUT /users/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `PUT /users/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 4.5: Change user password

As an authenticated caller,
I want to `PATCH /users/{{id}}` with a new password,
So that the user’s Keycloak credential is updated.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing user id
**When** the caller `PATCH /users/{{id}}` with JSON `{ "password": "..." }`
**Then** the API returns `200` with an empty body

**Given** the id does not exist
**When** the caller `PATCH /users/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** the body is missing `password` or is invalid
**When** the caller `PATCH /users/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `PATCH /users/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 4.6: Soft-delete user

As an authenticated caller,
I want to `DELETE /users/{{id}}`,
So that the user is disabled in Keycloak rather than hard-deleted.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing enabled user
**When** the caller `DELETE /users/{{id}}`
**Then** the API disables the user in Keycloak (does not hard-delete)
**And** the API returns `204` with an empty body

**Given** a valid Bearer token and a user who is already disabled
**When** the caller `DELETE /users/{{id}}`
**Then** the API returns `204` (idempotent: user remains disabled; never hard-deleted)

**Given** the id does not exist
**When** the caller `DELETE /users/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** a malformed id
**When** the caller `DELETE /users/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `DELETE /users/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

## Epic 5: Administer roles and assignments

Authenticated callers CRUD roles (logical delete) and assign/unassign roles to users; paths documented in the oauth README.

**FRs covered:** FR11, FR12, FR13  
**NFRs:** NFR7, NFR8

Canonical client role names on `oauth` are **`administrator`**, **`coordinator`**, **`professor`**, **`student`** (SPEC / B.2). Do not use `funcionario` / `coordenador` or other translations.

Assign/unassign paths are pinned in Story 5.7 and reused in 5.8 — no floating paths across PRs.

Stories 5.2–5.8 reuse Bearer `401`/`403` rules from Story 4.1.

### Story 5.1: Create role

As an authenticated caller,
I want to `POST /roles`,
So that a client role exists on `oauth` in realm `constrsw`.

**Acceptance Criteria:**

**Given** a valid Bearer token
**When** the caller `POST /roles` with a documented JSON body (at least a role `name`)
**Then** the API creates a **client role** on client `oauth` and returns a success status with the created role representation (shape documented in the oauth README)
**And** canonical names `administrator`, `coordinator`, `professor`, `student` are valid `name` values (not `funcionario` / `coordenador`)

**Given** the body is missing required fields or is invalid
**When** the caller `POST /roles`
**Then** the API returns `400` with the OA envelope

**Given** a role with that `name` already exists on client `oauth`
**When** the caller `POST /roles`
**Then** the API returns `409` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `POST /roles`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.2: List roles

As an authenticated caller,
I want to `GET /roles`,
So that I can see client roles on `oauth`.

**Acceptance Criteria:**

**Given** a valid Bearer token
**When** the caller `GET /roles`
**Then** the API returns `200` with a list of roles (shape documented in README), filtered to client `oauth` when binding to Keycloak

**Given** missing/invalid token or insufficient permission
**When** the caller `GET /roles`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.3: Get role by id

As an authenticated caller,
I want to `GET /roles/{{id}}`,
So that I can read one role.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing role id
**When** the caller `GET /roles/{{id}}`
**Then** the API returns `200` with that role’s representation

**Given** the id does not exist
**When** the caller `GET /roles/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** a malformed id
**When** the caller `GET /roles/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `GET /roles/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.4: Replace role

As an authenticated caller,
I want to `PUT /roles/{{id}}`,
So that role attributes are replaced/updated.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing role id
**When** the caller `PUT /roles/{{id}}` with a documented body
**Then** the API returns success (`200`) with empty or documented body
**And** allowed PUT attributes are listed in the oauth README

**Given** the id does not exist
**When** the caller `PUT /roles/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** the body is invalid
**When** the caller `PUT /roles/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `PUT /roles/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.5: Partially update role

As an authenticated caller,
I want to `PATCH /roles/{{id}}`,
So that I can change a subset of role attributes.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing role id
**When** the caller `PATCH /roles/{{id}}` with a documented partial body
**Then** the API returns `200` with empty or documented body
**And** allowed PATCH attributes are listed in the oauth README

**Given** the id does not exist
**When** the caller `PATCH /roles/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** the body is invalid
**When** the caller `PATCH /roles/{{id}}`
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `PATCH /roles/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.6: Logical-delete role

As an authenticated caller,
I want to `DELETE /roles/{{id}}`,
So that the role is marked inactive in Keycloak, not hard-deleted.

**Acceptance Criteria:**

**Given** a valid Bearer token and an existing active role
**When** the caller `DELETE /roles/{{id}}`
**Then** the API logically deletes the role (does not hard-delete)
**And** Keycloak has no native “disabled role”: the oauth README defines how inactivity is represented (prefix, attribute, or other soft-flag) so graders and Epic 6 do not assume hard-delete
**And** the API returns success with empty body (`204` unless README documents otherwise)

**Given** the role is already logically deleted
**When** the caller `DELETE /roles/{{id}}`
**Then** the API returns `204` (idempotent: remains inactive per the README representation; never hard-deleted)

**Given** the id does not exist
**When** the caller `DELETE /roles/{{id}}`
**Then** the API returns `404` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller `DELETE /roles/{{id}}`
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.7: Assign role to user

As an authenticated caller,
I want to assign a role to a user,
So that the user holds that client role on `oauth`.

**Acceptance Criteria:**

**Given** a valid Bearer token, an existing user, and an existing client role on `oauth`
**When** the caller `POST /users/{{id}}/roles` with a documented body identifying the role
**Then** the role is assigned to the user in Keycloak
**And** the oauth README pins this path (or one chosen equivalent, documented once) so later PRs do not introduce a second assign URL

**Given** the user or role does not exist
**When** the caller invokes assign
**Then** the API returns `404` with the OA envelope

**Given** the payload is invalid
**When** the caller invokes assign
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller invokes assign
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

### Story 5.8: Unassign role from user

As an authenticated caller,
I want to unassign a role from a user,
So that the user no longer holds that client role on `oauth`.

**Acceptance Criteria:**

**Given** a valid Bearer token and a user who currently has the role
**When** the caller `DELETE /users/{{id}}/roles/{{roleId}}` (same pinning as Story 5.7 / oauth README)
**Then** the role is removed from the user in Keycloak
**And** no alternate unassign path is introduced in this PR

**Given** the user or role does not exist
**When** the caller invokes unassign
**Then** the API returns `404` with the OA envelope

**Given** the user does not currently have the role
**When** the caller invokes unassign
**Then** the operation is idempotent success (`204` or documented equivalent) — not a hard failure unless Keycloak requires otherwise

**Given** the payload is invalid
**When** the caller invokes unassign
**Then** the API returns `400` with the OA envelope

**Given** missing/invalid token or insufficient permission
**When** the caller invokes unassign
**Then** the API returns `401` or `403` with the OA envelope (Bearer rules from Story 4.1)

## Epic 6: Authorize and validate resource access

B.2 client roles, eight resources, policies, and permissions exist on the imported realm; `POST /authz/validate` uses Keycloak Authorization Services (not a local matrix).

**Professor import note (2026-09-03, re-verified 2026-09-09):** `constrsw.json` already includes the four `oauth` client roles, eight named resources with URLs, Authz enabled on client `oauth`, four role policies (`administrator-policy`, `coordinator-policy`, `professor-policy`, `student-policy`) and three resource permissions. Stories **6.1–6.4 are verify-first**: confirm in a running Keycloak console after volume import; **create or fix only genuinely missing objects**. Do not replace professor realm JSON as a group deliverable. Story **6.5** implements the API.

**Matrix decision (2026-09-09):** the realm's permissions apply **more than one policy each** (`AFFIRMATIVE`), so real access is hierarchical (`administrator` ⊇ `coordinator` ⊇ `professor`), unlike the disjoint table in the Moodle brief. **The realm wins** — it is this semester's file (committed 2026-09-02), while the brief's screenshots still show realm `constr-sw-2022-2` / client `grupo1`. Rationale and the brief's original table are preserved in `keycloak-authz.md` § "Divergence from the T1 brief".

**FRs covered:** FR15, FR16  
**NFRs:** NFR8, NFR9, NFR10, NFR11

How Stories 6.1–6.4 are verified/applied (console checklist, overlay, or Admin API) is documented in the oauth README. That is **not** a group-owned compose or realm-JSON deliverable.

### Story 6.1: Ensure B.2 client roles on `oauth`

As a group developer,
I want client roles `administrator`, `coordinator`, `professor`, and `student` on client `oauth` in realm `constrsw`,
So that policies can bind to the SPEC names (not `funcionario` / `coordenador`).

**Acceptance Criteria:**

**Given** the professor-imported realm (`infrastructure/dev.local/services/keycloak/constrsw.json`) for `constrsw` / `oauth`
**When** this story is done
**Then** those four **client** roles are **verified** present on client `oauth` (create only if missing)
**And** if the running import uses different names, work **stops** and realigns before coding policies (NFR8)
**And** the oauth README documents the verification result (and any gap-fill steps)

**Given** Story 5.6’s logical-delete representation
**When** listing roles for authz binding
**Then** B.2 policy binding uses the canonical active role names, not a hard-deleted or undocumented inactive form

### Story 6.2: Authorization resources with URLs

As a group developer,
I want eight Keycloak Authorization Services resources on client `oauth`,
So that permissions can target named resources with URLs.

**Acceptance Criteria:**

**Given** client `oauth` in realm `constrsw`
**When** this story starts
**Then** Authorization Services is **verified** enabled on client `oauth` (`authorizationServicesEnabled` or equivalent)

**Given** Authorization Services is enabled
**When** this story is done
**Then** resources are **verified** (create only gaps): `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`
**And** each has a URL (as imported or defaults from `keycloak-authz.md`)
**And** these URLs identify Authz resources only — this SPEC does **not** implement those domain APIs (NFR11)
**And** the oauth README documents verification / gap-fill — not a group-owned compose or realm-JSON deliverable

### Story 6.3: Role policies on client `oauth`

As a group developer,
I want one Role policy per B.2 permission-bearing role,
So that permissions can evaluate membership of `oauth` client roles.

**Acceptance Criteria:**

**Given** the four client roles from Story 6.1
**When** this story is done
**Then** policies are **verified** (create only gaps): `administrator-policy` → `administrator`, `coordinator-policy` → `coordinator`, `professor-policy` → `professor`
**And** each policy filters by client `oauth` when selecting the role
**And** `student` may exist as a role **without** a dedicated B.2 permission set (import may also contain `student-policy` — document what is present; B.2 matrix still treats student as no resource grants)
**And** the oauth README documents verification / gap-fill

### Story 6.4: Resource-based permissions

As a group developer,
I want the three B.2 permission sets on client `oauth`,
So that Keycloak Authorization Services can allow or deny named resources.

**Acceptance Criteria:**

**Given** resources from Story 6.2 and policies from Story 6.3
**When** this story is done
**Then** permission bindings are **verified** in the running realm (create/fix only genuinely missing objects), each with `decisionStrategy: AFFIRMATIVE`:  
`administrator-permissions` → `resources`, `rooms`, `professors`, `students` via `administrator-policy`;  
`coordinator-permissions` → `courses`, `classes` via `coordinator-policy` **+ `administrator-policy`**;  
`professor-permissions` → `lessons`, `reservations` via `professor-policy` **+ `coordinator-policy` + `administrator-policy`**
**And** those extra policies are recorded as **intentional** (professor's realm wins over the brief — see `keycloak-authz.md` § "Divergence from the T1 brief"); they **must not** be removed to match the brief
**And** note: the professor export may list `*-permissions` under `policies` with an empty top-level `permissions` array — confirm effective bindings in Admin Console after import
**And** configuration remains present in or applied **atop** the professor-imported realm (not a group-owned compose/realm-JSON deliverable)
**And** the oauth README documents verification / gap-fill

### Story 6.5: Validate resource access via Keycloak Authorization Services

As an API caller,
I want to `POST /authz/validate` with a Bearer token and a resource name,
So that I learn whether that token is permitted — decided by Keycloak, not a local role matrix.

**Acceptance Criteria:**

**Given** a valid access token and JSON `{ "resource": "<name>" }` where name is one of the eight resources
**When** the caller `POST /authz/validate` with `Authorization: Bearer {{access_token}}`
**Then** the API calls **Keycloak Authorization Services** (token/permission evaluation against configured resources/policies)
**And** Keycloak Authorization Services is the **decision engine**
**And** the B.2 matrix is the **test oracle only** — not a local allow/deny implementation

**Given** Keycloak permits access
**When** validate runs
**Then** the API returns `200`

**Given** Keycloak denies access
**When** validate runs
**Then** the API returns `403` with the OA envelope

**Given** the token is missing or invalid
**When** the caller `POST /authz/validate`
**Then** the API returns `401` with the OA envelope (Bearer rules from Story 4.1)

**Given** the body is malformed or `resource` is not one of the eight names
**When** the caller `POST /authz/validate`
**Then** the API returns `400` with the OA envelope (`error_code` e.g. `OA-400` if not relaying Keycloak)

**Given** the realm-derived matrix in `keycloak-authz.md`
**When** tests run against professor Keycloak
**Then** at least one permitted pair returns `200` and at least one forbidden pair returns `403`
**And** outcomes match the **hierarchical** grants: administrator → **all eight** resources; coordinator → `courses`, `classes`, `lessons`, `reservations`; professor → `lessons`, `reservations`; student → **none**
**And** no test asserts a `403` for administrator or coordinator on a resource the realm actually grants (the brief's disjoint table is not the oracle)
