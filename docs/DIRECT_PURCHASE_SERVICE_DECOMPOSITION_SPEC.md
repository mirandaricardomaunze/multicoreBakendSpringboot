# Especificação de Decomposição: DirectPurchaseService (Fase 157)

## 1. Diagnóstico do Monólito
`DirectPurchaseService.java` possuía **224 linhas**, misturando:
- Registo de compra directa com validação de fornecedor/armazém, cálculo de linhas (`LineCalculator`), entrada de stock (`InventoryService`), pagamento imediato (`FinanceService`), numeração (`DocumentNumberService`) e emissão de `PurchaseRegisteredEvent`.
- Consultas por empresa e projecção `PurchaseDTO` com agregação de unidade de negócio.

## 2. Arquitectura Alvo
1. `DirectPurchaseService` (`<= 85` linhas): Fachada `@Service` e `@Transactional`, preservando `toDTO` e o construtor de 9 argumentos.
2. `DirectPurchaseRegistrationService` (`<= 145` linhas): Componente `@Component` dedicado à criação da compra, entrada de stock, tesouraria e publicação de evento.
3. `DirectPurchaseQueryService` (`<= 75` linhas): Componente `@Component` dedicado à listagem por empresa e projecção `PurchaseDTO`.
