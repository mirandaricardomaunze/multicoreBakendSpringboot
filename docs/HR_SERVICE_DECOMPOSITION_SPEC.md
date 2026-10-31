# Especificação de Decomposição: HRService (Fase 181)

## Objetivo
Decompor o monolito `HRService.java` (979 linhas e 22 dependências injetadas) numa fachada enxuta (`<= 145` linhas) que delega para quatro colaboradores coesos por subdomínio de Recursos Humanos.

## Arquitetura Alvo
1. `HRService.java` (`<= 145` linhas) — Fachada pública `@Service` preservando 100% da API pública e os construtores legados de 21 e 22 argumentos.
2. `HrEmployeeManagementService.java` (`<= 225` linhas) — Cadastro e atualização de colaboradores, validação de unicidade, associação a `AppUser` e submissão/consulta de notas de despesa (`ExpenseClaim`).
3. `HrPayslipIssuanceService.java` (`<= 250` linhas) — Emissão individual e processamento mensal de recibos salariais (`createPayslip`, `processMonthlyPayroll`), cálculo de IRPS/INSS, faltas, horas extra, bónus comerciais e descontos comprometidos.
4. `HrPayslipLifecycleService.java` (`<= 150` linhas) — Aprovação, pagamento (com integração financeira, passivo fiscal e evento contabilístico), anulação e consulta/impressão de recibos salariais.
5. `HrAbsenceAndVacationService.java` (`<= 215` linhas) — Registo, justificação e remoção de faltas, bem como submissão, validação de direito anual em dias úteis e decisão de pedidos de férias.
