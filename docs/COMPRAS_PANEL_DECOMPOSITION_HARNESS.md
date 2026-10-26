# Harness Canónico: Decomposição de `ComprasPanel` (Fase 9)

**Identificador:** `HARNESS-COMPRAS-PANEL-DECOMPOSITION-001`  
**Referência:** [COMPRAS_PANEL_DECOMPOSITION_SPEC.md](COMPRAS_PANEL_DECOMPOSITION_SPEC.md)  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Casos de Teste do Harness

O harness `ComprasPanelDecompositionHarnessTest` valida as garantias arquiteturais e operacionais da decomposição:

| ID | Cenário | Validação |
| :--- | :--- | :--- |
| `decomp01_strictLineLimits` | Limites estritos de linhas de código | `ComprasPanel.java` $\le 450$, `PurchaseMetadataCoordinator.java` $\le 160$, `PurchaseInvoiceOperations.java` $\le 190$. |
| `decomp02_zeroLegacyDialogs` | Ausência de diálogos modais bloqueadores | 0 chamadas a `JOptionPane` em todos os ficheiros do domínio de compras. |
| `decomp03_tabStructurePreserved` | Integridade das abas corporativas | `JTabbedPane` com as abas de Faturas de Compra, Reposição, Encomendas, Contas a Pagar, Fornecedores e Negócios. |
| `decomp04_packageEditorPreserved` | Editor canónico de embalagem preservado | `PackageQuantityEditor` presente no formulário de linhas de compra. |

---

## 2. Comandos de Verificação

```powershell
mvn test -pl desktop "-Dtest=ComprasPanelDecompositionHarnessTest,DocumentPackageQuantityCoverageHarnessTest,TableActionPlacementHarnessTest,ProfessionalFeedbackHarnessTest,DesktopInitializationTest,AccountStatementPanelHarnessTest,DesktopThinContextTest"
mvn surefire:test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
