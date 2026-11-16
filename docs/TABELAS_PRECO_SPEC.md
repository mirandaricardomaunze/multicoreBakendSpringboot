# Spec — Tabelas de preço por cliente (SA-C03)

**Data:** 2026-11-08 · **Estado:** feito (F1) · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Harness:** `docs/SECTORES_ACTIVIDADE_HARNESS.md` · **Complementa:** `docs/PRECO_GROSSO_SPEC.md`

## Problema

O empreiteiro, o revendedor e o funcionário pagam preços diferentes do particular. O sistema só tinha o
preço normal e o preço de grosso por quantidade, e o desconto era feito à mão, linha a linha.

## Decisões

1. **Tabela** (`price_lists`): código único por empresa, nome, validade opcional, activa ou não.
2. **Preços** (`price_list_items`): por produto, na unidade base e na mesma base de IVA de
   `Product.unitPrice`. `min_quantity` cria escalões (0 a 900 MT, a partir de 10 a 850 MT).
3. **Atribuição** (`client_price_lists`): no máximo uma tabela por cliente e por empresa.
   **O Consumidor Final não aceita tabela**, porque isso mudaria todas as vendas de balcão.
4. **Fonte única do preço:** `PriceResolutionService`.
   1. Tabela do cliente, activa e válida hoje, no maior escalão atingido.
   2. Senão, a regra de sempre: `Product.effectiveUnitPrice` (grosso ou normal).
   Sem tabelas atribuídas o resultado é **exactamente o de antes**.
5. **Onde se aplica:** fatura, encomenda e cotação (o servidor calcula o preço) e o checkout do POS
   quando o desktop não envia preço.
6. **POS sem regressão.** O piso de preço do caixa (`PosPriceGuard`) passa a ser o menor entre o preço
   normal e o da tabela do cliente: nunca fica mais apertado do que era. O carrinho do POS no desktop
   ainda mostra o preço normal; mostrar o preço da tabela no carrinho fica para a F2.
7. **Compatibilidade.** Os serviços de venda recebem o `PriceResolutionService` como último argumento.
   As fachadas antigas que os montam à mão (`ComercialService`, `QuotationService`) passam `null`, e
   `null` mantém a regra de sempre. No POS, o preço por omissão vem de `PosPriceGuard.defaultPrice`,
   para não fazer crescer o `PosCheckoutAndPaymentService`, que está no tecto de linhas.
8. Faturas já emitidas nunca mudam: o preço fica gravado na linha.

## API (`/api/pricing`, leitura para vendedores, alteração para gestão)

| Método | Caminho | Uso |
|---|---|---|
| GET/PUT | `/api/pricing/lists` | Listar / criar ou editar tabela |
| DELETE | `/api/pricing/lists/{id}?companyId=` | Eliminar (recusado se tiver clientes) |
| GET | `/api/pricing/lists/{id}/items?companyId=` | Preços da tabela |
| PUT / DELETE | `/api/pricing/items` · `/api/pricing/items/{id}` | Guardar / remover preço |
| GET | `/api/pricing/lists/{id}/clients?companyId=` | Clientes da tabela |
| GET / PUT | `/api/pricing/clients/{clientId}` · `/api/pricing/clients` | Ver / atribuir ou retirar |
| GET | `/api/pricing/resolve?companyId=&clientId=&productId=&quantity=` | Preço que o documento vai gravar |

Auditoria: `PRICE_LIST_*`, `PRICE_LIST_ITEM_*`, `CLIENT_PRICE_LIST_ASSIGN` (antes e depois).

## Interface

Comercial › **Promoções** › secção **Tabelas de Preço**: lista com pesquisa e estado. O detalhe tem os
separadores **Preços** (com o preço normal e a diferença em %) e **Clientes**. A secção fica dentro de
"Promoções" porque a barra do Comercial está no limite de largura (`TabStripFitsTest`).
