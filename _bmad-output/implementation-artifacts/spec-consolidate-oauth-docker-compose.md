---
title: 'Consolidate OAuth Docker Compose Environment'
type: 'chore'
created: '2026-09-09'
status: 'done'
baseline_commit: '1791eabdae89978a0c5dc6d06b56b5c9464e6c2e'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent - do not modify unless human renegotiates">

## Intent

**Problem:** OAuth currently has a standalone Compose definition that duplicates Keycloak and conflicts with the root Compose configuration. The root service also injects variable names and port values that do not match the OAuth application contract.

**Approach:** Make the root `docker-compose.yml` the single Compose entry point for Keycloak and OAuth, and align the OAuth service's environment, network, published ports, healthcheck, and Keycloak dependency with the application and `.env.example`. Remove only the obsolete standalone duplication while preserving the OAuth HTTP contract.

## Boundaries & Constraints

**Always:** Keep the OAuth image built from `backend/oauth/Dockerfile`; preserve the OAuth API port `8088` and `/health` endpoint; use the shared `constrsw` network; configure OAuth to reach Keycloak at the Compose service name; keep OAuth startup gated on a healthy Keycloak; retain the existing authentication variables and cookie behavior.

**Ask First:** None.

**Never:** Do not change OAuth controllers, authentication behavior, realm/client semantics, Keycloak assets, unrelated services, or introduce a second Compose entry point for the same local environment.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Root startup | Root `.env` values and `docker compose up` | Keycloak becomes healthy, then OAuth starts on the published OAuth port | OAuth waits through `depends_on` until Keycloak health succeeds |
| OAuth health | `GET /health` on the published OAuth port | HTTP 200 with the existing health response | Compose healthcheck marks OAuth unhealthy when the endpoint is unavailable |
| Internal Keycloak URL | OAuth container on `constrsw` network | OAuth resolves `keycloak:8080` without using `localhost` | Authentication dependency errors remain handled by the existing OAuth code |

</frozen-after-approval>

## Code Map

- `docker-compose.yml` -- authoritative root definitions for Keycloak and OAuth build, environment, network, ports, healthcheck, and dependency.
- `.env` -- local Compose values that must use the OAuth application's supported variable names and port contract.
- `backend/oauth/.env.example` -- OAuth-supported environment variable contract for local/container configuration.
- `backend/oauth/docker-compose.yml` -- obsolete standalone Keycloak/OAuth duplication to remove after root consolidation.
- `backend/oauth/Dockerfile` -- existing OAuth image build and runtime contract; preserve unchanged.
- `backend/oauth/src/main.ts` -- defines the OAuth listening port contract.
- `backend/oauth/src/auth/keycloak.client.ts` -- defines the Keycloak URL and authentication variable contract.
- `backend/oauth/src/health.controller.ts` -- defines the health endpoint used by the Compose healthcheck.

## Tasks & Acceptance

**Execution:**
- [x] Update `docker-compose.yml` and `.env` to use `PORT=8088`, `KEYCLOAK_URL=http://keycloak:8080`, the existing OAuth credentials/cookie settings, shared network, published port, healthcheck, and healthy-Keycloak dependency -- make the root Compose file runnable without legacy variable mismatches.
- [x] Remove `backend/oauth/docker-compose.yml` -- eliminate the duplicated standalone environment while retaining the Dockerfile and OAuth source contract.
- [x] Update `backend/oauth/.env.example` only if needed to document the same supported container variables -- keep local development defaults valid.

**Acceptance Criteria:**
- Given the repository root and a valid `.env`, when `docker compose config` runs, then the resolved configuration contains Keycloak and one OAuth service using the shared network, aligned variables, expected port mappings, healthcheck, and healthy-Keycloak dependency.
- Given the root Compose configuration, when `docker compose build oauth` runs, then the OAuth image builds from `backend/oauth/Dockerfile` successfully.
- Given the OAuth container is running, when `GET /health` is requested on the published OAuth port, then it returns HTTP 200 and the existing OAuth health payload.
- Given the root Compose file is used, when the obsolete submodule Compose file is absent, then no OAuth or Keycloak behavior is lost from the root environment.

## Verification

**Commands:**
- `docker compose config` -- expected: valid resolved configuration with no OAuth variable or dependency errors.
- `docker compose build oauth` -- expected: successful OAuth image build from the existing Dockerfile.

## Suggested Review Order

**Compose integration**

- Root Compose owns both services and gates OAuth on healthy Keycloak.
	[`docker-compose.yml:48`](../../docker-compose.yml#L48)

- OAuth environment and published port now match the application contract.
	[`docker-compose.yml:52`](../../docker-compose.yml#L52)

- The healthcheck probes the existing OAuth endpoint on the actual process port.
	[`docker-compose.yml:73`](../../docker-compose.yml#L73)

**OAuth build and operator guidance**

- The image builds from the existing package manifest despite its peer dependency mismatch.
	[`Dockerfile:1`](../../backend/oauth/Dockerfile#L1)

- Local instructions now direct operators to the root Compose ports.
	[`README.md:30`](../../backend/oauth/README.md#L30)

- Root environment values provide the service URL, port, and cookie configuration.
	[`.env:25`](../../.env#L25)
