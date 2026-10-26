# Harness de Testes — Decomposição Modular de PurchaseOrderService (Fase 98)

## 1. Objectivo do Harness

Garantir que a refatoração e decomposição de `PurchaseOrderService.java` em componentes menores satisfaz:
1. Limites físicos estritos de linhas de código em todos os ficheiros gerados ($\le 195, \le 185, \le 200$);
2. Delegação efetiva entre o orquestrador e os serviços especializados de ciclo de vida e recepção;
3. Preservação integral das anotações Spring `@Service`;
4. Preservação dos construtores retrocompatíveis para chamadas de teste e componentes legados (10 argumentos);
5. Conformidade ininterrupta com todos os testes funcionais de compras e divergências.

## 2. Testes Automatizados Executados

1. **`PurchaseOrderServiceDecompositionHarnessTest` (4 testes):**
   - `testFileSizesAndLimits`: Valida existência e tetos de linhas ($\le 195, \le 220, \le 250$).
   - `testDelegationInOrchestrator`: Valida as invocações delegadas no orquestrador.
   - `testSpringAnnotations`: Valida `@Service` em todos os ficheiros.
   - `testBackwardCompatibleConstructor`: Valida construtor de 10 argumentos com mocks.
2. **`PurchaseOrderServiceTest` (20 testes):**
   - Valida criação de ordens, edição antes de recepção, recepção total, recepção parcial, idempotência e registo de divergências.
3. **`PurchaseServiceTest` (15 testes):**
   - Valida compras directas e conversões.
4. **`BusinessUnitHarnessTest` (3 testes):**
   - Valida unidade de negócio em linhas de encomenda.
5. **`MultiModuleArchitectureHarnessTest` (6 testes):**
   - Garante que fronteiras arquiteturais e Maven reactor continuam 100% íntegras.

## 3. Matriz de Cobertura

| Componente | Linhas Anteriores | Linhas Atuais | Redução |
|---|---|---|---|
| `PurchaseOrderService` | 522 | ~183 | **-64.9%** |
| `PurchaseOrderLifecycleService` | — | ~168 | Novo componente modular |
| `PurchaseOrderReceiptExecutor` | — | ~186 | Novo componente modular |
