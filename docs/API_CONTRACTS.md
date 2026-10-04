# Contratos API - Multicore ERP

Este documento define o padrao para endpoints REST. A API deve ser estavel para permitir que o desktop migre de chamadas directas a Services para HTTPS.

## URL e versionamento

Padrao actual:

```text
/api/<modulo>/<recurso>
```

Exemplos:

```text
/api/comercial/products
/api/inventory/transfers
/api/pos/checkout
/api/hr/employees
```

Transferências de stock com rascunho editável:

```text
POST /api/inventory/transfers
PUT  /api/inventory/transfers/{id}
POST /api/inventory/transfers/{id}/submit
POST /api/inventory/transfers/{id}/approve
POST /api/inventory/transfers/{id}/reject
POST /api/inventory/transfers/{id}/cancel
```

`PUT` recebe `UpdateStockTransferRequest` com `version`; apenas `DRAFT` é editável. Submeter
bloqueia o conteúdo e apenas aprovar uma guia `PENDING_APPROVAL` movimenta stock.

Se houver quebra futura de contrato publico, introduzir versionamento explicito:

```text
/api/v2/<modulo>/<recurso>
```

## Controllers

Controllers fazem apenas:

- Receber request.
- Aplicar `@Valid`.
- Delegar para Service.
- Devolver DTO ou status HTTP.

Controllers nao fazem:

- Regra de negocio.
- Query.
- Calculo fiscal.
- Transaccao.
- Conversao complexa.
- Acesso a Repository.

## DTOs

- DTOs vivem em `modules/<dominio>/dto`.
- Usar `record`.
- Input: `CreateXxxRequest`, `UpdateXxxRequest`, `SaveXxxRequest` ou nome especifico do caso de uso.
- Output: `XxxDTO`.
- DTO de input usa Bean Validation.
- DTO de output nao expoe campos sensiveis, hashes, entidades ou detalhes internos de auditoria.

Exemplo:

```java
public record CreateProductRequest(
    @NotBlank @Size(max = 32) String sku,
    @NotBlank @Size(max = 200) String name,
    @NotNull @Positive BigDecimal unitPrice
) {}
```

## Status HTTP

| Caso | Status |
|------|--------|
| Criacao com sucesso | `201 Created` |
| Leitura/listagem | `200 OK` |
| Actualizacao | `200 OK` ou `204 No Content` |
| Remocao logica/anulacao | `200 OK` ou `204 No Content` |
| Validacao sintactica | `400 Bad Request` |
| Regra de negocio | `400 Bad Request` ou status definido pelo handler |
| Sem autenticacao | `401 Unauthorized` |
| Sem permissao | `403 Forbidden` |
| Nao encontrado | `404 Not Found` quando houver handler proprio |

## Autorizacao e sessoes

- Credenciais invalidas no login devolvem uma mensagem uniforme, independentemente de o nome
  de utilizador existir, estar inactivo ou ter senha errada.
- Os pedidos de tenant revalidam utilizador, associacao, empresa activa e assinatura.
- Repor uma senha revoga todas as sessoes da conta.
- `/api/monitoring/**` requer `ADMIN` da empresa indicada em `X-Company-Id` ou `SUPERADMIN`
  sem empresa. `ADMIN` recebe apenas dados da propria empresa; incidentes globais e teste de
  email exigem `SUPERADMIN`.

Detalhes e casos de regressao: [SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md](SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md).

## Erros

Erros devem ser uniformes e accionaveis:

```json
{
  "timestamp": "2026-06-01T17:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Quantidade da linha deve ser positiva.",
  "path": "/api/purchases"
}
```

Mensagens:

- Em portugues de Mocambique.
- Sem stack trace.
- Sem nomes tecnicos como `NullPointerException`.
- Sem expor SQL, token, password ou path interno.

## Paginacao e filtros

Para listagens grandes, preferir:

```text
GET /api/<modulo>/<recurso>?page=0&size=50&sort=createdAt,desc
```

Filtros devem ter nomes estaveis e em ingles:

```text
?status=OPEN&fromDate=2026-01-01&toDate=2026-01-31
```

## Compatibilidade com desktop

Como o Swing vai migrar para clients HTTP:

- Nao devolver HTML.
- Nao depender de sessao de servidor para dados de negocio.
- Contratos devem ser previsiveis para `RestClient` ou `WebClient`.
- Mudancas de DTO devem preservar compatibilidade quando possivel.
- Operacoes atomicas devem existir como endpoints de caso de uso, nao como sequencia fragil de chamadas UI.
# Catálogo POS paginado

- `GET /api/comercial/products/pos-catalog/page?query=&availableOnly=false&page=0&size=36`
  devolve `PageResponse<POSCatalogItemDTO>`.
- `GET /api/comercial/products/pos-catalog/by-barcode?barcode=...` devolve um item com produto e
  disponibilidade, permitindo ao scanner operar fora da página visível.

# Editor de encomendas

- `PUT /api/comercial/orders/{id}` actualiza cliente, armazém, destino e linhas de uma encomenda
  ainda editável. Recebe `UpdateOrderRequest` com a versão optimista e devolve `OrderDTO`.
- `OrderDTO` inclui `warehouseId`, `warehouseName` e `version`, preservando os construtores
  retrocompatíveis usados pelos clientes anteriores.
- Número e tipo da encomenda não são editáveis. Estados fechados são recusados pelo Service.
- Ver `docs/ORDER_EDITOR_SPEC.md`.

# Editores de documentos pré-emissão

- `PUT /api/comercial/quotations/{id}` actualiza integralmente uma cotação em `DRAFT`.
  Recebe `UpdateQuotationRequest`, incluindo `version`, e devolve `QuotationDTO` com a nova versão.
- `PUT /api/purchases/orders/{id}` actualiza integralmente uma encomenda a fornecedor em
  `ORDERED`, desde que nenhuma linha tenha quantidade recebida. Recebe
  `UpdatePurchaseOrderRequest`, incluindo `version`, e devolve `PurchaseOrderDTO`.
- Os dois endpoints recalculam impostos e totais no Service, validam o tenant e recusam versões
  desactualizadas. Número, data de emissão e estado nunca são alterados pelo cliente.
- Ver `docs/EDITABLE_DOCUMENT_EDITORS_SPEC.md`.

# Saúde ocupacional

- `GET /api/hr/occupational-health/employee/{id}/summary` — resumo não clínico do último exame.
  Restrito a gestor/admin **e ao próprio trabalhador**: a aptidão é dado de saúde.
- `GET /api/hr/occupational-health/employee/{id}` — histórico clínico, restrito a gestor/admin.
  Cada consulta grava `OCCUPATIONAL_HEALTH_ACCESS` na auditoria.
- `GET /api/hr/occupational-health/expiring` — últimos exames a renovar em até 60 dias ou vencidos.
- `GET /api/hr/occupational-health/missing` — trabalhadores no activo sem exame de aptidão nenhum.
- `GET /api/hr/occupational-health/providers` — prestadores activos do cadastro de fornecedores.
- `GET /api/hr/occupational-health/costs?from=&to=` — custo dos exames no intervalo, por prestador.
- `POST /api/hr/occupational-health` — regista exame/renovação sem alterar o histórico anterior.
  Recusa texto com estado serológico e recusa número de factura sem valor.
- `POST /api/hr/occupational-health/{id}/pay` — paga o exame à clínica por saída de tesouraria.
  Encargo do empregador; pagar duas vezes é recusado. Ver
  [CONFORMIDADE_LEGAL_MZ_SPEC.md](CONFORMIDADE_LEGAL_MZ_SPEC.md).
