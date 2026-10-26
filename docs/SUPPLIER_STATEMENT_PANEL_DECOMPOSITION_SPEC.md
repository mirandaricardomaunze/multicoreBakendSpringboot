# Especificação de Decomposição Modular: Conta Corrente de Fornecedores (SupplierStatementPanel)

**Identificador:** `SPEC-SUPPLIER-STATEMENT-PANEL-DECOMPOSITION-001`  
**Domínio:** Compras / Contas a Pagar / Fornecedores / Conta Corrente  
**Alvo:** `mz.multicore.erp.gui.SupplierStatementPanel.java` (282 linhas)  
**Meta de Redução:** $\le 165$ linhas no orquestrador principal ($\approx 43\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `SupplierStatementPanel.java` é o centro operacional de consulta de Conta Corrente de Fornecedores com Saldo Progressivo e Reconciliação em PDF no Multicore ERP.

Anteriormente, o painel contava com o coordenador `SupplierStatementOperationsCoordinator.java` para emissão documental e Quick Peek. No entanto, o orquestrador retinha ainda 282 linhas contendo:
1. **Povoamento e orquestração de KPIs e unidades de negócio:** Atualização de saldos (anterior, compras, pagamentos e saldo devedor) e combos de unidade de negócio.
2. **Transformação e renderização de linhas de extrato:** `renderLines` com linhas de saldo inicial, tipos documentais, referências e paginação de saldo progressivo.
3. **Chamadas assíncronas concorrentes:** `loadSuppliers` e `refreshData` com workers Swing manuais.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Contratos Canónicos da UI Multicore:**
   - `new ModernPanel(16)` como invólucro único do cartão de dados.
   - Proibição de `new ModernPanel(12)` ou `new ModernPanel(10)`.
   - `UIHelper.tableCardTop("Conta Corrente & Reconciliação de Fornecedores", ...)` como cabeçalho do cartão.
   - Instalação de Quick Peek silencioso com tecla de atalho Espaço via `TableQuickPeekController.install(table, ...)`.
   - Calibração de larguras de colunas com `UIHelper.ensureHeadersFit(table)`.
   - Paginação client-side via `ClientTablePagination.install(table)`.
   - Grid de 4 cartões de KPI uniformes via `KpiCard.createGrid(4)`.
2. **Assinaturas Públicas e Records:**
   - Construtor público `SupplierStatementPanel(AccountStatementApiClient, PurchaseApiClient)`.
   - Record público `SupplierComboItem(Long id, String name, String taxId)`.
3. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── SupplierStatementPanel.java                       (Orquestrador principal: <= 165 linhas)
│   ├── buildSummaryCards(), buildTableCard()
│   ├── loadSuppliers(), refreshData(), emitStatementPdf()
│   └── SupplierComboItem
├── SupplierStatementOperationsCoordinator.java       (Emissão PDF e Quick Peek: <= 110 linhas)
│   ├── emitStatementPdf(...), populatePeek(...)
│   └── parseDate(...), formatMoney(...)
└── SupplierStatementViewCoordinator.java             (Apresentação, KPIs, worker async e linhas: <= 110 linhas)
    ├── populateKpisAndBusinessUnits(...)
    ├── renderLines(...)
    ├── loadSuppliersAsync(...)
    └── loadStatementAsync(...)
```
