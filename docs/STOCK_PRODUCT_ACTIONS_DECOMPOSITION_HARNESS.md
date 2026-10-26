# Plano de Testes de Harness: Decomposição de StockProductActions

- **Document ID**: HARNESS-STOCK-PRODUCT-ACTIONS-001
- **Fase**: 70
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.StockProductActionsDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **SPAD-01** | Limites físicos estritos de linhas | `StockProductActions.java` $\le 120$ linhas; `StockBatchEntryDialog.java` $\le 190$ linhas; `StockProductFormDialog.java` $\le 450$ linhas. |
| **SPAD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **SPAD-03** | Delegação canónica | `StockProductActions.java` delega para `StockBatchEntryDialog.show(...)` e `StockProductFormDialog.showCreate(...)` / `StockProductFormDialog.showEdit(...)`. |
| **SPAD-04** | Preservação de contratos do `StockInteractionHarnessTest` | Presença de `.setConfirmButton("Actualizar", "fas-save")` e `dialog.setOnSaveAsync` em `StockProductActions.java`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=StockProductActionsDecompositionHarnessTest,StockInteractionHarnessTest,ProfessionalFeedbackHarnessTest"
```
