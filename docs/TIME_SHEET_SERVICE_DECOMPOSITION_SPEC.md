# Especificação de Decomposição - TimeSheetService

## 1. Contexto e Motivação
O `TimeSheetService` (492 linhas) é o serviço autoritativo de ponto e assiduidade dos Recursos Humanos no Multicore ERP (docs/RH_COMPLETO_SPEC.md §B2). Ele concentra três responsabilidades distintas:
1. **Registo e Validação de Marcações:** Inserção, remoção, validação temporal (turnos nocturnos, meia-noite, deduplicação diária) e conversão em DTOs.
2. **Cálculo e Apuração Mensal:** Apuração matemática de horas normais, segregação legal de horas extra em três escalões (diurnas, nocturnas e dia de descanso), tolerância a atrasos e contagem de dias úteis da empresa.
3. **Coordenação de Período e Fecho:** Fecho e reabertura auditada de períodos mensais, e geração automática e idempotente de faltas por justificar (`PENDING_JUSTIFICATION`).

## 2. Nova Arquitectura Modular
O `TimeSheetService` passa a ser um orquestrador enxuto ($\le 180$ linhas) delegando para 3 colaboradores especializados no pacote `mz.multicore.erp.modules.hr.service`:

1. `TimeEntryRegistrationService` ($\le 160$ linhas):
   - Gestão individual de picagens de ponto (`recordEntry`, `deleteEntry`, `getEntries`).
   - Validação semântica de tempos de entrada, saída e intervalo.
   - Resolução de empregado e contexto da empresa.
2. `TimeSheetCalculator` ($\le 195$ linhas):
   - Apuração mensal de horas e dias (`getMonthlySheet`, `findEmployeeLine`, `computeLine`).
   - Classificação de escalões de horas extra e cálculo de atrasos (`isLate`).
   - Contagem autoritativa de dias úteis com base no horário corporativo (`workingDaysBetween`, `defaultSchedule`).
3. `TimeSheetPeriodCoordinator` ($\le 195$ linhas):
   - Ciclo de fecho mensal e trancamento (`closePeriod`, `isPeriodClosed`, `ensurePeriodOpen`).
   - Reabertura auditada com motivo obrigatório (`reopenPeriod`).
   - Geração idempotente de faltas por justificar (`generateMissingDayAbsences`).

## 3. Compatibilidade e Regras
- Preservar o construtor sobrecarregado com 8 parâmetros em `TimeSheetService` para compatibilidade total com os testes unitários legados (`TimeSheetServiceTest`, `OvertimeValuationServiceTest`, `HRServiceTest`).
- Manter a constante pública `PENDING_JUSTIFICATION = "PENDING_JUSTIFICATION"`.
- Respeitar as diretrizes de internacionalização pt-MZ e tipagem estrita de auditoria.
