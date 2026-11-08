# Spec — Encomendas com data e personalização (SA-C13, F4)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

O bolo de aniversário para sábado às 15:00, com "Parabéns Ana" e uma fotografia do modelo, vive num papel. A
cozinha não sabe o que tem de fazer em cada dia, o balcão não sabe se o cliente já pagou o sinal.

## Decisões

1. **A encomenda continua a ser o documento comercial** (Pedidos). A data de entrega fica na própria encomenda
   (`expected_delivery_date`); o módulo guarda à parte (`order_customizations`, uma por encomenda) a hora, o modo
   (levanta na loja ou entrega na morada, que passa a ser obrigatória), o contacto, a personalização (até 1000
   caracteres) e a fotografia de referência (JPEG ou PNG, até 2 MB, validada pelos bytes).
2. **Agenda:** todas as encomendas não anuladas com data no período (hoje, amanhã, 7, 14 ou 30 dias), por dia e
   hora, com o estado na cozinha (por produzir, em produção, pronta, entregue) e o **sinal pago** (soma dos
   adiantamentos ligados à encomenda, `ADIANTAMENTOS_SPEC`).
3. **Enviar para produção:** soma as quantidades ao plano do dia da entrega (`PLANO_PRODUCAO_SPEC`), pela receita
   activa de cada artigo; os artigos sem receita são indicados. Só uma vez por encomenda; passa a "em produção".
4. **Ficha da cozinha** (PDF interno): entrega, modo, contacto, sinal, artigos, personalização e fotografia, com
   assinaturas de quem prepara e de quem recebe.

## API (`/api/custom-orders`, vendedores e gestão)

| Método | Caminho |
|---|---|
| GET | `/agenda?companyId=&from=&to=` (máximo 2 meses) |
| PUT | `/{orderId}` |
| POST | `/{orderId}/stage` · `/{orderId}/send-to-plan?companyId=` |
| GET | `/{orderId}/image?companyId=` · `/{orderId}/sheet?companyId=` |

Migração `V127__encomendas_personalizadas.sql`: só a tabela nova.

## Interface

Módulo **Agenda de Encomendas** (capacidade `CUSTOM_ORDERS`) com indicadores (encomendas, para hoje, prontas,
por receber). "Agendar Encomenda" escolhe uma encomenda aberta de Pedidos; "Entrega e Personalização" com
escolha da fotografia; à direita, a personalização e a fotografia. "Enviar para Produção", "Mudar Estado" e
"Ficha da Cozinha".

Os clientes dos módulos sectoriais novos foram agrupados em `SectorApiClients`, para o `MainFrame` não crescer.

## Testes

`CustomOrderHttpIntegrationTest`: morada obrigatória na entrega, fotografia inválida recusada, gravação com hora e
fotografia, agenda, envio ao plano (e recusa do segundo envio), estado e ficha em PDF.
