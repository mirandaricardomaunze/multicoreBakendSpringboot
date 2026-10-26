# Plano de Testes de Harness: Decomposição de CommercialOrderEditorActions

- **Document ID**: HARNESS-COMMERCIAL-ORDER-ACTIONS-001
- **Fase**: 81
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.CommercialOrderEditorActionsDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **COEA-01** | Limites físicos estritos de linhas | `CommercialOrderEditorActions.java` $\le 210$ linhas; `CommercialOrderCreditPolicy.java` $\le 120$ linhas; `CommercialOrderLifecycleCoordinator.java` $\le 75$ linhas; `CommercialOrderBarcodeAndLookupHandler.java` $\le 110$ linhas. |
| **COEA-02** | Zero diálogos legados `JOptionPane` | Nenhuma das 4 classes contém chamadas a `JOptionPane.`. |
| **COEA-03** | Preservação de métodos públicos estáticos de compatibilidade | Todos os métodos estáticos canónicos presentes em `CommercialOrderEditorActions` (`addBlankLine`, `syncLineFromGrid`, `openSelected`, etc.). |
| **COEA-04** | Delegação canónica | `CommercialOrderEditorActions` delega crédito para `CommercialOrderCreditPolicy`, ciclo de vida para `CommercialOrderLifecycleCoordinator` e scanner/lookup para `CommercialOrderBarcodeAndLookupHandler`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=CommercialOrderEditorActionsDecompositionHarnessTest,OrderEditorHarnessTest,PhcDocumentEditorHomologationHarnessTest,PhcInlineDocumentGridHarnessTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
