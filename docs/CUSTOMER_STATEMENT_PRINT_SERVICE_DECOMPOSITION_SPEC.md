# Especificação Técnica: Decomposição do CustomerStatementPrintService (Fase 141)

## Diagnóstico

`CustomerStatementPrintService` (251 linhas) misturava a obtenção da empresa e do extrato com a composição
A4 (identificação do cliente, cartões de saldos, tabela de movimentos com saldo progressivo, termo de
circularização e assinaturas).

## Decisão

- `CustomerStatementPrintService`: fachada que valida a empresa, obtém `CustomerStatementDTO` e delega a emissão.
- `CustomerStatementDocumentRenderer`: composição integral do PDF A4 do Extrato de Conta Corrente.
- Construtor legado `(CustomerStatementService, CompanyService)` preservado.
