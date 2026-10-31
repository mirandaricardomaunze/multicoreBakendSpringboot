# Especificação de Decomposição: SubscriptionRenewalDialog (Fase 201)

## Objectivo
Decompor o modal de ativação/renovação de subscrição `SubscriptionRenewalDialog.java` (388 linhas) numa fachada enxuta que delega o cálculo de tarifas/descontos a `SubscriptionRenewalPricingCalculator` e a montagem dos painéis visuais a `SubscriptionRenewalViewBuilder`.

## Colaboradores Extraídos
1. `SubscriptionRenewalPricingCalculator.java`: Cálculo de tarifa mensal, desconto progressivo (3, 6, 12 meses), chave do plano e parsing de montantes.
2. `SubscriptionRenewalViewBuilder.java`: Construção do cabeçalho, cartão de total, seletores de meio de pagamento, cartão móvel USSD, cartão bancário e rodapé.

## Invariantes Preservadas
- Métodos estáticos `show(...)` e fluxo de pagamento móvel/bancário permanecem 100% compatíveis.
