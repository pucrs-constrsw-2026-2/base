# ConstrSW 2026/2 — Grupo 07 — Requirements pack

Source: course T1 + Keycloak lab + professors DB brief (as provided by the group).
Status: raw intake for BMAD (`bmad-spec` / epics). Resolve conflicts in SPEC before coding.

**Group scope:** `oauth` (identity gateway) + `professors` (domain API, PostgreSQL).

---

## A. T1 — base / oauth bootstrap

- On repo `base`, checkout the **group branch**.
- `docker-compose.yml` runs Keycloak; on start it restores a backup that configures realm **`constrsw`** and client **`oauth`**.
- Access parameters are in `.env` next to compose.
- Uncomment the **`oauth`** service in compose when the API image exists.
- In submodule `backend/oauth`, checkout the **group branch**.
- Create the **Dockerfile** in the oauth repo.
- Build a REST API that consumes Keycloak’s REST API.

### A.1 Users routes

#### `POST {{base-api-url}}/login`

- Headers: empty
- Body: form-data — `username`, `password`
- Response JSON: `token_type`, `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in` (typo as in brief)
- Logic: call Keycloak  
  `POST {{base-keycloak-url}}/auth/realms/{{realm}}/protocol/openid-connect/token`  
  with `client_id`, `client_secret`, `username`, `password`, `grant_type: password`
- Codes: `201`, `400` (bad structure), `401` (bad credentials)

#### `POST {{base-api-url}}/users`

- Header: `Authorization: Bearer {{access_token}}`
- Body JSON: `username` (= email), `password` (plain text OK for now), `first-name`, `last-name`
- Response JSON: `id` (from Keycloak `Location`), `username`, `first-name`, `last-name`, `enabled`
- Codes: `201`, `400` (structure / invalid email RFC 5322 regex from brief), `401`, `403`, `409` (username exists)

#### `GET {{base-api-url}}/users`

- Header: Bearer token; empty body
- Response: list of `{ id, username, first-name, last-name, enabled }`
- Filter: `?enabled=true|false`
- Codes: `200`, `400`, `401`, `403`

#### `GET {{base-api-url}}/users/{{id}}`

- Codes: `200`, `400`, `401`, `403`, `404`

#### `PUT {{base-api-url}}/users/{{id}}`

- Body: updated user attributes; empty response body
- Codes: `200`, `400`, `401`, `403`, `404`

#### `PATCH {{base-api-url}}/users/{{id}}`

- Body: `{ "password": "..." }`; empty response
- Codes: `200`, `400`, `401`, `403`, `404`

#### `DELETE {{base-api-url}}/users/{{id}}`

- Soft delete = disable user in Keycloak; empty body
- Codes: `204`, `400`, `401`, `403`, `404`

### A.2 Roles routes

- `POST /roles`, `GET /roles`, `GET /roles/{{id}}`, `PUT /roles/{{id}}`, `PATCH /roles/{{id}}`, `DELETE /roles/{{id}}` (logical delete)
- Endpoints to **assign** a role to a user and **unassign** a role from a user

### A.3 Error body (oauth)

```json
{
  "error_code": "OA-000",
  "error_description": "...",
  "error_source": "...",
  "error_stack": [{ "...": "..." }]
}
```

- `error_code`: unless specified otherwise, relay Keycloak response code
- `error_description`: group-provided message
- `error_source`: e.g. `OAuthAPI`
- `error_stack`: chain of errors to the final one

---

## B. Keycloak configuration & authorization

### B.1 Historical / reference run command (older image)

```text
docker run -d -p 8080:8080 -v keyclock-data:/opt/jboss/keycloak/standalone/data \
  -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=a12345678 jboss/keycloak
```

**Note for SPEC:** Grupo 07 compose uses `quay.io/keycloak/keycloak:26.7.2` + realm import — prefer that over `jboss/keycloak`. Paths may be `/realms/...` without `/auth` on KC 26; confirm and document.

### B.2 Realm / client setup (concepts)

- Create realm; email as username
- Create client; generate client secret
- Access control in realm `constrsw`, client `oauth`:
  1. Roles: `administrator`, `coordinator`, `professor`, `student`
  2. Authorization resources (with URL each): `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`
  3. One **Role** policy per role (filter by client when picking role)
  4. Resource-based permissions:
     - `administrator-permissions` → resources `resources`, `rooms`, `professors`, `students` + `administrator-policy`
     - `coordinator-permissions` → `courses`, `classes` + `coordinator-policy`
     - `professor-permissions` → `lessons`, `reservations` + `professor-policy`
  5. OAuth API endpoint that validates access token and checks whether a role grants access to the requested **resource**; `200` OK / `403` Forbidden

### B.3 Keycloak reference routes

- Token: `POST .../realms/{realm}/protocol/openid-connect/token` (form urlencoded)
- UserInfo: `GET .../protocol/openid-connect/userinfo` with Bearer token

### B.4 Spec variants / extras (resolve in SPEC)

Second brief variant for login body also lists form fields: `client_id`, `username`, `password`, `grant_type` (T1 lists only username/password — client secret from env).
- Capture access_token expiry and refresh via refresh_token (may need a new route); follow Keycloak’s approach.

---

## C. Professors domain — PostgreSQL

- Group works on **professors** domain.
- Discuss data requirements; implement CRUD for **two classes**:
  - **Main** entity
  - **Secondary** entity as a **collection** attribute of the main (e.g. class → grades)
- Each class (main and secondary): **≥ 3 attributes besides ID**
- IDs: **UUID** on create
- Deliverables (Astah + examples — mostly human):
  - Conceptual schema (class diagram)
  - Logical **relational** schema (PostgreSQL)
  - ≥ 2 JSON examples of main object including secondary collection

---

## D. Open conflicts for bmad-spec to decide

1. Login body: username/password only vs also client_id/grant_type in form
2. Login success code: `201` (T1) vs Keycloak’s `200`
3. Role names in realm export vs auth brief: `funcionario/coordenador/...` vs `administrator/coordinator/...`
4. Keycloak URL prefix `/auth` (old) vs KC 26 without `/auth`
5. Whether Authorization (resources/policies) is in T1 MVP or a follow-up epic
6. Professors main/secondary pair (group must choose — e.g. Professor + titles/degrees/publications)
