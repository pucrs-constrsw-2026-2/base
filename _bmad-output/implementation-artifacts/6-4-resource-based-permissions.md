---
baseline_commit: 1791eabdae89978a0c5dc6d06b56b5c9464e6c2e
---

# Story 6.4: Resource-based permissions

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a group developer,
I want the three B.2 permission sets on client `oauth`,
so that Keycloak Authorization Services can allow or deny named resources.

## Acceptance Criteria

1. **Given** resources from Story 6.2 and policies from Story 6.3
   **When** this story is done
   **Then** permission bindings are **verified** in the running realm (create/fix only genuinely missing objects), each with `decisionStrategy: AFFIRMATIVE`:  
   `administrator-permissions` → `resources`, `rooms`, `professors`, `students` via `administrator-policy`;  
   `coordinator-permissions` → `courses`, `classes` via `coordinator-policy` **+ `administrator-policy`**;  
   `professor-permissions` → `lessons`, `reservations` via `professor-policy` **+ `coordinator-policy` + `administrator-policy`**
   **And** the extra policies are treated as **intentional configuration**, not gaps — they **must not** be removed to match the brief
   **And** note: the professor export may list `*-permissions` under `policies` with an empty top-level `permissions` array — confirm **effective** bindings in Admin Console after import
   **And** configuration remains present in or applied **atop** the professor-imported realm (not a group-owned compose/realm-JSON deliverable)
   **And** the oauth README documents verification / gap-fill

## Tasks / Subtasks

- [ ] Task 1 — Console/effective verify (AC: #1)
  - [ ] Do not trust JSON shape alone (permissions nested under `policies` in export)
  - [ ] Confirm the three named permission sets and resource lists
- [ ] Task 2 — Gap-fill only; README
- [ ] Task 3 — Do **not** implement `POST /authz/validate` (6.5)
- [ ] Confirm the multi-policy `applyPolicies` are present and **leave them intact** (see Dev Notes — they are intentional, not gaps)

## Dev Notes

### Scope (this story only)

**In scope:** verify/gap-fill three B.2 permission sets in the **running** realm.

**Verify-first (6.1–6.4):** professor `constrsw.json` already contains B.2-named objects. Confirm in Admin Console **after volume import**. Create/fix **only gaps**. Do **not** replace professor realm JSON or invent compose as a git deliverable. Document method (console checklist / overlay / Admin API) in oauth README.

Import caveat: `start-dev --import-realm` runs only if realm `constrsw` is **absent** on volume `constrsw-keycloak-data`. Changing JSON without recreating the volume does nothing. Recreate volume only when the team intends a full reimport (professor Keycloak README).

**Brief divergence — RESOLVED 2026-09-09 (group decision): the realm wins.**

In `constrsw.json`, `coordinator-permissions` `applyPolicies` includes `coordinator-policy` **and** `administrator-policy`; `professor-permissions` includes professor + coordinator + administrator policies. All three permissions use `decisionStrategy: AFFIRMATIVE`, so access is **hierarchical**: `administrator` reaches all eight resources, `coordinator` reaches `courses`, `classes`, `lessons`, `reservations`, `professor` reaches `lessons`, `reservations`, `student` reaches none.

Item 4 of the Moodle brief lists a single policy per permission (disjoint sets). We follow the **realm**, because it is this semester's file (committed 2026-09-02 by the professor, `51423d2`, alongside `10e6838` / `1791eab` the same day), while the brief's screenshots still show realm `constr-sw-2022-2` and client `grupo1` — 2022 material.

**Therefore:**

- These extra policies are **intentional**, not gaps. **Do not remove them** — that would mean editing the professor's configuration, which `SPEC.md` forbids.
- **Story 6.5 tests use the realm-derived matrix as oracle**, not the brief's table. See `keycloak-authz.md` § "Divergence from the T1 brief", where the brief's original table is preserved as a historical note.
- Still verify **effective** console behavior after import and record actual grants in the oauth README.
- Do not replace git `constrsw.json` as the deliverable.

**Out of scope:** Nest validate endpoint (6.5), domain APIs.


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

- [Source: `epics.md` — Story 6.4, FR15, NFR10]
- [Source: `keycloak-authz.md` — Resource-based permissions table]
- [Source: `constrsw.json` — administrator/coordinator/professor-permissions]

## Previous story intelligence

6.2 resources + 6.3 policies. Empty top-level `permissions` array in export is called out in epics.md.

## Latest tech information

- Resource permissions in KC 26 Authorization settings; export may serialize them as type `resource` inside `policies`.

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

