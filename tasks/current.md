# Tarefa actual

Estado: **manutenção e estabilização**  
Actualizado em: 2026-10-10  
Histórico anterior: [arquivo até 2026-10-10](archive/current-history-through-2026-10-10.md)

Este ficheiro contém apenas o contexto necessário para continuar o trabalho actual. Resultados
concluídos devem ser movidos para `tasks/archive/` no fecho de cada fase.

## Situação actual

- O reactor Maven mantém os módulos físicos `contracts`, `backend` e `desktop`.
- O desktop comunica exclusivamente com o backend por HTTP/HTTPS.
- A separação arquitectural é protegida por `MultiModuleArchitectureHarnessTest`.
- O contexto Swing é protegido por `DesktopThinContextTest`.
- A última fase documentada consolidou PDFs oficiais, impressão térmica e exportação tabular.
- A validação registada para essa fase inclui `ExecutivePdfHarnessTest`, testes de impressão,
  `TableExportHarnessTest`, `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
- Guia de remessa avulsa implementada com selecção paginada de produtos e saída FEFO apenas na
  aprovação; decisão em `docs/STANDALONE_DELIVERY_GUIDE_SPEC.md`.
- Validação dirigida concluída: compilação dos três módulos,
  `DeliveryGuideServiceTest`, `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.

## Trabalho activo

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
    - Backend reiniciado como daemon (status `UP`) e Desktop lançado interativamente no Windows.

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
