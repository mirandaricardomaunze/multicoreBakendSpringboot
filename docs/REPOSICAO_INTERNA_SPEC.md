# SPEC — Reposição interna: a encomenda da loja vira transferência entre armazéns

**Criado em:** 2026-08-20
**Módulo:** `comercial` + `inventory`
**Migração:** `V46__internal_replenishment.sql`
**Harness:** [REPOSICAO_INTERNA_HARNESS.md](REPOSICAO_INTERNA_HARNESS.md) (RI-01..30 auto, RI-50..60 manuais)
**Canónico afectado:** [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) §5

---

## 1. Problema

Uma cadeia com armazém central e várias lojas repõe stock todos os dias. Hoje o sistema tem as duas
pontas e **nada entre elas**:

| | Existe | Falta |
|---|---|---|
| A loja pedir | — | não há documento de pedido interno |
| O armazém entregar | `StockTransfer` (guia de transferência) | não sabe de que pedido veio |

A transferência cria-se **do zero**: escolhe-se origem, destino, e escreve-se produto a produto.
Numa reposição de quarenta artigos, alguém digita os quarenta — a partir de um papel, de uma
mensagem ou de memória. Cada linha é uma oportunidade de enganar-se na referência ou na quantidade,
e o erro só aparece quando a loja recebe o que não pediu.

E não fica registo do pedido: não se sabe quem pediu o quê, quando, nem se o que chegou foi o que
se pediu.

## 2. A armadilha que este desenho tem de evitar

Uma encomenda a um cliente termina em **factura**, e facturar **consome** stock
(`billOrder` → `registerMovement` negativo). Uma transferência **não é uma venda**: a mercadoria
muda de armazém e continua a ser da empresa.

Se a mesma encomenda pudesse gerar transferência **e** ser facturada, o stock saía duas vezes — e a
segunda saída viria do armazém de onde a mercadoria já tinha partido. É a mesma família dos três
defeitos mais caros deste projecto (IVA, saldo em dívida, margem): **a mesma regra em duas portas**.

**A trava não é um aviso na UI. É uma via declarada no documento** (ver §3), pela mesma mecânica
que já decide aprovação e formato desde [ENCOMENDA_DUAS_VIAS_SPEC](ENCOMENDA_DUAS_VIAS_SPEC.md).

---

## 3. Decisão — uma terceira via

`OrderKind` ganha `INTERNAL_REPLENISHMENT` ("Reposição interna").

| | `FORMAL_ORDER` | `PICKING_REQUEST` | `INTERNAL_REPLENISHMENT` |
|---|---|---|---|
| Para quem | cliente | cliente | **uma loja da própria empresa** |
| Aprovação da encomenda | sim (por valor) | não | **não** — quem aprova é a transferência |
| Documento | A4 | talão térmico | talão térmico |
| Circuito de separação | não | sim | **sim** |
| Termina em | factura | factura | **transferência entre armazéns** |
| Factura | sim | sim | **nunca** |
| Guia de remessa ao cliente | sim | não | **nunca** |

**R1 — A reposição interna nunca é facturada.** `billOrder` recusa-a. Não há cliente a quem cobrar:
a mercadoria não saiu da empresa.

**R2 — A reposição interna nunca gera guia de remessa ao cliente.** Não há cliente.

**R3 — Não há dupla aprovação.** A transferência já exige MANAGER/ADMIN para mover stock. Exigir
aprovação também na encomenda seria pedir duas vezes a mesma autorização, para o mesmo acto.

**R4 — O stock move-se uma só vez, na aprovação da transferência.** A encomenda de reposição não
reserva nem deduz nada — é um pedido. Idêntico ao que a cotação faz face à encomenda.

### 3.1 O armazém de destino

A encomenda passa a ter **destino** além de origem:

- `warehouse` — de onde a mercadoria sai (o armazém central). Já existia.
- `destinationWarehouse` — para onde vai (a loja que pediu). Novo, **obrigatório** na reposição
  interna e nulo em tudo o resto.

Sem destino gravado, a conversão teria de o perguntar outra vez e nada garantiria que a mercadoria
chegava a quem a pediu.

**R5 — Origem e destino têm de ser armazéns diferentes**, ambos da empresa activa.

---

## 4. Encomenda → Transferência

**R6.** Converte-se uma reposição interna **aprovada (`PENDING`) ou separada (`SEPARATED`)**.

`SEPARATED` entra porque é o fim natural do circuito do armazém: o pedido foi impresso em talão, os
artigos foram recolhidos das prateleiras e conferidos. É esse o momento em que a carrinha carrega.

**R7 — A conversão copia as linhas verbatim** (artigo e quantidade). Não reapreça e não consulta o
catálogo: a transferência não tem preços, tem mercadoria.

**R8 — Converter trava a encomenda.** `PENDING`/`SEPARATED` → `TRANSFER_PENDING`, no mesmo molde do
`GUIDE_PENDING` da guia de remessa. Uma encomenda travada não converte outra vez.

**R9 — O que a transferência decide, a encomenda segue:**

| Transferência | Encomenda |
|---|---|
| aprovada (stock move-se) | `TRANSFERRED` — terminal |
| rejeitada | volta a `PENDING`, para se corrigir e converter de novo |
| cancelada | volta a `PENDING` |

A ligação é gravada nos dois sentidos (`Order.stockTransferId`/`transferNumber` e
`StockTransfer.orderId`/`orderNumber`), para que qualquer dos documentos leve ao outro.

---

## 5. Transferência → Encomenda (registo retroactivo)

**R10.** O armazém que já transferiu sem pedido formal pode registar a encomenda em falta a partir
da transferência.

Isto é **registo, não criação de compromisso**. A encomenda gerada:

- nasce já `TRANSFERRED` — a mercadoria mudou de armazém, não há nada a preparar;
- **não move stock nenhum** (R4 vale aqui com mais força: mover seria contar a mesma saída duas vezes);
- é `INTERNAL_REPLENISHMENT`, logo nunca facturável;
- herda origem, destino e linhas da transferência.

**R11 — Uma transferência regista no máximo uma encomenda.** Já ter `orderId` recusa nova tentativa.
Duas encomendas para a mesma mercadoria transferida contariam o mesmo pedido duas vezes em qualquer
relatório de reposição.

**R12 — Só transferências aprovadas.** Registar o pedido de uma transferência ainda por aprovar
inventaria um facto que não aconteceu.

---

## 6. Fronteira HTTP

| Verbo | Rota | Faz |
|---|---|---|
| `POST` | `/api/comercial/orders/{id}/transfer` | R6 — converte em transferência |
| `POST` | `/api/inventory/stock-transfers/{id}/order` | R10 — regista a encomenda em falta |

`CreateOrderRequest` ganha `destinationWarehouseId` opcional (construtor retrocompatível).
`OrderDTO` ganha `destinationWarehouseId`, `destinationWarehouseName`, `stockTransferId`,
`transferNumber`. `StockTransferDTO` ganha `orderId`/`orderNumber`.

---

## 7. Migração V46 — retroactiva conservadora

```sql
alter table customer_orders add column if not exists destination_warehouse_id bigint;
alter table customer_orders add column if not exists stock_transfer_id bigint;
alter table customer_orders add column if not exists transfer_number varchar(40);
alter table stock_transfers add column if not exists order_id bigint;
alter table stock_transfers add column if not exists order_number varchar(40);
```

Todas as colunas nascem nulas. Nenhuma encomenda ou transferência existente muda de via, de estado
ou de comportamento: a via só é `INTERNAL_REPLENISHMENT` para documentos criados como tal.

---

## 8. Limites declarados (v1)

- **Não há recepção conferida no destino.** A transferência aprovada dá a mercadoria por chegada.
  Conferir à chegada existe nas **compras** (`GoodsReceiptDiscrepancy`) e não foi estendido aqui;
  é o candidato natural à v2, e o sítio onde divergências entre pedido e entrega apareceriam.
- **Não há reposição automática por mínimos.** O pedido é sempre humano. O `ReorderService` já
  calcula o que falta, mas não cria encomendas sozinho.
- **A reposição interna não passa pela contabilidade.** Movimento entre armazéns da mesma empresa
  não é compra nem venda; não há lançamento a fazer.
- **Uma encomenda de reposição serve um só destino.** Repor três lojas são três encomendas.
