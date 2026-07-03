# Spec — Reposição automática de stock

> Sugerir ao operador **que produtos encomendar** ao fornecedor, a partir do stock mínimo, para
> evitar ruturas. Leitura pura — não cria encomendas automaticamente.

**Última actualização:** 2026-07-03

## Problema

A loja só descobria que um produto estava a acabar quando ia à prateleira ou ao ecrã de stock,
produto a produto. Não havia uma vista única de **"o que preciso de encomendar hoje"**, apesar de já
existir o campo **stock mínimo** por produto e o alerta de mínimo no dashboard.

## Decisão

Novo `ReorderService.suggestions(companyId)` (leitura, guarda multi-tenant) que devolve a lista de
produtos a repor:

- **Elegíveis:** produtos com **controlo de stock** (`stockTracked`) e **`minStock > 0`**.
- **Critério:** stock **total da empresa** (soma de todos os armazéns) **abaixo do mínimo**.
  Produto sem qualquer linha de stock conta como **0** (é sugerido).
- **Quantidade sugerida:** o que falta para repor o mínimo (`minStock − stockAtual`),
  **arredondado para cima a caixas inteiras** (`unitsPerBox`) — a loja compra ao grosso. Devolve
  tanto **caixas** como **unidades**.
- **Ordenação:** mais urgentes primeiro (menor cobertura `stockAtual / minStock`).

Não cria encomendas — o operador vê a lista e cria a encomenda com o **fornecedor à sua escolha** na
aba "Encomendas a Fornecedor" (fornecedor não está ligado ao produto, por decisão de simplicidade).

- **API:** `GET /api/purchases/reorder-suggestions?companyId=`.
- **UI:** nova aba **"Reposição"** no `ComprasPanel` (tabela Produto · SKU · Stock Atual · Mínimo ·
  Und/Caixa · Sugerido em caixas · Sugerido em unidades; botão "Criar Encomenda" salta para a aba de
  encomendas; "Atualizar").

## Não-objetivos

- Não criar/enviar encomendas automaticamente (o humano confirma).
- Não escolher fornecedor automaticamente (produto não tem fornecedor preferido nesta iteração).
- Não introduzir "stock máximo"/nível-alvo — a reposição é até ao **mínimo** (evita comprar a mais).
  Um nível-alvo configurável fica como evolução futura.
