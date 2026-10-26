# Especificação de Decomposição Modular — ProductLookupDialog (Fase 84)

## 1. Contexto e Diagnóstico

A classe `ProductLookupDialog.java` acumulava 696 linhas no pacote `gui.components`, misturando num único ficheiro:
1. Contratos públicos e fábrica de diálogo (`show`, `showMulti`, `showRemote`, `showMultiRemote`).
2. Configuração de colunas, larguras e renderizadores de tabela (`COLUMNS`, `TableCellRenderers.money`).
3. Algoritmos de formatação de embalagens multinível (`formatPackaging`), normalização diacrítica e sanitização de texto.
4. Lógica de pesquisa em memória com paginação local (`showInternal`, ~180 linhas).
5. Lógica de paginação remota assíncrona de servidor (`showInternalRemote`, ~290 linhas), incluindo controlo de concorrência (`AtomicLong requestCounter`), timer de debounce, barra de navegação paginada e actualização de status.

## 2. Princípios Arquiteturais e Restrições Não Negociáveis

1. **Separação de Responsabilidades (SOLID / SRP):**
   - `ProductLookupDialog`: Fachada pública de conveniência com os contratos `ProductPageProvider` e `CategoryOption` ($\le 120$ linhas).
   - `ProductLookupTableHelper`: Criação de modelos de tabela, configuração de colunas, normalização e formatação de dados ($\le 110$ linhas).
   - `ProductLookupLocalDialog`: Orquestrador de diálogo para catálogos em memória ($\le 190$ linhas).
   - `ProductLookupRemoteDialog`: Orquestrador de diálogo para paginação assíncrona remota no servidor ($\le 240$ linhas).
2. **Design Corporativo e Ergonómico:**
   - Modais com dimensões canónicas contidas via `UIHelper.applyStandardModalBounds`.
   - Tabelas estilizadas via `UIHelper.styleTable`, filtros com `TableFilter.bar`.
   - Atalhos de teclado canónicos: Enter para submeter, seta para baixo no campo de busca para focar na tabela, e clique duplo para selecionar.
3. **Preservação Total de Contratos e Zero Regressão:**
   - A interface funcional `ProductLookupDialog.ProductPageProvider` e o record `ProductLookupDialog.CategoryOption` mantêm-se exatamente no mesmo ficheiro/assinatura para 100% de retrocompatibilidade com testes e chamadores.
   - Zero `JOptionPane`.
