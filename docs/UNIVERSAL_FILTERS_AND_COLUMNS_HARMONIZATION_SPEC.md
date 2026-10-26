# Universal Filters and Columns Harmonization Specification

## 1. Contexto e Diagnóstico
Com a evolução e estabilização de módulos transversais no Multicore ERP (Fase 9 e 10: Unidades de Negócio multi-fornecedor, Fase 8: Rastreabilidade de impressões e Fase 20: Dashboard em abas corporativas), diversos testes de integridade mantinham asserções legadas rígidas que entraram em conflito com o schema e layout atuais:
1. `UniversalPeriodFilterHarnessTest`: O teste asseria que `invoicesTable` possuía exatamente 7 colunas. Com a inclusão das colunas "Negócio" e "Impressão", a tabela agora possui 9 colunas operacionais, mantendo a coluna "Data" no índice 3 e o filtro de período perfeitamente operacional.
2. `StockWastePanelHarnessTest`: O teste asseria 12 colunas em `StockWastePanel`. Com a inclusão da coluna "Negócio", a tabela agora possui 13 colunas operacionais.
3. `EditableDocumentEditorsHarnessTest`: O teste procurava pela asserção de duplo clique em `QuotationsPanel` via `getClickCount() == 2`. A tabela de cotações deve implementar o duplo clique explícito na linha selecionada para abrir o editor/consulta.
4. `OperationalPulseWidgetHarnessTest`: O teste procurava pela adição em `centerContainer.add(operationalPulseWidget);` do layout anterior descontinuado com scroll longo. Com a modernização do `DashboardPanel` em abas corporativas (`JTabbedPane`), o widget reside no contentor da aba de Cockpit Operacional 360° (`tab2Center.add(operationalPulseWidget);`).

## 2. Invariantes
- As tabelas de documentos devem suportar filtros de período universais mantendo a integridade de todas as colunas operacionais (incluindo Linha de Negócio e Impressão).
- O duplo clique em tabelas editáveis deve abrir com segurança o editor ou consulta do documento sem efeitos colaterais.
- O Dashboard corporativo mantém o `OperationalPulseWidget` na aba de Cockpit Operacional sem quebras funcionais.

## 3. Harnesses de Validação
- `UniversalPeriodFilterHarnessTest`
- `StockWastePanelHarnessTest`
- `EditableDocumentEditorsHarnessTest`
- `OperationalPulseWidgetHarnessTest`
