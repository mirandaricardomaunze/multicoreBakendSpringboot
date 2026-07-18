# Etiquetas de código de barras (produtos)

**Última actualização:** 2026-07-12
**Estado:** feito (serviço + UI + impressão). Validação de leitor real = manual.

## Objectivo

Imprimir **etiquetas de produto** (código de barras + nome + preço) numa folha A4, para etiquetar
prateleiras/artigos. Complementa o leitor/balança: os artigos passam a ter etiqueta legível por
scanner. O operador escolhe os artigos e o nº de cópias por etiqueta.

## Formato

- Folha **A4**, grelha de **3 colunas** de etiquetas. Cada etiqueta:
  - **Nome** do artigo (truncado),
  - **Código de barras** (Code128 — universal, aceita EAN/SKU/referência) como imagem,
  - **Texto do código** por baixo (legível),
  - **Preço** (`%,.2f MT`).
- O código impresso é, por ordem de preferência: `barcode` → `reference` → `sku` (sempre há SKU).
- Code128 (não EAN-13) para funcionar com qualquer código do catálogo, numérico ou não.

## Peças

- **`ProductLabelPrintService`** (`modules/printing`) — `render(companyId, productIds, copies)` → PDF.
  Gera o código de barras como **imagem AWT** (`Barcode128.createAwtImage`) e embebe-a (não precisa do
  `PdfWriter`). Reusa `CompanyHeaderRenderer` + `PdfDocumentBuilder`. Uma responsabilidade: dados →
  folha de etiquetas.
- **UI (`StockPanel`)** — botão **"Etiquetas"**: tabela de produtos (multi-selecção) + nº de **Cópias
  por etiqueta** + **Imprimir Selecionadas**.

## Regras / limites

- Tenant-scoped (produtos da empresa activa). Cópias limitadas (1..200) para não gerar PDFs enormes.
- Sem preço de etiqueta configurável (usa o preço de venda `unitPrice`). Tamanho/nº de colunas fixos
  nesta iteração — dimensões de etiqueta configuráveis é um passo futuro.
- **Leitura por scanner real** validada manualmente (o Code128 gerado é padrão).
