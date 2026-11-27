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

#### 3.1.1 Gestão das chaves (API-02)

- Rotas **internas** (login de utilizador + `X-Company-Id`, nunca `X-Api-Key`), sempre na empresa activa:
  `GET /api/integrations/api-keys` (lista sem segredo nem hash), `POST /api/integrations/api-keys`
  (`{name, permissions}` → `201` com a chave e o segredo completo, a única vez que sai) e
  `POST /api/integrations/api-keys/{id}/revoke` (→ `200` com a chave revogada).
- Só o **ADMIN** lista, cria e revoga: qualquer outro perfil → `403` (política HTTP e guarda do Service).
- Chave de outra empresa ou já revogada, nome vazio ou sem permissões → `400` com mensagem PT-MZ.
- A auditoria regista o utilizador, o nome e a identificação `mk_live_XXXXXXXX...` da chave, **nunca** o segredo.

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

#### 3.2.1 Erros e paginação (API-06)

- **Todo** o erro em `/api/public/**` sai em JSON (`application/json`) com só dois campos: `code` e `message`
  (PT-MZ). Nunca stack trace, nome de classe Java, SQL ou outro detalhe interno; esse detalhe fica no log.
- Códigos comuns a todas as rotas (além dos próprios de cada recurso, §3.2–§3.4.3):
  - `400 VALIDATION_ERROR` — corpo que não é JSON, campo inválido, parâmetro em falta ou com tipo errado
    (ex.: `page=abc`), data de `updatedSince` que não é ISO 8601; a `message` diz o que está mal.
  - `404 NOT_FOUND` — rota que não existe na API pública (com chave válida; sem chave é o `401` da §3.2).
  - `405 METHOD_NOT_ALLOWED` — método que a rota não aceita, com o cabeçalho `Allow`.
  - `409 CONFLICT` — o pedido colidiu com outro gravado em simultâneo (ex.: dois `POST /orders` iguais ao
    mesmo tempo); nada fica duplicado e o cliente pode repetir.
  - `500 INTERNAL_ERROR` — avaria inesperada, com mensagem genérica.
- Paginação: `page` começa em 0 (negativa conta como 0); `size` por omissão 50, no máximo 100 (acima usa-se
  100; menor que 1 usa-se o por omissão). A resposta diz sempre o `page` e o `size` efectivos.

#### 3.2.2 Documentação OpenAPI (API-11)

- `GET /api/public/v1/docs` devolve o documento **OpenAPI 3** em JSON (`application/json`) do grupo público.
- Só descreve rotas de `/api/public/v1/**` (produtos, stock, encomendas). Nenhuma rota interna (`/api/**` do
  desktop, `/api/integrations/**`, autenticação, relatórios) nem o próprio `/docs` aparece.
- Declara o esquema de segurança `ApiKeyAuth` (`apiKey`, cabeçalho `X-Api-Key`) aplicado a todas as rotas, e o
  corpo de erro `PublicApiError` (`code`, `message`).
- Como o resto de `/api/public/**` (§3.2), exige uma chave **válida e não revogada**, de qualquer permissão;
  sem chave → `401 API_KEY_REQUIRED`, chave inválida/revogada → `401 API_KEY_INVALID`; conta para o limite
  de 60/min.
- Existe também em produção, independentemente de `springdoc.api-docs.enabled`/`springdoc.swagger-ui.enabled`,
  que continuam a controlar só a documentação **interna** (`/v3/api-docs`, desligada em prod).
- Sem Swagger UI para o grupo público nesta versão (o cliente importa o JSON na sua ferramenta).

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

#### 3.4.3 Consulta de `GET /orders/{number}` (API-10)

- Mostra qualquer **encomenda de cliente** da empresa da chave (Encomenda A4 ou Pedido de separação), venha ela
  da API ou do desktop: a permissão `ORDERS_READ` existe para chaves que só lêem. As reposições internas não são
  encomendas de cliente e não aparecem.
- Procura pelo número exacto da encomenda (sem espaços nas pontas), que vai tal e qual no caminho, com a barra
  (ex.: `GET /api/public/v1/orders/ENC-2026/00001`). Número inexistente, de outra empresa ou de
  uma reposição interna → `404 ORDER_NOT_FOUND` com mensagem PT-MZ (não se revela se existe noutra empresa).
- Corpo: o mesmo de `POST /orders` (`number`, `externalReference` — `null` nas do desktop —, `status`,
  `statusLabel`, cliente, armazém, linhas com preço do sistema e IVA em %, totais, `createdAt`) **mais**
  `invoiceNumber`: o número da factura associada, ou `null` enquanto não estiver facturada. O estado é o actual.
- Nada de ids internos, custos, margens ou reservas.

## 4. Webhooks (GOAL-014)

### 4.1 Subscrições

- Por empresa, criadas pelo **ADMIN**: URL, eventos escolhidos, activa/inactiva.
- URL tem de ser **HTTPS** em produção; endereços privados/locais (`localhost`, `10.*`, `192.168.*`,
  `172.16–31.*`, `127.*`, `169.254.*`) são recusados.
- Cada subscrição tem um **segredo** gerado pelo sistema, mostrado uma vez.

#### 4.1.1 Regras da subscrição (WH-01)

- **Quem**: só o **ADMIN** cria, lista, activa e desactiva subscrições, sempre na **empresa activa**; qualquer
  outro perfil → `BusinessRuleException` "Só o administrador gere os webhooks." (a política HTTP `403` é do WH-08).
- **Campos ao criar**: `url` (obrigatória, ≤ 500 caracteres) e `events` (pelo menos um dos quatro de §4.2,
  sem repetidos). Nasce **activa**. A URL e os eventos não se editam: para mudar, cria-se outra e desactiva-se
  a antiga. Activar/desactivar é a única alteração.
- **URL válida**: absoluta, com host, sem utilizador/senha (`https://u:p@...` recusada). Esquema **`https`**
  obrigatório com o perfil `prod`; fora de `prod` aceita também `http` (desenvolvimento). Outro esquema → recusa.
- **Endereços privados/locais recusados** (anti-SSRF): `localhost` e `*.localhost`; IPv4 `0.*`, `10.*`, `127.*`,
  `169.254.*`, `172.16–31.*`, `192.168.*`; IPv6 `::`, `::1`, `fc00::/7`, `fe80::/10` (e IPv4 mapeado nestes
  intervalos). Um nome de host é resolvido (DNS) e **todos** os endereços devolvidos são verificados; nome que
  não resolve → recusa. A resolução passa por uma porta injectável (testes sem rede). A mesma verificação
  repete-se antes de cada entrega (WH-04/05), para um DNS que mude depois não abrir a rede interna.
- **Segredo**: `whsec_` + 32 bytes aleatórios em base62 (mesmo gerador das chaves de API, §3.1). Sai **só** na
  resposta da criação; listas e detalhes nunca o devolvem. Como a assinatura HMAC (§4.3) precisa dele em claro,
  fica guardado **cifrado em repouso** com a cifra AES-256-GCM existente (`AttachmentCrypto`, prefixo `MCE1`;
  sem chave configurada fora de prod fica em claro, como os anexos).
- **Auditoria**: `WEBHOOK_CREATE`, `WEBHOOK_ACTIVATE`, `WEBHOOK_DEACTIVATE` com o utilizador e a URL, **nunca**
  o segredo. Subscrição de outra empresa → "Subscrição não encontrada."
- **Mensagens PT-MZ** (o backend devolve-as em `400`):
  - "Indique o URL do webhook." · "O URL do webhook é inválido." · "O URL do webhook tem de usar https://."
  - "Não são permitidos endereços privados ou locais no webhook." · "Não foi possível resolver o endereço do webhook."
  - "Escolha pelo menos um evento." · "Evento de webhook desconhecido: X."
- **Persistência**: tabela `webhook_subscriptions` (empresa, URL, eventos, segredo cifrado, activa, criada em/por,
  contador de falhas seguidas para o WH-07), migração Flyway com o próximo número livre, sintaxe PostgreSQL.

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

#### 4.2.1 Corpo dos eventos (WH-02)

- **Origem**: os webhooks escutam os eventos de domínio que já existem (nunca o Repository de outro módulo):
  `invoice.issued` ← `SaleRegisteredEvent` (factura, factura-recibo e venda POS, já publicados por
  `ComercialInvoiceLifecycleService` e `PosCheckoutAndPaymentService`); `credit_note.issued` ←
  `CreditNoteApprovedEvent` (nota de crédito "emitida" = aprovada); `order.status_changed` e
  `stock.availability_changed` ← eventos novos `OrderStatusChangedEvent` e `StockAvailabilityChangedEvent`
  (pacote `architecture.events`, só números/textos, nunca entidades). O resumo que falta no evento de domínio
  (cliente, NUIT, SKU) é lido pelo Service público do módulo dono.
- **Quem recebe**: só subscrições **activas** da **mesma empresa** do evento que escolheram aquele tipo. Sem
  subscrição interessada não se constrói corpo nenhum. Um evento gera **um** corpo (mesmo `id`) por subscrição.
- **Envelope** (JSON, chaves em camelCase, datas ISO 8601 com fuso, valores monetários com 2 casas em número):
  `{"id": "evt_<32 base62>", "type": "<código §4.2>", "occurredAt": "...", "companyId": 1, "data": {...}}`.
  `id` único por ocorrência (gerador `SecretTokens`); `occurredAt` = instante do facto.
- **`data` por evento**:
  - `invoice.issued`: `invoiceNumber`, `documentDate` (data), `channel` (`MANUAL` | `POS`), `customer`,
    `totalBeforeTax`, `taxAmount`, `totalAmount`, `amountPaid` (o que entrou no acto).
  - `credit_note.issued`: `creditNoteNumber`, `invoiceNumber` (factura de origem ou `null`), `customer`,
    `totalBeforeTax`, `taxAmount`, `totalAmount`.
  - `order.status_changed`: `orderNumber`, `previousStatus`, `status` (nomes de `OrderStatus`, ex. `BILLED`),
    `customer`, `totalAmount`, `invoiceNumber` (quando já facturada, senão `null`). Só se publica quando o estado
    **muda** de facto (mesmo estado → nada); a criação da encomenda não é mudança de estado.
  - `stock.availability_changed`: `sku`, `productName`, `warehouseCode`, `warehouseName`, `available`
    (o mesmo cálculo do `GET /stock`, §3.3.1) e `status` (`OUT_OF_STOCK` quando `available` passa de > 0 a 0;
    `BACK_IN_STOCK` quando passa de 0 a > 0). Só armazéns de venda (§3.3.1) e artigos com controlo de stock;
    variações que não cruzam o zero não geram evento; várias variações do mesmo artigo × armazém na mesma
    transacção comparam só o primeiro antes com o último depois.
  - `customer` = `{"name": ..., "nuit": ...}` (NUIT `null` quando não há; Consumidor Final com o nome guardado
    no documento). Nenhum outro dado pessoal (telefone, email, morada) nem custos ou margens.
- **Contrato**: o corpo é construído por `WebhookEventFactory` (módulo webhooks) a partir de um record de
  domínio, testável sem HTTP; WH-02 cobre a construção e a escolha das subscrições, o envio é do WH-03/04.

#### 4.2.2 Envio depois do commit (WH-03)

- **Quando**: os webhooks escutam os quatro eventos de domínio com `@TransactionalEventListener(phase =
  AFTER_COMMIT)`. Transacção que faz **rollback** → nenhuma entrega registada nem enviada. Evento publicado fora
  de transacção (`fallbackExecution = true`) conta como já confirmado.
- **Registo primeiro, envio depois**: depois do commit, numa transacção **nova** (`REQUIRES_NEW`, porque a
  original já fechou), `WebhookEventService.prepare(...)` escolhe as subscrições (§4.2.1) e grava **uma entrega
  por subscrição** em `webhook_deliveries` com estado `PENDING`, `attempts = 0`, `next_attempt_at = agora` e o
  corpo JSON já serializado (o corpo não volta a ser construído: reenvios e tentativas mandam exactamente o mesmo
  texto, mesmo `id` de evento). Sem subscrição interessada → nenhuma linha.
- **Envio fora da thread do negócio**: gravadas as entregas, cada uma é entregue a uma porta
  `WebhookDispatcher` que corre num executor próprio (assíncrono). A operação do utilizador (factura, POS,
  aprovação, guia) nunca espera pelo HTTP. O POST, a assinatura e os cabeçalhos são do WH-04; as tentativas e o
  `FAILED` do WH-05. Até lá a entrega fica `PENDING`.
- **Um erro no webhook nunca anula a operação**: qualquer excepção no caminho do webhook (leitura do resumo,
  serialização, gravação da entrega, despacho) é apanhada no listener e registada em log `WARN` com o tipo do
  evento e a empresa (nunca o segredo nem o corpo); a operação de negócio já está confirmada e não é afectada, e
  os outros eventos/subscrições seguem. Uma subscrição que falha a gravação não impede as outras.
- **Janela aceite**: se o processo morrer entre o commit e a gravação da entrega, esse evento perde-se (não há
  outbox na transacção de negócio, para um erro do webhook nunca a poder anular). Fica documentado; não se
  reconstrói depois.
- **Persistência**: tabela `webhook_deliveries` (empresa, subscrição, `event_id`, `event_type`, corpo,
  estado `PENDING` | `DELIVERED` | `FAILED`, tentativas, próxima tentativa, último código HTTP, última duração
  em ms, último erro curto, criada em, entregue em), índices por (estado, próxima tentativa) e por (subscrição,
  criada em); migração Flyway com o próximo número livre, sintaxe PostgreSQL. A limpeza aos 30 dias é do WH-06.
- **Contrato de teste** (`WebhookAfterCommitJpaTest`, com Hibernate e commits reais; `WebhookEventTest` continua a
  provar os corpos): commit com subscrição interessada → uma entrega `PENDING` por
  subscrição com o mesmo `event_id` e despacho chamado depois do commit; rollback → zero entregas e nenhum
  despacho; falha forçada no caminho do webhook → a operação fica confirmada e não sai excepção para quem
  chamou; sem interessados → zero entregas.

### 4.3 Entrega

- `POST` com cabeçalhos `X-Multicore-Event`, `X-Multicore-Delivery` (id) e
  `X-Multicore-Signature: sha256=<HMAC-SHA256 do corpo com o segredo>`.
- Sucesso = resposta 2xx em 10 s. Falha → novas tentativas a **1 min, 5 min, 30 min, 2 h e 12 h**;
  depois fica `FAILED`.
- Registo de entregas por subscrição (evento, tentativa, código HTTP, duração), guardado 30 dias.
- Reenviar manualmente uma entrega `FAILED`.
- **20 falhas seguidas** desactivam a subscrição e avisam o ADMIN no sino.

#### 4.3.1 Envio, assinatura e cabeçalhos (WH-04)

- **Quem envia**: o `AsyncWebhookDispatcher` (executor próprio, §4.2.2) chama `WebhookSender.send(deliveryId)`, que
  lê a entrega e a subscrição em transacção própria, faz **uma** tentativa e grava o resultado. O HTTP passa por uma
  porta injectável `WebhookHttpClient` (testes sem rede; a implementação real usa `java.net.http.HttpClient`).
- **Pedido**: `POST` para o URL da subscrição com o corpo **exactamente** como está em `webhook_deliveries.body`
  (bytes UTF-8, nunca reconstruído nem reformatado). Cabeçalhos:
  - `Content-Type: application/json; charset=utf-8`
  - `User-Agent: Multicore-Webhooks/1`
  - `X-Multicore-Event`: o código do evento (`invoice.issued`, ...), igual ao `type` do corpo.
  - `X-Multicore-Delivery`: o `id` da entrega (`webhook_deliveries.id`, em texto); o mesmo em todas as tentativas e
    reenvios da mesma entrega, para o receptor deduplicar (o `id` do evento no corpo também não muda).
  - `X-Multicore-Signature: sha256=<hex>` — HMAC-SHA256 dos bytes do corpo com a chave = o segredo completo da
    subscrição (`whsec_...`, decifrado com `AttachmentCrypto`, em bytes UTF-8); `<hex>` em minúsculas, 64 caracteres.
    O segredo em claro só existe em memória durante o cálculo; nunca vai para log, `last_error` nem cabeçalho.
- **Rede**: timeout total de **10 s** (ligação + resposta); redirecções **não** são seguidas (3xx = falha). Antes de
  ligar, o URL volta a passar pelo `WebhookUrlValidator` (§4.1.1, DNS outra vez): endereço privado/local ou nome que
  não resolve → falha sem abrir ligação. O corpo da resposta é ignorado (não se guarda).
- **Resultado gravado** (uma tentativa):
  - **2xx** → `DELIVERED`, `delivered_at = agora`, `attempts + 1`, `last_http_status`, `last_duration_ms`,
    `last_error = null`, `next_attempt_at = null`.
  - **Falha** (não-2xx, timeout, erro de rede, endereço recusado) → `attempts + 1`, `last_http_status` (ou `null` sem
    resposta), `last_duration_ms`, `last_error` curto em PT-MZ (≤ 500: "Resposta HTTP 500.", "Tempo de resposta
    excedido (10 s).", "Erro de rede: <classe>.", "Endereço do webhook recusado."); a entrega **continua `PENDING`**.
    O agendamento das próximas tentativas e o `FAILED` são do WH-05; o contador de falhas seguidas, do WH-07.
- **Não envia**: entrega que já não está `PENDING` (ex. `DELIVERED`) ou subscrição **inactiva** → nenhum pedido HTTP e
  nada muda na entrega. Um erro no envio nunca sai do executor (apanhado e registado em `WARN`, sem segredo nem corpo).
- **Contrato de teste** (`WebhookDeliveryTest`, cliente HTTP falso): corpo enviado = corpo gravado; cabeçalhos com o
  tipo, o id da entrega e a assinatura igual ao HMAC-SHA256 calculado à parte (vector conhecido); 2xx → `DELIVERED`
  com código e duração; 500/timeout/erro de rede → `PENDING` com `attempts = 1` e `last_error`; URL que passou a
  resolver para endereço privado → nenhum pedido e falha registada; subscrição inactiva ou entrega já entregue →
  nenhum pedido.

#### 4.3.2 Tentativas e `FAILED` (WH-05)

- **Calendário**: a primeira tentativa é a do despacho logo a seguir ao commit (§4.2.2). Cada falha (§4.3.1) agenda a
  seguinte a contar do **fim da tentativa que falhou**: depois da 1.ª falha +**1 min**, da 2.ª +**5 min**, da 3.ª
  +**30 min**, da 4.ª +**2 h**, da 5.ª +**12 h**. A **6.ª falha** (a inicial + 5 novas tentativas) passa a entrega a
  `FAILED` com `next_attempt_at = null`; o `last_error`, o código e a duração da última tentativa ficam. O calendário
  vive num só sítio (`WebhookRetryPolicy`, constante, sem configuração) e usa um `java.time.Clock` injectável.
- **Quem repete**: `WebhookRetryScheduler` (`@Scheduled(fixedDelay)`, intervalo `webhooks.retry.poll-ms`, por omissão
  30 000; desligável com `webhooks.retry.enabled=false`) procura entregas `PENDING` com `next_attempt_at <= agora` de
  subscrições **activas**, por `next_attempt_at` crescente, no máximo **50** por volta, e entrega cada id ao
  `WebhookDispatcher` (o mesmo envio do WH-04). Também apanha entregas que ficaram `PENDING` depois de um reinício (o
  despacho assíncrono perdeu-se): a primeira tentativa não se perde, só se atrasa.
- **Sem envios em duplicado**: antes de ligar, a tentativa é **reclamada** numa actualização condicional
  (`PENDING` e `next_attempt_at <= agora` → `next_attempt_at = agora + 2 min`, um prazo maior que o timeout de 10 s);
  se outra thread/instância já a reclamou, não há pedido. O resultado da tentativa substitui esse prazo (`DELIVERED` →
  `null`; falha → próximo do calendário ou `FAILED`). Se o processo morrer a meio, o prazo expira e a tentativa repete
  (o receptor deduplica por `X-Multicore-Delivery`).
- **Subscrição inactiva**: as entregas `PENDING` dela não são enviadas nem contadas como falha; ficam à espera e seguem
  pelo calendário quando a subscrição volta a ficar activa. Desactivar não as passa a `FAILED`.
- **Timeout** continua o do §4.3.1 (10 s por tentativa); timeout conta como falha igual às outras.
- `FAILED` é final para o agendador; só o reenvio manual (WH-06) a volta a pôr `PENDING`. O contador de falhas
  seguidas da subscrição é do WH-07.
- **Contrato de teste** (`WebhookDeliveryTest`, relógio fixo e cliente HTTP falso): falhas seguidas agendam
  `next_attempt_at` a +1 min, +5 min, +30 min, +2 h e +12 h do fim de cada tentativa; a 6.ª falha → `FAILED`,
  `attempts = 6`, `next_attempt_at = null`; sucesso numa repetição → `DELIVERED` sem mais tentativas; o agendador só
  despacha `PENDING` vencidas de subscrições activas (não as futuras, `DELIVERED`, `FAILED` nem de inactivas) e no
  máximo 50; uma tentativa já reclamada não volta a ser enviada dentro do prazo e volta a sê-lo depois de ele expirar.

### 4.4 Desktop

- Configurações › Integrações: chaves de API (criar com permissões, mostrar uma vez, revogar, último
  uso) e webhooks (URL, eventos, segredo mostrado uma vez, registo de entregas, reenviar).

#### 4.4.1 Ecrã de chaves de API (API-UI-01)

- Configurações › separador **"Integrações"** (último, ícone `fas-plug`), com a secção "Chaves de API"; os
  webhooks (WH-UI-01, GOAL-014) entram depois no mesmo separador. Tudo pela API interna de §3.1.1
  (`/api/integrations/api-keys`), sempre na empresa activa; o desktop nunca gera nem guarda segredos.
- Tabela: nome, identificação `mk_live_XXXXXXXX...`, permissões (rótulos PT-MZ: "Ler catálogo",
  "Ler encomendas", "Criar encomendas"), criada em / por, último uso ("Nunca usada" quando vazio) e estado
  ("Activa" ou "Revogada em dd/MM/yyyy HH:mm por X"). Datas em `dd/MM/yyyy HH:mm`. Chaves revogadas continuam
  visíveis (histórico). Botão "Actualizar" relê a lista.
- **"Nova chave"**: diálogo com nome (obrigatório) e uma caixa por permissão (pelo menos uma); validação
  local com as mesmas mensagens do backend, mas o backend decide (`400` mostrado tal como vem).
- **Mostrar uma vez**: depois do `201`, diálogo modal com o segredo completo num campo só de leitura,
  botão "Copiar" (área de transferência) e o aviso "Esta chave não volta a ser mostrada. Guarde-a agora.";
  ao fechar, o segredo é descartado da memória do ecrã e a tabela recarrega sem ele.
- **"Revogar"**: só com uma chave **activa** seleccionada; pede confirmação ("A chave X deixa de funcionar
  imediatamente. Revogar?") e recarrega a tabela.
- Só o **ADMIN** gere chaves: para outro perfil, "Nova chave" e "Revogar" ficam desactivados e a lista não é
  pedida (mostra "Só o administrador gere as chaves de API."); o backend continua a ser quem decide (`403`).
- UI uniforme: tokens do tema e `UIHelper` (ícones `UIHelper.icon`, nunca emojis), como os outros
  separadores de Configurações.

## 5. Fora de âmbito

- OAuth, chaves por utilizador, escrita de artigos/preços/clientes pela API.
- Facturar, receber pagamentos ou movimentar stock pela API.
- Eventos de RH, contabilidade ou compras.
