# Editor Unificado de Encomendas

**ID:** SPEC-ORDER-EDITOR-001  
**Estado:** obrigatório  
**Domínio:** Comercial

## Objectivo

Criar e actualizar encomendas no mesmo editor de página inteira. A listagem principal nunca é
transformada numa grelha de edição e não é aberto um segundo formulário modal. Ao escolher
`Nova encomenda` ou `Editar encomenda`, a aba alterna entre a lista e o editor através de
`CardLayout`.

## Modos

| Modo | Entrada | Comportamento |
|---|---|---|
| Criar | `Novo Pedido de Cliente` | Editor vazio; o tipo da encomenda pode ser escolhido. |
| Editar | `Mais acções > Editar encomenda` | Carrega cabeçalho e todas as linhas; número e tipo ficam imutáveis. |
| Consultar | encomenda num estado fechado | Não altera dados; encaminha para os detalhes do documento. |

## Estrutura visual

1. Barra fixa no topo: `Voltar à lista`, título contextual e `Guardar alterações`.
2. Dados gerais: cliente, armazém, comprador, tipo e destino quando aplicável.
3. Card da tabela de itens com pesquisa tipada na célula e composição
   `Caixa -> Embalagem -> Unidade`, desconto, FEFO e série.
4. Acções `Adicionar linha` e `Remover item` no topo do card; a actualização ocorre nas células.
5. Rodapé da tabela reservado aos totais e à carga estimada.

Não existe formulário externo de linha. Produto, quantidade, embalagem, caixa, desconto e série
são alterados directamente na posição da grelha; a remoção exige uma linha seleccionada. Qualquer
erro mantém o editor aberto.

## Regras de negócio

- O número e o tipo da encomenda são imutáveis depois da criação.
- São editáveis apenas `PENDING_APPROVAL`, `PENDING` e `AWAITING_SEPARATION`.
- `IN_SEPARATION`, `SEPARATED`, `INVOICED`, `BILLED`, `GUIDED`, `CANCELLED` e `TRANSFERRED`
  são somente consulta.
- Uma encomenda formal alterada volta a `PENDING_APPROVAL`; o pedido de aprovação pendente anterior
  é fechado e uma nova aprovação é criada com o total actualizado.
- Um pedido em `AWAITING_SEPARATION` pode ser alterado antes da impressão da lista. O backend
  recalcula a reserva por produto e armazém atomicamente, sem contar duas vezes a reserva antiga.
- Cliente, armazéns e produtos pertencem obrigatoriamente à empresa activa.
- A encomenda deve manter pelo menos uma linha, com quantidade positiva e desconto entre 0 e 100.
- Preço e IVA são sempre novamente obtidos do cadastro pelo backend.
- A gravação usa a versão recebida no `OrderDTO`. Se outro utilizador já alterou a encomenda, a
  actualização é recusada com mensagem clara e o operador deve recarregar.
- A alteração é transaccional e gera evento de auditoria `ORDER_UPDATE`.

## Contrato HTTP

`PUT /api/comercial/orders/{id}` recebe `UpdateOrderRequest` e devolve `OrderDTO`.

O pedido contém `version`, cliente/comprador, armazém, destino e linhas. A versão é obrigatória.
`OrderDTO.version` devolve a versão vigente. Construtores antigos do record continuam disponíveis.

## Critérios de aceitação

- Criar e editar reutilizam exactamente a mesma instância visual do editor.
- Editar carrega todas as linhas e permite actualizar/remover sem abrir modal.
- Guardar uma edição chama `PUT`, actualiza a lista e regressa à encomenda seleccionada.
- Sair com alterações por gravar pede confirmação.
- Estados fechados nunca são alterados pelo endpoint, mesmo que a UI seja contornada.
- `OrderEditorHarnessTest`, `ComercialServiceTest`, `MultiModuleArchitectureHarnessTest` e
  `DesktopThinContextTest` passam.
