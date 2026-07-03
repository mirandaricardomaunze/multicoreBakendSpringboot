# Spec — Pagamento por Mobile Money (M-Pesa / e-Mola)

> Aceitar **M-Pesa (Vodacom)** e **e-Mola (Movitel)** como métodos de pagamento no POS e na
> devolução, tratados como electrónicos (entram na tesouraria, não na gaveta) e com referência da
> transação.

**Última actualização:** 2026-07-03

## Problema

O dinheiro móvel é o meio de pagamento nº1 em Moçambique, mas o sistema só aceitava
`CASH / CARD / BANK_TRANSFER / CREDIT`. As vendas por M-Pesa/e-Mola eram registadas à força como
"Transferência", sem distinção nem referência da transação — mau para conciliação.

## Decisão

- **Enum `PaymentMethod`** ganha `MPESA` e `EMOLA`. Persistido como texto (`EnumType.STRING`,
  coluna `method` length 20) — **sem migração** (valores novos cabem na coluna existente).
- **Comportamento = electrónico** (igual a CARD/BANK_TRANSFER): entra **directamente na tesouraria**
  (`financeService.registerTransaction(..., "DEBIT", ...)`), **não na gaveta de numerário**, e
  **exige conta de tesouraria**. A `reference` (já existente em `PaymentEntry`) guarda o **ID/
  comprovativo da transação** móvel.
- **Devolução/reembolso:** M-Pesa/e-Mola reembolsam por tesouraria (`CREDIT`), como cartão.
- **Recibo:** `ReceiptPrintService.methodLabel` mostra "M-Pesa" / "e-Mola".
- **UI POS** (`askPayment`): combo passa a ter "M-Pesa" e "e-Mola"; campo **"Referência"** (activo
  para métodos electrónicos) recolhe o ID da transação. Diálogo de devolução inclui os dois métodos.

## Não-objetivos

- **Não** integrar a API real da Vodacom/Movitel (confirmação automática do pagamento) — o operador
  confirma manualmente que recebeu e regista o ID. Integração de API fica como evolução futura.
- Não mexer no cálculo de valores/IVA (o método de pagamento não afecta o total).
- Não alterar o fecho de caixa: métodos electrónicos nunca contaram para a gaveta, continua igual.
