# Especificação de Decomposição: SupplierStatementService (Fase 180)

## Contexto e Problema
`SupplierStatementService` (177 linhas) combinava a validação do fornecedor e do período com a recolha de compras e pagamentos, ordenação cronológica e cálculo do saldo de abertura, movimentos e saldo final do extrato de conta corrente.

## Solução Arquitectural
1. **`SupplierStatementService` (Fachada — `<= 60` linhas)**:
   - Valida o fornecedor, resolve o período e obtém as compras da empresa.
   - Delega a construção do extrato para `SupplierStatementMovementBuilder`.
   - Mantém construtor compatível `SupplierStatementService(SupplierRepository, PurchaseRepository)`.
2. **`SupplierStatementMovementBuilder` (`@Component` — `<= 140` linhas)**:
   - Constrói os movimentos `V/FT` e `PG`, ordena cronologicamente e calcula o saldo progressivo do `SupplierStatementDTO`.
