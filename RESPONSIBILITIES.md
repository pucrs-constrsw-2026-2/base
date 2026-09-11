# Grupo 07 — Responsibilities

Who owns what in the `oauth` service, and what each track delivered.

Superseded the original A/B/C plan: that split assumed four implementers and
different epic boundaries. This file records what was actually built.

| Related | Path |
| --- | --- |
| Contract | `_bmad-output/specs/spec-grupo07-keycloak-oauth/` |
| Epics / ACs | `_bmad-output/planning-artifacts/epics.md` |
| Architecture | `_bmad-output/planning-artifacts/architecture-grupo07-keycloak-oauth.md` |
| Story files | `_bmad-output/implementation-artifacts/*.md` |
| Sprint status | `_bmad-output/implementation-artifacts/sprint-status.yaml` |
| Handoff log | `CHANGELOGS.md` |
| AI prompt | `AI-CONTEXT-PROMPT.md` |
| Service docs | `backend/oauth/README.md` |

---

## Tracks

| Track | Owner | Scope | Delivered |
| --- | --- | --- | --- |
| **Planning** | Juliano Chies | SPEC, architecture, epics, all 26 story files | ✅ |
| **Usuários** | William Klein | Config foundation, Bearer guard, Epic 4 | ✅ |
| **Roles** | Gabriel Hoppe | Dockerfile, `/health`, Epic 5 | ✅ |
| **Tokens & Autorização** | Leonardo Gemin | Epics 2, 3 and 6 | ✅ |

All 26 stories are implemented and merged into `grupo07`. What remains is
**verifying them against a running Keycloak** — see "Still open" below.

### Planning — Juliano

The contract every other track implemented against: `SPEC.md` with its two
companions, the architecture document, the six epics, and a story file per
story with acceptance criteria. No implementation work was needed afterwards
because the story files carried the ACs.

### Usuários — William

`src/config/` — settings bound to the professor's environment names, failing
fast at boot, plus the URL builder every module uses instead of reading
`process.env`.

`src/common/` — `BearerAuthGuard` (who is calling; never permissions) and
`KeycloakAdminClient` (admin token with caching, timeouts, failure mapping),
shared by roles and users.

`src/users/` — the six `/users` routes.

### Roles — Gabriel

`Dockerfile` and `GET /health`, which is what makes the service runnable under
the professor's compose, plus `src/roles/` with the eight role routes,
including assign and unassign.

### Tokens & Autorização — Leonardo

`src/auth/` — `POST /login` and `POST /refresh` over form data.

`src/errors/` — the uniform OA envelope and the mapper that relays Keycloak's
own error code, used by every module.

`src/authz/` — `POST /authz/validate`, which delegates the decision to Keycloak
Authorization Services.

---

## Rules everyone follows

1. **Branch per piece of work**, named `grupo07-feat/<name>` or
   `grupo07-fix/<name>`, always cut from `grupo07` — never from `main`, which
   belongs to the professor.
2. **One commit per story at least.** A single commit at the end hides who did
   what in `git log`.
3. **Append to `CHANGELOGS.md`** every session, newest entry on top, using the
   template in that file. Never paste secret values; names are fine.
4. **Update `sprint-status.yaml`** when a story changes state.
5. **Update the submodule pointer.** Merging inside `backend/oauth` does *not*
   move the reference stored here. Check `git ls-tree grupo07 backend/oauth`
   against the submodule's own HEAD — it was three merges behind once, and a
   fresh clone would have built an image without the `/users` routes.
6. **Never invent infrastructure.** `docker-compose.yml`, `.env` and the
   Keycloak realm are the professor's. Plug into them.

## Conventions worth knowing before touching the code

- **Inject `KeycloakSettingsService`**, never read `process.env` in a feature
  module. The `/auth` URL rule lives there and must not be duplicated.
- **Inject `KeycloakAdminClient`** rather than writing a second Admin API
  client; it owns the admin token, the timeout and the failure mapping.
- **No local permission table.** Authorization is answered by Keycloak. The
  brief's matrix already disagrees with what `constrsw.json` configures, so a
  table copied into code would ship that contradiction.
- **An unreachable Keycloak is `503`**, not `401`, everywhere.
- Errors use the shared OA envelope; `error_stack` redacts tokens and passwords.

## Still open

- **Story 1.3 — run the stack for real.** The 254 tests face a faked upstream:
  they prove the logic and the contracts, not the wiring. Nobody has walked the
  routes against a running Keycloak yet.
- Professors domain and PostgreSQL, which are out of the T1 scope.
