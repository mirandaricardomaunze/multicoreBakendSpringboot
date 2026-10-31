# Especificação de Decomposição: FleetService (Fase 168)

## Contexto e Problema
`FleetService` (206 linhas) concentrava a gestão de viaturas (registo, consulta, actualização de estado e mapeamento `VehicleDTO`) juntamente com o registo de abastecimentos de combustível, cálculo de consumo médio Km/L, emissão de `FleetFuelLoggedEvent` e histórico de abastecimentos.

## Solução Arquitectural
1. **`FleetService` (Fachada — `<= 95` linhas)**:
   - Orquestra as chamadas transaccionais delegando para `FleetVehicleManagementService` e `FleetFuelLogService`.
   - Mantém os construtores retrocompatíveis de 3 e 4 argumentos.
2. **`FleetVehicleManagementService` (`@Component` — `<= 100` linhas)**:
   - Gere `createVehicle`, `listVehicles`, `getVehicle`, `updateStatus` e `toVehicleDTO`.
3. **`FleetFuelLogService` (`@Component` — `<= 125` linhas)**:
   - Gere `recordFuel`, atualização de odómetro/consumo médio, emissão de `FleetFuelLoggedEvent`, `listFuelLogs` e `toFuelDTO`.
