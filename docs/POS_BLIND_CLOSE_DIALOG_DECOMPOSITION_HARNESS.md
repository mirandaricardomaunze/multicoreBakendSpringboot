# Plano de Testes de Harness: Decomposição de PosBlindCloseDialog

- **Document ID**: HARNESS-POS-BLIND-CLOSE-001
- **Fase**: 73
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.pos.PosBlindCloseDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **BCD-01** | Limites físicos estritos de linhas | `PosBlindCloseDialog.java` $\le 290$ linhas; `PosCashBreakdownPanel.java` $\le 270$ linhas; `PosBlindCloseResultPanel.java` $\le 140$ linhas. |
| **BCD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas directas a `JOptionPane.`. |
| **BCD-03** | Delegação canónica | `PosBlindCloseDialog.java` instancia e utiliza `PosCashBreakdownPanel` e `PosBlindCloseResultPanel`. |
| **BCD-04** | Preservação de contratos POS | `PosBlindCloseDialogHarnessTest` e `PosZReportUiHarnessTest` continuam aprovados com 100% de sucesso. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=PosBlindCloseDialogDecompositionHarnessTest,PosBlindCloseDialogHarnessTest,PosZReportUiHarnessTest,PrintModalHarnessTest,DesktopThinContextTest"
```
