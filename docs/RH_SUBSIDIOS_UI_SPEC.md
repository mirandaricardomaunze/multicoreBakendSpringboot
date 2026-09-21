# SPEC — Subsídios legais no desktop RH

## Objectivo

Fechar a última lacuna entre o backend de subsídios legais e o cliente desktop, sem duplicar
cálculos na UI. O backend continua dono do apuramento, pagamento, tesouraria, permissões,
idempotência e auditoria.

## Fluxos

### 13.º mês

1. Em **Recibos de Salário**, o utilizador escolhe o ano.
2. O desktop consulta `GET /api/hr/payroll/thirteenth-month/{year}`.
3. Mostra colaboradores, meses trabalhados, salário base, valor individual e total.
4. O pagamento exige confirmação explícita.
5. O desktop chama `POST /api/hr/payroll/thirteenth-month/{year}/pay` fora do EDT.
6. A resposta informa quantos colaboradores foram pagos e o total efectivamente movimentado.

### Subsídio de férias

1. Em **Férias**, o utilizador selecciona um pedido aprovado.
2. O desktop consulta `GET /api/hr/payroll/vacation-allowance/{vacationId}`.
3. Mostra colaborador, dias, valor diário e total.
4. O pagamento exige confirmação explícita.
5. O desktop chama `POST /api/hr/payroll/vacation-allowance/{vacationId}/pay` fora do EDT.

## Regras de arquitectura e UI

- `desktop` usa apenas `HRApiClient` e DTOs de `contracts`.
- Nenhum cálculo monetário ou regra legal vive no Swing.
- Chamadas HTTP não bloqueiam o EDT.
- Confirmações usam `ModernMessageDialog`; sucesso usa toast e erro usa feedback contextual.
- Valores usam `BigDecimal` e a apresentação monetária canónica.
- Não se cria novo separador, preservando a largura da barra do RH.

## Fora desta fase

- Alterar fórmulas legais ou percentagens.
- Escolher contas contabilísticas para o lançamento automático.
- Executar pagamentos reais fora do ambiente de teste.

