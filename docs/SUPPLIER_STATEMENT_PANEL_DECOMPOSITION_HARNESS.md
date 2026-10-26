# Harness de Verificação de Decomposição Modular: SupplierStatementPanel

**Identificador:** `HARNESS-SUPPLIER-STATEMENT-PANEL-DECOMPOSITION-001`  
**Documento Relacionado:** [SUPPLIER_STATEMENT_PANEL_DECOMPOSITION_SPEC.md](SUPPLIER_STATEMENT_PANEL_DECOMPOSITION_SPEC.md)  
**Data:** 2026-10-24  

---

## 1. Escopo de Validação

Este harness valida a integridade estrutural, ergonomia, contratos canónicos e redução de linhas do painel `SupplierStatementPanel.java` e seus coordenadores colaboradores.

---

## 2. Matriz de Critérios

| ID | Descrição | Limite / Expectativa | Ficheiro de Teste |
|---|---|---|---|
| `SSP-01` | Redução do orquestrador principal | $\le 165$ linhas | `SupplierStatementPanelDecompositionHarnessTest.java` |
| `SSP-02` | Redução dos coordenadores colaboradores | $\le 110$ linhas cada | `SupplierStatementPanelDecompositionHarnessTest.java` |
| `SSP-03` | Contratos literais canónicos | `ModernPanel(16)`, `tableCardTop`, `ensureHeadersFit`, `KpiCard.createGrid(4)` | `SupplierStatementPanelDecompositionHarnessTest.java` |
| `SSP-04` | Eliminação total de diálogos legados | 0 chamadas a `JOptionPane` | `SupplierStatementPanelDecompositionHarnessTest.java` |
| `SSP-05` | API pública preservada | Construtor público e `SupplierComboItem` record | `SupplierStatementPanelDecompositionHarnessTest.java` |

---

## 3. Comandos de Execução

```powershell
Get-Process javaw -ErrorAction SilentlyContinue | Stop-Process -Force
mvn test -pl desktop "-Dtest=SupplierStatementPanelDecompositionHarnessTest,StatementsCashFlowUnifiedHarnessTest,KpiCardStandardizationTest,DesktopInitializationTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
