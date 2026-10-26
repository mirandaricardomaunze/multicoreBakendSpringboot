# Especificação Canónica — Decomposição do PurchaseBusinessUnitsPanel

**ID:** `SPEC-PURCHASE-BUSINESS-UNITS-PANEL-DECOMPOSITION-001`  
**Domínio:** Compras / Linhas de Negócio & Distribuição  
**Alvo:** `mz.multicore.erp.gui.PurchaseBusinessUnitsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `PurchaseBusinessUnitsPanel` gerencia as Linhas de Negócio e marcas de distribuição (ex.: Unilever, BAT, Alimentares), sua associação a fornecedores fabricantes e contagem de artigos vinculados.

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem dos 3 KPIs executivos, tabela de 6 colunas com renderizadores de quantidade e status, Quick Peek detalhado, ações de ativação/desativação e formulário modal completo de criação/edição assíncrona com validação de código e nome (`openBusinessUnitDialog`).
- **Tamanho original:** 308 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

1. **`PurchaseBusinessUnitsPanel` (Orquestrador / View Principal):**
   - Construção dos 3 KPIs padronizados de 96px em linha única horizontal via `KpiCard.createGrid(3, 8, 8)`.
   - Card principal `ModernPanel(16)` via `UIHelper.tableCardTop("Linhas de Negócio / Distribuição", ...)`.
   - Preservação de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`, `ClientTablePagination.install(table)`.
   - Métodos canónicos preservados: construtor `PurchaseBusinessUnitsPanel(ComprasPanel owner)`, `buildPanel()`, `loadBusinessUnits()`.
   - Meta de linhas: $\le 180$ linhas.

2. **`BusinessUnitFormDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal ergonómico baseado em `ModernFormDialog` com validação assíncrona atómica (`setOnSaveAsync`), limites contidos na área útil (`UIHelper.applyStandardModalBounds`) e feedback via `ToastManager`.

3. **`PurchaseBusinessUnitsOperationsCoordinator` (`mz.multicore.erp.gui`):**
   - Coordenador de operações visuais para configuração dinâmica do Quick Peek (`populateQuickPeek`) e comutação segura de estado ativo/inativo (`toggleActive`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Padrões Canónicos:**
  - `KpiCard.createGrid(3, 8, 8)` com os 3 KPIs uniformes de 96px.
  - `UIHelper.tableCardTop("Linhas de Negócio / Distribuição"`.
  - `UIHelper.ensureHeadersFit(table)` e `TableQuickPeekController.install(table`.
  - 0 chamadas a `JOptionPane`.
