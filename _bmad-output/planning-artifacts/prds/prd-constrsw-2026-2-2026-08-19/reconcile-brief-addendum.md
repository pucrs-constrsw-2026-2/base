# Reconcile — brief addendum

Fonte: `_bmad-output/planning-artifacts/briefs/brief-constrsw-2026-2-2026-08-17/addendum.md` (Malha, atualizado 2026-08-19).  
Contra: `prd.md` + `addendum.md` deste workspace.  
Regra: extrair lacunas — **não** reescrever o PRD. O `reconcile-brief.md` cobre o `brief.md`; este extrato é só o addendum (mapa de costura, esquema 1–N, invariantes numeradas, regulação nomeada, submódulos).

## Covered

Quase todo o addendum entrou no PRD ou no addendum do PRD, com a mesma língua.

- Split registros vs alocação; nove contextos; quatro personas; papel ≠ dono de contexto; Professor is-a Employee (`employeeId` 1–1, sem copiar nome/e-mail); Student não é Employee e não se cadastra na Malha (seed/import).
- Mapa de escrita: `courses`/`professors`/`students`/`employees` publicam; `classes`/`lessons`/`rooms`/`resources`/`reservations` consomem IDs (NFR-CTR-1/2). `employees` não é dono de Room, Resource nem da classe Professor; a persona funcionário opera esses cadastros (UJ-1, §4.1).
- Pares 1–N e atributos v1 (tabelas do addendum) copiados no addendum do PRD. Equidade: IDs alheios são atributo, não terceira classe; `bff`/`frontend`/`oauth` e o catálogo de faixas fora do critério 1–N.
- Sala padrão na abertura; sala efetiva derivada (linha SALA senão `Class.roomId`; remoto sem sala); no máximo uma ReservationLine SALA por Lesson; Resource = caminho feliz; Room = exceção/ad hoc; trocar a sala libera a padrão na `data+faixa` (FR-15, FR-18, FR-23, FR-24).
- Grade canônica A–E, F–N, P (horários, `[início, fim)`, sem E1/E2/O, depois de E vem F) no addendum do PRD; NFR-CTR-4.
- Invariantes 1–7 e 9–11: identidade de Student recebida; roster no mesmo seed (FR-9); coordenador abre Class, professor não cria órfã, várias turmas por disciplina/período (FR-13); capacidade ≥ vagas + AccessibilityFeature (não booleano) (FR-15, FR-21); esqueleto gera Lesson; feriados não excluídos (FR-16); prédio = agrupador textual (FR-20); faixa ≠ décimo contexto; BFF/OAuth compartilhados, cada grupo pluga o próprio domínio (FR-26, FR-27); is-a (FR-4, FR-5).
- Course como agregado regulado com identificador; v1 não protocola no MEC e não inventa disciplina sem id (FR-10, §4.4). Plano datado *antes* do período (FR-11) com SyllabusItem estruturado (FR-12). LGPD: titulares distintos, sem cópia de CPF, exceção acadêmica estreita (NFR-ID, NFR-LGPD). IDs compatíveis com diploma/Censo; XML, RDC-Arq, Censo fora da v1 (§7).
- Formato: OpenAPI contract-first; layout `backend/{oauth,…}` + `frontend`; login institucional; OIDC (RNP/BAITA) e Keycloak no addendum do PRD; sem app nativo, hardware ou sensores no caminho crítico.
- Alternativas rejeitadas, nomes descartados (`SIG*`, suíte, “Campus AI”, marca PUCRS), estrada pós-v1 (Censo → ocupação/no-show → Edu-API → CAFe → agente) e lacunas conhecidas (secretaria, rubrica, bombeiros, SIS ao vivo) — no addendum do PRD.
- IDs canônicos listados em FR-25 (Student, Employee, Professor, disciplina, turma, aula, Room, Resource, Reservation, faixa).

Não é lacuna deste input: frontend cruzado e gateway REST/Keycloak — o addendum do brief não os pedia; o PRD acrescentou (memlog 2026-08-19).

## Gaps

### G1 — Invariante de costura 8 na *abertura* da turma (sala padrão + professor)

- **Ideia no input:** “Não confirma reserva (**nem sala padrão na abertura**) com sobreposição de **sala efetiva**, **recurso** ou **professor** na mesma `data+faixa`.” Dois write-paths, uma regra.
- **Silêncio/diluição no PRD:** FR-24 recusa sobreposição na Reservation. FR-18 recusa duas Lesson na mesma sala efetiva. SM-5 cobre sala/recurso/professor como métrica. **FR-13–16 e UJ-2 só recusam capacidade &lt; vagas ou falta de AccessibilityFeature.** Abertura com a mesma sala padrão (ou o mesmo Professor) em WeeklySlot sobreposto não é consequência testável da oferta.
- **Qualitativo dropped:** a costura como *um* contrato entre `classes` e `reservations`. Sem a abertura, o grupo `classes` lê só §4.5 e trata conflito como problema do grupo `reservations` — exatamente o rasgo que o invariante 8 impede (18 holds vs sala padrão *e* dois professores na mesma faixa).
- **Severidade:** phase-blocker (UX de UJ-2 precisa do estado de erro; arquitetura precisa saber que `Class.create` é write com conflito, não só cadastro + generate).

### G2 — Seta `lessons` → `rooms` (sala efetiva é leitura, não só regra)

- **Ideia no input:** “Sala **efetiva** da aula = reserva de sala daquela aula, senão sala padrão da turma; **`lessons` lê `rooms` por esse valor**.”
- **Silêncio/diluição no PRD:** a derivação está em FR-18 e no glossário. NFR-CTR-1: “`lessons` consome Class.” **Não há seta `lessons` → `rooms`.** Quem resolve a Room efetiva (contexto `lessons`, BFF, `reservations`) fica em branco.
- **Qualitativo dropped:** aula como encontro *em um espaço endereçável* — `lessons` é cliente de `rooms` pelo valor derivado, não um timestamp órfão que o BFF adivinha. O mapa de interação do addendum era o contrato de costura *antes da tela*.
- **Severidade:** should-fix (fronteira de contexto; não bloqueia o restante do PRD se FR-18 permanecer a regra de produto).

### G3 — Plano de ensino como payload `courses` → consulta do estudante

- **Ideia no input:** “Plano de ensino é publicação datada *antes* do período (CNE/CES) — **payload `courses` → consulta do estudante**.”
- **Silêncio/diluição no PRD:** FR-11 diz que Student (e Professor) leem o plano publicado. **UJ-4 e FR-19** — a única superfície de leitura do Student — são *onde / o quê / tem avaliação?* via Lesson + LessonTopic + `flagAvaliacao`. Bibliografia e critério do Course que não viraram tópico de aula não têm jornada.
- **Qualitativo dropped:** o plano como *documento publicado ao aprendiz* (direito CNE/CES), não só como estoque de itens que o professor puxa para a aula. “O quê” da UJ-4 virou fragmento de Lesson, não a publicação datada de `courses`.
- **Severidade:** should-fix (antes do UX fechar UJ-4 só como lista de aulas).

### G4 — Repositório base = *submódulos*, não só pastas

- **Ideia no input:** “Repositório base: **submódulos** `backend/{oauth,classes,…}` + `frontend`.”
- **Silêncio/diluição no PRD:** addendum do PRD: “Layout de repositório herdado” + a mesma lista de paths. A palavra **submódulos** (vários repositórios, um por contexto) caiu.
- **Qualitativo dropped:** a forma organizacional da ConstrSW (nove clones, não um monorepo com pastas). Quem ler só o PRD pode tratar o layout como convenção de diretório.
- **Severidade:** should-fix (handoff de arquitetura / setup; o `.gitmodules` do repo já existe).

### G5 — Voz de costura numerada + instrumentos nomeados

- **Ideia no input:** onze “invariantes de costura” como feixe compartilhado; “Não forçam monolito. Forçam identidades e documentos”; e-MEC / Decreto 9.235; CNE/CES; specialists nomeados (CollegeNET/Ad Astra ao lado do SIS); AccessibilityFeature **pertinente**.
- **Silêncio/diluição no PRD:** as regras testáveis sobreviveram espalhadas em FR/NFR. Sumiram o **feixe** (“costura”), os números dos invariantes, os nomes legais (resta “agregado regulado” / “não protocola no MEC”), os nomes CollegeNET/Ad Astra (“padrão dos specialists de grade”) e “pertinente” (virou “pelo menos um” AccessibilityFeature).
- **Qualitativo dropped:** tom de *malha que não pode rasgar* e de *regulação que não vira monolito* — o que os nove grupos deveriam citar juntos. A malha de FRs herdou o mecanismo e perdeu o nome do contrato e os ganchos da pesquisa que o addendum mandou não relitigar.
- **Severidade:** defer (não muda o MVP; vale a pena se houver passagem de Vision/Constraints ou de handoff para arquitetura). “Pertinente” vs “pelo menos um”: defer — no modelo v1 não há perfil de acessibilidade do Student para casar feature.

## Conflicts

Nenhum conflito com regra travada do addendum do brief.

Notas (não conflito):

- Frontend cruzado e gateway REST para Keycloak são **acréscimos** do PRD (addendum do brief só exigia plugar papéis/escopos no OAuth compartilhado e incorporar o domínio no BFF).
- “Brief em inglês rejeitado” é disciplina de artefato; o PRD já está em pt-BR.
- Feriados na v1 estão no FR-16 como `[ASSUMPTION]`; o addendum do PRD não os repete em “Lacunas conhecidas” — colocação, não contradição.
