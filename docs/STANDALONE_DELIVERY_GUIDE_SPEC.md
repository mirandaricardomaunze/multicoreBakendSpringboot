# SPEC — Guia de remessa avulsa com artigos

Estado: **implementado e validado**  
Decisão: 2026-10-10

## Objectivo

Permitir emitir uma guia de remessa sem encomenda de origem, seleccionando cliente, armazém e
artigos directamente. A guia nasce pendente e só retira stock quando um gestor ou administrador a
aprova.

## Regras de negócio

1. A guia avulsa pertence sempre à empresa activa.
2. Cliente, armazém e pelo menos uma linha são obrigatórios.
3. Cada linha contém produto e quantidade positiva. Preço e imposto são copiados do cadastro do
   produto e têm finalidade informativa; a guia não é documento fiscal nem cria dívida.
4. A criação atribui número `GR`, grava auditoria e deixa a guia em `PENDING_APPROVAL`, sem movimento
   de stock.
5. A aprovação exige `MANAGER` ou `ADMIN` e cria saídas `SALE` por FEFO no armazém da guia.
6. Rejeição e cancelamento de guia pendente não movimentam stock.
7. Uma guia aprovada não pode ser cancelada pelo fluxo normal.
8. Uma guia avulsa não altera estado de encomenda; `order_id` e `order_number` ficam nulos.
9. Produto, cliente e armazém devem pertencer à empresa activa. O backend nunca confia apenas nos
   identificadores recebidos do desktop.
10. Falha em qualquer linha reverte toda a aprovação na mesma transacção.

## Contrato HTTP

`POST /api/comercial/delivery-guides/standalone`

```json
{
  "clientId": 10,
  "warehouseId": 3,
  "responsible": "João Chissano",
  "vehicle": "ABC-12-34",
  "notes": "Entrega parcial",
  "lines": [
    { "productId": 42, "quantity": 5.000 }
  ]
}
```

O endpoint devolve `DeliveryGuideDTO` com `orderId` e `orderNumber` nulos.

## Experiência desktop

- A central de guias apresenta a acção primária `Nova guia avulsa`.
- O formulário abre como página/editor, não como sequência de `JOptionPane`.
- Cabeçalho: cliente, armazém, responsável, viatura e observações.
- Grelha: produto, quantidade, embalagem, caixas, referência, código de barras, valor unitário, IVA
  e total.
- A escolha de produto pesquisa no backend por nome, SKU, referência ou código de barras, com
  paginação. Apenas o produto escolhido fica na linha.
- `F2` mantém suporte ao leitor de código de barras.
- A validação mantém o formulário aberto, selecciona a linha inválida e foca o campo correcto.

## Compatibilidade

- O endpoint existente que converte encomenda em guia permanece inalterado.
- `CreateDeliveryGuideRequest` e os construtores existentes de DTOs são preservados.
- A migration torna `delivery_guides.order_id` anulável sem eliminar ou transformar dados.

## Testes obrigatórios

- Criação avulsa não movimenta stock.
- Tenant inválido em cliente, armazém ou produto é recusado.
- Quantidade nula, zero ou negativa é recusada.
- Aprovação movimenta stock uma única vez e usa FEFO.
- Rejeição e cancelamento não movimentam stock nem procuram encomenda nula.
- Contrato e fronteiras Maven passam em `MultiModuleArchitectureHarnessTest`.
- O formulário e `MainFrame` instanciam em `DesktopThinContextTest`.
