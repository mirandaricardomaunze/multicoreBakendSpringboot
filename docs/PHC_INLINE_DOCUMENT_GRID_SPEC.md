# SPEC — Grelhas documentais editáveis no modelo PHC

**ID:** SPEC-PHC-INLINE-GRID-001  
**Estado:** obrigatório  
**Âmbito:** Comercial, Compras e Stock

## Objectivo

Documentos com várias linhas são preparados e actualizados directamente na grelha. Seleccionar uma
linha nunca transporta os seus valores para um formulário externo. A lista de documentos continua
separada da área de trabalho documental através de `CardLayout`.

## Documentos abrangidos

| Documento | Momento editável | Grelha |
|---|---|---|
| Encomenda de cliente | estados permitidos pela `ORDER_EDITOR_SPEC` | Produto, Qtd, Emb., Cx., desconto e série |
| Encomenda a fornecedor | `ORDERED`, sem recepção | Produto, Qtd, Emb., Cx., preço, lote, validade e série |
| Cotação | `DRAFT` | Produto, Qtd, Emb., Cx. e desconto |
| Preparação de fatura | antes da emissão | Produto, Qtd, Emb., Cx., desconto e série |
| Transferência de stock | `DRAFT` | Produto, Qtd, Emb. e Cx. |

Notas de crédito/débito e recepção parcial já usam quantidades directamente nas células. São
operações transaccionais curtas, não editores persistentes: o diálogo de confirmação pode permanecer.

## Regras de interface

1. `Adicionar linha` cria uma linha e inicia a pesquisa de produto na primeira célula.
2. `Remover linha` actua sobre a selecção; não existe botão `Editar item`.
3. A pesquisa de produto usa `ProductSearchComboBox` como editor da célula.
4. `Qtd`, `Emb.` e `Cx.` aceitam entrada e mantêm a quantidade total sincronizada conforme
   `Caixa → Embalagem → Unidade`.
5. `% Cx.`, preço, IVA, peso e totais são derivados e somente leitura.
6. A toolbar fica no topo do `ModernPanel(16)` que contém a tabela.
7. Guardar termina a edição da célula, valida todas as linhas e mantém o editor aberto em erro.
8. Estado não editável desactiva todas as células e os botões de alteração.

## Limites fiscais e de stock

- Fatura emitida nunca é reaberta para edição; correcção usa anulação ou nota.
- Nota emitida, recepção iniciada e documento fechado permanecem imutáveis.
- A grelha calcula apenas pré-visualizações; preços, IVA, stock, FEFO e totais oficiais continuam a
  ser recalculados e validados pelos Services.
- Quantidades e dinheiro permanecem `BigDecimal`.

## Critérios de aceitação

- As cinco áreas documentais abrangidas não apresentam formulário externo para alterar uma linha.
- Produto e quantidades são realmente `TableCellEditor`, não apenas texto que abre outro ecrã.
- Embalagens e caixas actualizam a quantidade enviada ao contrato HTTP.
- `PhcInlineDocumentGridHarnessTest`, harnesses anteriores, `DesktopThinContextTest` e o harness de
  arquitectura modular passam.
