# HARNESS-FLEET-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Decomposição do Painel de Frotas

## 1. Objectivo do Harness

Garantir que a decomposição modular de `FleetPanel.java` preserva:
1. Os contratos e asserções testados por `GuiActionAlertGuidanceHarnessTest` (método `showFuelHistory()`, campos `table` e `feedback`).
2. O topo de card canónico verificado por `TableActionPlacementHarnessTest` (`UIHelper.tableCardTop`).
3. A ausência de regressões no contexto desktop (`DesktopThinContextTest`, `ProfessionalFeedbackHarnessTest`) e na arquitetura do reactor (`MultiModuleArchitectureHarnessTest`).

---

## 2. Casos de Teste do Harness (`FleetPanelDecompositionHarnessTest`)

| ID | Teste | Objetivo | Critério de Sucesso |
|---|---|---|---|
| TC-FLT-01 | `testLineCount` | Verificar que `FleetPanel.java` foi reduzido | Linhas $\le 240$ |
| TC-FLT-02 | `testComponentLineCounts` | Verificar tamanho dos novos colaboradores | `VehicleFormDialog` $\le 135$, `FuelOperationsCoordinator` $\le 180$ |
| TC-FLT-03 | `testZeroLegacyDialogs` | Garantir ausência total de `JOptionPane` | Ocorrências de `JOptionPane` = 0 |
| TC-FLT-04 | `testRequiredFieldsAndMethods` | Garantir presença de `table`, `feedback` e `showFuelHistory()` | Campos e métodos presentes |
| TC-FLT-05 | `testNoUnicodeEmojis` | Garantir ausência de emojis Unicode em código UI | Emojis = 0 em todas as classes |

---

## 3. Matriz de Testes de Regressão

1. `FleetPanelDecompositionHarnessTest` (Novo)
2. `GuiActionAlertGuidanceHarnessTest` (Desktop - alerta ao consultar histórico sem viatura selecionada)
3. `TableActionPlacementHarnessTest` (Desktop - topo de card canónico)
4. `DesktopThinContextTest` (Desktop - integridade de cliente fino sem DB)
5. `ProfessionalFeedbackHarnessTest` (Desktop - conformidade corporativa)
6. `MultiModuleArchitectureHarnessTest` (Backend - pureza de módulos Maven)
