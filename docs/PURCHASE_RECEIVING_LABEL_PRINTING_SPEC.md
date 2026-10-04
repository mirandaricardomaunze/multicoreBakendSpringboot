# SPEC-PRL-001: Impressão Directa de Etiquetas na Recepção de Mercadorias

## 1. Princípio e Enquadramento

Durante o processo de recepção física e conferência de encomendas a fornecedores (`PurchaseOrderReceivingDialog` e `PurchaseOrdersPanel`), a mercadoria é descarregada e conferida em armazém.
Para garantir rastreabilidade física imediata e organização de prateleiras ou paletes:
- O operador deve poder emitir e imprimir etiquetas com código de barras, SKU, designação e preço para os artigos que estão a dar entrada física.
- A geração das etiquetas deve usar directamente as quantidades conferidas em bom estado (`A Receber (Boas)`), evitando a necessidade de ir manualmente ao catálogo de stock recriar as etiquetas.
- Também deve ser possível reimprimir etiquetas a partir da lista de encomendas (`PurchaseOrdersPanel`) para encomendas recebidas ou parcialmente recebidas.

## 2. Comportamento e Apresentação Canónica

1. **Botão de Acção na Janela de Conferência:**
   - No diálogo modal `PurchaseOrderReceivingDialog`, é disponibilizado o botão de acção secundário `"Etiquetas"` com ícone FontAwesome `fas-barcode`.
   - Ao premir o botão, o sistema lê as linhas conferidas com quantidade a receber (`goodQty > 0`).
   - Converte os registos para objectos de etiqueta (`ProductDTO`) com SKU, código de barras, nome e preço.
   - Abre o diálogo profissional `ShelfLabelsDialog` permitindo a pré-visualização ao vivo e envio para impressora de etiquetas (A4 ou térmica).
   - Se nenhuma quantidade a receber tiver sido preenchida, emite aviso inline/toast sem fechar o diálogo.

2. **Integração no Menu de Gestão de Encomendas:**
   - No menu de acções de encomenda (`PurchaseOrdersPanel`), é disponibilizada a opção `"Imprimir Etiquetas"` com ícone `fas-barcode`.
   - Permite emitir etiquetas para encomendas com mercadorias já recebidas (`PARTIALLY_RECEIVED` ou `RECEIVED`).

3. **Garantia de Semântica e Formatação:**
   - Preços e valores formatados a 2 casas decimais.
   - Códigos de barras baseados no SKU / Barcode do produto.
   - Ícones estritamente vetoriais FontAwesome (proibido emojis ou Unicode).

## 3. Harness e Verificação

- `PurchaseReceivingLabelPrintingHarnessTest` verifica:
  1. Extração fiel dos produtos com quantidade a receber positiva a partir das linhas e modelo de conferência.
  2. Construção correta dos metadados de etiqueta (SKU, código de barras, preço unitário).
  3. Comportamento defensivo quando as quantidades a receber são zero ou nulas.
  4. Disponibilidade do botão de acção e integração no diálogo de recepção.
