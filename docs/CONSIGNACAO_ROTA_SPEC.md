# Spec — Consignação e venda em rota (SA-C11, F4)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

A padaria deixa pão de manhã em mercearias e barracas, recolhe as sobras no dia seguinte e cobra só o vendido. O
distribuidor de bebidas faz o mesmo na rota. Hoje isto vive num caderno: ninguém sabe quanto está nos
revendedores, nem quanto se perde em sobras.

## Decisões (incluindo a decisão em aberto §7.5 da SPEC mestre)

1. **A mercadoria sai do stock da loja na entrega**, porque saiu fisicamente. Vai para o armazém
   **"Em consignação"** (tipo trânsito, sem venda no POS), criado automaticamente, por transferência. O stock da
   loja fica certo e o que está nos revendedores vê-se num só sítio.
2. **Guia de consignação** (série interna `CG`, não fiscal): revendedor identificado (nunca o Consumidor Final),
   armazém de origem, dia, rota/viatura e quantidades. Recusa entregar mais do que o stock. A mesma `requestKey`
   não entrega duas vezes.
3. **Acerto** (só gestão, porque emite fatura): por artigo, o que **voltou bom** regressa ao armazém da loja; o
   **estragado** é abatido como quebra por validade (alçada de aprovação normal); o **vendido** = entregue −
   devolvido − estragado. Nunca pode voltar mais do que foi entregue.
4. **Só o vendido é faturado**, com uma **fatura FT normal** ao revendedor, a partir do armazém de consignação, ao
   preço da tabela de preços do revendedor (`TABELAS_PRECO_SPEC`). Sem vendas, não há fatura. Uma guia acerta-se
   uma só vez.

## API (`/api/consignments`)

| Método | Caminho | Quem |
|---|---|---|
| GET | `?companyId=&days=30` (abertas + últimos dias) | vendedor |
| POST | `/` (entregar) | vendedor |
| POST | `/{id}/settle` | gestão |

Migração `V128__consignacao_rota.sql`: só tabelas novas.

## Interface

Módulo **Consignação e Rota** (capacidade `CONSIGNMENT`, perfil Padaria) com indicadores (guias no revendedor,
entregue hoje, faturado a 30 dias, percentagem de sobras). "Nova Entrega" e "Acertar", com o vendido calculado
enquanto se contam as devoluções.

## Limites

- O lote e a validade do pão não acompanham a mercadoria para o armazém de consignação (entra sem lote).
- Não há ainda guia de transporte fiscal para a rota; a guia CG é interna.

## Testes

`ConsignmentHttpIntegrationTest`: entrega acima do stock recusada, Consumidor Final recusado, idempotência, stock
da loja, devolução a mais recusada, acerto com fatura só do vendido (valor com IVA), devoluções de volta à loja e
segundo acerto recusado.
