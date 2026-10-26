# Plano de Testes de Harness: Decomposição de ShelfLabelsDialog

- **Document ID**: HARNESS-SHELF-LABELS-DIALOG-001
- **Fase**: 79
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.components.ShelfLabelsDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **SLD-01** | Limites físicos estritos de linhas | `ShelfLabelsDialog.java` $\le 210$ linhas; `ShelfLabelCardRenderer.java` $\le 160$ linhas; `ShelfLabelPrintEngine.java` $\le 85$ linhas. |
| **SLD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas a `JOptionPane.`. |
| **SLD-03** | Preservação de métodos públicos e enum | Enum `PackagingLevel` e métodos estáticos `createSingleLabelCard(...)`, `show(...)`, `showWithBatch(...)` presentes e funcionais. |
| **SLD-04** | Delegação canónica | `ShelfLabelsDialog` delega a renderização para `ShelfLabelCardRenderer` e impressão para `ShelfLabelPrintEngine`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=ShelfLabelsDialogDecompositionHarnessTest,ManufacturingLabelPrintingHarnessTest,ProductivitySuiteHarnessTest,PurchaseReceivingLabelPrintingHarnessTest,DesktopThinContextTest"
```
