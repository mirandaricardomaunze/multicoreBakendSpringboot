# Produto perecível e validade do lote

Decidido pelo responsável em 2026-11-04.

## Regra

- O produto tem a opção **"Produto perecível (exige validade nas entradas)"** (`Product.tracksExpiry`,
  coluna `products.tracks_expiry`, migração V111).
- **Perecível:** compra (`PURCHASE`) e entrada manual (`ENTRY`) sem data de validade são recusadas com
  `BusinessRuleException`. Fonte única: `StockMovementRegistrationService.requireExpiryForPerishableIntake`.
- **Não perecível** (loiça, mobília, ferragens): entra sem validade. O lote fica com a data interna
  `9999-12-31`, que a UI e os PDF mostram como "—" e que nunca gera alerta de validade.
- Devoluções, estornos, transferências e acertos repõem stock que já existia e **não** exigem validade.
- Um lote já vencido é sempre recusado na entrada (`ProductBatchMutationService`); o diálogo de
  entrada de lote avisa logo, sem pedir confirmação.

## Encomendas a fornecedor

A validade de cada linha é indicada na encomenda. Se um produto perecível chegar à recepção sem
validade, a recepção é recusada e a mensagem pede para editar a linha. Isto é sempre possível, porque
a encomenda só bloqueia a edição depois da primeira recepção.

## Migração dos dados existentes

A V111 marca como perecíveis os produtos que já tinham pelo menos um lote com validade real
(`expiration_date < 9999-01-01`). Os restantes ficam não perecíveis.

**Rever a marcação:** em Stock › Níveis de Stock, a coluna **Validade** mostra "Perecível" ou
"Sem validade" (`StockDTO.tracksExpiry`), e o filtro de estado tem as opções **Perecíveis** e
**Sem validade**. Um produto mal marcado corrige-se em Editar Produto.

## Compatibilidade HTTP

- `ProductDTO.tracksExpiry` e `StockDTO.tracksExpiry` (boolean) foram acrescentados no fim do record,
  cada um com um construtor compatível.
- `CreateProductRequest.tracksExpiry` é `Boolean`: `null` (cliente anterior) mantém o valor actual na
  edição e vale `false` na criação.

## Testes

- `PerishableIntakeRuleTest`: tipos de movimento abrangidos e excluídos.
- `StockPerishableFilterTest`: coluna Validade e filtros Perecíveis / Sem validade.
- `StockProductActionsDecompositionHarnessTest` (SPAD-04): o diálogo de lote recusa lotes vencidos e
  respeita `tracksExpiry`.
