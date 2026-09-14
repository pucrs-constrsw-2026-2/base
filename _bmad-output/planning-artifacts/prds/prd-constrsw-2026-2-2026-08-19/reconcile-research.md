# Reconcile — domain research 2026-08-17

Fonte: `_bmad-output/planning-artifacts/research/domain-university-academic-and-resource-management-research-2026-08-17.md`  
Contra: `prd.md` + `addendum.md` deste workspace (herdam o brief 2026-08-17; não relitigar).  
Regra: extrair lacunas qualitativas ou FRs que deixaram a ideia cair. **Não** reabrir decisões travadas no brief (em especial **Professor is-a Employee**).

## Covered (via brief inheritance)

A pesquisa já foi absorvida no brief; o PRD herda o recorte, não o relitiga.

- Split **registros** vs **alocação**; posição de specialist (mesh + contratos) ao lado do SIS, não suíte (SIGAA/TOTVS/Ellucian).
- Nove contextos, quatro personas; papel ≠ dono de contexto; mapa de escrita (staff / coordenador / professor / student).
- Língua: disciplina ≠ turma ≠ aula ≠ reserva; o brief acrescentou **faixa**, **sala padrão** e **sala efetiva** — o PRD ancora isso no Glossário e em FR-14, FR-15, FR-18, FR-25.
- **Professor is-a Employee** (`employeeId` 1–1, sem copiar cadastro). A pesquisa tratava professor e funcionário como contextos de pessoa distintos (“overlap in life, not in context”). O brief **sobrescreveu**; o PRD correto é FR-4/FR-5, não um “conflito a corrigir”.
- Student: identidade recebida (seed/import); sem tela de alta; roster na Class.
- Course como agregado regulado: identificador alinhável na v1; sem protocolo e-MEC/DCN (FR-10; out of scope).
- Plano de ensino = publicação datada *antes* do período, estruturada (FR-11, FR-12) — CNE/CES herdado.
- Room como objeto com capacidade + acessibilidade (não booleano; AccessibilityFeature 1–N); sem motor de alvará/bombeiros.
- Contract-first; BFF para não explodir join na UI; OIDC/Keycloak no addendum; sem app nativo, sem hardware, sem sensores no caminho crítico.
- LGPD: cadastro administrativo em escopo (não se esconder no art. 4º); sem cópia cruzada de CPF; Lesson + Student nomeado = dado de localização (NFR-LGPD-1–3).
- IDs de students/courses permanecem *compatíveis* com diploma/Censo; XML, acervo RDC-Arq e extrato Censo **fora da v1** (pesquisa: “design early / defer the consumer” → brief estacionou).
- Estrada pós-v1 no addendum do PRD = a da pesquisa: Censo → evento de ocupação/no-show → OneRoster/Edu-API → CAFe/BAITA → agente com confirmação humana.
- Equidade ConstrSW (1 principal + 1 secundária 1–N) e frontend cruzado são do brief/disciplina, não da pesquisa — fora deste reconcile.

## Gaps (qualitative or FR-dropped; not locked-decision fights)

Ideias da pesquisa que o brief ainda carregava em tom/JTBD, mas a malha de FRs **não realiza** — ou suaviza a ponto de perder o gancho qualitativo.

1. **Join *quem existe* × *o que se ensina* × *sala às 10:00*.** A síntese da pesquisa é o gargalo institucional: não falta tabela de estudante; falta juntar identidade, oferta curricular e o que ocupa a sala num relógio. O brief/PRD reformularam para *quem já existe* × *oferta do período com sala padrão nas faixas* × *o que acontece nesta aula* — correto para o recorte ConstrSW, mas os FRs cobrem os pedaços (identidade, Class, Lesson, sala efetiva, conflito) e **nenhum FR é essa consulta**. A JTBD do funcionário (“consultar ocupação por data + faixa”) está em §2.1 e no sucesso do brief; UJ-1 e FR-20/21 são só cadastro de Room. NFR-LGPD-2 fala em “agregados de ocupação” sem um FR que os produza.

2. **Language lock-in como produto.** A pesquisa trata `turma ≠ aula ≠ disciplina` como *produto* (ensino + API), não só naming. O PRD reduz a FR-25 (nomes no OpenAPI) e FR-28 (“contratos e telas usam a língua travada”). O tom — as quatro personas *falarem* os mesmos termos; falha = `pessoa` / `evento` / `slot` genérico — ficou na Vision e no contra-KPI SM-C3, não como propriedade demonstrável de lock-in (além de strings no contrato).

3. **Ocupação agendada vs ocupação real.** A pesquisa (McGill ~50% de salas pequenas ociosas; ghost booking; sensores vs timetable) distingue *scheduled occupancy* de *actual occupancy*. O PRD modela só a agendada (sala efetiva, conflito em `data+faixa`) e adia sensores (NFR-Q-2) — alinhado ao brief. O que caiu: **nomear a distinção**; o tom utilization-truth vs anedota (EDUCAUSE data literacy); o gancho da pesquisa de **modelar o evento de ocupação/no-show agora** (aditivo, sem IoT) vs addendum “depois da v1”. Não é pedido para puxar IoT; é o conceito que os FRs não carregam.

4. **BFF como read-model composto do join (além do Student).** A pesquisa pede um read-model student/professor: próxima aula, sala, conteúdo, flag de avaliação, minhas reservas — backends não donos do join. FR-19 cobre o Student (lista das Lesson das próprias Class). Professor tem FRs de *escrita* (FR-17, FR-23, FR-24); funcionário não tem FR de leitura de ocupação (ver gap 1). O join qualitativo ficou fatiado em escritas por contexto, não numa composição de leitura que realize “quem × o quê × sala neste relógio”.

## Conflicts (only if PRD contradicts research AND brief did not already override)

Nenhum.

Superfícies que *parecem* conflito já foram override do brief (não reabrir):

- Pesquisa: professor e employee “overlap in life, not in context” → brief/PRD: **Professor is-a Employee**.
- Pesquisa: desenhar extrato Censo / occupancy event cedo → brief/PRD: estacionados pós-v1; IDs só *compatíveis*.
- Pesquisa: `rooms`/`resources` como “spaces” no diagrama → brief/PRD: no grupo **alocação** (consomem IDs; não copiam pessoa).
- Pesquisa: um dono de identidade por *tipo de pessoa* com professors como SoR próprio → brief: identidade cadastral em `employees`; `professors` é papel.

O PRD não contradiz a pesquisa noutro ponto que o brief não tenha já decidido.
