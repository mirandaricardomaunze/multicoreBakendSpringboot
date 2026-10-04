# Harness — Teste e Validação da Reposição Inteligente Padronizada

> **Status:** Aprovado  
> **Data:** 2026-10-04  
> **Suíte Automatizada:** `PurchaseReorderUnifiedHarnessTest.java`

---

## 1. Objectivos de Teste

1. **REORDER-01: KPIs Canónicos (`KpiCard.createGrid` e 96px de Altura):**
   - Verificar que a grelha de KPIs utiliza `KpiCard.createGrid(4)` com cartões interactivos e paletas semânticas.
2. **REORDER-02: Eliminação de Duplicação e Contenção no Card:**
   - O painel possui um único cabeçalho integrado no topo do card `ModernPanel(16)`.
3. **REORDER-03: Quick Peek Silencioso Instalado:**
   - A tabela `reorderTable` responde à tecla `SPACE` para abrir o drawer lateral com resumo da reposição.
4. **REORDER-04: Drilldown Interactivo nos KPIs:**
   - Clicar em "Produtos Esgotados" filtra para `ESGOTADO`.
   - Clicar em "Rutura Iminente" filtra para `CRÍTICO`.
   - Clicar em "Total a Repor" repõe todos os produtos.
5. **REORDER-05: Ajuste de Larguras de Colunas e Header Fit:**
   - Nenhuma coluna essencial sofre truncamento por falta de dimensionamento preferencial.
