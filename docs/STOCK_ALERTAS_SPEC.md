# Alertas de Stock — esgotados e validades

**Última actualização:** 2026-07-06
**Estado:** feito.

## Objectivo

O gestor deve **ver num só sítio** os problemas de stock: produtos **esgotados** (ruptura) e lotes
**expirados ou a expirar**, para agir antes de faltar produto ou vender fora de validade.

## Regra

- **Esgotado (ruptura):** produto que **controla stock** (`stockTracked = true`) e cujo **saldo total
  na empresa** (soma de todos os armazéns) é **≤ 0**. Inclui produtos que nunca tiveram entrada
  (saldo 0). Serviços (`stockTracked = false`) nunca entram.
- **Validade:** lotes **com stock** (`quantity > 0`) cuja validade **já passou** (expirado) ou ocorre
  **dentro de 30 dias** (a expirar). Reutiliza a regra existente de
  [InventoryService.findExpiringBatches](../src/main/java/com/phcpro/modules/inventory/service/InventoryService.java).

## Implementação

- **`InventoryService.findOutOfStockProducts(companyId)`** → `List<StockAlertDTO>` (SKU, nome, saldo).
  Soma o stock por produto (`stockRepository.findByWarehouseCompanyId`) e cruza com o catálogo
  (`productRepository`), devolvendo os que controlam stock com saldo ≤ 0. Exige a empresa activa.
- **`StockAlertDTO`** (`productId, sku, name, currentStock`).
- Validades: já existia `findExpiringBatches(companyId, 30)`.
- **Desktop:** nova aba **"Alertas"** no `StockPanel` (a seguir a "Níveis de Stock"), com:
  - resumo (contagens: esgotados · expirados · a expirar);
  - sub-aba **"Esgotados"** (tabela SKU / Nome / Stock);
  - sub-aba **"Validade"** (tabela por lote; texto a **vermelho** se expirado, **amarelo** se a
    expirar — pela coluna Dias).

## Não-objectivos / notas

- Não altera o **catálogo do POS** (esse já esconde esgotados — ver
  [POS_CATALOGO_SEM_ESGOTADOS_SPEC.md](POS_CATALOGO_SEM_ESGOTADOS_SPEC.md)) nem a sugestão de
  reposição por stock mínimo (`ReorderService`), que é um alerta distinto (stock baixo, não zero).
- O "esgotado" é por **saldo total da empresa**; a ocultação no POS é por **armazéns de venda**.
