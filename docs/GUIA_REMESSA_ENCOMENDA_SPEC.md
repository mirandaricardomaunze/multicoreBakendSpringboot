# SPEC — Guia de Remessa a partir da Encomenda

**Criado em:** 2026-07-23
**Módulo:** `comercial`
**Séries afectadas:** nova série `GR` (guia de remessa ao cliente)
**Migração:** `V34__delivery_guide.sql`

> Reverte a decisão de 2026-06-21 em [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) §7.1
> ("Guia de Remessa ao cliente não é requisito"). **Decisão do utilizador (2026-07-23):** passa a
> ser requisito. Este documento é a nova fonte de verdade sobre a guia ao cliente.

---

## 1. Objectivo

Permitir converter uma **encomenda de cliente** (aprovada) numa **Guia de Remessa** — o documento
que acompanha a mercadoria **expedida ao cliente** e que **dá saída ao stock**.

## 2. Regra de negócio central (decidida com o utilizador)

**Caminhos separados.** Uma encomenda segue **um** de dois destinos, nunca os dois:

```
encomenda (EC, PENDING = aprovada)
   ├── [Gerar Guia]  → guia GR (PENDING_APPROVAL) → [Aprovar] → stock SAI (SALE)   ⇒ encomenda GUIDED
   └── [Faturar]     → fatura FT (billOrder, inalterado)      → stock SAI (SALE)   ⇒ encomenda BILLED
```

- A guia e a fatura **nunca** partilham a mesma encomenda.
- **Para faturar mercadoria já expedida por guia, cria-se uma NOVA encomenda** e fatura-se essa.
- Consequência de desenho: **`billOrder` NÃO é alterado** — continua a aceitar só encomendas
  `PENDING` e a baixar stock. Uma encomenda com guia deixa de estar `PENDING`, logo já não é
  faturável (o guard existente basta).

## 3. Momento do stock — só na aprovação

Espelha a **Guia de Transferência** (`StockTransferService`): a guia nasce `PENDING_APPROVAL` e
**o stock só sai na aprovação**. Rejeitar/cancelar **nunca** move stock.

A saída de stock usa exactamente a mesma chamada que o `billOrder` já faz:
`inventoryService.registerMovement(produto, armazém, qtd.negate(), "SALE", lote, série, descrição)`
— FEFO/lote e ledger de stock tratados internamente, idênticos à faturação.

## 4. Máquina de estados

### Guia (`DeliveryGuideStatus`)
```
PENDING_APPROVAL ──approve──▶ APPROVED   (stock SAI aqui; terminal)
        │
        ├────────reject────▶ REJECTED    (motivo obrigatório; sem stock; encomenda volta a PENDING)
        └────────cancel────▶ CANCELLED   (só se ainda não aprovada; encomenda volta a PENDING)
```

### Encomenda (`Order.status`, string)
```
PENDING ──criar guia──▶ GUIDE_PENDING ──aprovar guia──▶ GUIDED   (terminal)
   ▲                          │
   └──── reject/cancel guia ──┘        (a encomenda volta a ser faturável / re-guiável)
```
`GUIDE_PENDING` e `GUIDED` **não** são faturáveis nem canceláveis (não são `PENDING`/
`PENDING_APPROVAL`) — o guard de `billOrder` e a lista de canceláveis já os excluem.

## 5. Permissões e auditoria

- **Criar guia:** utilizador com acesso à empresa (tenant). Guia nasce pendente, sem efeito no stock.
- **Aprovar / Rejeitar:** `MANAGER`/`ADMIN` (`PermissionGuard.requireManagerOrAdmin`).
- **Auditoria:** `DELIVERY_GUIDE_CREATE`, `DELIVERY_GUIDE_APPROVE`, `DELIVERY_GUIDE_REJECT`,
  `DELIVERY_GUIDE_CANCEL`.

## 6. Numeração — série `GR`, por empresa

- `DocumentSeries.DELIVERY_GUIDE = "GR"`; número via `DocumentNumberService.next(...)` (gapless por
  ano, por empresa — mesmo mecanismo `document_sequences` da V30).
- Tabela `delivery_guides` com **`UNIQUE(company_id, guide_number)`** (lição da V31 — nunca UNIQUE
  global no número).

## 7. Camadas (ARCHITECTURE.md)

| Camada     | Ficheiro                                                                 |
|------------|---------------------------------------------------------------------------|
| model      | `DeliveryGuide`, `DeliveryGuideLine`, `DeliveryGuideStatus` (comercial)   |
| repository | `DeliveryGuideRepository` (finder por empresa + fetch de linhas)          |
| dto        | `CreateDeliveryGuideRequest`, `DeliveryGuideDTO`, `DeliveryGuideLineDTO`  |
| service    | `DeliveryGuideService` (regras + `@Transactional`)                        |
| controller | `DeliveryGuideController` — `/api/comercial/delivery-guides`              |
| printing   | `DeliveryGuidePrintService` — `GET /api/print/delivery-guide/{id}`       |

- DTO em toda a fronteira; nunca `@Entity` no controller.
- `BusinessRuleException` (mensagem PT) em toda a violação de regra.
- Injecção por construtor, campos `final`.

## 8. PDF

Reutiliza os building blocks partilhados (`CompanyHeaderRenderer`, `LineItemsTableRenderer`,
`LineRowMapper`, `DocumentConfigService`) e acrescenta bloco de **transporte** e **assinaturas**
(Expedidor / Recebi em conformidade), como a guia derivada de fatura já faz. Título "Guia de
Remessa", nº `GR-...`, destinatário = cliente, origem = armazém, referência à encomenda.

## 9. Cliente desktop Swing

- `ComercialApiClient` chama exclusivamente os endpoints HTTP de GR e obtém o PDF como bytes.
- A aba Encomendas expõe **Converter em Guia** apenas sobre a encomenda selecionada; o backend volta
  a validar o estado atomicamente.
- O diálogo de transporte recolhe responsável, viatura/matrícula e observações.
- A aba **Guias de Remessa (GR)** lista e filtra documentos e permite aprovar, rejeitar, cancelar,
  imprimir e atualizar. A confirmação de aprovação avisa que o stock será movimentado.
- Regras de estado, permissões, stock, tenant e auditoria permanecem no backend.

## 10. Fora de âmbito (v1)

- **Guia parcial** (expedir parte da encomenda) — v1 gera guia da encomenda **inteira**.
- Fatura automática a partir da guia — por decisão, faturação é caminho separado (nova encomenda).
</content>
