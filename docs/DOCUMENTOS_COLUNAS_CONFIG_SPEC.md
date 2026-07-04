# Spec — Colunas configuráveis dos documentos comerciais

> Permitir definir, por empresa, **quais colunas aparecem** na tabela de linhas dos documentos
> comerciais (Fatura, Encomenda, Nota de Crédito, Guia de Remessa) — que partilham o
> `LineItemsTableRenderer`.

**Última actualização:** 2026-07-04

## Problema

A tabela de linhas dos documentos tinha **8 colunas fixas** (Cód. Barras · Referência · Descrição ·
Validade · Qtd · Preço Unit. · IVA · Subtotal). Cada loja tem preferências diferentes (ex.: não
quer mostrar código de barras nem validade na fatura ao cliente). Não havia forma de configurar.

## Decisão

- **Âmbito:** documentos **comerciais** que usam `LineItemsTableRenderer` (Fatura, Encomenda, NC,
  Guia). **Só mostrar/ocultar** colunas (sem reordenar nesta iteração).
- **Novo módulo `documents`** (SOLID, scaffold `phc-new-module`):
  - `DocumentColumnConfig` (entidade, extends `BaseEntity`) — **uma linha por empresa**, 8 flags
    booleanas (`show_barcode`, `show_reference`, `show_description`, `show_expiry`, `show_quantity`,
    `show_unit_price`, `show_tax`, `show_subtotal`), todas default `true`. Migração `V22`.
  - `DocumentColumnConfigRepository` (`findByCompanyId`).
  - `DocumentColumnsDTO` (record, 8 booleanos) — valor de fronteira; `DocumentColumnsDTO.all()`.
  - `DocumentConfigService`: `getColumns(companyId)` (default `all()` se não existir);
    `save(companyId, dto)` (**MANAGER/ADMIN** + guarda multi-tenant + auditoria
    `DOCUMENT_COLUMNS_UPDATE`; recusa esconder **todas** as colunas).
  - `DocumentConfigController`: `GET`/`PUT` `/api/documents/columns?companyId`.
- **Renderer:** novo overload `LineItemsTableRenderer.build(rows, DocumentColumnsDTO)` — monta larguras,
  cabeçalhos e células **apenas das colunas activas** (mantém alinhamento/formatação por coluna). O
  `build(rows)` antigo delega em `build(rows, all())` — **retrocompatível** (subtotal continua
  `LineCalculator.net`, matemática intacta).
- **Serviços de impressão** (`InvoicePrintService`, `OrderPrintService`, `CreditNotePrintService`,
  `GuideRemittancePrintService`) injectam `DocumentConfigService` e passam
  `getColumns(companyId)` ao renderer. Nenhuma mudança de cálculo.
- **UI (`ConfigPanel`):** secção/aba "Colunas dos Documentos" — 8 checkboxes (estado actual) + botão
  Guardar. Injecção via `MainFrame`.

## Não-objetivos

- Não reordenar colunas nem mudar larguras manualmente (evolução futura).
- Não configurar recibo térmico, mapa fiscal, folha de salário ou inventário (âmbito só comercial).
- Não tocar em totais/IVA/matemática — só na **presença visual** de colunas.
- Não configurar por documento individual (Fatura ≠ Encomenda): uma config cobre os quatro.
