# Inventário físico (contagem cega + reconciliação)

**Última actualização:** 2026-07-10
**Estado:** feito.

## Objectivo

Completar o fluxo de inventário: **imprimir uma folha de contagem sem quantidades** (contagem cega),
**introduzir as contagens** e **reconciliar** — o sistema mostra a diferença face ao stock e gera os
ajustes. Complementa o [bloqueio de stock](BLOQUEIO_STOCK_SPEC.md) (esconder quantidades) com o passo
que faltava (contar e acertar).

## Fluxo

1. **Folha de contagem (PDF)** — `InventoryCountSheetPrintService`: lista os artigos de um armazém
   (Referência/SKU, Cód. Barras, Nome) com uma coluna **Contagem em branco** para escrever à mão,
   **sem** quantidades do sistema. Cabeçalho da empresa + bloco de assinaturas (Contado por / Conferido
   por). Reusa `CompanyHeaderRenderer` + `PdfDocumentBuilder` (layout consistente).
2. **Introdução das contagens** — diálogo "Inventário Físico" no `StockPanel`: escolher armazém, tabela
   editável (SKU · Artigo · **Contagem**). Botão "Imprimir Folha de Contagem".
3. **Reconciliação** — "Aplicar Ajustes": para cada artigo **com contagem preenchida**, chama
   `InventoryService.adjustStock(...)` (define a quantidade contada; MANAGER/ADMIN + auditoria) e mostra
   um resumo com as **diferenças** (sistema → contado, ±). Artigos deixados **em branco não são
   tocados** (só se ajusta o que foi efectivamente contado — evita zerar tudo por engano).

## Peças

- **`InventoryCountSheetPrintService`** (`modules/printing`) — `render(companyId, warehouseId)` → PDF da
  folha cega. `warehouseId` nulo = todos os armazéns (mostra coluna Armazém).
- **`StockPanel`**
  - Botão **"Inventário Físico"** no topo (junto ao bloqueio de stock — trancar + contar às cegas).
  - `openPhysicalInventoryDialog()` — selector de armazém, tabela editável, imprimir folha, aplicar
    ajustes + resumo de diferenças. Reusa `inventoryService.adjustStock` e
    `inventoryService.getStocksByWarehouse`.
  - Wiring: `InventoryCountSheetPrintService` injectado (StockPanel ← MainFrame).

## Notas / limites

- Reutiliza o ajuste de stock existente (absoluto = quantidade contada), já auditado e com permissão
  MANAGER/ADMIN — nada de novo no domínio, só orquestração + a folha PDF.
- Contagem cega **de verdade**: a tabela do diálogo e a folha PDF não mostram a quantidade do sistema;
  a diferença só aparece **depois** de aplicar (no resumo).
- Não há "sessão de inventário" persistente nesta iteração (a contagem vive no diálogo até aplicar). Um
  passo futuro seria guardar a sessão para continuar mais tarde / auditar a folha completa.
