# Especificação de Decomposição Modular: Extrato de Conta Corrente de Clientes (CustomerStatementPanel)

**Identificador:** `SPEC-CUSTOMER-STATEMENT-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / CRM / Conta Corrente & Reconciliação de Clientes  
**Alvo:** `mz.multicore.erp.gui.CustomerStatementPanel.java` (445 linhas)  
**Meta de Redução:** $\le 210$ linhas no orquestrador principal ($\approx 53\%$ de redução global)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `CustomerStatementPanel.java` providencia a visualização e reconciliação da conta corrente de clientes, incluindo o saldo de abertura transportado, débitos (faturas/notas de débito), créditos (recibos/liquidações), saldo progressivo linha a linha, exportação oficial para PDF com pré-visualização e envio seguro por email através do diálogo `SendStatementEmailDialog`.

Actualmente, o painel aglomerava:
1. **Coordenação de Exportação & Despacho:** Geração assíncrona do PDF via `PrintPreviewDialog.show`, controlo do spinner `busyBar`, e orquestração do envio por correio electrónico com feedback semântico.
2. **Inspeção Rápida (Quick Peek) e Formatação:** Mapeamento detalhado dos campos de cada linha de movimento, tratamento de cores de crédito/débito e formatação monetária em padrão moçambicano (`pt-MZ`).
3. **Povoamento e Renderização de Linhas:** Extração do histórico de movimentos para a tabela com saldo progressivo e agrupamento de linhas de negócio.
4. **Orquestração da Interface:** Grelha de 5 KPIs com `KpiCard.createGrid(5)`, card único `ModernPanel(16)` contendo barra de filtros (`TableFilter.bar`) e tabela de 9 colunas com paginação e cabeçalhos ergonómicos.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`StatementsCashFlowUnifiedHarnessTest` (`STMT-01`):**
   - Conter `new ModernPanel(16)` e não conter `new ModernPanel(12)`.
   - Conter `UIHelper.tableCardTop(`.
   - Conter `TableQuickPeekController.install(table`.
   - Conter `UIHelper.ensureHeadersFit(table)`.
2. **`AdminApprovalsUnifiedHarnessTest` (`AA-03`):**
   - Conter `UIHelper.tableCardTop("Conta Corrente & Reconciliação de Clientes"`.
   - Conter `ClientTablePagination.install(table)`.
   - Conter `KpiCard.createGrid(5)`.
3. **`AccountStatementPanelHarnessTest` e `DesktopInitializationTest`:**
   - Preservar o construtor público:
     - `CustomerStatementPanel(AccountStatementApiClient, ComercialApiClient)`
   - Preservar o record estático público:
     - `public record ClientComboItem(Long id, String name, String taxId)`
4. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── CustomerStatementPanel.java                     (Orquestrador principal: <= 210 linhas)
│   ├── buildSummaryCards() [KpiCard.createGrid(5)]
│   ├── buildTableCard() [ModernPanel(16), tableCardTop, TableQuickPeekController, ensureHeadersFit]
│   ├── loadClients(), refreshData(), populateData()
│   └── ClientComboItem
└── CustomerStatementOperationsCoordinator.java      (Coordenador de acções: <= 165 linhas)
    ├── emitStatementPdf(...)
    ├── emitStatementEmail(...)
    ├── populatePeek(...)
    ├── renderLines(...)
    ├── updateSummary(...)
    ├── parseDate(...)
    └── formatMoney(...)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `CustomerStatementPanel.java` | $\le 210$ linhas | Layout, tabela, filtros, KPIs e orquestração de dados |
| **Coordenador de Operações** | `CustomerStatementOperationsCoordinator.java` | $\le 165$ linhas | Emissão PDF, despacho email, renderização de linhas, Quick Peek e parsing |
