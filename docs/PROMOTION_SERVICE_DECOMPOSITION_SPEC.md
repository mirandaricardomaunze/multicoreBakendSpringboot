# Especificação de Decomposição: PromotionService (Fase 145)

## 1. Diagnóstico do Monólito
`PromotionService.java` possuía **241 linhas**, misturando:
- Ciclo de vida e consulta de promoções (`findByCompany`, `createPromotion`, `setActive`, validações de escopo e parâmetros por tipo).
- Motor de cálculo de desconto percentual efectivo (`bestPromotion`, `effectiveDiscountPercent`, `buyXGetYPercent`, elegibilidade produto/categoria).

## 2. Arquitectura Alvo
1. `PromotionService` (`<= 90` linhas): Fachada `@Service` e `@Transactional`, preservando `AppliedPromotion` e construtor retrocompatível de 5 argumentos.
2. `PromotionLifecycleService` (`<= 160` linhas): Componente `@Component` dedicado à listagem, criação, validação de escopo/parâmetros, activação/desactivação e mapeamento DTO.
3. `PromotionDiscountCalculator` (`<= 90` linhas): Componente `@Component` dedicado ao cálculo da melhor promoção activa e conversão de `PERCENT` / `BUY_X_GET_Y` em percentagem efectiva.
