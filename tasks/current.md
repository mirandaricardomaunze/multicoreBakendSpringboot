# Tarefa actual

Estado: **manutenção e estabilização**  
Actualizado em: 2026-10-12  
Histórico anterior: [arquivo até 2026-10-10](archive/current-history-through-2026-10-10.md)

Este ficheiro contém apenas o contexto necessário para continuar o trabalho actual. Resultados
concluídos devem ser movidos para `tasks/archive/` no fecho de cada fase.

## Situação actual

- Fase de performance de contas a receber concluída em código: saldo de crédito agregado na BD,
  mapa limitado a documentos em aberto e clientes carregados numa consulta. V90 corrigida após
  confirmação do utilizador de que só existe localmente. Evidência e limites:
  [fase de 2026-10-12](archive/performance-receivables-2026-10-12.md).
- Não confundir os harnesses funcionais com certificação de capacidade: carga e concorrência
  em PostgreSQL continuam por validar. Não anunciar número de utilizadores suportados.

- O reactor Maven mantém os módulos físicos `contracts`, `backend` e `desktop`.
- O desktop comunica exclusivamente com o backend por HTTP/HTTPS.
- A separação arquitectural é protegida por `MultiModuleArchitectureHarnessTest`.
- O contexto Swing é protegido por `DesktopThinContextTest`.
- A última fase documentada consolidou PDFs oficiais, impressão térmica e exportação tabular.
- Harmonizado o layout de vias em documentos PDF (Fatura, Notas de Crédito/Débito, Guias de Remessa/Transferência): o texto nominal da via (`1.ª VIA — ORIGINAL`, `2.ª VIA — DUPLICADO`, etc.) passa a residir na coluna direita junto ao título e número do documento (`CompanyHeaderRenderer`), eliminando faixas horizontais redundantes.
- Tabela de linhas (`LineItemsTableRenderer`) e discriminativo de imposto (`TotalsBlockRenderer`): títulos com `setNoWrap(true)` e larguras calibradas (coluna `Ref.` aumentada para 9.5f para conter códigos de referência sem quebra horizontal).
- A validação registada para essa fase inclui `ExecutivePdfHarnessTest`, testes de impressão,
  `TableExportHarnessTest`, `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
- Guia de remessa avulsa implementada com selecção paginada de produtos e saída FEFO apenas na
  aprovação; decisão em `docs/STANDALONE_DELIVERY_GUIDE_SPEC.md`.
- Validação dirigida concluída: compilação dos três módulos,
  `DeliveryGuideServiceTest`, `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.

## Trabalho activo

0. Formalizado o perfil empresarial `SELLER` (Vendedor): hierarquia e limites documentados em
   `docs/SELLER_ROLE_SPEC.md`, guarda obrigatória na criação/edição de encomendas, rótulo pt-MZ
   no desktop e editor de utilizadores alinhado aos quatro perfis realmente aceites pelo backend.
   POS agora exige perfil de vendedor ou superior para abrir caixa, vender e fechar; o vendedor
   só opera a sua sessão e a diferença de fecho mantém alçada de gestor. Validação dedicada:
   `SellerRoleHarnessTest`, `DesktopSellerRoleHarnessTest` e `POSServiceTest`.
0b. Otimização ergonómica de espaço útil no POS (`POSPanel.java`): alinhamento do widget da balança
   (display de peso, Capturar, Tarar e Simular) na linha de topo junto a `Actualizar` e `Abrir/Fechar Caixa`;
   acções secundárias de caixa (`Sangria / Suprimento` e `Passar Turno`) consolidadas no menu `Operações`;
   banner de sessão afinado para uma faixa mínima e botões de quantidade (`−`, `Quantidade [F6]`, `+`)
   movidos para o rodapé da carrinha junto a `Remover [Del]`, libertando ~70px verticais adicionais para a
   grelha de artigos do carrinho e o catálogo de produtos. Validação dedicada: `PosHeaderLayoutHarnessTest`,
   `PosProfessionalErgonomicsHarnessTest`, `PosButtonColourHierarchyTest` e `DesktopThinContextTest`.

1. Concluída a blindagem e padronização global de tabelas e duplo clique no Swing (`RowDetailsInspector`, `RecordDetailsDialog`, `PurchaseOrdersPanel`, `StockPanel`, `QuotationsPanel`, `CommercialInvoicesView`, `RecurringContractsPanel`, `ManufacturingPanel`, `FleetPanel`, `AssetsPanel`, `DeliveryGuidesPanel`).
2. Eliminado o defeito de redirecionamento indesejado para formulários/editores ao fechar janelas de detalhes.
3. Adicionada trava atómica de reentrância em `RowDetailsInspector` e `RecordDetailsDialog` prevenindo abertura dupla de modais.
4. Atualizada a especificação `DOCUMENT_TABLE_INTERACTION_SPEC.md` e validado o harness `DocumentTableInteractionHarnessTest`.
5. Manter a suite completa estável após alterações de UI, contratos ou fronteiras Maven.
6. Padronização DRY universal de pesquisa paginada de catálogo de produtos (`ProductLookupDialog`, `ProductSearchComboBox`, `UIHelper.createProductLookupButton`, atalho canónico `F3`) em todas as grelhas e tabelas de linhas de documento (Transferências de Stock, Encomendas de Cliente, Faturação/Vendas, Cotações e Encomendas a Fornecedor).
7. Concluída a erradicação de enums em inglês nas tabelas e filtros do ERP (substituição por termos canónicos em Português de Moçambique: `Pendente`, `Facturado`, `Aguarda separação`, `Encomendado`, `Rascunho`, `Activo`, etc.). Especificação documentada em `docs/TABELAS_ESTADOS_PTMZ_SPEC.md` e harness validado em `TableStatusPtMzHarnessTest` (4/4 testes aprovados).
8. Concluída a especificação e blindagem de segurança para faturação única de encomendas, notas de crédito/débito, visibilidade do histórico de impressões e segregação estrita na reimpressão presencial (documentada em `docs/SEGURANCA_FATURACAO_E_REIMPRESSAO_SPEC.md`). Validado o harness dedicado `OrderBillingAndReprintSecurityHarnessTest` (8/8 testes aprovados), `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
9. Concluído o módulo **Gestão de Negócios** (Linhas de Distribuição / Unidades de Negócio multi-fornecedor, ex.: Unilever, BAT, Food). Integrada aba "Gestão de Negócios" em Compras (`PurchaseBusinessUnitsPanel`), rastreamento de artigos vinculados e seletor no cabeçalho da faturação (`ComercialPanel` / `CommercialInvoicesView`). Regra R4 implementada: fatura aceita produtos mistos por defeito; quando restrita a uma unidade de negócio, valida estritamente todos os produtos e bloqueia artigos divergentes. Especificação documentada em `docs/GESTAO_DE_NEGOCIOS_SPEC.md`, harness validado em `BusinessUnitHarnessTest` (5/5 testes aprovados), `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
10. Implementados filtros e colunas de **Negócio** de forma universal em todas as tabelas do sistema onde circulam artigos, compras, fornecedores, propostas e movimentações:
    - Níveis de Stock / Inventário (`StockPanel`)
    - Lotes e Validades de Stock (`StockBatchesPanel`)
    - Faturas Recentes de Clientes (`CommercialInvoicesView` / `ComercialPanel`)
    - Encomendas de Clientes (`CommercialOrdersView` / `ComercialPanel`)
    - Cotações / Propostas Comerciais (`QuotationsPanel`)
    - Faturas de Compra / Entrada de Stock (`ComprasPanel`)
    - Sugestão de Reposição de Compras (`PurchaseReorderPanel`)
    - Encomendas a Fornecedor (`PurchaseOrdersPanel`)
    - Histórico de Vendas POS (`PosSalesHistoryPanel`)
    - Alertas de Stock - Lotes Expirados (`StockAlertsPanel`)
    - Alertas de Stock - Produtos Esgotados (`StockAlertsPanel`)
    - Movimentos & Rastreabilidade de Stock (`StockPanel`)
    - Transferências entre Armazéns (`StockPanel`)
    - Guias de Remessa (`DeliveryGuidesPanel`)
    - Quebras & Desperdício (`StockWastePanel`)
    - Notas de Crédito e Débito (`CommercialNotesPanel`)
    - Promoções de Loja (`PromotionsPanel`)
    - Contas em Aberto / Pendentes de Clientes (`OutstandingAccountsPanel`)
    - Extrato de Conta Corrente de Clientes (`CustomerStatementPanel`)
    - Extrato de Conta Corrente de Fornecedores (`SupplierStatementPanel`)
    - Contas a Pagar a Fornecedores (`PurchasePayablesPanel`)
    - Recibos Emitidos / Liquidações Comerciais (`ReceiptsPanel`)
    Todos os filtros seguem o padrão canónico `TableFilter.bar` com `TableFilter.combo("Todos os negócios")`, exibição em Quick-Peek (`Espaço`), retrocompatibilidade total de DTOs e validação por harness (`BusinessUnitHarnessTest`, `AccountStatementHarnessTest`, `DesktopThinContextTest`, `MultiModuleArchitectureHarnessTest`).
11. Iniciada e fechada a baseline transversal de robustez multiutilizador: capacidade Hikari/Tomcat
    configurável por ambiente, shutdown gracioso, probes readiness/liveness, métricas Prometheus
    apenas internas e invariantes permanentes de concorrência em `docs/ROBUSTEZ_MULTIUTILIZADOR_SPEC.md`.
    O `MultiUserRobustnessHarnessTest` protege stateless HTTP, locks de numeração/stock, versões dos
    agregados críticos, retries limitados, configuração operacional e não exposição pública do
    Actuator. Validados também `OptimisticLockingConcurrencyHarnessTest` e
    `MultiModuleArchitectureHarnessTest`; `mvn compile` passou. `mvn clean compile` não conseguiu
    apagar o JAR do backend porque a aplicação estava em execução, sem erro de compilação.
12. Primeira adopção de paginação real concluída em Tesouraria: novo endpoint retrocompatível
    `/api/finance/transactions/page`, `Pageable` aplicado no repositório, limite transversal de 200
    registos e método tipado no cliente desktop. O ecrã `FinanceiroPanel` (aba Histórico de Fluxo de
    Caixa) foi ligado a este endpoint via `TablePager`. `TreasuryPaginationHarnessTest`, testes de
    `FinanceService`, robustez e arquitectura aprovados. A vista agregada de Movimentos foi
    classificada para projecção própria; paginação posterior à materialização ficou proibida pela
    SPEC por não resolver consumo de memória/BD.
13. Paginação real aplicada ao histórico de movimentos de stock através de
    `/api/inventory/movements/page`, com consulta e `countQuery` tenant-scoped, limite transversal
    de página e cliente desktop tipado. O ecrã `StockPanel` (aba Movimentos & Rastreabilidade) foi
    migrado para navegação remota server-side via `TablePager`. O endpoint legado permanece disponível.
    Validado por `StockMovementPaginationHarnessTest`, `StockUnifiedHarnessTest`, `DesktopThinContextTest`
    e pelos gates de robustez/arquitectura.
14. Fechado o ciclo concorrente de recebimentos: liquidação directa bloqueia a factura por empresa;
    emissão de recibo usa o mesmo lock antes de calcular saldo; anulação bloqueia primeiro o recibo
    e depois a factura antes de estornar tesouraria. Isto impede dupla liquidação, recibos que em
    conjunto excedem o saldo e duplo estorno. Validado por `InvoicePaymentConcurrencyHarnessTest`,
    `FinanceServiceTest`, `ComercialServiceTest` e gates de arquitectura.
15. Fechado o ciclo concorrente das sessões POS: abertura usa a empresa como mutex de BD antes do
    teste/criação da sessão activa, impedindo duas caixas abertas para o mesmo operador; fecho usa
    lock pessimista tenant-scoped antes de reconciliar e depositar, impedindo duplo depósito.
    `PosSessionConcurrencyHarnessTest`, `POSServiceTest` e `PosZReportHarnessTest` aprovados, com
    confirmação de `FOR UPDATE` no teste de integração.
16. Implementado o 3.º nível de escalabilidade e performance para evitar latência em alto volume:
    - Migração Flyway `V90__scalability_performance_composite_indexes.sql`: índices B-Tree compostos
      tenant-scoped (`company_id` + data/estado/série) sobre tabelas de grande escala (`invoices`,
      `customer_orders`, `stock_movements`, `stocks`, `treasury_transactions`, `purchases`, `journal_entries`, `products`).
    - JDBC Batching no Hibernate configurado (`jdbc.batch_size=25`, `order_inserts=true`, `order_updates=true`,
      `batch_versioned_data=true`), eliminando round-trips N+1 de persistência de linhas de fatura e movimentos.
    - Paginação server-side de Encomendas de Clientes (`GET /api/comercial/orders/page`) com `Pageable`,
      `countQuery`, `PageResponseMapper` e cliente desktop tipado.
    - Integração na UI do Desktop (`CommercialOrdersView` / `ComercialPanel`) com `TablePager` e
      desativação da paginação artificial em memória (`ClientTablePagination.DISABLED`).
    - Aprovados com 100% de sucesso: `OrderPaginationHarnessTest`, `MultiModuleArchitectureHarnessTest`,
      `DesktopThinContextTest`, `CommercialCrmUnifiedHarnessTest`, compilação e empacotamento Maven.
17. Concluída a implementação de **Nota Profissional no Pé da Página da Encomenda** e ampliação de espaço das colunas numéricas (`Val.`, `Qtd.`, `Emb.`, `Cx.`, `% Cx.`, `P. Unit.`):
    - Migração `V91__order_notes.sql`: adicionada coluna `notes VARCHAR(1000)` em `customer_orders`.
    - Contratos (`contracts`): atualizados `CreateOrderRequest`, `UpdateOrderRequest`, `OrderDTO` e `OrderTerms` com campo `notes` e construtores retrocompatíveis.
    - Regras de negócio (`ComercialService` / `Quotation`): cópia fiel de observações ao converter cotação em encomenda, persistência na criação e atualização direta.
    - Interface Desktop (`CommercialOrdersView` / `ComercialPanel` / `CommercialOrderSubmission` / `CommercialOrderEditorActions`): campo dedicado `orderNotesField` com tooltip e sincronização de rascunho/edição.
    - PDF da Encomenda (`OrderPrintService`): adicionado bloco executivo no pé da página (`buildFooterBlock`) integrando as observações da encomenda e o rodapé institucional (`columns.footer()`).
    - Renderização de linhas (`LineItemsTableRenderer`): ampliado o espaço das colunas numéricas em crescimento (`Val.`: 9.0%, `Qtd.`: 8.0%, `Emb.`: 6.5%, `Cx.`: 6.5%, `% Cx.`: 8.0%, `P. Unit.`: 13.5%), balanceadas com `Cód.` (8.5%), `Ref.` (7.0%), `Desc.` (19.5%) e `Subt.` (9.5%), com `setNoWrap(true)` e borda refinada de 0.5f.
    - Aprovados com 100% de sucesso: `OrderPrintServiceTest` (incluindo teste com notas e rodapé), `LineItemsTableRendererTest`, `ExecutivePdfHarnessTest`, `MultiModuleArchitectureHarnessTest`, `DesktopThinContextTest` e `OrderEditorHarnessTest`.
    - Backend reiniciado como daemon (status `UP`) e Desktop lançado interativamente no Windows via `schtasks`.
18. Resolução e auditoria transversal do defeito de sobreposição visual (inputs e botões atrás de tabelas):
    - **Diagnóstico:** O componente `TableFilter.bar` e a secção de ações de `TableFilter.toolbar` utilizavam `FlowLayout` nativo do Java AWT. Quando a barra continha muitos elementos e a largura da janela/card era menor que o total somado, o `FlowLayout` quebrava componentes para a 2.ª linha (`y >= 38px`). Como o `BorderLayout.NORTH` do card só alocava a altura de 1 linha (~36px), a `JTable`/`JScrollPane` em `BorderLayout.CENTER` era desenhada por cima da 2.ª linha, fazendo com que inputs e botões aparecessem como elementos fantasmas cortados atrás do cabeçalho da tabela.
    - **Solução Arquitetural no `TableFilter`:** Blindados tanto `TableFilter.bar` como `TableFilter.toolbar` (ações à direita) para impor layout estritamente horizontal em linha única sem quebra (`y < 36px`), garantindo que nenhum elemento desça para as coordenadas físicas da tabela.
    - **Auditoria Transversal e Calibração de Todos os Painéis do Sistema:**
      - [ReceiptsPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/ReceiptsPanel.java): pesquisa (160px), negócio (135px), método (125px), estado (115px), período (120px); erradicados enums em inglês (`CASH`, `BANK_TRANSFER`, `CARD` -> `Dinheiro`, `Transferência`, `Cartão`, `M-Pesa`).
      - [OutstandingAccountsPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/OutstandingAccountsPanel.java): pesquisa (160px), negócio (135px), estado (130px), aging (145px), período (120px).
      - [StockBatchesPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockBatchesPanel.java): pesquisa (175px), negócio (135px), armazém (135px), validade (145px).
      - [PurchaseReorderPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PurchaseReorderPanel.java): pesquisa (160px), negócio (140px), urgência (130px), fornecedor (145px).
      - [PromotionsPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PromotionsPanel.java): pesquisa (160px), negócio (140px), tipo (135px), estado (125px).
      - [StockWastePanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockWastePanel.java): pesquisa (160px), negócio (140px), motivo (150px), estado (135px), período (120px).
      - [PurchaseOrdersPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java): pesquisa (160px), negócio (135px), estado (135px), período (120px).
      - [CustomerStatementPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CustomerStatementPanel.java): cliente (190px), negócio (140px).
      - [SupplierStatementPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/SupplierStatementPanel.java): fornecedor (190px), negócio (140px).
      - [StockPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockPanel.java): aba Movimentos calibrada (pesquisa 150px, negócio 135px, produto 145px, tipo 120px, datas 95px) e tipos de movimento traduzidos de enums em inglês para pt-MZ (`Compra`, `Entrada`, `Venda`, `Transferência`, `Ajuste`, `Devolução`, `Estorno`).
      - [CRMPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CRMPanel.java): abas Pedidos e Folhas de Obra calibradas com pesquisa (160px), estado (125px), prioridade (130px) e período (120px).
    - **Validação:** Aprovados `TableFilterHarnessTest` (5/5 testes), `TableStatusPtMzHarnessTest` (5/5 testes), `CommercialOperacionalUnifiedHarnessTest` (3/3 testes), `DesktopThinContextTest` (2/2 testes) e `MultiModuleArchitectureHarnessTest` (6/6 testes). JAR repackaged e aplicação desktop reiniciada com sucesso.
19. Implementação do **Reforço de Segurança Empresarial (PasswordPolicy, Revogação Atómica & Defense-in-Depth)**:
    - **Política de Senhas Fortes (`PasswordPolicy`):** criação de política canónica obrigatória (mínimo de 8 caracteres, pelo menos uma letra e um número, rejeição de senhas que contenham o username e blacklist de senhas triviais como `12345678`, `password`, `admin123`, `multicore`). Integrada centralmente em `createUser` e `resetPassword` em `AppUserService`.
    - **Revogação Imediata e Atómica de Sessões:** ao desativar um utilizador (`toggleUserStatus(username, false)`), redefinir senha (`resetPassword`) ou alterar o perfil da empresa (`updateCompanyRole`), o sistema revoga imediatamente todos os tokens Bearer ativos no `AuthSessionService`, eliminando o risco de janelas de privilégio indevido após demissões ou despromoções.
    - **Defense-in-Depth no Spring Security:** enriquecimento automático do `SecurityContextHolder` com `ROLE_<ROLE_NAME>` (ex.: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_SELLER`, `ROLE_EMPLOYEE`) pelo `SecurityInterceptor` a cada requisição após resolução de tenant, com limpeza atómica no `afterCompletion`.
    - **Especificação & Harness:** especificação canónica em `docs/REFORCO_SEGURANCA_ENTERPRISE_SPEC.md` e novo harness dedicado `SecurityEnterpriseHardeningHarnessTest` (10/10 testes aprovados). Aprovados a 100%: suite de segurança e autorização (32/32 testes), `MultiModuleArchitectureHarnessTest` (6/6 testes) e `DesktopThinContextTest` (2/2 testes).
20. Implementação da **Matriz Canónica de Perfis e do perfil Vendedor (`SELLER`)**:
    - Política HTTP central `ApiRolePolicy`, com negação por omissão e separação explícita entre
      `EMPLOYEE`, `SELLER`, `MANAGER` e `ADMIN`.
    - `EMPLOYEE` limitado a auto-serviço mínimo; `SELLER` limitado ao POS próprio e operação
      comercial; áreas financeiras, RH geral, aprovações, utilizadores e backup permanecem na gestão.
    - Barra lateral e navegação programática do desktop filtradas por perfil, sem tratar a UI como
      fronteira de segurança.
    - POS protegido por identidade do operador: vendedor só abre, usa e fecha a própria sessão.
    - Corrigida a aprovação de desconto superior a 10% para exigir sempre `MANAGER`, sem Green Lane
      baseada no pequeno valor da factura.
    - Especificações em `docs/ROLE_PERMISSION_MATRIX_SPEC.md` e `docs/SELLER_ROLE_SPEC.md`, com
      harnesses backend, API e desktop dedicados.
21. Implementação dos **Alertas e Orientação de Acções na Interface (Zero Retornos Silenciosos)**:
    - **Diagnóstico:** Botões e atalhos que falhavam em silêncio ao serem clicados sem linha seleccionada na tabela ou sem pré-requisitos de fluxo atendidos, deixando o utilizador sem orientação sobre a acção necessária.
    - **Resolução Transversal em 8 Módulos:**
      - `ManufacturingPanel`: alerta ao clicar em acções de ordem sem selecção de OP.
      - `FleetPanel`: alerta ao clicar em histórico de combustível sem selecção de viatura.
      - `AssetsPanel`: alerta ao clicar em detalhes de activo sem selecção.
      - `BankReconciliationPanel`: avisos via `ToastManager` ao desconciliar, associar manual ou lançar despesa sem linha de extracto seleccionada.
      - `PhysicalInventoryPanel`: avisos via `ToastManager` ao tentar bipar sem sessão de inventário aberta ou com campo de código vazio.
      - `CommercialNotesPanel`: aviso via `ToastManager` ao clicar em `[Remover]` sem linha seleccionada na grelha.
      - `POSPanel` / `PosCatalogController`: alerta orientador ao capturar peso com balança a zero ou carrinha vazia; alerta ao tentar finalizar com carrinho vazio.
      - `RecordDetailsDialog`: aviso informativo via `ToastManager` orientando a selecção de linha na tabela.
    - **Testabilidade:** Adicionado `setTestListener` em `ToastManager` e expostos getters contextuais em `InlineFeedbackPanel`.
    - **Especificação & Harness:** Criada especificação canónica em `docs/GUI_ACTION_ALERT_GUIDANCE_SPEC.md` e novo harness dedicado `GuiActionAlertGuidanceHarnessTest` (7/7 testes aprovados). Aprovados também `DesktopThinContextTest` (2/2) e `PosHeaderLayoutHarnessTest` (5/5).
22. Implementação da **Experiência Operacional Dedicada ao Vendedor**:
    - Painel inicial próprio com vendas e atendimentos da sessão de caixa autenticada, encomendas
      pendentes, cotações abertas e seis acções rápidas.
    - Pesquisa global reduzida aos destinos comerciais autorizados para `SELLER`.
    - Área Comercial simplificada para Cotações, consulta de Facturação e Pedidos, ocultando fluxos
      de gestão e acções que o backend recusaria.
    - Tentativas programáticas de navegação proibida apresentam orientação clara ao utilizador.
    - Especificação e harness em `docs/SELLER_EXPERIENCE_SPEC.md` e `SellerExperienceHarnessTest`.
22. Implementação das **Melhorias de Experiência do Utilizador (UX & Ergonomia de Alta Frequência)**:
    - **Auto-Foco Inteligente em Formulários (`ModernFormDialog`):** Foco automático recursivo no primeiro componente interactivo (`JTextField`, `JComboBox`, etc.) ao abrir qualquer modal de formulário, permitindo que o operador inicie a digitação de imediato sem tocar no rato.
    - **Restauração Rápida de Pesquisas (`TableFilter`):** Tecla `Esc` registada no campo de pesquisa para limpar a busca e restaurar a tabela instantaneamente; novo método canónico `TableFilter.clear(table)` para reinicializar pesquisa e dropdowns.
    - **Alinhamento de Atalhos Canónicos (`ShortcutHelpDialog`):** Actualização dos atalhos com `F10 - Finalizar Venda e Abrir Pagamento`, `Espaço - Espreitar Linha Selecionada (Quick-Peek)`, `F3 - Pesquisar Artigos no Catálogo` e `Esc - Limpar Pesquisas / Fechar Janelas`.
    - **Especificação & Harness:** Especificação canónica em `docs/USER_EXPERIENCE_ENHANCEMENT_SPEC.md` e harness dedicado `UserExperienceEnhancementHarnessTest` (4/4 testes aprovados). Aprovados também `TableFilterHarnessTest` (5/5) e `DesktopThinContextTest` (2/2).
23. Implementação do **Stock em Trânsito (Transferência entre Armazéns em 2 Passos)**:
    - **Modelo e Regra de Negócio (Invariante):** Opt-in por guia (`requiresReceipt`, padrão marcado no editor da guia, mantendo retrocompatibilidade total com fluxo de 1 passo existente).
    - **Despacho na Origem (Passo 1):** Aprovação de guia com recepção consome FEFO da origem e gera movimento `TRANSFER` negativo apenas no armazém de origem, promovendo a guia a `IN_TRANSIT` e registando os lotes e validades em viagem em `stock_transfer_line_batches`. O destino permanece intacto. Guias em trânsito podem emitir a guia oficial de transporte de 3 vias e ficam bloqueadas contra cancelamento ou rejeição destrutiva.
    - **Recepção no Destino (Passo 2):** Armazém de destino confirma a recepção física via `POST /api/inventory/transfers/{id}/receive`, momento em que o stock é creditado no destino replicando os lotes de viagem e gerando movimento `TRANSFER` positivo, avançando a guia para `RECEIVED`.
    - **Arquitectura Desacoplada:** Criação do colaborador independente `StockTransferTransitService` e do mapper estático sem estado `StockTransferDtoMapper`, sem acoplamento cruzado entre serviços e preservando construtores legados no `StockTransferService`.
    - **Desktop & UI:** 5º KPI "Em Trânsito" (`KpiCard.createGrid(5)`), filtro de estado atualizado com "Em trânsito" e "Recebido", acção "Confirmar Recepção" adicionada ao menu de documentos (respeitando limite estrito de 5 itens no `ActionMenuButton`), e novo checkbox corporativo no `StockTransferEditorForm`.
    - **Especificação & Harness:** Especificação canónica em `docs/STOCK_IN_TRANSIT_SPEC.md`. Harnesses dedicados aprovados a 100%: backend `StockTransferInTransitHarnessTest` (9/9), `StockTransferServiceTest` (25/25), `InternalReplenishmentServiceTest` (14/14), `MultiModuleArchitectureHarnessTest` (6/6); desktop `StockInTransitUiHarnessTest` (5/5), `TableStatusPtMzHarnessTest` (5/5), `StockTransferDraftUiHarnessTest` (2/2), `TableActionPlacementHarnessTest` (6/6) e `DesktopThinContextTest` (2/2).
24. **Monitorização Proactiva e Alertas de Stock em Trânsito Atrasado/Esquecido:**
    - **Regras & Limiares:** Guias de transferência em trânsito há >= 48h geram alerta operacional de Atenção (`Urgency.WARNING` / `priority = 2`), destacando dias decorridos, armazém de origem, armazém de destino e motorista/veículo responsável; guias com >= 5 dias em trânsito escalam para alerta Crítico (`Urgency.CRITICAL` / `priority = 3`).
    - **Integração no Radar de Alertas Inteligentes & Sino:** `NotificationFeed` invoca `StockTransferApiClient.findInTransit(...)` com isolamento de falhas (degradação graciosa caso a rede falhe). O `SmartAlertsDialog` mapeia o cartão `"stock_transfers"` com o botão de acção contextual `"Verificar Recepção"` que navega directamente para o separador de transferências do `StockPanel`.
    - **Desacoplamento e Decomposição do MainFrame:** Criado `MainFrameSearchIndexProvider` para conter a construção do índice de pesquisa global e dashboard do vendedor, mantendo o `MainFrame` rigorosamente contido em limites limpos e saudáveis conforme exigência do harness.
    - **Especificação & Harness:** Especificação canónica em `docs/STOCK_IN_TRANSIT_OVERDUE_ALERT_SPEC.md`. Harnesses dedicados aprovados a 100%: `StockInTransitOverdueAlertHarnessTest` (4/4), `RadarAlertasProactivoHarnessTest` (7/7), `DesktopThinContextTest` (2/2) e `MultiModuleArchitectureHarnessTest` (6/6).
25. **Implementação dos Indicadores Executivos de Gestão de Armazéns (WMS KPIs):**
    - **Painel & Layout Corporativo:** Adicionada faixa executiva de 4 KPIs (`KpiCard.createGrid(4)`, altura uniforme `STANDARD_CARD_HEIGHT = 96px`) no topo da aba "Gestão de Armazéns" (`StockWarehousesPanel`), em estrito respeito ao design system canónico:
      - **Armazéns Activos:** Rácio de armazéns operacionais face ao total cadastrado (`X / Y`), com subtítulo categorizado em `X loja(s) POS · Y depósito(s)`. Ícone `fas-warehouse` em `ACCENT_CYAN`.
      - **Capacidade Instalada:** Somatório da capacidade nominal agregada dos armazéns activos formatada (`%,.0f un`), com subtítulo `Unidades nominais`. Ícone `fas-boxes` em `MODULE_STOCK`.
      - **Valor em Armazém:** Somatório em tempo real das existências valorizadas em stock (`UIHelper.formatMzn(totalValue)`), com subtítulo `Existências valorizadas`. Ícone `fas-coins` em `APPROVED_GREEN`.
      - **Rede de Distribuição:** Contagem de tipos distintos de armazéns operacionais cadastrados (`X Tipos`), com subtítulo discriminado com as siglas dos tipos em operação (ex.: `Central · Loja · Depósito`). Ícone `fas-network-wired` em `ACCENT_BLUE`.
    - **Sincronização em Tempo Real:** Método `updateKpis()` acionado ao carregar/atualizar a lista de armazéns e sincronizado com o carregamento de saldos de stock (`StockPanel.applyStocks`), mantendo os valores de stock e capacidades sempre consistentes.
    - **Especificação & Harness:** Criada especificação canónica em `docs/WAREHOUSE_MANAGEMENT_KPIS_SPEC.md` e harness dedicado `StockWarehouseKpiHarnessTest` (2/2 testes aprovados). Aprovados a 100%: `StockWarehouseKpiHarnessTest`, `RadarAlertasProactivoHarnessTest`, `DesktopThinContextTest` (2/2) e `MultiModuleArchitectureHarnessTest` (6/6).
26. **Recepção de Transferências em Trânsito com Divergências e Devolução à Origem:**
    - **Invariante Permanente:** $\text{Despachado da Origem} = \text{Recebido no Destino} + \text{Devolvido à Origem}$. O que chegou entra no destino (alocação FEFO); a diferença volta imediatamente à origem com os mesmos lotes e datas de validade originais, sem perdas no éter nem duplicações.
    - **Persistência & Migração BD:** `V93__stock_transfer_receipt_discrepancy.sql` adiciona `received_quantity`, `returned_quantity` e `discrepancy_reason` em `stock_transfer_lines`, `received_quantity` em `stock_transfer_line_batches` e atualiza `stock_transfers.status` para VARCHAR(30).
    - **Ciclo de Vida & Estados:** Novo estado terminal `RECEIVED_WITH_DISCREPANCY` ("Recebido c/ divergência", badge âmbar `UIHelper.PENDING_YELLOW`), com auditoria `STOCK_TRANSFER_RECEIVE_DISCREPANCY` e autorização de impressão da Guia Oficial de Transporte.
    - **Desktop & UI:** Novo diálogo `StockTransferReceivingDialog` (`ModernFormDialog`) com cabeçalho contextual da guia/transportador, tabela de conferência física com cálculo automático de devolução em tempo real, proteção contra paginação comprimida (`ClientTablePagination.DISABLED`), campo de notas gerais e filtro no `StockPanel`.
    - **Especificação & Harness:** Especificação canónica em `docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md`. Harnesses dedicados aprovados a 100%: backend `StockTransferReceiptDiscrepancyHarnessTest` (10/10), `StockTransferInTransitHarnessTest` (9/9), `StockTransferServiceTest` (25/25), `MultiModuleArchitectureHarnessTest` (6/6); desktop `StockTransferReceiptDiscrepancyUiHarnessTest` (6/6), `StockInTransitUiHarnessTest` (5/5), `TableStatusPtMzHarnessTest` (5/5) e `DesktopThinContextTest` (2/2).
20. Reorganização e desanuviamento ergonómico do Painel Principal / Indicadores (`DashboardPanel.java`):
    - **Diagnóstico:** O painel principal encontrava-se visualmente sobrecarregado ("muito cheio"), acumulando 8 cartões hiper-saturados no topo e múltiplos widgets empilhados verticalmente num scroll longo, o que empurrava os 4 gráficos analíticos e operacionais para fora da área visível do ecrã.
    - **Arquitetura em Abas Corporativas (`JTabbedPane` com `UIHelper.styleTabbedPaneMulticore`):**
      - **Aba 1: "Visão Geral & Gráficos" (`fas-chart-pie`):** Linha única horizontal com os 4 KPIs financeiros essenciais (`KpiCard.createGrid(4)`: Tesouraria, Faturação Total, Vendas POS e IVA Liquidado), grelha 2x2 com os 4 gráficos em destaque direto sem scroll (`financialChart`, `salesChannelPieChart`, `operationsChart` e `financialStructurePieChart`), seguidos dos widgets de Top Produtos e Atividade Recente.
      - **Aba 2: "Cockpit Operacional 360°" (`fas-cogs`):** Linha única horizontal com os 4 KPIs operacionais (`KpiCard.createGrid(4)`: Aprovações Pendentes, Atendimento ao Cliente, Alertas de Stock Baixo e Alertas de Validade), seguidos dos cockpits analíticos `StrategicPulseWidget` e `OperationalPulseWidget`.
      - **Aba 3: "Rentabilidade & DRE" (`fas-coins`):** Painel de rentabilidade e margens comerciais (`ProfitAnalyticsWidget`: CMVMC, Margem Bruta e Lucro Operacional).
    - **Validação e Integridade:** Preservados todos os getters e listeners de ciclo de vida (`getStrategicPulseWidget()`, `getOperationalPulseWidget()`, `refreshData()`, `selectPeriod()`, `updateWelcomeMessage()`). Validado com 100% de sucesso em `DesktopThinContextTest` (2/2), `DashboardCommercialKpiHarnessTest` (8/8) e `SimplePieChartTest` (2/2). Backend UP e Desktop relançado via `schtasks`.

## Decisões permanentes relevantes

- O backend é headless e é o único módulo que conhece JPA, Flyway e a base de dados.
- O desktop conhece apenas `contracts`, clientes HTTP e integrações locais.
- DTOs HTTP são records em `contracts`; alterações preservam compatibilidade com a versão desktop
  anterior.
- Mensagens visíveis ao utilizador usam português de Moçambique.
- UI Swing segue `docs/UI_DESIGN_SYSTEM.md` e as regras adicionais de `AGENTS.md`.
- Alterações fiscais, contabilísticas, de stock, POS, caixa ou salários exigem confirmação quando a
  regra de negócio não estiver inequivocamente documentada.

## Próximos passos

- Seleccionar uma única dívida técnica por fase e criar a respectiva SPEC quando a mudança afectar
  contrato, fluxo de negócio ou decisão duradoura.
- Executar os testes dirigidos da área alterada.
- Executar `mvn clean compile` antes de concluir alterações de código, quando possível.
- Actualizar este ficheiro apenas com trabalho activo, bloqueios e decisões ainda abertas.

## Arranque da aplicação no Windows

A regra permanente está em `.agents/rules/startup_procedure.md` e em `AGENTS.md`.

1. Encerrar instâncias anteriores de `javaw` que possam bloquear o JAR.
2. Confirmar `http://localhost:8080/actuator/health` com estado `UP`.
3. Iniciar o desktop na sessão interactiva através de `scripts/run_gui.bat` ou da tarefa agendada
   `MulticoreERP`.
4. Usar sempre `-Djava.awt.headless=false -Dspring.profiles.active=desktop`.

## Critério de fecho da fase

- Código compila e os testes dirigidos passam.
- Fronteiras entre módulos permanecem válidas.
- Contratos e mensagens respeitam as convenções.
- A documentação canónica foi actualizada apenas se houve decisão duradoura.
- O resumo da fase foi movido para `tasks/archive/`.
