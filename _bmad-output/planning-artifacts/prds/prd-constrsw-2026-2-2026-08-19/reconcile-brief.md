# Reconcile — brief Malha

Fonte: `_bmad-output/planning-artifacts/briefs/brief-constrsw-2026-2-2026-08-17/brief.md` (atualizado 2026-08-19). Memlog do brief só para decisões que o texto aponta; addendum do brief **não** é input.

Contra: `prd.md` + `addendum.md` deste PRD. Extrato de lacunas — o PRD não foi reescrito.

## Covered (inherited, no gap)

- Nome **Malha**, recorte ConstrSW (9 grupos, este briefing veio de `employees` mas o produto é o mesh inteiro), não pitch nem substituto de SIS/SIGAA/TOTVS/Ellucian/Lyceum.
- Problema âncora: juntar quem já existe + oferta do período com sala padrão nas faixas + o que acontece nesta aula; língua não compartilhada (`disciplina` ≠ `turma` ≠ `aula` ≠ `reserva` ≠ `faixa`).
- Split **registros** (`students`, `courses`, `professors`, `employees`) vs **alocação** (`classes`, `lessons`, `rooms`, `resources`, `reservations`); alocação consome IDs, não copia cadastro; falha de produto = nove OpenAPIs com `pessoa` copiada.
- Formato travado: APIs contract-first + BFF + UI web; não app nativo, não hardware.
- Quatro personas, papel ≠ serviço; Student só leitura; identidade de Student recebida (seed/import), sem tela de alta; funcionário não dá alta; Professor is-a Employee (`employeeId` 1–1, não copia nome/e-mail); Student não é Employee.
- Sala padrão na abertura da turma; Reservation de Room só na exceção/ad hoc; recurso continua reserva; sala efetiva derivada; remoto sem Room; nunca horário solto; grade A–E, F–N, P (sem E1, E2, O); aulas seg–sáb.
- Equidade: 9 pares principal 1–N secundária; IDs alheios são atributo, não terceira classe; cada grupo incorpora o domínio no BFF e no OAuth compartilhados; sem grupo dono exclusivo de BFF/OAuth; `frontend` fora do par 1–N.
- Coordenador: plano estruturado datado *antes* do período; abre uma ou mais Class com vagas, sala padrão, esqueleto (seg–sáb, faixa); gera Lesson; não confirma reserva aula a aula; não e-MEC.
- Invariantes: sala padrão cabe nas vagas + AccessibilityFeature; duas Lesson não compartilham a mesma sala efetiva na mesma `data+faixa`; reserva não confirma com sobreposição de sala, recurso ou professor.
- Sucesso = demo de laboratório (sem rubrica numérica no repo); operador não escolhe microsserviço; sem Professor órfão; 18 reservas da sala padrão é anti-sucesso.
- Fora da v1 (diploma XML, otimizador/IA, IoT/twin, SIS ao vivo, tesouraria/ERP, LMS como produto, CAFe/BAITA de produção, folha/SIGRH/ponto, Lattes, etc.) e estrada estacionada (Censo → evento de ocupação → Edu-API → federação) — herdados no PRD §7–8.2 e addendum.
- Assumption aberta no brief (“dono da tela ainda não padronizado como o BFF”) foi **fechada** no PRD (FR-28 frontend cruzado; addendum, decisão 2026-08-19). Evolução, não lacuna do input.

## Gaps (input idea missing or diluted in PRD/addendum)

### G1 — Funcionário consulta ocupação por data + faixa
- **No input:** persona funcionário *opera* “consulta ocupação por **data + faixa**”; sucesso narrativo da v1 (item 1) inclui “lê ocupação por faixa” na UI, junto com o cadastro-mestre.
- **No PRD/addendum:** aparece só no JTBD (§2.1) e, de relance, no glossário (ocupação usa sala efetiva) e em NFR-LGPD-2 (agregado não nominativo). **Não há FR, nem passo em UJ-1, nem métrica SM.** O addendum estaciona “evento de ocupação / no-show” *depois* da v1 (NFR-Q-2), o que permite ler a consulta de ocupação — capacidade v1 do brief — como fora de escopo.
- **Qualitativo dropped:** o funcionário como *leitor da malha no chão* (a grade ocupada na faixa), não só como steward de cadastro. O FR split cadastrou Employee/Room/Resource e esqueceu a leitura que fecha o turno da persona.
- **Severidade:** should-fix (antes do UX travar UJ-1; senão a jornada do funcionário vira só CRUD).

### G2 — Professor altera/cancela (reserva e, no texto, o ajuste da aula)
- **No input:** professor *opera* “altera/cancela” no mesmo bloco em que reserva recurso/sala-exceção e ajusta a aula.
- **No PRD:** FR-17 cobre ajuste de LessonTopic / `flagAvaliacao` / modalidade (criar/editar tópicos, não cancelar Lesson). FR-23/FR-24 só **criam** Reservation/ReservationLine. Silêncio sobre alterar, cancelar ou substituir reserva; silêncio sobre cancelar aula.
- **Qualitativo dropped:** a exceção é um ato reversível (a sala padrão volta a valer; o kit é liberado). O tom do brief é “não inventar 18 holds” *e* poder desfazer o hold pontual — o FR ficou só no verbo criar.
- **Severidade:** should-fix (cancelar/alterar Reservation). Cancelar Lesson em si: defer (o brief é ambíguo entre aula e reserva).

### G3 — Professor lê plano **e roster**
- **No input:** professor *opera* “lê plano e roster”; sucesso da persona inclui o estudante ver a sala efetiva, o que pressupõe o docente ainda ter o quadro da turma.
- **No PRD:** FR-11 dá leitura do plano a Professor e Student. Roster (`Class.studentIds`) não tem FR de leitura para o professor; NFR-LGPD-3 só proíbe vazar roster *na tela de reserva*. UJ-3 não lê plano nem lista de Student.
- **Qualitativo dropped:** o professor como *referência ao plano* (não “nota de reserva”) e como quem vê *quem já veio no seed* — voz de aula, não só de booking.
- **Severidade:** should-fix (leitura de roster da Class atribuída, via BFF, distinta da tela de reserva).

### G4 — Dor do estudante = “local mentiroso”, não “reconciliar quatro sistemas”
- **No input:** o que quebra para o estudante é o *onde* mentir se não for a **sala efetiva**; o atalho errado produz “o estudante lê um local mentiroso”. Sucesso: onde = efetiva (exceção ou padrão); remoto se remoto; nunca horário solto.
- **No PRD:** o mecanismo está certo (FR-18, FR-19, SM-2). UJ-4 reframingou o climax para “sem reconciliar quatro sistemas” — fragmentação de suíte, não o engano de lugar. Nenhum FR/consequência nomeia o anti-exemplo (UI mostrar `Class.roomId` quando há linha SALA).
- **Qualitativo dropped:** tom de *decepção espacial* (chegar na sala errada / horário solto / remoto mal sinalizado). A estrutura FR herdou a regra e perdeu o feel que deveria guiar copy e estado vazio da UI do Student.
- **Severidade:** should-fix (consequência testável em FR-19 / UJ-4: nunca exibir sala padrão quando a efetiva for outra; never “local mentiroso”). Não é phase-blocker de modelo.

### G5 — Posicionamento honesto e anti-sucesso de mercado (voz que o FR não carrega)
- **No input:** “Não há moat.” Diferencial = execução e língua. Custo da disciplina **não é TAM**: é demo incoerente, cadastro de aluno inventado, horário fora da grade, plano que não vira dado. “Suítes colapsam os contextos; planilhas os separam.” Não é sucesso: share de mercado, NPS de campus, “IA na reserva”. Se der certo: **não vira ERP**; vira **evidência** de malha contract-first *ao lado* do sistema de registro.
- **No PRD:** Vision/NFR-Q repetem mesh ao lado do SIS e demo de laboratório. Alternativas rejeitadas cobrem nove CRUDs e “ser o SIGAA”. Counter-metrics (SM-C1–C3) são endpoints/RH/contagem de reservas — **não** NPS, share, “IA na reserva”. A frase “não há moat” e o par suíte↔planilha sumiram. “Não vira ERP” virou bullet de tesouraria/ERP (§7), não o desfecho “evidência de disciplina”.
- **Qualitativo dropped:** voz anti-pitch (sem moat, sem TAM, sem NPS); o feel de *prova de malha*, não de produto a escalar. UX/épicos que lerem só FRs herdam um sistema correto e uma narrativa de campus-product.
- **Severidade:** defer (não bloqueia implementação; deveria pingar Vision/Success counter-metrics se houver passagem de copy).

### G6 — Coordenador “vê turmas e flags”
- **No input:** coordenador *opera* “vê turmas e flags” depois de abrir a oferta (além de publicar plano e gerar aulas).
- **No PRD:** UJ-2 termina na geração das Lesson; FR-17 dá `flagAvaliacao` ao professor; FR-19 dá a flag ao Student. Nenhuma leitura do coordenador sobre turmas já abertas e flags de avaliação.
- **Qualitativo dropped:** coordenação como *acompanhamento da oferta viva* (o plano virou dado *e* a turma ainda é visível), não só como ato de abertura.
- **Severidade:** defer (está em “opera na v1”, não no sucesso demonstrável da persona).

## Conflicts (PRD contradicts input)

Nenhum conflito com regra travada do brief.

Nota (não conflito): FR-28 / addendum fecham a `[ASSUMPTION]` do brief sobre dono de tela (frontend cruzado). O brief não havia padronizado o contrário — só deixara em aberto.
