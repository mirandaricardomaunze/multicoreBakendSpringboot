# Especificação Técnica: Decomposição do PayrollLiabilityService (Fase 133)

## Diagnóstico

`PayrollLiabilityService` (266 linhas) concentrava o apuramento idempotente de retenções (IRPS, INSS
trabalhador e INSS patronal), a entrega ao Estado com saída de tesouraria e evento contabilístico, o radar
de prazos e o cálculo do custo mensal total do empregador.

## Decisão

- `PayrollLiabilityService`: fachada tenant-scoped preservando `DUE_ALERT_DAYS = 7` e o construtor legado de 7 argumentos.
- `PayrollLiabilityLifecycleService`: apuramento por período (`accrueForPeriod`), protecção de obrigações já entregues e entrega ao Estado (`markDelivered`) com saída financeira, evento `PayrollLiabilityDeliveredEvent` e auditoria.
- `PayrollLiabilityQueryService`: listagem de obrigações (`list`), alertas de vencimento/sem prazo (`dueAlerts`), custo mensal total com encargo patronal (`monthlyCost`) e mapeamento `PayrollLiabilityDTO`.
