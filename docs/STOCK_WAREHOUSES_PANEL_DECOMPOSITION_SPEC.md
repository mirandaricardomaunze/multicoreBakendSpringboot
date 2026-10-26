# Especificação Canónica — Decomposição do StockWarehousesPanel

**ID:** `SPEC-STOCK-WAREHOUSES-PANEL-DECOMPOSITION-001`  
**Domínio:** Inventário / Gestão de Armazéns & Centros de Distribuição  
**Alvo:** `mz.multicore.erp.gui.StockWarehousesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `StockWarehousesPanel` é responsável pelo ciclo de vida de armazéns, lojas POS, depósitos e centros de distribuição da empresa, monitorização da capacidade agregada e valorização do stock instalado, e ativação/desativação de locais de armazenagem.

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem dos 4 KPIs com métricas de distribuição, tabela de 9 colunas com calibração e Quick Peek, controlo de paginação, cálculo agregado de inventário e valorização, e diálogo modal completo de criação/edição de armazéns com validação de dados de endereço, tipo e flags de venda ao balcão.
- **Tamanho original:** 313 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

1. **`StockWarehousesPanel` (Orquestrador / View Principal):**
   - Construção dos 4 KPIs canónicos em `KpiCard.createGrid(4)`: "Armazéns Activos" (`fas-warehouse`), "Capacidade Instalada" (`fas-boxes`), "Valor em Armazém" (`fas-coins`) e "Rede de Distribuição" (`fas-network-wired`).
   - Card principal `ModernPanel(16)` via `UIHelper.tableCardTop("Gestão de Armazéns", whFilters, refreshBtn, moreBtn, editBtn, newBtn)`.
   - Invariantes obrigatórios: `TableFilter.bar(`, `UIHelper.ensureHeadersFit(owner.warehousesTable)`, `TableQuickPeekController.install(owner.warehousesTable`, `ClientTablePagination.install(owner.warehousesTable)`.
   - Métodos públicos canónicos preservados: `buildPanel()`, `refresh()`, `updateKpis()`, `createWarehouseDialogV2()`.
   - Meta de linhas: $\le 195$ linhas.

2. **`WarehouseFormDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal ergonómico baseado em `ModernFormDialog` para criação e edição de armazéns com suporte aos tipos `WarehouseType` moçambicanos.
   - Validação atómica de nome, código/número e capacidade $\ge 0$.

3. **`StockWarehouseViewCoordinator` (`mz.multicore.erp.gui`):**
   - Coordenador de operações visuais para configuração dinâmica do Quick Peek (`populatePeek`).
   - Cálculo e formatação agregada dos 4 KPIs executivos (`calculateKpis`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Contratos de Teste Existentes:**
  - `StockUnifiedHarnessTest` (STOCK-03): Presença de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(owner.warehousesTable)`, `TableQuickPeekController.install(owner.warehousesTable` e proibição de `whFilters = new JPanel(new GridBagLayout())`.
  - `StockWarehouseKpiHarnessTest` (H-WMS-01/02): Presença de `KpiCard.createGrid(4)` e dos 4 rótulos/ícones canónicos.
  - `TableActionPlacementHarnessTest`: Presença de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Manutenção de 0 chamadas a `JOptionPane`.
