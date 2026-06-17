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
