# Harness — demonstrações financeiras

**Data:** 2026-09-04  
**Estado:** implementado

| Caso | Garantia |
|---|---|
| CT-47 | Classes 7 e 6 são separadas e o resultado é proveitos menos custos |
| CT-48 | O balanço incorpora o resultado corrente e verifica a equação contabilística |
| CT-49 | Um período invertido é recusado com erro de negócio |
| ARQ | DTOs ficam em `contracts`; cálculo e persistência ficam exclusivamente no backend |
| UI | O desktop apenas consome os DTOs por HTTP e apresenta as duas demonstrações |

Comando:

```powershell
mvn -q "-Dtest=AccountingReportServiceTest,MultiModuleArchitectureHarnessTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```
