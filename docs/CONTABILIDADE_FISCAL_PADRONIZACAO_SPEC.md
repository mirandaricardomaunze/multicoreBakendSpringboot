# Especificação Técnica de Padronização — Fase 7: Contabilidade, Fiscal & Auditoria Forense

## 1. Contexto e Diagnóstico

Os módulos fiscais, contabilísticos e de auditoria forense do Multicore ERP são os pilares de conformidade regulatória e transparência financeira da empresa:
- **Área Fiscal:** Apuramento de IVA moçambicano, tabela de taxas fiscais, retenções na fonte (IRPS/IRPC) e mapa salarial.
- **Central de Auditoria Forense:** Deteção proativa de cancelamentos fraudulentos, desvios de stock, descontos anormais e quebras.
- **Contabilidade:** Plano de Contas PGC-NIRF, Diário de Lançamentos, Balancete de Verificação, Razão Geral e Demonstrações Financeiras.

### Diagnóstico de Desvios
1. **Inconsistência de Raio em Cartões (`ModernPanel(14)`):** `FiscalPanel.wrapTable` utilizava raio `14` em vez do padrão canónico obrigatório `ModernPanel(16)`.
2. **Ausência de Calibração `ensureHeadersFit`:** As tabelas fiscais (taxas e retenções), auditoria forense e tabelas contabilísticas corriam risco de truncagem de cabeçalhos operacionais.
3. **Ausência de Gaveta «Quick Peek» Silencioso:** Consultas rápidas de taxas, retenções e eventos forenses exigiam duplo clique ou navegação detalhada.

---

## 2. Requisitos Canónicos da Fase 7

1. **Card Canónico:** Todos os cartões de tabelas devem utilizar estritamente `ModernPanel(16)` (corrigindo `ModernPanel(14)` em `FiscalPanel`).
2. **Calibração de Colunas:** Aplicação obrigatória de `UIHelper.ensureHeadersFit(table)` nas tabelas de taxas fiscais, retenções, auditoria forense e tabelas contabilísticas.
3. **Quick Peek Silencioso com Tecla `Espaço`:**
   - **Fiscal (Taxas Fiscais):** Código, Designação, Tipo, Taxa (%), Base Legal, Estado.
   - **Fiscal (Retenções):** Beneficiário, NUIT, Categoria, Base Tributável, Taxa, Valor Retido, Estado.
   - **Auditoria Forense:** Categoria, Referência, Operador, Severidade, Impacto Financeiro, Recomendação.
4. **Preservação de Integridade:** Testes `AccountingFiscalUnifiedHarnessTest`, `DesktopThinContextTest` e `UiPanelDecompositionTest` 100% verdes.

---

## 3. Matriz de Ficheiros

| Painel | Ficheiro | Ações Canónicas |
| :--- | :--- | :--- |
| **Área Fiscal** | `FiscalPanel.java` | Corrigir `ModernPanel(14)` -> `16`; `ensureHeadersFit` e Quick Peek em `taxRatesTable` e `withholdingsTable`. |
| **Auditoria Forense** | `ForensicAuditPanel.java` | `ensureHeadersFit` e Quick Peek em `table`. |
| **Contabilidade** | `accounting/AccountingPanel.java` | `ensureHeadersFit` centralizado em `styledTable`. |
