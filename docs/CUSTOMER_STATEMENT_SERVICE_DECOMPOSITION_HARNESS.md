# Harness de Decomposição - CustomerStatementService (Fase 123)

## 1. Objectivo

Validar estrutural e funcionalmente a decomposição de `CustomerStatementService.java` em componentes especializados de coleta de eventos de conta corrente, cálculo de saldo progressivo e fachada orquestradora.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **CSSD-01** | Limites físicos estritos de linhas | `CustomerStatementService` $\le 85$, `CustomerStatementCollector` $\le 195$, `CustomerStatementCalculator` $\le 95$, `CustomerStatementMovement` $\le 30$. |
| **CSSD-02** | Backend headless | Zero referências a `JOptionPane` ou `javax.swing`. |
| **CSSD-03** | Delegação canónica | Fachada delega em `collector` e `calculator`; repositórios de faturas, recibos e notas isolados no coletor; cálculo progressivo isolado no calculador. |
| **CSSD-04** | Preservação de contratos | Preservação do método `getStatement(Long clientId, LocalDate startDate, LocalDate endDate)`, anotação `@Autowired` e construtor de 5 repositórios. |

## 3. Regressão Funcional

- `AccountStatementHarnessTest`
- `MultiModuleArchitectureHarnessTest`
- `DesktopThinContextTest`
