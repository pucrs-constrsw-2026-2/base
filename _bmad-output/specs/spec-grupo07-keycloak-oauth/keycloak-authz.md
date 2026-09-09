# keycloak-authz — realm, client, policies, validate

Companion to `SPEC-grupo07-keycloak-oauth`. Load-bearing authorization contract for CAP-5–CAP-6.

## Substrate

- Realm: `constrsw`
- Client: `oauth` (confidential; secret from professor `.env` as `KEYCLOAK_CLIENT_SECRET`)
- Email as username
- Professor stack: Keycloak **26.0.1** image under `infrastructure/dev.local/services/keycloak`; import file `constrsw.json` via `start-dev --import-realm`; historical `jboss/keycloak` is **superseded**
- **Verify-first:** the import already ships B.2 client roles, eight resources, Authz enabled, and named policies — confirm in Admin Console after volume import; create/fix **only gaps**. Do not invent root compose or replace professor realm JSON as a deliverable.

## URL base (assumption to verify)

Use `{keycloak-base}/realms/constrsw/...` **without** `/auth` unless the provided base URL already includes `/auth`.

## CAP-5 — Access control model (client `oauth`)

### Roles (client roles)

| Role |
| --- |
| `administrator` |
| `coordinator` |
| `professor` |
| `student` |

Canonical contract is this English B.2 set; confirmed to match the intended realm. If a future import diverges, notify and realign before wiring policies.

### Authorization resources (each with a URL)

| Resource | Default URL (if professor does not prescribe) |
| --- | --- |
| `classes` | `/classes` |
| `courses` | `/courses` |
| `lessons` | `/lessons` |
| `professors` | `/professors` |
| `reservations` | `/reservations` |
| `resources` | `/resources` |
| `rooms` | `/rooms` |
| `students` | `/students` |

URLs identify the resource in Keycloak Authorization Services; they do **not** imply implementing those domain APIs in this SPEC.

### Role policies

One **Role** policy per role, filtering by client `oauth` when selecting the role:

| Policy | Bound role |
| --- | --- |
| `administrator-policy` | `administrator` |
| `coordinator-policy` | `coordinator` |
| `professor-policy` | `professor` |

(`student` may exist as a role without a dedicated permission set in B.2.)

### Resource-based permissions

**Source of truth: the professor's `constrsw.json`** (see "Divergence from the T1 brief" below).
All three permissions use `decisionStrategy: AFFIRMATIVE` — **any** applied policy granting is enough.

| Permission | Resources | Applied policies |
| --- | --- | --- |
| `administrator-permissions` | `resources`, `rooms`, `professors`, `students` | `administrator-policy` |
| `coordinator-permissions` | `courses`, `classes` | `coordinator-policy`, `administrator-policy` |
| `professor-permissions` | `lessons`, `reservations` | `professor-policy`, `coordinator-policy`, `administrator-policy` |

`student-policy` exists in the realm but is **not** applied to any permission.

### Divergence from the T1 brief (group decision, 2026-09-09)

Item 4 of the Moodle brief lists **one** policy per permission (`coordinator-permissions` → `coordinator-policy` only, `professor-permissions` → `professor-policy` only). The professor's imported realm applies **several**, producing hierarchical access instead of disjoint sets.

**We follow the realm, not the brief.** Rationale:

- `constrsw.json` was committed by the professor on **2026-09-02** (`51423d2`) for **this** semester, and the surrounding commits (`10e6838`, `1791eab`) show the stack was actively being adjusted that same day.
- The brief's screenshots show realm `constr-sw-2022-2` and client `grupo1` — material re-uploaded from 2022, superseded by the current `constrsw` / `oauth` realm.
- `SPEC.md` already forbids owning or rewriting the professor's realm JSON; matching the brief would mean **editing his configuration**.

**Consequence for implementers:** the multi-policy bindings are **intentional, not gaps**. Do **not** remove `administrator-policy` / `coordinator-policy` from the permissions to make the realm match the brief. The brief's table is kept below as a historical note only.

| Brief's table (historical — NOT the oracle) | Resources | Policy |
| --- | --- | --- |
| `administrator-permissions` | `resources`, `rooms`, `professors`, `students` | `administrator-policy` |
| `coordinator-permissions` | `courses`, `classes` | `coordinator-policy` |
| `professor-permissions` | `lessons`, `reservations` | `professor-policy` |

## CAP-6 — Validate endpoint (oauth API)

Provisional contract (brief specified behavior, not path):

`POST {{base-api-url}}/authz/validate`

| | |
| --- | --- |
| Header | `Authorization: Bearer {{access_token}}` |
| Body JSON | `{ "resource": "<resource-name>" }` — one of the eight resource names above |
| `200` | Keycloak Authorization Services permits access |
| `403` | Keycloak Authorization Services denies access |
| `401` | Missing/invalid token |
| `400` | Unknown resource name / bad structure |

**Must** call **Keycloak Authorization Services** (e.g. token/permission evaluation against the configured resources/policies). Do **not** decide allow/deny by reading roles locally. The matrix below is the expected-outcome oracle for tests. Errors use the oauth OA error body (`oauth-api.md`).

### Matrix quick check (test oracle)

Derived from the realm's actual bindings + `AFFIRMATIVE` strategy. **This is the oracle for Story 6.5 tests.**

| Role | Allowed resources |
| --- | --- |
| `administrator` | **all eight** — `resources`, `rooms`, `professors`, `students`, `courses`, `classes`, `lessons`, `reservations` |
| `coordinator` | `courses`, `classes`, `lessons`, `reservations` |
| `professor` | `lessons`, `reservations` |
| `student` | *(none — `student-policy` is applied to no permission)* |

Access is **hierarchical**: `administrator` ⊇ `coordinator` ⊇ `professor`.

> The T1 brief implies disjoint sets (administrator → four resources only). That table is **not** the oracle — see "Divergence from the T1 brief" above. `student → none` is the one row where brief and realm agree.

## OIDC reference routes

- Token: `POST …/realms/{realm}/protocol/openid-connect/token`
- UserInfo: `GET …/realms/{realm}/protocol/openid-connect/userinfo` + Bearer

## Out of companion scope

- Implementing professors/classes/… domain services
- Owning professor compose/realm JSON as a git deliverable (verify/adapt only)
