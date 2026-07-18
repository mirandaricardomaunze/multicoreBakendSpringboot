# Inventário físico (contagem cega + reconciliação)

**Última actualização:** 2026-07-12
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
- **Sessão de inventário persistente (feito, 2026-07-11):** a contagem passou a ser uma **sessão**
  guardada (`InventoryCount` + `InventoryCountLine`, migração `V28`). Fluxo: **Inventário Físico** →
  gestor de sessões (criar / retomar rascunho / aplicar / cancelar). Uma sessão nasce `DRAFT` com uma
  linha por artigo do armazém; as contagens guardam-se (**Guardar Rascunho**) e retomam-se mais tarde;
  ao **Aplicar Ajustes** cada linha contada gera um ajuste (via `InventoryService.adjustStock`), a
  sessão passa a `APPLIED` e o histórico (sistema → contado por artigo) fica auditável e só-leitura.
  Serviço `InventoryCountService` (MANAGER/ADMIN + auditoria + tenant); testes `InventoryCountServiceTest`.
- **API REST (feito, 2026-07-12):** `InventoryCountController` expõe as sessões em `/api/inventory/counts`
  — `GET` (lista por empresa) · `GET /{id}` (detalhe) · `POST` (criar — `CreateInventoryCountRequest`) ·
  `PUT /{id}/counts` (guardar contagens — `SaveCountsRequest`) · `POST /{id}/apply` (aplicar) ·
  `POST /{id}/cancel` (cancelar). Só HTTP → delega no `InventoryCountService` (mesma lógica/regras da UI
  desktop, que chama o serviço em processo). Sem lógica no controller, sem entidades expostas.
- **Reconciliação sem no-op (corrigido, 2026-07-12):** ao aplicar, uma linha cuja **contagem é igual à
  quantidade do sistema** não gera ajuste (evita o `BusinessRuleException` de delta zero do `adjustStock`)
  mas fica na mesma marcada como aplicada, com o `systemQuantity` registado. Assim uma sessão em que o
  contador confirma valores já correctos **aplica-se sem rollback**. Coberto por `InventoryCountServiceTest`.
