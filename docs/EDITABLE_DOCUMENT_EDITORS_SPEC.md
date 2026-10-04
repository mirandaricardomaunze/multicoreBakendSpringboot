# Editores Unificados de Documentos Pré-Emissão

**ID:** SPEC-EDITABLE-DOCUMENT-EDITORS-001  
**Estado:** obrigatório  
**Domínios:** Comercial e Compras

## Objectivo

Aplicar o editor de página inteira com tabela de linhas aos documentos que ainda podem ser
alterados sem quebrar o rasto fiscal, financeiro ou de stock. Criar e actualizar reutilizam o
mesmo editor através de `CardLayout`; formulários modais deixam de ser usados nestes fluxos.

## Documentos abrangidos

| Documento | Estado editável | Motivo |
|---|---|---|
| Cotação / pró-forma | `DRAFT` | Ainda não foi enviada nem aceite pelo cliente. |
| Encomenda a fornecedor | `ORDERED`, sem qualquer quantidade recebida | Ainda não houve entrada física de stock. |

## Documentos deliberadamente excluídos

- Facturas de venda e de compra já emitidas;
- recibos e movimentos de tesouraria;
- notas de crédito e débito emitidas;
- cotações enviadas, aceites, recusadas, convertidas ou canceladas;
- encomendas a fornecedor parcialmente recebidas, recebidas ou canceladas.

Estes documentos são corrigidos pelas operações próprias do domínio: anulação controlada,
documento rectificativo, extensão de validade, nova revisão ou recepção/cancelamento.

## Experiência de utilização

1. A lista e o editor são páginas do mesmo painel.
2. `Novo` abre o editor vazio; `Editar / Consultar` carrega o documento seleccionado.
3. A tabela de itens fica dentro de `ModernPanel(16)`, com `Adicionar linha` e `Remover item` no topo.
4. Produto, quantidade, embalagem, caixa e valores próprios do documento são editados nas células.
5. Não existe formulário externo nem botão `Editar item` para transportar valores da linha.
6. `Voltar à lista` avisa quando existem alterações por gravar.
7. Em estado bloqueado, o mesmo editor abre em consulta e o botão de guardar fica desactivado.

## Regras da cotação

- Número, data de emissão e estado são imutáveis.
- Apenas `DRAFT` pode ser actualizado.
- Cliente, armazém e produtos devem pertencer à empresa activa.
- A validade é recalculada em dias a partir da data da actualização do rascunho.
- Preço e IVA são novamente obtidos do cadastro pelo backend; desconto fica entre 0 e 100.
- A actualização gera auditoria `QUOTATION_UPDATE`.

## Regras da encomenda a fornecedor

- Número, data e estado são imutáveis.
- Apenas `ORDERED` sem quantidades recebidas pode ser actualizado.
- Fornecedor activo, armazém e produtos devem pertencer à empresa activa.
- Preço e IVA acordados com o fornecedor são recalculados pelo backend.
- Linhas mantêm lote, validade e série enquanto são alteradas.
- A actualização gera auditoria `PURCHASE_ORDER_UPDATE` e nunca move stock.

## Concorrência e contratos

- `QuotationDTO` e `PurchaseOrderDTO` expõem `version`.
- `PUT /api/comercial/quotations/{id}` recebe `UpdateQuotationRequest`.
- `PUT /api/purchases/orders/{id}` recebe `UpdatePurchaseOrderRequest`.
- Uma versão desactualizada é recusada com mensagem clara para recarregar o documento.
- Construtores antigos dos DTOs continuam disponíveis.

## Critérios de aceitação

- Não existe formulário modal para criar/editar os dois documentos abrangidos.
- Cabeçalho e linhas são carregados no mesmo editor usado na criação.
- Actualizar/remover linhas não abre outro formulário.
- Estados bloqueados continuam protegidos no Service mesmo que a UI seja contornada.
- `EditableDocumentEditorsHarnessTest`, testes dos Services,
  `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest` passam.
