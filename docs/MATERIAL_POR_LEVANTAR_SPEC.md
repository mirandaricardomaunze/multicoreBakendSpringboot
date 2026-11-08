# Spec — Material pago por levantar (SA-C07)

**Data:** 2026-11-08 · **Estado:** feito (F2, primeira capacidade) · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Harness:** `docs/SECTORES_ACTIVIDADE_HARNESS.md`

## Problema

No estaleiro o cliente paga 200 sacos de cimento e levanta 20 por semana. Ninguém sabe o saldo, o
camião sai com mais do que foi pago e a contagem física nunca bate, porque há material vendido no chão.

## Decisões

1. **A fatura continua a ser a venda.** Emitida, já tirou o material do stock vendável, como sempre.
   O levantamento **nunca mexe no stock**: regista só a entrega física. Nada muda na fatura, no IVA,
   no SAF-T nem na tesouraria.
2. **Conta de levantamento por fatura** (`pickup_accounts`). Só para faturas emitidas (paga, a crédito
   ou paga em parte) que não saíram com guia de remessa. Uma fatura tem no máximo uma conta. Prazo opcional.
3. **Talão LV** (`material_pickups`, série interna `LV`, não fiscal): quem levanta, documento, viatura,
   linhas e quantidades. Imprime-se em A4 com assinaturas do fiel e de quem recebe.
4. **Saldo por linha** = faturado − creditado (notas de crédito **aprovadas**) − levantado (talões emitidos).
   Nunca negativo. O servidor recusa entregar mais do que o saldo.
5. **Concorrência:** a conta fica bloqueada (`PESSIMISTIC_WRITE`) enquanto se emite o talão, por isso dois
   postos nunca gastam o mesmo saldo. Uma `requestKey` torna o pedido idempotente: um duplo clique ou
   uma nova tentativa depois de falha de rede devolve o mesmo talão.
6. **Estados:** a conta fecha sozinha quando tudo foi levantado. Anular um talão (só gestão, com motivo)
   devolve a quantidade e reabre a conta. Fatura anulada: a conta aparece como cancelada e não entrega mais.
7. **Prazo vencido:** só gestão pode entregar depois do prazo.
8. **Preço:** fica o da fatura, porque já foi faturado. Isto responde à decisão em aberto da SPEC mestre
   (§7.3). O prazo máximo é opcional e é a empresa que o define, fatura a fatura.
9. **Material no estaleiro:** um relatório soma os saldos abertos por artigo e armazém, para explicar a
   diferença numa contagem física.

## API (`/api/pickups`: vendedores e gestão; anular só gestão)

| Método | Caminho | Uso |
|---|---|---|
| GET | `/accounts?companyId=&status=` | Lista de contas |
| GET | `/accounts/{id}?companyId=` | Saldo por linha e talões |
| POST | `/accounts` | Registar fatura como material por levantar |
| GET | `/eligible-invoices?companyId=&search=` | Faturas que ainda podem ser registadas |
| GET | `/pending-stock?companyId=` | Material vendido ainda no estaleiro |
| POST | `/slips` | Emitir talão LV |
| POST | `/slips/{id}/cancel` | Anular talão (gestão) |
| GET | `/slips/{id}/pdf?companyId=` | Talão em PDF |

Auditoria: `PICKUP_ACCOUNT_OPEN`, `MATERIAL_PICKUP_ISSUE`, `MATERIAL_PICKUP_CANCEL`.

## Interface

Comercial › **Pedidos & Separação** › secção **Material por Levantar**. Visível aos vendedores, que estão
ao balcão do estaleiro. À esquerda as faturas; à direita o saldo por artigo e os talões. A acção principal
"Entregar Material" tem quantidades editáveis, um botão "Levantar tudo" e oferece logo a impressão do talão.
O botão do armazém mostra o material vendido que ainda está no estaleiro.

## Limites conhecidos

- Uma nota de crédito sobre material já levantado não é bloqueada pelo módulo das notas; o saldo fica
  em zero e não negativo. Ligar esta regra ao fluxo das notas de crédito fica para depois.
- Sem modo offline: se a internet falhar, o talão não se emite (SA-C20, fase seguinte).
