# Especificação Técnica Canónica: Extrato de Conta Corrente de Clientes & Fornecedores

**Identificador:** `SPEC-ECC-001`  
**Versão:** `1.0.0`  
**Data:** `2026-09-16`  
**Autor:** `Multicore ERP Architecture Team`  
**Status:** `Aprovado`  

---

## 1. Visão Geral & Enquadramento de Negócio

No contexto empresarial e fiscal moçambicano (PGC-NIRF e regulamentos da Autoridade Tributária), a transparência e rastreabilidade nas relações comerciais com clientes e fornecedores exigem um extrato de conta corrente fidedigno, com saldo progressivo acumulado e suporte à circularização / reconciliação de saldos.

A **Fase 2: Extrato de Conta Corrente de Clientes & Fornecedores** disponibiliza uma visão cronológica detalhada de todos os lançamentos mercantis e de tesouraria, com cálculo automático de saldos acumulados e emissão de documento PDF canónico de reconciliação.

---

## 2. Regras de Negócio e Fórmula do Saldo Progressivo

### 2.1 Conta Corrente de Clientes (Ótica da Empresa)

| Tipo de Documento | Sigla | Efeito na Conta Corrente | Coluna Afetada | Observação / Origem |
|---|---|---|---|---|
| **Factura a Crédito** | `FT` | Aumenta o montante que o cliente deve à empresa | **Débito (+)** | Módulo Comercial / Facturação |
| **Nota de Débito** | `ND` | Encargo adicional (juros, despesas acessórias) | **Débito (+)** | Módulo Comercial |
| **Recibo / Liquidação** | `RC` | Amortização ou quitação de factura pelo cliente | **Crédito (-)** | Módulo Comercial / Tesouraria |
| **Nota de Crédito** | `NC` | Devolução de mercadoria ou abatimento de preço | **Crédito (-)** | Módulo Comercial |

**Fórmula de Saldo Progressivo de Clientes:**
- **Saldo Anterior ($S_0$):** Soma de todos os débitos menos créditos com data estritamente anterior à data de início do período selecionado.
- **Para cada movimento $i$ no período:**
  $$S_i = S_{i-1} + \text{Débito}_i - \text{Crédito}_i$$
- **Convenção de Sinal:**
  - $S_i > 0$: **Saldo Devedor** (o cliente deve à empresa).
  - $S_i = 0$: **Conta Liquidada / Regularizada**.
  - $S_i < 0$: **Saldo Credor** (adiantamento do cliente ou crédito a favor do cliente).

---

### 2.2 Conta Corrente de Fornecedores (Ótica da Dívida a Pagar)

| Tipo de Documento | Sigla | Efeito na Dívida ao Fornecedor | Coluna Afetada | Observação / Origem |
|---|---|---|---|---|
| **Factura de Fornecedor / Compra** | `V/FT` | Aumenta o montante que a empresa deve ao fornecedor | **Crédito (+)** | Módulo de Compras |
| **Nota de Débito de Fornecedor** | `ND` | Encargos faturados pelo fornecedor | **Crédito (+)** | Módulo de Compras |
| **Pagamento / Liquidação** | `PG` | Pagamento emitido pela nossa tesouraria | **Débito (-)** | Módulo de Tesouraria / Compras |
| **Nota de Crédito de Fornecedor** | `NC` | Abatimento ou devolução aceite pelo fornecedor | **Débito (-)** | Módulo de Compras |

**Fórmula de Saldo Progressivo de Fornecedores:**
- **Saldo Anterior a Pagar ($S_0$):** Soma de todas as compras menos pagamentos anteriores à data de início.
- **Para cada movimento $i$ no período:**
  $$S_i = S_{i-1} + \text{Crédito}_i - \text{Débito}_i$$
- **Convenção de Sinal:**
  - $S_i > 0$: **Saldo a Pagar ao Fornecedor**.
  - $S_i = 0$: **Conta Corrente Regularizada**.
  - $S_i < 0$: **Saldo a Favor da Empresa** (pagamentos em adiantamento).

---

## 3. Contratos de Dados (`contracts` Module)

1. **`CustomerStatementDTO`** (Record imutável):
   - `clientId`, `clientName`, `clientTaxId`, `email`, `phone`, `address`
   - `startDate`, `endDate`
   - `openingBalance`, `totalDebits`, `totalCredits`, `closingBalance`, `overdueAmount`
   - `lines`: `List<CustomerStatementLineDTO>`

2. **`CustomerStatementLineDTO`** (Record imutável):
   - `date`: `LocalDate`
   - `documentType`: `String` (`"FT"`, `"RC"`, `"NC"`, `"ND"`)
   - `documentNumber`: `String`
   - `description`: `String`
   - `reference`: `String`
   - `debit`: `BigDecimal`
   - `credit`: `BigDecimal`
   - `runningBalance`: `BigDecimal`

3. **`SupplierStatementDTO`** e **`SupplierStatementLineDTO`** (Records imutáveis com semântica espelhada para contas a pagar).

---

## 4. Endpoints REST da API

- `GET /api/comercial/statements/customer?clientId={id}&startDate={iso}&endDate={iso}`
  - Retorna `CustomerStatementDTO`.
- `GET /api/comercial/statements/customer/pdf?clientId={id}&startDate={iso}&endDate={iso}`
  - Retorna PDF A4 formatado para impressão e circularização.
- `GET /api/purchases/statements/supplier?supplierId={id}&startDate={iso}&endDate={iso}`
  - Retorna `SupplierStatementDTO`.
- `GET /api/purchases/statements/supplier/pdf?supplierId={id}&startDate={iso}&endDate={iso}`
  - Retorna PDF A4 formatado de extrato do fornecedor.

---

## 5. Interface Gráfica Desktop (Swing)

- **`CustomerStatementPanel`**:
  - Incorporado como aba em `ClientesPanel.java`.
  - Seletor de Cliente com filtro rápido.
  - Filtro por período (`TableFilter.periodCombo()` ou intervalo de datas personalizado).
  - Cartões métricos de resumo: Saldo Anterior, Total Débitos, Total Créditos, Saldo Atual e Total Vencido.
  - Tabela zebrada com alinhamento monetário à direita e cor de saldo contextual.
  - Botão de ação rápida: *"Imprimir Extrato & Reconciliação (PDF)"*.
- **`SupplierStatementPanel`**:
  - Incorporado como aba em `ComprasPanel.java`.
  - Estrutura análoga com cálculo de contas a pagar.

---

## 6. Documento Canónico de Reconciliação (PDF)

O gerador de PDF (`CustomerStatementPrintService` e `SupplierStatementPrintService`) inclui:
1. Cabeçalho canónico da empresa emitente (`CompanyHeaderRenderer`) com NUIT e contactos.
2. Bloco da entidade destinatária (`ClientBlockRenderer`).
3. Tabela completa de transações com saldo progressivo.
4. Resumo com totais de débitos, créditos e saldo apurado.
5. **Termo de Circularização & Confirmação de Saldos:**
   - Parágrafo formal solicitando a confirmação do saldo à data de corte, com campos para carimbo e assinatura de ambas as partes.
