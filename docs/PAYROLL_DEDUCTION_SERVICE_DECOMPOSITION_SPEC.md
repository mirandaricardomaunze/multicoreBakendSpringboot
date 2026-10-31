# Decomposição do PayrollDeductionService — Fase 127

## Diagnóstico

O serviço acumulava criação e tesouraria, política de prestações, aplicação em recibos, libertação de
linhas anuladas e consultas de saldo. O saldo continuava correctamente derivado das linhas aplicadas,
mas todas as razões para mudar estavam concentradas em 306 linhas.

## Decisão

- `PayrollDeductionService` (126 linhas): fachada e ciclo de vida do compromisso.
- `PayrollDeductionApplicationService` (68 linhas): aplicação, limite pelo líquido e libertação.
- `PayrollDeductionQueryService` (53 linhas): consultas, saldo e DTOs.
- `PayrollDeductionPolicy` (40 linhas): validação e cálculo puro das prestações.

O construtor anterior de seis dependências foi preservado. Não mudaram o arredondamento `CEILING`,
a ordem de aplicação, o limite pelo valor disponível, a saída de tesouraria, nem o cálculo de saldo.
