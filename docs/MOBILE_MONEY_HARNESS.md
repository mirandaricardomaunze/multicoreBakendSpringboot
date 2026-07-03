# Harness — Pagamento por Mobile Money (M-Pesa / e-Mola)

> Cenários para [MOBILE_MONEY_SPEC.md](MOBILE_MONEY_SPEC.md).
> MM-01 automático (`POSServiceTest`); MM-50..MM-53 manuais (UI POS).

**Última actualização:** 2026-07-03

## Automático — `POSServiceTest`

| ID    | Cenário | Esperado |
|-------|---------|----------|
| MM-01 | Checkout com pagamento `MPESA` (50, referência MP-ABC123, conta de tesouraria). | Entra na **tesouraria** (`DEBIT` 50.00); **não** move a gaveta (`tillMovementRepository` nunca chamado). |

## Manuais (UI POS)

| ID    | Passos | Esperado |
|-------|--------|----------|
| MM-50 | POS → finalizar venda → método **"M-Pesa"** → preencher **Referência** → confirmar. | Venda paga; entrada na tesouraria; recibo mostra "M-Pesa" e a referência. |
| MM-51 | Igual com **"e-Mola"**. | Recibo mostra "e-Mola". |
| MM-52 | M-Pesa/e-Mola **sem conta de tesouraria** seleccionada. | Erro "Conta de tesouraria é obrigatória". |
| MM-53 | Devolver uma venda paga por M-Pesa → método de reembolso **MPESA** + conta. | Reembolso sai da tesouraria (`CREDIT`); nota de crédito emitida. |

## Verificação

- `mvn clean test` → verde (inclui MM-01). Fecho de caixa inalterado (electrónico nunca conta para a gaveta).
