# HARNESS-ACCOUNTING-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Decomposição do Painel de Contabilidade

## 1. Objectivo do Harness

Garantir que a decomposição modular do painel `AccountingPanel.java` em abas corporativas especializadas preserva:
1. Todos os contratos de integração com `MainFrame` e `AccountingApiClient`.
2. As expectativas de testes de auditoria existentes: `AccountingFiscalUnifiedHarnessTest`, `TableActionPlacementHarnessTest`, `DesktopInitializationTest`, `DesktopThinContextTest`, `ProfessionalFeedbackHarnessTest` e `MultiModuleArchitectureHarnessTest`.
3. A conformidade visual de topo de card, atalhos de teclado e ausência de emojis Unicode.

---

## 2. Casos de Teste do Harness (`AccountingPanelDecompositionHarnessTest`)

| ID | Teste | Objetivo | Critério de Sucesso |
|---|---|---|---|
| TC-ACC-01 | `testLineCount` | Verificar que `AccountingPanel.java` foi reduzido | Linhas $\le 120$ |
| TC-ACC-02 | `testTabsLineCounts` | Verificar tamanho das novas abas especializadas | Cada aba $\le 150$ linhas |
| TC-ACC-03 | `testZeroLegacyDialogs` | Garantir ausência total de `JOptionPane` | Ocorrências de `JOptionPane` = 0 |
| TC-ACC-04 | `testConstructorsAndLifecycle` | Garantir instanciação e ciclo de vida | Construtor de 1 argumento e `onPanelSelected()` |
| TC-ACC-05 | `testCanonicalCardStructureAndNoUnicode` | Verificar `tableCardTop`, `ensureHeadersFit` e sem emojis | Assertions satisfeitas |

---

## 3. Matriz de Testes de Regressão

1. `AccountingPanelDecompositionHarnessTest` (Novo)
2. `AccountingFiscalUnifiedHarnessTest` (Desktop - conformidade canónica e ensureHeadersFit)
3. `TableActionPlacementHarnessTest` (Desktop - topo de card canónico e regras anti-rodapé)
4. `DesktopInitializationTest` (Desktop - arranque limpo do painel de contabilidade)
5. `DesktopThinContextTest` (Desktop - integridade de cliente fino sem DB)
6. `ProfessionalFeedbackHarnessTest` (Desktop - conformidade corporativa de diálogos)
7. `MultiModuleArchitectureHarnessTest` (Backend - separação de camadas do reactor)
