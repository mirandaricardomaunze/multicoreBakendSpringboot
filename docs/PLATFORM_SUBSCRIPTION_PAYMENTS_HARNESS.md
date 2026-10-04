# HARNESS-PSP-001: Matriz de Testes para Pagamentos e Ativação de Planos

> **Documento Canónico de Testes e Validação**  
> **Referência Técnica:** `docs/PLATFORM_SUBSCRIPTION_PAYMENTS_SPEC.md` (`SPEC-PSP-001`)  
> **Data:** 2026-09-25

---

## Matriz de Cobertura de Testes

| ID | Cenário | Entrada | Resultado Esperado | Camada |
| :--- | :--- | :--- | :--- | :--- |
| **PSP-01** | Consulta de planos disponíveis da plataforma | `GET /api/subscription/plans` | Retorna lista de planos com nomes, preços mensais e descrições. | Backend / Service |
| **PSP-02** | Cálculo determinístico de valor de renovação com desconto | Plano `PRO` (3500 MT) por 12 meses (15% desc.) | Valor total = 35.700,00 MT (ao invés de 42.000 MT). | Backend / Logic |
| **PSP-03** | Iniciação de renovação por M-Pesa com Push USSD | Pedido de 1 mês `BASIC` com número `841234567` | Inicia transação móvel no `MobilePaymentService` e retorna `transactionId`. | Backend / Integration |
| **PSP-04** | Confirmação de pagamento e extensão da validade | Confirmação de transação `SUCCESS` | Assinatura passa a `ACTIVE`, `validUntil` avança N meses e regista `SubscriptionPayment`. | Backend / Service |
| **PSP-05** | Registo de pagamento manual por transferência bancária | Método `BANK_TRANSFER` com referência de comprovativo | Regista pagamento na BD, estende período e audita a operação. | Backend / Service |
| **PSP-06** | Preservação estrita da ativação manual pelo SuperAdmin | Chamada a `saveSubscription` ou `recordPayment` na rota `/api/platform` | Operação do SuperAdmin continua a funcionar sem impedimentos. | Backend / Security |
| **PSP-07** | Abertura do diálogo de renovação na UI Desktop | Clique no botão `[ Renovar / Activar Plano ]` | Abre `SubscriptionRenewalDialog` com cálculo em tempo real. | Desktop / UI |
| **PSP-08** | Atualização reativa de estado após renovação | Pagamento móvel concluído com sucesso | Atualiza imediatamente o card da assinatura e exibe toast de sucesso. | Desktop / UI |
