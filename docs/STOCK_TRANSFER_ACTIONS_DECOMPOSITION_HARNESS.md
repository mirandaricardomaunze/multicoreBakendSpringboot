# Plano de Testes de Harness: Decomposição de StockTransferActions

- **Document ID**: HARNESS-STOCK-TRANSFER-ACTIONS-001
- **Fase**: 68
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.StockTransferActionsDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **STAD-01** | Limites físicos estritos de linhas | `StockTransferActions.java` $\le 360$ linhas; `StockTransferLinesDialog.java` $\le 190$ linhas; `StockTransferCreateModalDialog.java` $\le 450$ linhas. |
| **STAD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **STAD-03** | Delegação canónica | `StockTransferActions.java` delega para `StockTransferLinesDialog.show(...)` e `StockTransferCreateModalDialog.show(...)`. |
| **STAD-04** | Preservação de contratos de stock | Testes `StockTransferReceiptDiscrepancyUiHarnessTest`, `StockInTransitUiHarnessTest`, `StockTransferDraftUiHarnessTest` continuam a passar sem erros. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=StockTransferActionsDecompositionHarnessTest,StockTransferReceiptDiscrepancyUiHarnessTest,StockInTransitUiHarnessTest,StockTransferDraftUiHarnessTest"
```
