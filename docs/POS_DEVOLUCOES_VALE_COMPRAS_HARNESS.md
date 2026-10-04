# Matriz de Testes & Harness: Devoluções e Trocas com Vale de Compras no POS

**Código:** `HARNESS-POS-VOUCHER-001`  
**Referência:** `SPEC-POS-VOUCHER-001`  
**Data:** 2026-09-27  

---

## 1. Objectivo

Garantir a integridade arquitetural, semântica, financeira e ergonómica do fluxo de Devoluções e Trocas no POS com emissão e utilização de Vale de Compras (Store Credit).

---

## 2. Casos de Teste do Backend (`PosVoucherHarnessTest`)

| ID | Cenário | Comportamento Esperado |
| :--- | :--- | :--- |
| **VOUCH-01** | Devolução com método `STORE_CREDIT` | Cria `CreditNote` aprovada, emite `StoreVoucher` com código único, status `ACTIVE`, saldo igual ao total devolvido e validade de 90 dias. |
| **VOUCH-02** | Devolução com métodos tradicionais (`CASH`, `CARD`) | Preserva comportamento original: `CASH` gera movimento na gaveta, `CARD` na tesouraria, sem emitir voucher (`voucher == null`). |
| **VOUCH-03** | Consulta de vale existente por código | Retorna `StoreVoucherDTO` com saldo, beneficiário e validade corretos. |
| **VOUCH-04** | Consulta de vale inexistente | Lança `BusinessRuleException` informativa indicando que o vale não foi encontrado. |
| **VOUCH-05** | Checkout com pagamento integral via `STORE_CREDIT` | Abate o total da venda no voucher; se saldo ficar zero, altera status para `FULLY_REDEEMED`. Regista `PaymentEntry`. |
| **VOUCH-06** | Checkout com abate parcial via `STORE_CREDIT` | Reduz o saldo remanescente do voucher e transita status para `PARTIALLY_USED`. |
| **VOUCH-07** | Tentativa de resgate com saldo insuficiente no vale | Lança `BusinessRuleException` indicando saldo insuficiente e informando valor disponível vs valor a pagar. |
| **VOUCH-08** | Tentativa de resgate de vale já totalmente resgatado ou inativo | Lança `BusinessRuleException` bloqueando a transação. |
| **VOUCH-09** | Tentativa de resgate de vale expirado | Atualiza status para `EXPIRED` e lança `BusinessRuleException`. |
| **VOUCH-10** | Isolamento multi-tenant de vales | Impede consulta ou resgate de vale pertencente a outra empresa. |

---

## 3. Casos de Teste de Interface Desktop (`PosVoucherUiHarnessTest`)

| ID | Cenário | Comportamento Esperado |
| :--- | :--- | :--- |
| **UI-VOUCH-01** | Opção de Vale de Compras no `PosReturnDialog` | ComboBox de métodos inclui a opção "VALE DE COMPRAS (Store Credit)". |
| **UI-VOUCH-02** | Confirmação de devolução com Vale | Exibe código do vale, valor em MT e validade com formatação profissional. |
| **UI-VOUCH-03** | Método Vale de Compras no `PosPaymentDialog` | Botão/seletor contém "Vale de Compras" com ícone `fas-ticket-alt`. |
| **UI-VOUCH-04** | Verificação de código de vale no checkout | Permite introduzir código e valida saldo disponível reativamente. |
| **UI-VOUCH-05** | Restrição de linhas de código Swing | Todos os arquivos Swing modificados (`PosReturnDialog`, `PosPaymentDialog`, `POSPanel`) cumprem rigorosamente `< 1000` linhas. |
| **UI-VOUCH-06** | Inexistência de emojis Unicode em elementos Swing | Proibido uso de emojis crus em botões, abas, labels e tabelas. |
