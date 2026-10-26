# Especificação Técnica — Decomposição Modular de StockTransferPrintService (Fase 96)

## 1. Contexto e Diagnóstico

`StockTransferPrintService.java` acumulava mais de 765 linhas de código gerando documentos PDF para transferências de stock entre armazéns e respetiva separação física (picking), misturando:
1. Orquestração das 3 vias nominais (Original, Duplicado, Triplicado) da Guia de Transferência e da Guia de Separação;
2. Construção de tabelas com formatação logística avançada (caixas, embalagens, pesos unitários e totais, resolução FEFO de lotes e validades);
3. Construção de blocos de cabeçalho de rota, viatura, motorista e responsável;
4. Resumos financeiros e de carga logística com cálculo volumétrico/peso;
5. Blocos de assinaturas operacionais;
6. Métodos legados duplicados e não utilizados que inflavam desnecessariamente o tamanho da classe.

## 2. Princípios Arquiteturais e Diretrizes Aplicadas

- **SOLID - Princípio da Responsabilidade Única (SRP):** Cada componente passa a ter um escopo exclusivo e bem definido.
- **Injeção de Dependências por Construtor:** Todas as classes filhas são `@Component` Spring e injetadas via construtor com campos `final`.
- **Retrocompatibilidade Rigorosa:** Construtores com 1 e 2 parâmetros preservados em `StockTransferPrintService`.
- **Limites Físicos de Linhas:**
  - `StockTransferPrintService.java`: $\le 160$ linhas (orquestrador enxuto de ~130 linhas).
  - `StockTransferPrintRouteBuilder.java`: $\le 130$ linhas (~107 linhas).
  - `StockTransferPrintLinesTableBuilder.java`: $\le 225$ linhas (~217 linhas).
  - `StockTransferPrintSummaryBuilder.java`: $\le 150$ linhas (~128 linhas).

## 3. Estrutura Modular Decomposta

```
backend/src/main/java/mz/multicore/erp/modules/printing/
├── StockTransferPrintService.java           (Orquestrador enxuto, ~130 linhas)
├── StockTransferPrintRouteBuilder.java      (Blocos de rota, viatura e separador, ~107 linhas)
├── StockTransferPrintLinesTableBuilder.java (Tabelas de artigos, embalagens e lotes FEFO, ~176 linhas)
└── StockTransferPrintSummaryBuilder.java    (Totais, carga logística e assinaturas, ~128 linhas)
```

## 4. Colaboradores Especializados

### 4.1 StockTransferPrintRouteBuilder
- `buildHorizontalRule()`: Régua decorativa da marca.
- `buildProfessionalRouteBlock(StockTransfer)`: Bloco de origem/destino, responsáveis, estado, data, motorista e matrícula.
- `buildPickingRouteBlock(StockTransfer)`: Bloco específico de recolha física para conferentes.
- Utilitários estáticos: `valueOrLine`, `firstNonBlank`.

### 4.2 StockTransferPrintLinesTableBuilder
- `buildProfessionalLinesTable(StockTransfer)`: Tabela com 11 colunas com conversões em caixas, embalagens, peso total kg, preço unitário efetivo e total.
- `buildPickingLinesTable(List<StockTransferLine>)`: Tabela operacional com 7 colunas e caixas de verificação física para conferência.
- `resolveBatchInfo(StockTransferLine, Long)`: Resolução FEFO do lote e data de validade com `ProductBatchRepository`.
- Utilitários de cálculo e formatação: `formatBoxes`, `formatPackages`, `formatBoxPercentage`, `formatRate`, `formatQty`, `effectiveUnitPrice`, `formatWeight`, `unitWeight`.

### 4.3 StockTransferPrintSummaryBuilder
- `buildProfessionalSummary(StockTransfer)`: Observações, contagem de linhas, caixas totais, quantidade total, peso total aferido e valor total MZN.
- `buildLoadSummary(List<StockTransferLine>)`: Resumo analítico de carga com `LoadSummaryRenderer`.
- `buildProfessionalSignatureBlock()`: 3 colunas de assinaturas (Expedidor, Transportador, Destinatário).
- `buildPickingSignatureBlock()`: 2 colunas de assinaturas (Separador, Conferente).
