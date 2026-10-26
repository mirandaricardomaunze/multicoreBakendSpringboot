# Especificação de Decomposição - SalesGoalService

## 1. Contexto e Motivação
O `SalesGoalService` (504 linhas) é o serviço autoritativo de gestão de metas comerciais e apuração de prémios de desempenho no Multicore ERP (CDC-BIZ). Ele concentra quatro responsabilidades críticas:
1. **Ciclo de Vida de Metas:** Definição (`createGoal`), alteração com regras de alvos mínimos (`updateGoal`), cancelamento (`cancelGoal`) e validação de escopos (empresa vs colaborador).
2. **Coordenação de Progresso e Fecho:** Cálculo em tempo real do progresso da meta (`getGoalProgress`, `getActiveGoalsSummary`), transição para `ACHIEVED` ou `MISSED`, e disparo de prémios automáticos.
3. **Gestão e Integração de Prémios:** Listagem (`listBonuses`), aprovação administrativa (`approveBonus`), ajuste com justificação (`adjustBonus`), e integração transacional na folha salarial (`integrateBonusWithPayroll`).
4. **Consultas e Projeção:** Listagem filtrada por estado e período (`listGoals`), detalhe (`getGoal`) e mapeamento para DTOs.

## 2. Nova Arquitectura Modular
O `SalesGoalService` passa a actuar como orquestrador enxuto ($\le 160$ linhas) delegando para 4 serviços especializados no pacote `mz.multicore.erp.modules.performance.service`:

1. `SalesGoalLifecycleService` ($\le 215$ linhas):
   - Criação, edição e cancelamento de metas comerciais.
   - Validações de escopo, unicidade e datas de vigência.
2. `SalesGoalProgressCoordinator` ($\le 130$ linhas):
   - Apuração de progresso via `GoalProgressEngine`.
   - Transição de estado de metas expiradas e conclusão de metas atingidas.
3. `SalesGoalBonusService` ($\le 240$ linhas):
   - Geração de bónus por alcance de meta (`generateBonusesForAchievedGoal`).
   - Aprovação, ajuste e integração direta na folha de vencimento (`integrateBonusWithPayroll`).
4. `SalesGoalQueryService` ($\le 95$ linhas):
   - Consultas de metas e mapeamento para `SalesGoalDTO`.

## 3. Compatibilidade e Regras
- Preservar o construtor sobrecarregado com 9 parâmetros em `SalesGoalService` para retrocompatibilidade com a suite `CommercialPerformanceHarnessTest`.
- Respeitar as directrizes de permissões (`PermissionGuard.requireAdmin`, `PermissionGuard.requireManagerOrAdmin`), auditoria e internacionalização pt-MZ.
