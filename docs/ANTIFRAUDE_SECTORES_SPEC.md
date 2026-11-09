# Spec — Controlos anti-fraude por sector (SA-C21, F6)

**Data:** 2026-11-09 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md` §4

## Problema

As perdas de uma loja raramente aparecem num só número: um operador que abate muito mais do que os outros, uma
desmancha que "perde" 30%, guias que nunca chegam a fatura, pesos escritos à mão, caixas que fecham sempre com
falta. A gestão precisa de uma lista curta do que verificar, por gravidade.

## Decisões

1. **Só leitura.** O relatório lê os registos que já existem (auditoria, caixas, quebras, desmanchas, planos,
   consignações, guias, levantamentos, granel, báscula, vasilhame, linhas de fatura). Não cria tabelas nem altera nada.
2. **Um controlo por classe e sector** (`ExceptionCheck`): `ButcheryExceptionChecks`, `BakeryExceptionChecks`,
   `YardExceptionChecks`, `GeneralExceptionChecks`. Acrescentar um controlo não mexe nos outros.
3. **Gravidade:** ALTA (verificar hoje), MÉDIA (esta semana), BAIXA (para conhecimento). Ordenado pela gravidade e
   depois pela data mais recente.
4. **Só gestão** (`/api/exceptions`), no ecrã e em PDF. O PDF lembra que uma excepção não prova fraude.

## Controlos e limites

| Sector | Código | Regra | Gravidade |
|---|---|---|---|
| Talho | `BREAKDOWN_LOSS` | Perda da desmancha acima da aceite pelo modelo | ALTA se mais de 5 pontos acima |
| Talho | `WASTE_BY_OPERATOR` | Operador com quebras ≥ 2× a média dos operadores (mínimo 2 operadores) | ALTA |
| Padaria | `LEFTOVERS_HIGH` | Sobras > 20% do produzido num dia fechado | ALTA acima de 40% |
| Padaria | `CONSIGNMENT_UNSETTLED` | Guia de consignação sem acerto há mais de 2 dias | ALTA acima de 7 dias |
| Estaleiro | `GUIDE_WITHOUT_INVOICE` | Guia aprovada sem fatura há mais de 3 dias | ALTA acima de 10 dias |
| Estaleiro | `PICKUP_OVERDUE` | Levantamento com prazo vencido | BAIXA |
| Estaleiro | `BULK_OUT_OF_TOLERANCE` | Medição de granel fora da tolerância | ALTA se já acertada |
| Estaleiro | `WEIGHING_MANUAL` | Pesagem escrita à mão na báscula | MÉDIA |
| Estaleiro | `RETURNABLES_LONG_OUT` | Vasilhame fora há mais de 60 dias | BAIXA |
| Geral | `CANCELLATIONS_BY_OPERATOR` | ≥ 3 anulações ou devoluções do mesmo operador | ALTA a partir de 9 |
| Geral | `DISCOUNTS_BY_OPERATOR` | Descontos de linha acima de 10%, por operador | ALTA a partir de 10 |
| Geral | `CASH_DIFFERENCE` | Caixa fechada com diferença acima de 100 MT | ALTA se faltam mais de 1 000 MT |

"Levantamento acima do saldo pago" não aparece porque já é **bloqueado** (`MATERIAL_POR_LEVANTAR_SPEC`).

## Limites conhecidos

- **Báscula vs guia:** a guia de remessa não guarda o peso, por isso a diferença não se calcula.
- **Produção vs farinha:** o consumo é calculado pela receita, por isso não pode divergir. Só uma contagem física
  da farinha mostraria desvio (fica para o inventário).
- **Venda ao peso manual no POS:** o POS ainda não regista se o peso foi lido ou escrito.
- **Alerta no Painel Executivo:** o painel é de outra frente de trabalho; o relatório expõe as contagens por
  gravidade para ligar lá depois.

## Interface

Módulo **Excepções** (sem capacidade: visível a toda a gestão, nunca a vendedores) com indicadores por gravidade e
operadores envolvidos. Filtros por período (7, 30, 90 dias), gravidade e sector. O detalhe da linha escolhida
aparece por baixo; "Exportar PDF" imprime o período.

## Testes

`ExceptionReportTest` (operador fora da média, sozinho não conta; desmancha com 30% de perda contra 5% aceites
aparece em primeiro com gravidade alta; pesagem manual; período invertido recusado; PDF) e `SectorModulesHarnessTest`
SM-05 (visível sem capacidade, nunca a vendedores).
