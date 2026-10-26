# Plano de Testes de Harness: Decomposição de StockProductFormDialog

- **Document ID**: HARNESS-STOCK-PRODUCT-FORM-DIALOG-001
- **Fase**: 82
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.StockProductFormDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **SPFD-01** | Limites físicos estritos de linhas | `StockProductFormDialog.java` $\le 195$ linhas; `StockProductFormBuilder.java` $\le 150$ linhas; `StockProductFormValidator.java` $\le 140$ linhas. |
| **SPFD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das 3 classes contém chamadas a `JOptionPane.`. |
| **SPFD-03** | Preservação de métodos públicos estáticos de compatibilidade | Métodos `showCreate(...)` e `showEdit(...)` presentes e com assinaturas inalteradas. |
| **SPFD-04** | Delegação canónica | `StockProductFormDialog` delega a montagem de layout para `StockProductFormBuilder` e as validações para `StockProductFormValidator`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=StockProductFormDialogDecompositionHarnessTest,StockProductActionsDecompositionHarnessTest,StockInteractionHarnessTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
