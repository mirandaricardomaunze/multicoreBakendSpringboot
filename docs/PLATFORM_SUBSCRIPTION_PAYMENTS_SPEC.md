# SPEC-PSP-001: Pagamentos de Plataforma e Ativação de Planos (Self-Service & Manual)

> **Documento Canónico de Especificação Técnica**  
> **Módulo:** Subscrições / Plataforma SaaS / Pagamento Móvel & Manual  
> **Data:** 2026-09-25  
> **Status:** Aprovado para Implementação

---

## 1. Visão Geral e Contexto de Negócio

No modelo de negócio SaaS do Multicore ERP, as empresas utilizadoras utilizam planos de subscrição (`TRIAL`, `BASIC`, `PRO`, `ENTERPRISE`) com renovação periódica mensal ou anual.

A presente especificação implementa a **ativação e renovação de planos em modelo híbrido**:
1. **Self-Service pelo Assinante (Pagamento Automático por M-Pesa / e-Mola):**
   - Os gestores e administradores da empresa podem ativar ou estender a sua subscrição diretamente a partir da aba "A Minha Assinatura" ou do diálogo de renovação.
   - O sistema dispara um **Push USSD** para o telemóvel do cliente.
   - Ao digitar o PIN e aprovar, a subscrição é ativada/estendida automaticamente na base de dados com registo de pagamento auditado.
2. **Ativação por Comprovativo / Transferência Manual pelo Assinante:**
   - Possibilidade de submeter dados de transferência bancária para validação administrativa.
3. **Ativação Manual Preservada pelo SuperAdmin (Plataforma):**
   - O SuperAdmin mantém controlo total para editar, ativar, suspender, estender datas ou registar pagamentos manuais para qualquer empresa a qualquer momento via `/api/platform/subscriptions`.

---

## 2. Planos Padrão e Precificação (`PlanType`)

| Plano | Preço Mensal | Descrição |
| :--- | :---: | :--- |
| **Básico (BASIC)** | **1.500,00 MT** | Ideal para pequenos retalhistas: 1 armazém, faturação, caixa POS e relatórios essenciais. |
| **Profissional (PRO)** | **3.500,00 MT** | Médias empresas: multi-armazém, multi-caixa POS com balança, compras e controlo de tesouraria. |
| **Empresarial (ENTERPRISE)** | **7.500,00 MT** | Operações completas: todos os módulos, RH & Salários (IRPS/INSS), contabilidade PGC-NIRF e auditoria. |

Descontos de período:
- **1 Mês:** Valor normal.
- **3 Meses:** 5% de desconto.
- **6 Meses:** 10% de desconto.
- **12 Meses (Anual):** 15% de desconto.

---

## 3. Contratos e DTOs (`contracts`)

- `SubscriptionPlanDetailDTO`:
  - `plan` (`String`)
  - `label` (`String`)
  - `monthlyPrice` (`BigDecimal`)
  - `description` (`String`)
- `SelfServiceSubscriptionPaymentRequest`:
  - `plan` (`String`)
  - `months` (`int`)
  - `paymentMethod` (`String`: `MPESA`, `EMOLA`, `BANK_TRANSFER`, `MANUAL`)
  - `phoneNumber` (`String`)
  - `reference` (`String`)
  - `notes` (`String`)
- `SubscriptionPaymentResultDTO`:
  - `success` (`boolean`)
  - `message` (`String`)
  - `transactionId` (`String`)
  - `subscription` (`MySubscriptionDTO`)

---

## 4. Endpoints REST (`backend`)

- **Espaço do Assinante (`/api/subscription`):**
  - `GET /plans`: Lista planos disponíveis com preços e detalhes.
  - `POST /renew`: Inicia renovação (inicia Push USSD ou regista pedido manual).
  - `POST /confirm-payment/{transactionId}`: Valida transação móvel aprovada e estende validade.
- **Espaço SuperAdmin (`/api/platform/subscriptions`):**
  - Inalterado e 100% preservado (`saveSubscription`, `recordPayment`, `changeStatus`).

---

## 5. Interface Desktop (`desktop`)

- **ConfigPanel ("A Minha Assinatura"):**
  - Adicionado botão de destaque: `[ Renovar / Activar Plano ]`.
- **Diálogo `SubscriptionRenewalDialog.java`:**
  - Cards visuais de seleção de plano com cálculo dinâmico de preços.
  - Seletor de período (1, 3, 6 ou 12 meses).
  - Comutação entre Pagamento Móvel (M-Pesa / e-Mola) e Transferência Bancária Manual.
  - Integração com `MobilePaymentModal` para aprovação instantânea com PIN no telemóvel.
