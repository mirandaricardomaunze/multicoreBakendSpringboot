# Harness — Colunas de Linha dos Documentos Comerciais

Valida o [DOCUMENT_LINE_COLUMNS_SPEC.md](DOCUMENT_LINE_COLUMNS_SPEC.md). Legenda: ✅ feito · 🟡 parcial · ❌ em falta.

## Dados base

- Produto perecível `Iogurte`: com `barcode`, `reference`, lote com validade.
- Produto sem lote `Serviço Entrega`: sem `barcode`/validade.
- Fatura, encomenda e nota de crédito com pelo menos uma linha de cada produto.

## Matriz de cenários

| ID | Documento | Cenário | Resultado esperado | Estado |
|----|-----------|---------|--------------------|--------|
| DC-01 | Fatura | Imprimir com artigo com lote | Mostra as 11 colunas, incluindo Embalagens, Caixas e % da Caixa | ✅ |
| DC-02 | Fatura | Linha sem lote | Coluna Validade mostra `—` | ✅ |
| DC-03 | Encomenda | Imprimir com artigo com lote | Mesmas 11 colunas que a fatura | ✅ |
| DC-04 | Nota de Crédito | Imprimir devolução | Mesmas 11 colunas (desconto 0) | ✅ |
| DC-05 | Subtotal | Linha com desconto | Subtotal = líquido (qtd×preço−desc, antes de IVA) | ✅ |
| DC-06 | Consistência | Soma dos subtotais | Bate com `totalBeforeTax` do documento | ✅ |
| DC-07 | Validade | Lote com validade | Mostra `expirationDate` em `dd/MM/yyyy` | ✅ |
| DC-08 | Guia de Remessa | Imprimir guia a partir da fatura | Mesmas 11 colunas + bloco de transporte e assinaturas; ref. `GR-<nºfatura>` | ✅ |
| DC-09 | Composição 12 × 6; linha com 18 unidades | Renderizar linha | Emb. = `3`; Cx. = `0.25`; % Cx. = `25%` | ✅ |
| DC-10 | Composição 12 × 6; linha com 90 unidades | Renderizar linha | Emb. = `15`; Cx. = `1.25`; % Cx. = `125%` | ✅ |
| DC-11 | Cabeçalho com todas as colunas | Renderizar linha | Todos os nomes longos usam as abreviações canónicas | ✅ |

## Testes automatizados

- [x] `LineItemsTableRendererTest` (novo): subtotal líquido coerente com `LineCalculator`,
      validade `—` quando nula, número de colunas/cabeçalhos canónicos.

Estado: `mvn test` verde.

## Verificação manual (UI desktop)

- [ ] `ComercialPanel` → Faturar → Imprimir: confirmar 11 colunas e validade do lote FEFO.
- [ ] `ComercialPanel` → Encomendas → Imprimir.
- [ ] `ComercialPanel` → Emitir NC → Imprimir.
- [ ] `ComercialPanel` → Faturas → "Imprimir Guia": confirmar guia de remessa com as 11 colunas e
      blocos de transporte/assinaturas.

## Notas de implementação

- Renderizador único: `LineItemsTableRenderer` (colunas/formatos).
- Mapeamento partilhado `linha → Row`: `LineRowMapper` (resolve barcode/ref/descrição + validade do lote
  via `ProductBatchRepository.findFirstByProductIdAndBatchNumber`).
- Subtotal via `LineCalculator.net(...)`.
