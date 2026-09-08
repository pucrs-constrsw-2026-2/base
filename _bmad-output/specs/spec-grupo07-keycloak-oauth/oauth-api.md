# oauth-api — routes, payloads, errors

Companion to `SPEC-grupo07-keycloak-oauth`. Load-bearing HTTP contract for CAP-1–CAP-4, CAP-7–CAP-8.

## Runtime substrate

- Work in `backend/oauth` on the **group branch**; ship a **Dockerfile** (NestJS stub + `GET /health` preferred; professor compose healthcheck uses Node against `/health`).
- Keycloak settings come from **professor-provided** `.env` beside compose, using these names:
  - `KEYCLOAK_SERVER_URL` (e.g. `http://keycloak:8080`)
  - `KEYCLOAK_REALM` (`constrsw`)
  - `KEYCLOAK_CLIENT_ID` (`oauth`)
  - `KEYCLOAK_CLIENT_SECRET`
  - plus related `KEYCLOAK_ADMIN*` / `OAUTH_*` as needed
- Root `docker-compose.yml` already defines/enables service `oauth` — **verify** build/run; do not invent compose as a deliverable.
- External API port **8181**; Keycloak console **8081**; create volume `constrsw-keycloak-data` before first `compose up`.
- Upstream Keycloak calls use OIDC paths under `{KEYCLOAK_SERVER_URL}/realms/constrsw/…` (no `/auth` unless base URL includes it).
- Login contract for this API remains **form-data + HTTP 201** (T1/SPEC), even if Keycloak README shows a JSON curl example.

## Error body (all error responses)

```json
{
  "error_code": "OA-000",
  "error_description": "...",
  "error_source": "OAuthAPI",
  "error_stack": [{ "...": "..." }]
}
```

| Field | Rule |
| --- | --- |
| `error_code` | Relay Keycloak response code unless a route specifies otherwise |
| `error_description` | Group-provided human message |
| `error_source` | e.g. `OAuthAPI` |
| `error_stack` | Chain of errors down to the final/root cause |

## CAP-1 — Login

`POST {{base-api-url}}/login`

| | |
| --- | --- |
| Headers | none required |
| Body | form-data: `username`, `password` only |
| Server adds | `client_id`, `client_secret`, `grant_type=password` from env |
| Keycloak | `POST {keycloak-base}/realms/constrsw/protocol/openid-connect/token` |
| Success | `201` |
| Body | `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in` (spelling as brief) |
| Errors | `400` bad structure; `401` bad credentials |

## CAP-7 — Refresh

`POST {{base-api-url}}/refresh`

| | |
| --- | --- |
| Body | form-data: `refresh_token` |
| Server adds | `client_id`, `client_secret`, `grant_type=refresh_token` |
| Keycloak | same token endpoint as login |
| Success | `200` with same token field set as login (including `referesh_expires_in` if Keycloak returns refresh expiry) |
| Errors | `400` bad structure; `401` invalid/expired refresh |

## CAP-2 — Users

All user routes (except where noted) require `Authorization: Bearer {{access_token}}`.

### `POST /users`

- Body JSON: `username` (= email), `password` (plain text OK for lab), `first-name`, `last-name`
- Success `201`: `{ id, username, first-name, last-name, enabled }` — `id` from Keycloak `Location`
- Errors: `400` structure / invalid email (RFC 5322 regex from brief); `401`; `403`; `409` username exists

### `GET /users`

- Query: optional `?enabled=true|false`
- Success `200`: list of `{ id, username, first-name, last-name, enabled }`
- Errors: `400`, `401`, `403`

### `GET /users/{{id}}`

- Success `200`: same user object shape
- Errors: `400`, `401`, `403`, `404`

### `PUT /users/{{id}}`

- Body: updated user attributes
- Success `200`, empty body
- Errors: `400`, `401`, `403`, `404`

### `PATCH /users/{{id}}`

- Body: `{ "password": "..." }`
- Success `200`, empty body
- Errors: `400`, `401`, `403`, `404`

### `DELETE /users/{{id}}`

- Soft delete: disable user in Keycloak
- Success `204`, empty body
- Errors: `400`, `401`, `403`, `404`

## CAP-3 — Roles

Endpoints (Bearer required):

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/roles` | create |
| `GET` | `/roles` | list |
| `GET` | `/roles/{{id}}` | get |
| `PUT` | `/roles/{{id}}` | replace/update |
| `PATCH` | `/roles/{{id}}` | partial update |
| `DELETE` | `/roles/{{id}}` | logical delete |

Plus endpoints to **assign** a role to a user and **unassign** a role from a user (path naming left to implementation; must be documented in the service README when chosen). Standard auth failures: `401`, `403`; missing entity: `404`; bad payload: `400`.

Target role names for authz alignment: `administrator`, `coordinator`, `professor`, `student` (client roles on `oauth`).

## Keycloak reference (integration)

- Token: `POST {keycloak-base}/realms/{realm}/protocol/openid-connect/token` (form urlencoded)
- UserInfo: `GET {keycloak-base}/realms/{realm}/protocol/openid-connect/userinfo` with Bearer token

## Resolved brief conflicts (oauth)

| Conflict | Resolution |
| --- | --- |
| Login body includes client_id/grant_type? | No — client credentials server-side only |
| Login `201` vs Keycloak `200` | API returns `201` |
| `/auth` prefix | Prefer none (KC 26); verify professor base URL |
| Refresh | In scope via `POST /refresh` |
