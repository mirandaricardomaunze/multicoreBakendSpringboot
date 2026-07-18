# Spec — Vista Unificada de Movimentos Comerciais

> Fonte de verdade do ecrã/endpoint que lista **todos os documentos comerciais** de uma
> empresa num só sítio, filtráveis por cliente/nº e período. Fecha a dívida técnica §7.3 de
> [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md).

**Última actualização:** 2026-06-25
**Estado:** implementado e coberto por testes.

---

## 1. Problema

Hoje cada tipo de documento (Fatura, Encomenda, Nota de Crédito, Nota de Débito) só se
consulta no seu próprio painel/tab. Não existe **uma visão única** que responda a perguntas
operacionais comuns numa loja/mercearia:

- «Que documentos é que este cliente tem este mês?»
- «O que se passou comercialmente entre o dia X e o dia Y?»

## 2. Âmbito

**Leitura agregada, read-only.** Não cria, edita nem anula documentos — só lista. As acções
sobre cada documento continuam nos seus painéis dedicados.

Documentos abrangidos (os quatro do mapa canónico §2):

| Tipo (enum)    | Origem                | Nº            | Data              | Cliente                              |
|----------------|-----------------------|---------------|-------------------|--------------------------------------|
| `FATURA`       | `Invoice`             | `invoiceNumber` | `createdAt`     | `client.name` ou `customerName`      |
| `ENCOMENDA`    | `Order`               | `orderNumber`   | `createdAt`     | `client.name` ou `walkInName`        |
| `NOTA_CREDITO` | `CreditNote`          | `noteNumber`    | `issueDate`     | `client.name` (ou `—`)               |
| `NOTA_DEBITO`  | `DebitNote`           | `noteNumber`    | `issueDate`     | `client.name` (ou `—`)               |

## 3. Contrato

### 3.1 Service

`MovimentosService.listar(companyId, query, from, to) → List<MovimentoDTO>`

- **Guarda de empresa:** `CurrentUserContext.requireCompany(companyId)` (mesma disciplina de
  `ReportService`). Isolamento multi-tenant obrigatório.
- **`query`** (nullable/blank = sem filtro): substring **case-insensitive** sobre **nº OU
  nome do cliente**.
- **`from` / `to`** (`LocalDate`, nullable): filtram pela **data** do movimento, **inclusivo
  nas duas pontas** (`from ≤ data ≤ to`). Qualquer um pode ser nulo (intervalo aberto).
- **Ordenação:** por **data descendente** (mais recente primeiro). Movimentos sem data vão
  para o fim.
- **Saída:** `MovimentoDTO` — record na fronteira, **nunca** `@Entity`.

```java
record MovimentoDTO(
    MovimentoTipo tipo,   // enum com label PT
    Long documentId,
    String numero,
    String cliente,
    LocalDateTime data,
    String estado,        // estado bruto do documento (ex.: PAID, PENDING, APPROVED)
    BigDecimal total      // totalAmount do documento
)
```

### 3.2 Controller

`GET /api/movimentos?companyId={id}&query={q}&from={yyyy-MM-dd}&to={yyyy-MM-dd}`
→ `200 List<MovimentoDTO>`. `query/from/to` opcionais. Protegido pelo `SecurityInterceptor`
(401 sem token, 403 empresa sem acesso) como todo o `/api/**`.

### 3.3 UI (desktop)

Nova tab **«Movimentos»** no `ComercialPanel`:
- Campos de filtro: **Pesquisar (nº ou cliente)**, **De** e **Até** (datas, texto
  `yyyy-MM-dd`, vazias = sem limite) + botão **Aplicar**.
- Tabela read-only: **Tipo · Nº · Cliente · Data · Estado · Total**, ordenada por data desc.
- Rodapé com **contagem de documentos** e **soma dos totais** das linhas apresentadas.
- Estilo via `UIHelper.styleTable`; filtro instantâneo no campo de texto via
  `UIHelper.onTextChange`.

## 4. Arquitectura

Módulo novo `modules/movimentos/` — **leitura agregada**, sem entidade/repositório próprios
(reutiliza os repositórios de `comercial`, exactamente como `ReportService` reutiliza
`InvoiceRepository`/`StockRepository`). Mantém SRP: uma responsabilidade — agregar e
projectar documentos comerciais para visualização.

```
modules/movimentos/
├── controller/MovimentosController.java   (só HTTP)
├── service/MovimentosService.java         (@Transactional(readOnly=true), agregação)
└── dto/
    ├── MovimentoDTO.java                   (record de saída)
    └── MovimentoTipo.java                  (enum + label PT)
```

## 5. Não-objectivos

- Não há paginação (volumes de loja são pequenos; rever se necessário).
- Não exporta PDF (cada documento já tem o seu).
- Não soma por moeda/IVA — o total é o `totalAmount` de cada documento.
