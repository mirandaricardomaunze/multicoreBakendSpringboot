# Spec — Compras & Aprovisionamento Profissional

> Fonte de verdade do ciclo de compras: gestão de fornecedores, categorias de produto e
> **encomenda de fornecedor** (purchase order). Detalhe de camadas em [ARCHITECTURE.md](../ARCHITECTURE.md);
> ledgers em [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md).

**Última actualização:** 2026-06-25
**Estado:** Fases 1–4 implementadas e testadas.

---

## 1. Ponto de partida (o que já existia)

- **Fornecedores:** só criar + listar (5 campos). Sem editar, desactivar, pesquisar, telefone/contacto.
- **Categorias de produto:** backend CRUD completo (`ProductCategoryService`), mas sem ecrã de gestão
  — apenas um combo na criação de produto.
- **Faturas de Compra (`Purchase`, série `V/FT`):** entrada directa de stock, paga na tesouraria no
  acto. Não há **encomenda** (pedido ao fornecedor antes da recepção).

## 2. Âmbito desta spec

### Fase 1 — Gestão de fornecedores completa
- Campos novos: **telefone**, **pessoa de contacto**, **activo** (default `true`).
- Operações: criar, **editar**, **activar/desactivar** (soft-delete — preserva histórico), **pesquisar**
  por nome/NUIT (substring case-insensitive), listar.
- Regras: NUIT 9 dígitos (`TaxIdValidator`); guarda de empresa; desactivar exige **MANAGER/ADMIN** e é
  auditado. Fornecedor inactivo não pode ser usado em nova encomenda/compra.

### Fase 2 — Categorias de produto (ecrã de gestão)
- UI sobre o `ProductCategoryService` existente: listar, criar, editar, activar/desactivar.
- Activar/desactivar e editar exigem **MANAGER/ADMIN**.

### Fase 3 — Encomenda de Fornecedor (`PurchaseOrder`, série `EC-F`)
Documento de **pedido ao fornecedor** antes da entrega física.

| Aspecto      | Definição                                                                          |
|--------------|------------------------------------------------------------------------------------|
| Entidade     | `PurchaseOrder` + `PurchaseOrderLine`                                               |
| Série        | `EC-F` (encomenda a fornecedor), numeração gapless via `DocumentNumberService`      |
| Ciclo        | `ORDERED → RECEIVED` / `CANCELLED`                                                  |
| Stock        | **Não move stock na criação.** Só a **recepção** gera entrada (FEFO por lote).      |
| Recepção     | `receive(id)` → por linha, `StockMovement` tipo `PURCHASE` (entrada no lote/validade), respeitando o bloqueio de lote vencido (`addToBatch`). Estado → `RECEIVED`. **MANAGER/ADMIN**, auditado. |
| Cancelamento | `cancel(id, motivo)` — só se `ORDERED`. **MANAGER/ADMIN**, auditado. Não toca stock. |
| Pagamento    | A encomenda **não** paga. O pagamento ao fornecedor vive na Fatura de Compra / contas a pagar (Fase 4). |

**Princípio (mirror da encomenda de cliente):** tal como a `Order` de cliente não move stock até
faturar, a `PurchaseOrder` não move stock até **receber**. A recepção é o equivalente de aprovisionamento
à baixa de stock da venda.

## 3. Contrato (Service)

```
SupplierService/PurchaseService:
  createSupplier(req) / updateSupplier(id, req) / setSupplierActive(id, active) / searchSuppliers(companyId, query)
PurchaseOrderService:
  createOrder(CreatePurchaseOrderRequest) → PurchaseOrderDTO        // estado ORDERED
  receiveOrder(id)  → PurchaseOrderDTO                              // gera entradas de stock; RECEIVED
  cancelOrder(id, reason) → PurchaseOrderDTO                        // CANCELLED
  findOrdersByCompany(companyId) / searchOrders(companyId, query)
```

- DTOs em todas as fronteiras; nunca expor `@Entity`.
- `BusinessRuleException` em regras (fornecedor inactivo, recepção de encomenda já recebida/cancelada, etc.).
- `@Transactional` em escrita; `@Transactional(readOnly = true)` em leitura.
- Injecção por construtor.

## 4. Endpoints REST (`/api/purchases`)

| Método | Caminho                          | Efeito                          |
|--------|----------------------------------|---------------------------------|
| PUT    | `/suppliers/{id}`                | editar fornecedor               |
| PATCH  | `/suppliers/{id}/active?value=`  | activar/desactivar              |
| GET    | `/suppliers/search?companyId&q`  | pesquisar fornecedores          |
| POST   | `/orders`                        | criar encomenda (ORDERED)       |
| GET    | `/orders?companyId`              | listar encomendas               |
| POST   | `/orders/{id}/receive`           | receber (entra stock)           |
| POST   | `/orders/{id}/cancel`            | cancelar                        |

Tudo protegido pelo `SecurityInterceptor` (`/api/**`).

## 5. UI (desktop — `ComprasPanel`)

- Tab **Gestão de Fornecedores**: + pesquisar, **Editar**, **Activar/Desactivar**; colunas telefone/contacto/estado.
- Tab nova **Encomendas a Fornecedor**: formulário (fornecedor, armazém, linhas) + lista com **Receber**,
  **Cancelar**, **Imprimir** (PDF reutiliza building blocks de `printing`).
- Tab nova **Categorias** (ou no Stock): listar/criar/editar/activar.

### Fase 4 — Contas a Pagar (implementada)

A Fatura de Compra (`Purchase`) passou a registar **`amountPaid`** (em dívida = total − pago):
- **Compra a crédito:** `createPurchase` com `financeAccountId = null` não paga no acto (`amountPaid = 0`);
  com conta, paga tudo e regista saída de tesouraria (CREDIT) como antes. Migration `V15`.
- `findPayablesByCompany(companyId)` → faturas com saldo > 0 (exclui pagas/anuladas).
- `registerSupplierPayment(purchaseId, amount, accountId, reference)` → abate no saldo (cap no
  outstanding), saída de tesouraria CREDIT e auditoria (`SUPPLIER_PAYMENT`).
- API: `GET /api/purchases/payables`, `POST /api/purchases/{id}/pay`. UI: opção "— A crédito —" na
  compra + tab **Contas a Pagar** (lista + Registar Pagamento).

## 6. Não-objectivos (futuro)

- Recepção **parcial** de encomenda (split de linhas) — por agora a recepção é total.
- Ligação automática Encomenda→Fatura de Compra.
- Extracto consolidado por fornecedor (vários documentos) — hoje as contas a pagar são por fatura.
