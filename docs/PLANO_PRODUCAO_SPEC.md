# Spec — Plano de Produção da padaria (SA-C06, F4)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Prompt:** `docs/prompts/PROMPT_PRODUCAO_TRANSFORMACAO.md`

## Problema

A padaria decide "a olho" quantos pães fazer, descobre a meio da madrugada que falta farinha, não sabe quanto
custa cada pão e deita fora as sobras sem registo.

## Decisões

1. **Receitas** = as fichas técnicas (BOM) de sempre, tipo Produção Padrão e activas. O módulo não cria outro
   cadastro: o separador Receitas mostra-as e "Nova Receita" abre o mesmo formulário.
2. **Plano do dia** (`production_plans`, um por empresa, armazém e dia): receita e quantidade planeada. Uma linha
   já produzida não sai do plano.
3. **Sugestão:** média vendida (faturas realizadas) no mesmo dia da semana nas últimas 4 semanas, arredondada
   para cima. Uma semana sem vendas conta como zero, para não produzir o que deixou de vender.
4. **Matéria-prima necessária** para o que falta produzir, com a quebra da receita, contra o stock do armazém.
   Ordenada pelo que falta comprar.
5. **Apontar produção** (`production_runs`): consome os ingredientes por FEFO e dá entrada do produto com o
   lote `PD<aammdd>-<linha>-<n>` e a validade (obrigatória nos perecíveis), com o tipo de movimento `PRODUCTION`.
   Recusa se faltar ingrediente. Custo = ingredientes ao preço de custo + mão-de-obra e gastos gerais da receita
   na proporção; o produto fica com custo médio ponderado (`AverageCostUpdater`, partilhado com a desmancha).
   A mesma `requestKey` não produz duas vezes.
6. **Fechar o dia:** regista as sobras de cada produto (nunca mais do que o produzido). As marcadas para abater
   vão para a quebra por validade; as outras ficam em stock e o módulo Validades remarca-as. Fechado, o dia não
   aceita lotes nem alterações.

## API (`/api/production-plans`, vendedores e gestão)

| Método | Caminho |
|---|---|
| GET | `/day?companyId=&warehouseId=&date=` |
| GET | `/suggestions?companyId=&date=` |
| PUT | `/` (guardar plano) |
| GET | `/{id}/requirements?companyId=` |
| POST | `/runs` |
| POST | `/{id}/close` |

Migração `V126__plano_producao.sql`: só tabelas novas.

## Interface

Módulo **Plano de Produção** (capacidade `PRODUCTION`) com indicadores do dia. Barra do dia (anterior, data,
seguinte, hoje, armazém). Separadores **Plano do Dia** (sugerir, acrescentar receita, guardar, apontar, fechar),
**Matéria-Prima Necessária**, **Lotes Produzidos** e **Receitas**.

## Testes

`ProductionPlanRulesTest` (ingredientes com quebra, sugestão por dia da semana, falta) e
`ProductionPlanHttpIntegrationTest` (plano, necessidades, falta de ingrediente, custo por unidade, idempotência,
validade obrigatória, fecho com abate e dia fechado).
