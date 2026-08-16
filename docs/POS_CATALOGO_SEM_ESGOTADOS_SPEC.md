# POS — Catálogo sem produtos esgotados

**Última actualização:** 2026-07-05
**Estado:** substituído por `POS_CATALOGO_ESTADO_STOCK_SPEC.md`.

> Esta decisão foi revista: os produtos esgotados agora aparecem atenuados no filtro **Todos** e
> continuam bloqueados para venda. O endpoint vendável permanece como fonte canónica do estado.

## Objectivo

No **catálogo de venda do POS**, os produtos **esgotados** não devem aparecer, para o operador não
tentar vender o que não há e não poluir a grelha de produtos.

## Regra

Um produto aparece no catálogo do POS se:

- **não controla stock** (`stockTracked = false`, ex.: serviços) — aparece sempre; **ou**
- **controla stock e tem quantidade disponível** (`quantity > 0`) em **pelo menos um armazém de
  venda** (activo + `allowsSales`).

Produtos com stock controlado e saldo `≤ 0` (zero ou negativo) em todos os armazéns de venda ficam
**ocultos**. Considera-se o stock **agregado dos armazéns de venda** (o POS já vende só desses —
ver [ARMAZEM_PROFISSIONAL_SPEC.md](ARMAZEM_PROFISSIONAL_SPEC.md)); depósitos puros não contam.

## Implementação

- **`InventoryService.getInStockProductIdsForSale(companyId)`** → `Set<Long>` dos IDs de produto com
  `quantity > 0` nos armazéns de venda. Exige a empresa activa (`requireCompany`). Núcleo testável.
- **`ComercialService.getSellableProducts()`** → `List<ProductDTO>`: parte de todos os produtos da
  empresa e filtra por `!stockTracked || inStock.contains(id)`. Reutiliza o mapper `toDTO` existente.
- **`POSPanel.loadMetadata()`** passa a usar `getSellableProducts()` em vez de `getAllProducts()`.
  O resto do fluxo (pesquisa, cards, adicionar ao carrinho) fica igual.

## Não-objectivos / notas

- A **facturação** (`ComercialPanel`) e a gestão de stock continuam a ver **todos** os produtos —
  a ocultação é só do **catálogo do POS**.
- A validação de stock no **checkout** e no **scan de código de barras** não muda (já existe); esta
  alteração é só de apresentação do catálogo.
- Quando um produto esgota, some do catálogo na próxima recarga do painel (`onPanelSelected` /
  botão de actualizar); ao repor stock, volta a aparecer.
