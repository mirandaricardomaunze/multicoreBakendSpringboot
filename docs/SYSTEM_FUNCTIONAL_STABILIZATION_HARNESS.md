# Harness de Conformidade — Estabilização Funcional

**Código:** `HARNESS-SFS-001`  
**Referência:** `docs/SYSTEM_FUNCTIONAL_STABILIZATION_SPEC.md`

| ID | Critério | Cobertura |
|---|---|---|
| SFS-01 | Último administrador activo não pode perder papel, estado ou acesso | `TenantIsolationIntegrationTest`, `AppUserServiceTest` |
| SFS-02 | Auditoria e previsão exigem `MANAGER/ADMIN` | `SecurityPermissionGuardCoverageTest`, harnesses funcionais dos serviços |
| SFS-03 | Oito períodos e limites retrospectivos | `TableFilterHarnessTest`, `UniversalPeriodFilterHarnessTest` |
| SFS-04 | Matriz de tesouraria mostra imediatamente os dados recebidos | `CashFlowForecastPanelHarnessTest` |
| SFS-05 | Estado vazio e selecção filtrada respeitam EDT/modelo/vista | `TableUxTest`, `StockWastePanelHarnessTest` |
| SFS-06 | PDFs passam pela pré-visualização, inclusive em ecrãs decompostos | `PrintModalHarnessTest` |
| SFS-07 | Funcionalidades novas não aumentam `JOptionPane` legado | `ProfessionalFeedbackHarnessTest` |
| SFS-08 | Rótulos e cores semânticas do POS seguem a versão actual | `PosButtonColourHierarchyTest`, `StockInteractionHarnessTest` |
| SFS-09 | Fronteiras Maven continuam isoladas | `MultiModuleArchitectureHarnessTest` |

## Execução focada

```powershell
mvn -q -pl backend -Dtest=TenantIsolationIntegrationTest,SecurityPermissionGuardCoverageTest,ForensicAuditHarnessTest,CashFlowForecastHarnessTest,MultiModuleArchitectureHarnessTest test
mvn -q -pl desktop -am -Dtest=CashFlowForecastPanelHarnessTest,TableFilterHarnessTest,UniversalPeriodFilterHarnessTest,TableUxTest,StockWastePanelHarnessTest,PrintModalHarnessTest,ProfessionalFeedbackHarnessTest,PosButtonColourHierarchyTest,StockInteractionHarnessTest test
```
