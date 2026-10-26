# Harness de Testes — Decomposição Modular de StockTransferPrintService (Fase 96)

## 1. Objectivo do Harness

Garantir que a refatoração e decomposição de `StockTransferPrintService.java` em componentes menores satisfaz:
1. Limites físicos estritos de linhas de código em todos os ficheiros gerados;
2. Delegação efetiva entre o orquestrador e os construtores de blocos;
3. Preservação integral das anotações Spring `@Service` e `@Component`;
4. Preservação dos construtores retrocompatíveis para chamadas de teste e componentes legados;
5. Renderização idêntica dos PDFs oficiais das 3 vias nominais e da folha de separação interna.

## 2. Testes Automatizados Executados

1. **`StockTransferPrintServiceDecompositionHarnessTest` (4 testes):**
   - `testFileSizesAndLimits`: Valida existência e tetos de linhas ($\le 160, \le 130, \le 225, \le 150$).
   - `testDelegationInOrchestrator`: Valida as invocações de delegação nos métodos de renderização.
   - `testSpringAnnotations`: Valida `@Service` e `@Component`.
   - `testBackwardCompatibleConstructors`: Valida construtores de 1 e 2 parâmetros.
2. **`StockTransferPrintServiceTest` (5 testes):**
   - `render_imprimeMotoristaEMatricula`: Verifica texto extraído contendo motorista e matrícula.
   - `render_semMotoristaEMatricula_imprimeLinhasSublinhadas`: Verifica linhas de preenchimento manual quando ausentes.
   - `render_imprimeTabelaComColunasObrigatorias`: Verifica cabeçalhos de caixas, embalagens, peso, lote e totais.
   - `render_imprimeTresViasNominais`: Verifica criação das 3 páginas horizontais com títulos nominais.
   - `renderPickingSheet_emiteGuiaDeSeparacaoInterna`: Verifica cabeçalho e assinaturas da folha de picking.
3. **`MultiModuleArchitectureHarnessTest` (6 testes):**
   - Garante que fronteiras arquiteturais e Maven reactor continuam 100% íntegras.

## 3. Matriz de Cobertura

| Componente | Linhas Anteriores | Linhas Atuais | Redução |
|---|---|---|---|
| `StockTransferPrintService` | 765 | ~132 | **-82.7%** |
| `StockTransferPrintRouteBuilder` | — | ~107 | Novo componente modular |
| `StockTransferPrintLinesTableBuilder` | — | ~176 | Novo componente modular |
| `StockTransferPrintSummaryBuilder` | — | ~128 | Novo componente modular |
