# Especificação de Decomposição: MobilePaymentService (Fase 176)

## Contexto e Problema
`MobilePaymentService` (189 linhas) concentrava a normalização de números de telefone moçambicanos (`+258`), validação de prefixos por operadora (`84/85` M-Pesa, `86/87` e-Mola) e geração de identificadores financeiros juntamente com o processamento de Push USSD, polling de estado com expiração aos 60s e simulação de aprovação/recusa.

## Solução Arquitectural
1. **`MobilePaymentService` (Fachada — `<= 80` linhas)**:
   - Orquestra as chamadas transaccionais delegando para `MobilePaymentTransactionProcessor` e `MobilePaymentPhoneValidator`.
   - Mantém `normalizePhoneNumber`, `validateProviderPhone` e o construtor compatível `MobilePaymentService(MobilePaymentRepository)`.
2. **`MobilePaymentPhoneValidator` (`@Component` — `<= 75` linhas)**:
   - Normaliza números moçambicanos para 9 dígitos, valida prefixos por operadora e gera identificadores `TX-...` e referências financeiras `MP...`/`EM...`.
3. **`MobilePaymentTransactionProcessor` (`@Component` — `<= 125` linhas)**:
   - Gere `initiatePayment`, `checkStatus` e `simulateComplete`.
