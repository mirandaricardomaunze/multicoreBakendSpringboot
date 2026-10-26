# Especificação Técnica: Decomposição Modular de StockTransferActions

- **Document ID**: SPEC-STOCK-TRANSFER-ACTIONS-001
- **Fase**: 68
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Domínio**: Stock & Armazéns / Transferências e Guias de Transporte

---

## 1. Contexto e Motivação

O ficheiro `StockTransferActions.java` acumulava **935 linhas de código**, concentrando num único controlador:
1. **Orquestração de Ações e Ciclo de Vida da Transferência**: Abertura do editor em rascunho (`openCreateEditor`), edição/consulta (`openSelectedEditor`), submissão para armazém (`submitSelectedTransfer`), emissão de guia de separação (`printPickingSheetForSelected`), conclusão de separação (`completeSeparationForSelected`), cancelamento (`cancelSelectedTransfer`) e registo de encomenda de reposição (`recordOrderForSelectedTransfer`).
2. **Diálogo Modal Extenso de Consulta de Linhas (`displayTransferLinesModal`)**: 160 linhas dedicadas à criação de um modal complexo com grelha de 9 colunas canónicas (Referência, Cód. Barras, Produto, Qtd, Embalagem, Caixa, % Caixa, Preço Unitário, IVA) e resumo de viatura/motorista.
3. **Diálogo Modal Legado de Criação de Transferência (`createTransferDialog`)**: Mais de 420 linhas construindo um formulário modal completo com cálculo de carga, FEFO e linhas de documento.
4. **Resíduos de Diálogos Legados `JOptionPane`**: Chamadas a `JOptionPane.showConfirmDialog` nos métodos `completeSeparationForSelected`, `cancelSelectedTransfer` e `recordOrderForSelectedTransfer`.

---

## 2. Decisão Arquitetural

1. **Eliminação de 100% de `JOptionPane`**:
   - Substituição de todas as chamadas a `JOptionPane.showConfirmDialog` por `ModernMessageDialog.confirm(...)` com estilo corporativo, ícone semântico e feedback em português de Moçambique.

2. **Extração de `StockTransferLinesDialog.java`**:
   - Encapsula o diálogo modal de consulta detalhada das linhas de transferência (`displayTransferLinesModal`), exibindo as 9 colunas canónicas e o cartão de dados de transporte (`originWarehouseName`, `destinationWarehouseName`, `driverName`, `vehiclePlate`, `transferDate`).
   - Limite físico: $\le 190$ linhas.

3. **Extração de `StockTransferCreateModalDialog.java`**:
   - Encapsula o diálogo modal completo de criação de transferências com seleção de armazéns, viatura, motorista, cálculo de carga e grelha de produtos.
   - Limite físico: $\le 450$ linhas.

4. **Refatoração de `StockTransferActions.java`**:
   - Mantém as ações de coordenação e delega para os novos componentes modais especializados.
   - Reduz a contagem física de 935 para $\le 360$ linhas (**redução de mais de 61%**).
   - Preserva estritamente todas as chamadas e contratos de integração esperados pelos testes de harness (`StockTransferReceiptDiscrepancyUiHarnessTest`, `StockInTransitUiHarnessTest`, `TableActionPlacementHarnessTest`).

---

## 3. Critérios de Aceitação

- [x] Zero chamadas a `JOptionPane` em `StockTransferActions.java` e componentes extraídos.
- [x] Criação de `StockTransferLinesDialog.java` com $\le 190$ linhas.
- [x] Criação de `StockTransferCreateModalDialog.java` com $\le 450$ linhas.
- [x] `StockTransferActions.java` reduzido para $\le 360$ linhas.
- [x] Testes existentes e novos passam com 100% de sucesso.
