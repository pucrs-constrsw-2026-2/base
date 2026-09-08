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

| Permission | Resources | Policy |
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

### Matrix quick check

| Role | Allowed resources |
| --- | --- |
| `administrator` | `resources`, `rooms`, `professors`, `students` |
| `coordinator` | `courses`, `classes` |
| `professor` | `lessons`, `reservations` |
| `student` | *(none in B.2 permission sets)* |

## OIDC reference routes

- Token: `POST …/realms/{realm}/protocol/openid-connect/token`
- UserInfo: `GET …/realms/{realm}/protocol/openid-connect/userinfo` + Bearer

## Out of companion scope

- Implementing professors/classes/… domain services
- Owning professor compose/realm JSON as a git deliverable (verify/adapt only)
