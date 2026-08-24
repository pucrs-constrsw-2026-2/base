---
reviewer: adversarial-general
lens: duas unidades um nível abaixo que obedecem todos os ADs à letra e mesmo assim divergem
spine: ARCHITECTURE-SPINE.md
date: 2026-08-24
status: complete
spine_unchanged: true
altitude: initiative
---

# Review adversarial — Architecture Spine Closed CRAS

Ataque: para cada buraco, duas unidades (contexto, módulo BFF ou superfície) constroem de forma **legal** sob AD-1…AD-8 + convenções e, juntas, não fecham malha — forma compartilhada incompatível, dois donos da mesma entidade, ou dois caminhos de mutação do mesmo estado. Spine **não** alterada. Cada par é um AD novo ou um Rule a apertar.

Glossário, UJs e OQ-1 não foram relitigados como produto; só entram quando a spine os deixa como escolha de implementação.

## Veredito

**A spine não é contrato suficiente para nove grupos em paralelo.** AD-1…AD-8 fecham direção grosseira, UUID/faixa/problem+json, paved path e o recorte BFF/SPA. Não fecham: quem **persiste** Lesson; o **schema** e o **universo** de `GET ocupacao`; o vínculo `sub` Keycloak ↔ entidade de domínio; o dono dos read-models FR-19/FR-29; nem o JSON dos pares 1–N além de meia dúzia de tokens. Unidades compliant colidem no dia 1. Não é handoff-clean.

## Método

Uma unidade “obedece à letra” se: respeita as setas de AD-1 como únicas dependências de runtime; publica `openapi/openapi.yaml` 3.1 com `/v1/<contexto>` e os tokens listados em AD-2; escolhe Spring **ou** Nest (AD-3); resource-server + `GET /v1/identidade/me` (AD-4); não calcula sala efetiva fora de `lessons` (AD-5); não abre CORS de SPA nem junta OpenAPI no browser (AD-6); sobe no compose com `cras_<contexto>` (AD-7); UI só fala com BFF via TanStack Query (AD-8). O teste falha se duas dessas unidades não integram.

## Pares (buracos)

### P1 — `classes` × `lessons`: dois donos da escrita de Lesson

**Unidade A (`classes`).** POST `/v1/classes` confirma a turma, recusa via `GET /v1/lessons/ocupacao?data=&faixaId=` (única seta CLA→LES no diagrama), e **não** chama nenhum POST em `lessons` — AD-1: “setas abaixo são as únicas dependências de runtime permitidas”. Lesson “gerada” vira atributo interno, job, ou espera que outro dispare. Capability map e NFR-CTR-2 (“Class que gera Lesson”) ficam como comentário de produto, não como seta.

**Unidade B (`lessons`).** Único persistidor de Lesson (equidade 1–N, AD-5 dono da derivação). Expõe `POST /v1/lessons` e/ou `POST /v1/lessons/gerar`. Sem barramento (convenção), não é notificado da confirmação. Espera que `classes` ou o BFF chamem. GET em `classes` (seta LES→CLA) só ajuda se alguém disparar o pull.

**ADs cumpridos:** AD-1 (A respeita o diagrama; B respeita LES→CLA), AD-5 (B é dono da derivação), AD-7 (DBs separados — A não pode inserir em `cras_lessons`), convenção “mutação no contexto dono”.

**Colisão:** UJ-2 confirma Class e a grade de aulas não nasce; ou os dois implementam geração e duplicam Lesson no retry. O Rule de AD-1 cita NFR-CTR-2 (“gera Lesson”) e o diagrama **proíbe** a seta de escrita que tornaria isso possível. Unidades honestas escolhem lados opostos da contradição.

**Fechar:** apertar AD-1 — ou (i) seta de escrita explícita e única `classes` → `POST /v1/lessons/gerar { classId }` (lessons persiste; classes não grava aula), ou (ii) BFF orquestra POST Class depois POST gerar, e o diagrama admite essa orquestração sem persistência de agregado. Uma das duas. Proibir as duas ao mesmo tempo.

### P2 — `classes` × `reservations`: ocupação que cada escritor inventa

**Unidade A (`classes`, FR-13).** Recusa abertura só com `GET /v1/lessons/ocupacao` (AD-5). Interpreta o payload como salas efetivas de **Lesson**. SALA **ad hoc** (Reservation sem `lessonId`) não é Lesson → não entra. Compromisso de Professor: AD-5 descreve ocupacao como `salaEfetivaId` já resolvido — A consulta `professorIds` só nas próprias Classes (não vê Reservation do mesmo Professor nem Lesson de outra turma, a menos que ocupacao as traga — o Rule não manda).

**Unidade B (`reservations`, FR-23/24/30).** Recusa no próprio POST chamando a mesma ocupacao (AD-5) **e** as próprias linhas. Inclui Resource, linha SALA ligada a Lesson, e ad hoc. Professor ocupado = Reservation local ∪ “se ocupacao devolver professor”. Se não devolver, B só vê as próprias reservas — Lesson do coordenador não bloqueia projetor na mesma faixa.

**ADs cumpridos:** AD-1 (RSV→LES GET ocupacao; CLA→LES GET ocupacao; profundidade 1), AD-5 letra (ambos recusam no escritor; BFF não calcula sala efetiva), AD-2 (`data`+`faixaId`).

**Colisão:** (1) Rafael abre turma na Room que já tem reserva ad hoc na faixa — A autoriza; FR-29 depois mostra dois ocupantes. (2) Marina tem Reservation ad hoc na H; Rafael abre Class com o mesmo `professorId` na H — A autoriza. (3) B autoriza Resource na faixa em que já existe Lesson do mesmo Professor, se ocupacao for só sala. SM-5 quebra com implementações legais.

**Fechar:** apertar AD-5 (e AD-2) — schema canônico de `GET /v1/lessons/ocupacao` **e** o universo: salas efetivas de Lesson, **mais** Room de SALA ad hoc, **mais** `professorId`s ocupados (Lesson via Class.professorIds **e** Reservation.professorId). `reservations` permanece o dono das linhas; `lessons` é o único *leitor de composição* que os escritores de conflito chamam. Sem segundo algoritmo de ocupação em `classes` ou no BFF.

### P3 — `lessons` × `reservations`: dois caminhos de mutação da sala efetiva

**Unidade A (`lessons`, FR-17).** PATCH `modalidade=remoto` é ajuste de Lesson; Professor atribuído pode. Fórmula AD-5 na ordem escrita: “Remoto → sem Room” curto-circuita linha SALA. `GET ocupacao` deixa de emitir a sala padrão **e** o `targetId` da linha SALA. A só **GET** linhas SALA (AD-1: ciclo só GET, profundidade 1) — não DELETE em `reservations`.

**Unidade B (`reservations`).** Linha `kind=SALA` daquela `lessonId` continua persistida até FR-30. Não é ad hoc. AD-5 manda o BFF unir ocupacao de lessons com “linhas RECURSO e SALA **ad hoc**” — linha ligada a Lesson **não** entra no join do BFF. B não é chamado no PATCH de modalidade.

**ADs cumpridos:** AD-1 (sem escrita lessons→reservations), AD-5 (A é o único dono da derivação; B não persiste `salaEfetivaId`), AD-6 (BFF não recalcula).

**Colisão:** aula vira remota; Caio (FR-19) não vê Room (A); a Room alvo da linha SALA some da ocupação FR-29 (não está em lessons, não é ad hoc); Rafael pode abrir outra Class naquela Room; B ainda “segura” o recurso espacial. Cancelar depois reabre um alvo que já foi reatribuído. Estado da sala efetiva tem dois escritórios: PATCH Lesson vs POST/DELETE ReservationLine, sem invariante de compensação.

**Fechar:** apertar AD-5 — ordem da fórmula: remoto **não** ignora linha SALA existente; ou PATCH `modalidade=remoto` é recusa FR enquanto existir linha SALA daquela `lessonId` (A GET linhas, 409); ou a única mutação que libera sala é FR-30, e `lessons` não pode marcar remoto sem essa pré-condição. Escolher um caminho de mutação.

### P4 — `lessons` × `reservations`: forma compartilhada de ocupacao e de linha SALA

**Unidade A (`lessons`).** Publica OpenAPI 3.1 em `/v1/lessons`. Tokens AD-2 presentes (`lessonId`, `salaEfetivaId`, `faixaId`, `roomId`). Ocupacao:

```json
{ "data": "2026-03-12", "faixaId": "H", "salaEfetivaId": "…" }
```

Um único id (a “sala da vez”). Lista de aulas da faixa omitida. GET linhas: A chama `GET /v1/reservations?lessonId=&kind=SALA` — path que AD-2 não fixa (só `/v1/reservations`).

**Unidade B (`reservations`).** OpenAPI legal. Linhas embutidas no agregado:

```json
{ "id": "…", "linha": [{ "tipo": "SALA", "alvoId": "…" }] }
```

`tipo`/`alvoId` não estão na lista de tokens de AD-2 (`kind=SALA` e `targetId` vivem só na prosa de AD-5). Collection GET devolve array nu, sem filtro `lessonId`.

**ADs cumpridos:** AD-2 (paths `/v1/lessons`, `/v1/reservations`; UUID; `data`+`faixaId`; tokens *listados*), AD-5 (conceito kind=SALA).

**Colisão:** A não parseia `linha[].tipo`; B não parseia ocupacao sem array de salas/lessons. Integração GET profundidade 1 quebra em runtime com dois YAMLs “AD-2 green”. O fio compartilhado pinou o vocabulário errado: pinou `employeeId` e esqueceu `kind`, `targetId`, `professorId`, `studentId`, `modalidade`, o envelope de ocupacao e o envelope de coleção.

**Fechar:** apertar AD-2 — JSON Schema (ou fragmento OpenAPI compartilhado em `oauth/contracts`) para: ocupacao, Reservation, ReservationLine (`kind` enum `SALA|RECURSO`, `targetId`), Lesson (modalidade, `flagAvaliacao`, `classId`), e envelope de lista (`{ items }`). Proibir segundo nome para esses campos. Query canônica `GET /v1/reservations?lessonId=&kind=`.

### P5 — `employees` × `professors`: um is-a, dois agregados, duas chaves

**Unidade A (`employees`).** Agregado Employee, UUID v4 em `id`. JSON `{ id, nome, email, ativo }` — “nome/e-mail” são prosa do PRD, não token AD-2. Nested `GET /v1/employees/{id}/employment-bonds`. `GET /v1/identidade/me` resolve o usuário por e-mail do JWT (`preferred_username`).

**Unidade B (`professors`).** is-a 1–1. Como PK do Professor = `employeeId` (um UUID só, “não copiar cadastro”). `GET /v1/professors/{employeeId}`. `GET /v1/identidade/me` resolve por claim `sub`. AcademicDegree em `/v1/academic-degrees?professorId=`. Não chama o nested de A; espera `{ employeeId, name, mail, active }` em Employee.

**ADs cumpridos:** AD-1 (PRO→EMP; IDs alheios são atributos), AD-2 (`employeeId` existe em B; `id` UUID em A), AD-4 (ambos têm `/v1/identidade/me` do próprio domínio), FR-5 espírito (não copiam nome).

**Colisão:** `classes` guarda `professorIds` (FR-13). São ids de Professor ou de Employee? A e B divergem; `classes` escolhe um e o outro contexto 404. UX-S2 resolve nome via BFF: um módulo busca `id`, o outro `employeeId`. Dois `/me` no mesmo JWT. NFR-ID “um dono por tipo de pessoa” não fixa a chave canônica do papel docente.

**Fechar:** apertar AD-2/AD-4 — Professor tem UUID v4 **próprio** (`professorId`); `employeeId` obrigatório 1–1 distinto; consumidores de alocação guardam **`professorId`**, nunca `employeeId`. Shape de Employee publicado (`id`, `nome`, `email`, `ativo`). Path 1–N pinado (`/v1/employees/{employeeId}/bonds` e `/v1/professors/{professorId}/degrees`). Um algoritmo de resolução `sub`/e-mail → entidade, no catálogo oauth — não nove.

### P6 — `employees` × (`classes` | `reservations`): OQ-2 sem aresta de leitura

**Unidade A (`employees`).** Desativar: `ativo=false`; EmploymentBond permanece (FR-1). Convenção OQ-2: Class somente leitura; nova Reservation recusada.

**Unidade B (`reservations`).** Recusa nova Reservation se o titular estiver desativado. Setas AD-1 de B: RSV→ROM, RSV→RES, RSV→PRO, RSV→LES (GET). **Não há RSV→EMP.** GET Professor não copia `ativo` (AD-1/FR-3). B inventa: (i) cache local de `ativo` no POST de Reservation (cópia de cadastro — espírito FR-3, mas AD-1 não a impede se for um booleano “snapshot”), ou (ii) ignora OQ-2 porque não pode ler Employee sem violar o diagrama.

**Unidade B′ (`classes`).** Setas: CLA→CRS/PRO/ROM/STU e GET ocupacao. **Não há CLA→EMP.** Mesmo buraco para “Class somente leitura”.

**ADs cumpridos:** AD-1 à letra (não inventam seta), convenção OQ-2 como *intenção*, FR-3 (B′ não copia e-mail).

**Colisão:** Helena desativa Employee; Marina ainda reserva; Rafael ainda PATCH Class. Ou B copia `ativo` e divergem os snapshots. A convenção exige um fato que o diagrama torna ilegível.

**Fechar:** ou seta de leitura `professors`→`employees` **já existe** e GET `/v1/professors/{id}` passa a incluir `employeeAtivo` **derivado** (não cópia de nome/e-mail) — pinado em AD-2; `classes` e `reservations` leem só isso — ou OQ-2 sai das convenções da malha e vira regra só de `employees`+BFF. Não deixar os dois.

### P7 — `students` × `classes`: dois donos de `Class.studentIds`

**Unidade A (`students`).** Convenção: seed JSON idempotente por `id` em `students`. FR-9: roster chega no **mesmo** seed/import. A inclui `classIds` no JSON de carga e persiste o vínculo no próprio DB (não é AcademicAffiliation — FR-8 — então cria tabela de matrícula, ou empilha ids na afiliação “para a demo”). Sem seta STU→CLA no diagrama: A **não** PATCH `classes`.

**Unidade B (`classes`).** `studentIds` é atributo da principal Class (addendum). Seed/abertura grava o array. GET `students` para recusar id inexistente (CLA→STU). Não lê o JSON de A.

**ADs cumpridos:** AD-1 (A não chama classes; B não chama students para escrever), AD-8 (sem tela de alta), convenção Student, FR-7/FR-9 cada um no seu recorte.

**Colisão:** Caio existe em `students` e não está em nenhuma Class; ou B tem `studentIds` fantasma recusados por A; ou os dois seeds aplicam o roster e um overwrite apaga o outro. UJ-4 depende desse array. Dois donos da mesma associação.

**Fechar:** apertar AD-1/convenção Student — **único escritor** de `Class.studentIds` = `classes`. Artefato de carga: `students` importa só Student+AcademicAffiliation; roster é arquivo (ou seção) consumido **só** por `classes`, que GET `students` para FR-7. Proibir `classIds` no contexto `students`.

### P8 — `courses` × `classes`: FR-11 com dois calendários

**Unidade A (`courses`).** Publica plano com `dataPublicacao`. Recusa FR-11 se a data não for “antes do início do período”. Sem seta CRS→CLA: A não lê Class. Inventa `periodoInicio` no Course (ou “período letivo 2026/2” string).

**Unidade B (`classes`).** Class tem período início/fim (addendum). Abertura exige plano já publicado. GET Course (CLA→CRS). Compara `Class.periodoInicio` com `Course.dataPublicacao` — ou só checa se o plano existe.

**ADs cumpridos:** AD-1 (setas só CLA→CRS), AD-2 (`courseId`), mapa FR-11 em courses / FR-13 em classes.

**Colisão:** plano publicado “antes do período do Course”; Class abre com início **anterior** à publicação; ou o contrário — A recusa publicar porque o Course.periodo já passou, B ainda nem existe. A recusa FR-11 não é a mesma comparação que B usa na abertura.

**Fechar:** apertar AD-2 — período da oferta vive **só** em Class (`periodoInicio`, `periodoFim`, `YYYY-MM-DD`). FR-11 no escritor `courses`: ou deixa de depender de Class (recusa só se já existir Class consumidora — exige seta CRS→CLA GET, hoje ilegal) ou a recusa FR-11 **move** para `classes` na abertura (plano com `dataPublicacao >= Class.periodoInicio` → 409). Um dono da comparação.

### P9 — módulo BFF `lessons` × módulo BFF `reservations`: dois donos de FR-29 (e FR-19)

**Unidade A (`backend/bff/src/modules/lessons`).** AD-6: cada grupo adiciona o **próprio** domínio. FR-29 e FR-19 “são read-models do BFF que consomem AD-5”. A implementa `GET /v1/ocupacao?data=&faixaId=` e `GET /v1/me/aulas` (Student), juntando lessons+rooms e, por zelo, chamando reservations.

**Unidade B (`modules/reservations`).** Mesma frase AD-6. FR-29 precisa das linhas RECURSO e SALA ad hoc — isso é *o* domínio de B. Implementa `GET /v1/occupancy` e join próprio. Módulo `reservations` não inclui `studentIds` (AD-6) — B cumpre. Não implementa roster.

**ADs cumpridos:** AD-6 (cada um no seu diretório; sem CORS nos contextos; sem roster em reserva), AD-5 (não recalculam sala efetiva; chamam GET lessons), AD-8 (UI via BFF).

**Colisão:** UX-S4 não tem path canônico; duas respostas, dois envelopes, possível dupla contagem da linha SALA de Lesson (A já a refletiu em `salaEfetivaId`; B lista a linha). FR-19: `modules/students`, `modules/lessons` e `modules/classes` também podem se achar donos (plano+aulas+roster filtrado). “Sem dono exclusivo do BFF” virou **sem dono** dos únicos joins que a UI precisa.

**Fechar:** AD novo (composição BFF) — tabela rota → **um** módulo:

| Read-model | Path BFF | Módulo |
| --- | --- | --- |
| FR-29 | `GET /v1/ocupacao?data=&faixaId=` | `lessons` (chama reservations; não o contrário) |
| FR-19 | `GET /v1/students/me/agenda` | `students` (chama lessons+courses; sem inventar sala efetiva) |
| FR-31 | já em `classes` | manter |
| login | `POST /v1/auth/login` | não é domínio; pinado em `bff` (`src/auth`), não em oauth-como-serviço |

Proibir segundo path para o mesmo join.

### P10 — `bff`/`oauth` × `frontend` (shell × qualquer UX-S*): sessão e `/me`

**Unidade A (BFF, AD-4).** `POST /v1/auth/login`, cookie httpOnly SameSite=Lax, sem nome/path/domain pinados. `Set-Cookie` no host `:8088`. CORS para origem do frontend (AD-6). JWT `iss` = `http://keycloak:8080/realms/closed-cras` (rede compose).

**Unidade B (UX-LOGIN em `src/shell`).** POST absoluto `http://localhost:8088/v1/auth/login`. TanStack Query default **sem** `credentials: 'include'`. Superfície UX-S1 (outro grupo, AD-8 “no máximo um grupo por UX-S*”) usa `VITE_API_URL=/v1` na origem `:5173` (sem proxy no seed). `GET /v1/identidade/me` — nove contextos expõem isso; o BFF não tem path único de sessão. Shell pede `/v1/identidade/me` e recebe 404 ou o `/me` de `employees` com shape de Employee enquanto o papel é Student.

**ADs cumpridos:** AD-4 (browser não fala com Keycloak; sem localStorage; Direct Access Grant), AD-6 (CORS só no BFF), AD-7 (portas 5173/8088/8080), AD-8 (uma SPA, Query → BFF).

**Colisão:** login “funciona” no banner e a jornada morre na superfície seguinte (cookie host-only / same-site cruzando portas sem credentials / sem proxy). Quatro papéis, nove `/me`, zero contrato de identidade de UI (`nome` + `papel` + ids). AD-3 autoriza Spring e Nest no resource-server: um mapeia `realm_access.roles` = `funcionario`; o outro exige `ROLE_funcionario` ou `aud` = client do serviço → 403 seletivo por contexto, BFF “legal” nos dois.

**Fechar:** apertar AD-4/AD-7/AD-8 — (i) seed: proxy Vite `/v1` → BFF **ou** cookie `Path=/; SameSite=Lax` na **mesma origem** via compose (Caddy/nginx na frente); (ii) cookie nome `closed_cras_session`; (iii) QueryClient do shell com `credentials: 'include'`; (iv) **um** `GET /v1/auth/me` no BFF (papel realm + `employeeId`/`professorId`/`studentId` resolvidos por regra oauth); (v) JWT: `iss` interno vs host documentado em env; papéis **exatos** `funcionario|coordenador|professor|student` sem prefixo `ROLE_`; `aud` aceito pelos nove. Adapters oauth de referência implementam isso — não “só um exemplo”.

### P11 — `classes` × `lessons`: WeeklySlot e o loop de ocupacao

**Unidade A (`classes`).** WeeklySlot: `{ dayOfWeek: 1, faixaId: "H" }` (ISO 1=segunda). Período `{ start: "2026-03-02", end: "2026-07-04" }`. Na abertura, chama ocupacao **uma vez por par data×faixa** (~dezenas/centenas de GET). Snapshot muda no meio do loop.

**Unidade B (`lessons`).** Gera Lesson com `weekday: "SEG"` herdado do GET Class — não casa com `1`. Implementa `GET /v1/lessons/ocupacao?de=&ate=` (não pinado) **ou** só o query `data`+`faixaId` de AD-5. Segunda-feira Java `DayOfWeek` vs JS `getDay()` (0=domingo).

**ADs cumpridos:** AD-2 (`faixaId`, `data` YYYY-MM-DD; **não** pina dia da semana nem período), AD-5 (query documentada é um par data+faixa).

**Colisão:** aulas nascem no domingo; ou esqueleto “seg” não gera nada; FR-13 aprova uma metade do período. Forma compartilhada do esqueleto é o input da geração (P1) — mesmo que P1 feche o *quem*, este par quebra o *quê*.

**Fechar:** apertar AD-2 — `WeeklySlot.diaDaSemana` enum `SEG|TER|QUA|QUI|SEX|SAB` (nunca 0–6). `periodoInicio`/`periodoFim` date-only. Ocupacao de intervalo: `GET /v1/lessons/ocupacao?de=&ate=&faixaId=` **ou** body de verificação na abertura (`POST /v1/lessons/verificar-esqueleto`) atômico — proibir 90 GETs como protocolo.

### P12 — `rooms` × (`classes` | `reservations`): cadastro de Room que os escritores não leem igual

**Unidade A (`rooms`).** `{ id, codigo, predio, occupancy, enabled, features: [{ code, present }] }`. AD-2 pina `roomId`, não `capacidade`, `ativo`, `AccessibilityFeature`.

**Unidade B (`classes`, FR-15).** GET Room; recusa se `capacidade < vagas` ou se não houver AccessibilityFeature. Procura `capacidade` e `accessibilityFeatures[]`. 404 semântico: Room “cabe” sempre (campo ausente) ou recusa sempre.

**Unidade B′ (`reservations`, FR-24).** Mesma FR, outro parser (`capacity`, `acessivel: true` — boolean na principal, o que o PRD proíbe mas AD-2 não).

**ADs cumpridos:** AD-1 (IDs; CLA→ROM, RSV→ROM), AD-2 (UUID, `/v1/rooms`).

**Colisão:** Rafael abre Class em Room sem feature porque A usou `features` e B olhou `accessibilityFeatures`. Marina reserva SALA na mesma Room e toma 409. Equidade 1–N do par Room/AccessibilityFeature não tem JSON.

**Fechar:** apertar AD-2 — Room `{ roomId, codigo, predio, capacidade, ativo }`; AccessibilityFeature `{ codigo, descricao }`; path `/v1/rooms/{roomId}/features`. Recusa FR-15/24 = `capacidade` numérico **e** `features.length >= 1`. Mesmo para Resource `{ resourceId, nome, tipo, ativo }` + InventoryItem `{ identificador, descricao, estado: disponivel|indisponivel }` (FR-22).

### P13 — `oauth` × qualquer contexto: `faixas.json` e o relógio da faixa

**Unidade A (`lessons`).** Carrega `backend/oauth/contracts/faixas.json` (AD-2). Parser: mapa `{ "A": { "start": "08:00:00", "end": "08:45:00" } }`. Conflito [início,fim) com Date UTC.

**Unidade B (`reservations`).** O arquivo real (ainda seed) é lista `{ faixaId, inicio, fim }` `"08:00"`/`"08:45"`. Extremos que se tocam: A e B (08:45) — um trata overlap, o outro não (NFR-CTR-4). Códigos minúsculos `"a"` passam num validador e falham no outro.

**ADs cumpridos:** AD-2 (não copiar a tabela; cânone A–E F–N P; [início,fim)), AD-3 (ambos lêem o catálogo).

**Colisão:** duas aulas “adjacentes” no extremo: um escritor 409, o outro 201. Grade visível em UX-S4/S6/S7 (faixa-picker) ainda outra interpretação.

**Fechar:** apertar AD-2 — schema de `faixas.json` no próprio catálogo (lista ordenada, `faixaId` enum, `inicio`/`fim` `HH:mm`, timezone implícito America/Sao_Paulo, comparação em minutos locais). Consumidores não reinterpretam ISO datetime.

### P14 — UX-S7 × `lessons` × `reservations`: uma superfície, duas mutações, zero ordem

**Unidade A (frontend `src/surfaces/ux-s7`, grupo cruzado).** Uma superfície, duas zonas (EXPERIENCE). Duas mutations TanStack: PATCH Lesson e POST Reservation. Ordem na UI: primeiro tópico/modalidade, depois projetor/sala. Obedece AD-8 (uma pasta, Query→BFF, tokens.css).

**Unidade B (`lessons` + seu módulo BFF).** PATCH Lesson é o agregado. Não sabe de Reservation.

**Unidade C (`reservations` + seu módulo BFF).** POST Reservation com `lessonId`. Recusa via ocupacao (P2/P3).

**ADs cumpridos:** AD-8 (um grupo por UX-S*; S7 é uma superfície), AD-5/AD-6 (UI não decide colisão; roster não na S7), OQ-1 aberto (spine não atribui dono — o grupo de S7 **não** é o de `lessons` nem o de `reservations`).

**Colisão:** A dispara as duas mutations em paralelo; C GET ocupacao antes do PATCH remoto de B commit; ou o contrário (P3). Shell e S7 não compartilham query keys (`['lessons', id]` vs `['aulas', id]`) — S4/S9 mostram sala velha. OQ-1 no Deferred **é** um par: dois grupos escolhem S7; AD-8 diz “no máximo um” e a spine recusa atribuir. “Deferred porque a turma fecha” não impede divergência no cold-start do repo `frontend`.

**Fechar:** apertar AD-8 — query keys canônicas; S7 serializa zona Lesson **depois** zona Reservation (ou o inverso, pinado) no BFF (`POST /v1/lessons/{id}/ajuste-com-reserva` **proibido** se reintroduzir dono BFF de agregado — então: UI sequencia, BFF não). OQ-1: protocolo de claim (`frontend/OWNERS` ou tabela no oauth) **antes** do código das superfícies, mesmo sem nomes de grupo. Deferred sem mecanismo ainda é buraco.

### P15 — Spring `employees` × Nest `professors`: o mesmo JWT, duas AuthZ

**Unidade A (Spring Boot 4.1.x, AD-3 permitido).** Resource-server default: autoridade `ROLE_funcionario`; `aud` = `account`; 401 se `iss` ≠ `http://localhost:8080/...`.

**Unidade B (Nest 11.2.x, AD-3 permitido).** Guard lê `realm_access.roles` literal `funcionario`; ignora `aud`; `iss` = hostname compose `keycloak`.

**ADs cumpridos:** AD-3 (um paved path cada), AD-4 (JWKS do realm, papéis v1 nomeados, `/v1/identidade/me`, catálogo `roles-scopes.yaml` sem dono exclusivo — cada PR adiciona escopos do próprio domínio com nomes que o outro não lê).

**Colisão:** Helena autentica; POST Employee 201; POST Professor 403. Demo UJ-1 morta. “PR de qualquer grupo” no YAML de escopos é o segundo dono: dois PRs definem `scope: class:write` vs `classes.write` vs papel só.

**Fechar:** apertar AD-4 — adapters de referência **são** o comportamento obrigatório (não ilustração): extractor de papéis, issuer(s) aceitos, audience. Catálogo: só os quatro papéis na v1; escopos extras no Deferred (já está) **e** regra “não inventar scope até lá”. Compose: `KEYCLOAK_ISS` único interno + `KEYCLOAK_ISS_HOST` se o browser nunca vê o IdP (não vê — então um iss, o interno).

### P16 — dois grupos no `compose.yaml`: Postgres, `/health`, dono do arquivo

**Unidade A (`employees`).** Adiciona `CREATE DATABASE cras_employees` num `initdb.d/01.sql`. Health Spring `/actuator/health`. Serviço `backend-employees`.

**Unidade B (`rooms`).** Mesmo `initdb.d/01.sql` (last-write-wins no clone). Health `GET /health` → `{ "ok": true }` (AD-7 pina o path, não o body). Serviço `rooms`. `depends_on` postgres sem database ready.

**ADs cumpridos:** AD-7 (um compose, um Postgres, `cras_<contexto>`, `GET /health` anônimo, portas seed).

**Colisão:** segundo init falha ou apaga o primeiro; healthcheck do compose verde com processo up e DB down; BFF env `EMPLOYEES_URL=http://employees:8101` não resolve. Seed operacional tem dono implícito (`oauth`? raiz da malha?) e nove autores.

**Fechar:** apertar AD-7 — `compose.yaml` na raiz é da malha; lista de databases `cras_employees`…`cras_reservations` + `keycloak` num **único** init da raiz (não nos submódulos). Serviço = nome do contexto. `GET /health` → 200 `{"status":"ok"}` sem auth. Variáveis `*_URL` no BFF pinadas.

## Achados laterais (mesmo teste, menos centrais)

- Envelope RFC 9457: `code`/`type` pinados; `title`/`detail` e maiúsculas `FR-13` vs `fr-13` soltos — UI `refusal-banner` mapeia errado (AD-2 × AD-8).
- 409 vs 404 no FR-4 (Employee ausente): AD-2 dá os dois status para casos vizinhos; `professors` escolhe um, UX-S2 o outro.
- `PATCH` vs `PUT` vs `POST …/cancel` em FR-30 — UX-S7 e `reservations`.
- `BFF_ORIGIN` na convenção vs origem CORS do frontend vs URL pública do BFF — três leituras.
- Paradigma “SPA por papel” vs AD-8 uma SPA — dois grupos leem o paradigma e abrem `frontend-professor/` (já atacado no reconcile UX; aqui: par shell × “SPA do Student”).
- Codegen OpenAPI no Deferred: sem envelope comum (P4/P12), dois geradores legalizam a divergência.
- `GET /v1/lessons/:id` “lê rooms por esse id”: A embute objeto Room (cópia de cadastro); B devolve só `salaEfetivaId`; BFF FR-19/S9 escolhe um e Caio vê código da sala ou UUID.

## O que a spine já impede (não relitigar)

UI→domínio direto; BFF persistindo `salaEfetivaId`; 18 Reservation da sala padrão; `/funcionarios` vs `/employees` no path; `pessoa`/`evento`/`slot` como nomes de agregado; datetime solto no lugar de data+faixa; cookie em `localStorage`; `keycloak-js`; Next/RSC; schema SQL único; quatro SPAs; kit MUI com tema próprio; CORS de SPA nos nove.

Esses Prevents funcionam. Os pares acima vivem **no interior** do que restou opcional.

## Fechos sugeridos (para o autor; não aplicados)

1. **AD-1:** resolver a contradição diagrama vs “Class gera Lesson”; arestas de leitura que OQ-2 e FR-11 exigem, ou mover a regra para quem já tem a aresta.
2. **AD-2:** schema compartilhado (ocupacao, pares 1–N, envelopes, faixas.json, dia da semana, problem+json `code` canônico) — hoje o “fio” é uma lista de 6 tokens.
3. **AD-4:** um `/v1/auth/me`; binding `sub`↔ids; papéis JWT literais; iss/cookie/proxy de laboratório.
4. **AD-5:** universo e schema de ocupacao; um caminho de mutação remoto vs linha SALA; verificação de esqueleto atômica.
5. **AD-6/AD-8:** dono (módulo + path) de FR-19 e FR-29; query keys; claim OQ-1.

Sem isso, nove implementações “AD-compliant” não produzem UJ-1–UJ-4.
