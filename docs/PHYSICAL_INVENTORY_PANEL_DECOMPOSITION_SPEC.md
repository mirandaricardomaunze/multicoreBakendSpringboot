# Especificação de Decomposição Modular: Painel de Inventário Físico & Reconciliação (PhysicalInventoryPanel)

**Identificador:** `SPEC-PHYSICAL-INVENTORY-PANEL-DECOMPOSITION-002`  
**Domínio:** Inventário / Armazém / Contagem Física / Reconciliação de Stock  
**Alvo:** `mz.multicore.erp.gui.inventory.PhysicalInventoryPanel.java` (365 linhas)  
**Meta de Redução:** $\le 200$ linhas no orquestrador principal ($\approx 46\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `PhysicalInventoryPanel.java` é o centro operacional de contagem física de inventário, bipamento rápido de códigos de barras / SKU, cálculo em tempo real de sobras e faltas com impacto financeiro (MT), reconciliação e acerto automático de stock.

Anteriormente, o painel foi parcialmente decomposto com `PhysicalInventoryOperationsCoordinator` e `PhysicalInventoryTableHelper`. No entanto, o painel principal acumulava ainda 365 linhas com:
1. **Povoamento e Formatação Tabular:** Conversão e formatação de linhas de contagem aberta vs cega (`blindCounting`), diferenças e impacto financeiro (`populateTable`).
2. **Atualização Reativa de KPIs e Estados Operacionais:** Atualização de métricas (total de itens, contados, sobras e faltas em MT) e desativação reativa de controlos de bipamento (`updateKpiAndButtons`, `enableControls`).
3. **Barra de Ferramentas e Filtros:** Construção da barra de selecção de sessão, pesquisa e bipamento.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Contratos Visuais & Invariantes de Tabela (`PhysicalInventoryPanelHarnessTest` & `PhysicalInventoryPanelDecompositionHarnessTest`):**
   - Preservar `getItemsTable()` devolvendo `itemsTable` com `RowSorter` ativo (`TableFilter.install(itemsTable, searchField)`).
   - Preservar `handleBarcodeScan()` com foco no campo de código de barras.
   - Preservar `BorderLayout.NORTH` do painel central contendo a grelha de KPIs via `KpiCard.createGrid(4)`.
   - Preservar `UIHelper.tableCardTop("Inventário Físico & Reconciliação", ...)` no topo do `ModernPanel(16)`.
   - Preservar no máximo 3 botões na barra de ações à direita (`sessionActionsMenu`, `printPdfBtn`, `newSessionBtn`), contendo `ActionMenuButton`.
   - Preservar `TableQuickPeekController.install(itemsTable, card, ...)`.
   - Preservar `UIHelper.ensureHeadersFit(itemsTable)`.
   - Preservar `ClientTablePagination.install(itemsTable)`.
2. **Eliminação Total de Diálogos Legados:**
   - 0 chamadas a `JOptionPane`.
3. **Ausência de Emojis Unicode:**
   - 0 caracteres especiais/emojis no código-fonte.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui.inventory/
├── PhysicalInventoryPanel.java                       (Orquestrador principal: <= 200 linhas)
│   ├── buildCenterPanel(), buildTableCard(), buildCardToolbar()
│   ├── refreshSessions(), onSessionSelected(), handleBarcodeScan()
│   └── getItemsTable()
├── PhysicalInventoryOperationsCoordinator.java       (Ciclo de vida assíncrono: <= 160 linhas)
│   ├── openNewSessionDialog, handleStartCounting, handleCloseSession, handleCancelSession, handlePrintPdf
├── PhysicalInventoryTableHelper.java                 (Configuração de grelha e atalhos: <= 120 linhas)
│   └── configureTableInteractions
└── PhysicalInventoryViewCoordinator.java             (Apresentação, KPIs e linhas: <= 110 linhas)
    ├── populateTable(tableModel, session)
    ├── updateKpiAndButtons(...)
    └── enableControls(...)
```
