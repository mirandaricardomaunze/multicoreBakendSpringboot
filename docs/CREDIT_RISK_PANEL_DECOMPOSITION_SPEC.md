# Especificação de Decomposição Modular: Matriz de Risco de Crédito & Aging (CreditRiskPanel)

**Identificador:** `SPEC-CREDIT-RISK-PANEL-DECOMPOSITION-002`  
**Domínio:** Comercial / Gestão Financeira de Clientes / Risco de Crédito  
**Alvo:** `mz.multicore.erp.gui.CreditRiskPanel.java` (283 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 42\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `CreditRiskPanel.java` é o centro executivo de monitorização de risco de crédito, baldes de aging (corrente, 1-30d, 31-60d, 61-90d, >90d), suspensão/bloqueio automático de crédito a clientes e emissão de Cartas Formais de Cobrança em PDF.

Anteriormente, o painel foi parcialmente decomposto com `CreditRiskOperationsCoordinator` (operações assíncronas de cobrança e busca remota) e `CreditRiskCellRenderer`. Contudo, acumulava ainda 283 linhas com:
1. **Lógica de Filtragem e Baldes de Risco:** `applyFilters`, `matchesRisk` e conversão analítica de linhas monetárias `toRowData` (60 linhas).
2. **Atualização de KPIs:** `updateKpis` com formatação monetária MZN de saldos em carteira e contadores de clientes bloqueados/críticos.
3. **Povoamento e Configuração do Quick Peek:** `populateQuickPeek` com atalho direto à ficha de cobrança.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Campos Públicos e de Acesso para Testes (`CreditRiskPanelDecompositionHarnessTest` & `CreditRiskPanelHarnessTest`):**
   - Preservar construtor público `CreditRiskPanel(CreditRiskApiClient creditRiskApiClient)`.
   - Preservar método público `refreshData()`.
   - Preservar campos `kpiTotalReceivable`, `kpiTotalOverdue`, `kpiBlockedCount`, `kpiCriticalCount`.
   - Preservar campos `dateField`, `riskFilterCombo`, `searchField`, `tableModel`, `table`, `allClientsRisk`, `visibleClientsRisk`.
   - Preservar métodos package-private `updateKpis(CreditRiskSummaryDTO summary)` e `applyFilters()`.
2. **Contratos Visuais & Invariantes de Tabela:**
   - Preservar `UIHelper.tableCardTop("Matriz de Aging & Gestão de Risco", filterBar, busyBar, refreshBtn, pdfBtn, exceptionBtn)`.
   - Preservar `TableQuickPeekController.install(table, card, ...)`.
   - Preservar `UIHelper.ensureHeadersFit(table)`.
   - Preservar `ClientTablePagination.install(table)`.
   - Preservar `ModernPanel(16)` e `KpiCard.createGrid(4)`.
3. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── CreditRiskPanel.java                       (Orquestrador principal: <= 165 linhas)
│   ├── buildKpiBar(), buildMainCard(), refreshData()
│   ├── updateKpis(summary), applyFilters()
├── CreditRiskOperationsCoordinator.java       (Operações assíncronas e PDF: 89 linhas)
├── CreditRiskCellRenderer.java                (Renderizador visual da matriz de aging)
└── CreditRiskViewCoordinator.java             (Apresentação, filtragem e quick peek: <= 110 linhas)
    ├── updateKpis(panel, summary)
    ├── applyFilters(panel)
    ├── matchesRisk(c, filter)
    ├── toRowData(c)
    └── populateQuickPeek(peek, mr, panel)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `CreditRiskPanel.java` | $\le 165$ linhas | Grid de KPIs, estrutura do card, controlos de filtro e listeners |
| **Coordenador de Visualização** | `CreditRiskViewCoordinator.java` | $\le 110$ linhas | Lógica de filtros, baldes de aging, formatação de linhas e QuickPeek |
