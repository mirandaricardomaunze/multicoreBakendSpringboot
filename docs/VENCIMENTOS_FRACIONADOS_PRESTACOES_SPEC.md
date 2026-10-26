# Especificação Funcional & Técnica: Vencimentos Fracionados & Prestações Comerciais (Multi-Installments)

## 1. Visão Geral e Enquadramento de Negócio

No comércio B2B e distribuição em Moçambique, faturas com prazos de crédito alargados são frequentemente acordadas em modalidades parceladas ou fracionadas (ex.: 30/60/90 dias, 50% de entrada + 50% a 30 dias, 3 prestações mensais iguais).

Atualmente, o Multicore ERP possui uma data de vencimento única (`dueDate`) por fatura. O módulo de **Vencimentos Fracionados & Prestações Comerciais** introduz a capacidade de estruturar o valor total da fatura num **Plano de Prestações (Installment Plan)** com vencimentos escalonados:
1. Divide o valor total da fatura em $N$ prestações com datas e montantes definidos.
2. Cada prestação mantém o seu próprio ciclo de liquidação (`PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`).
3. Liquidações e recibos abatem sequencialmente nas prestações mais antigas ou diretamente numa parcela selecionada.
4. O documento oficial PDF da fatura (`InvoicePrintService`) inclui o **Plano de Vencimentos** discriminado.
5. O painel financeiro e comercial permite visualizar e gerir o estado de cada prestação em aberto.

---

## 2. Invariantes e Regras de Negócio

1. **Consistência Matemática do Plano:**
   - A soma de todas as prestações de uma fatura deve ser rigorosamente igual ao `totalAmount` da fatura.
   - Qualquer arredondamento de centavos no cálculo fracionário (ex.: $1000.00 / 3 = 333.33$) deve ser absorvido pela última ou primeira parcela para garantir soma exata:
     $$\sum_{i=1}^{N} \text{amount}_i = \text{Invoice.totalAmount}$$

2. **Vencimentos Escalonados:**
   - As datas de vencimento devem ser cronologicamente crescentes:
     $$\text{dueDate}_1 \le \text{dueDate}_2 \le \dots \le \text{dueDate}_N$$

3. **Liquidação Sequencial (FEFO Financeiro):**
   - Quando um pagamento parcial é recebido sem especificação de parcela, o sistema aloca o montante amortizando primeiramente a prestação pendente mais antiga com vencimento mais próximo.

4. **Estado da Prestação:**
   - `PENDING`: Parcela em aberto sem pagamentos, dentro do prazo.
   - `PARTIALLY_PAID`: Parcela parcialmente amortizada.
   - `PAID`: Parcela 100% liquidada.
   - `OVERDUE`: Parcela não liquidada após a sua data de vencimento (`dueDate < LocalDate.now()`).

5. **Retrocompatibilidade:**
   - Faturas emitidas sem plano de prestações explícito continuam a funcionar como prestação única integral (1/1), preservando compatibilidade total com os módulos de tesouraria, recibos e relatórios fiscais.

---

## 3. Modelo de Dados e Contratos (Módulo `contracts`)

### DTOs em `mz.multicore.erp.modules.comercial.dto`:

- `InvoiceInstallmentDTO`:
  - `id` (Long)
  - `invoiceId` (Long)
  - `installmentNumber` (int - ex: 1, 2, 3)
  - `totalInstallments` (int - ex: 3)
  - `dueDate` (LocalDate)
  - `amount` (BigDecimal)
  - `paidAmount` (BigDecimal)
  - `remainingBalance` (BigDecimal)
  - `status` (String - `PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`)
  - `notes` (String)

- `CreateInstallmentPlanRequest`:
  - `invoiceId` (Long)
  - `numberOfInstallments` (int)
  - `intervalDays` (int - ex: 30)
  - `firstDueDate` (LocalDate)
  - `customAmounts` (List<BigDecimal> - opcional para parcelas desiguais)

---

## 4. Persistência e Base de Dados (Módulo `backend`)

- Tabela: `invoice_installments` (Migração Flyway `V95__invoice_installments_plan.sql`):
  - `id BIGSERIAL PRIMARY KEY`
  - `invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE`
  - `company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE`
  - `installment_number INT NOT NULL`
  - `total_installments INT NOT NULL`
  - `due_date DATE NOT NULL`
  - `amount NUMERIC(14, 2) NOT NULL`
  - `paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00`
  - `remaining_balance NUMERIC(14, 2) NOT NULL`
  - `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'`
  - `notes VARCHAR(255)`
  - `created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()`
  - `updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()`
  - `CONSTRAINT uk_invoice_installment UNIQUE (invoice_id, installment_number)`

---

## 5. Estratégia de Testes e Harness

- `InvoiceInstallmentServiceTest` (backend): valida criação de planos proporcionais e customizados, absorção de centavos, amortização de pagamentos e estados.
- `InvoiceInstallmentUiHarnessTest` (desktop): valida integridade dos DTOs e formatação da tabela de parcelas.
- `MultiModuleArchitectureHarnessTest`: garante isolamento arquitetural.
- `DesktopThinContextTest`: garante estabilidade do contexto Swing.
