# Especificação de Decomposição: ReorderService (Fase 172)

## Contexto e Problema
`ReorderService` (200 linhas) concentrava a recolha agregada de saldos de stock, vendas dos últimos 30 dias e histórico de compras juntamente com o motor de cálculo de reposição inteligente (caixas inteiras, velocidade diária, previsão de dias de rutura, estado de criticidade e ordenação prioritária).

## Solução Arquitectural
1. **`ReorderService` (Fachada — `<= 65` linhas)**:
   - Valida o tenant (`CurrentUserContext.requireCompany`) e orquestra `ReorderDataCollector` + `ReorderSuggestionCalculator`.
   - Mantém o construtor compatível de 4 argumentos.
2. **`ReorderDataCollector` (`@Component` — `<= 85` linhas)**:
   - Agrega saldos por produto, vendas realizadas dos últimos 30 dias e última compra por produto (`ProductPurchaseInfo`).
3. **`ReorderSuggestionCalculator` (`@Component` — `<= 125` linhas)**:
   - Calcula défice em caixas inteiras, velocidade diária, dias restantes, estado de urgência (`ESGOTADO`, `CRÍTICO`, `BAIXO`), custo estimado e ordenação prioritária.
