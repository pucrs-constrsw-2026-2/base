## Document Summary
- **Purpose:** Overflow do PRD Malha — cânone que não cabe no corpo: mecanismo, grade, atributos v1, telas (e o que não relitigar).
- **Audience:** Arquitetura / grupos ConstrSW.
- **Reader type:** humans
- **Structure model:** Reference/Database (consulta aleatória a tabelas-cânone), com fecho Pyramid (rejeitado / lacunas / estacionado no fim).
- **Current length:** 1212 words across 8 H2 sections (1 H1)

Este documento exists to help arquitetura e os nove grupos consultar o cânone de overflow (mecanismo travado, grade, pares/atributos v1, inventário de telas) sem reabrir o PRD.

Nenhum estilo (`style_guide`) foi fornecido; valem os princípios humanos + o modelo Reference. **CONTENT IS SACROSANCT:** recomendações só de organização, não de ideias. **Length target:** sem padding; manter tabelas.

### Step notes (estrutura atual)

| Seção | ~palavras | Serve o propósito? |
|---|---|---|
| Abertura (H1 + 2 frases) | ~40 | Sim — mapa de overflow; auditoria apontada para `.memlog.md` |
| Mecanismo e stack | ~210 | Sim — cânone travado |
| Grade de faixas | ~90 | Sim — tabela-cânone |
| Pares 1–N e atributos v1 | ~350 | Sim — duas tabelas de contrato |
| O que o frontend cruzado força | ~180 | Sim — mas o payload “telas” está no título errado |
| Carga de Student | ~30 | Sim (formato JSON) — H2 órfão |
| Alternativas rejeitadas | ~220 | Sim — lookup de “não relitigar” |
| Estrada depois da v1 | ~55 | Overflow estacionado (fora do recorte v1 nomeado; ainda evita relitigação) |
| Lacunas conhecidas | ~70 | Sim — abertos de v1; hoje *depois* do estacionado |

**Modelo Reference:** acesso aleatório funciona nas tabelas; falha no H2 de telas (não diz “telas”), no H2 de 2 frases (Carga) e na lista de abertura (promete “transporte”, não promete telas). **MECE:** a regra BFF vs frontend cruzado aparece duas vezes quase idêntica. **Pyramid:** o útil para implementar v1 (lacunas) está depois do estacionado pós-v1.

**Comprehension aids (humans):** as cinco tabelas ancoram o cânone — preservar. A frase “Efeito: quem implementa a UI de Class…” é o único gancho concreto da regra cruzada — preservar uma vez. Sem diagramas; não inventar (seria padding).

---

## Recommendations

### 1. MERGE - Carga de Student (formato) → Pares 1–N / Student
**Rationale:** Dois períodos viram H2 com o mesmo peso de Grade e Atributos; o conteúdo é a carga da identidade Student já listada na tabela de principais (`origem seed/import`) e deve viver ao lado dela.
**Impact:** ~0 words (relocates ~30); −1 H2
**Comprehension note:** Não corta auxílio; o `[ASSUMPTION]` JSON/idempotência/FR-7 permanece, só muda de endereço.

### 2. MERGE - Regra BFF próprio vs frontend alheio (Mecanismo + seção de telas)
**Rationale:** A mesma distinção está nos dois últimos bullets de Mecanismo e nas duas primeiras linhas da seção de telas — redundância verdadeira, não reforço.
**Impact:** ~−40 words (dois bullets → um ponteiro; manter a frase de **Efeito** + tabela)
**Comprehension note:** Preservar a frase do cliente de `classes` / sala padrão vs efetiva — é o gancho humano. Cortar só o eco.

### 3. MOVE - Inventário de telas: título e tabela na frente
**Rationale:** O propósito nomeia “telas”; o H2 atual esconde isso atrás de “o que o frontend cruzado força”, e quatro frases de setup atrasam a tabela que os grupos vão consultar.
**Impact:** ~0 words (retítulo + tabela imediatamente após um único período da regra BFF/UI)
**Comprehension note:** Título sugerido (ideia intacta): **Telas v1 (frontend cruzado)**. Um período de regra antes da tabela basta para o modelo mental.

### 4. MOVE - Lacunas conhecidas antes de Estrada depois da v1
**Rationale:** Quem implementa v1 precisa dos buracos (OQ-1, feriados, SIS, ocupação agendada) antes da lista estacionada; o estacionado é “depois”, não o cânone.
**Impact:** ~0 words
**Comprehension note:** Nenhum. Pyramid: crítico de v1 acima de nice-to-know pós-v1.

### 5. CONDENSE - Frase de abertura vs H2 reais
**Rationale:** A abertura lista “mecanismo, transporte, grade, atributos v1, alternativas” e omite telas/carga/lacunas/estacionado; “transporte” não tem seção (está dentro de Mecanismo).
**Impact:** ~0 words (reescrever o inventário para espelhar os H2)
**Comprehension note:** Expectation-setting humano — o mapa tem de bater com os destinos. Não acrescentar TOC.

### 6. PRESERVE - As cinco tabelas (grade, pares 1–N, principais, telas, alternativas)
**Rationale:** São a densidade do overflow e o alvo de consulta aleatória; o length target pede para mantê-las.
**Impact:** ~0 words (não achatar em prosa)
**Comprehension note:** Cortar ou linearizar tabelas prejudicaria scan e retenção. Linhas de Alternativas que ecoam o cânone (E1/E2, adaptador JS, telas próprias) são lookup de “por que não”, não padding.

### 7. PRESERVE - Estrada depois da v1 (estacionado)
**Rationale:** Fora do recorte nomeado (v1), mas impede que Censo/CAFe/agente voltem para o corpo do PRD; o disclaimer “não compromisso” já delimita.
**Impact:** ~0 words if kept; ~−55 if cut (não recomendado)
**Comprehension note:** Manter *depois* das lacunas de v1 (rec. 4).

### 8. QUESTION - Subdividir Mecanismo (identidade / repositório / fora da v1)
**Rationale:** Dez bullets misturam repo, Keycloak/OIDC, BFF e hardware; `###` ajudaria MECE, mas o length target veta padding — só vale se a arquitetura reclamar de scan.
**Impact:** ~+15 words se aceito; 0 se recusado
**Comprehension note:** Default: **não** subdividir. A lista já é curta.

---

## Summary
- **Total recommendations:** 8 (5 ações, 2 preserve, 1 question)
- **Estimated reduction:** ~40 words (~3% of original) — o ganho é scan/MECE, não corte de volume
- **Meets length target:** Yes (sem padding novo; tabelas intactas; único corte é eco BFF/frontend)
- **Comprehension trade-offs:** Nenhum corte de tabela, exemplo ou frase-gancho. Rec. 8 rejeitada por padrão para não inflar headings.

Ordem alvo dos H2, se as ações 1–5 forem aceitas:

1. Mecanismo e stack
2. Grade de faixas
3. Pares 1–N e atributos v1 *(inclui carga JSON de Student)*
4. Telas v1 (frontend cruzado)
5. Alternativas rejeitadas
6. Lacunas conhecidas
7. Estrada depois da v1
