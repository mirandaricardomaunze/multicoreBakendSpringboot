# Spec — Obras e projectos do cliente (SA-C09)

**Data:** 2026-11-08 · **Estado:** feito · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`

## Problema

Um empreiteiro tem várias obras ao mesmo tempo e quer saber, por obra, o que encomendou, o que foi
faturado e o que ainda deve.

## Decisões

1. **Obra** (`customer_projects`): pertence a um cliente identificado (nunca ao Consumidor Final), com
   código `OB-0001` por empresa, nome, morada, contacto e estado activa/fechada.
2. **Ligação à encomenda** por tabela própria (`customer_project_orders`, uma obra por encomenda). A
   encomenda, a fatura e os seus contratos HTTP **não mudam**.
3. Só se liga uma encomenda a uma obra **do mesmo cliente** e activa. Mudar de obra fica na auditoria;
   retirar a ligação é só gestão.
4. **Extracto da obra:** cada encomenda, a fatura em que se transformou e o que está em dívida (só faturas
   emitidas contam como faturadas).
5. Migração `V122__obras_cliente.sql` só acrescenta tabelas.

## API (`/api/projects`)

| Método | Caminho | Quem |
|---|---|---|
| GET / PUT | `/api/projects?companyId=&clientId=` · `/api/projects` | vendedor |
| GET | `/api/projects/{id}/statement?companyId=` | vendedor |
| PUT | `/api/projects/{id}/orders/{orderId}?companyId=` | vendedor |
| GET | `/api/projects/orders/{orderId}?companyId=` | vendedor (204 sem obra) |
| DELETE | `/api/projects/orders/{orderId}?companyId=` | gestão |

## Interface

- Módulo **Obras** na barra lateral (capacidade `CUSTOMER_PROJECTS`), com indicadores (obras activas,
  encomendado, em dívida), lista de obras e extracto à direita.
- No editor de encomendas: campo "Obra / projecto" com o botão "+" para criar a obra do cliente; a ligação
  é feita ao guardar.

## Testes

`CustomerProjectHttpIntegrationTest`: criação, recusa do Consumidor Final, ligação, extracto, recusa de
encomenda de outro cliente e retirada da ligação.
