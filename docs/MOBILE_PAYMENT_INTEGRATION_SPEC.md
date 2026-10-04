# SPEC-MPI-001: Integração de Pagamento Móvel (M-Pesa & e-Mola via Push USSD)

> **Documento Canónico de Especificação Técnica**  
> **Módulo:** POS / Faturação / Pagamentos Móveis Moçambicanos  
> **Data:** 2026-09-25  
> **Status:** Aprovado para Implementação

---

## 1. Visão Geral e Contexto de Negócio

Em Moçambique, os pagamentos móveis (**M-Pesa da Vodacom** e **e-Mola da Movitel**) representam a esmagadora maioria das transações digitais no retalho e balcão de vendas.  
Anteriormente, o operador de caixa precisava que o cliente fizesse a transferência manual para o número da loja e aguardava o SMS de confirmação, digitando manualmente o código no sistema.

A presente especificação define o **fluxo automático de Push USSD (STK Push / C2B)**:
1. O operador seleciona **M-Pesa** ou **e-Mola** no checkout do POS e insere o número de telemóvel do cliente.
2. O sistema envia uma solicitação de débito diretamente para a rede da operadora móvel.
3. O telemóvel do cliente acende automaticamente com um pop-up de sistema solicitando a autorização:  
   *"Confirmar pagamento de X.XX MT para Multicore ERP? Digite o PIN:"*.
4. O cliente digita o PIN no seu telemóvel.
5. O sistema recebe a confirmação em tempo real via polling ou webhook, toca o sinal sonoro de sucesso, preenche a referência financeira e finaliza a venda sem intervenção manual.

---

## 2. Regras de Validação de Números de Telemóvel em Moçambique

O sistema deve validar e higienizar os números antes do envio:
- Aceita formatos: `84XXXXXXX`, `85XXXXXXX`, `+25884XXXXXXX`, `25884XXXXXXX`, com espaços ou hífens.
- Higienização para 9 dígitos numéricos:
  - **Vodacom M-Pesa:** Prefixos obrigatórios `84` e `85` (9 dígitos).
  - **Movitel e-Mola:** Prefixos obrigatórios `86` e `87` (9 dígitos).
  - **Tmcel mKesh (reserva futura):** Prefixos `82` e `83`.
- Se o operador selecionar "M-Pesa" e introduzir um número Movitel (`86...`), o sistema deve emitir aviso visual imediato de incompatibilidade de operadora.

---

## 3. Estados do Pagamento Móvel (`MobilePaymentStatus`)

- `PENDING`: Solicitação enviada à operadora; aguardando cliente digitar o PIN (timeout de 60 segundos).
- `SUCCESS`: Transação aprovada e liquidada pela operadora; referência financeira gerada (ex.: `MP260925.1055.X921`).
- `FAILED`: Recusada por PIN incorreto, saldo insuficiente ou rejeição da operadora.
- `CANCELLED`: Cancelada explicitamente pelo cliente no visor do telemóvel ou pelo operador no POS.
- `EXPIRED`: Tempo limite de 60 segundos ultrapassado sem resposta do cliente.

---

## 4. Contratos e DTOs (`contracts`)

- `MobilePaymentProvider`: `MPESA`, `EMOLA`.
- `MobilePaymentStatus`: `PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`, `CANCELLED`.
- `InitiateMobilePaymentRequest`:
  - `provider` (`MobilePaymentProvider`)
  - `phoneNumber` (`String`)
  - `amount` (`BigDecimal`)
  - `reference` (`String`, ex.: referência interna do POS)
  - `companyId` (`Long`)
  - `operator` (`String`)
- `MobilePaymentResponse`:
  - `transactionId` (`String`)
  - `provider` (`MobilePaymentProvider`)
  - `phoneNumber` (`String`)
  - `amount` (`BigDecimal`)
  - `reference` (`String`)
  - `financialReference` (`String`)
  - `status` (`MobilePaymentStatus`)
  - `message` (`String`)
  - `createdAt` (`Instant`)
- `MobilePaymentStatusResponse`:
  - `transactionId` (`String`)
  - `status` (`MobilePaymentStatus`)
  - `financialReference` (`String`)
  - `message` (`String`)

---

## 5. Persistência e Backend (`backend`)

- Tabela `mobile_payment_transactions` via migration Flyway `V70__mobile_payment_transactions.sql`:
  - `id` (BIGSERIAL PK)
  - `company_id` (BIGINT NOT NULL, isolamento tenant)
  - `transaction_id` (VARCHAR 64 UNIQUE NOT NULL)
  - `provider` (VARCHAR 20 NOT NULL)
  - `phone_number` (VARCHAR 20 NOT NULL)
  - `amount` (NUMERIC 15,2 NOT NULL)
  - `reference` (VARCHAR 64)
  - `financialReference` (VARCHAR 64)
  - `status` (VARCHAR 20 NOT NULL)
  - `message` (VARCHAR 255)
  - `operator` (VARCHAR 64)
  - `created_at`, `updated_at` (TIMESTAMP)
- **Modo Sandbox / Simulado Inteligente:**
  - Caso não existam chaves de produção da Vodacom/Movitel configuradas, o sistema opera em modo de demonstração/sandbox de alta fidelidade:
    - Simula a latência de rede e tempo de digitação do PIN (2 a 3 segundos).
    - Permite testar números de teste canónicos (sucesso, rejeição, cancelamento).
- **Controlador REST (`/api/pos/mobile-payment`):**
  - `POST /initiate`: Inicia o processo de Push USSD.
  - `GET /{transactionId}/status`: Consulta o estado atual da transação.
  - `POST /{transactionId}/simulate-complete`: Força conclusão em ambiente de teste/demonstração.

---

## 6. Interface do Utilizador Desktop (`desktop`)

- Diálogo `MobilePaymentModal.java` e integração no `PosPaymentDialog.java`:
  - Chips visuais para M-Pesa (Vermelho) e e-Mola (Laranja).
  - Campo de entrada com formatação de telemóvel e verificação dinâmica de operador.
  - Painel de espera com contagem decrescente (60s), animação de pulso e mensagem instrutiva.
  - Feedback sonoro imediato (`PosAudioFeedbackEngine`): tom de sucesso ao aprovar e tom de alerta ao falhar.
  - Preenchimento automático do campo de referência e devolução da transação aprovada para o checkout.
