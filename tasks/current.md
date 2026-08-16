# Tarefa Actual

> Ponteiro da sessão. A IA lê-o no início e actualiza-o sempre que uma fase fecha. ≤1 página. Histórico no `git log`.

**Última actualização:** 2026-08-15
**Estado:** software de loja concluído; Track B (cliente-fino) + correcções multi-tenant **em `main`**.
As **cinco lacunas de gestão** levantadas na auditoria de 09/08 estão **fechadas** (ver abaixo) —
incluindo a contabilidade, que era a maior ausência.
**Passo de profissionalização em curso** (rumo a produção): #1 teste de regressão ✅, #2 CI+gate ✅
(falta ligar a proteção de branch no GitHub), #6 sem dados/segredos de demo em prod ✅, #7 numeração
multi-empresa dos payslips ✅. **Falta:** ligar a proteção de branch; 1.º `docker compose up` real numa
VPS + smoke; backup restaurável verificado; validação em loja + hardware. A fonte de verdade operacional
é [tasks/retail_store_readiness.md](retail_store_readiness.md).
**Por integrar:** o ramo `feat/guia-remessa-navegacao-suite-local` está **23 commits à frente da
`main`** e por enviar para o remoto.

### Progresso — 2026-08-15 (navegação visual mais limpa)

- A barra superior mantém os módulos operacionais visíveis e agrupa Área Fiscal, Contabilidade,
  Aprovações e Configurações num menu textual “Mais”, reduzindo a densidade sem remover acessos.
- Os itens da navegação passaram a aceitar Enter/Espaço, expor nome acessível e mostrar foco visual.
- Verificação: `mvn -q -DskipTests compile` verde; teste focado de navegação adicionado.

### Progresso — 2026-08-15 (hierarquia visual de RH e Configurações)

- Spec/harness: `docs/HR_CONFIG_UI_HIERARCHY_SPEC.md` e
  `docs/HR_CONFIG_UI_HIERARCHY_HARNESS.md` (HCUI-01..24).
- Novo `ActionMenuButton` canónico limita menus a cinco entradas, mantém altura/ícones/acessibilidade
  e agrupa apenas acções secundárias.
- RH: Colaboradores ganhou “Mais acções”; Recibos ganhou “Documentos”. Configuração: Utilizadores
  ganhou “Mais acções”; as modalidades de backup passaram para “Criar backup”.
- Acções críticas continuam explícitas: Aprovar, Rejeitar, Eliminar, Marcar Pago e Processar Mês.
- Verificação: `mvn clean compile` e harness focado verdes; suite completa verde com **487 testes,
  0 falhas, 0 erros e 0 ignorados**. HCUI-20..24 requerem validação manual em Windows real.

### Progresso — 2026-08-15 (fecho visual de Stock e Comercial)

- Spec/harness: `docs/STOCK_COMERCIAL_UI_FINISH_SPEC.md` e
  `docs/STOCK_COMERCIAL_UI_FINISH_HARNESS.md` (SCUI-01..24).
- Faturas, encomendas, notas e guias agrupam impressão/exportação/actualização; Liquidar, Anular,
  Faturar, Converter, Cancelar, Aprovar e Rejeitar continuam visíveis.
- Stock global, categorias e armazéns ganharam hierarquia de acções; filtros passaram de altura 35
  para `UIHelper.FORM_CONTROL_HEIGHT`.
- `DocumentEditorHost` e `ModernFormDialog` ganharam `Ctrl+S` e `Esc`; atalhos POS preservados.
- Validação real a 1382×736 no tema claro: Comercial, RH e Stock sem cortes/sobreposições.
- Verificação: build limpo, harness focado e suite completa verdes com **492 testes, 0 falhas,
  0 erros e 0 ignorados**. Escalas 100/125/150%, tema escuro e acções com dados reais continuam
  como evidência manual obrigatória; a automação Windows falhou sob DPI elevado.

### Progresso — 2026-08-15 (uniformização final do design)

- Spec/harness: `docs/UI_UNIFORMIZACAO_FINAL_SPEC.md` e
  `docs/UI_UNIFORMIZACAO_FINAL_HARNESS.md` (FU-01..23).
- Perfis técnicos permanecem códigos internos, mas tabelas/selects de RH, Configuração, Aprovações
  e Plataforma apresentam Administrador/Gestor/Funcionário.
- Plataforma (empresas, assinaturas, utilizadores) e Fiscal/IVA adoptaram menus canónicos; acções
  principais e críticas continuam visíveis.
- `createDialogForm` passou a construir `FormField` canónico, preservando inputs e marcando labels
  com `*` como obrigatórias.
- Painéis de negócio ficaram sem cores locais; idioma uniformizado para “Actualizar” e “Registar”.
- Verificação: build limpo e suite completa verdes com **496 testes, 0 falhas, 0 erros e
  0 ignorados**; auditoria final sem `new Color`, “Atualizar”, “Cadastrar” ou `MANAGER/ADMIN` nos
  painéis. Certificação visual FU-20..23 continua manual.

### Progresso — 2026-08-15 (fecho das lacunas de gestão + contabilidade)

- **Suite volta a ser verde de forma determinística.** `LoadingCursorTest` rebentava com
  `HeadlessException` conforme a **ordem das classes**: o surefire arranca `headless=true` e o AWT
  decide a headlessness uma só vez, pelo que quem decidia era a primeira classe a tocar em AWT.
  `java.awt.headless=false` passou a ser declarado no `pom.xml` (a CI já corre com `xvfb-run`).
- **Vencimento e antiguidade** (V35): `Client.paymentTermsDays` + `Invoice.dueDate` **gravada no
  documento**; `assignDueDate` chamada pelas três portas que emitem fatura; `AgingBucket` como fonte
  única dos cortes 30/60/90; `ReceivablesService` + `/api/comercial/receivables/aging`. Contas
  Correntes ganharam Vencimento/Dias em Atraso/Antiguidade e ordenam pelo maior atraso.
  Spec/harness: `VENCIMENTO_ANTIGUIDADE_*` (VA-01..25).
- **Limite de crédito** (V36): três estados (nulo = sem limite, zero = não vende fiado, >0 = tecto);
  aritmética no domínio; trava nas três portas que criam dívida. **Encontrado ao testar:** no POS o
  stock saía *antes* da venda estar autorizada — extraído `deductStockForSale()` e movido para
  depois da trava. Spec/harness: `LIMITE_CREDITO_*` (LC-01..32).
- **Margem com o custo do acto da venda** (V37): `InvoiceLine.unitCost` fotografado na emissão; o
  relatório lia o preço de compra **actual**, pelo que a margem de vendas antigas mudava sozinha
  quando o fornecedor subia o preço. **MC-01 confirmado a falhar contra o código antigo.**
  Spec/harness: `MARGEM_CUSTO_HISTORICO_*` (MC-01..06).
- **Paginação**: `PageQuery`/`PageResponse` + `TablePager`; faturas e histórico do POS paginados. O
  mais grave não era a listagem — o **dashboard** lia todas as faturas da empresa a cada abertura
  para responder sobre *hoje*; as perguntas passaram a ir na consulta. Spec/harness: `PAGINACAO_*`
  (PG-01..11).
- **Contabilidade (PGC-NIRF)** (V38) — decisões do utilizador: plano moçambicano + lançamentos
  automáticos **e** manuais. Plano por empresa (natureza gravada **na conta**, não derivada da
  classe), partida dobrada validada numa só porta, série `LC` por empresa, razão com saldo de
  abertura e balancete que diz **"NÃO FECHA"** quando não fecha. Os lançamentos automáticos entram
  por **eventos** (`SaleRegisteredEvent`/`PaymentReceivedEvent`), pelo que o comercial não passou a
  conhecer a contabilidade. Painel novo com 4 abas. Spec/harness: `CONTABILIDADE_*` (CT-01..46).
  **Por fazer (declarado na spec §7):** salários e compras ainda não lançam automaticamente.
- `NotificationsPanel` migrado para `loadAsync` (mantendo o contador de versão, que protege contra
  respostas fora de ordem da **mesma** empresa — coisa que o `loadAsync` não cobre).
- **Verificação:** `mvn -o clean test` → **482 testes, 0 falhas, 0 erros, 0 ignorados** (eram 401).
- **Por validar ao vivo:** VA-50..57, LC-50..56, MC-50..53, PG-50..56, CT-50..60.

### Progresso — 2026-08-09 (consistência profissional da UI — fundação e adopção inicial)

- Criadas spec e harness: `UI_CONSISTENCIA_PROFISSIONAL_SPEC.md` e
  `UI_CONSISTENCIA_PROFISSIONAL_HARNESS.md` (UI-01..26 automáticos/estáticos; UI-50..62 manuais).
- Componentes canónicos: `FormField`, `MoneyField`, `QuantityField`, `DateField`; erro inline,
  obrigatoriedade, acessibilidade e estado read-only centralizados em `UIHelper`.
- Selects preservam renderers existentes; estados têm tradução central; botão icon-only exige nome
  acessível; tabelas ganharam renderers canónicos de dinheiro, quantidade e estado.
- `loadAsync` passou a propagar contexto, entregar erros no EDT e ignorar resposta de tenant antigo;
  `submitAsync` bloqueia duplo envio; `ModernFormDialog.setOnSaveAsync` impede HTTP no EDT.
- Adopção: Dashboard, Clientes, CRM e Tesouraria carregam assincronamente; cliente usa validação
  inline + submissão assíncrona; CRM/Tesouraria usam renderers tipados. Dashboard ficou com zero
  `new Color` e zero `setPreferredSize` (tokens semânticos no tema).
- Segunda vaga: Aprovações, Promoções e Fiscal migrados para loading/submissão assíncronos.
  Relatórios/PDF/SAF-T saem do EDT; taxas, retenções e promoções usam inputs tipados e validação
  inline. Os três painéis ficaram com zero cores ad-hoc; Promoções também com zero tamanhos fixos.
- Verificação: **389 testes, 0 falhas/erros/ignorados**. Próxima fase: alastrar loading/submissão aos
  restantes painéis e migrar formulários/documentos longos.

### Progresso — 2026-08-09 (recibo parcial deixa de apagar a dívida — 3 furos de dinheiro)

- **Encontrado a auditar o sistema a pedido do utilizador** ("está preparado para gestão?"). Três
  implementações do mesmo conceito — *quanto o cliente ainda deve* — a divergir em silêncio. Mesma
  forma do bug do IVA de 06/08: **a mesma regra em duas portas**.
- **(1) Recibo parcial dava a fatura por paga.** `ComercialService.createReceipt` marcava `PAID`
  por qualquer valor e nunca acumulava `amountPaid`. Pagar 100 de 232 → fatura *Paga*, os 132
  desapareciam das contas correntes e de qualquer cobrança. O **POS já fazia certo**
  (`deriveStatus`/`settleCredit`); só a porta comercial é que não.
- **(2) Dashboard e Contas Correntes discordavam.** `ReportService.unpaidInvoicesTotal` contava só
  `APPROVED` pelo **total**; as Contas Correntes contavam `APPROVED`+`PARTIALLY_PAID` pelo **saldo**.
  Idem em "vendas de hoje": o dashboard contava só `PAID`, o relatório diário tudo o que não fosse
  anulado — dois números para a mesma pergunta.
- **(3) `/api/finance/pay-invoice` sem guarda de papel.** `financeira` era o **único módulo de
  dinheiro sem `PermissionGuard`** — qualquer EMPLOYEE liquidava faturas. E registava sempre o
  total, contando em duplicado o que já tinha sido recebido.
- **Correcção — fonte única no domínio** (padrão do `Product.effectiveTaxRate()`):
  `Invoice.outstandingAmount()` + `Invoice.deriveStatusFromPayments()`, e
  `InvoiceStatus.isRealisedSale()`/`isCollectable()`. O `POSService.deriveStatus` privado foi
  **eliminado** — POS, faturação, tesouraria e relatórios passam pela mesma regra. `createReceipt`
  aceita vários recibos até o saldo zerar e recusa valor ≤ 0 ou acima do saldo; `cancelReceipt`
  devolve só o valor daquele recibo; `payInvoice` exige MANAGER/ADMIN e move só o saldo.
- **Desktop:** coluna **Em Dívida** na tabela de faturas, 2.º recibo permitido sobre
  `PARTIALLY_PAID`, valor sugerido = saldo (não o total) e mensagem que distingue recibo parcial
  de liquidação.
- **Verificação:** 15 testes novos (`ReportServiceTest` e `FinanceServiceTest` novos), **12
  confirmados a falhar contra o código antigo**. `POSServiceTest` (19) verde após a extracção da
  regra. `mvn -o clean test` → **371 testes, 0 falhas/erros/ignorados** (eram 356).
- Spec/harness: [docs/RECEBIMENTOS_SALDO_SPEC.md](../docs/RECEBIMENTOS_SALDO_SPEC.md) +
  [docs/RECEBIMENTOS_SALDO_HARNESS.md](../docs/RECEBIMENTOS_SALDO_HARNESS.md) (RP-01..23 auto,
  RP-50..56 manuais).
- **VALIDADO AO VIVO (RP-50..57):** backend de pé (H2, dados de demo), percurso HTTP completo com
  ADMIN e EMPLOYEE. Fatura de 950,00: recibo de 400 → `PARTIALLY_PAID`; recibo de 700 sobre saldo
  de 550 → **recusado** com a mensagem exacta; recibo de 550 → `PAID` (tesouraria 18.464,50 →
  19.414,50); anular o recibo de 400 → volta a **`PARTIALLY_PAID`** com 550 (não a `APPROVED`) e
  estorna 400; dashboard, relatório diário e contas correntes **de acordo** (`1 / 950.00`,
  400,00 por cobrar); EMPLOYEE recusado no `pay-invoice`; `payInvoice` moveu **400** (o saldo) e
  não 950.
- **Bug adicional encontrado durante a validação:** `POST /api/comercial/receipts` devolvia **500**
  apesar de gravar — `LazyInitializationException` no `toDTO` chamado **fora** da transacção pelo
  controller (`open-in-view=false`). Pré-existente e independente dos fixes de saldo, mas
  **agravado** por eles: antes a fatura ficava logo `PAID` e a repetição era recusada; agora
  continua cobrável, pelo que repetir criaria um 2.º recibo e duplicaria a caixa. Corrigido pela
  regra do próprio projecto (CLAUDE.md #3/#4): `createReceipt` e `getReceiptsByCompany` devolvem
  `ReceiptDTO` convertido **dentro** da transacção. O `GET /receipts` tinha o mesmo defeito latente.
- **Dados existentes:** faturas marcadas `PAID` por recibo parcial antes deste fix ficam como
  estão — query de diagnóstico na §5 da spec.
- **Por validar na UI Swing:** coluna Em Dívida, aviso de recibo parcial e valor sugerido no
  diálogo (o backend está validado; a UI chama exactamente estes endpoints).
- **Lacunas de gestão levantadas na mesma auditoria, por fazer:** sem `dueDate`/aging (não se sabe o
  que está **em atraso**), sem limite de crédito do cliente, margem calculada com o preço de compra
  **actual** (não o do acto da venda), **zero paginação** em todo o sistema (o dashboard carrega
  todas as faturas da empresa), e **sem contabilidade** (nem plano de contas, nem razão, nem
  balancete). Esta última é a maior ausência para um ERP de gestão.

### Progresso — 2026-08-08 (contexto de utilizador/empresa passa a fail-closed)

- **Encontrado a auditar a arquitectura a pedido do utilizador:** o `CurrentUserContext` inventava
  uma sessão quando não havia nenhuma — papel **`ADMIN`** e empresa **`1`**. Como o `PermissionGuard`
  (única guarda de papel do sistema) lê `getRole()`, **todas** as chamadas a `requireAdmin`/
  `requireManagerOrAdmin` eram no-ops em qualquer thread sem contexto, contra o tenant errado.
- **O fallback era load-bearing:** o `DataLoader` semeia tickets/despesas **através dos Services**
  (`crmService.createTicket`, `hrService.submitExpense`) sem contexto — só funcionava porque a empresa
  em falta virava `1`, que **por acaso** é a `ptCompany` (a primeira gravada). Mudar a ordem do seed
  aterrava os dados no tenant errado, sem erro. Agora declara
  `CurrentUserContext.runAsSystem(ptCompany.getId(), …)`.
- **Correcção:** `getRole()` sem contexto → `""` (o guard recusa); `getCurrentCompanyId()` → lança
  em vez de assumir a empresa 1; variantes **nullable** `findCurrentUser`/`findCurrentCompanyId` para
  infra que corre sem tenant (`UIHelper.loadAsync`, superadmin); `runAsSystem(...)` torna a elevação
  de privilégio **explícita e greppável**. O sino de notificações deixou de mostrar alertas da
  empresa 1 ao superadmin.
- **Nota de rigor:** o backup nocturno *parecia* o suspeito, mas **não** dependia do fallback — o
  `DatabaseBackupService` já separa `executePhysicalBackup()` (com guarda) de `runPhysicalBackup()`
  (núcleo, para o agendador). Não foi alterado.
- **Verificação:** CF-01/03/07/08 **confirmados a falhar contra o código antigo**. Ligar o fail-closed
  fez cair **8 testes em 2 classes** que dependiam dos fallbacks sem o declarar (`MulticoreServicesTest`,
  `ReceiptPrintServiceTest`) — passaram a declarar o contexto, sem mudar asserções.
  `mvn -o clean test` → **356 testes, 0 falhas/erros/ignorados**.
- Spec/harness: [docs/CONTEXTO_FAIL_CLOSED_SPEC.md](../docs/CONTEXTO_FAIL_CLOSED_SPEC.md) +
  [docs/CONTEXTO_FAIL_CLOSED_HARNESS.md](../docs/CONTEXTO_FAIL_CLOSED_HARNESS.md) (CF-01..08 auto,
  CF-50..54 manuais).
- **Pendente manual:** CF-50..54 (com o backend de pé), em especial **CF-53** — login do superadmin no
  desktop.

### Progresso — 2026-08-07 (redução incremental de dependências entre domínios)

- Centralizada em `CompanyService.getCurrentCompanyReference` a resolução de empresa usada para
  associações entre agregados, com validação obrigatória da empresa activa antes da consulta.
- `ProductCategoryService`, `TaxRateService` e `WithholdingService` deixaram de importar e chamar
  directamente `CompanyRepository`; passam agora pela API pública do domínio `company`.
- Novo `CompanyServiceTest` cobre empresa activa e recusa cross-tenant antes do Repository.
- Verificação: compilação limpa; `mvn -q test` → **347 testes, 0 falhas/erros/ignorados**.
- Próxima fatia: separar o acesso do POS a entidades comerciais/inventário por contratos próprios,
  numa alteração isolada devido à atomicidade checkout → stock → pagamentos.

### Progresso — 2026-08-05 (POS: operação rápida e acabamento profissional)

- Cabeçalho simplificado: Cliente e Código de barras sempre visíveis; Armazém e Conta ficam em
  **Mais opções**, sem perder a selecção usada no checkout.
- Atalhos reais: **F2** produto, **F4** cliente, **F6** quantidade, **F9** finalizar e **Delete** só
  com foco no carrinho. Duplo clique reutiliza o editor de quantidade, inclusive decimal.
- Numerário ganhou recebimento rápido Exacto/100/200/500/1000 MT, ligado ao cálculo de troco existente.
- Spec/harness: `docs/POS_OPERACAO_RAPIDA_SPEC.md` e `docs/POS_OPERACAO_RAPIDA_HARNESS.md`.
- Verificação: `mvn clean compile`, testes focados do POS e `mvn test` completos verdes.

### Correcção — 2026-08-05 (IVA visível no POS e recibo térmico)

- Corrigido o fallback visual de produtos sem taxa explícita: o carrinho usa a mesma taxa padrão do
  checkout, em vez de os apresentar incorrectamente como isentos.
- Recibo térmico passa a identificar a taxa em cada artigo (`IVA: 16%`, `IVA: 5%` ou `IVA: Isento`),
  mantendo Subtotal, IVA agregado e Total no resumo.
- `POSKeyboardShortcutTest`, `ReceiptPrintServiceTest` e `POSServiceTest` verdes; compilação limpa.
- Layout térmico ajustado para 80 mm: duas colunas (Artigo 65% / Total 35%), com quantidade × preço
  e IVA empilhados sob a descrição para evitar texto e valores apertados.

### Progresso — 2026-08-06 (IVA: a taxa é do artigo, não do ecrã — bug fiscal fechado)

- **Bug encontrado a auditar o IVA a pedido do utilizador** ("o IVA está incluso no POS e noutros
  lugares?"): o **mesmo artigo** era tributado de forma diferente conforme a porta. Provado ao vivo:
  Farinha de Trigo, cadastrada **IVA Isento** — fatura `80,00 + 12,80 = 92,80`, POS `80,00 + 0,00`.
- **Causa:** `ComercialService` usava a taxa **enviada no pedido HTTP** e o `ComercialPanel` gravava
  lá `TaxRates.STANDARD_VAT` fixo (16%). O POS, esse, já lia a taxa do artigo. Contaminava a fatura,
  a encomenda, a fatura gerada da encomenda, a guia e a NC (que herdam a linha), a **declaração
  mensal de IVA** e o **SAF-T** — ambos lêem `invoice.taxAmount`.
- **Correcção:** `Product.effectiveTaxRate()` passa a ser a **fonte única** (mesmo padrão do
  `effectiveUnitPrice`): taxa do cadastro, senão a padrão. Chamada por `POSService.checkout`,
  `createInvoice` e `createOrder`; o POS deixou de repetir a regra. `ProductDTO.effectiveTaxRate()`
  espelha-a só para a pré-visualização do rascunho nos painéis. O campo `taxRate` do pedido
  mantém-se por compatibilidade mas é **ignorado** — era a porta que permitia a qualquer integração
  faturar à taxa que quisesse.
- **Verificação:** `ProductTest` (4, IV-04..07) + `ComercialServiceTest` (+3, IV-01..03) —
  **confirmado que IV-01/02 falham contra o código antigo**. `mvn -o clean test` → **343 testes, 0
  falhas**. Ao vivo: fatura de artigo isento com pedido a insistir em 16% → **IVA 0,00**, igual ao
  POS; artigo a 5% → 7,00 sobre 140 (e não 22,40).
- **Compras (fechado a seguir, 2026-08-06):** numa compra manda a **factura do fornecedor**, não o
  cadastro — o mesmo artigo chega com taxas diferentes de fornecedores diferentes. `PurchaseService`
  e `PurchaseOrderService` passaram a usar a taxa indicada na linha e, sem ela, `effectiveTaxRate()`
  do artigo; nunca a constante. Campo **"IVA da factura (%)"** no `ComprasPanel` (aceita `16`/`5,5`,
  vazio = taxa do artigo) + coluna IVA no rascunho. DTOs com campo opcional e construtor
  retrocompatível. `PurchaseOrderServiceTest` +2 (IV-11/12), **verificados a falhar contra os 16%
  cegos**. `mvn -o clean test` → **345 testes, 0 falhas**.
- Spec/harness: [docs/IVA_TAXA_CANONICA_SPEC.md](../docs/IVA_TAXA_CANONICA_SPEC.md) +
  [docs/IVA_TAXA_CANONICA_HARNESS.md](../docs/IVA_TAXA_CANONICA_HARNESS.md).

### Progresso — 2026-08-01 (notificações: marcar como lida — sino **e** página)

- **Porquê estado no cliente:** as notificações são **derivadas** (agregam aprovações, stock,
  validades e assinatura em tempo real), não entidades com id — não há "read flag" no servidor. Novo
  `NotificationReadStore`: chave estável `type|title|detail|when` + conjunto de lidas nas
  **Preferences** do utilizador (sobrevive ao reinício; sem Preferences funciona em memória, com
  tecto de 200 chaves).
- **Sino:** badge passa a contar **não-lidas**; cada notificação do popup é um submenu com **Abrir
  módulo** e **Marcar como lida**; entrada **Marcar todas como lidas** (desactivada se não houver).
- **Página `NotificationsPanel` alinhada** (fechou o limite v1 da spec): coluna **Leitura**
  (`Por ler`/`Lida`), dropdown de filtro por leitura, botões **Marcar como lida** / **Marcar todas
  como lidas** e resumo "N por ler de M". Sino e página partilham a **mesma instância** do store; a
  página avisa o `MainFrame` por `IntConsumer` e o **badge actualiza sem novo pedido HTTP**.
- **Também aqui:** correcção do estado vazio das tabelas (`TableEmptyState`) — o overlay "Sem
  registos." podia sobreviver a actualizações consecutivas do modelo/sorter e ficar por cima de
  linhas reais; passou a confirmar-se após os listeners do Swing/`RowSorter`, com barreira defensiva
  no layout. Regressão coberta em `TableUxTest`.
- **Verificação:** `mvn -o clean compile` limpo; `NotificationReadStoreTest` (5, NL-01..05) +
  `NotificationsPanelTest` (2, NL-06/07) + `NotificationFeedTest` (2) + `TableUxTest` (6) verdes.
  Spec/harness: [docs/NOTIFICACOES_LIDAS_SPEC.md](../docs/NOTIFICACOES_LIDAS_SPEC.md) +
  [docs/NOTIFICACOES_LIDAS_HARNESS.md](../docs/NOTIFICACOES_LIDAS_HARNESS.md).
- **Pendente manual:** NL-50..59 (UI ao vivo, com backend de pé).

### Progresso — 2026-07-27 (piloto UX: documento em painel completo, não modal — Encomenda)

- **Decisão de UX (2026-07-27):** híbrido — listagem como ecrã principal, **painel completo** para
  documentos com linhas, **modais só para acções curtas**. Piloto aplicado à **criação de Encomenda**.
- **Feito (só UI):** novo componente reutilizável `com.phcpro.gui.components.DocumentEditorHost`
  (barra: **← Voltar à lista** com guarda de alterações + título + **Guardar**). A aba **Encomendas**
  passou a `CardLayout` (lista ⇄ editor): **Nova Encomenda** mostra o editor a ecrã inteiro
  (reutiliza o mesmo `orderFormContent` + `issueOrderOrThrow`) em vez do modal; **Guardar** cria,
  informa, recarrega e volta à lista; **Voltar** confirma descarte se houver rascunho. O modal
  `openOrderFormDialog` foi **removido**.
- **Verificação:** `DocumentEditorHostTest` (2, DE-01/02); build limpo. Spec/harness:
  [docs/DOCUMENTO_PAINEL_EDITOR_SPEC.md](../docs/DOCUMENTO_PAINEL_EDITOR_SPEC.md) +
  [docs/DOCUMENTO_PAINEL_EDITOR_HARNESS.md](../docs/DOCUMENTO_PAINEL_EDITOR_HARNESS.md).
- **Alastrado à Fatura (2026-07-31):** a aba Faturação passou ao mesmo padrão (CardLayout lista⇄editor,
  reutiliza `invoiceFormContent` + `submitInvoiceOrThrow`, modal `openInvoiceFormDialog` removido). O
  `DocumentEditorHost` ganhou **scroll vertical** (formulários altos deixam de cortar os botões de baixo).
- **A seguir:** Compras (encomenda a fornecedor); suportar **editar** documento existente no host.

### Progresso — 2026-07-23 (central de notificações + bell)

- **Nova página `NotificationsPanel`:** tabela pesquisável/filtrável por tipo com alertas reais de
  aprovações pendentes, stock abaixo do mínimo, lotes vencidos/a vencer em 30 dias e assinatura em
  risco. Ações **Atualizar** e **Abrir módulo** encaminham para Aprovações, Stock ou Configurações.
- **Bell na barra superior:** contador de alertas + dropdown com as 5 primeiras notificações. O item
  **Ver todas** navega para a página completa. Recarrega ao trocar de empresa.
- **Cliente-fino e EDT:** `NotificationFeed` agrega apenas clientes HTTP; bell/página carregam via
  `SwingWorker`. O `companyId` é capturado no EDT e passado explicitamente à thread de fundo, evitando
  perder o tenant por causa do `CurrentUserContext` ser `ThreadLocal`; respostas antigas são ignoradas
  quando a empresa muda durante o carregamento.
- **Testes:** `NotificationFeedTest` cobre agregação das quatro fontes + estado vazio;
  `DesktopThinContextTest` confirma que o desktop continua sem DataSource/Services backend.
  `mvn test` → **321 testes, 0 falhas/erros/ignorados** (51 suites).

### Progresso — 2026-07-23 (lote de UX das tabelas: auto-hide, estados vazios, menu de contexto, loading)

- **Pedido do utilizador (4 melhoras de UI, todas):** ligadas centralmente em `styleScrollPane`.
  1. **Barra de navegação auto-esconde** (só quando a tabela transborda) + **atalhos** Home/End/
     PgUp/PgDn na tabela (`TableNavigator`).
  2. **Estados vazios** (`TableEmptyState`, novo): tabela sem linhas mostra "Sem registos." (texto
     personalizável por `putClientProperty("emptyText", …)`), overlay centrado que não tapa dados.
  3. **Menu de contexto** (`TableContextMenu`, novo): botão direito selecciona a linha e abre
     Copiar linha/célula · Ir topo/fundo (genérico; acções de domínio ficam nos botões).
  4. **Feedback de carregamento** (`UIHelper.loadAsync`): busca fora do EDT + cursor de espera;
     adoptado na aba **Guias de Remessa** (referência; restantes painéis adoptam incrementalmente).
- **Verificação:** `TableNavigatorTest` (9, +UX-01/02) + `TableUxTest` (5, UX-03..06). Spec/harness:
  [docs/UI_TABELAS_UX_SPEC.md](../docs/UI_TABELAS_UX_SPEC.md) + [docs/UI_TABELAS_UX_HARNESS.md](../docs/UI_TABELAS_UX_HARNESS.md).

### Progresso — 2026-07-23 (barra lateral de navegação em todas as tabelas)

- **Pedido do utilizador:** botões laterais nas tabelas para navegar (topo/cima/baixo/fundo), como
  noutros sistemas. Spec+harness.
- **Feito (só UI, sem backend):** novo componente `com.phcpro.gui.components.TableNavigator` — barra
  vertical (Topo `fas-angle-double-up`, Página acima `fas-angle-up`, Página abaixo `fas-angle-down`,
  Fundo `fas-angle-double-down`) **fora da tabela, no EAST do contentor** do scroll (mesmo padrão do
  rodapé `maybeAddListingFooter`, que vai ao SOUTH) — não sobrepõe células. **DRY:** ligada
  **num só ponto** — `UIHelper.styleScrollPane(...)` instala-a quando o conteúdo é uma `JTable`, pelo
  que **cobre transversalmente todas as ~60 tabelas** sem tocar nos ~80 sítios. Opera sobre a
  `JScrollBar` vertical (independente do modelo/filtro), idempotente, ícones vectoriais (sem emojis).
- **Verificação:** `TableNavigatorTest` (7, JUnit puro — TN-01..07). Spec/harness:
  [docs/TABELAS_NAVEGACAO_SPEC.md](../docs/TABELAS_NAVEGACAO_SPEC.md) +
  [docs/TABELAS_NAVEGACAO_HARNESS.md](../docs/TABELAS_NAVEGACAO_HARNESS.md).

### Progresso — 2026-07-23 (Guia de Remessa ao cliente a partir da encomenda — backend + desktop)

- **Pedido do utilizador:** converter encomenda em guia, à maneira profissional (spec+harness).
  **Reverte** a decisão de 2026-06-21 (MOVIMENTOS_COMERCIAIS §7.1 dizia "não é requisito").
- **Regra central (decidida com o utilizador): caminhos separados.** Uma encomenda vira **guia OU
  fatura**, nunca as duas; para faturar mercadoria expedida por guia, faz-se **nova encomenda**.
  Consequência: **`billOrder` NÃO foi alterado** (continua a exigir `PENDING` e a baixar stock).
- **Feito (backend):** novo documento `DeliveryGuide` + linhas no módulo `comercial`, série **`GR`**
  numerada por empresa (migração **V34**, `UNIQUE(company_id, guide_number)` — respeita a V31).
  `DeliveryGuideService` no molde do `StockTransfer`: nasce `PENDING_APPROVAL` e o **stock (SALE) sai
  só na aprovação** (FEFO, via `inventoryService.registerMovement` — mesmo caminho da faturação),
  MANAGER/ADMIN + auditoria. Gerar a guia trava a encomenda (`PENDING → GUIDE_PENDING → GUIDED`);
  rejeitar/cancelar liberta-a (`→ PENDING`). Controller `/api/comercial/delivery-guides`
  (create/approve/reject/cancel/list/get) + PDF `DeliveryGuidePrintService`
  (`GET /api/print/delivery-guide/{id}`, reutiliza cabeçalho/linhas partilhados + transporte/assinaturas).
- **Verificação:** `mvn clean compile` → **BUILD SUCCESS** (461 fontes);
  `DeliveryGuideServiceTest` (9, Mockito puro — GR-01..GR-10); `mvn test` → **305 testes,
  0 falhas/erros/ignorados** (48 suites; contexto Spring arranca com os novos beans).
- Spec/harness: [docs/GUIA_REMESSA_ENCOMENDA_SPEC.md](../docs/GUIA_REMESSA_ENCOMENDA_SPEC.md) +
  [docs/GUIA_REMESSA_ENCOMENDA_HARNESS.md](../docs/GUIA_REMESSA_ENCOMENDA_HARNESS.md) (§9 UI, GR-60..69).
  Canónico [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) actualizado (§1, §2, §4, §5.1, §7.1).
- **Fase 2 (feita) — UI cliente-fino + Movimentos:** `ComercialApiClient` ganhou list/get/create/
  approve/reject/cancel/print da guia (só HTTP/DTO). `ComercialPanel`: botão **"Converter em Guia"** na
  aba Encomendas (modal de transporte) + aba **"Guias de Remessa (GR)"** (aprovar/rejeitar/cancelar/
  imprimir/atualizar, com aviso de saída de stock na aprovação). **A conversão aparece nos Movimentos:**
  `MovimentoTipo.GUIA_REMESSA` + `MovimentosService` agrega `delivery_guides` (`MovimentosServiceTest`
  ajustado ao novo repositório). Harness GR-60..69 actualizado com o percurso desktop completo.
- **Pendente manual:** validação ao vivo (GR-50..55 backend, GR-61..69 desktop) com o backend de pé; regra de
  stock/guardas ficam no backend (a UI só chama HTTP).

### Progresso — 2026-07-22 (suite completa a correr localmente — fim da limitação de RAM)

- **Lacuna fechada:** a suite `@SpringBootTest` **só corria na CI** — localmente rebentava por RAM
  (visto em várias iterações). Causa raiz: cada teste de integração define um `spring.datasource.url`
  próprio → **contexto Spring distinto** por classe; a cache não os reutiliza mas mantinha **8 contextos
  ERP completos vivos ao mesmo tempo**.
- **Fix (só código de teste/build, nada no runtime de produção):**
  - `src/test/resources/spring.properties` → `spring.test.context.cache.maxSize=1` (evicta o contexto
    anterior antes de construir o próximo; pico de RAM ~8× menor, **sem** perda de reutilização — cada
    contexto já era usado por uma só classe).
  - `src/test/resources/application-test.properties` (perfil `test`): pool Hikari mínimo, springdoc +
    consola H2 desligados, logging silencioso. **Não** mexe em `headless` — os testes de contexto
    completo carregam beans Swing e exigem `headless=false` (ver `MulticoreServicesTest`).
  - `pom.xml`: `maven-surefire-plugin` com `forkCount=1`/`reuseForks=true` (um só JVM, para a cache ser
    eficaz) + `-Xmx1536m`.
- **Verificado localmente:** `mvn -o clean test` → **296 testes, 0 falhas, 0 erros, 0 ignorados** (47
  classes). Log confirma a eviction (pool do contexto anterior fecha ao arrancar o seguinte).

### Progresso — 2026-07-21 (dados completos da empresa em TODOS os documentos)

- **Pedido do utilizador:** todos os documentos com os dados da empresa. Auditoria: já mostravam Nome/
  NUIT/Morada (A4 também Email) via `CompanyHeaderRenderer` partilhado; **faltavam Telefone e Logótipo**.
- **Feito:** `Company` + `phone` + `logo` (`bytea`, migração **V33**). `CompanyHeaderRenderer` (cobre ~13
  documentos A4, DRY) e `ReceiptPrintService` (recibo térmico) passam a desenhar **Logótipo + Nome + NUIT
  + Morada + Telefone + Email**. À prova de falha: sem logo/telefone → sai na mesma; logótipo inválido →
  try/catch, sem crash. Entrada pelo **superadmin**: `Create/UpdateCompanyRequest` += `phone`,
  `PlatformCompanyDTO` += `phone`/`hasLogo`, endpoint `POST /api/platform/companies/{id}/logo`
  (octet-stream) + `PlataformaPanel` (campo Telefone + seletor de logótipo, reusa `UIHelper.readScaledImage`).
- **Verificado AO VIVO** (PostgreSQL real): definido telefone+logo na MZ; extração de texto do PDF de
  **fatura** e **recibo** confirma o cabeçalho completo + imagem embutida (`PdfVerify.java` com OpenPDF).
  Fail-safe (sem dados / logo inválido) OK. `mvn -o test` (Platform/Comercial/POS + regressão) **50, 0 falhas**.
  Dados de teste repostos.
- Spec/harness: [docs/DADOS_EMPRESA_DOCUMENTOS_SPEC.md](../docs/DADOS_EMPRESA_DOCUMENTOS_SPEC.md) +
  [docs/DADOS_EMPRESA_DOCUMENTOS_HARNESS.md](../docs/DADOS_EMPRESA_DOCUMENTOS_HARNESS.md) (DE-01..03 auto, DE-50..55 manuais).

### Progresso — 2026-07-20/21 (profissionalização rumo a produção — #1, #2, #6, #7)

- **#1 Teste de regressão** do bug multi-empresa: `InvoiceNumberUniquenessPerCompanyTest` (`@DataJpaTest`,
  leve — corre onde a suite `@SpringBootTest` não corre por RAM). Prova que o mesmo número coexiste em
  empresas diferentes e é rejeitado na mesma empresa. **Verificado que FALHA contra o código antigo** e
  passa com o fix. (`76f5d6e`)
- **#2 CI + gate de merge:** `build.yml` endurecido (`permissions: contents:read`, `timeout-minutes`).
  Confirmado via API pública que a **suite completa passa na CI** (o problema de RAM é só local).
  Documentado no README como ligar a **proteção de branch** (require PR + check `build`) — só o dono
  pode no GitHub. (`af5d28b`)
- **#6 Sem dados/segredos de demo em produção:** `DataLoader` gatado por `app.seed-demo-data` (default
  `true` em dev; `false` no perfil `prod`) — empresas/utilizadores/produtos fictícios já não entram em
  prod (taxas de IVA e categorias, de referência, continuam). Superadmin: senha por
  `${SUPERADMIN_PASSWORD}`; **sem env → conta não é criada** (fim do `superadmin/superadmin` default).
  **Verificado ao vivo** (H2 fresca, config tipo-prod): `ana/password`→400, `superadmin/superadmin`→400,
  `superadmin/<senha-config>`→OK (0 empresas). Senhas são bcrypt (legadas re-encriptadas no 1.º login).
- **#7 Numeração multi-empresa dos `payslips`** (fecha o follow-up do V31): a tabela não tinha
  `company_id` e a numeração (via `DocumentNumberService`) colidiria entre empresas. `Payslip` ganhou
  `company` (= empresa do colaborador), migração **V32** (add `company_id` + backfill de `employees` +
  FK + `UNIQUE(company_id, payslip_number)` no lugar da global). Teste de regressão
  `PayslipNumberUniquenessPerCompanyTest` (`@DataJpaTest`, **verificado que falha contra o código antigo**).
  **Verificado ao vivo:** PT e MZ emitiram ambos `REC-2026/3` e **coexistem** (V32 aplicada em PostgreSQL
  real, backfill 0 nulos). Dados de teste limpos.

- **Bug encontrado ao validar "vários postos ao mesmo tempo"** (2 sessões HTTP em paralelo contra o
  backend `prod`/PostgreSQL real): a numeração é **por empresa** (`document_sequences` chave
  `(company_id, series, doc_year)`, V30) mas a coluna do número tinha `UNIQUE` **global**. Duas empresas
  que cheguem ao mesmo número (ex.: ambas `FT-2026/5`) colidem → **HTTP 500**, mesmo **sem concorrência**.
  Afeta 8 tabelas (invoices, credit_notes, debit_notes, customer_orders, purchase_orders, purchases,
  receipts, stock_transfers). Relevante para a plataforma multi-empresa (superadmin + vários NUITs numa BD).
- **Correcção:** `UNIQUE(numero)` → `UNIQUE(company_id, numero)` nas 8 entidades + migração **V31**
  (`per_company_document_numbers`). Número string mantém-se (`FT-2026/N`); a empresa distingue pelo
  cabeçalho/NUIT. **Follow-up:** `payslips` não tem `company_id` (fica de fora, documentado).
- **Rede de segurança de concorrência:** `ConcurrencyRetry` (`architecture/concurrency`) — reexecuta a
  escrita em `ConcurrencyFailureException` (lock optimista `@Version` / pessimista), 3 tentativas,
  cada uma em transação nova. Ligado em `POSController.checkout` e `ComercialController.createInvoice`.
- **Verificação AO VIVO:** antes → MZ `FT-2026/5` colidia com PT `FT-2026/5` (500). Depois → MZ criou
  `FT-2026/5..12` **coexistindo** com a PT; **mesma empresa**, 2 postos, 8 faturas em paralelo →
  `FT-2026/6..13` gapless/distintas, stock −8. `mvn -o test` (Comercial+POS+PurchaseOrder) **64, 0 falhas**.
- Spec/harness: [docs/NUMERACAO_MULTIEMPRESA_SPEC.md](../docs/NUMERACAO_MULTIEMPRESA_SPEC.md) +
  [docs/NUMERACAO_MULTIEMPRESA_HARNESS.md](../docs/NUMERACAO_MULTIEMPRESA_HARNESS.md) (NM-01..02 auto,
  NM-50..55 manuais).

### Progresso — 2026-07-19 (Track B FECHADO — desktop cliente-fino completo)

- **Os 4 gigantes migraram para HTTP:** Stock (`884d67c`), POS (`da3596b`), Comercial (`d85febd`,
  "4.º/último gigante — fecha Track B"). Cada painel deixou de chamar o Service em processo.
- **Runtime cliente-fino (`a1af165`) — fecha o objetivo:** `DesktopApplication` passou a um contexto
  **não-web** (`WebApplicationType.NONE`), sem `DataSource`/JPA/Flyway; scan só de `com.phcpro.desktop`
  + `com.phcpro.gui` + `com.phcpro.modules.pos.scale`. `application-desktop.properties` reduzido a
  `desktop.api.base-url`. **O desktop arranca SEM base de dados.** `MainFrame` já não depende de nenhum
  `@Service`/`@Repository` (últimos 2 removidos: `companyService` morto, `subscriptionService` →
  `MySubscriptionApiClient`).
- **Consequência:** o PostgreSQL pode agora ser fechado ao exterior — só o backend lhe acede.
- **Testes:** `DesktopThinContextTest` (novo) prova que o contexto desktop arranca sem `DataSource` e
  sem serviços/repositórios de backend, só com clientes HTTP. `MainFrameNavigationSmokeTest` removido
  (testava o desktop GORDO em processo, arquitetura extinta). Fluxos de dinheiro por HTTP cobertos no
  harness (`aa43d36`). `mvn -o clean test` → **281 testes, 0 falhas**.
- **Falta:** 1.º `docker compose up` real numa VPS (sem Docker nesta máquina) + merge para `main`
  (`feat/stock-thin-client` está 6 commits à frente).

### Progresso — 2026-07-18 (Endurecimento de segurança + validação ao vivo do deploy)

- **Segurança (item #1 de go-live):** `TokenAuthenticationFilter` valida o token opaco e o `SecurityConfig`
  deixou de ser `permitAll()` — `/api/**` exige token; login/logout e `/actuator/health` públicos. Actuator
  expõe só health. Dockerfile healthcheck → `/actuator/health`. Spec/harness:
  [docs/SEGURANCA_HARDENING_SPEC.md](../docs/SEGURANCA_HARDENING_SPEC.md) (SH-01..07).
- **Validado AO VIVO** (backend `prod` standalone contra PostgreSQL 18 real): app arranca, Flyway valida 30
  migrações, sem token→401, com token→200, health UP, login errado→400. Os 11 domínios migrados também
  responderam ao vivo (ex.: `/api/platform/companies` devolveu empresas reais).
- **Deploy:** `scripts/deploy-smoke.sh` (verificação pós-deploy) + secção de deploy no README.
- Commits: `e1202db` (hardening). Falta: os 3 gigantes (POS/Stock/Comercial), 1.º `docker compose up` real, merge.

### Progresso — 2026-07-13 (Hospedagem do backend + desktop cliente-fino — Track B)

- **Decisão do utilizador:** hospedar o backend Spring Boot separadamente em **VPS + Docker** com
  **PostgreSQL** privado, e migrar o desktop para **cliente-fino (só HTTPS)** — a BD nunca exposta.
- **Deploy (Track A) — feito:** [Dockerfile](../Dockerfile) multi-stage (inclui `postgresql-client`
  para o backup físico), [docker-compose.yml](../docker-compose.yml) (backend + PostgreSQL privado +
  Caddy TLS automático), [Caddyfile](../Caddyfile), `.env.example`, `.dockerignore`. Guião +
  checklist de hardening: [docs/DEPLOY_VPS_SPEC.md](../docs/DEPLOY_VPS_SPEC.md). **Não testado ao vivo**
  (sem Docker nesta máquina); o `SecurityConfig` fica permissivo (item #1 de go-live).
- **Migração para cliente-fino (Track B) — arrancou:** padrão provado (inclui **PDF-over-HTTP** e o
  1.º painel gigante); **10 de ~26 domínios** a passar por HTTP em vez de chamar o Service em processo:
  - Novos clientes `@Profile("desktop")`: `ApprovalApiClient`, `CRMApiClient`, `FinanceApiClient`,
    `PromotionApiClient`, `InventoryApiClient`, `PurchaseApiClient`; `ComercialApiClient` +=
    `getAllInvoices/getAllProducts/getActiveCategories`. Painéis migrados: Aprovações, CRM, Financeiro,
    Promoções, **Dashboard**, **RH** (Clientes já estava). O Dashboard passou a consumir **DTOs** em vez
    de entidades JPA (`StockDTO`/`PurchaseDTO`) e tem arranque resiliente; `ApprovalService`/`CRMService`/
    `HRService`/`PayslipPrintService` saíram do `MainFrame` (já sem painel a usá-los).
  - **PDF-over-HTTP:** `DesktopApiClient` ganhou `getBytes` (GET→PDF) e `postForList` (POST→array). Como
    os endpoints `/api/print/**` já existiam, o recibo de salário do RH imprime via
    `/api/print/payslip/{id}`. Padrão pronto para as impressões dos restantes painéis.
    Testes: `DesktopApiClientTest` passou a **7** (TC-06 postForList, TC-07 getBytes).
  - **Fiscal** (8.º): `FiscalApiClient` colapsa os 8 serviços do painel. **1.º domínio a exigir endpoints
    novos no backend** — `GET /api/fiscal/saft/export` (DTO com metadados), `GET /api/fiscal/saft/validate`
    (XSD) e `GET /api/print/payroll-fiscal-map` (PDF). Harness TC-60.
  - **Compras** (9.º, **1.º gigante** — 1.324 linhas): `PurchaseApiClient` estendido colapsa
    purchase+order+reorder; `DesktopApiClient` ganhou `patch` (PATCH do estado do fornecedor);
    `InventoryApiClient` += armazéns. O painel converteu `Supplier`/`Warehouse`/`Purchase` (entidades)
    para DTOs (nome do armazém resolvido por lookup, pois o `PurchaseDTO` só traz o id). Harness TC-61.
  - **Plataforma** (10.º, superadmin): `PlatformApiClient` colapsa empresas+utilizadores+assinaturas+
    suporte (~22 métodos, só DTOs). 3 endpoints novos de options (plan/method/status). `/api/platform/**`
    não precisa de empresa. `PlatformCompanyService`/`PlatformUserService` saíram do `MainFrame`. Harness TC-62.
  - **Config** (11.º): maior esforço single-panel. **3 controllers novos** (`/api/users`, `/api/audit`,
    `/api/backup`) + `AppUserDTO`/`AuditLogDTO`/`BackupStatusDTO` + 6 clientes. **Decisão de design:** o
    **backup corre no servidor** (onde está a BD) e a auditoria dos backups é registada server-side (o
    desktop deixou de chamar `logEvent`). Papel do utilizador = por empresa. 7 serviços saíram do
    `MainFrame`. Harness TC-63.
  - Carregamento passou para `onPanelSelected()` (nunca no construtor) para não rebentar sem empresa.
  - **Falta:** médios (Promoções, Fiscal, RH, Dashboard) e os grandes (POS/Stock/Compras/Comercial),
    que precisam de endpoints novos. Só se fecha o PostgreSQL ao exterior quando **todos** migrarem.
- Spec/harness: [docs/DESKTOP_THIN_CLIENT_SPEC.md](../docs/DESKTOP_THIN_CLIENT_SPEC.md) +
  [docs/DESKTOP_THIN_CLIENT_HARNESS.md](../docs/DESKTOP_THIN_CLIENT_HARNESS.md) (TC-01..05 auto,
  TC-50..56 manuais).
- **Verificação:** `mvn -o compile` limpo; `DesktopApiClientTest` (5) verde. Ida-e-volta HTTP real de
  cada painel valida-se ao vivo (manual) quando o backend estiver de pé.

### Progresso — 2026-07-11 (Polish Visual PHC — aspecto ERP profissional)

- **`SlimScrollBarUI`** (novo): scroll bars finas (6 px), thumb violeta arredondado, sem setas — aplicado via `UIHelper.styleScrollPane()` em todos os JScrollPane do sistema.
- **`StatusBar`** (novo): rodapé de 24 px com módulo activo · nº registos · empresa · utilizador · hora. Timer interno (60 s) actualiza a hora. `MainFrame.navigate()` actualiza o módulo; `applyAuthenticatedUser` inicializa empresa e utilizador.
- **`SectionHeader`** (novo): cabeçalho de secção reutilizável — ícone opcional + título + separador 1 px.
- **`ModernPanel`** (melhorado): borda usa `UIHelper.BORDER` em painéis normais (adapta ao tema claro/escuro); painéis com gradiente mantêm branco translúcido subtil.
- **`styleTabbedPanePHC`** (novo em `UIHelper`): tabs PHC com linha de acento 3 px na base (sem fundo cheio). Migrado em **10 painéis**: ApprovalsPanel, ComercialPanel, ComprasPanel, ConfigPanel, CRMPanel, FinanceiroPanel, FiscalPanel, HRPanel, PlataformaPanel, StockPanel.
- **Tooltips premium** em `UIHelper.initGlobalTheme()`: fundo `BG_CARD`, borda `BORDER`, fonte 12 px, delay 600 ms.
- Spec: [docs/PHC_UI_POLISH_SPEC.md](../docs/PHC_UI_POLISH_SPEC.md) | Harness: [docs/PHC_UI_POLISH_HARNESS.md](../docs/PHC_UI_POLISH_HARNESS.md).
- **Verificação:** `mvn -o compile` → BUILD SUCCESS.


- **Filtros de tabela transversais:** o componente `com.phcpro.gui.components.TableFilter` (pesquisa
  com lupa + funil + dropdowns tipo/estado + período por data, colunas ordenáveis) foi estendido a
  **todas** as tabelas de listagem que ainda não tinham: Comercial (NC/ND/Recibos/Encomendas/Contas
  Correntes), Compras (Faturas/Fornecedores/Reposição/Contas a Pagar/Encomendas), Clientes, Stock →
  Gestão de Armazéns, RH (5 tabelas), Fiscal (Taxas/Retenções), Config (Auditoria/Utilizadores/
  Suporte), POS (Histórico de Vendas), Promoções. Acções que indexam a selecção passam por
  `TableFilter.selectedModelRow(...)` (o sorter faz a vista divergir do modelo). Fornecedores/
  Encomendas/Clientes migraram de pesquisa server-side/própria para o filtro cliente.
- Spec/harness: [docs/TABELAS_FILTROS_SPEC.md](../docs/TABELAS_FILTROS_SPEC.md) +
  [docs/TABELAS_FILTROS_HARNESS.md](../docs/TABELAS_FILTROS_HARNESS.md) (FT-01..07 auto, FT-50..68 manuais).
- **Verificação:** `mvn -o compile` limpo; `TableFilterTest` (7) verde; render confirmado ao vivo
  (Vendas → NC/Recibos, Compras → Faturas). Commit `a3ca84f`.
- **Backup/restore (Fase 6) — avançado:** validado ponta-a-ponta **exceto o apply final**, de forma
  não-destrutiva sobre a BD viva (PostgreSQL 18): `pg_dump -Fc` OK (58/58 tabelas, 520 objetos),
  `pg_restore --list` OK ⇒ arquivo completo e restaurável. **Falta** o passo BR-50..54 (restaurar em
  BD limpa + comparar contagens), que exige role com `createdb`/superuser — a role `multicore` não
  tem. É um passo manual de ~3 comandos (documentado no handover).

### Progresso — 2026-07-05 (Assinante: vista própria + alertas de expiração)

- **Vista do assinante** (só-leitura, tenant-scoped): `SubscriptionService.getMySubscription()` +
  `GET /api/subscription/me` → `MySubscriptionDTO` (plano, estado, validade, **dias restantes**,
  mensalidade). Desktop: aba **"A Minha Assinatura"** no `ConfigPanel`.
- **Alertas (7 dias)**, severidade única (vermelho expirada/suspensa; amarelo ≤7 dias): (1) aviso no
  login (`MainFrame.checkSubscriptionOnStartup`, disparado no `DesktopLauncher`); (2) chip permanente
  na barra de topo (só em risco); (3) linhas coloridas na aba Assinaturas do superadmin. À prova de
  falha (leitura falha ⇒ sem alerta, UI não rebenta).
- Spec/harness: SB-06 auto + SB-55..59 manuais.
- **Também corrigido no arranque real** (bugs que só aparecem a correr): colisão de bean
  (`PlatformSupportTicketRepository`), colisão de nome de entidade (`@Entity("PlatformSupportTicket")`)
  e crash da `MainFrame` no login do superadmin (`ClientesPanel` deixou de carregar no construtor).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (6) verde; app corre em
  PostgreSQL real (Flyway V24–V26 aplicadas).

### Progresso — 2026-07-05 (Superadmin — Fase 4: assistência) — funcionalidade fechada

- Módulo `support` (migração `V26`), distinto do `crm`: `SupportTicket` (assunto, descrição, estado
  OPEN/IN_PROGRESS/RESOLVED/CLOSED, prioridade, responsável) + `SupportMessage` (conversa;
  `fromSuperAdmin`). `SupportService` com **dois lados**: empresa (MANAGER/ADMIN, tenant-scoped) abre
  e responde; superadmin vê todos, responde (assume + OPEN→IN_PROGRESS) e muda estado. Resposta da
  empresa a RESOLVED reabre; CLOSED bloqueia. Controllers `/api/support/tickets` (tenant) e
  `/api/platform/support/tickets` (superadmin).
- **Desktop:** aba "Assistência" no `PlataformaPanel` (superadmin) + aba "Suporte à Plataforma" no
  `ConfigPanel` (empresa abre/consulta/responde).
- Spec/harness: ST-01..05 auto, ST-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `SupportServiceTest` (5) + `PlatformUserServiceTest` (5) +
  `PlatformCompanyServiceTest` (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Superadmin completo (Fases 1–4).** Assinaturas continuam manuais (sem gateway M-Pesa/e-Mola);
  esse é o próximo passo natural se se quiser cobrança automática.

### Progresso — 2026-07-05 (Superadmin — Fase 3: utilizadores globais)

- **Corte vertical.** `PlatformUserService` (SUPERADMIN + auditado) dá a visão de **todas** as
  empresas: `listUsers`, `createUser` (liga a empresa/papel), `setUserActive` (não desactiva o
  superadmin), `resetPassword`, `grantAccess`, `revokeAccess` (**protege o último ADMIN**). Controller
  `/api/platform/users`. `AppUser.revokeCompany` novo (orphanRemoval).
- **Desktop:** aba "Utilizadores" no `PlataformaPanel` (Novo, Conceder/Alterar Acesso, Revogar,
  Repor Senha, Activar/Desactivar).
- Spec/harness: SU-01..05 auto, SU-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `PlatformUserServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Próxima:** Fase 4 (tickets `support` empresa→superadmin + abrir tickets no lado da empresa).

### Progresso — 2026-07-04 (Superadmin — Fase 2: assinaturas + pagamentos)

- **Corte vertical** (backend + UI). Módulo `subscription` (migração `V25`): `Subscription` (1:1 com
  empresa; plano TRIAL/BASIC/PRO/ENTERPRISE, estado TRIAL/ACTIVE/SUSPENDED/EXPIRED, validade, preço;
  `effectiveStatus()` deriva EXPIRED da validade) + `SubscriptionPayment` (valor, método
  DINHEIRO/MPESA/EMOLA/TRANSFERENCIA/OUTRO, período, nota). `SubscriptionService` (SUPERADMIN +
  auditoria): `listOverview/saveSubscription/changeStatus/recordPayment/listPayments` + `allowsLogin`
  (política interna). Registar pagamento **estende a validade** e reactiva. Controller
  `/api/platform/subscriptions`.
- **Login:** passa a filtrar por `allowsLogin` além de `company.active` — assinatura expirada/suspensa
  bloqueia; sem assinatura continua acessível.
- **Desktop:** aba "Assinaturas & Pagamentos" no `PlataformaPanel` (Definir Plano/Validade, Registar
  Pagamento, Ver Pagamentos, Suspender/Reactivar).
- Spec/harness actualizados (SB-01..05 auto, SB-50..54 manuais).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**.
- **Próximas:** Fase 3 (utilizadores globais), Fase 4 (tickets `support`).

### Progresso — 2026-07-04 (Superadmin / Consola da Plataforma — Fase 1)

- **Pedido do utilizador:** um **superadmin** (dono da plataforma) que vê todas as empresas,
  activa/desactiva, gere utilizadores/assinaturas/pagamentos e dá assistência. Decisões: aba
  escondida no mesmo app (papel `SUPERADMIN`); pagamentos **manuais**; assistência por **tickets**
  empresa→superadmin; empresa suspensa **bloqueia o login** (não mata sessões vivas).
- **Processo:** spec+harness →
  [docs/SUPERADMIN_PLATAFORMA_SPEC.md](../docs/SUPERADMIN_PLATAFORMA_SPEC.md) +
  [docs/SUPERADMIN_PLATAFORMA_HARNESS.md](../docs/SUPERADMIN_PLATAFORMA_HARNESS.md) (SA-01..04 auto,
  SA-50..56 manuais). Entregue **por fases**; esta é a **Fase 1**.
- **Fase 1 (feita):** `AppUser.platformAdmin` + `Company.active` (migração `V24`); seed idempotente
  da conta `superadmin/superadmin`. Autorização: `/api/platform/**` sai do tenant-check do
  `SecurityInterceptor` (exige `platformAdmin`, papel `SUPERADMIN`, sem empresa);
  `PermissionGuard.requireSuperAdmin`; `TenantAccessService.requireSuperAdmin`. Login devolve
  `superAdmin` e só empresas **activas**; utilizador de tenant sem empresa activa é recusado.
  Módulo `platform`: `PlatformCompanyService` (listar/criar/editar/activar-desactivar, auditado) +
  controller `/api/platform/companies` + DTOs. Desktop: sessão transporta `superAdmin`;
  `PlataformaPanel` (aba Empresas: tabela + Novo/Editar/Activar-Desactivar); `MainFrame` mostra só a
  aba "Plataforma" ao superadmin (sem seletor de empresa/abas de tenant). Logout excluído do
  interceptor (superadmin não tem empresa).
- **Próximas fases:** 2 (assinaturas+pagamentos), 3 (utilizadores globais), 4 (tickets `support`),
  5 (painéis Pagamentos/Utilizadores/Assistência + abrir tickets no lado da empresa).
- **Verificação:** `mvn -o compile` limpo; `PlatformCompanyServiceTest` (4) +
  `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**. Suite completa
  não corre por falta de RAM (limitação de ambiente, como iterações anteriores).

### Progresso — 2026-07-04 (config separada por tipo de documento + comentário do recibo)

- **Pedido do utilizador:** o **POS** deve ter **configuração separada** dos documentos comerciais, e
  poder **definir o comentário/rodapé** que aparece no recibo.
- Config passou a ser **por `DocumentType`** (COMMERCIAL vs POS_RECEIPT), independente por empresa
  (entidade ganha `document_type`; unique `(company_id, document_type)`; migração `V23` recria a
  tabela de forma portável H2+PostgreSQL). `DocumentColumnsDTO` ganha `footer`. Service/controller/UI
  passam o tipo. Os 4 serviços comerciais usam COMMERCIAL; o `ReceiptPrintService` usa POS_RECEIPT e o
  rodapé configurável (`footer`; vazio = "Obrigado pela sua preferência!", suporta multi-linha).
  `ConfigPanel` ganhou selector de tipo + campo "Comentário do recibo".
- Spec/harness actualizados (DC-07 auto; DC-54..DC-57 manuais).
- **Verificação:** compila; testes da funcionalidade + serviços tocados → **56, 0 falhas**
  (`DocumentConfigServiceTest` 6, `LineItemsTableRendererTest` 4, POS/Comercial/Reorder/Inventory).
  ⚠️ A suite **completa** (209) não correu por **falta de RAM da máquina** (~568 MB livres) nos testes
  de integração Spring — limitação de ambiente, não de código (correr num ambiente com mais memória).

### Progresso — 2026-07-04 (colunas configuráveis dos documentos comerciais)

- **Pedido do utilizador:** poder definir **quais colunas** aparecem nos documentos comerciais
  (Fatura/Encomenda/NC/Guia, que partilham o `LineItemsTableRenderer`). Só mostrar/ocultar.
- **Processo:** spec+harness → skill `phc-new-module` → **implementação delegada a um agent** →
  revisão `phc-solid-review` (sem apontamentos bloqueantes) → verificação e commit.
- **Módulo `documents`** (`DocumentColumnConfig` por empresa, 8 flags, migração `V22`;
  `DocumentConfigService.getColumns/save` com MANAGER/ADMIN + auditoria `DOCUMENT_COLUMNS_UPDATE` +
  regra "pelo menos uma coluna"; `DocumentColumnsDTO` record; controller `GET/PUT /api/documents/columns`).
  `LineItemsTableRenderer` ganhou overload `build(rows, cols)` (extraiu `record Column` + `activeColumns`,
  **melhor DRY**; `build(rows)` delega em `all()`, retrocompatível). Os 4 serviços de impressão passam a
  config. UI: aba "Colunas dos Documentos" no `ConfigPanel` (8 checkboxes + Guardar) + wiring no `MainFrame`.
- Spec/harness: [docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md) +
  [docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md) (DC-01..06 auto, DC-50..53 manuais).
- Testes: `DocumentConfigServiceTest` (5) + `LineItemsTableRendererTest` (+1). `mvn clean test` → **209, 0 falhas**.
- **Recibo do POS (extensão):** `ReceiptPrintService` passou a respeitar a mesma config no que cabe
  num recibo térmico — Qtd e Preço Unit. como colunas opcionais, Referência/Código de Barras como
  sublinha do nome; Descrição e Total sempre; Validade/IVA/subtotal por linha não aplicáveis.
  Harness DC-54/DC-55. `mvn test` → **209, 0 falhas** (recibo continua sem teste automático, como os
  restantes print services; validação manual).

### Progresso — 2026-07-04 (campos profissionais do armazém)

- **Pedido do utilizador:** `Warehouse` ganha (migração `V21`) **active**, **type**
  (`WarehouseType`: Loja/Depósito/Central/Trânsito), **allowsSales**, **manager**, **phone**.
  `getWarehousesByCompany` passa a filtrar **inactivos**; novo `getSalesWarehousesByCompany`
  (activo + allowsSales) usado pelo **POS** (deixa de vender de depósito). Diálogo "Criar Armazém"
  com Tipo/Responsável/Telefone/Permite vendas; `createWarehouse` overload novo (antigos delegam,
  retrocompatível).
- Spec/harness: [docs/ARMAZEM_PROFISSIONAL_SPEC.md](../docs/ARMAZEM_PROFISSIONAL_SPEC.md) +
  [docs/ARMAZEM_PROFISSIONAL_HARNESS.md](../docs/ARMAZEM_PROFISSIONAL_HARNESS.md) (AR-01 auto, AR-50..53 manuais).
- Testes: `InventoryServiceTest` +1 (filtro de vendas). `mvn test` → **203, 0 falhas**.
- **Ecrã de gestão de armazéns (feito):** nova aba "Gestão de Armazéns" no `StockPanel` (tabela com
  todos + Novo/Editar/Activar-Desactivar, duplo-clique edita). Backend `getAllWarehousesByCompany`,
  `updateWarehouse`, `setWarehouseActive` (MANAGER/ADMIN + auditoria). Diálogo criar/editar partilhado.
  Harness AR-53..AR-55.

### Progresso — 2026-07-03 (polish: cor de estado nas linhas)

- **Pedido do utilizador:** leitura de estado **por linha**. `UIHelper.styleTable` deteta uma coluna
  "Estado"/"Situação"/"Status" e pinta a **linha** com tom subtil (blend ~18% com a zebra, adapta ao
  tema). Vocabulário semântico centralizado em `statusColorFor` (verde/amarelo/vermelho, PT/EN de
  retalho, inclui ESGOTADO/BAIXO/EM STOCK/ANULADO/EM DÍVIDA…). Colunas "Estado" acrescentadas a
  **Níveis de Stock** e **Reposição**. Automático/DRY para qualquer tabela com coluna de estado.
- Só apresentação. Spec/harness: [docs/COR_ESTADO_LINHAS_SPEC.md](../docs/COR_ESTADO_LINHAS_SPEC.md) +
  [docs/COR_ESTADO_LINHAS_HARNESS.md](../docs/COR_ESTADO_LINHAS_HARNESS.md) (CE-01..05 manuais).
  `mvn test` → **202, 0 falhas**.

### Progresso — 2026-07-03 (preço grosso vs retalho — por produto + qtd mínima)

- **Sugestão do utilizador (3/3):** `Product` ganha `wholesalePrice` + `wholesaleMinQty` (migração
  `V20`). Regra pura `Product.effectiveUnitPrice(qty)` — aplica grosso quando `qty ≥ min`; senão
  retalho. **Aplicada nos 3 fluxos** (`createInvoice`, `createOrder`, `POSService.checkout`) sem
  tocar no `LineCalculator` (recebe o preço já resolvido; IVA por unidade). `ProductDTO` +2 campos;
  diálogos Cadastrar/Editar Produto com "Preço Grosso" e "Qtd mín. grosso" (opcionais). Retrocompatível
  (createProduct/updateProduct antigos delegam com grosso null).
- Spec/harness: [docs/PRECO_GROSSO_SPEC.md](../docs/PRECO_GROSSO_SPEC.md) +
  [docs/PRECO_GROSSO_HARNESS.md](../docs/PRECO_GROSSO_HARNESS.md) (PG-01/02 auto, PG-50..53 manuais).
- Testes: `ComercialServiceTest` +2 (grosso/retalho por quantidade). `mvn test` → **202, 0 falhas**.
- **As 3 sugestões pedidas ficaram concluídas** (reposição automática, Mobile Money, preço grosso).

### Progresso — 2026-07-03 (Mobile Money: M-Pesa / e-Mola)

- **Sugestão do utilizador (2/3):** `PaymentMethod` ganha **MPESA** e **EMOLA**, tratados como
  **electrónicos** (entram na tesouraria, não na gaveta; exigem conta; guardam a **referência** da
  transação em `PaymentEntry.reference`). Sem migração (enum é STRING). Devolução também reembolsa
  por tesouraria. Recibo mostra "M-Pesa"/"e-Mola". UI POS `askPayment` com os métodos + campo
  Referência; diálogo de devolução idem.
- Spec/harness: [docs/MOBILE_MONEY_SPEC.md](../docs/MOBILE_MONEY_SPEC.md) +
  [docs/MOBILE_MONEY_HARNESS.md](../docs/MOBILE_MONEY_HARNESS.md) (MM-01 auto, MM-50..53 manuais).
- Testes: `POSServiceTest` +1 (MPESA → tesouraria, não gaveta). `mvn test` → **200, 0 falhas**.
- **A seguir (3/3):** preços grosso vs retalho.

### Progresso — 2026-07-03 (reposição automática de stock — nova funcionalidade)

- **Sugestão do utilizador (1/3):** novo `ReorderService.suggestions(companyId)` — lista de produtos
  **abaixo do stock mínimo** (soma de todos os armazéns; produto sem stock = 0), com quantidade a
  encomendar **arredondada a caixas inteiras** (`unitsPerBox`), ordenada por urgência. Leitura pura
  (não cria encomendas). **API** `GET /api/purchases/reorder-suggestions`; **UI** nova aba
  "Reposição" no `ComprasPanel` (botão "Criar Encomenda" salta para a aba de encomendas).
- Spec/harness: [docs/REPOSICAO_AUTOMATICA_SPEC.md](../docs/REPOSICAO_AUTOMATICA_SPEC.md) +
  [docs/REPOSICAO_AUTOMATICA_HARNESS.md](../docs/REPOSICAO_AUTOMATICA_HARNESS.md) (RA-01..03 auto, RA-50..52 manuais).
- Testes: `ReorderServiceTest` (3). `mvn test` → **199, 0 falhas**.
- **A seguir (2/3, 3/3):** Mobile Money (M-Pesa/e-Mola) e preços grosso vs retalho.

### Progresso — 2026-07-02 (resiliência das ligações à BD — PC de balcão)

- **Lacuna operacional fechada:** com a app aberta e a máquina em suspensão longa, o pool Hikari
  mantinha ligações **mortas** ao PostgreSQL → gravações falhavam até reiniciar (visto em uso real).
  Config de resiliência no perfil **`desktop`** e espelhada em **`prod`**: `keepalive-time=120s`
  (sonda ligações ociosas), `max-lifetime=600s` (rotação), `connection-timeout=10s` (falha rápida),
  `validation-timeout=5s`; desktop com pool 5/min-idle 1. Sem tocar em código/Services/schema.
- Validado: desktop arranca com Hikari sem avisos de `keepalive/maxLifetime`; `mvn test` 196/0.
- Spec/harness: [docs/RESILIENCIA_LIGACOES_SPEC.md](../docs/RESILIENCIA_LIGACOES_SPEC.md) +
  [docs/RESILIENCIA_LIGACOES_HARNESS.md](../docs/RESILIENCIA_LIGACOES_HARNESS.md) (RL-01..RL-05 manuais).

### Progresso — 2026-07-01 (documento de inventário simplificado)

- **Pedido do utilizador:** o PDF de inventário passou a ter **só 6 colunas** — Referência · Código de
  Barras · Nome · Quantidade · **Caixas** (qtd ÷ und/caixa) · **Valor**. Removidas SKU, Armazém,
  Mínimo, Preço de Compra, Estado. **Valor a preço de VENDA** (`unitPrice`), por linha e no total.
  Rodapé reduzido a "Artigos no inventário" + "VALOR TOTAL DO STOCK". `InventoryReportPrintService`.

### Progresso — 2026-07-01 (venda ao grosso: helper "Caixas" na faturação)

- **Decisão (utilizador vende ao grosso):** a linha da **fatura** ganhou campo opcional **"Caixas"**
  que preenche a **Qtd (unidades) = caixas × unidades/caixa** do produto. **Cálculo de dinheiro
  continua por unidade** (`LineCalculator` intacto, IVA por unidade). **POS não é tocado** (retalho
  rápido à unidade). Entrada directa em unidades mantém-se (campo vazio). `ComercialPanel.applyInvoiceBoxes()`.
- Harness CX-09/CX-10. Helper **replicado na Encomenda a Cliente** (`applyOrderBoxes()`) — mesmo
  comportamento (caixas → Qtd em unidades, dinheiro por unidade).

### Progresso — 2026-07-01 (edição de produtos + entrada de stock por caixas)

- **Editar Produto (lacuna fechada):** existia `createProduct` mas **não havia como actualizar** um
  artigo já cadastrado. Novo `ComercialService.updateProduct(...)` (SKU imutável = identidade;
  referência/código de barras revalidam unicidade **excluindo o próprio**; não toca no stock;
  auditoria `PRODUCT_UPDATE`). **UI:** botão **"Editar Produto"** no topo do `StockPanel` → diálogo
  com selector de produto que **pré-preenche** o formulário (mesmos campos do cadastro, incluindo
  unidades/caixa, IVA, categoria e imagem). Testes: `ComercialServiceTest` 20 → **23**.
- **Entrada de stock por caixas:** ver abaixo.

### Progresso — 2026-07-01 (entrada de stock por caixas)

- **Pedido do utilizador:** dar entrada de mercadoria **por nº de caixas** (a loja arruma às caixas),
  mantendo faturação/reserva/guia/POS **em unidades**. Já existiam `Product.unitsPerBox`, o campo
  "Unidades por Caixa" no cadastro e a coluna "Qtd Caixas" no inventário — faltava o **caminho de
  entrada por caixas**.
- **Decisão:** a unidade interna de stock continua a **UNIDADE** (nada muda a jusante). A caixa é só
  camada de **entrada + visualização**. No `createBatchEntryDialog` (stock inicial **e** "Adicionar
  Lote/Validade" — mesmo método) o operador indica **Nº de Caixas + Unidades soltas**, vê as
  **unidades/caixa** do produto e o **Total (unidades)** calculado em tempo real
  (`total = caixas × unitsPerBox + soltas`); o movimento grava o total em unidades.
- Helpers `parseIntOrZero`/`parseDecimalOrZero` + `UIHelper.onTextChange` para recálculo ao vivo.
  Sem tocar em Services/DTOs (faturação/POS/guia/inventário inalterados).
- Spec/harness: [docs/CADASTRO_POR_CAIXAS_SPEC.md](../docs/CADASTRO_POR_CAIXAS_SPEC.md) +
  [docs/CADASTRO_POR_CAIXAS_HARNESS.md](../docs/CADASTRO_POR_CAIXAS_HARNESS.md) (CX-01..CX-08 manuais).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 196 testes, 0 falhas**.

### Progresso — 2026-07-01 (recepção parcial de encomenda a fornecedor)

- **Fecha a "Fase 4 (futuro)"** das compras: a recepção de encomenda deixou de ser tudo-ou-nada.
  - `PurchaseOrderLine.receivedQuantity` (migração `V19`) regista o já recebido; **em falta =
    quantity − receivedQuantity**. Novo estado `PARTIALLY_RECEIVED` (ciclo
    `ORDERED → PARTIALLY_RECEIVED* → RECEIVED` / `CANCELLED`).
  - `receivePartial(id, itens)`: recebe as quantidades indicadas por linha (entra stock só pela
    quantidade do acto, FEFO/lote, sem recontar o já recebido), valida `0 < qty ≤ emFalta`, recalcula
    o estado. `receiveOrder` passou a **receber o em falta** (de ORDERED ou PARTIALLY_RECEIVED) sem
    dupla entrada; `cancelOrder` aceita PARTIALLY_RECEIVED (stock recebido mantém-se). MANAGER/ADMIN +
    auditoria. Estado é **derivado** das linhas, nunca escrito à mão.
  - **API:** `POST /api/purchases/orders/{id}/receive-partial`. **UI:** botão "Receber Parcial…" no
    `ComprasPanel` (modal com tabela editável "A receber agora" por linha).
  - Spec/harness: [docs/RECECAO_PARCIAL_SPEC.md](../docs/RECECAO_PARCIAL_SPEC.md) +
    [docs/RECECAO_PARCIAL_HARNESS.md](../docs/RECECAO_PARCIAL_HARNESS.md) (RP-01..RP-12 automáticos,
    RP-50..RP-52 manuais). Testes: `PurchaseOrderServiceTest` 8 → **20**.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 193 testes, 0 falhas**.

### Progresso — 2026-07-01 (exportação fiscal de vendas — estrutura SAF-T)

- **Nova exportação fiscal de auditoria:** o sistema calculava o apuramento de IVA mas não
  exportava um ficheiro estruturado dos documentos de venda. Novo `FiscalSalesExportService`
  produz um **XML alinhado com a estrutura SAF-T** (`Header` / `MasterFiles` / `SourceDocuments` →
  `SalesInvoices`) para um período.
  - **Permissão** MANAGER/ADMIN + guarda multi-tenant (`requireCompany`). Inclui faturas emitidas
    (`APPROVED/PAID/PARTIALLY_PAID`) e **anuladas** (`CANCELLED`, com estado, sem somar aos totais);
    exclui `DRAFT/PENDING*/REJECTED`.
  - **Reutiliza os valores fiscais persistidos** na fatura (não recalcula impostos → não diverge da
    engine de faturação/POS). Escaping XML correcto, determinístico, totais conferíveis.
  - **API:** `GET /api/fiscal/saft?companyId&from&to` (`application/xml`). **UI:** botão
    "Exportar SAF-T (Vendas)" na aba IVA do `FiscalPanel` (usa o ano/mês selecionado, grava `.xml`
    via `JFileChooser`).
  - **Limite honesto:** segue a *estrutura* SAF-T mas **não é certificado** — validar contra a XSD
    oficial da AT-MZ antes de submissão (documentado na spec/harness, SF-51).
  - Spec/harness: [docs/FISCAL_SAFT_EXPORT_SPEC.md](../docs/FISCAL_SAFT_EXPORT_SPEC.md) +
    [docs/FISCAL_SAFT_EXPORT_HARNESS.md](../docs/FISCAL_SAFT_EXPORT_HARNESS.md) (SF-01..SF-14
    automáticos, SF-50..SF-52 manuais). Testes: `FiscalSalesExportServiceTest` (12).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 181 testes, 0 falhas**.

### Progresso — 2026-06-30 (backup físico restaurável — recuperação de desastres)

- **Lacuna fechada (parcial):** o `BackupService` existente grava um **dump JSON lógico lossy**
  (subconjunto de campos, relações achatadas) — bom para auditoria/verificação, **não restaurável**.
  Faltava o caminho de DR real. Novo `DatabaseBackupService` faz **backup físico via
  `pg_dump`/`pg_restore`** (formato custom `-Fc`), fidelidade total da instância.
  - `executePhysicalBackup()` (ADMIN): recusa BD não-PostgreSQL, password só por `PGPASSWORD` no
    ambiente do subprocesso (nunca na linha de comando), escreve `backups/multicore_<db>_<ts>.dump`.
  - `restorePhysicalBackup(path, confirmOverwrite)` (ADMIN, **destrutivo** → exige confirmação):
    `pg_restore --clean --if-exists --no-owner`.
  - Config: `backup.pg-bin-dir` (binários fora do PATH) e `backup.dir`.
  - **UI:** botão "Backup Físico (BD)" no `ConfigPanel` (aba Cópias de Segurança), via
    `runWithProgress`; descrição da aba corrigida (já não diz "base de dados em memória").
  - Spec/harness: [docs/BACKUP_RESTORE_SPEC.md](../docs/BACKUP_RESTORE_SPEC.md) +
    [docs/BACKUP_RESTORE_HARNESS.md](../docs/BACKUP_RESTORE_HARNESS.md) (BR-01..BR-12 automáticos,
    BR-50..BR-54 manuais — o round-trip real precisa de PostgreSQL + binários, fora de CI).
  - Testes: `DatabaseBackupServiceTest` (12: parsing JDBC, construção de comandos, guardas).
- **Ainda pendente para fechar o item de DR:** correr BR-50..BR-54 num ambiente separado (gerar
  `.dump` → restaurar em BD limpa → confirmar contagens idênticas). O software está pronto; falta a
  execução manual com PostgreSQL real.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 169 testes, 0 falhas**.

### Progresso — 2026-06-29/30 (polish de UI profissional transversal)

Várias iterações **só de apresentação** (sem tocar em Services/DTOs/regras), cada uma com spec+harness:

- **Grelha estilo PHC** (`UIHelper.styleTable`): números alinhados à direita (qtd/preço/IVA/total) +
  cabeçalho com separadores; **calha de selecção** ▸ na margem esquerda (rowHeader, sem mexer no
  modelo de colunas). [POS_*]/grelha.
- **Cabeçalho POS compacto:** código de barras subiu para a linha dos selects; catálogo ganhou
  altura. [POS_CABECALHO_COMPACTO_SPEC](../docs/POS_CABECALHO_COMPACTO_SPEC.md).
- **RH — aba "Visão Geral":** 6 cards de KPI + 2 gráficos, reutilizando `KpiCard`/`SimpleBarChart`
  extraídos do Dashboard (DRY). [RH_VISAO_GERAL_SPEC](../docs/RH_VISAO_GERAL_SPEC.md).
- **Inputs/selects profissionais:** `FIELD_BG`/`BORDER` por tema + **realce de foco a acento** +
  combo achatado. [INPUTS_SELECTS_SPEC](../docs/INPUTS_SELECTS_SPEC.md).
- **Inspetor de detalhes** (duplo-clique) → `ModernFormDialog` só-leitura (`asReadOnly`).
  [INSPETOR_DETALHES_SPEC](../docs/INSPETOR_DETALHES_SPEC.md).
- **Aprovações:** inspector inline → **modal de decisão** (Aprovar/Rejeitar/Fechar);
  `ModernFormDialog.addActionButton`/`close`. [APROVACOES_MODAL_SPEC](../docs/APROVACOES_MODAL_SPEC.md).
- **Formulários inline → modal** (CRM Folha de Obra, Financeiro Recebimento, Config Utilizador +
  bug do combo de perfil corrigido). [FORMULARIOS_INLINE_MODAL_SPEC](../docs/FORMULARIOS_INLINE_MODAL_SPEC.md).
- **Prompts de dados → modal** via helpers `UIHelper.promptRequiredText`/`promptAmount` (motivos de
  anulação/rejeição, abrir/fechar caixa). Removido `StockPanel.createWarehouseDialog` V1 morto.
  [PROMPTS_MODAL_SPEC](../docs/PROMPTS_MODAL_SPEC.md).
- **Despesa RH** migrada para `ModernFormDialog`. [MODAIS_ICONES_SPEC](../docs/MODAIS_ICONES_SPEC.md).
- **Modais de despesa/submeter:** despesa do RH em modal.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**.

### Progresso — 2026-06-28 (gestão de categorias profissional + modal de pagamento premium)

- **Gestão de categorias profissionalizada** (tab Categorias do Stock, sobre o `ProductCategoryService`
  existente): tabela com **amostra de cor** (`ColorCellRenderer`), **contagem de produtos** por
  categoria, **pesquisa** por código/nome. Diálogo com **seletor de cor** (`JColorChooser` + amostra ao
  vivo + Limpar) num `ModernFormDialog` premium (ícone `fas-tags`). Activar/desactivar (sem apagar).
  Spec/harness: [docs/CATEGORIAS_GESTAO_SPEC.md](../docs/CATEGORIAS_GESTAO_SPEC.md) +
  [docs/CATEGORIAS_GESTAO_HARNESS.md](../docs/CATEGORIAS_GESTAO_HARNESS.md) (CT-01..10).
- **Modal de pagamento do POS premium:** `askPayment` migrou de `JOptionPane` para `ModernFormDialog`
  (ícone `fas-money-bill-wave`, subtítulo, botão "Confirmar Pagamento" `fas-check`); validação no
  `onSave` (mantém aberto em erro) — fim da recursão.
- **Carrinho POS:** container com `VScrollPanel` (acompanha a largura, scroll vertical quando falta
  altura) → a tabela deixa de colapsar para só o cabeçalho. Bloco **Subtotal s/ IVA + IVA** sempre
  visível por cima do TOTAL. Selector de vista (Venda POS | Histórico) na mesma linha que as acções de
  caixa (poupa espaço). Lupa de pesquisa **dentro** do input.

### Progresso — 2026-06-28 (catálogo POS em cards com imagem + modais premium)

- **Catálogo POS em cards com imagem:** o separador "Venda POS" passou a mostrar os produtos como
  **grid de cards** (imagem/marcador + nome + preço). **Clicar adiciona ao carrinho** (qtd 1, FEFO/
  promoção automáticos); clicar de novo **incrementa** (merge), como num carrinho web. O leitor de
  código de barras usa o mesmo caminho (`addProductToCart`). Removido o formulário detalhado antigo
  (combo de produto, qtd, desconto, lote, série, "Adicionar Artigo", `refreshFEFOHint`).
- **Imagem por produto (bytea):** `Product.imageData`, `ProductDTO.image`, migração `V18`,
  `ComercialService.updateProductImage`; cadastro de produto (StockPanel) ganhou **selector de imagem**
  com pré-visualização (auto-reduzida a 320px). Helpers `UIHelper.readScaledImage` / `imageIconFromBytes`.
- **Selects alinhados no topo:** Cliente/Armazém/Conta passaram para uma **barra superior compacta**
  (estilo web), libertando largura para catálogo + carrinho.
- Spec/harness: [docs/POS_CATALOGO_CARDS_SPEC.md](../docs/POS_CATALOGO_CARDS_SPEC.md) +
  [docs/POS_CATALOGO_CARDS_HARNESS.md](../docs/POS_CATALOGO_CARDS_HARNESS.md) (PC-01..11).
- **Modais premium + botões estilizados:** todos os ~21 modais legados (`JOptionPane`) migraram para
  `ModernFormDialog` (cabeçalho com badge+ícone+subtítulo, botões Cancelar/Confirmar com ícone, rodapé
  fixo). `createDialogForm` passou a **grelha de 2 colunas**. Lupa de pesquisa do POS **dentro** do input.
  Spec/harness: [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) (MI-01..16).

### Progresso — 2026-06-28 (desktop em PostgreSQL real + ícones nos modais + grelha PHC)

- **Base de dados real no desktop:** o perfil `desktop` deixou de usar H2 em memória e passou a usar
  **PostgreSQL local persistente** (BD `multicore`, role dedicada `multicore`). Credenciais fora do git:
  password lida de `${DB_PASSWORD}` (variável de ambiente persistente da máquina).
  [application-desktop.properties](../src/main/resources/application-desktop.properties) com
  **Flyway dono do schema + Hibernate `validate`** (igual a prod).
- **`V17__sync_schema_with_entities.sql`:** fecha o desvio acumulado em dev (que corria `ddl-auto=update`,
  por isso as migrações estavam atrasadas face às entidades). Gerada a partir do diff do Hibernate:
  colunas em falta (`stock_transfers.approved_at/approved_by/rejection_reason`), precisão numérica
  `numeric(38,2)` em `purchase_orders`/`purchase_order_lines`, e 7 `UNIQUE` declaradas nas entidades
  (employees, payslips, payroll_bonuses, product_batches, stocks, tax_rates). **Reposto o caminho
  prod (Flyway+validate), que antes falhava.** Spec/harness:
  [docs/BD_POSTGRES_DESKTOP_SPEC.md](../docs/BD_POSTGRES_DESKTOP_SPEC.md) +
  [docs/BD_POSTGRES_DESKTOP_HARNESS.md](../docs/BD_POSTGRES_DESKTOP_HARNESS.md) (DB-01..06).
- **Ícones nos modais de formulário:** `ModernFormDialog` ganhou ícone contextual no título (deduzido do
  título via `iconForTitle`, domínio>verbo) + `setIconImage`, e `fas-times` no Cancelar. Cobre todos os
  `ModernFormDialog` sem tocar nos call sites. Vocabulário `phc-icons` += Fornecedor/Categoria. Spec/harness:
  [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) +
  [docs/MODAIS_ICONES_HARNESS.md](../docs/MODAIS_ICONES_HARNESS.md) (MI-01..08).
- **Tabelas em grelha estilo PHC:** `UIHelper.styleTable` passou a desenhar linhas verticais + horizontais
  (grelha completa, `setIntercellSpacing(1,1)`, contorno na cor da grelha).
- **Pendente (legado):** modais baseados em `JOptionPane.showConfirmDialog` (Cadastrar Produto, Armazém,
  Ajuste) ainda sem iconografia própria — migrar para `ModernFormDialog` numa próxima iteração.

### Progresso — 2026-06-27 (IVA dinâmico no POS + altura uniforme botões/inputs)

- **IVA dinâmico por produto** (decisão do utilizador): novo FK `Product.taxRate → TaxRate`
  (entidade fiscal configurável já existente), migration `V16`. A taxa efetiva = taxa do produto,
  ou a padrão (`TaxRates.STANDARD_VAT` 16%) quando não definida. Cálculo continua na engine única
  `LineCalculator`. **Deixou de aplicar a constante hardcoded** no checkout do POS.
  - `ProductDTO` passou a expor `taxRateId/taxRate/taxRateLabel`; `createProduct` aceita `taxRateId`;
    formulário de produto (StockPanel) ganhou seletor **Taxa de IVA** (default 16%).
  - **POS UI:** carrinho com colunas **Líquido · IVA · Total** (IVA = "Isento" a 0% ou "valor (taxa%)"),
    rodapé **Subtotal s/ IVA · IVA** e **TOTAL A PAGAR = líquido + IVA** (pagamento/troco usam este total).
  - **Seed:** cesta básica isenta (arroz/açúcar/farinha/feijão), massa 5%, óleo 16% — demonstra taxas
    mistas numa só venda.
  - Spec/harness: [docs/POS_IVA_DINAMICO_SPEC.md](../docs/POS_IVA_DINAMICO_SPEC.md) +
    [docs/POS_IVA_DINAMICO_HARNESS.md](../docs/POS_IVA_DINAMICO_HARNESS.md) (IV-01..09). Testes:
    `POSServiceTest` +2 (isento / 16%). **Verificado visualmente** (Açúcar isento + Óleo 16% + Massa 5%).
- **Altura uniforme botões/inputs:** `ModernButton.getPreferredSize()` impõe altura mínima
  `FORM_CONTROL_HEIGHT` (38px), igual aos campos, mantendo a largura natural (com ícone, sem truncar).
- **Modais contidos na janela principal (mesmo ao arrastar):** `MainFrame.registerMainWindow(this)`
  regista a janela e instala **um listener global** (`AWTEventListener` `COMPONENT_MOVED`) que prende
  qualquer `Dialog` dentro da janela principal — não sai para fora nem ao arrastar (`clampInsideMain`).
  Ao abrir, `containWithinMain` limita a ~94% e centra; `ModernFormDialog` e `makeDialogScrollable`
  dimensionam-se pela janela principal (não pelo ecrã). Cobre os ~333 pontos de diálogo sem os tocar.
  Spec/harness: [docs/MODAIS_CONTIDOS_SPEC.md](../docs/MODAIS_CONTIDOS_SPEC.md) +
  [docs/MODAIS_CONTIDOS_HARNESS.md](../docs/MODAIS_CONTIDOS_HARNESS.md) (MD-01..06, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**. Verificado visualmente
  (carrinho com IVA, botões alinhados, modal "Cadastrar Produto" centrado e contido).

### Progresso — 2026-06-27 (polish profissional do ecrã POS)

- **`POSPanel` repolido** (só apresentação, sem mexer em Service/DTO/cálculos):
  - Formulário esquerdo **reposto ao topo** em `onPanelSelected()` → secção DOCUMENTO (Cliente/
    Armazém/Conta) deixa de aparecer cortada.
  - **Emojis 🔍 removidos** dos campos de pesquisa (não renderizavam sob Metal/Ocean); pista agora é
    **ícone vectorial** `fas-search` à esquerda (helper `searchRow`), padrão da barra de código de barras.
  - **Total em destaque**: faixa `ModernPanel` "TOTAL A PAGAR" + valor a 26px (`%,.2f MT`).
  - **Empty state do carrinho** via `CardLayout` (ícone + "Carrinho vazio" + dica), alternado em
    `updateCartTotal`/`refreshCartView`.
  - **Estado da caixa com ícone** (cadeado aberto/verde vs fechado/amarelo).
- Spec/harness: [docs/POS_UI_POLISH_SPEC.md](../docs/POS_UI_POLISH_SPEC.md) +
  [docs/POS_UI_POLISH_HARNESS.md](../docs/POS_UI_POLISH_HARNESS.md) (PU-01..08, manual).
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (formulários em modal responsivo)

- **`ModernFormDialog`** passou a ser o modal canónico: **scroll automático** do conteúdo,
  **responsivo** (≤92%×88% do ecrã, centrado), botão Gravar com ícone `fas-save` (removido o emoji).
- **Formulários de criação convertidos em modais**, deixando as tabelas a ecrã inteiro: Faturação
  (FT) «Nova Fatura…», Compras «Registar Compra…», Encomendas a Fornecedor «Nova Encomenda…».
  Cada submit lança em erro (modal fica aberto) e recarrega a lista em sucesso.
- Spec/harness: [docs/FORMULARIOS_MODAIS_SPEC.md](../docs/FORMULARIOS_MODAIS_SPEC.md) +
  [docs/FORMULARIOS_MODAIS_HARNESS.md](../docs/FORMULARIOS_MODAIS_HARNESS.md) (FM-01..08, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (contas a pagar a fornecedor — Fase 4)

- **`Purchase.amountPaid`** (em dívida = total − pago), migration `V15` (backfill: compras antigas
  ficam pagas). **Compra a crédito**: `createPurchase` com `financeAccountId = null` não paga no acto.
- `findPayablesByCompany` (saldo > 0) + `registerSupplierPayment` (abate, cap no saldo, saída de
  tesouraria CREDIT, auditoria). API: `GET /api/purchases/payables`, `POST /api/purchases/{id}/pay`.
- **UI:** opção "— A crédito —" no combo de conta da compra + tab **Contas a Pagar** (lista com
  total em dívida + Registar Pagamento). Testes: `PurchaseServiceTest` AP-03..06.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas**.

### Progresso — 2026-06-25 (compras & aprovisionamento profissional)

- **Gestão de fornecedores completa:** campos novos (telefone, contacto, **activo**), editar,
  activar/desactivar (soft-delete, MANAGER/ADMIN + auditado), pesquisar por nome/NUIT. Fornecedor
  inactivo bloqueado em compra/encomenda. Migration `V13`.
- **Encomenda de Fornecedor (`PurchaseOrder`, série `EC-F`):** novo workflow `ORDERED → RECEIVED /
  CANCELLED` (mirror da encomenda de cliente). **Não move stock até à recepção**; a recepção gera
  entrada `PURCHASE` por linha (FEFO/lote, bloqueio de lote vencido), MANAGER/ADMIN + auditado.
  Migration `V14`. Endpoints sob `/api/purchases/orders`.
- **UI (`ComprasPanel`):** tab «Encomendas a Fornecedor» (form + linhas + lista com Receber/Cancelar/
  pesquisa) e tab de fornecedores com editar/pesquisar/activar. **Categorias** ganharam ecrã de gestão
  (nova tab no `StockPanel` sobre o `ProductCategoryService`).
- Spec/harness: [docs/COMPRAS_APROVISIONAMENTO_SPEC.md](../docs/COMPRAS_APROVISIONAMENTO_SPEC.md) +
  [docs/COMPRAS_APROVISIONAMENTO_HARNESS.md](../docs/COMPRAS_APROVISIONAMENTO_HARNESS.md).
  Testes: `PurchaseServiceTest` (5) + `PurchaseOrderServiceTest` (8).
- **Fase 4 (futuro):** contas a pagar a fornecedor (saldo + pagamento → tesouraria) e recepção parcial.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 151 testes, 0 falhas**.

### Progresso — 2026-06-25 (UI faturação: tabela de linhas em largura total)

- Tab **Faturação (FT)** reorganizada com **split vertical**: formulário + faturas recentes em cima,
  **Linhas da Fatura em largura total** em baixo (divisor 50/50 aplicado no 1.º resize real — corrige
  o `setDividerLocation` que não pegava antes do componente ter altura).

### Progresso — 2026-06-25 (vista unificada de movimentos comerciais)

- **Dívida §7.3 fechada:** novo módulo de leitura agregada `modules/movimentos/` (DTO/enum/service/
  controller, sem entidade própria — reutiliza repositórios de `comercial`, padrão do `ReportService`).
  `MovimentosService.listar(companyId, query, from, to)` junta **fatura, encomenda, NC e ND** numa só
  lista, filtrável por **nº/cliente** (substring case-insensitive) e **período** (inclusivo), ordenada
  por **data desc**, com guarda multi-tenant (`requireCompany`).
- **UI:** nova tab "Movimentos" no `ComercialPanel` com filtros (pesquisar/de/até) e rodapé
  contagem+soma. **API:** `GET /api/movimentos`.
- Spec/harness: [docs/MOVIMENTOS_UNIFICADOS_SPEC.md](../docs/MOVIMENTOS_UNIFICADOS_SPEC.md) +
  [docs/MOVIMENTOS_UNIFICADOS_HARNESS.md](../docs/MOVIMENTOS_UNIFICADOS_HARNESS.md). Testes:
  `MovimentosServiceTest` (7: MU-01..MU-07).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 138 testes, 0 falhas**.

### Progresso — 2026-06-25 (recibo pontilhado + loading reutilizável no checkout/PDF)

- **Recibo térmico pontilhado:** separadores passaram de traços a **linhas pontilhadas reais**
  (`DottedLineSeparator`) e as linhas da tabela ganharam **borda inferior pontilhada** (evento de
  célula com `setLineDash`) — aspecto de recibo térmico. `ReceiptPrintService`.
- **Loading profissional reutilizável:** novo `UIHelper.runWithProgress(...)` corre tarefas
  demoradas num `SwingWorker` com diálogo modal "a processar…" + barra indeterminada. Aplicado ao
  **checkout do POS** ("A finalizar venda…") e à **geração/reimpressão de recibos** ("A gerar
  recibo…"). Padrão pronto para outros PDFs/cargas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (promoções de loja + login com loading + maximizar)

- **Módulo `promotions` (sugestão #3):** novo domínio completo (model/enum/repo/dto/service/controller)
  seguindo a arquitetura. Tipos: **percentagem** (produto ou categoria) e **leve X, pague Y** (produto).
  - `PromotionService.bestPromotion(...)` traduz a promoção activa no **desconto % efectivo** por
    linha — reutiliza o checkout existente sem mexer no cálculo do POS/faturação.
  - **POS:** ao adicionar artigo sem desconto manual, aplica automaticamente a melhor promoção e
    marca "Promo: <nome>" na linha do carrinho.
  - **UI:** nova tab "Promoções" no `ComercialPanel` (`PromotionsPanel`) — listar, criar, activar/
    desactivar (permissão MANAGER/ADMIN, auditado).
  - Migration `V12__store_promotions.sql`. Testes: `PromotionServiceTest` (8).
- **Login com loading profissional:** autenticação passou a correr em `SwingWorker` (não congela o
  EDT) com barra de progresso indeterminada reutilizável (`UIHelper.createBusyBar`) e estado
  "A entrar…". Campos/botão bloqueados durante a chamada.
- **Arranque maximizado** após login (`DesktopLauncher`), com estado preservado na troca de tema.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (alerta de validade proativo + UI tema/POS)

- **Alerta de validade (sugestão #4 p/ loja):** lotes com stock vencidos ou a vencer em ≤30 dias
  passam a ser **proativos**, não só consultáveis. `InventoryService.findExpiringBatches(companyId,
  daysAhead)` (com guarda de empresa) delega em `ProductBatchService.findExpiringByCompany`.
  - **Dashboard:** novo cartão "ALERTAS DE VALIDADE" (visível no login) com total + repartição
    "X vencidos · Y a vencer (≤30d)". Grelha de KPIs passou a 3 colunas/linhas automáticas.
  - **Stock › Lotes & Validades:** resumo proativo no topo (vermelho se há vencidos) e **cor de
    urgência** na coluna Estado (VENCIDO vermelho, VENCE EM BREVE amarelo) via `UIHelper.styleTable`.
  - Testes: `InventoryServiceTest` (2: corte hoje+dias / guarda de empresa).
- **UI (sessão anterior):** barra de topo passou a acompanhar o tema claro/escuro; POS com
  formulário/carrinho em `JSplitPane` redimensionável; dropdown dos combos legível em tema claro;
  botões dos diálogos legíveis (gradiente Metal achatado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 123 testes, 0 falhas**.

### Progresso — 2026-06-20 (dívida técnica + cobertura de testes)

- **Nota de Débito** alinhada com `DocumentSeries`: nova série `ND`, número sequencial gapless via
  `DocumentNumberService.next(...)` em vez de `"ND-" + timestamp`. (resolve dívida §7.2 de MOVIMENTOS_COMERCIAIS.md)
- **Cobertura de testes** dos Services críticos que faltavam ao harness:
  - `POSServiceTest` (10): checkout sem sessão, via legada vs multi-método, fiado parcial,
    numerário+cartão sem dupla contagem, fecho de caixa (sem diferença / diferença exige permissão / depósito).
  - `CreditNoteServiceTest` (8): RETURN repõe stock só na aprovação, limites de quantidade/valor, permissão.
  - `DocumentNumberServiceTest` (6): sequência gapless, séries independentes, corrida na criação, série ND.
- **BackupService confirmado**: faz export real (dump JSON de todas as coleções por empresa) — não é
  placeholder. Só não tem restore programático (por design; restore é ao nível de BD em ambiente separado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 71 testes, 0 falhas**.

### Progresso — 2026-06-20 (faturação directa + lote vencido + mais testes)

- **Faturação directa (decisão do utilizador):** `ComercialService.createInvoice` deixou de passar
  sempre pela Engine de Aprovações. Agora exige perfil **MANAGER/ADMIN**, emite a fatura já **APPROVED**
  e baixa stock no acto. **Só desconto >10%** mantém o caminho `PENDING_DISCOUNT_APPROVAL` → aprovação
  do gerente (stock baixa na aprovação via callback). Sem dupla baixa de stock.
- **Lote vencido (RS-12 / spec §4):** `ProductBatchService.addToBatch` bloqueia entrada de stock com
  validade já no passado (validade = hoje ainda entra; sem validade não bloqueia). Guarda todas as
  entradas porque compra/ENTRY passam por `addToBatch`.
- **Novos testes:** `ComercialServiceTest` (8) e 3 cenários de lote vencido em `ProductBatchServiceTest`.
  `MulticoreServicesTest.testDiscountApprovalThreshold` actualizado para o novo fluxo (5% → APPROVED).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 82 testes, 0 falhas**.

### Progresso — 2026-06-21 (segurança/roles por API)

- **Confirmado:** o `SecurityInterceptor` já impõe token+empresa em todo o `/api/**` (excepto
  `/api/auth/login`): 401 sem token, 403 sem acesso à empresa, e resolve o role por empresa. A spec §9
  está satisfeita ao nível do interceptor — o filtro Spring permissivo **não** é uma falha real.
- **Novo teste:** `SecurityApiIntegrationTest` (4) valida ponta-a-ponta pela API: 401 sem token,
  403 empresa sem acesso, e o role gate ao faturar (EMPLOYEE bloqueado / ADMIN passa). Fecha o item
  "login, tenant e roles testados por API" do harness.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 86 testes, 0 falhas**.

### Progresso — 2026-06-21 (localizar documento de origem por pesquisa, estilo PHC)

- **Faturar a partir de encomenda** e **NC/ND a partir da fatura** passaram a permitir **pesquisar o
  documento de origem por nº ou cliente** (BUSINESS_FLOWS passo 1 "documento origem é localizado").
- Backend: `ComercialService.searchInvoices(query)` e `searchPendingOrders(query)` (filtro substring
  case-insensitive por nº/cliente, empresa activa) + helper `matches()`. Lógica no Service, UI fina.
- UI: campo "Pesquisar (nº ou cliente)" nos 3 diálogos do `ComercialPanel` (Faturar Encomenda, Emitir
  NC, Emitir ND) que filtra a lista ao escrever. Novo helper reutilizável `UIHelper.onTextChange(...)`.
  Faturação manual directa e por encomenda **ambas mantidas**.
- Testes: `ComercialServiceTest` (5 novos) para as pesquisas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 91 testes, 0 falhas**.

### Progresso — 2026-06-21 (bug: encomenda não chegava à aprovação)

- **Causa:** `ComercialService.createOrder` guardava a encomenda como `PENDING` mas **nunca a
  submetia** à Engine de Aprovações — só faturas (desconto >10%) e despesas lá chegavam.
- **Fix:** a encomenda nasce `PENDING_APPROVAL` e é submetida via `approvalService.submitRequest("ORDER", …)`.
  Novo `OrderApprovalCallback`: aprovado → `PENDING` (faturável); rejeitado → `CANCELLED`. `billOrder`
  já exige `PENDING`, logo só fatura encomendas aprovadas (aprovar → depois faturar).
- UI: mensagem de criação passa a indicar "Submetida para aprovação"; a área de aprovação mostra o
  tipo em PT ("Encomenda"/"Fatura") via `humanType(...)`.
- Testes: `OrderApprovalCallbackTest` (4) + `createOrder` submete aprovação (`ComercialServiceTest`);
  `MulticoreServicesTest` actualizado (aprovar a encomenda antes de faturar).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 96 testes, 0 falhas**.

### Progresso — 2026-06-21 (cancelar encomenda)

- `ComercialService.cancelOrder(id, reason)` (mirror de `cancelInvoice`): exige MANAGER/ADMIN e motivo,
  bloqueia encomenda já faturada/cancelada, fecha o pedido de aprovação aberto e audita (`ORDER_CANCEL`).
- `ComercialService.searchCancellableOrders(query)` + `ApprovalService.cancelPendingForDocument(...)`.
- UI: botão "Cancelar Encomenda…" na tab Encomendas → diálogo com pesquisa por nº/cliente + motivo.
- Testes: `ComercialServiceTest` (cancelOrder + searchCancellableOrders) e `ApprovalServiceTest`.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 103 testes, 0 falhas**.

### Progresso — 2026-06-21 (recibo POS: nome + valor pago/troco; UI)

- **Recibo térmico POS** (spec §4 "troco para numerário", RS-05): o nome do comprador passa a ser
  gravado na fatura (`Invoice.customerName`, migration `V9`) e impresso; o `ReceiptPrintService` mostra
  bloco de pagamento por método + **Valor pago** e **Troco**. UI: diálogo de pagamento no checkout
  (`POSPanel.askPayment`) recolhe método e, em numerário, **valor entregue com troco em tempo real**;
  passa a usar a via multi-pagamento (`PaymentEntry` com tendered/change). Bónus: cartão/transferência
  já vão à tesouraria sem contar como numerário na gaveta. Testes: `POSServiceTest` (+2 → 105).
- **Marca com empresa activa:** topo mantém MULTICORE e mostra o nome da empresa por baixo; role
  traduzido para PT discreto (`UIHelper.humanRole`: Administrador/Gestor/Funcionário).
- **Bug do seletor de empresa:** `styleComboBox` substituía o renderer e mostrava `CompanyAccess[...]`.
  Corrigido reaplicando o renderer depois de `styleComboBox` (`MainFrame.applyCompanyRenderer`).
- **Navegação no topo (decisão do utilizador):** sidebar esquerda → **barra de topo só-ícones**
  (`TopNavBar` + `TopNavItem`, ícones via `UIHelper.icon`, tooltip por módulo, quebra para 2ª linha
  via `WrapLayout`), libertando largura para tabelas/formulários. `CollapsibleSidebar`/`SidebarNavItem`
  ficaram sem uso (não removidos).
- **Scroll no formulário de compra:** "Registar Compra (Entrada Stock)" ganhou scroll vertical via
  host `Scrollable` (acompanha largura, não altura) dentro de `JScrollPane` no `ComprasPanel`.
- **Cadastro de cliente fora das Faturas:** removida a tab "Registar Cliente" do `ComercialPanel`
  (e código sem uso: `createRegistarClienteTab`/`registerClient`/4 campos). Gestão de clientes vive
  só na área Clientes (`ClientesPanel`); o diálogo rápido "+ Novo" do POS mantém-se.
- **Nota de arranque desktop:** o `-Dspring-boot.run.main-class` não sobrepõe o `<mainClass>` literal do
  pom; README actualizado para arrancar o desktop via `java -cp`.
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **105 testes, 0 falhas**.

---

## Foco em curso

Fechar lacunas para uso real em loja/mercearia:

- Spec: [docs/RETAIL_STORE_SPEC.md](../docs/RETAIL_STORE_SPEC.md)
- Harness: [docs/RETAIL_STORE_HARNESS.md](../docs/RETAIL_STORE_HARNESS.md)
- Task faseada: [tasks/retail_store_readiness.md](retail_store_readiness.md)

Prioridade imediata: executar o harness RS-01 a RS-22 com dados reais de loja, validar impressora/leitor/gaveta e testar restore num ambiente separado.

### Progresso — 2026-06-22 (colunas de linha dos documentos comerciais)

- **Fatura, encomenda e nota de crédito** passaram a mostrar, por linha: **código de barras, referência,
  descrição, validade (do lote), qtd, preço unit., IVA e subtotal líquido** — via o renderizador
  partilhado `LineItemsTableRenderer` (8 colunas canónicas; subtotal = `LineCalculator.net`).
- Novo `LineRowMapper` (DRY) resolve barcode/ref/descrição do `Product` e a validade via novo
  `ProductBatchRepository.findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc`. Os 3 serviços
  de impressão deixaram de montar linhas à mão.
- Spec/harness: [docs/DOCUMENT_LINE_COLUMNS_SPEC.md](../docs/DOCUMENT_LINE_COLUMNS_SPEC.md) +
  [docs/DOCUMENT_LINE_COLUMNS_HARNESS.md](../docs/DOCUMENT_LINE_COLUMNS_HARNESS.md). ND é baseada em
  valor (sem linhas de artigo).
- **Nova Guia de Remessa**: `GuideRemittancePrintService.render(invoiceId)` gera a guia a partir da
  fatura (mesmas 8 colunas + bloco de transporte/assinaturas, ref. `GR-<nºfatura>` sem consumir
  numeração). Endpoint `GET /api/print/guide/{invoiceId}` + botão "Imprimir Guia" no `ComercialPanel`.
- Teste: `LineItemsTableRendererTest` (3). Verificação: `mvn clean test` → **121 testes, 0 falhas**.

### Backlog — RH/Folha (avaliação 2026-06-22)

Módulo `com.phcpro.modules.hr` está avançado (motor fiscal IRPS/INSS, recibos, despesas, férias,
faltas, UI + PDF) mas **não pronto para produção**. Spec-alvo e harness criados:
- Spec: [docs/HR_PAYROLL_SPEC.md](../docs/HR_PAYROLL_SPEC.md)
- Harness: [docs/HR_PAYROLL_HARNESS.md](../docs/HR_PAYROLL_HARNESS.md) (cenários RH-01..RH-25 + punch list)

Lacunas prioritárias: (1) auditoria ausente no RH; (2) nº de recibo por timestamp em vez de
`DocumentNumberService` gapless; (3) marcar recibo pago não gera saída de tesouraria; (4) mapa
fiscal e config de impostos sem endpoint/PDF; (5) férias sem saldo e `decideVacation` confia no
`decidedBy` do body; (6) faltas não descontam no recibo; (7) 13.º mês e subsídio de férias em falta;
(8) só 6 testes para um módulo de dinheiro+impostos (alvo ~30+).

**Progresso RH — 2026-06-22 (itens 1–3 da punch list):**
- (1) **Auditoria** em todo o RH: `AuditLogService` injectado em `HRService`, audita
  `EMPLOYEE_CREATE/UPDATE/STATUS`, `PAYSLIP_ISSUE/PAID/CANCEL`, `PAYROLL_PROCESS`, `VACATION_DECISION`.
- (2) **Nº de recibo gapless**: série `REC` via `DocumentNumberService.next` (fim do timestamp).
- (3) **Líquido → tesouraria** em `markPayslipPaid` via novo `FinanceService.registerAutoPayout`
  (refactor DRY de `registerAutoExpensePayout`).
- (4) **Mapa fiscal + config de impostos via API/PDF**: `HRController` expõe
  `GET /payroll/fiscal-summary/{year}/{month}` e `GET/POST /payroll/tax-config`; novo
  `PayrollFiscalMapPrintService` (PDF mapa INSS/IRPS) + botão "Imprimir Mapa Fiscal" no `FiscalPanel`;
  novo `HRApiIntegrationTest` (4: 401/403/200).
- (5) **Férias: saldo + decisor seguro**: direito anual 22 dias, saldo = direito − reservados
  (`VacationRepository.sumReservedDays`), `submitVacation` bloqueia acima do saldo; `decideVacation`
  exige MANAGER/ADMIN, resolve decisor por `CurrentUserContext` (deixou de confiar no `decidedBy` do
  body) e exige motivo na rejeição. Testes: `HRServiceTest` (+2).
- (6) **Faltas não remuneradas descontam no recibo**: novo campo `Payslip.absenceDeduction`
  (migration `V10`); `createPayslip` desconta faltas `UNJUSTIFIED` que se sobrepõem ao mês
  (valor/dia = salário base / 30), reflectido no líquido, `PayslipDTO` e PDF. Teste: `HRServiceTest` (+1).
- (7) **13.º mês + subsídio de férias (cálculo + pagamento)**: novo `PayrollBonusService` calcula
  (`thirteenthMonth`/`vacationAllowance`) e **paga de forma persistida e idempotente** via nova
  entidade `PayrollBonus` (migration `V11`, unique `employee+type+year+reference`): `payThirteenthMonth`
  /`payVacationAllowance` exigem MANAGER/ADMIN, saída de tesouraria + auditoria. Endpoints GET (calcular)
  e POST `/pay`. `PayrollBonusServiceTest` (6).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 118 testes, 0 falhas**.
- **RH: punch list 1–7 fechada.** Cobertura RH = 19 testes próprios. Item 8 (cobertura) substancialmente
  cumprido. Falta apenas (opcional) UI desktop para 13.º/subsídio de férias — backend completo.

### Progresso da Fase 1 — 2026-06-17

- Criado `PermissionGuard` central para exigir `MANAGER/ADMIN` ou `ADMIN`.
- Aplicada permissão em sangria, fecho de caixa com diferença, anulação de factura/recibo, aprovação/rejeição/cancelamento de notas e aprovação/rejeição de transferências.
- Auditados fecho de caixa, sangria/suprimento, anulações, notas e transferências.
- Verificado que o desktop propaga utilizador/role/empresa para `CurrentUserContext` e que o client HTTP envia `X-Company-Id`.
- Verificação técnica: `mvn -q -DskipTests compile` e `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest" test` passaram.

### Progresso das Fases 2-5 — 2026-06-17

- POS: criado `POST /api/pos/returns` para devolução por nota de crédito `RETURN`, com reembolso CASH/CARD/BANK_TRANSFER/CREDIT.
- Produtos: adicionados `ProductSaleType` e `stockTracked`; linhas de factura/encomenda/POS migradas para `BigDecimal`; POS Swing aceita quantidade decimal.
- Stock: criado `POST /api/inventory/adjustments` para contagem/ajuste com motivo, permissão e auditoria.
- Relatórios: criado `GET /api/reports/daily-store` com vendas do dia, fiado em aberto, pagamentos por método, movimentos de caixa e top produtos.
- Migration: criada `V8__retail_product_sale_type_and_decimal_quantities.sql`.
- Verificação técnica: `mvn -q -DskipTests compile` passou; `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest,MulticoreServicesTest" test` passou.

### Progresso final de prontidão loja/mercearia — 2026-06-17

- POS Swing: devolução/troca operacional ligada ao histórico de vendas, com motivo, quantidade devolvida, método de reembolso e armazém.
- Stock Swing: ajuste passou a ser contagem física com quantidade contada e motivo, usando o Service auditado.
- Relatórios: `GET /api/reports/daily-store` passou a incluir vendas por operador e margem bruta por produto.
- Backup: criado verificador não destrutivo de backup JSON, botão "Verificar Backup" no painel de Configurações e teste unitário.
- Verificação técnica final: `mvn -q clean compile` passou; `mvn -q test` passou.

### Ainda pendente para declarar loja real pronta em ambiente físico

- Teste manual do harness RS-01 a RS-22 em ambiente real.
- Restore de backup testado em ambiente separado.
- Validação de impressora, leitor, gaveta e decisão de balança/etiquetas.

## Feito nas últimas iterações

### Funcionalidade — Validades & FEFO
- Backend: `ProductBatchService.findNextFEFO(productId, warehouseId)` exposto via `InventoryService`.
- UI:
  - [StockPanel](src/main/java/com/phcpro/gui/StockPanel.java) — botão "Adicionar Lote/Validade" + diálogo dedicado; chain após "Cadastrar Produto".
  - [POSPanel](src/main/java/com/phcpro/gui/POSPanel.java) — campos Lote+Validade FEFO read-only auto-preenchidos.
  - [ComercialPanel](src/main/java/com/phcpro/gui/ComercialPanel.java) — Faturas e Encomendas com Lote/Validade FEFO read-only.
  - Diálogo de transferência ganhou colunas Lote+Validade FEFO recalculadas.

### Documentação (spec-driven harness)
- `README.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `CLAUDE.md` criados.
- `ARCHITECTURAL_GUIDELINES.md` + `ARCHITECTURE_SEPARATION.md` consolidados e removidos.

### Infra "production-ready"
- **Lombok**: [lombok.config](lombok.config) — `stopBubbling`, marca métodos gerados, proíbe `@Data`/`@AllArgsConstructor`/`@Builder`. Setup IDE documentado em [CONVENTIONS.md §3](CONVENTIONS.md#3-lombok).
- **Handler global**: confirmado [GlobalExceptionHandler](src/main/java/com/phcpro/architecture/exception/GlobalExceptionHandler.java) já existia (BusinessRule → 400, Validation → 400 com mapa, fallback → 500).
- **Flyway + PostgreSQL**: dependências em [pom.xml](pom.xml); [application-prod.properties](src/main/resources/application-prod.properties) com `ddl-auto=validate`, Flyway ON, vars `DB_URL/DB_USER/DB_PASSWORD`. Pasta [db/migration/](src/main/resources/db/migration/) com README explicando como gerar `V1__init.sql` a partir das entidades JPA.
- **Spring Security scaffold**: [SecurityConfig](src/main/java/com/phcpro/architecture/security/SecurityConfig.java) — BCryptPasswordEncoder + filter chain permissiva (não quebra desktop). [AppUserService](src/main/java/com/phcpro/modules/users/service/AppUserService.java) migrado para BCrypt com fallback de migração suave (passwords em texto-plano legadas continuam a autenticar e são re-encriptadas na próxima autenticação).
- **OpenAPI / Swagger**: dependência `springdoc-openapi-starter-webmvc-ui` adicionada. Em dev: `http://localhost:8080/swagger-ui.html`. Desactivado em prod até haver autenticação para a UI.
- **CI**: [.github/workflows/build.yml](.github/workflows/build.yml) — `mvn clean compile`, `mvn test`, `mvn package` em cada push/PR para `main`.
- **Testes unitários**: [ProductBatchServiceTest](src/test/java/com/phcpro/modules/inventory/service/ProductBatchServiceTest.java) — 9 testes com Mockito a cobrir `findNextFEFO`, `consumeFEFO` (single batch / multi-batch / stock insuficiente / qty inválida) e `addToBatch` (novo / acumular / qty inválida). **9/9 verde.**

## Por validar manualmente (não posso fazer como agente)

- [ ] Cadastrar produto novo → confirmar prompt "Adicionar stock inicial".
- [ ] Adicionar 2 lotes do mesmo produto com validades diferentes → confirmar FEFO escolhe o mais próximo.
- [ ] POS: vender produto → confirmar Lote/Validade FEFO mostra o lote correcto e o movimento consome-o.
- [ ] Encomenda + Fatura: confirmar Lote/Validade FEFO refresca ao mudar armazém.
- [ ] Transferência multi-linha: confirmar colunas FEFO actualizam ao trocar armazém de origem.
- [ ] Autenticar com utilizador antigo (password em texto-plano) → confirmar que entra E que o hash na BD passou a `$2a$...`.
- [ ] Abrir `http://localhost:8080/swagger-ui.html` em dev → confirmar que lista todos os endpoints.

## Por fazer antes de produção real

1. ~~**Gerar V1__init.sql**~~ — já existe baseline `V1__init.sql` + V2..V8 em [db/migration/](src/main/resources/db/migration/). Falta aplicar/validar em PostgreSQL limpo (checklist do harness).
2. ~~**Restringir Security**~~ — **confirmado (2026-06-21)**: o `SecurityInterceptor` já impõe token+empresa+role em todo o `/api/**` (401/403), validado por `SecurityApiIntegrationTest`. O `.anyRequest().permitAll()` do filtro Spring é redundante (a guarda é o interceptor), não uma falha. Endurecer o filtro Spring fica como hardening opcional, não bloqueante.
3. ~~**Endpoints `/api/auth/login`**~~ — existe e está coberto (`AuthControllerIntegrationTest`).
4. ~~**Cobertura de testes** dos Services críticos~~ — **feito (2026-06-20)** para `POSService.checkout/closeSession`, `CreditNoteService`, `DocumentNumberService`. Falta `ComercialService.issueInvoice` e o cenário lote vencido (RS-12).
5. ~~**Backups reais**~~ — confirmado: `BackupService.executeBackup()` faz export real. Restore real continua a exigir ambiente separado (ponto manual do harness).

## Decisões tomadas

- Validades **não** têm tabela autónoma — pertencem sempre a um lote (`ProductBatch`).
- Lote/Validade no UI **read-only** — FEFO decide. Backend volta a aplicar FEFO em transacção mesmo que o UI passe `batchNumber`.
- Spring Security é scaffold **permissivo** por agora — restringir endpoints só quando houver login HTTP real, para não quebrar o desktop que ainda chama Services directamente.
- Backend puro de dev (`application.properties`, `mvn spring-boot:run`) continua em **H2 + `ddl-auto=update`**.
  O **desktop** (perfil `desktop`) usa **PostgreSQL local + Flyway + `validate`**, igual a prod (desde 2026-06-28).
  Prod com PostgreSQL gerido externamente + Flyway + `validate`.

## Estado de build

```
mvn clean compile   → BUILD SUCCESS
mvn clean test      → BUILD SUCCESS, 193 testes, 0 falhas (2026-07-01)
```

Diagnostics Lombok no IDE (`cannot find symbol: getX()`) são **ruído**. Critério único: `mvn compile`.

### Consistência profissional da UI Swing — 2026-08-09

- Criadas a especificação `docs/UI_CONSISTENCIA_PROFISSIONAL_SPEC.md` e o harness
  `docs/UI_CONSISTENCIA_PROFISSIONAL_HARNESS.md`.
- Uniformizados inputs tipados, selects, botões, tabelas, estados vazios/loading, acessibilidade e
  submissões assíncronas com protecção contra duplo clique e respostas de empresa obsoletas.
- Removidas chamadas remotas síncronas identificadas nos fluxos prioritários de POS, Stock,
  Comercial, Compras, RH, CRM, Financeiro e Configuração.
- Decompostos os seis painéis prioritários, todos agora abaixo de 1.000 linhas; o limite está
  protegido por `UiPanelDecompositionTest`.
- `mvn dependency:analyze` revisto: starters Spring Boot e drivers runtime reportados como unused
  são necessários por boot/autoconfiguração; nenhuma dependência declarada pôde ser removida com
  segurança. A redução efectuada foi de acoplamento interno da UI.
- Verificação: harness focado verde; `mvn clean test` verde com **391 testes, 0 falhas, 0 erros e
  0 ignorados**.
- Pendente apenas a evidência manual UI-50..62 em Windows real (escalas, temas, API lenta e
  periféricos POS), conforme o harness; não é substituída por testes headless.

### Layout responsivo do POS — 2026-08-11

- Catálogo/carrinho passam a iniciar em 36/64, com mínimos operacionais de 380/650 px.
- A tabela preserva as larguras das oito colunas com scroll horizontal abaixo de 900 px e volta a
  preencher o viewport quando existe largura confortável.
- Totais e acções de checkout permanecem fixos; apenas as linhas da tabela fazem scroll.
- Spec e harness: `docs/POS_LAYOUT_RESPONSIVO_SPEC.md` e `docs/POS_LAYOUT_RESPONSIVO_HARNESS.md`.
# Carrinho operacional do POS

- O carrinho foi compactado para seis colunas essenciais, eliminando a rolagem horizontal no
  viewport operacional de 620 px.
- Produtos adicionados ou incrementados ficam seleccionados e visíveis automaticamente.
- Nova barra de quantidade oferece diminuir, editar (F6) e aumentar, mantendo totais e checkout fixos.
- Promoção, lote e série permanecem acessíveis no tooltip da linha.
- Correcção visual: a tabela permanece auto-ajustável abaixo de 620 px e reserva altura para pelo
  menos três linhas; "Mais opções" abre um diálogo sem comprimir o workspace.
- Correcção de altura: pesquisa/selecção de cliente ficam na mesma linha; subtotal, IVA e total
  foram unidos numa faixa; Fiado passou para a linha de acções, libertando o corpo da tabela.
- Ritmo vertical compactado no POS: margem externa 14 px, secções 6 px e cartão 8 px; inputs sobem
  para junto das acções de caixa e o espaço recuperado aumenta o viewport do carrinho.
- Cabeçalho POS final em linha única: Pesquisa, Cliente, Armazém, Conta e Código de barras usam
  larguras responsivas de 20/22/16/18/24%; removido o fluxo "Mais opções".
- Catálogo POS paginado no servidor em blocos de 36, com pesquisa/disponibilidade antes da
  transferência, debounce de 300 ms e navegação Anterior/Próximo. Scanner consulta endpoint directo
  para continuar independente da página actual.
- Corrigida activação operacional: uma instância antiga do backend ficou temporariamente na porta
  8080 durante o reinício. Endpoint verificado ao vivo e coberto por novo teste HTTP autenticado.
- Spec e harness: `docs/POS_CARRINHO_OPERACIONAL_SPEC.md` e
  `docs/POS_CARRINHO_OPERACIONAL_HARNESS.md`.
- Catálogo POS passa a abrir em **Todos**: produtos esgotados aparecem atenuados, etiquetados e sem
  clique; filtro **Disponíveis** preserva a vista rápida. O estado continua vindo do endpoint
  canónico de vendáveis e scanner/balança também bloqueiam esgotados.
- Spec/harness: `docs/POS_CATALOGO_ESTADO_STOCK_SPEC.md` e
  `docs/POS_CATALOGO_ESTADO_STOCK_HARNESS.md`.

### Paginação uniforme das tabelas — 2026-08-16

- Listagens Swing carregadas integralmente passam a receber paginação local central (25/50/100/200),
  aplicada depois dos filtros e recalculada com alterações do modelo.
- Listagens de crescimento elevado mantêm paginação no servidor via `TablePager`/`PageResponse`;
  tabelas transaccionais (carrinho, linhas e diálogos) permanecem contínuas.
- Spec/harness: `docs/TABELAS_PAGINACAO_UNIFORME_SPEC.md` e
  `docs/TABELAS_PAGINACAO_UNIFORME_HARNESS.md`.
- Navegação lateral externa refinada: início/Page Up/Page Down/fim com nomes acessíveis, tooltips
  claros e desactivação automática nos limites da lista.
- Categorias: removida a lupa externa duplicada; o `SearchField` mantém uma única lupa integrada.
- POS: hierarquia cromática semântica aplicada aos botões (verde, azul, âmbar, vermelho e grafite),
  substituindo acções operacionais que pareciam pretas.
- Paginação: controlos separados por 8 px e margem vertical de 10 px antes das acções inferiores.
- Backup automático: deixa de tentar `pg_dump` no backend H2; a execução interactiva usa backup
  lógico JSON, enquanto PostgreSQL mantém o `.dump` físico restaurável.
