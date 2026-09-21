# SPEC-ICF-001: Módulo de Inventário & Contagem Física de Stock

## 1. Visão Geral e Objectivos de Negócio

O módulo de **Inventário & Contagem Física de Stock** do Multicore ERP assegura o controlo rigoroso, a auditoria periódica e a reconciliação entre o stock lógico (registado no sistema) e o stock físico existente no armazém ou loja.

### Requisitos Principais:
1. **Sessões de Inventário Auditáveis (`InventorySession`):** Abertura de inventários com numeração própria (`INV-YYYY/N`), descrição, filtro opcional de categoria e estado auditável (`DRAFT`, `IN_PROGRESS`, `CLOSED`, `CANCELLED`).
2. **Contagem Cega (Blind Counting):** Configuração que esconde a quantidade esperada dos operadores de contagem no desktop até ao momento do fecho, prevenindo manipulações e contagens viciadas.
3. **Leitura Rápida por Código de Barras / SKU / Pesquisa:** Adição e atualização instantânea de contagem por scanner de código de barras ou digitação.
4. **Cálculo da Variação e Impacto Financeiro:**
   - $\text{Diferença} = \text{Quantidade Contada} - \text{Quantidade Esperada}$
   - $\text{Valor Sobra} = \sum (\text{Diferença Positiva} \times \text{Preço de Custo})$
   - $\text{Valor Falta} = \sum (|\text{Diferença Negativa}| \times \text{Preço de Custo})$
   - $\text{Variação Líquida} = \text{Valor Sobra} - \text{Valor Falta}$
5. **Reconciliação Automática de Stock ao Fechar:** No fecho da sessão pelo gestor, o sistema gera automaticamente os movimentos de ajuste no stock lógico (`StockMovement`), igualando o stock lógico ao stock contado.
6. **Dossiê Oficial em PDF A4:** Emissão de relatório impresso assinado com discriminação de itens, divergências e valores financeiros.

---

## 2. Diagrama de Estados do Inventário

```
[ DRAFT ] ──( Iniciar Contagem )──> [ IN_PROGRESS ] ──( Concluir & Ajustar Stock )──> [ CLOSED ]
    │                                     │
    └───( Cancelar )──────────────────────┴──( Cancelar )─────────────────────────> [ CANCELLED ]
```

---

## 3. Modelo Contratual e DTOs

- `InventoryStatus`: Enum (`DRAFT`, `IN_PROGRESS`, `CLOSED`, `CANCELLED`).
- `InventoryItemDTO`: Detalhe de cada produto na contagem física.
- `InventorySessionDTO`: Cabeçalho da sessão de inventário com valores agregados de sobras e faltas.
- `CreateInventorySessionRequest`: Requisição para abrir inventário.
- `UpdateInventoryItemCountRequest`: Registo de quantidade contada ou incremento via código de barras.
