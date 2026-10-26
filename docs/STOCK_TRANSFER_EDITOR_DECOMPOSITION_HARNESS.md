# Plano de Testes de Harness: Decomposição de StockTransferEditorForm

- **Document ID**: HARNESS-STOCK-TRANSFER-EDITOR-001
- **Fase**: 69
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.StockTransferEditorFormDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **STED-01** | Limites físicos estritos de linhas | `StockTransferEditorForm.java` $\le 550$ linhas; `StockTransferReplenishmentHelper.java` $\le 130$ linhas. |
| **STED-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **STED-03** | Delegação canónica | `StockTransferEditorForm.java` delega para `StockTransferReplenishmentHelper.suggest(...)`. |
| **STED-04** | Preservação de contratos do editor PHC | Testes `StockTransferDraftUiHarnessTest` e `StockInTransitUiHarnessTest` continuam a passar sem erros. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=StockTransferEditorFormDecompositionHarnessTest,StockTransferDraftUiHarnessTest,StockInTransitUiHarnessTest"
```
