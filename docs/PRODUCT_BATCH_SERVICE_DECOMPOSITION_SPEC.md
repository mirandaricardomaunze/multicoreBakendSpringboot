# Especificação de Decomposição: ProductBatchService (Fase 165)

## Contexto e Problema
`ProductBatchService` (211 linhas) concentrava as mutações de lote (entrada com validação de validade, consumo FEFO multi-lote e materialização de lotes legados) juntamente com as consultas por empresa, armazém, validade próxima e mapeamento DTO.

## Solução Arquitectural
1. **`ProductBatchService` (Fachada — `<= 95` linhas)**:
   - Orquestra as chamadas transaccionais delegando para `ProductBatchMutationService` e `ProductBatchQueryService`.
   - Mantém o record `BatchConsumption`, `toDTO(ProductBatch)` e o construtor compatível `ProductBatchService(ProductBatchRepository)`.
2. **`ProductBatchMutationService` (`@Component` — `<= 140` linhas)**:
   - Gere `addToBatch`, `consumeFEFO`, `ensureLegacyBatch` e `materialiseLegacyIfNeeded`.
3. **`ProductBatchQueryService` (`@Component` — `<= 85` linhas)**:
   - Gere `findByCompany`, `findByWarehouse`, `findExpiringByCompany`, `sumQuantity`, `findNextFEFO` e `toDTO`.
