# HARNESS-STOCK-PANEL-DECOMPOSITION-001: Testes de Conformidade da Decomposição do StockPanel

## 1. Objectivo
Este documento define os critérios de teste e gates de qualidade aplicados à decomposição modular do [StockPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockPanel.java), em conformidade com [docs/STOCK_PANEL_DECOMPOSITION_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/STOCK_PANEL_DECOMPOSITION_SPEC.md).

---

## 2. Critérios de Conformidade (Gates de Qualidade)

| ID | Descrição do Teste / Invariante | Verificação |
| :--- | :--- | :--- |
| **STK-DEC-01** | Limite Físico de Linhas | `StockPanel.java` $\le 450$ linhas; `StockMovementsCoordinator.java` $\le 200$ linhas; `StockLockCoordinator.java` $\le 120$ linhas; `StockWarehouseCoordinator.java` $\le 160$ linhas. |
| **STK-DEC-02** | Zero `JOptionPane` | Ausência total de `JOptionPane` em todos os ficheiros desacoplados de stock. |
| **STK-DEC-03** | Retrocompatibilidade com Suites Existentes | Passagem de 100% dos testes em `StockUnifiedHarnessTest`, `StockInteractionHarnessTest`, `StockCommercialUiHarnessTest`, `StockInTransitUiHarnessTest` e `StockTransferDraftUiHarnessTest`. |
| **STK-DEC-04** | Integridade dos KPIs e Editor de Transferências | Preservação de `kpiTransfersSeparation`, `kpiTransfersPending`, `kpiTransfersInTransit`, `kpiTransfersCompleted`, `kpiTransfersTotalValue`, `transferEditorHost` e `transferEditorForm`. |
| **STK-DEC-05** | Inicialização em Tempo de Execução | `DesktopThinContextTest` executa com sucesso sem falhas no carregamento de dependências. |
| **STK-DEC-06** | Integridade Multi-Módulo | `MultiModuleArchitectureHarnessTest` no backend valida que não há quebra de fronteiras arquiteturais. |

---

## 3. Matriz de Execução

```powershell
# 1. Validação unitária e de regressão dos painéis desktop
mvn test -pl desktop "-Dtest=StockPanelDecompositionHarnessTest,StockUnifiedHarnessTest,StockInteractionHarnessTest,StockCommercialUiHarnessTest,StockInTransitUiHarnessTest,StockTransferDraftUiHarnessTest,DesktopThinContextTest"

# 2. Validação de arquitetura multi-módulo
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
