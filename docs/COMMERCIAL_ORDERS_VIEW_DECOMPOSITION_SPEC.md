# Especificação Técnica: Decomposição Modular de CommercialOrdersView

- **Document ID**: SPEC-COMMERCIAL-ORDERS-VIEW-001
- **Fase**: 67
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Domínio**: Comercial / Encomendas de Cliente (Orders)

---

## 1. Contexto e Motivação

O ficheiro `CommercialOrdersView.java` acumulava mais de 515 linhas de código, agregando num único método estático `create(ComercialPanel owner)` três responsabilidades visuais distintas:
1. **Cabeçalho e Metadados do Pedido**: Configuração de GridBagLayout com 6 colunas de inputs (Cliente, Comprador Walk-in, Modalidade de Pagamento, Armazém Origem, Tipo de Encomenda, Armazém Destino para reposição interna, dicas contextuais `kindHint`, alerta de risco de crédito e notas de rodapé).
2. **Grelha de Artigos e Rascunho (Draft Card)**: Tabela de linhas com edição em linha, leitor de código de barras F2, pesquisa de produtos e painel de totais.
3. **Listagem Operacional de Encomendas (List Card)**: Tabela com ordenação e paginação, barra de filtros unificada, menu de acções e painel QuickPeek.

Essa densidade dificulta a manutenção e a legibilidade do editor de encomendas.

---

## 2. Decisão Arquitetural

1. **Extração de `CommercialOrderHeaderForm.java`**:
   - Cria o card `ModernPanel(16)` contendo os controlos de cabeçalho do pedido.
   - Encapsula a lógica de visibilidade condicional do armazém de destino para pedidos de reposição interna (`OrderKind.requiresDestinationWarehouse()`).
   - Fornece as dicas contextuais em português de Moçambique através de `kindHint(OrderKind kind)`.
   - Liga os listeners de alteração para marcar o documento como modificado (`owner.markOrderEditorDirty()`).
   - Limite físico: $\le 210$ linhas.

2. **Refatoração de `CommercialOrdersView.java`**:
   - Delega a construção do cartão de cabeçalho para `CommercialOrderHeaderForm.createHeaderCard(owner)`.
   - Mantém as garantias de contrato exigidas pelos testes de harness (`DocumentEditorHost`, `encomendasHost`, atalhos de grelha, menus de acção, sem `JOptionPane`).
   - Reduz a contagem física de 516 para $\le 360$ linhas.

3. **Garantia de Não Regressão**:
   - Todos os contratos dos testes existentes (`OrderEditorHarnessTest`, `PhcInlineDocumentGridHarnessTest`, `StockCommercialUiHarnessTest`, `DocumentTableInteractionHarnessTest`, `TableFilterHarnessTest`) continuam satisfeitos.

---

## 3. Critérios de Aceitação

- [x] Criação de `CommercialOrderHeaderForm.java` no pacote `mz.multicore.erp.gui`.
- [x] `CommercialOrderHeaderForm.java` possui $\le 210$ linhas.
- [x] `CommercialOrdersView.java` possui $\le 360$ linhas (reduzido de 516 linhas).
- [x] Zero uso de `JOptionPane`.
- [x] Todos os testes de harness passam com sucesso.
