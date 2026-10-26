# Plano de Testes de Harness: Decomposição de PosShiftHandoverDialog

- **Document ID**: HARNESS-POS-SHIFT-HANDOVER-001
- **Fase**: 74
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.pos.PosShiftHandoverDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **SHD-01** | Limites físicos estritos de linhas | `PosShiftHandoverDialog.java` $\le 190$ linhas; `PosShiftHandoverResultPanel.java` $\le 120$ linhas. |
| **SHD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **SHD-03** | Reutilização canónica de componentes | `PosShiftHandoverDialog.java` instancia e utiliza `PosCashBreakdownPanel` e `PosShiftHandoverResultPanel`. |
| **SHD-04** | Preservação de contratos de passagem de turno | `performHandover()`, `ShiftHandoverRequest`, `posApiClient.performShiftHandover` e listeners são mantidos. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=PosShiftHandoverDialogDecompositionHarnessTest,PosBlindCloseDialogDecompositionHarnessTest,DesktopThinContextTest"
```
