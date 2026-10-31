# Especificação de Decomposição: POSService (Fase 193)

## Objetivo
Decompor o monolito `POSService.java` (1.294 linhas e 21 dependências injetadas) numa fachada enxuta (`<= 215` linhas) que preserva os locks pessimistas de abertura/fecho de caixa (`openSession` e `closeSession`) e delega para quatro colaboradores coesos por subdomínio de Ponto de Venda (POS).

## Arquitetura Alvo
1. `POSService.java` (`<= 215` linhas) — Fachada `@Service` preservando `openSession`, `closeSession`, todos os métodos públicos e os 3 construtores legados (19, 20 e 21 argumentos).
2. `PosSessionAndShiftService.java` (`<= 210` linhas) — Sessões ativas, movimentos de caixa (sangria/suprimento), cálculo de saldo esperado, passagem de turno (`performShiftHandover`) e conversões DTO.
3. `PosZReportCalculator.java` (`<= 145` linhas) — Apuramento do Relatório Z (`buildZReport`) e histórico de sessões (`getSessionsHistory`).
4. `PosCheckoutAndPaymentService.java` (`<= 295` linhas) — Emissão de Venda POS (`checkout`) com idempotência offline, abate de stock, conversão de cotação, pagamentos múltiplos e pagamento posterior (`registerLatePayment`).
5. `PosReturnAndVoucherService.java` (`<= 245` linhas) — Devoluções no POS (`returnSale`), reembolsos por numerário/banco e emissão/resgate de vales de compras (`StoreVoucher`).
