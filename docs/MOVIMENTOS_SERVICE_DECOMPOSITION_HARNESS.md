# Harness de Testes - MovimentosService Decomposição (Fase 113)

## Casos de Teste Estruturais e Funcionais
1. **Verificação de Limites Físicos (Linhas de Código):**
   - `MovimentosService.java`: $\le 100$ linhas.
   - `MovimentoSummarizer.java`: $\le 190$ linhas.
   - `MovimentoCollector.java`: $\le 205$ linhas.
2. **Delegação e Preservação de Assinatura:**
   - Garantir que `MovimentosService` contém o método `listar(Long, String, LocalDate, LocalDate)`.
   - Garantir que `MovimentosService` delega a coleta a `MovimentoCollector`.
   - Garantir que `MovimentoCollector` utiliza `MovimentoSummarizer` para artigos e quantidades.
3. **Casos Funcionais (MU-01 a MU-08):**
   - Sucesso em `MovimentosServiceTest` com os testes unitários legados cobrindo faturas, encomendas, cotações, notas de crédito, notas de débito, recibos e transferências de stock.
   - Preservação da ordenação por data descendente e filtros por query e período.
4. **Verificação Arquitetural Global:**
   - `MultiModuleArchitectureHarnessTest`: 100% aprovado.
   - `DesktopThinContextTest`: 100% aprovado.
