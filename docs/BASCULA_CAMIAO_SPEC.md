# Spec — Báscula de camião (SA-C18, F5)

**Data:** 2026-11-09 · **Estado:** feito, por validar com a báscula da loja piloto · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

O estaleiro e a fábrica de blocos vendem e compram areia e brita ao camião. Sem pesagem registada, o peso
facturado é o que o motorista diz, e não há prova do que entrou ou saiu.

## Decisões

1. **Talão de pesagem** (série interna `TP`, não fiscal): matrícula, motorista, sentido, cliente ou fornecedor,
   artigo e documento (fatura ou guia).
2. **Duas pesagens:** à entrada e à saída. **Saída de material** (venda): o camião tem de sair mais pesado.
   **Entrada de material** (compra): tem de sair mais leve. O líquido é a diferença; uma pesagem trocada é
   recusada em vez de ficar com sinal errado.
3. **Uma matrícula, um talão aberto.** As matrículas comparam-se sem espaços nem hífenes.
4. **Peso da báscula:** "Ler Báscula" usa a ligação série do posto (`BALANCA_SERIE_SPEC`) e só aceita peso
   estável. Um peso **escrito à mão** é aceite mas fica marcado como manual no talão, na lista e na auditoria
   (`WEIGHING_*_MANUAL`), para o controlo anti-fraude.
5. **Talão em PDF** com as duas pesagens, o líquido e as assinaturas do operador e do motorista.
6. A pesagem não move stock nem cria documento fiscal: a fatura ou a guia continuam a ser o documento.

## API (`/api/weighbridge`, vendedores e gestão)

`GET /tickets?companyId=&days=7` (abertos + recentes) · `POST /tickets` (1.ª) · `POST /tickets/{id}/second` ·
`GET /tickets/{id}/pdf`. Migração `V131__bascula_camiao.sql`: só a tabela nova.

## Interface

Módulo **Báscula** (capacidade nova `TRUCK_SCALE`, perfis Estaleiro e Fábrica de blocos) com o peso em directo no
topo e indicadores (no recinto, pesagens e toneladas hoje, pesos manuais). Após a 2.ª pesagem o talão abre logo
para imprimir.

## Testes

`WeighbridgeTest`: líquido por sentido, pesagem trocada recusada, matrícula normalizada; talão com idempotência,
segundo talão aberto da mesma matrícula recusado, 2.ª pesagem errada recusada, peso manual assinalado, talão
fechado não reabre, PDF.
