# Review — Good-spine rubric walker

- **Alvo:** `ARCHITECTURE-SPINE.md` (Closed CRAS, altitude initiative, purpose build-substrate, status draft)
- **Lens:** checklist good-spine (Reviewer Gate)
- **Nível abaixo:** nove grupos de alunos + módulos compartilhados `oauth`, `bff`, `frontend`
- **Spine não foi alterada**
- **Lint mecânico:** `lint_spine.py` → `ok: true`, 0 findings (sem TBD/TODO, AD-n monotônicos, Binds/Prevents/Rule presentes, Stack com versão em toda linha)

## Verdict

**fail**

A spine é um contrato enxuto e bem calibrado para ConstrSW: paradigma nomeado, AD-2/AD-5/AD-7/AD-8 travam língua, sala efetiva, compose e UI. Falha o teste decisivo do nível abaixo: dois grupos que obedeçam cada AD à letra ainda podem entregar malha incompatível na geração de Lesson (FR-16), no fio JWT↔id de domínio, no contrato público do BFF e na ocupação de Professor. Isso não é nitpick de stack — é o tipo de divergência que quebra UJ-2/UJ-3 na demo.

## Scorecard

| Critério | Resultado | Nota |
| --- | --- | --- |
| Cobre os pontos reais de divergência do nível abaixo e não omite nenhum | **fail** | Omite escrita `classes`→`lessons`, junção JWT↔agregado, contrato BFF (path + envelope + credentials) e dono da ocupação de Professor |
| Toda Rule de AD é exigível e impede a divergência declarada | **fail parcial** | AD-2 (faixas.json), AD-4 (`/identidade/me` + claims), AD-5 (ocupação = sala, não Professor) e AD-6 (`Origin`) não fecham o Prevents |
| Nada em Deferred deixaria duas unidades divergirem | **fail parcial** | OQ-3 é do grupo `lessons` mas UX-S7 (outro grupo) precisa do mesmo shape; resto do Deferred é seguro |
| Tech nomeada verificada-atual (versões presentes) | **pass** | Pins conferidos na web em 2026-08-24; Compose `v2` é pin fraco (low) |
| Greenfield: sem contradição brownfield | **pass** | `.gitmodules` = seed; backends vazios; sem `compose.yaml` legado |
| Spec-driven: FR-1–31, NFRs, UX-S* | **pass com ressalva** | Mapa cobre os IDs; NFR-LGPD-4 (retenção de `id`) fica frouxo |
| Toda dimensão da altitude initiative decidida, deferred ou OQ — envelope operacional | **pass com ressalva** | AD-7 fecha deploy/env/infra/ops de laboratório; CI/TLS/segredos/contract-test silenciosos (deveriam estar em Deferred) |
| Spine = invariantes terças, não PRD copiado | **pass** | Glossário/UJs/OQ-1 não relitigados; shape em mermaid |
| Mermaid válido; sem comentários de template | **pass** | Três diagramas válidos; zero `<!-- -->`; Inherited Invariants corretamente omitido |

---

## Findings

### Critical

#### F-1 — FR-16: quem *escreve* Lesson não está no grafo de AD-1

- **Onde:** AD-1 mermaid (só `CLA -->|"GET ocupacao"| LES`); mapa de capability “`classes` escreve; … gera `lessons`”; NFR-CTR-2 “Class (que gera Lesson)”; eventos v1 proibidos (convenções).
- **Divergência:** grupo `classes` POSTA em `/v1/lessons`; grupo `lessons` espera `POST /v1/lessons/gerar` chamado pelo BFF; ou `lessons` “puxa” Class. Os três obedecem AD-1 (não há seta de escrita) e a demo de UJ-2 não gera aula.
- **Por que a Rule não impede:** “setas abaixo são as únicas dependências de runtime” + ciclo de escrita só citado para `lessons`↔`reservations`. Ciclo de escrita `classes`↔`lessons` nem é nomeado nem permitido.
- **Ação:** **autofix** — uma seta de escrita (ou orquestração BFF explícita) + Rule: dono do POST que materializa Lesson; falha parcial (Class criada, Lesson não) é recusa do escritor, não retry ad hoc. Se for BFF orquestrando dois POSTs, dizer isso em AD-6 (composição ≠ dono de agregado).

#### F-2 — Junção Keycloak ↔ `employeeId` / `professorId` / `studentId` / papel

- **Onde:** AD-4 Rule (`POST /v1/auth/login`, JWT Bearer, `GET /v1/identidade/me` “do próprio domínio”, papéis `funcionario|coordenador|professor|student`).
- **Divergência:** nove resource-servers + BFF escolhem join key diferente (`sub` UUID vs `preferred_username` vs `email` vs claim custom). Helena autentica e `employees` não acha o agregado; Caio não vê UX-S9; Marina não passa no gateway de `lessons`. Tudo isso ainda “valida JWT via JWKS”.
- **Por que a Rule não impede:** Prevents cobre adapter JS, nove logins, dono exclusivo do IdP, `localStorage`. Não cobre o *mapeamento* usuário IdP → id canônico. `/identidade/me` sem schema é nove envelopes.
- **Ação:** **discuss** (lab) / **autofix** (mínimo) — pin: (1) claim de junção no realm `closed-cras`; (2) schema mínimo de `/v1/identidade/me` e do body de `/v1/auth/login` (papel atual); (3) coordenador é Employee com papel, não quinto tipo.

#### F-3 — Contrato público do BFF (nove módulos, um frontend)

- **Onde:** AD-6 pasta `backend/bff/src/modules/<contexto>/`; AD-2 paths `/v1/...` só nos *contextos*; AD-4 só pina `POST /v1/auth/login`.
- **Divergência:** grupo A expõe no BFF `/employees`; grupo B `/v1/employees`; grupo C `/api/employees`. Coleção: `[]` vs `{ items }` vs `{ data, total }`. Fetch sem `credentials: 'include'` e CORS sem `Allow-Credentials` — cookie httpOnly SameSite=Lax entre `:5173` e `:8088` (cross-origin, same-site) morre em silêncio. Nove superfícies TanStack Query, um shell compartilhado.
- **Ação:** **autofix** — BFF público = `/v1/<contexto>/...` (mesmo inglês do submódulo); coleção JSON `{ items: T[] }` (paginação fora da v1, EXPERIENCE data-table sem infinite scroll); CORS BFF = origem do frontend + credentials; UI sempre `credentials: 'include'`. Cookie name pode ficar seed (`cras_session`) ou Deferred com dono = BFF.

### High

#### F-4 — Ocupação de Professor não tem dono (AD-5 só fecha sala)

- **Onde:** AD-5 `GET /v1/lessons/ocupacao` devolve `salaEfetivaId`; FR-13 recusa esqueleto que colide com Professor; FR-23 recusa Professor já ocupado (Lesson *ou* Reservation) em qualquer `kind`.
- **Divergência:** `classes` e `reservations` implementam “Professor livre?” com GETs diferentes, ou só olham sala. SM-5 passa num grupo e falha no outro. Prevents de AD-5 (“UI decidindo colisão”, “ocupação só a partir de Reservation”) não cobre este eixo.
- **Ação:** **autofix** — endpoint canônico de ocupação de Professor (provavelmente `lessons` para aulas + GET profundidade 1 em `reservations`, **ou** BFF read-model só para FR-29 e os escritores chamam os dois contextos com query pinada: `professorId+data+faixaId`). Recurso permanece no escritor `reservations` (já implícito).

#### F-5 — AD-2 “não copiar `faixas.json`” é inexigível no layout de submódulo

- **Onde:** AD-2 Rule: `faixaId` A–E, F–N, P carregado de `backend/oauth/contracts/faixas.json`. Cada contexto é repo Git próprio (seed + `.gitmodules`); o arquivo vive em `oauth`.
- **Divergência:** nove enums OpenAPI copiados “pra não depender do irmão”; um grupo HTTP GET no BFF; outro path relativo que só existe no compose da malha.
- **Ação:** **autofix** — um GET canônico (BFF ou fragmento `oauth` servido no compose) **ou** `$ref` no fragmento OpenAPI do catálogo; “não copiar” vira teste (enum === cânone), não um path de arquivo que o submódulo não tem.

#### F-6 — OQ-3 em Deferred deixa `lessons` e UX-S7 divergirem

- **Onde:** Deferred “OQ-3 LessonTopic — grupo `lessons`”. UX-S7 é superfície exclusiva de *outro* grupo (OQ-1).
- **Divergência:** `lessons` publica `syllabusItemId`; UX-S7 grava `enunciado` solto (ou o inverso). OpenAPI-como-verdade só funciona *depois* de um dos dois publicar; em paralelo, os dois inventam.
- **Ação:** **autofix** (não precisa fechar OQ-3): Rule de consumo — shape de LessonTopic = OpenAPI de `lessons`; UX-S7 não inventa campo paralelo. OQ-3 permanece aberto quanto a *qual* variante o grupo `lessons` escolhe.

### Medium

#### F-7 — Envelope operacional: CI, TLS, segredos, contract-test silenciosos

AD-7 fecha compose, `local`/`lab`, um DB por contexto, `/health`, stdout, sem K8s/Helm/sensor. Na altitude initiative isso é o envelope certo. Falta marcar como Deferred (não silêncio): pipeline CI por submódulo vs malha; HTTP claro no lab (sem TLS); senhas default do compose; teste de contrato além do YAML. Duas turmas podem inventar GitHub Actions incompatíveis no `frontend`/`bff` — impacto menor que F-1–F-3, mas a dimensão não está “decidida | deferred | OQ”.

**Ação:** **defer** — uma linha em Deferred.

#### F-8 — AD-6 “contextos recusam `Origin` de browser”

Ambíguo: não enviar `Access-Control-Allow-Origin` (suficiente contra SPA) vs 403 se header `Origin` existir (quebra preflight e ferramentas). Nove interpretações. Prevents “CORS de SPA nos contextos” fecha-se com “não habilitar CORS; só o BFF”.

**Ação:** **autofix** — reescrever a Rule.

#### F-9 — NFR-LGPD-4 no mapa ≠ retenção de `id`

Mapa: “NFR-LGPD-1–4 | BFF recortes + sem CPF v1”. LGPD-4 é: não apagar `id` canônico para limpar tela; titulares Student ≠ Employee. OQ-2 cobre Employee desativado; delete físico de Student/Employee fica à escolha do grupo.

**Ação:** **autofix** — uma convenção: desativar ≠ DELETE de `id`; sem CPF já está.

#### F-10 — Tokens de domínio além da lista curta de AD-2

AD-2 pina `employeeId`, `salaEfetivaId`, `faixaId`, `classId`, `lessonId`, `roomId`. Faltam no fio compartilhado (addendum já tem, consumidores cruzados vão divergir): `ReservationLine.kind` = `SALA|RECURSO` (AD-5 usa, AD-2 não pina); `modalidade` = `presencial|remoto`; `WeeklySlot.dia` (seg–sáb vs ISO vs `MONDAY`); `flagAvaliacao`. Um dono de OpenAPI mitiga; frontend cruzado (FR-28) não espera o YAML nascer.

**Ação:** **autofix** — estender a linha de tokens de AD-2 / convenções (não copiar o addendum inteiro).

#### F-11 — TypeScript 7.0.2: versão atual, *fit* com Nest/ESLint

Pin `typescript@7.0.2` existe (GA nativo). Ferramentas que ainda precisam da API programática 6.x (`typescript-eslint`, parte do tooling Nest) podem empurrar grupos Node para `typescript@6` enquanto a Stack manda 7. Não é pin inventado; é risco de paved path.

**Ação:** **discuss** — manter 7 e documentar alias `@typescript/typescript6` se o starter Nest quebrar, ou pinar 6.x no paved path Nest até 7.1.

### Low

#### F-12 — Docker Compose versão `v2`

Geração do plugin, não tag (`2.29.x`). Não gera divergência de API entre grupos. **ignore** ou pin da spec Compose no `compose.yaml` seed.

#### F-13 — “Biblioteca de componentes com tema próprio” em Deferred

Já é proibição de AD-8. Em Deferred parece escolhível depois. **autofix** — tirar da lista Deferred (ou rotular “proibido, não adiado”).

#### F-14 — OpenAPI Spec 3.2.0 é o latest; pin 3.1 é consciente

Memlog: springdoc 3.1.0 e Nest swagger alinhados a 3.1. Correto. Não é finding de atualidade — só não deixar um grupo publicar `openapi: 3.2.0`. Já coberto por AD-2/AD-3.

---

## O que a spine acerta (não relitigar)

- Paradigma **malha de bounded contexts com BFF** + tabela Pode/Não pode: altitude certa; oauth não é 10º domínio.
- AD-1 direção de leitura/registros vs alocação e ciclo GET `lessons`↔`reservations` profundidade 1 — o miolo certo; falta só a escrita de geração.
- AD-2 língua, UUID v4, date-only+`faixaId`, RFC 9457 + `code=FR-*`, UI só mapeia `refusal-banner`.
- AD-3 paved path Spring Boot 4.1.x **ou** NestJS 11.2.x, BFF Nest pinado, Spring 3.5 banido (EOL 2026-06-30).
- AD-5 fórmula de sala efetiva no dono `lessons`; FR-29 no BFF; sem 18 reservas da sala padrão.
- AD-7 compose, `local`/`lab`, um database `cras_<contexto>`, sem K8s/IdP SaaS/sensor.
- AD-8 uma SPA Vite, shell vs `ux-s1`…`ux-s9`, tokens `DESIGN.md`, kit com tema próprio = defeito.
- Mapa FR-1–FR-31, NFR-ID/LGPD/CTR/Q, UX-LOGIN/HOME/S1–S9 — rastreável.
- Seed de árvore e portas = `.gitmodules`; greenfield honesto.
- Terse: não copia UJs nem a tabela de faixas.

## Versões (verificadas na web, 2026-08-24)

| Pin da Stack | Checagem |
| --- | --- |
| Spring Boot 4.1.1 | Release 2026-08-20; OSS até 2027-07-31 |
| springdoc-openapi 3.1.0 | Docs atuais; default OpenAPI 3.1 |
| Java 21 | LTS (25 também LTS; 21 ainda válido) |
| Node.js 24.19.0 | LTS Krypton 2026-08-03 |
| NestJS 11.2.1 | `@nestjs/core@latest` |
| Express 5.2.1 | `express@latest` |
| TypeScript 7.0.2 | Tag + npm latest; ver F-11 |
| Keycloak 26.7.2 / `quay.io/keycloak/keycloak:26.7.2` | Release 2026-08-19; `start-dev` no getting-started |
| PostgreSQL 18.6 | Release 2026-08-13 |
| React / React DOM 19.2.8 | Latest 2026-07-21 |
| Vite 8.2.2 | npm latest 2026-08-20 |
| @vitejs/plugin-react 6.1.0 | npm latest (peer vite ^8) |
| react-router 8.3.0 | npm latest 2026-07-22 |
| @tanstack/react-query 5.102.3 | npm `latest` (registry) |
| @fontsource/ibm-plex-sans\|mono 5.3.0 | npm 5.3.0 |
| OpenAPI 3.1 | Consciente vs OAS 3.2.0 (2025-09-19) |
| Direct Access Grant | Ainda existe no Keycloak 26; default off em client novo; tagged `[ASSUMPTION]` — ok para lab |

Adapter Node.js Keycloak DEPRECATED: corretamente banido em AD-4.

## Mermaid e template

- `flowchart TB` (AD-1), `flowchart LR` + subgraph compose, `sequenceDiagram` com `alt/else` de recusa FR: sintaxe válida, labels entre aspas, sem grafo vazio.
- Sem comentários `<!-- TEMPLATE GUIDE -->` nem tokens `{name}`.
- Sem seção Inherited Invariants (não há parent spine) — correto.

## Brownfield / greenfield

`.gitmodules` lista exatamente `backend/{oauth,classes,lessons,reservations,students,courses,resources,professors,rooms,employees,bff}` + `frontend`. Glob em `backend/**` não acha código; não há `compose.yaml` na raiz. Seed = cold-start, não contradiz convenção existente.

## Cobertura spec

| Área | Na spine |
| --- | --- |
| FR-1…FR-31 | Mapa linha a linha (incl. FR-29/30/31 fora de ordem no PRD) |
| NFR-ID-1/2 | AD-1 + mapa |
| NFR-LGPD-1…4 | AD-6 + mapa; LGPD-4 fraco (F-9) |
| NFR-CTR-1…4 | AD-1, AD-2 |
| NFR-Q-1/2 | AD-7, AD-8 |
| UX-LOGIN, UX-HOME, UX-S1–S9 | AD-8 + mapa; OQ-1 deferred de propósito |
| SM-5 | Binds AD-2; SM-1…4 são métricas de demo, não invariante de malha |

## Dimensões da altitude initiative

Decididas: paradigma, limites, dependências de runtime (exceto F-1), contrato HTTP dos contextos, identidade de lab, stack compartilhada vs paved path, sala efetiva, BFF como única origem de browser, UI única, envelope compose/local/lab.

Deferred de verdade: OQ-1, OQ-3, interno do serviço, codegen, eventos/saga/OneRoster, PKCE/CAFe, escopos finos, K8s.

Silêncio a promover a Deferred: CI, TLS, segredos do compose, contract-test (F-7).

## Recomendações de triagem (para o Finalize, não aplicadas aqui)

1. **Autofix agora:** F-1, F-3, F-4, F-5, F-6 (uma frase), F-8, F-9, F-10, F-13.
2. **Discuss com docente/lab:** F-2 (claim de junção / realm), F-11 (TS 7 vs Nest).
3. **Defer com condição:** F-7; OQ-1 permanece turma/docente.
4. **Ignore:** F-12, F-14.

Com F-1–F-4 fechados, o scorecard de divergência do nível abaixo vira pass; o restante não segura um fail sozinho.
