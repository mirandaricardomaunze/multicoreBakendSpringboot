# Spec — Catálogo POS em cards com imagem

> O ecrã de venda (POS) passa a mostrar os produtos como **cards com imagem**; clicar num card
> adiciona o artigo ao carrinho. Os selects de documento ficam num **topo compacto** (estilo web),
> libertando espaço para o catálogo.

**Última actualização:** 2026-08-16

## Problema

O POS escolhia o produto por uma combobox + formulário detalhado (qtd, desconto, lote, série) na
coluna esquerda, ocupando muito espaço vertical e pouco visual. Faltava imagem por produto. O fluxo
moderno (web/retalho) é um **grid de cards** clicáveis.

## Decisão

### Imagem por produto (backend)
- **Bytes na base de dados** (`products.image_data bytea`, migração `V17`→`V18`). Portátil para
  multi-utilizador PostgreSQL; a imagem viaja com os dados. Auto-reduzida a ~320px no upload.
- `Product.imageData (byte[])`; `ProductDTO.image (byte[])` (mapeado em `ComercialService.toDTO`).
- `ComercialService.updateProductImage(productId, bytes)` — guardado após criar o produto, com guarda
  de empresa. Cadastro de produto (StockPanel) ganha **selector de imagem** (`JFileChooser`) + pré-visualização.
- Helpers `UIHelper.readScaledImage(file, maxDim)` (ficheiro→bytes reduzidos) e
  `UIHelper.imageIconFromBytes(bytes, w, h)` (bytes→ícone).

### Catálogo em cards (POS)
- Painel esquerdo do separador "Venda POS" passa a ser um **grid de cards** (scroll), filtrável pelo
  campo de pesquisa de produto. Cada card: **imagem** (ou ícone de marcador quando sem imagem),
  **nome** e **preço**.
- **Clicar no card adiciona ao carrinho** (quantidade 1, FEFO/promoção automáticos). Clicar de novo no
  mesmo produto **incrementa a quantidade** (merge), como num carrinho web. O leitor de código de
  barras usa o mesmo caminho.
- O formulário detalhado antigo (combo de produto, qtd, desconto, lote, série, botão "Adicionar
  Artigo") é **removido** do POS — o card trata da adição; ajustes finos passam pelo carrinho.

### Selects alinhados (espaço)
- Os selects de documento — **Cliente, Armazém, Conta de Tesouraria** — passam para uma **barra
  superior compacta** (em linha), alinhada com a barra do leitor de código de barras, em vez da
  coluna esquerda. Liberta a largura toda para o catálogo + carrinho.

## Não-objetivos

- Não alterar o cálculo de venda/checkout/IVA/promoções (reutiliza `LineCalculator`/`PromotionService`).
- Não suportar múltiplas imagens por produto nem edição de imagem no POS (só no cadastro).
- Não migrar o número de série por linha para o fluxo de card (raro; fica via barcode/carrinho futuro).

## Notas técnicas

- O grid reconstrói-se em `filterProducts(...)` a partir de `filteredProducts`.
- `addProductToCart(product)` centraliza a adição (merge + promoção) usada por card e barcode.
- Imagens são thumbnails (~320px) — `getAllProducts()` carrega os bytes em memória; aceitável para o
  catálogo de uma loja.

## Densidade operacional

- A imagem do card usa `96 × 60 px`, suficiente para reconhecer o artigo sem dominar o catálogo.
- O card usa margem interna de `7 px`, intervalo interno de `4 px` e cantos de `10 px`.
- O grid mantém duas colunas e intervalo de `8 px`, permitindo visualizar mais produtos por ecrã.
- Nome, preço, estado de stock, tooltip e toda a superfície clicável permanecem disponíveis.
