# Especificação de Decomposição: OperationalPulseService (Fase 179)

## Contexto e Problema
`OperationalPulseService` (184 linhas) concentrava os cálculos de pulso operacional de quatro domínios distintos (Produção Industrial, Frota e Abastecimentos, Contratos de Avença MRR e Imobilizado Líquido).

## Solução Arquitectural
1. **`OperationalPulseService` (Fachada — `<= 65` linhas)**:
   - Obtém `companyId` do `CurrentUserContext` e agrega os 4 pulsos delegando para `IndustrialAndFleetPulseCalculator` e `FinancialContractsAndAssetsPulseCalculator`.
   - Mantém o construtor compatível de 5 argumentos.
2. **`IndustrialAndFleetPulseCalculator` (`@Component` — `<= 100` linhas)**:
   - Calcula `ManufacturingPulse` e `FleetPulse`.
3. **`FinancialContractsAndAssetsPulseCalculator` (`@Component` — `<= 85` linhas)**:
   - Calcula `RecurringPulse` (MRR e contratos vencidos) e `AssetPulse` (valor líquido contabilístico).
