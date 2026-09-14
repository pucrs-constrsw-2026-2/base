---
reviewer: reconcile-ux
lens: quiet UX constraints vs architecture spine
spine: ARCHITECTURE-SPINE.md
inputs:
  - DESIGN.md (ux-constrsw-2026-2-2026-08-24)
  - EXPERIENCE.md (ux-constrsw-2026-2-2026-08-24)
date: 2026-08-24
status: complete
spine_unchanged: true
out_of_scope:
  - tokens visuais (paleta, tipo, raios, occupancy-*)
  - anatomia de tela / mockups / wireframes
  - relitigar inventário UX-S1–UX-S9
---

# Reconcile UX — Closed CRAS spine

Comparação de `DESIGN.md` + `EXPERIENCE.md` contra `ARCHITECTURE-SPINE.md`. Spine **não** alterada. Tokens visuais e telas **não** relitigados. Foco: restrições silenciosas de experiência (auth UX, refusal banners, superfícies LGPD, shell, zero seletor de microsserviço, login via gateway) e falhas/contradições com **AD-4 / AD-6 / AD-8**.

## Veredito

**Ressalvas — não handoff-clean.** Os invariantes load-bearing de UX aterrissaram (browser só no BFF, login sem adapter IdP, uma SPA, zero seletor de serviço, roster fora de Reservation/S7, `code` FR → `refusal-banner`). O que a UX trata como chrome **compartilhada** e como **estados de sessão/LGPD** não virou Rule em AD-4/AD-6/AD-8. O parágrafo do paradigma (“SPA por papel”) contradiz EXPERIENCE e a própria AD-8.

Disposição sugerida (para o autor da spine, não feita aqui): **autofix** nos findings F1–F3; **discuss** F4–F5 se o recorte “altitude initiative” quiser deixar estado de sessão no EXPERIENCE.

## O que aterrissou (não relitigar)

| Restrição UX | Onde na spine | Nota |
|---|---|---|
| Uma UI web com acesso por papel; não quatro produtos | AD-8 Prevents `quatro SPAs`; FR-28 no mapa | Rule de AD-8 está alinhada; paradigma **não** (F1) |
| Zero seletor de microsserviço; papel ≠ serviço; journey-card = verbo | AD-8 Prevents `seletor de microsserviço`; paradigma “não um seletor de serviço”; UJ-1 no EXPERIENCE | Aterrissou |
| Login sem adapter JS / sem Keycloak no browser | AD-4 Prevents `keycloak-js`, URL do Keycloak, `localStorage`; sequência UX-LOGIN → `POST /v1/auth/login` | Espírito aterrissou; alvo nomeado é o BFF (ver F5) |
| `login-panel` recusa credencial no próprio banner (não hosted IdP) | AD-4 `[ASSUMPTION]` ROPC/Direct Access Grant | Justifica o grant; não amarra os outros estados de auth (F3) |
| UI não faz join de nove OpenAPIs; jornadas via BFF | AD-6 Prevents join + CORS de SPA; AD-8 dados só TanStack Query → BFF | Aterrissou |
| NFR-LGPD-3 / roster **ausente** em UX-S7; plano+roster = UX-S8 | AD-6 Rule: módulo `reservations` do BFF **não** inclui `Class.studentIds`; mapa FR-31 → S8 (não S7) | Aterrissou |
| Ocupação FR-29 não nominativa de Student | AD-5 Rule (não AD-6): “nunca nominativa de Student” | Aterrissou no AD certo |
| Sem tela de alta de Student | AD-8 Prevents; convenção Student | Aterrissou |
| Recusa de invariante → `{components.refusal-banner}`; UI não decide colisão | **AD-2** (não AD-8): `code` = FR → `refusal-banner`; diagrama de sequência | Aterrissou no AD de contrato; AD-8 não cita o componente (F2 residual) |
| Accent só sala efetiva divergente; v1 light-only; IBM Plex | AD-8 `tokens.css` reproduz DESIGN.md | Fora de escopo deste reconcile (token) |
| OQ-1 aberto; UX-S3 e UX-S7 são uma superfície cada | Deferred OQ-1; seed `ux-s1`…`ux-s9` | Aterrissou |

## Findings

### F1 — Crítico. Paradigma “SPA por papel” contradiz EXPERIENCE e AD-8

- **UX (EXPERIENCE Foundation + FR-28):** uma UI web com acesso por papel — **não quatro produtos**.
- **Spine Design Paradigm:** “A UI é uma **SPA por papel**, não quatro produtos e não um seletor de serviço.”
- **AD-8 Rule:** “**um** app Vite SPA”; Prevents: **quatro SPAs**.

“SPA por papel” em pt-BR lê-se como um app por papel (`funcionario-spa`, `coordenador-spa`, …). EXPERIENCE e AD-8 exigem um único app com nav por papel. Nove grupos no `frontend` compartilhado podem fatiar o repo em quatro Vite apps e ainda achar que obedecem o paradigma.

**AD:** contradiz AD-8; enfraquece o bind UX-LOGIN / UX-HOME / UX-S1–S9 (um shell, não quatro).

**Disposição:** autofix no parágrafo do paradigma (espelhar AD-8: uma SPA, acesso por papel). Não é relitigar tela.

### F2 — Alto. Shell UX = chrome em todas as autenticadas; AD-8 reduz `src/shell` a duas páginas

- **UX (DESIGN app-shell / role-nav; EXPERIENCE Component Patterns):** `{components.app-shell}` em **todas** as autenticadas: barra (produto **Closed CRAS** + papel + identidade) + `{components.role-nav}` só com destinos do papel. Itens de outro papel **não existem no DOM visível** (não ficam “desabilitados”). Logout → UX-LOGIN. Sem menu de serviços. Sem atalho que troque de papel.
- **AD-8 Rule / Structural Seed:** `src/shell` = **UX-LOGIN + UX-HOME**. Superfícies exclusivas em `src/surfaces/ux-s1`…`ux-s9`. Prevents seletor de microsserviço e quatro SPAs — **não** amarra que S1–S9 **consomem** o chrome compartilhado, nem a regra de DOM da `role-nav`.

Nove donos de UX-S* (OQ-1) podem cada um desenhar header/nav próprios, listar as onze superfícies “disabled”, ou expor troca de papel. Isso reintroduz seletor-de-serviço por outro nome e quebra a partição “LOGIN/HOME compartilhadas, S* exclusivas”.

**AD-8** é o AD que deveria fechar isso. AD-6 não fala de chrome.

**Disposição:** autofix na Rule de AD-8: shell = LOGIN + HOME **+** `app-shell`/`role-nav` obrigatórios nas autenticadas; nav = só papel corrente; superfícies não reimplementam chrome.

### F3 — Alto. Auth UX (sessão, recusa, IdP/BFF down, 403) não entrou em AD-4 nem AD-8

EXPERIENCE **State Patterns** (Foundation + login-panel) que a spine não amarra:

| Estado UX | Tratamento EXPERIENCE | Spine |
|---|---|---|
| Credencial recusada | Banner **no** `{components.login-panel}`; **não enumera** se o id/conta existe | AD-4 assume ROPC para o banner no painel; **não** bind de não-enumeração |
| Gateway/IdP indisponível | Banner: “Identidade indisponível. A UI não fala com o IdP direto.” Sem fallback de adaptador | AD-4 Prevents adapter; **não** o estado de superfície |
| BFF indisponível | Banner de página; jornada ilegível; **sem join local** de APIs | AD-6 Prevents join/CORS (o fallback ruim); **não** o banner |
| Sessão ausente / expirada | Redirect UX-LOGIN; **volta à URL pedida** se o papel puder | Ausente |
| Logout | Volta a UX-LOGIN | Ausente |
| URL de outro papel | Redirect UX-HOME do papel atual. **Sem “403” com nome de serviço** | Ausente (AD-8 só veta seletor) |
| Offline | Sem modo offline; escritas **não** entram em fila local | Ausente |

AD-4 cobre o **mecanismo** (BFF `POST /v1/auth/login`, cookie httpOnly, UI sem URL Keycloak). AD-8 cobre pastas e tokens. Os estados acima são o que impede dois grupos no shell compartilhado de: toast “user not found”, redirect ao Keycloak quando o BFF cai, 403 `employees`, PWA com fila local, ou login sem return-to.

**AD-4** deveria bindar os estados de UX-LOGIN + cookie/sessão. **AD-8** deveria bindar redirect de papel e chrome pós-login (nome + papel). Hoje AD-4 exige `GET /v1/identidade/me` **em cada um dos nove** e não diz qual read-model alimenta a barra do shell — cada superfície pode escolher um `/me` de contexto e furar AD-6 (join).

**Disposição:** autofix — AD-4: login-panel = só BFF; falha de credencial sem enumeração; IdP down sem adapter; sessão/logout. AD-8 ou AD-6: identidade de chrome via **um** read-model BFF (não nove `/me` no browser). Redirect de papel ≠ 403 de serviço.

### F4 — Alto. LGPD de superfície: mapa cita NFR-LGPD-1–4; AD-6 só fecha 2 (parcial) e 3

- **UX (EXPERIENCE Superfícies e LGPD + State Patterns):**
  - **NFR-LGPD-2:** Lesson + Student nomeado = localização. UX-S9 ao próprio Student. S4 ocupação **não** nominativa. Professor lê roster **só** em UX-S8 e **só** da Class **atribuída**; Class alheia **não aparece**.
  - **NFR-LGPD-3:** UX-S7 não lista roster. Aterrissou em AD-6.
  - **NFR-LGPD-4:** desativar/esconder na UI **não apaga** `id` canônico “para limpar a tela”.
  - Sem CPF na ficha de Employee (v1).
- **AD-6 Binds:** NFR-LGPD-**2** e **3**, UX-S7/S8/S9. Rule: CORS; módulo reservations sem `studentIds`; roster só módulo `classes` → S8. Não diz: S8 filtrado por atribuição; S9 só o próprio Student; DELETE de id para limpar UI.
- **Capability map:** “NFR-LGPD-1–4 \| BFF recortes + sem CPF v1 \| AD-6, AD-2” — **overclaim**. AD-2 não menciona CPF nem retenção de id. AD-6 não menciona LGPD-1, LGPD-4, CPF, nem recorte “Class atribuída”.
- **NFR-LGPD-1** (exceção acadêmica estreita, não isenção) não precisa de endpoint; precisa não ser “Deferred por silêncio” se o mapa diz que vive em AD-6.

Dois grupos em S8 podem listar roster de **qualquer** turma (vazamento LGPD-2). Um grupo em S1 pode `DELETE` Employee para “sumir da tabela” (LGPD-4). Um grupo pode campo CPF no formulário (NFR-ID / EXPERIENCE).

**Disposição:** autofix na Rule de AD-6 (e/ou AuthZ conventions): roster S8 = Class atribuída; FR-19/S9 = titular; desativar ≠ apagar id; sem CPF v1. LGPD-1 pode ir para convenção de uma linha, não sumir no mapa.

### F5 — Médio. “Login via gateway” aterrissou remapeado; UX ainda distingue gateway ≠ BFF

- **UX Foundation:** duas frases distintas — (1) UI autentica via **gateway REST de identidade**, sem adapter IdP; (2) lê/escreve **jornadas via BFF**.
- **UX-LOGIN / login-panel:** “Submit chama o **gateway REST de identidade**.” Falha no banner do painel.
- **PRD FR-27 / addendum:** cada um dos nove implementa gateway REST **no próprio serviço**; a UI fala com o gateway, não com o IdP.
- **AD-4:** browser autentica **só no BFF** `POST /v1/auth/login`; Prevents **nove endpoints de login**; os nove são **resource-server** + `GET /v1/identidade/me`. Título do AD = “Gateway REST de identidade”.

Espírito (não `keycloak-js`, um login-panel, não nove logins de UI) **alinha** com UX-LOGIN compartilhada. O **vocabulário** não: EXPERIENCE não diz que o gateway que o painel chama **é** o BFF. Grupo no shell compartilhado pode POSTar em `oauth`, num contexto `:810x`, ou achar que FR-27 exige login em nove resource-servers — exatamente o que AD-4 Prevents.

Não é drop do anti-adapter. É contradição **de nomeação** entre UX/PRD (“gateway” por grupo) e AD-4 (login browser = BFF). AD-1 já proíbe UI→contexto; falta AD-4 dizer em Rule: **o único gateway que UX-LOGIN chama é `POST /v1/auth/login` no BFF**; o “gateway” dos nove = JWKS/papéis, não login.

**Disposição:** discuss/autofix de uma frase em AD-4. Não reabrir ROPC vs code flow (já justificado pelo banner no painel).

## Findings menores (cauda)

- **M1 (médio/baixo).** AD-8 não internaliza `{components.refusal-banner}` (`role="alert"`, substitui sucesso, formulário retido, sem toast “Oops”). O mapeamento `code`→banner está em **AD-2**. Dono de superfície que ler só AD-8 + tokens pode usar toast. Autofix: uma linha em AD-8 apontando AD-2, sem copiar microcopy.
- **M2 (baixo).** Distinção S4 ocupado (célula muted) vs S6/S7 recusa (banner destructive) é comportamento EXPERIENCE; cor é token (fora de escopo). AD-5+AD-2 já separam read-model vs 409. Não precisa relitigar.
- **M3 (baixo).** Desktop-first; cadastro/grade não são recorte de telefone; S9 cabe em viewport estreita. Form-factor silencioso; dois grupos podem PWA-mobile S1. Opcional em AD-8; não bloquear.
- **M4 (info).** Chrome “Closed CRAS” (DESIGN Don’t: Malha, SIG*, marca PUCRS) não está na Rule de AD-8. Quase marca/token; citar só se F2 for reescrito.
- **M5 (info).** Banidos de interação (DnD da grade, infinite scroll, time picker livre, stack de modal > 1) — AD-2 já mata horário solto; o resto é EXPERIENCE. Não contradiz ADs.

## Contradições explícitas AD-4 / AD-6 / AD-8

| AD | vs UX | Tipo |
|---|---|---|
| Paradigma “SPA por papel” vs AD-8 “um app” vs EXPERIENCE “uma UI” | F1 | Contradição interna + vs UX |
| AD-8 `src/shell` = LOGIN+HOME vs app-shell em todas autenticadas | F2 | Miss que vira divergência |
| AD-4 mecanismo login vs estados UX-LOGIN / sessão / 403 | F3 | Miss |
| AD-4 nove `/identidade/me` no browser vs AD-6 “só BFF” + chrome única | F3 | Tensão AD-4 × AD-6 |
| AD-6 Rule vs mapa NFR-LGPD-1–4 + EXPERIENCE S8 atribuída / LGPD-4 / CPF | F4 | Overclaim + miss |
| AD-4 login=BFF vs EXPERIENCE “submit → gateway REST” (≠ BFF) | F5 | Contradição de vocabulário; espírito ok |

Nada em AD-4/AD-6/AD-8 **reabre** seletor de microsserviço, quatro produtos (exceto F1), adapter JS, join no browser, ou roster em Reservation. Esses anti-padrões estão fechados.

## Fora de escopo (proposital)

- Paleta, tipografia, raios, occupancy-free/taken/exception, chip accent, 56px de barra, 400px do login-panel, IBM Plex sizes.
- Inventário e jornadas UX-S1–S9, UJ-1–UJ-4 passo a passo, OQ-1.
- Escolha React/Vite (UX não nomeia lib; arquitetura pode).
- ROPC vs Authorization Code além do já amarrado ao banner do login-panel.
