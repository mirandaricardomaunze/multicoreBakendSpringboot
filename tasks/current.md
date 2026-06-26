# Tarefa Actual

> Ponteiro da sessão. A IA lê-o no início e actualiza-o sempre que uma fase fecha. ≤1 página. Histórico no `git log`.

**Última actualização:** 2026-06-21
**Estado:** software principal de prontidão para loja/mercearia concluído e testado. O que resta depende de validação manual/hardware/restore em ambiente separado. A fonte de verdade operacional é [tasks/retail_store_readiness.md](retail_store_readiness.md).

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
- Flyway desactivado em dev (H2 + `ddl-auto=update`); activo em prod com `validate`.

## Estado de build

```
mvn clean compile   → BUILD SUCCESS
mvn clean test      → BUILD SUCCESS, 155 testes, 0 falhas (2026-06-26)
```

Diagnostics Lombok no IDE (`cannot find symbol: getX()`) são **ruído**. Critério único: `mvn compile`.
