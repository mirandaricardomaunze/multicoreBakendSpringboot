# Especificação de Decomposição: PayrollBonusService (Fase 175)

## Contexto e Problema
`PayrollBonusService` (194 linhas) concentrava o apuramento proporcional do 13.º mês e subsídio de férias juntamente com o processamento de pagamento idempotente, integração com tesouraria (`FinanceService.registerAutoPayout`) e auditoria.

## Solução Arquitectural
1. **`PayrollBonusService` (Fachada — `<= 80` linhas)**:
   - Aplica os guards de segurança (`PermissionGuard.requireManagerOrAdmin`) e delega para `PayrollBonusCalculationService` e `PayrollBonusPaymentService`.
   - Mantém o construtor compatível de 5 argumentos.
2. **`PayrollBonusCalculationService` (`@Component` — `<= 105` linhas)**:
   - Gere `thirteenthMonth`, `vacationAllowance`, `allowanceFor`, `line` e `monthsWorkedInYear`.
3. **`PayrollBonusPaymentService` (`@Component` — `<= 100` linhas)**:
   - Gere `payThirteenthMonth`, `payVacationAllowance` e `persistAndPay`.
