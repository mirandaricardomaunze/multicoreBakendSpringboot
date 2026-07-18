# Recepção Parcial de Encomenda a Fornecedor — Especificação

> Fonte de verdade sobre como uma encomenda a fornecedor pode ser **recebida em partes**.
> Lê antes de mexer na recepção de `modules/purchases/`. Harness em
> [RECECAO_PARCIAL_HARNESS.md](RECECAO_PARCIAL_HARNESS.md). Contexto do ciclo da encomenda em
> [tasks/current.md](../tasks/current.md) (Compras, Fases 1–4).

**Última actualização:** 2026-07-01

---

## 1. Problema

A recepção de encomenda a fornecedor (`PurchaseOrderService.receiveOrder`) é **tudo-ou-nada**:
recebe todas as linhas pela quantidade total e fecha como `RECEIVED`. Na prática o fornecedor
entrega **parte** da encomenda; o resto vem depois. Hoje não há forma de receber 6 de 10 e manter
4 em aberto.

---

## 2. Decisão

A linha passa a registar **quanto já foi recebido** e a encomenda ganha um estado intermédio.

- `PurchaseOrderLine.receivedQuantity` (default 0). **Em falta = quantity − receivedQuantity.**
- Novo estado `PARTIALLY_RECEIVED`. Ciclo: `ORDERED → PARTIALLY_RECEIVED* → RECEIVED` / `CANCELLED`.
- Cada recepção (total ou parcial) gera **entrada de stock só pela quantidade recebida nesse acto**
  (movimento `PURCHASE`, FEFO/lote, validade da linha), MANAGER/ADMIN, auditada.

---

## 3. Âmbito

### 3.1 `receivePartial(orderId, itens)` — recepção parcial
- `itens` = lista de `(lineId, quantity)` a receber **agora**.
- **Permissão:** MANAGER/ADMIN. **Estado:** `ORDERED` ou `PARTIALLY_RECEIVED`.
- Por item: `quantity > 0`; `quantity ≤ emFalta` (não se recebe mais do que o encomendado);
  regista o movimento de stock pela `quantity`; soma a `receivedQuantity` da linha.
- **Recálculo de estado** no fim:
  - todas as linhas com `receivedQuantity == quantity` → `RECEIVED` (+ `receivedAt`);
  - alguma linha recebida mas nem todas completas → `PARTIALLY_RECEIVED`.

### 3.2 `receiveOrder(id)` — receber tudo o que falta (conveniência)
- Recebe o **em falta** de cada linha (de `ORDERED` **ou** `PARTIALLY_RECEIVED`) e fecha `RECEIVED`.
- Reutiliza a mesma lógica de movimento por quantidade em falta (sem dupla entrada do já recebido).

### 3.3 `cancelOrder(id, motivo)`
- Passa a aceitar `ORDERED` **e** `PARTIALLY_RECEIVED`. O stock já recebido **mantém-se** (não há
  reversão); cancela-se o que falta. Continua a exigir motivo + MANAGER/ADMIN + auditoria.

---

## 4. Regras / invariantes

1. **Nunca receber mais do que o encomendado** por linha (`receivedQuantity ≤ quantity`).
2. **Stock entra só pela quantidade recebida no acto** — sem recontar o já recebido.
3. **Estado é derivado**, não escrito à mão: função única de recálculo a partir das linhas.
4. **Idempotência de entrada:** receber uma linha já completa não move stock (em falta = 0 → erro
   claro "linha já totalmente recebida" / ignorada conforme o caminho).
5. Permissão e auditoria iguais à recepção total já existente.

---

## 5. Exposição

- **API:** `POST /api/purchases/orders/{id}/receive-partial` (corpo: `{lines:[{lineId,quantity}]}`).
  Mantém-se `POST /api/purchases/orders/{id}/receive` (receber tudo).
- **UI (`ComprasPanel`):** na tab «Encomendas a Fornecedor», botão **"Receber Parcial…"** abre um
  modal com as linhas, a quantidade **em falta** e uma coluna editável **"A receber agora"**;
  confirma → `receivePartial`. O botão "Receber" existente continua a receber tudo.

---

## 6. Mapa de ficheiros

| Quero… | Ficheiro |
|--------|----------|
| Lógica de recepção | `modules/purchases/service/PurchaseOrderService.java` |
| Quantidade recebida por linha | `modules/purchases/model/PurchaseOrderLine.java` |
| Estado da encomenda | `modules/purchases/model/PurchaseOrder.java` |
| Pedido de recepção parcial | `modules/purchases/dto/ReceivePurchaseOrderRequest.java` |
| Coluna em falta/recebido no DTO | `modules/purchases/dto/PurchaseOrderLineDTO.java` |
| Endpoint | `modules/purchases/controller/PurchaseController.java` |
| Migração | `db/migration/V19__purchase_order_partial_receipt.sql` |
| UI | `gui/ComprasPanel.java` |
| Cenários | [RECECAO_PARCIAL_HARNESS.md](RECECAO_PARCIAL_HARNESS.md) |
