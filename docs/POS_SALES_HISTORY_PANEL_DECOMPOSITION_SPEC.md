# Especificação de Decomposição Modular: Histórico de Vendas POS (PosSalesHistoryPanel)

**Identificador:** `SPEC-POS-SALES-HISTORY-PANEL-DECOMPOSITION-001`  
**Domínio:** Ponto de Venda (POS) / Histórico de Vendas e Fecho de Caixa  
**Alvo:** `mz.multicore.erp.gui.PosSalesHistoryPanel.java` (293 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 44\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `PosSalesHistoryPanel.java` é responsável pela visualização, pesquisa paginada remota (`TablePager`) e auditoria de vendas realizadas nos postos de venda (POS), bem como reimpressão sensível de talões de venda (`SensitivePrintCoordinator`) e acionamento de devoluções/trocas (`PosReturnDialog`).

Actualmente, o painel acumulava:
1. **Coordenação de KPIs & Chips de Resumo:** Construção de chips de contadores (`buildKpiBar`, `createSummaryChip`), cálculo de variações percentuais e formatação analítica de totais.
2. **Povoamento e Mapeamento de Linhas:** Mapeamento de `InvoiceDTO` para linhas de tabela, cálculo do total da página local versus total do servidor, e sincronização dinâmica do seletor de linhas de negócio.
3. **Cálculo de Períodos:** Mapeamento do combo de períodos para intervalos de data (`DateRange`).
4. **Orquestração da Interface:** Tabela de histórico de 8 colunas, filtros com `TableFilter.bar`, paginação remota com `TablePager` e card `ModernPanel(16)`.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`CommercialCrmUnifiedHarnessTest` (`COMM-03`):**
   - Não conter `shBar = new JPanel(new GridBagLayout())`.
   - Conter `TableFilter.bar(`.
   - Conter `UIHelper.ensureHeadersFit(owner.salesHistoryTable)`.
2. **`BrightButtonsHarnessTest`:**
   - Proibido conter `createSecondaryButton`.
   - Proibido conter `BUTTON_NEUTRAL`.
3. **Contratos Visuais & Invariantes:**
   - Preservar construtor package-private `PosSalesHistoryPanel(POSPanel owner)`.
   - Preservar métodos públicos `buildPanel()` e `refresh()`.
   - Preservar `ModernPanel(16)`.
4. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── PosSalesHistoryPanel.java                     (Orquestrador principal: <= 165 linhas)
│   ├── buildPanel() [ModernPanel(16), TableFilter.bar, ensureHeadersFit, TablePager]
│   ├── refresh(), loadPage(page, size)
└── PosSalesHistoryViewCoordinator.java           (KPIs, resumo e mapeamento: <= 135 linhas)
    ├── buildKpiBar(...)
    ├── applySalesHistory(...)
    ├── applySummary(...)
    └── selectedDateRange(...)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `PosSalesHistoryPanel.java` | $\le 165$ linhas | Layout, tabela, filtros, paginação e acções de botões |
| **Coordenador de Visualização** | `PosSalesHistoryViewCoordinator.java` | $\le 135$ linhas | KPIs, chips de resumo, mapeamento de tabela e filtros |
