# base

Umbrella repository for ConstrSW 2026-2. It holds the local infrastructure
(Docker Compose, Keycloak, Prometheus, OpenTelemetry) and pulls every backend
service and the frontend in as git submodules.

## Layout

| Path | Contents |
|---|---|
| `backend/*` | One submodule per service (`oauth`, `bff`, `classes`, `courses`, …) |
| `frontend` | Frontend submodule |
| `infrastructure/dev.local/services` | Keycloak, Prometheus and OTel Collector images and config |
| `docker-compose.yml` / `.env` | Single entry point for the local environment |
| `_bmad-output/planning-artifacts` | Brief, PRD, architecture and UX artifacts |

## Prerequisites

- Docker with Docker Compose v2
- Git

## Getting started

```bash
git clone --recurse-submodules https://github.com/pucrs-constrsw-2026-2/base.git
cd base
# already cloned without submodules? run: git submodule update --init --recursive

cp .env.example .env

# first run only: create the external volumes
docker volume create constrsw-keycloak-data
docker volume create constrsw-prometheus-data

docker compose up -d --build --wait
```

`--wait` returns once every service is healthy. `oauth` only starts after
Keycloak is healthy.

Every service with a `build:` entry is compiled inside its Dockerfile. Don't
rely on build artifacts from your machine (`dist/`, `node_modules/`).

## Services

| Service | URL | Notes |
|---|---|---|
| oauth API | http://localhost:8181 | Login, users and roles gateway over Keycloak |
| oauth Swagger | http://localhost:8181/docs | |
| oauth health | http://localhost:8181/health | |
| Keycloak console | http://localhost:8081 | `admin` / `a12345678`, realm `constrsw` |
| Keycloak health/metrics | http://localhost:9001 | |
| Prometheus | http://localhost:9090 | |
| OTel Collector | `localhost:4317` (gRPC), `localhost:4318` (HTTP) | Metrics on 8888 / 8889 |
| Blackbox Exporter | http://localhost:9115 | |

Quick check:

```bash
curl localhost:8181/health
curl -X POST localhost:8181/v1/roles -H 'content-type: application/json' -d '{"name":"test"}'
```

A `503` on the second call means `oauth` can't get an admin token from
Keycloak. See [Keycloak realm](#keycloak-realm).

## Configuration

All values live in the root `.env`, which is git-ignored. Copy it from
`.env.example`, whose defaults are only meant for local development. When you
add a variable, add it to `.env.example` too. After changing `.env` or the compose file, recreate the affected service:

```bash
docker compose up -d --build --force-recreate oauth
```

## Keycloak realm

The `constrsw` realm is imported from
`infrastructure/dev.local/services/keycloak/constrsw.json`. It defines two
clients used by `oauth`:

| Client | Used for | Variables |
|---|---|---|
| `oauth` | User login (password grant) | `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` |
| `oauth-admin` | Admin API for users and roles (service account) | `KEYCLOAK_ADMIN_CLIENT_ID` / `KEYCLOAK_ADMIN_CLIENT_SECRET` |

The realm also ships the users `admin@pucrs.br`, `coordinator@pucrs.br`,
`professor@pucrs.br` and `student@pucrs.br`. Manage their passwords in the
Keycloak console.

**The import only runs once.** Keycloak keeps its database in the
`constrsw-keycloak-data` volume and skips the import if the realm already
exists, so later changes to `constrsw.json` don't reach an existing volume. To
reload the realm from the file (this deletes any local users and roles):

```bash
docker compose down
docker volume rm constrsw-keycloak-data
docker volume create constrsw-keycloak-data
docker compose up -d --build --wait
```

To keep your data, apply the change in the Keycloak console instead.

## Common commands

```bash
docker compose ps                  # service status
docker compose logs -f oauth       # follow a service's logs
docker compose down                # stop everything (volumes are kept)
git submodule update --remote      # move submodules to their remote branch heads
```

## More docs

- [backend/oauth/README.md](backend/oauth/README.md): oauth routes, decisions and tests
- [infrastructure/dev.local/services/keycloak/README.md](infrastructure/dev.local/services/keycloak/README.md): Keycloak setup
- [infrastructure/dev.local/services/prometheus/README.md](infrastructure/dev.local/services/prometheus/README.md): monitoring
