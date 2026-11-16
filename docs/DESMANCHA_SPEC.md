# Spec — Desmancha de carcaças (talho, F3)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md` (SA-C06, SA-C16)

## Problema

O talho compra um boi ou um quarto por peso e não sabe quanto custa cada kg de lombo ou de bife, nem se o
cortador rendeu o que devia.

## Decisões

1. **Modelo de desmancha** (`breakdown_templates`): carcaça, cortes esperados com rendimento esperado (%) e
   perda aceite (%). Só gestão define modelos.
2. **Desmancha** (`carcass_breakdowns`, série interna `DM`, não fiscal): peso da carcaça, custo total (vazio =
   preço de custo x peso), referência (guia do matadouro) e peso de cada corte. Regista-se de uma vez.
3. **Stock:** sai a carcaça e entram os cortes, com o tipo de movimento novo `PRODUCTION`, sempre por
   `StockMovementRegistrationService`. A desmancha repetida (mesma `requestKey`) não move stock duas vezes.
4. **Custo:** repartido pelos cortes conforme o valor de venda (kg x preço). Cortes sem preço (ossos) não
   levam custo; sem preços, reparte-se por peso. Os cortes somam sempre o custo da carcaça. O preço de custo
   de cada corte passa a média ponderada (stock no armazém ao custo antigo + kg novos ao custo da desmancha).
5. **Perda** = peso da carcaça − soma dos cortes. Cortes acima do peso da carcaça são recusados. Perda acima
   da aceite fica assinalada na lista e no detalhe.
6. **Anular** (só gestão, com motivo): retira os cortes e repõe a carcaça, com movimentos `REVERSAL`. É recusada
   se parte dos cortes já foi vendida. O preço de custo médio não é recalculado na anulação.

## API (`/api/butchery`)

| Método | Caminho | Quem |
|---|---|---|
| GET / PUT | `/templates?companyId=` · `/templates` | vendedor lê · gestão grava |
| GET | `/breakdowns?companyId=&days=90` | vendedor |
| POST | `/breakdowns` | vendedor (bancada) |
| POST | `/breakdowns/{id}/cancel` | gestão |

Migração `V124__desmancha_talho.sql`: tabelas novas e `PRODUCTION` no `CHECK` de `stock_movements`.

## Interface

Módulo **Talho** na barra lateral (capacidade `CARCASS_BREAKDOWN`, só no perfil Talho), com indicadores do
mês. Separador **Desmanchas**: lista à esquerda; à direita os cortes com rendimento real e esperado, custo por
kg, preço e margem. "Nova Desmancha" preenche os cortes a partir do modelo e mostra a perda enquanto se pesa.
Separador **Modelos de Desmancha**.

## Testes

`BreakdownCostRulesTest` (repartição por valor, soma exacta, por peso, cortes acima da carcaça, tolerância) e
`CarcassBreakdownHttpIntegrationTest` (stock, custo por kg, rendimento, perda, idempotência, recusa e anulação).
Desktop: `SectorModulesHarnessTest` SM-04.
