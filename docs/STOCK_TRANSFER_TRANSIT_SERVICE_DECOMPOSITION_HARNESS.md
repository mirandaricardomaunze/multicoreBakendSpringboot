# Harness de Decomposição - StockTransferTransitService (Fase 120)

## 1. Objectivo

Validar a decomposição de `StockTransferTransitService.java` em ledger, despacho, validador e recepção, garantindo limites de linhas, backend headless, delegação canónica e preservação de contratos.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **STTD-01** | Limites físicos de linhas | Fachada $\le 110$, `StockTransferReceiptService` $\le 155$, `StockTransferReceiptValidator` $\le 105$, `StockTransferDispatchService` $\le 85$, `StockTransitLedger` $\le 80$. |
| **STTD-02** | Backend headless | Nenhuma referência a `javax.swing` ou `JOptionPane`. |
| **STTD-03** | Delegação canónica | Fachada delega em `dispatchService` / `receiptService` e não toca em `stockRepository`; ledger usa `findForReservation`; recepção mantém `PermissionGuard` e `lockByIdAndCompanyId`. |
| **STTD-04** | Contratos preservados | `dispatch`, `receive`, `findInTransit`, construtor retrocompatível de 5 dependências e `@Autowired`; zero `BigDecimal.ZERO.equals`. |

## 3. Regressão funcional

`StockTransferInTransitHarnessTest`, `StockTransferReceiptDiscrepancyHarnessTest`, `MultiUserRobustnessHarnessTest`, `MultiModuleArchitectureHarnessTest`.
