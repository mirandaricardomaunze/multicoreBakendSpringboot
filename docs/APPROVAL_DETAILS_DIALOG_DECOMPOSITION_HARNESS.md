# Plano de Testes de Harness: Decomposição de ApprovalDetailsDialog

- **Document ID**: HARNESS-APPROVAL-DETAILS-DIALOG-001
- **Fase**: 76
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.ApprovalDetailsDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **ADD-01** | Limites físicos estritos de linhas | `ApprovalDetailsDialog.java` $\le 190$ linhas; `ApprovalDocumentDetailsView.java` $\le 185$ linhas. |
| **ADD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **ADD-03** | Delegação canónica | `ApprovalDetailsDialog.java` delega para `ApprovalDocumentDetailsView.buildTab(...)`. |
| **ADD-04** | Preservação de métodos públicos e contratos | Os métodos `show(...)`, tratamento de aprovação/rejeição e listeners continuam presentes. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=ApprovalDetailsDialogDecompositionHarnessTest,ApprovalsPanelDecompositionHarnessTest,DesktopThinContextTest"
```
