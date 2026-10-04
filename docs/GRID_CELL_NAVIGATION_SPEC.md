# SPEC-GCN-001: Navegação Fluida por Teclado nas Grelhas de Documentos (Estilo Excel / PHC)

## 1. Princípio e Enquadramento Ergonómico

Em ambientes de balcão e armazém, a velocidade de introdução de itens é um fator crítico. Os operadores utilizam preferencialmente o teclado numérico e teclas de navegação, dispensando o rato durante a digitação contínua de linhas de documentos (Facturas, Encomendas de Cliente, Cotações, Transferências de Stock e Encomendas a Fornecedor).

## 2. Regras de Transição e Atalhos Canónicos

A navegação de células deve obedecer às seguintes regras integradas na grelha:

1. **Confirmação e Avanço (`Enter` ou `Tab`):**
   - Se uma célula estiver em edição, o editor é confirmado (`stopCellEditing`).
   - O foco salta para a próxima coluna **editável** da mesma linha.
2. **Criação Automática de Linha no Final:**
   - Se o cursor estiver na última coluna editável da linha e o operador pressionar `Enter` ou `Tab`:
     - Se existir uma linha seguinte, salta para a primeira coluna editável da linha seguinte.
     - Se estiver na última linha da grelha, aciona automaticamente a adição de uma nova linha em branco (`onAddLine.run()`) e foca a primeira coluna editável da nova linha.
3. **Retrocesso (`Shift+Tab`):**
   - Confirma a célula actual e recua para a coluna editável anterior da mesma linha ou da linha acima.
4. **Cancelamento (`Esc`):**
   - Cancela a edição da célula (`cancelCellEditing`) e restaura o valor prévio sem perder a linha.

## 3. Implementação Centralizada

A lógica é centralizada em `UIHelper.installDocumentGridShortcuts` e `UIHelper.installCellNavigationKeys`, aplicando-se universalmente a todas as grelhas documentais do ERP.

## 4. Harness e Verificação

- `GridCellNavigationHarnessTest` verifica:
  1. Instalação das acções de teclado na grelha.
  2. Determinação correcta da próxima coluna editável.
  3. Disparo da acção de adição de linha ao atingir o final da tabela.
