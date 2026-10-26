# Especificação Técnica: Decomposição de StockProductActions (Fase 70)

- **Document ID**: SPEC-STOCK-PRODUCT-ACTIONS-001
- **Fase**: 70
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Autor**: Antigravity / DeepMind

---

## 1. Contexto e Diagnóstico

A classe `StockProductActions.java` (735 linhas) acumulava a orquestração e construção de interfaces complexas modais de produto e lote:
1. **Registo de Lote / Entrada FEFO (`createBatchEntryDialog`)**: Cálculo de caixas, embalagens soltas, unidades soltas, validação de datas FEFO e diálogo legado `JOptionPane`.
2. **Criação de Produto no Catálogo (`showCreateProductDialog`)**: Ficha de artigo com 4 áreas (Dados Gerais, Preços e Impostos, Embalagens e Logística, Imagem), validação de regras de negócio e diálogo legado `JOptionPane` para adicionar stock inicial.
3. **Edição de Produto no Catálogo (`showEditProductDialog`)**: Pesquisa e selecção do produto a editar, preenchimento reativo de campos e persistência assíncrona.
4. **Violação das Directrizes de Feedback**: Presença de 2 chamadas a `JOptionPane.showConfirmDialog`.

---

## 2. Arquitetura Alvo e Decomposição

```
                     ┌──────────────────────────────────────┐
                     │       StockProductActions.java       │
                     │  (Orquestrador enxuto, ≤ 120 linhas) │
                     └──────────────────┬───────────────────┘
                                        │
           ┌────────────────────────────┴───────────────────────────┐
           ▼                                                        ▼
┌──────────────────────────────────────┐ ┌──────────────────────────────────────┐
│      StockBatchEntryDialog.java      │ │      StockProductFormDialog.java     │
│   (Registo de Lote, ≤ 190 linhas)    │ │   (Criação e Edição, ≤ 450 linhas)   │
│  - Cálculo de caixas e unidades      │ │  - Ficha de produto corporativa      │
│  - ModernMessageDialog para expirados│ │  - ModernMessageDialog stock inicial │
│  - Validação FEFO e movimento        │ │  - .setOnSaveAsync / Actualizar      │
└──────────────────────────────────────┘ └──────────────────────────────────────┘
```

---

## 3. Limites Físicos e Requisitos Não Negociáveis

1. **Limites de Linhas**:
   - `StockProductActions.java`: $\le 120$ linhas (reduzido de 735 linhas).
   - `StockBatchEntryDialog.java`: $\le 190$ linhas.
   - `StockProductFormDialog.java`: $\le 450$ linhas.
2. **Zero `JOptionPane`**:
   - Eliminação de 100% dos diálogos legados `JOptionPane` em todo o domínio de produtos de stock.
   - Atualização do `ProfessionalFeedbackHarnessTest` com `assertThat(optionPaneCalls(gui.resolve("StockProductActions.java"))).isZero();` e redução do teto global de 62 para 60.
3. **Preservação de Contratos Existentes**:
   - `StockInteractionHarnessTest` exige que `StockProductActions.java` contenha os tokens literais `.setConfirmButton("Actualizar", "fas-save")` e `dialog.setOnSaveAsync`.
   - Textos e mensagens visíveis estritamente em português de Moçambique.
   - Comparação decimal estrita via `compareTo(BigDecimal.ZERO) == 0` ou `.signum() == 0`.
