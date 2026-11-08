# Spec — Validades e remarcação (SA-C14, SA-C15, F3)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

A carne, o pão e os lacticínios vencem na prateleira. Ninguém vê a tempo o que está a vencer, o desconto é dado
"a olho" no balcão e o que venceu fica no stock até à contagem.

## Decisões

1. **FEFO** já existia em todas as saídas (`ProductBatchService.consumeFEFO`): a venda e a produção levam
   sempre o lote que vence primeiro. A entrada de um lote já vencido continua recusada.
2. **Quadro de validades:** lotes com stock, vencidos ou a vencer nos próximos 14 dias. Situações: vencido,
   vence hoje, 1 a 2 dias, esta semana. Para cada lote mostra o valor a custo, o desconto que o POS já dá hoje e
   o desconto sugerido pelas regras.
3. **Regras de remarcação** (`markdown_rules`): "a partir de N dias antes da validade, X% de desconto", gerais
   ou por categoria. Aplica-se o maior desconto cuja janela já começou; a regra da categoria do artigo prevalece
   sobre as gerais.
4. **Remarcar** = criar uma **promoção de loja** normal (percentagem, só aquele artigo, de hoje até à validade do
   lote). O POS já aplica promoções sozinho. A promoção vale para o artigo, não para um lote, mas como a venda
   sai por FEFO é este lote que sai primeiro. Não se remarca um lote vencido, nem com desconto menor do que o
   que já existe.
5. **Aplicar regras:** botão para a gestão. Também corre uma vez por dia, quando um gestor abre o módulo.
6. **Abater vencido** = quebra normal com motivo "Validade Expirada", com a alçada de aprovação de sempre.
   Recusado para lotes ainda dentro da validade (esses remarcam-se).
7. **Perdas no mês** = quebras aprovadas por validade.

## API (`/api/expiry`)

| Método | Caminho | Quem |
|---|---|---|
| GET | `/board?companyId=&days=14` | vendedor |
| GET / PUT | `/rules?companyId=` · `/rules` | vendedor lê · gestão grava |
| POST | `/markdowns` · `/markdowns/apply-rules?companyId=` | gestão |
| POST | `/write-offs` | vendedor (aprovação conforme alçada) |

Migração `V125__remarcacao_validades.sql`: só a tabela `markdown_rules`.

## Interface

Módulo **Validades** (capacidade `PERISHABLES`: mercearia, talho, padaria) com indicadores (vencido em stock,
em risco a 7 dias, lotes remarcados, perdas no mês). Separadores **Lotes por Vencer** e **Regras de Remarcação**.

## Testes

`ExpiryRulesTest` (situações, maior desconto, precedência da categoria) e `ExpiryHttpIntegrationTest`
(quadro, regras, aplicação sem duplicar, remarcação menor recusada, abate dentro da validade recusado, abate
de lote vencido como quebra por validade).
