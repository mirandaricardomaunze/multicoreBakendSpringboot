# Especificação Canónica — Devoluções Assistidas de Faturação no Backoffice com Reentrada FEFO

## 1. Contexto e Justificação de Negócio

No fluxo comercial e logístico em Moçambique, a devolução de artigos faturados pelo cliente (por avaria de transporte, excesso de fornecimento, erro de pedido ou acordo comercial) exige controlo fiscal e rastreabilidade rigorosa de armazém:
1. **Controlo Fiscal:** Emissão de Nota de Crédito (NC) nos termos da legislação moçambicana, referenciando formalmente a Fatura original.
2. **Prevenção de Sobre-Devolução:** Nenhuma linha de fatura pode ter mais unidades devolvidas do que as que foram faturadas menos o somatório de devoluções anteriores não anuladas.
3. **Destino Físico do Artigo:**
   - **Reintegração em Armazém (`RESTOCK`):** O artigo retorna ao stock vendável no armazém de expedição (ou armazém selecionado), respeitando o lote original e a data de validade (FEFO).
   - **Avaria / Perda Comercial (`DAMAGE_DISCARD`):** O crédito financeiro é concedido ao cliente, mas o produto não reentra no stock disponível de venda.
4. **Decomposição de Embalagens:** Exibição clara e conversão multinível em caixas (`Cx.`), embalagens (`Emb.`) e unidades soltas, garantindo que o armazém confere embalagens fechadas ou frações com rigor (`0.00`).

---

## 2. Contratos e DTOs (`contracts`)

1. `ReturnableInvoiceLineDTO`:
   - `invoiceLineId` (Long)
   - `productId` (Long)
   - `productCode` (String)
   - `productName` (String)
   - `unitPrice` (BigDecimal)
   - `taxRate` (BigDecimal)
   - `invoicedQuantity` (BigDecimal)
   - `alreadyReturnedQuantity` (BigDecimal)
   - `returnableQuantity` (BigDecimal)
   - `boxes` (BigDecimal - com percentagem fracionada `0.75` ou `0.00`)
   - `packsPerBox` (int)
   - `unitsPerPack` (int)
   - `batchNumber` (String)
   - `expiryDate` (LocalDate)

2. `SalesReturnWizardRequest`:
   - `invoiceId` (Long)
   - `reason` (String: "RETURN", "DEFECT", "COMMERCIAL_DISCOUNT")
   - `warehouseId` (Long)
   - `autoApprove` (boolean)
   - `description` (String)
   - `lines` (List<SalesReturnWizardLineRequest>)

3. `SalesReturnWizardLineRequest`:
   - `invoiceLineId` (Long)
   - `quantity` (BigDecimal)
   - `restock` (boolean)

4. `SalesReturnWizardResultDTO`:
   - `creditNoteId` (Long)
   - `creditNoteNumber` (String)
   - `invoiceId` (Long)
   - `invoiceNumber` (String)
   - `status` (String: "PENDING_APPROVAL" ou "APPROVED")
   - `totalCredited` (BigDecimal)
   - `restockedLinesCount` (int)
   - `message` (String)

---

## 3. Backend Headless (`backend`)

- `SalesReturnService`:
  - `getReturnableLines(Long invoiceId)`: obtém linhas da fatura cruzando com `sumNonVoidedReturnedByInvoiceLineId` e calcula `returnableQuantity` e caixas.
  - `processReturn(SalesReturnWizardRequest request)`:
    - Valida permissões e existência da fatura.
    - Valida que `quantity <= returnableQuantity`.
    - Invoca `CreditNoteService.create(...)`.
    - Se `autoApprove == true` e o utilizador tiver perfil de gestão/admin, invoca `CreditNoteService.approve(...)`.
- `SalesReturnController`:
  - `GET /api/comercial/invoices/{id}/returnable-lines`
  - `POST /api/comercial/invoices/{id}/sales-return`

---

## 4. Desktop Client (`desktop`)

- `ComercialApiClient`:
  - `getReturnableInvoiceLines(Long invoiceId)`
  - `processSalesReturn(Long invoiceId, SalesReturnWizardRequest request)`
- `SalesReturnDialog`:
  - Modal ergonómico conforme `applyStandardModalBounds`.
  - Tabela com linhas faturadas, quantidades já devolvidas e editor de quantidade a devolver.
  - Seleção de destino (Reintegrar Stock vs Avaria Comercial).
  - Seleção de armazém de destino.
  - Resumo em tempo real do crédito a conceder.
  - Botão de ação "Confirmar Devolução & Emitir NC".
- Integração nas vistas de faturação:
  - Botão de ação "Devolver Fatura (NC)" em `CommercialInvoicesView` / `ComercialPanel`.

---

## 5. Harness e Verificação

- `SalesReturnServiceTest`: valida cálculo de linhas devolvíveis e criação/aprovação atómica de Nota de Crédito.
- `SalesReturnUiHarnessTest`: valida DTOs de devolução, cálculo de caixas e limites de devolução.
- `MultiModuleArchitectureHarnessTest`: garante preservação dos limites físicos.
- `DesktopThinContextTest`: garante que o arranque Swing permanece 100% funcional.
