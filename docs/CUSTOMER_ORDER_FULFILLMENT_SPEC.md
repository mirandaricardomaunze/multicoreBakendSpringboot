# Atendimento, Reserva e Separacao de Pedidos

## Objectivo

Vendedores autorizados registam pedidos e enviam-nos para a central de separacao. O stock fica
reservado no envio e so sai fisicamente quando o pedido separado e facturado.

## Estados

`AWAITING_SEPARATION -> IN_SEPARATION -> SEPARATED -> INVOICED`. Os tres primeiros estados podem
transitar para `CANCELLED`. A primeira impressao termica inicia a separacao. Reimprimir exige outro
utilizador `MANAGER` ou `ADMIN`, senha valida e motivo.

## Invariantes

- `idempotencyKey` unico por empresa impede duplicacao.
- Disponivel = stock fisico - reservas activas; reservar nao cria movimento fisico.
- Facturar exige `SEPARATED`, consome a reserva e cria a saida `SALE` pelo fluxo fiscal existente.
- Cancelar liberta a reserva; pedidos facturados nao podem ser cancelados.
- O autor vem do token autenticado, nunca do corpo HTTP.
- Cada accao grava evento imutavel com estados, autor, papel, data, terminal e detalhes.

## Contratos

- `POST /api/comercial/orders/fulfillment`
- `POST /api/comercial/orders/{id}/picking-print`
- `POST /api/comercial/orders/{id}/picking-reprint`
- `POST /api/comercial/orders/{id}/separate`
- `POST /api/comercial/orders/{id}/fulfillment-bill`
- `GET /api/comercial/orders/{id}/events`
