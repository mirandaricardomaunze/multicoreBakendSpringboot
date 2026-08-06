# HARNESS — Taxa de IVA canónica

Complementa [IVA_TAXA_CANONICA_SPEC.md](IVA_TAXA_CANONICA_SPEC.md). `IV-01..07` automáticos;
`IV-50..` manuais/ao vivo.

## Automáticos

| ID | Onde | Cenário | Esperado |
|----|------|---------|----------|
| IV-01 | `ComercialServiceTest` | fatura de artigo **isento** com pedido a insistir em 16% | IVA 0,00; total = líquido |
| IV-02 | `ComercialServiceTest` | fatura de artigo a **5%** com pedido a insistir em 16% | IVA 10,00 sobre 200 (não 32,00) |
| IV-03 | `ComercialServiceTest` | **encomenda** de artigo isento | IVA 0,00 (a fatura herda esta linha) |
| IV-04 | `ProductTest` | artigo sem taxa no cadastro | taxa-padrão (16%) |
| IV-05 | `ProductTest` | artigo isento | 0% — **não** cai no fallback |
| IV-06 | `ProductTest` | artigo a 5% / 16% | a do cadastro |
| IV-07 | `ProductTest` | `TaxRate` configurada sem valor | taxa-padrão, sem rebentar |
| — | `POSServiceTest` | `checkout_produtoIsento_naoAplicaIva` (já existia) | continua verde após o refactor |

**Verificado que IV-01 e IV-02 falham contra o código antigo** (repondo `lineReq.taxRate()`:
`expected: <0> but was: <1>`), como manda o processo de regressão do projecto.

## Ao vivo (backend de pé, dados demo)

| ID | Passos | Esperado |
|----|--------|----------|
| IV-50 | `POST /api/comercial/invoices` de Farinha (isento) com `"taxRate":0.16` | `taxAmount` = 0,00 |
| IV-51 | `POST /api/pos/checkout` do mesmo artigo | `taxAmount` = 0,00 — **igual ao da fatura** |
| IV-52 | Fatura de Óleo (16%) | `taxAmount` = 16% do líquido |
| IV-53 | Fatura de Massa (5%) | `taxAmount` = 5% do líquido |
| IV-54 | PDF da fatura de um artigo isento | bloco de totais com IVA 0,00 |

## Manuais (UI)

| ID | Cenário | Evidência |
|----|---------|-----------|
| IV-60 | Faturação → juntar artigo isento ao rascunho | "Total Rascunho" sem IVA; bate certo com a fatura emitida |
| IV-61 | Faturação → juntar artigo a 16% | rascunho com IVA a 16% |
| IV-62 | Encomenda → mesmos dois casos | idem |

## Definition of done

- `mvn -o clean test` verde.
- IV-50/IV-51 dão o **mesmo** `taxAmount` para o mesmo artigo.
