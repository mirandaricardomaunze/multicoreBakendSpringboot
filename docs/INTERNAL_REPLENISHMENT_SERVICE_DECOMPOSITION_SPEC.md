# Especificação de Decomposição: InternalReplenishmentService (Fase 174)

## Contexto e Problema
`InternalReplenishmentService` (196 linhas) concentrava a validação e conversão de encomendas de reposição em transferências de stock, o registo retroactivo de encomendas a partir de transferências aprovadas e a sincronização de estados via `StockTransferResolvedEvent`.

## Solução Arquitectural
1. **`InternalReplenishmentService` (Fachada — `<= 80` linhas)**:
   - Orquestra as chamadas transaccionais e o listener `@EventListener` delegando para `ReplenishmentOrderConversionService`.
   - Mantém a classe interna `ReplenishmentStatus` e o construtor compatível de 3 argumentos.
2. **`ReplenishmentOrderConversionService` (`@Component` — `<= 150` linhas)**:
   - Gere `convertToTransfer`, `recordOrderFromTransfer` e `onTransferResolved`.
