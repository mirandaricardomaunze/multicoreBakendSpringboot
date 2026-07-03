# Harness — Preço de venda ao grosso

> Cenários para [PRECO_GROSSO_SPEC.md](PRECO_GROSSO_SPEC.md).
> PG-01/PG-02 automáticos (`ComercialServiceTest`); PG-50..PG-53 manuais (UI).

**Última actualização:** 2026-07-03

## Automáticos — `ComercialServiceTest`

| ID    | Cenário | Esperado |
|-------|---------|----------|
| PG-01 | Produto: retalho 100, grosso 80, mínimo 10. Fatura de **qtd 10**. | Linha usa **80** (grosso). |
| PG-02 | Mesmo produto, fatura de **qtd 5**. | Linha usa **100** (retalho). |

## Manuais (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| PG-50 | Cadastrar/Editar produto → "Preço Grosso" 80, "Qtd mín. grosso" 10. | Gravado; reabrir Editar mostra os valores. |
| PG-51 | Faturação: vender 12 unidades desse produto. | Preço unitário na linha = 80 (total 12 × 80 + IVA). |
| PG-52 | Faturação: vender 9 unidades. | Preço unitário = 100 (retalho). |
| PG-53 | POS: vender ≥ mínimo do produto. | Aplica automaticamente o preço de grosso. |

## Verificação

- `mvn clean test` → verde (PG-01/PG-02 em `ComercialServiceTest`; a regra vive em
  `Product.effectiveUnitPrice`, partilhada por fatura/encomenda/POS).
