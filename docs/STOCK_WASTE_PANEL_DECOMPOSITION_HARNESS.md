# HARNESS-STOCK-WASTE-PANEL-DECOMPOSITION-001: Testes de Conformidade da Decomposição do StockWastePanel

## 1. Objectivo
Este documento define a suite de testes e os critérios de validação estrutural e comportamental aplicados à decomposição modular do [StockWastePanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockWastePanel.java), conforme a especificação [docs/STOCK_WASTE_PANEL_DECOMPOSITION_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/STOCK_WASTE_PANEL_DECOMPOSITION_SPEC.md).

---

## 2. Critérios de Conformidade (Gates de Qualidade)

| ID | Descrição do Teste / Invariante | Verificação |
| :--- | :--- | :--- |
| **SWP-DEC-01** | Limite Físico de Linhas | `StockWastePanel.java` $\le 380$ linhas; `StockWasteRegistrationDialog.java` $\le 200$ linhas; `StockWasteRadarView.java` $\le 190$ linhas; `StockWasteMetricsView.java` $\le 180$ linhas. |
| **SWP-DEC-02** | Zero `JOptionPane` | Nenhum ficheiro desacoplado utiliza `JOptionPane` (uso estrito de `ModernFormDialog` ou `ToastManager`). |
| **SWP-DEC-03** | Retrocompatibilidade com Suite Existente | `StockWastePanelHarnessTest` (GQS-01 a GQS-08) passa 100% sem alterações. |
| **SWP-DEC-04** | Integridade dos KPIs e Tabelas | KPIs (`kpiTotalCost`, `kpiTotalQty`, `kpiWasteRate`, `kpiRiskBatches`) e tabelas (`wasteTable` 13 cols, `radarTable` 10 cols, altura 42px) mantêm acessibilidade. |
| **SWP-DEC-05** | Fluxo de Aprovação e Alçada | Linhas com estado `PENDING_APPROVAL` habilitam botões de aprovação/rejeição; perdas > 2.500 MT geram estado pendente. |
| **SWP-DEC-06** | Arranque Desktop Seguro | `DesktopThinContextTest` executa com sucesso sem falhas no carregamento de dependências. |
| **SWP-DEC-07** | Fronteira Arquitetural Multi-Módulo | `MultiModuleArchitectureHarnessTest` no backend valida que não há vazamento entre contratos, backend e desktop. |

---

## 3. Matriz de Execução

```powershell
# 1. Validação unitária e de regressão dos painéis desktop
mvn test -pl desktop "-Dtest=StockWastePanelDecompositionHarnessTest,StockWastePanelHarnessTest,StockUnifiedHarnessTest,UniversalPeriodFilterHarnessTest,KpiCardStandardizationTest,DesktopInitializationTest,DesktopThinContextTest"

# 2. Validação de arquitetura multi-módulo
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
