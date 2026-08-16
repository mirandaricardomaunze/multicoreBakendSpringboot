# Margem com o custo do acto da venda — harness

Cenários de verificação da [MARGEM_CUSTO_HISTORICO_SPEC.md](MARGEM_CUSTO_HISTORICO_SPEC.md).
**MC-01..MC-06** automáticos; **MC-50..MC-53** manuais.

---

## Automáticos

### `ReportServiceTest` — cálculo da margem

| ID | Cenário | Esperado | Falhava antes |
|---|---|---|---|
| MC-01 | Vendidas 2 un. a 100 com custo gravado de 60; cadastro hoje diz 80 | custo **120,00**, margem **80,00** | ✅ dava custo 160 / margem 40 |
| MC-02 | Linha anterior à V37 (sem custo gravado), cadastro a 80 | custo 160,00 (estimativa declarada) | — (não-regressão) |
| MC-03 | Produto sem preço de compra e linha sem custo | custo 0, margem = receita; sem `NullPointerException` | — (guarda) |

**MC-01 confirmado a falhar contra o código antigo** (2026-08-15): reposto o cálculo pelo preço
actual, a asserção falhou com `expected: <0> but was: <1>` na comparação de 120,00; reposto o
fix, verde.

### `ComercialServiceTest` / `POSServiceTest` — a fotografia é tirada

| ID | Cenário | Esperado |
|---|---|---|
| MC-04 | `createInvoice` com produto a 60,00 de compra | linha gravada com `unitCost = 60.00` |
| MC-05 | `billOrder` com produto a 45,50 | linha da fatura com `unitCost = 45.50` (data da **fatura**) |
| MC-06 | `checkout` do POS com produto a 72,25 | linha gravada com `unitCost = 72.25` |

**Execução:** `mvn -o test -Dtest=ReportServiceTest,ComercialServiceTest,POSServiceTest`
→ 72 testes, 0 falhas (2026-08-15).

---

## Manuais (backend de pé)

| ID | Passos | Esperado |
|---|---|---|
| MC-50 | Vender um artigo; anotar a margem no relatório diário | margem = preço − custo actual |
| MC-51 | Alterar o preço de compra do artigo no cadastro | a margem **daquela venda** não muda |
| MC-52 | Vender de novo o mesmo artigo | a nova venda usa o preço novo |
| MC-53 | Consultar uma venda anterior à V37 | margem continua a aparecer (estimativa pelo preço actual) |

**Estado:** por executar (exigem backend de pé).
