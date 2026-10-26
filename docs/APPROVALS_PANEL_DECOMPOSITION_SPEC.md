# SPEC-APPROVALS-PANEL-DECOMPOSITION-001: Decomposição Modular da Central de Aprovações

## 1. Visão Geral e Objectivo
O painel `ApprovalsPanel.java` (341 linhas) é o centro nevrálgico de governança, autorizações e alçadas da plataforma Multicore ERP (autorização de encomendas, faturas, transferências entre armazéns, quebras anormais e pedidos de compra).
O objetivo desta decomposição é desacoplar a visualização e filtragem temporal do histórico de auditoria (`ApprovalsHistoryView`) e a coordenação de métricas, despacho de decisão e humanização de tipos documentais (`ApprovalsOperationsCoordinator`), reduzindo o orquestrador para $\le 230$ linhas sem quebrar nenhum contrato literal pré-existente.

## 2. Invariantes Arquiteturais e Restrições Não Negociáveis
1. **Camadas Físicas**: Módulo `desktop` consome clientes HTTPS (`ApprovalApiClient`, `StockTransferApiClient`, `ComercialApiClient`, `PrintApiClient`).
2. **Contratos Literais e de Teste**:
   - Construtores sobrecarregados preservados:
     - `public ApprovalsPanel(ApprovalApiClient approvalApiClient)`
     - `public ApprovalsPanel(ApprovalApiClient, StockTransferApiClient, ComercialApiClient)`
     - `public ApprovalsPanel(ApprovalApiClient, StockTransferApiClient, ComercialApiClient, PrintApiClient)`
   - Invariantes literais exigidos por `AdminApprovalsUnifiedHarnessTest` (`AA-01`):
     - `UIHelper.ensureHeadersFit(pendingTable)`
     - `TableQuickPeekController.install(pendingTable`
     - `KpiCard.createGrid(4)`
   - Invariante literal exigido por `TableActionPlacementHarnessTest`:
     - `UIHelper.tableCardTop(`
   - Eliminação absoluta de `JOptionPane`.
   - Limite estrito de 4 KPIs no topo via `KpiCard.createGrid(4)`.
   - Estrutura corporativa em abas via `JTabbedPane` com `UIHelper.styleTabbedPaneMulticore`.

## 3. Arquitetura de Componentes e Decomposição
A central de aprovações é estruturada em:

```
mz.multicore.erp.gui
├── ApprovalsPanel.java (Orquestrador UI e aba de Pendentes: <= 230 linhas)
├── ApprovalsHistoryView.java (Aba de Histórico e Auditoria com filtros e Quick Peek: <= 95 linhas)
└── ApprovalsOperationsCoordinator.java (Coordenação de KPIs, decisão modal e tipologia: <= 85 linhas)
```

### 3.1 `ApprovalsHistoryView.java`
- Painel corporativo baseado em `ModernPanel(16)` contendo a tabela de histórico (`historyTable`, `historyModel`), filtros de estado ("APPROVED", "REJECTED") e período via `TableFilter.periodCombo()`.
- Quick Peek integrado (`TableQuickPeekController`) exibindo metadados, valores e motivos de rejeição/parecer.
- Paginação corporativa via `ClientTablePagination.install(historyTable)`.
- Método `populateHistory(List<ApprovalRequestDTO> requests)`.

### 3.2 `ApprovalsOperationsCoordinator.java`
- Método `humanType(String documentType)`: Tradução semântica de tipos de documento em pt-MZ (Encomenda, Fatura, Despesa, Transferência de Stock, Quebra de Stock, Ordem de Compra).
- Método `updateKpis(...)`: Cálculo e formatação em tempo real dos 4 KPIs executivos (pedidos pendentes, valor acumulado sob validação, pedidos aprovados e rejeitados).
- Método `openDecisionForSelected(...)`: Despacho modal seguro para `ApprovalDetailsDialog.show`.

## 4. Metas de Linhas de Código
| Ficheiro | Linhas Antes | Meta Após | Limite Máximo |
| :--- | :--- | :--- | :--- |
| `ApprovalsPanel.java` | 341 | ~215 | $\le 230$ |
| `ApprovalsHistoryView.java` | — | ~85 | $\le 95$ |
| `ApprovalsOperationsCoordinator.java` | — | ~65 | $\le 85$ |
