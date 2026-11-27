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
- Limite: **60 pedidos por minuto por chave** → `429` com `Retry-After`. Janela fixa de 1 minuto que abre no
  primeiro pedido da chave; os pedidos recusados não contam; `Retry-After` em segundos (≥ 1) até a janela
  fechar; corpo com `code` `RATE_LIMIT_EXCEEDED`. O limite é por chave: uma chave esgotada não trava outra.
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
- A permissão é verificada por rota (método + caminho) antes do recurso: chave válida sem a permissão
  da rota → `403` com `code` `API_PERMISSION_DENIED` e `message` PT-MZ que nomeia a permissão em falta.
  Ter `ORDERS_WRITE` não dá `ORDERS_READ` (cada permissão vale só pela sua rota).

#### 3.3.1 Catálogo e stock (API-07)

- **Artigo activo** = associado à empresa da chave (o cadastro não tem outro estado). O artigo de sistema
  *Adiantamento* nunca aparece (só se vende pelo módulo Adiantamentos).
- Campos de `GET /products` e `/products/{sku}`: `sku`, `name`, `category` (nome ou `null`), `barcode`,
  `unit` (unidade base), `vatRatePercent` (ex.: `16.00`), `priceWithVat` (preço de retalho + IVA, 2 casas,
  o mesmo cálculo dos documentos de venda) e `updatedAt` (ISO 8601 com fuso). Nada de preço de custo, preço de
  grosso, margens, fornecedores ou unidade de negócio.
- `updatedSince` (ISO 8601: data, data-hora ou data-hora com fuso): só artigos criados ou alterados a partir
  desse instante. Valor inválido → recusado.
- `/products/{sku}` de um SKU inexistente na empresa → `404` `PRODUCT_NOT_FOUND`.
- `GET /stock`: uma linha por artigo com controlo de stock × armazém de venda da empresa (activo, permite
  vendas, não é de trânsito), com `sku`, `warehouseCode` (número do armazém ou `null`), `warehouseName` e
  `available` = quantidade − reservas activas de encomendas nesse armazém, nunca negativa.
- `warehouse` filtra por número ou nome do armazém (sem distinguir maiúsculas); armazém de venda inexistente
  → `404` `WAREHOUSE_NOT_FOUND`.
- Listas paginadas: `page` começa em 0, `size` por omissão 50 e no máximo 100 (acima disso usa-se 100);
  resposta `{items, page, size, totalItems, totalPages}`, ordenada por SKU (e armazém no stock).

### 3.4 Criar encomenda

- Corpo: `externalReference` (obrigatório, único por chave), cliente por `nuit` (existente ou criado
  com as regras do registo manual) ou, sem NUIT, **Consumidor Final** com `contactName` e
  `contactPhone` guardados na encomenda; `customerName`; `warehouse`; linhas `sku` + `quantity`; `notes`.
- **Cliente novo por NUIT (decisão do responsável, 2026-11-27):** `customerName` é opcional, mas
  obrigatório quando o NUIT ainda não existe na empresa; sem ele → `400 CUSTOMER_NAME_REQUIRED`
  com mensagem PT-MZ. O `contactName` nunca é usado como nome do cliente.
- Preço e IVA vêm **sempre do sistema** (o pedido não envia preços).
- **Via da encomenda (decisão do responsável, 2026-11-27):** a encomenda nasce como `FORMAL_ORDER`
  (Encomenda A4) em `PENDING_APPROVAL` e passa pelo motor de aprovações como no desktop, **mais**
  verificação de stock disponível e reserva activa logo na criação (respeitando as reservas de outros
  pedidos). Sem stock disponível → recusada com mensagem PT-MZ. A reserva liberta-se ao facturar ou
  cancelar, como hoje; o limite de crédito verifica-se na facturação, como no desktop. Esta combinação
  só é usada pela API pública e pela loja online.
- **Idempotente**: repetir o mesmo `externalReference` com a mesma chave devolve a encomenda já
  criada (200), sem duplicar; com conteúdo diferente → `409`.
- A encomenda guarda a origem (`API` + nome da chave) e aparece no desktop como qualquer outra.

#### 3.4.1 Detalhe de `POST /orders` (API-08)

- Campos: `externalReference` (obrigatório, até 80 caracteres), `warehouse` (obrigatório: número ou nome de um
  armazém de venda, como no `GET /stock`), `lines` (pelo menos uma; `sku` e `quantity` > 0), `nuit`,
  `customerName`, `contactName`, `contactPhone`, `notes` (até 1000).
- Sem `nuit`: `contactName` e `contactPhone` são obrigatórios (Consumidor Final precisa de contacto) →
  `400 CONTACT_REQUIRED`. Com `nuit` válido (9 dígitos): cliente da empresa com esse NUIT; se não existir,
  é registado com `customerName` pelas regras do registo manual (NUIT inválido → `400`).
- A encomenda é criada em nome da chave (utilizador `API · <nome da chave>`, perfil de vendedor) e na empresa
  dela; o motor de aprovações trata-a como uma encomenda de vendedor.
- Respostas: `201` com a encomenda (`number`, `externalReference`, `status`, linhas com preço do sistema e
  totais); SKU inexistente → `404 PRODUCT_NOT_FOUND`; armazém → `404 WAREHOUSE_NOT_FOUND`; stock disponível
  insuficiente → `422 INSUFFICIENT_STOCK`; outra regra de negócio → `422 BUSINESS_RULE_VIOLATION`; corpo
  inválido → `400 VALIDATION_ERROR`. Mensagens em PT-MZ.
- Uma encomenda com reserva activa rejeitada no motor de aprovações fica cancelada e **liberta a reserva**.

#### 3.4.2 Idempotência de `POST /orders` (API-09)

- A unicidade é por **chave + `externalReference`** (sem espaços nas pontas): duas chaves da mesma empresa
  podem usar a mesma referência sem colidir.
- O "conteúdo" é a impressão digital SHA-256 do corpo normalizado: todos os campos do corpo, sem espaços nas
  pontas (vazio = ausente), quantidades sem zeros à direita (`3` = `3.000`), linhas pela ordem enviada. É
  guardada na encomenda (`request_fingerprint`).
- Repetição com o mesmo conteúdo → `200` com a encomenda já criada **no estado actual**, sem nova encomenda,
  sem nova reserva e sem registar cliente.
- Mesma referência com conteúdo diferente → `409 EXTERNAL_REFERENCE_CONFLICT` com mensagem PT-MZ que indica a
  referência e o número da encomenda existente; nada é gravado.
- A verificação acontece antes de qualquer outra regra do pedido (artigos, armazém, cliente, stock).

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
