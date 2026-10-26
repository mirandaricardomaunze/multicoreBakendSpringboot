# Plano de Testes de Harness: Decomposição de QuotationEditorForm

- **Document ID**: HARNESS-QUOTATION-EDITOR-001
- **Fase**: 71
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.commercial.QuotationEditorFormDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **QED-01** | Limites físicos estritos de linhas | `QuotationEditorForm.java` $\le 380$ linhas; `QuotationEditorHeaderForm.java` $\le 210$ linhas; `QuotationEditorRowCalculator.java` $\le 110$ linhas. |
| **QED-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **QED-03** | Delegação canónica | `QuotationEditorForm.java` delega cabeçalho para `QuotationEditorHeaderForm` e cálculos para `QuotationEditorRowCalculator`. |
| **QED-04** | Preservação de contratos PHC | Testes `EditableDocumentEditorsHarnessTest`, `PhcDocumentEditorHomologationHarnessTest`, `DocumentTableInteractionHarnessTest` e `PhcInlineDocumentGridHarnessTest` continuam a passar sem erros. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=QuotationEditorFormDecompositionHarnessTest,EditableDocumentEditorsHarnessTest,PhcDocumentEditorHomologationHarnessTest,DocumentTableInteractionHarnessTest,PhcInlineDocumentGridHarnessTest"
```
