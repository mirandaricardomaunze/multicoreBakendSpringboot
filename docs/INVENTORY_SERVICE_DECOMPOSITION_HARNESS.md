# Harness de Testes — Decomposição Modular de InventoryService (Fase 97)

## 1. Objectivo do Harness

Garantir que a refatoração e decomposição de `InventoryService.java` em componentes menores satisfaz:
1. Limites físicos estritos de linhas de código em todos os ficheiros gerados;
2. Delegação efetiva entre o orquestrador e os serviços especializados de armazéns, consultas e movimentos;
3. Preservação integral das anotações Spring `@Service`;
4. Preservação dos construtores retrocompatíveis para chamadas de teste e componentes legados (9 parâmetros);
5. Conformidade ininterrupta com os harnesses permanentes do Multicore ERP:
   - `MultiUserRobustnessHarnessTest` (bloqueios pessimistas `findForReservation`);
   - `StockMovementPaginationHarnessTest` (paginação server-side do histórico de movimentos).

## 2. Testes Automatizados Executados

1. **`InventoryServiceDecompositionHarnessTest` (4 testes):**
   - `testFileSizesAndLimits`: Valida existência e tetos de linhas ($\le 320, \le 215, \le 205, \le 380$).
   - `testDelegationInOrchestrator`: Valida as invocações delegadas no orquestrador.
   - `testPermanentHarnessConstraints`: Valida os contratos estritos de concorrência e paginação.
   - `testBackwardCompatibleConstructor`: Valida construtor de 9 argumentos com mocks.
2. **`InventoryServiceTest` (10 testes):**
   - Valida regras de negócio de expiração de lotes, armazéns de venda, permissões e DTOs.
3. **`StockMovementPaginationHarnessTest` (1 teste):**
   - Valida paginação de movimentos de stock.
4. **`MultiUserRobustnessHarnessTest` (6 testes):**
   - Valida locking pessimista e versões de concorrência.
5. **`MultiModuleArchitectureHarnessTest` (6 testes):**
   - Garante que fronteiras arquiteturais e Maven reactor continuam 100% íntegras.

## 3. Matriz de Cobertura

| Componente | Linhas Anteriores | Linhas Atuais | Redução |
|---|---|---|---|
| `InventoryService` | 735 | ~272 | **-63.0%** |
| `WarehouseService` | — | ~183 | Novo componente modular |
| `StockQueryService` | — | ~172 | Novo componente modular |
| `StockMovementService` | — | ~318 | Novo componente modular |
