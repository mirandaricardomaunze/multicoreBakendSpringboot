# Plano de Testes & Harness - HRApiClient (Fase 94)

## 1. Objectivos de Teste

Validar a integridade estrutural, desacoplamento e limites físicos da decomposição de `HRApiClient`.

## 2. Casos de Teste (HRACD)

- **HRACD-01: Limites físicos estritos de linhas de código:**
  - `HRApiClient.java` $\le 185$ linhas.
  - `HREmployeeApiClient.java` $\le 110$ linhas.
  - `HRContractApiClient.java` $\le 65$ linhas.
  - `HRTimeSheetApiClient.java` $\le 100$ linhas.
  - `HRPayrollApiClient.java` $\le 130$ linhas.
  - `HRExpensesAndBenefitsApiClient.java` $\le 105$ linhas.
- **HRACD-02: Zero diálogos legados JOptionPane:**
  - Nenhum ficheiro do pacote `desktop/client` pode conter chamadas a `JOptionPane`.
- **HRACD-03: Delegação canónica para sub-clientes especializados:**
  - `HRApiClient` deve delegar para `HREmployeeApiClient`, `HRContractApiClient`, `HRTimeSheetApiClient`, `HRPayrollApiClient` e `HRExpensesAndBenefitsApiClient`.
- **HRACD-04: Contratos e métodos de api preservados:**
  - Verificação de métodos como `getAllEmployees`, `getAllContracts`, `getMonthlySheet`, `processMonthlyPayroll`, `getThirteenthMonth`, `getVacationAllowance`, `getContractAlerts`, `getPayrollLiabilityAlerts`.
