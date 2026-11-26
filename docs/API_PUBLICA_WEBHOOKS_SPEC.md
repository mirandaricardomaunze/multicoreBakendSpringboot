# Spec — API pública e webhooks

Status: **aprovada**
Effective date: 2026-11-19
Harness: [API_PUBLICA_WEBHOOKS_HARNESS.md](API_PUBLICA_WEBHOOKS_HARNESS.md)
Goals: [GOAL-013](../tasks/goals/GOAL-013-api-publica.md) (API) ·
[GOAL-014](../tasks/goals/GOAL-014-webhooks.md) (webhooks)

## 1. Problema

Lojas online, contabilistas, ferramentas de BI e aplicações de entrega precisam de falar com o
sistema sem usar o desktop. Hoje a única API é a interna do desktop, com login de utilizador.

## 2. Decisões do responsável (2026-11-19)

| Tema | Decisão |
|---|---|
| O que a API faz | **Ler catálogo e criar encomendas**: artigos, preços, stock disponível, estado de encomendas; criar encomendas de cliente que ficam pendentes para a loja facturar. Nunca cria facturas nem mexe no stock directamente. |
| Webhooks | Avisam sistemas externos de vendas, facturas, mudanças de stock e de estado de encomendas. |

## 3. API pública (GOAL-013)

### 3.1 Chaves de API

- Chave por empresa, criada pelo **ADMIN** com nome (ex.: "Loja online") e permissões:
  `CATALOG_READ`, `ORDERS_READ`, `ORDERS_WRITE`.
- A chave completa (`mk_live_` + 32 bytes aleatórios em base62) é mostrada **uma única vez**; o
  servidor guarda só o **hash SHA-256** e os primeiros 8 caracteres para identificação (os 8 que vêm
  depois de `mk_live_`, porque o prefixo é igual em todas as chaves; mostrados como `mk_live_XXXXXXXX...`).
- Revogar é imediato. Cada chave regista a data do último uso.
- Criar e revogar geram auditoria `API_KEY_CREATE` / `API_KEY_REVOKE`.

### 3.2 Pedidos

- Prefixo versionado **`/api/public/v1/**`**, autenticação por cabeçalho `X-Api-Key`. Os tokens de
  utilizador **não** servem aqui e as chaves de API **não** servem em `/api/**` interno.
- A empresa é a da chave; não há `X-Company-Id`.
- Limite: **60 pedidos por minuto por chave** → `429` com `Retry-After`.
- Erros em JSON com `code` estável em inglês e `message` em PT-MZ; nunca stack traces.
- Paginação `page`/`size` (máx. 100), datas ISO 8601, valores com ponto decimal (é API, não ecrã).
- Documentação OpenAPI (springdoc) só do grupo público, em `/api/public/v1/docs`.

### 3.3 Recursos

| Rota | Permissão | Conteúdo |
|---|---|---|
| `GET /products` | `CATALOG_READ` | SKU, nome, categoria, preço de venda com IVA, taxa de IVA, unidade, código de barras, `updatedSince` opcional |
| `GET /products/{sku}` | `CATALOG_READ` | o mesmo, um artigo |
| `GET /stock` | `CATALOG_READ` | por SKU e armazém: quantidade **disponível** (stock − reservas); `warehouse` opcional |
| `GET /orders/{number}` | `ORDERS_READ` | estado, linhas, totais, factura associada (número) |
| `POST /orders` | `ORDERS_WRITE` | cria encomenda de cliente |

- Só artigos activos da empresa. Custos, margens e fornecedores **nunca** saem pela API.

### 3.4 Criar encomenda

- Corpo: `externalReference` (obrigatório, único por chave), cliente por `nuit` (existente ou criado
  com as regras do registo manual) ou, sem NUIT, **Consumidor Final** com `contactName` e
  `contactPhone` guardados na encomenda; `warehouse`; linhas `sku` + `quantity`; `notes`.
- Preço e IVA vêm **sempre do sistema** (o pedido não envia preços).
- Mesmas regras das encomendas do desktop: stock disponível, reserva, limite de crédito, fluxo de
  aprovação. Estado inicial o mesmo que uma encomenda nova do desktop.
- **Idempotente**: repetir o mesmo `externalReference` com a mesma chave devolve a encomenda já
  criada (200), sem duplicar; com conteúdo diferente → `409`.
- A encomenda guarda a origem (`API` + nome da chave) e aparece no desktop como qualquer outra.

## 4. Webhooks (GOAL-014)

### 4.1 Subscrições

- Por empresa, criadas pelo **ADMIN**: URL, eventos escolhidos, activa/inactiva.
- URL tem de ser **HTTPS** em produção; endereços privados/locais (`localhost`, `10.*`, `192.168.*`,
  `172.16–31.*`, `127.*`, `169.254.*`) são recusados.
- Cada subscrição tem um **segredo** gerado pelo sistema, mostrado uma vez.

### 4.2 Eventos

| Evento | Quando |
|---|---|
| `invoice.issued` | factura, factura-recibo ou venda POS emitida |
| `credit_note.issued` | nota de crédito emitida |
| `order.status_changed` | encomenda muda de estado |
| `stock.availability_changed` | artigo passa a esgotado ou volta a ter stock disponível num armazém |

- Corpo JSON: `id` do evento (único), `type`, `occurredAt`, `companyId` e um resumo do recurso
  (números, totais, SKU), sem dados pessoais além do nome e NUIT do cliente.
- O envio acontece **depois do commit** da transacção que originou o evento; um erro no webhook
  nunca anula a operação.

### 4.3 Entrega

- `POST` com cabeçalhos `X-Multicore-Event`, `X-Multicore-Delivery` (id) e
  `X-Multicore-Signature: sha256=<HMAC-SHA256 do corpo com o segredo>`.
- Sucesso = resposta 2xx em 10 s. Falha → novas tentativas a **1 min, 5 min, 30 min, 2 h e 12 h**;
  depois fica `FAILED`.
- Registo de entregas por subscrição (evento, tentativa, código HTTP, duração), guardado 30 dias.
- Reenviar manualmente uma entrega `FAILED`.
- **20 falhas seguidas** desactivam a subscrição e avisam o ADMIN no sino.

### 4.4 Desktop

- Configurações › Integrações: chaves de API (criar com permissões, mostrar uma vez, revogar, último
  uso) e webhooks (URL, eventos, segredo mostrado uma vez, registo de entregas, reenviar).

## 5. Fora de âmbito

- OAuth, chaves por utilizador, escrita de artigos/preços/clientes pela API.
- Facturar, receber pagamentos ou movimentar stock pela API.
- Eventos de RH, contabilidade ou compras.
