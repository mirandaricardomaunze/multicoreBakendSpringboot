# Harness de Verificação de Decomposição Modular: PhysicalInventoryPanel

**Identificador:** `HARNESS-PHYSICAL-INVENTORY-PANEL-DECOMPOSITION-002`  
**Documento Relacionado:** [PHYSICAL_INVENTORY_PANEL_DECOMPOSITION_SPEC.md](PHYSICAL_INVENTORY_PANEL_DECOMPOSITION_SPEC.md)  
**Data:** 2026-10-24  

---

## 1. Escopo de Validação

Validação estrutural, ergonómica e de conformidade do painel `PhysicalInventoryPanel.java` e seus coordenadores desacoplados no pacote `mz.multicore.erp.gui.inventory`.

---

## 2. Matriz de Critérios

| ID | Descrição | Limite / Expectativa | Ficheiro de Teste |
|---|---|---|---|
| `ID-01` | Redução do orquestrador principal | $\le 200$ linhas | `PhysicalInventoryPanelDecompositionHarnessTest.java` |
| `ID-02` | Redução dos coordenadores colaboradores | $\le 160$, $\le 120$ e $\le 110$ linhas | `PhysicalInventoryPanelDecompositionHarnessTest.java` |
| `ID-03` | Ausência total de diálogos legados | 0 chamadas a `JOptionPane` | `PhysicalInventoryPanelDecompositionHarnessTest.java` |
| `ID-04` | Métodos essenciais preservados | `getItemsTable()` e `handleBarcodeScan()` | `PhysicalInventoryPanelDecompositionHarnessTest.java` |
| `ID-05` | Conformidade canónica e ausência de emojis | `tableCardTop`, `createGrid(4)`, sem Unicode | `PhysicalInventoryPanelDecompositionHarnessTest.java` |
| `PIH-01..04` | Testes de Layout e Ações | Card único, KPIs no topo, máx. 3 botões | `PhysicalInventoryPanelHarnessTest.java` |

---

## 3. Comandos de Execução

```powershell
Get-Process javaw -ErrorAction SilentlyContinue | Stop-Process -Force
mvn test -pl desktop "-Dtest=PhysicalInventoryPanelDecompositionHarnessTest,PhysicalInventoryPanelHarnessTest,GuiActionAlertGuidanceHarnessTest,TableActionPlacementHarnessTest,DesktopInitializationTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
