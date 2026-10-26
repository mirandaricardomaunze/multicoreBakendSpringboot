# Especificação Técnica de Decomposição: StockTransferEditorForm

## 1. Contexto e Diagnóstico

O formulário `StockTransferEditorForm.java` implementa a edição canónica em grelha interativa de transferências de stock entre armazéns (padrão PHC/documento integral com cabeçalho tabular e grelha de linhas com atalhos F2/F3/F4).
Antes da Fase 90, acumulava **546 linhas** de código, misturando num único ficheiro:
1. Montagem e estilização do formulário de cabeçalho com GridBagLayout (campos de armazém, motorista, matrícula, responsável, viatura e notas).
2. Configuração de colunas, larguras e renderizadores/editores visuais da grelha de linhas (`linesTable`).
3. Coordenação de leitura ótica por leitor de códigos de barras USB/Bluetooth (F2) e abertura/inserção múltipla do catálogo de produtos (F3).
4. Orquestração de ciclo de vida do documento (preparação, carregamento, validações semânticas e conversão em DTOs de criação/atualização).

## 2. Objectivos da Refacção (Fase 90)

- Reduzir `StockTransferEditorForm.java` para $\le 240$ linhas (redução de mais de 55%).
- Extrair `StockTransferEditorHeaderSection.java` ($\le 85$ linhas) para montagem e layout do cabeçalho tabular de transporte.
- Extrair `StockTransferEditorGridConfigurator.java` ($\le 65$ linhas) para definição de renderizadores, editores e larguras de colunas da grelha de linhas.
- Extrair `StockTransferCatalogCoordinator.java` ($\le 95$ linhas) para integração com leitor de código de barras e seleção múltipla do catálogo.
- Preservar rigorosamente 100% dos contratos, DTOs e asserções dos harnesses `StockTransferDraftUiHarnessTest`, `StockInTransitUiHarnessTest`, `PhcDocumentEditorHomologationHarnessTest`, `DocumentTableInteractionHarnessTest` e `StockTransferEditorFormDecompositionHarnessTest`.
- Garantir zero `JOptionPane` em todos os ficheiros.

## 3. Arquitectura dos Componentes Decompostos

```
mz.multicore.erp.gui
 ├── StockTransferEditorForm.java             (Orquestrador magro <= 240 linhas)
 ├── StockTransferEditorHeaderSection.java    (Layout do card de cabeçalho <= 85 linhas)
 ├── StockTransferEditorGridConfigurator.java (Configuração de colunas e renderizadores <= 65 linhas)
 ├── StockTransferCatalogCoordinator.java     (Lookup de catálogo e leitura ótica F2/F3 <= 95 linhas)
 ├── StockTransferEditorRowCalculator.java    (Cálculo e sincronização Qtd/Emb/Cx <= 120 linhas)
 └── StockTransferReplenishmentHelper.java    (Sugestão de reposição <= 130 linhas)
```

## 4. Garantia de Conformidade

- **Limites Físicos:** Todos os ficheiros respeitam os tetos estabelecidos.
- **Zero Regressões:** Testes de integração, espelhamento de UI e arquitetura Maven passam integralmente.
