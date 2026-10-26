# Plano de Testes de Harness: Decomposição de CommercialOrdersView

- **Document ID**: HARNESS-COMMERCIAL-ORDERS-VIEW-001
- **Fase**: 67
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.CommercialOrdersViewDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **COVD-01** | Limites físicos estritos de linhas | `CommercialOrdersView.java` $\le 360$ linhas; `CommercialOrderHeaderForm.java` $\le 210$ linhas. |
| **COVD-02** | Zero diálogos legados `JOptionPane` | Ambas as classes não contêm chamadas directas a `JOptionPane.`. |
| **COVD-03** | Delegação de cabeçalho | `CommercialOrdersView.java` delega para `CommercialOrderHeaderForm.createHeaderCard(owner)`. |
| **COVD-04** | Preservação de contratos existentes | Testes `OrderEditorHarnessTest`, `PhcInlineDocumentGridHarnessTest`, `StockCommercialUiHarnessTest` e `DocumentTableInteractionHarnessTest` continuam a passar sem erros. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=CommercialOrdersViewDecompositionHarnessTest,OrderEditorHarnessTest,PhcInlineDocumentGridHarnessTest,StockCommercialUiHarnessTest,DocumentTableInteractionHarnessTest"
```
