# HARNESS-COTACAO-CONVERSAO-001 — Test Harness para Conversão de Cotações

## 1. Objectivo

Garantir que a conversão direta de cotações em faturas comerciais e em vendas no POS cumpre rigorosamente as regras de negócio de precificação, integridade de stock, controlo de validade, isolamento multi-empresa e UX no Swing sem quebrar limites de arquitetura física ou tamanho de classes.

---

## 2. Casos de Teste Automatizados

### Backend (`QuotationConversionHarnessTest.java`)
- **CONV-01: Conversão direta de cotação em fatura com preços acordados:**
  - Cria cotação com preço diferente do catálogo.
  - Converte para fatura via `convertToInvoice`.
  - Verifica que a fatura nasce com número `FT-XXXX`, estado `APPROVED`, baixa de stock no armazém e linhas com o preço cotado.
  - Verifica que a cotação passa para `CONVERTED`, com `invoiceId` e `invoiceNumber` preenchidos.
- **CONV-02: Bloqueio de conversão em fatura de cotação expirada:**
  - Cria cotação com `validUntil` no passado.
  - Invoca `convertToInvoice` -> lança `BusinessRuleException` exigindo estender a validade.
- **CONV-03: Bloqueio de dupla conversão:**
  - Cotação já convertida não pode ser convertida novamente.
- **CONV-04: Consulta de cotações abertas (`findOpenByCompany`):**
  - Retorna apenas cotações `DRAFT`, `SENT`, `ACCEPTED` e vigentes.
- **CONV-05: Checkout no POS associando cotação de origem:**
  - Cria venda no POS passando `quotationId` e linhas com `unitPrice` da cotação.
  - Verifica que a fatura é emitida com os preços da proposta e a cotação é marcada como `CONVERTED` com `invoiceId`.

### Desktop UI (`QuotationConversionUiHarnessTest.java`)
- **UI-CONV-01: Ação de conversão em fatura em `QuotationsPanel`:**
  - Verifica presença da ação "Converter em Factura" com ícone vetorial FontAwesome sem emojis.
- **UI-CONV-02: Diálogo `PosImportQuotationDialog`:**
  - Componente modal instanciável sem erros, com tabela de cotações e filtro de pesquisa.
- **UI-CONV-03: `PosCartItem` suporta preço unitário cotado personalizado:**
  - Verifica que o subtotal, imposto e total respeitam o preço cotado quando informado.
- **UI-CONV-04: Limite de linhas em componentes Swing:**
  - `QuotationsPanel.java`, `PosImportQuotationDialog.java` e `POSPanel.java` têm estritamente $< 1000$ linhas.

---

## 3. Critérios de Aprovação

- `QuotationConversionHarnessTest` passa 100%.
- `QuotationConversionUiHarnessTest` passa 100%.
- `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest` passam 100%.
- Compilação limpa sem erros em todo o reactor Maven (`contracts`, `backend`, `desktop`).
