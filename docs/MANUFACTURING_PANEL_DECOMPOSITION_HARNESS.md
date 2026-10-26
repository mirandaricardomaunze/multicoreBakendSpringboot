# HARNESS-MANUFACTURING-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Decomposição do Painel de Produção

## 1. Objectivo do Harness

Garantir que a decomposição modular do painel `ManufacturingPanel.java` em colaboradores especializados (`ProductionOrderOperationsCoordinator.java` e `BomFormDialog.java`) preserva:
1. Todos os contratos de integração com `MainFrame` e clientes de API (`ProductionApiClient`, `ComercialApiClient`, `InventoryApiClient`).
2. As expectativas de testes de auditoria existentes: `GuiActionAlertGuidanceHarnessTest`, `ManufacturingLabelPrintingHarnessTest`, `DesktopThinContextTest`, `ProfessionalFeedbackHarnessTest` e `MultiModuleArchitectureHarnessTest`.
3. A integridade visual, ergonómica e de acessibilidade do painel.

---

## 2. Casos de Teste do Harness (`ManufacturingPanelDecompositionHarnessTest`)

| ID | Teste | Objetivo | Critério de Sucesso |
|---|---|---|---|
| TC-MFG-01 | `testLineCount` | Verificar que `ManufacturingPanel.java` foi reduzido | Linhas $\le 260$ |
| TC-MFG-02 | `testComponentLineCounts` | Verificar tamanho dos novos colaboradores | `ProductionOrderOperationsCoordinator` $\le 260$, `BomFormDialog` $\le 130$ |
| TC-MFG-03 | `testZeroLegacyDialogs` | Garantir ausência total de `JOptionPane` | Ocorrências de `JOptionPane` = 0 |
| TC-MFG-04 | `testRequiredFieldsAndMethods` | Garantir presença dos campos e métodos de teste | `ordersTable`, `feedback`, `showOrderActions()`, construtor de 3 args |
| TC-MFG-05 | `testNoUnicodeEmojis` | Garantir ausência de emojis Unicode em código UI | Emojis = 0 em todas as classes de produção |

---

## 3. Matriz de Testes de Regressão

1. `ManufacturingPanelDecompositionHarnessTest` (Novo)
2. `GuiActionAlertGuidanceHarnessTest` (Desktop - alerta quando nenhuma OP selecionada)
3. `ManufacturingLabelPrintingHarnessTest` (Desktop - dimensões de etiquetas de lote e OP)
4. `DesktopThinContextTest` (Desktop - inicialização limpa do `MainFrame` e beans)
5. `ProfessionalFeedbackHarnessTest` (Desktop - conformidade de diálogo corporativo)
6. `MultiModuleArchitectureHarnessTest` (Backend - pureza de módulos Maven)
