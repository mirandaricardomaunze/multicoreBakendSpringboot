# Especificação Técnica: Decomposição do ReportService (Fase 129)

## Diagnóstico

`ReportService` misturava orquestração tenant-scoped, consultas temporais, recolha de indicadores
operacionais e cálculos analíticos sobre facturas.

## Decisão

- `ReportService`: valida tenant, define intervalos e consulta facturas por data/estado.
- `ReportSalesAnalyzer`: cálculos puros de vendas, top artigos, operadores e margem histórica.
- `ReportOperationsCollector`: stock baixo, aprovações, sessões, pagamentos e movimentos de caixa.
- O construtor legado de seis repositórios permanece disponível.

Mantêm-se as definições de venda realizada, saldo por cobrar, custo histórico e os DTOs existentes.
Não há alteração de contrato HTTP nem de regra comercial.
