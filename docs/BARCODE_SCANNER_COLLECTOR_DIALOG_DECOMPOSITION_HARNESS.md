# Plano de Testes de Harness: Decomposição de BarcodeScannerCollectorDialog

- **Document ID**: HARNESS-BARCODE-SCANNER-COLLECTOR-001
- **Fase**: 80
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.components.BarcodeScannerCollectorDialogDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **BCD-01** | Limites físicos estritos de linhas | `BarcodeScannerCollectorDialog.java` $\le 180$ linhas; `BarcodeScannerProductDetailsCard.java` $\le 150$ linhas; `BarcodeScannerExecutionEngine.java` $\le 140$ linhas. |
| **BCD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes contém chamadas a `JOptionPane.`. |
| **BCD-03** | Preservação de enum `CollectorMode`, construtor e método `showDialog` | Assinatura e campos inalterados. |
| **BCD-04** | Delegação canónica | `BarcodeScannerCollectorDialog` delega a gestão do card de produto para `BarcodeScannerProductDetailsCard` e a lógica de processamento para `BarcodeScannerExecutionEngine`. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=BarcodeScannerCollectorDialogDecompositionHarnessTest,BarcodeScannerCollectorDialogHarnessTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
