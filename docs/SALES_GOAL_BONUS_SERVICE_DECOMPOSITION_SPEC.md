# Especificação de Decomposição: SalesGoalBonusService (Fase 149)

## 1. Diagnóstico do Monólito
`SalesGoalBonusService.java` possuía **233 linhas**, misturando:
- Listagem, aprovação e ajuste administrativo de prémios de desempenho comercial (`listBonuses`, `approveBonus`, `adjustBonus`).
- Integração de prémios no recibo salarial (`integrateBonusWithPayroll`) e geração automática de prémios ao atingir metas (`generateBonusesForAchievedGoal`).

## 2. Arquitectura Alvo
1. `SalesGoalBonusService` (`<= 90` linhas): Fachada `@Service` e `@Transactional`, preservando `toDTO` e o construtor de 6 argumentos.
2. `SalesGoalBonusApprovalService` (`<= 125` linhas): Componente `@Component` dedicado à listagem, aprovação, ajuste e mapeamento DTO de prémios.
3. `SalesGoalBonusPayrollService` (`<= 125` linhas): Componente `@Component` dedicado à integração no recibo de salário e apuramento automático por meta atingida.
