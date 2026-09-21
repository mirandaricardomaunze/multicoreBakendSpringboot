# HARNESS — Validação Ergonómica e Visual do POS

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing  
**Classe de teste:** `mz.multicore.erp.gui.PosErgonomicsHarnessTest`

---

## Critérios de Conformidade Automatizados

| ID | Regra | Teste Automatizado | Estado |
|---|---|---|:---:|
| **POS-01** | A aba de venda ativa do POS não possui cartões KPI volumosos no topo, reservando a altura para o catálogo e carrinho. | `testSalesTabHeaderIsCompactWithoutKpis` | ✅ |
| **POS-02** | Os cartões de produto possuem dimensões compactas padronizadas (`CARD_IMAGE_WIDTH <= 85`, `CARD_IMAGE_HEIGHT <= 45`). | `testProductCardDimensions` | ✅ |
| **POS-03** | O bloco de total a pagar no carrinho apresenta destaque visual e formatação monetária correta. | `testCartTotalHierarchy` | ✅ |
| **POS-04** | Os atalhos de teclado operacionais (`F2`, `F4`, `F6`, `F9`, `Delete`) estão registados no InputMap do POS. | `testOperationalKeyboardShortcuts` | ✅ |
| **POS-05** | `POSPanel.java` mantém rigorosamente menos de 1000 linhas de código. | `UiPanelDecompositionTest` | ✅ |
| **POS-06** | Nenhum literal `new Color(...)` foi introduzido no pacote `gui`. | `FinalUiUniformityHarnessTest` | ✅ |
