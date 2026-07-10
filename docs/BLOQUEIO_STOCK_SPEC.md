# Bloqueio de stock (contagem cega / visível só para admin)

**Última actualização:** 2026-07-10
**Estado:** feito.

## Objectivo

Permitir ao **ADMIN trancar o stock** para que os funcionários (não-admins) **não vejam as
quantidades** — útil na **hora do inventário** (contagem cega: o funcionário conta de facto em vez de
copiar o número do sistema) e como controlo geral ("stock visível só para admin"). O ADMIN vê sempre.

## Comportamento

- **Estado por empresa:** `companies.stock_count_locked` (migração `V27`, default `false`). Ligado/
  desligado só pelo **ADMIN**, auditado (`STOCK_LOCK`).
- **Quando trancado**, utilizadores **sem papel ADMIN** vêem as quantidades **mascaradas** (`•••`) nas
  vistas de stock:
  - **Níveis de Stock:** Qtd Unidades, Qtd Caixas e Estado (EM STOCK/BAIXO/ESGOTADO).
  - **Alertas:** valor de Stock (esgotados) e Qtd (validade); o resumo passa a "Quantidades ocultas…".
  - **Lotes & Validades:** Quantidade.
  - **ADMIN vê sempre os valores reais** (o mascaramento é `trancado && !ADMIN`).
- **Não mascarado:** identificação (SKU/nome/código de barras), **preço**, datas de validade/dias e
  estado de validade (VENCIDO/…), e **Movimentos & Rastreabilidade** (trilho de auditoria mantém-se
  íntegro). O objectivo é ocultar *quantidades actuais*, não a identidade nem a rastreabilidade.
- **UI:** botão **"Trancar Stock" / "Destrancar Stock"** no topo do painel de Stock (só visível a ADMIN,
  ícone cadeado) + **banner** de aviso quando trancado (amarelo). Alternar recarrega as tabelas.
- **À prova de falha:** se a leitura do estado falhar, **não** oculta (mostra normalmente) — nunca
  bloqueia trabalho por erro de leitura.

## Peças

- **Migração `V27__company_stock_count_lock.sql`** — coluna `stock_count_locked`.
- **`Company.stockCountLocked`** — campo + Lombok (`isStockCountLocked`/`setStockCountLocked`).
- **`InventoryService`**
  - `isStockCountLocked(companyId)` — leitura tenant-scoped.
  - `setStockCountLocked(companyId, locked)` — **ADMIN** (`PermissionGuard.requireAdmin`) + auditoria.
- **`StockPanel`**
  - `isAdmin()`, `stockHidden()` (= trancado && !ADMIN, à prova de falha), `refreshStockLock()`
    (banner + texto/ícone do botão), `toggleStockLock()`.
  - Mascaramento (`MASK = "•••"`) em `filterStocks`, `loadAlerts`, `filterBatches`.

## Notas / limites

- Superadmin não usa este painel (sem empresa) — não afectado.
- O **PDF de inventário** e o **POS** não são mascarados nesta iteração (âmbito: vistas de Stock). Um
  passo natural seria uma **folha de contagem sem quantidades** para impressão.
- O estado é lido do servidor a cada abertura/alternância do painel (desktop chama o serviço
  in-process); mudanças por outro utilizador reflectem-se ao reabrir o painel.
