# Especificação Técnica de Padronização — Fase 5: Documentos Comerciais Operacionais

## 1. Contexto e Diagnóstico

Os documentos comerciais operacionais do Multicore ERP são a espinha dorsal das vendas orçamentadas, liquidação e logística de entregas:
- **Cotações** (`QuotationsPanel.java`): Propostas comerciais enviadas a clientes com conversão em encomenda/fatura.
- **Recibos de Liquidação** (`ReceiptsPanel.java`): Prova de recebimento de pagamentos com vínculo à fatura e conta de tesouraria.
- **Guias de Remessa** (`DeliveryGuidesPanel.java`): Documentos de expedição e transporte de mercadoria com baixa de stock.
- **Faturação & Pedidos** (`CommercialInvoicesView.java` e `CommercialOrdersView.java`): Central de faturas e pedidos de clientes.

### Diagnóstico de Desvios
1. **Ausência de Calibração `ensureHeadersFit`:** As tabelas de cotações, recibos, guias, faturas e encomendas não garantiam a calibração automática de largura de colunas, causando truncagem de títulos e valores monetários.
2. **Ausência de Gaveta «Quick Peek» Silencioso:** O utilizador precisava abrir diálogos ou formulários completos para verificar dados rápidos de cotações, recibos e guias de transporte.
3. **Uniformidade de Filtros:** Migração de `UIHelper.filterBar(...)` para o canónico `TableFilter.bar(...)` com botão toggle de drawer de inspeção integrado.

---

## 2. Requisitos Canónicos da Fase 5

1. **Card Único Canónico:** Toda tabela reside num `ModernPanel(16)` padronizado via `UIHelper.tableCardTop(...)`.
2. **Barra de Filtros Padronizada:** Uso exclusivo de `TableFilter.bar(...)` em linha única horizontal compacta com campo de pesquisa, seletores e botão de gaveta.
3. **Calibração de Colunas:** Invocação de `UIHelper.ensureHeadersFit(table)` na montagem e no carregamento de dados.
4. **Quick Peek Silencioso:** Instalação de `TableQuickPeekController` na tecla `Espaço` com drawer lateral (`QuickPeekPanel` de 340px) exibindo metadados chave:
   - Cotação: ID, Cliente, NUIT, Total, Validade, Estado.
   - Recibo: Nº Recibo, Fatura, Cliente, Montante Pago, Método, Data.
   - Guia: Nº Guia, Encomenda, Cliente, Armazém, Motorista, Veículo, Total, Estado.
5. **Preservação de Integridade:** Testes `CommercialOperacionalUnifiedHarnessTest`, `DesktopThinContextTest` e `UiPanelDecompositionTest` 100% verdes.

---

## 3. Matriz de Ficheiros

| Ecrã | Ficheiro | Ações |
| :--- | :--- | :--- |
| **Cotações** | `commercial/QuotationsPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (com atalho para editar). |
| **Recibos** | `commercial/ReceiptsPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (detalhes da liquidação). |
| **Guias** | `commercial/DeliveryGuidesPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (detalhes de transporte). |
| **Faturas** | `CommercialInvoicesView.java` | Calibração `ensureHeadersFit` em `owner.invoicesTable`. |
| **Pedidos** | `CommercialOrdersView.java` | Calibração `ensureHeadersFit` em `owner.ordersTable`. |
