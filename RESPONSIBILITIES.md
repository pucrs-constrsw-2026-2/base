# Grupo 07 — Responsibilities (implementation tracks)

Divide **implementation** of the Keycloak/oauth backlog among **3 people**.  
Planning (SPEC, epics, story files) is already done — this file assigns **who builds what**.

| Related | Path |
| --- | --- |
| Contract | `_bmad-output/specs/spec-grupo07-keycloak-oauth/` |
| Epics / ACs | `_bmad-output/planning-artifacts/epics.md` |
| Architecture | `_bmad-output/planning-artifacts/architecture-grupo07-keycloak-oauth.md` |
| Story files | `_bmad-output/implementation-artifacts/*.md` |
| Sprint status | `_bmad-output/implementation-artifacts/sprint-status.yaml` |
| Handoff log | `CHANGELOGS.md` |
| AI prompt | `AI-CONTEXT-PROMPT.md` |

**Assign names here** (Grupo 07 members: Gabriel Hoppe, Juliano Chies, Leonardo Gemin, William Klein — pick 3 owners; 4th can pair/review):

| Track | Owner (fill in) | Focus |
| --- | --- | --- |
| **A — Foundation** | _TBD_ | Epics 1–3 |
| **B — Users** | _TBD_ | Epic 4 |
| **C — Roles & Authz** | _TBD_ | Epics 5–6 |

---

## Dependency rule (read before coding)

```text
Track A (1 → 2 → 3) should land first (or stay slightly ahead).
Track B needs: Nest app + config (1.1), OA mapper (3.1) before shipping user errors.
Track C needs: Bearer/OA from A+B (4.1 guard); assign/unassign needs users existing in Keycloak.
Epic 6.1–6.4 are mostly verify-on-professor-import; 6.5 needs working token login (Track A).
```

Parallelism is OK if Track A has **1.1 + 3.1** available; otherwise B/C block on shared error/auth pieces.

---

## Track A — Foundation (Epics 1, 2, 3)

**~7 stories** — stack, tokens, uniform errors.

| Story | File | Outcome |
| --- | --- | --- |
| 1.1 | `1-1-load-keycloak-settings-from-professor-environment.md` | Nest config + URL builder _(may already be in review)_ |
| 1.2 | `1-2-dockerfile-for-the-oauth-api-image.md` | Dockerfile + `GET /health` |
| 1.3 | `1-3-verify-oauth-runs-on-professor-provided-compose.md` | Verify professor compose (do not invent compose) |
| 2.1 | `2-1-login-with-username-and-password.md` | `POST /login` → 201 + tokens |
| 2.2 | `2-2-refresh-tokens-without-re-entering-password.md` | `POST /refresh` → 200 |
| 3.1 | `3-1-oa-error-envelope-and-mapper.md` | Shared OA error mapper |
| 3.2 | `3-2-oa-error-body-on-login-and-refresh-failures.md` | OA on login/refresh failures |

**Owns:** `config/`, `health/`, `auth/` (login/refresh), `errors/`, Dockerfile, oauth README bootstrap sections.

---

## Track B — Users API (Epic 4)

**~6 stories** — Keycloak user administration.

| Story | File | Outcome |
| --- | --- | --- |
| 4.1 | `4-1-create-user.md` | `POST /users` + **Bearer guard** (reuse later) |
| 4.2 | `4-2-list-users.md` | `GET /users` + `?enabled=` |
| 4.3 | `4-3-get-user-by-id.md` | `GET /users/{{id}}` |
| 4.4 | `4-4-update-user-attributes.md` | `PUT /users/{{id}}` |
| 4.5 | `4-5-change-user-password.md` | `PATCH /users/{{id}}` password |
| 4.6 | `4-6-soft-delete-user.md` | `DELETE` = disable → 204 |

**Owns:** `users/` module, Admin API user client, Bearer guard documentation in README.

**Must reuse:** Track A OA mapper; professor env (`KEYCLOAK_ADMIN*`, `adminRealmUrl`).

---

## Track C — Roles & Authorization (Epics 5, 6)

**~13 stories** — roles CRUD/assign + authz verify + validate endpoint.

### Epic 5 — Roles

| Story | File | Outcome |
| --- | --- | --- |
| 5.1 | `5-1-create-role.md` | `POST /roles` |
| 5.2 | `5-2-list-roles.md` | `GET /roles` |
| 5.3 | `5-3-get-role-by-id.md` | `GET /roles/{{id}}` |
| 5.4 | `5-4-replace-role.md` | `PUT /roles/{{id}}` |
| 5.5 | `5-5-partially-update-role.md` | `PATCH /roles/{{id}}` |
| 5.6 | `5-6-logical-delete-role.md` | Logical delete role |
| 5.7 | `5-7-assign-role-to-user.md` | Pin `POST /users/{{id}}/roles` |
| 5.8 | `5-8-unassign-role-from-user.md` | Unassign (same path family) |

### Epic 6 — Authz

| Story | File | Outcome |
| --- | --- | --- |
| 6.1 | `6-1-ensure-b-2-client-roles-on-oauth.md` | Verify client roles on import |
| 6.2 | `6-2-authorization-resources-with-urls.md` | Verify 8 resources |
| 6.3 | `6-3-role-policies-on-client-oauth.md` | Verify role policies |
| 6.4 | `6-4-resource-based-permissions.md` | Verify B.2 permissions |
| 6.5 | `6-5-validate-resource-access-via-keycloak-authorization-services.md` | `POST /authz/validate` |

**Owns:** `roles/`, `authz/`, Keycloak console verify notes in README (not replacing `constrsw.json`).

**Must reuse:** Bearer from 4.1; OA from 3.1; login tokens from 2.1 for live authz checks.

---

## Shared duties (all tracks)

1. Append a block to **`CHANGELOGS.md`** every session (template inside that file).
2. Update **`sprint-status.yaml`** when a story moves (`ready-for-dev` → `in-progress` → `review` → `done`).
3. Work in submodule **`backend/oauth`** on **`grupo07`**; do not invent root compose/`.env`.
4. Follow SPEC / story file ACs (form-data login, `201`, `referesh_expires_in`, OA body, etc.).
5. After finishing a story: prefer a short **code-review** chat before marking `done`.

---

## Suggested sequencing for the group

1. Fill owner names in the table above.  
2. Track A finishes **1.1 review → 1.2 → 1.3 → 2.x → 3.x** (or 3.1 early so B/C can use OA).  
3. Track B starts **4.1** once OA + Nest config exist.  
4. Track C starts **5.1** after Bearer exists; run **6.1–6.4** verify in parallel with roles if Keycloak is up; **6.5** after login works.
