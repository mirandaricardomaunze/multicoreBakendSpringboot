# Plano de Testes de Harness: Decomposição de HREmployeeActions

- **Document ID**: HARNESS-HR-EMPLOYEE-ACTIONS-001
- **Fase**: 72
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.HREmployeeActionsDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **EAD-01** | Limites físicos estritos de linhas | `HREmployeeActions.java` $\le 100$ linhas; `HREmployeeSalaryDialog.java` $\le 180$ linhas; `HREmployeeDocumentsDialog.java` $\le 160$ linhas; `HREmployeeOccupationalHealthDialog.java` $\le 320$ linhas. |
| **EAD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **EAD-03** | Delegação canónica | `HREmployeeActions.java` delega para `HREmployeeSalaryDialog`, `HREmployeeDocumentsDialog` e `HREmployeeOccupationalHealthDialog`. |
| **EAD-04** | Preservação de contratos dos drivers de ecrã | Métodos públicos/package-private de `HREmployeeActions` (`openSalaryHistory`, `openDocuments`, `openOccupationalHealth`, `openOccupationalHealthForm`, `openOccupationalHealthCompliance`, `payOccupationalHealthExam`, `openOccupationalHealthAttachment`, `justifyAbsence`) permanecem acessíveis e funcionais. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=HREmployeeActionsDecompositionHarnessTest,ProfessionalFeedbackHarnessTest,DesktopThinContextTest"
```
