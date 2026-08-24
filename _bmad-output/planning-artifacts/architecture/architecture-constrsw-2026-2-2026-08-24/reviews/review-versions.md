# Review: versões e realidade da stack — Architecture Spine Closed CRAS

- **Data da revisão:** 2026-08-24
- **Alvo:** `ARCHITECTURE-SPINE.md` (status `draft`, atualizado 2026-08-24)
- **Lente:** cada tecnologia nomeada / pinada foi conferida na web (existência, versão corrente, fit, defaults vivos de starters) — não no treinamento.
- **Veredito:** **CONDICIONAL.** Quase todos os pins numéricos da tabela Stack existem e são o `latest` estável do dia. Dois problemas de *fit* impedem “apto sem ajuste”: TypeScript **7.0.2** quebra o paved path NestJS (`nest build` / `nest start`) e diverge do starter oficial Vite `react-ts`; AD-4 classifica `keycloak-js` como DEPRECATED, o que as páginas oficiais do Keycloak **não** fazem.

A frase da spine “Verificado na web em 2026-08-24” é, em geral, verdadeira para os números. O que falhou foi o *reality-check de fit* do TypeScript único da malha e a classificação do adapter JS.

---

## Método

Fontes consultadas em 2026-08-24 (não exaustivo): Maven Central / spring.io / GitHub Releases; npm registry (`latest`); nodejs.org/download e schedule LTS; keycloak.org (downloads, getting-started Docker, adapters); spec.openapis.org; PostgreSQL.org + Docker Hub `postgres:18.6`; GitHub `vitejs/vite` `template-react-ts`; GitHub `nestjs/typescript-starter`; issues `nestjs/nest-cli` #3477/#3479; endoflife.date/spring-boot; RFC 9457; RFC 9700 (ROPC).

Nada abaixo é inferência de training data sem cruzamento.

---

## Tabela Stack — item a item

| Pin da spine | Existe? | É o corrente em 2026-08-24? | Fit / starter | Status |
| --- | --- | --- | --- | --- |
| OpenAPI Specification **3.1** | Sim (`3.1.0`–`3.1.2`) | **Não é o latest da spec.** Latest publicado = **OAS 3.2.0** (19 set 2025, `spec.openapis.org/oas/latest.html`) | Pin **consciente** (memlog): springdoc 3.1.0 e tooling Nest/Swagger ainda alinhados a 3.1. 3.2 é backward-compat com 3.1; codegen/linter de 9 grupos pode não estar maduro em 3.2 | **OK (conservador)** — não desatualizado por acidente |
| Java **21** | Sim (LTS) | **Não é o LTS mais novo.** Adoptium lista **JDK 25 LTS** e **JDK 21 LTS**. Spring Boot 4.1.1: Java 17–26 | 21 continua suportado. Conservador para laboratório. Não pinou patch (`21.0.x`) — aceitável para major | **OK (conservador)** |
| Spring Boot **4.1.1** | Sim | **Sim.** Release 2026-08-20 ([spring.io/blog](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/), Maven Central). OSS até 2027-07-31. Há `4.2.0-M1` (milestone — não pinar) | Substitui 3.5 (OSS EOL **2026-06-30**, confirmado endoflife.date). Baseline Java 17 | **OK** |
| springdoc-openapi **3.1.0** | Sim | **Sim.** Latest no site e Maven Central. POM parent = Spring Boot **4.1.0** (não 4.1.1) | Linha 3.x = Boot 4 / OpenAPI 3.1. Compatível com Boot 4.1.1 (patch). Default `openapi_3_1` | **OK** (parent 4.1.0, não 4.1.1 — irrelevante) |
| Node.js **24.19.0** | Sim | **Sim, Active LTS.** [nodejs.org/en/download](https://nodejs.org/en/download) oferece v24.19.0 LTS (Krypton, 2026-08-03). Node **26** é Current (LTS previsto 2026-10-28). 22 = Maintenance LTS | Nest 11 exige ≥20; react-router 8.3.0 exige ≥22.22.0. 24.19.0 serve os dois | **OK** |
| NestJS **11.2.1** (`@nestjs/core`) | Sim | **Sim.** npm `latest` = 11.2.1 (2026-08-14). Nest **12** só em alpha | Express 5 é o default do Nest 11. Starter GitHub `nestjs/typescript-starter` **não** está no 11.2.1 (ver Starters) | **OK** (npm; não o repo starter) |
| Express **5.2.1** | Sim | **Sim.** npm `latest` = 5.2.1 (2025-12-01; reverte breaking change de 5.2.0) | `@nestjs/platform-express@11.2.1` depende **exatamente** de `express@5.2.1` | **OK** |
| TypeScript **7.0.2** | Sim | **Sim como `latest` npm** (2026-07-08). Compilador nativo (Go); API programática **não** vem no 7.0 — prevista no 7.1 | **Não cabe no Nest CLI.** `nest build` / `nest start` quebram (`getParsedCommandLineOfConfigFile is not a function`). Vite `react-ts` vivo pina **`typescript: ~6.0.2`**, não 7.0.2 | **FALHA DE FIT** |
| Keycloak **26.7.2** | Sim | **Sim.** Release 2026-08-19 ([keycloak.org](https://www.keycloak.org/2026/08/keycloak-2672-released), GitHub tag). Patch de segurança (CVE-2026-18963 e outras) | `start-dev` ainda é o modo de desenvolvimento oficial | **OK** |
| Imagem `quay.io/keycloak/keycloak:26.7.2` | Sim | **Sim.** Getting started Docker oficial: `quay.io/keycloak/keycloak:26.7.2 start-dev` (docs “Nightly 26.7.2”). Renovate em repos públicos já bumpou para 26.7.2 | Registro oficial = Quay, não Docker Hub como fonte primária | **OK** |
| PostgreSQL **18.6** | Sim | **Sim.** Release 2026-08-13 (pula 18.5 por regressão). PG 19 ainda Beta 3 | Tag Docker Hub oficial `postgres:18.6` ativa (pushed 2026-08-13/15). **Breaking change da imagem 18:** VOLUME = `/var/lib/postgresql` (não `/var/lib/postgresql/data`) | **OK** + aviso de volume |
| React **19.2.8** / React DOM **19.2.8** | Sim | **Sim.** GitHub React “Latest” 19.2.8 (2026-07-21); npm `react` = 19.2.8 | Vite `react-ts` vivo: `"react": "^19.2.8"`, `"react-dom": "^19.2.8"`. react-router 8 pede ≥19.2.7 | **OK** |
| Vite **8.2.2** | Sim | **Sim.** npm `latest` = 8.2.2 (updated 2026-08-20) | Template oficial usa `"vite": "^8.2.2"` | **OK** |
| `@vitejs/plugin-react` **6.1.0** | Sim | **Sim.** npm `latest` = 6.1.0 (2026-08-20). Peers: `vite ^8`, react 19.2.8 nos devDeps | Template oficial: `"@vitejs/plugin-react": "^6.1.0"` | **OK** |
| `react-router` **8.3.0** | Sim | **Sim.** npm `latest` = 8.3.0 (2026-07-22). v8 **removeu** `react-router-dom` | Não vem no starter Vite (adição da spine — ok). `engines.node >=22.22.0`; peers React ≥19.2.7. API SPA: `createBrowserRouter` | **OK** |
| `@tanstack/react-query` **5.102.3** | Sim | **Sim.** npm `latest` = 5.102.3, publicado **2026-08-24T19:26:18Z** (hoje). Release GitHub de ontem ainda listava 5.102.2 | Não vem no starter Vite (adição — ok). Compatível React 18+ | **OK** (pin fresco do dia) |
| `@fontsource/ibm-plex-sans` **5.3.0** | Sim | **Sim.** npm `latest` = 5.3.0 (2026-07-19) | Pacote certo para DESIGN.md (IBM Plex Sans) | **OK** |
| `@fontsource/ibm-plex-mono` **5.3.0** | Sim | **Sim.** npm `latest` = 5.3.0 (2026-07-19) | Idem Mono | **OK** |
| Docker Compose **v2** | Sim (família `docker compose`) | **Impreciso.** Plugin atual = **Compose v5.5.0** (GitHub 2026-08-17). Docs Docker: v5 usa o mesmo comando `docker compose` que v2 | “v2” = “não é `docker-compose` v1 Python”. Funciona, mas não é um pin de versão | **OK fraco** — pin vago |

---

## Tecnologias commitidas fora da tabela Stack

| Claim | Veredito web | Notas |
| --- | --- | --- |
| Spring Boot **3.5 EOL 2026-06-30** (AD-3 Prevents) | **Confirmado.** OSS ended 2026-06-30; último patch OSS 3.5.16 (2026-06-25). Comercial Broadcom até 2032 | Justifica o pin 4.1.1 |
| RFC 9457 `application/problem+json` | **Confirmado.** Standards Track, jul 2023; obsoleta RFC 7807 | Envelope ainda corrente |
| UUID v4 | **Confirmado** como RFC 4122 / 9562 `version=4` | UUID v7 existe; pin v4 é escolha, não desatualização |
| Keycloak `start-dev` | **Confirmado** na doc de containers e getting-started Docker 26.7.2 | Adequado a AD-7 laboratório |
| Direct Access Grant / ROPC (AD-4 `[ASSUMPTION]`) | **Ainda existe** em 26.x; **não é default** em clients novos (~26.2+). RFC 9700: MUST NOT. Fora do OAuth 2.1 | Flow de laboratório ainda implementável se o client confidencial `bff` ligar Direct Access Grants. Não é “versão errada”; é risco de produto já tagueado |
| `keycloak-js` **DEPRECATED** (AD-4 Prevents) | **FALSO.** Downloads 26.7.2: JavaScript = *separate release* **26.2.4** (não DEPRECATED). Só **Node.js adapter** está `[DEPRECATED]`. Doc JS: `npm install keycloak-js` | Decisão arquitetural (UI não fala com IdP) continua válida. A *classificação* está errada |
| Adapter Node.js (`keycloak-connect`) DEPRECATED | **Confirmado** (downloads + blog 2022/2023) | Correto não usar no BFF |
| Porta frontend **5173** | **Confirmado** default Vite | Seed ok |
| `GET /health` anônimo | Convenção da malha, **não** o default Spring Actuator (`/actuator/health`) | Não é pin de lib; grupos Spring precisam mapear |

---

## Starters greenfield — defaults vivos

### Vite oficial `react-ts` (o que a spine “lean on”)

Fonte: `https://github.com/vitejs/vite/blob/main/packages/create-vite/template-react-ts/package.json` (main, 2026-08-24).

| Pacote | Template vivo | Spine | Match? |
| --- | --- | --- | --- |
| `react` / `react-dom` | `^19.2.8` | 19.2.8 | Sim |
| `vite` | `^8.2.2` | 8.2.2 | Sim |
| `@vitejs/plugin-react` | `^6.1.0` | 6.1.0 | Sim |
| `typescript` | **`~6.0.2`** | **7.0.2** | **Não** |
| lint | **oxlint** `^1.79.0` | não pinado | Spine omite (ok — seed) |
| `@types/node` | `^24.13.3` | não pinado | Alinha com Node 24 |
| `@types/react` | `^19.2.18` | não pinado | — |
| router / query / fontes | ausentes | pinados à parte | Adições conscientes |

Comando vivo: `npm create vite@latest -- --template react-ts`. Template **existe** e é o starter oficial. A spine **não** reproduz o TypeScript do template.

### NestJS

- npm `@nestjs/core@11.2.1` + `@nestjs/platform-express` → Express **5.2.1**: bate.
- Repo `nestjs/typescript-starter` (master) **está atrás**: `@nestjs/core ^11.0.1`, `typescript ^5.7.3`, `@types/node ^22`, `engines.node >=20`. **Não** usar esse GitHub como default vivo.
- `nest new` / CLI 11.0.24: guarda que **rejeita TypeScript 7.0** (falta Compiler API); mensagem oficial pede `typescript@^6` até 7.1.

Conclusão starter: o paved path Node da spine deveria pinhar **TypeScript 6.x** (Vite template `~6.0.2` + Nest CLI), não 7.0.2 — ou documentar coexistência `typescript@6` (CLI) + `tsc` 7 só no frontend.

### Spring Boot

Não há um “starter Vite” equivalente. Initializr 4.1.1 + Java 17–26. Pin Java 21 é válido; Java 25 é o LTS mais recente (set 2025).

### PostgreSQL 18 no Compose

Default vivo da imagem oficial 18.x: montar volume em `/var/lib/postgresql`, não `/var/lib/postgresql/data`. A spine não especifica o mount — risco de compose quebrado no cold-start se alguém copiar receitas PG 16/17.

---

## O que estava desatualizado / mal classificado

1. **TypeScript 7.0.2 (bloqueante de fit).** Número corrente no npm; **incompatível** com NestJS 11 CLI e **diferente** do default vivo Vite `react-ts` (`~6.0.2`). Um único pin 7.0.2 para BFF + contextos Node + UI não passa no reality-check.
2. **`keycloak-js` marcado DEPRECATED.** Só o adapter **Node.js** está deprecated. JS adapter é release separado 26.2.4, docs ativas.
3. **OpenAPI 3.1 vs 3.2.** Spec latest = 3.2.0. Pin 3.1 **justificado por tooling** (memlog + springdoc). Não corrigir para 3.2 sem checar Nest Swagger/orval/openapi-generator dos 9 grupos.
4. **Java 21 vs 25.** 21 não está “morto”; 25 é o LTS novo. Conservador, ok para laboratório.
5. **Docker Compose “v2”.** Família certa, versão errada/vaga (plugin atual 5.x). Preferir “Docker Compose plugin (`docker compose`), não Compose v1”.
6. **ROPC.** Não é pin de versão, mas o default Keycloak 26.2+ **desliga** Direct Access Grant em client novo. Compose/realm precisa ligar explicitamente no client `bff`.

---

## O que *não* ficou sem confirmação web

Nenhum pin da tabela Stack ficou só em training data. Residual de confiança baixo:

- Tag Quay `26.7.2`: confirmada pela **página oficial** getting-started Docker (comando com essa tag). API Quay desta sessão timeout; não contradiz a doc.
- Patch exato de Temurin 21 (`21.0.x`): não pinado na spine — sem drift.
- `nestjs/typescript-starter` master: confirmado **stale**; a spine pinou npm, não esse repo.

---

## Recomendações (não aplicadas neste arquivo)

1. **Trocar o pin TypeScript** para `6.0.2` (ou `~6.0.2`) na malha Nest/Vite; deixar 7.x como Deferred até Nest CLI + API programática 7.1. Se quiser 7 só no frontend, **dois pins**, não um.
2. **Corrigir AD-4 Prevents:** “adapter Node.js DEPRECATED; UI sem `keycloak-js` (ainda mantido, mas fora da malha)”.
3. Opcional: Compose “v2” → `docker compose` (plugin atual). Nota de volume PG 18 no seed operacional.
4. Manter OpenAPI 3.1, Spring Boot 4.1.1, Nest 11.2.1, Node 24.19.0 LTS, Keycloak 26.7.2, React/Vite/router/query/fontsource como estão.

---

## Fontes (principais)

- Spring Boot 4.1.1: https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now/
- Spring Boot 4.1 system requirements: https://docs.spring.io/spring-boot/4.1/system-requirements.html
- Spring Boot EOL: https://endoflife.date/spring-boot
- springdoc 3.1.0: https://springdoc.org/
- Node 24.19.0 LTS: https://nodejs.org/en/download — https://nodejs.org/en/blog/release/v24.19.0
- Node schedule: https://github.com/nodejs/Release
- npm: `@nestjs/core@11.2.1`, `express@5.2.1`, `typescript@7.0.2`, `vite@8.2.2`, `@vitejs/plugin-react@6.1.0`, `react@19.2.8`, `react-router@8.3.0`, `@tanstack/react-query@5.102.3`, `@fontsource/ibm-plex-{sans,mono}@5.3.0`
- Nest CLI × TS 7: https://github.com/nestjs/nest-cli/issues/3477 — https://github.com/nestjs/nest-cli/issues/3479
- Vite template: https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts
- Nest starter (stale): https://github.com/nestjs/typescript-starter
- Keycloak 26.7.2: https://www.keycloak.org/2026/08/keycloak-2672-released — https://www.keycloak.org/downloads — https://www.keycloak.org/getting-started/getting-started-docker
- Keycloak adapters: downloads (JS 26.2.4 vs Node DEPRECATED); https://www.keycloak.org/securing-apps/javascript-adapter
- OAS 3.2.0: https://spec.openapis.org/oas/latest.html — 3.1.2: https://spec.openapis.org/oas/v3.1.2.html
- PostgreSQL 18.6: https://www.postgresql.org/about/news/postgresql-186-1711-1615-1519-1424-and-19-beta-3-released-3365/
- Docker Hub postgres:18.6 + nota PGDATA 18: https://hub.docker.com/_/postgres
- RFC 9457: https://datatracker.ietf.org/doc/html/rfc9457
- Docker Compose v5.5.0: https://github.com/docker/compose/releases/tag/v5.5.0
- Adoptium LTS list: https://adoptium.net/temurin/releases/ (JDK 25 LTS e JDK 21 LTS)
