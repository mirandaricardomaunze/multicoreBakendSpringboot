# Spec — Adiantamentos e sinais de clientes (SA-C10)

**Data:** 2026-11-08 · **Estado:** feito (servidor e desktop), por rever visualmente · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

Bolo de aniversário com 50% de sinal, carne para festa paga em parte, empreiteiro que adianta dinheiro
antes da obra, blocos por medida. Hoje o dinheiro entra sem documento próprio, ou entra como venda.

## Decisões do responsável (2026-11-08)

1. **Tratamento fiscal: o recomendado e seguro.** Ao receber o sinal emite-se uma **fatura de
   adiantamento com IVA** (documento fiscal, série FT, entra no SAF-T). Não se usa documento interno.
   Confirmar com o jurista/contabilista a redacção do documento e o momento da exigibilidade.
2. **Conta contabilística: saldo credor em Clientes (2101).** Não se cria conta nova.

## Circuito

| Passo | Documento | Caixa | Contabilidade (lançamentos existentes) |
|---|---|---|---|
| 1. Recebe o sinal | Fatura FT do artigo "Adiantamento" (serviço, sem stock), com IVA | — | D Clientes / C Vendas + C IVA liquidado |
| 2. Paga no acto | Recibo RC normal | Entrada | D Caixa/Banco / C Clientes |
| 3. Fatura final | Fatura FT normal da encomenda | — | D Clientes / C Vendas + C IVA |
| 4. Regulariza o sinal | Nota de crédito NC sobre a fatura do passo 1, motivo "Regularização de adiantamento" | — | D Vendas + D IVA / C Clientes |
| 5. Compensa | Recibo de compensação sobre a fatura final, valor = sinal | **Não mexe** | **Nenhum** (os passos 3 e 4 já acertam a 2101) |

Resultado: IVA do sinal liquidado no recebimento e estornado na regularização; IVA total = IVA da fatura
final; a conta 2101 fica com o que o cliente ainda deve; a caixa só registou dinheiro uma vez.

## Alterações ao núcleo (cuidado máximo, com harness)

1. **Preço indicado só para o artigo de adiantamento.** `CreateInvoiceLineRequest` ganha `unitPrice`
   opcional, aceite pelo servidor **apenas** quando o produto é o artigo de sistema "ADIANTAMENTO" e o
   utilizador é gestor; em qualquer outro produto é ignorado (o preço continua a vir do produto/tabela).
2. **Artigo de sistema "ADIANTAMENTO"** por empresa: serviço, sem stock, IVA da taxa normal, criado no
   arranque (idempotente), não editável nem vendável no POS.
3. **Recibo de compensação.** Novo `paymentMethod = "COMPENSACAO_ADIANTAMENTO"`: não chama a tesouraria e
   não publica `PaymentReceivedEvent`. A anulação desse recibo também não mexe na tesouraria nem estorna
   caixa. Cobrir com harness: anular um recibo normal continua igual; anular uma compensação não toca caixa.
4. **Nota de crédito automática** pelo serviço de notas existente (aprovação incluída), nunca por SQL.

## Módulo `advances`

- `CustomerAdvance`: empresa, cliente, encomenda opcional, fatura do sinal, recibo, valor, estado
  (`OPEN`, `APPLIED`, `REFUNDED`), fatura final, nota de crédito, recibo de compensação, auditoria.
- Regras: sinal > 0; só sobre cliente identificado (não Consumidor Final); aplicar só a fatura do mesmo
  cliente; valor aplicado ≤ em dívida da fatura final (o excesso fica em aberto); concorrência com bloqueio
  da linha; idempotência por chave de pedido.
- Devolução do sinal (cliente desiste): nota de crédito + saída de caixa com motivo, só gestão.

## Interface

Módulo **Adiantamentos** na barra lateral (capacidade `ADVANCE_PAYMENTS`):
- lista com indicadores (em aberto, valor em aberto, aplicados no mês);
- "Receber sinal" (cliente, encomenda opcional, valor, forma de pagamento) que emite e imprime FT + RC;
- na fatura final, aviso "Este cliente tem X MT de sinal" com a acção "Aplicar adiantamento".

## Testes obrigatórios

Ponta a ponta por HTTP: sinal de 3.000 → FT + RC (caixa +3.000) → fatura final 10.000 → aplicar → NC de
3.000 + compensação → em dívida 7.000; caixa só +3.000; diário equilibrado; IVA total = IVA de 10.000.
Mais: aplicar duas vezes recusado, cliente diferente recusado, anulação da compensação sem tocar caixa,
paridade PostgreSQL, `ApiRolePolicy`.

## Implementação (2026-11-08)

- Módulo `advances` (`/api/advances`): listar (vendedor), receber e aplicar (gestão). Migração
  `V123__adiantamentos.sql` só acrescenta a tabela `customer_advances`.
- O artigo de sistema `SYS-ADIANT-<empresa>` é criado no primeiro sinal (não no arranque). Encomendas,
  cotações, POS e faturas sem preço indicado recusam-no (`Product#requireOrdinarySale`).
- O valor recebido inclui IVA; a linha da fatura leva o preço sem IVA.
- Aplicar move o valor pago da fatura do sinal para a fatura final e emite o recibo de compensação
  (`COMPENSACAO_ADIANTAMENTO`), que não mexe na tesouraria nem no diário e não entra no extracto.
- Anulação recusada para o recibo original de um sinal já aplicado e para o recibo de compensação.
- Desktop: módulo **Adiantamentos** (só gestão) com indicadores, "Receber Sinal" (imprime a fatura do sinal)
  e "Aplicar na Fatura".

## Limites conhecidos

- Aplica-se o sinal inteiro: a fatura final tem de ter em dívida pelo menos o valor do sinal.
- Um sinal aplicado não se desfaz no módulo; corrige-se com documentos normais (nota de débito, etc.).
- Ainda não existe: devolução do sinal ao cliente, aviso automático no editor da fatura final e PDF próprio do recibo RC.

**Parecer pedido:** `docs/PEDIDO_PARECER_FATURA_ADIANTAMENTO.md` (8 perguntas ao jurista/contabilista, enviado
para confirmação da redacção e do circuito fiscal).

Teste: `CustomerAdvanceHttpIntegrationTest` (caixa só uma vez, conta 2101, extracto, balancete equilibrado,
aplicar duas vezes, outro cliente, fatura de sinal como destino, anulações recusadas, idempotência,
Consumidor Final e artigo de sistema fora do módulo).
