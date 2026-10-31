# Harness de Decomposição - CustomerOrderFulfillmentService (Fase 120)

## 1. Objectivo

Validar a decomposição estrutural e comportamental de `CustomerOrderFulfillmentService.java`, garantindo conformidade com limites físicos de linhas, ausência de `JOptionPane` e delegação canónica para os colaboradores.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **COFSD-01** | Limites físicos estritos de linhas | `CustomerOrderFulfillmentService` $\le 160$, `OrderPickingCoordinator` $\le 195$, `OrderFulfillmentLifecycleService` $\le 135$. |
| **COFSD-02** | Zero diálogos legados `JOptionPane` | Nenhuma chamada a `JOptionPane` em qualquer das classes. |
| **COFSD-03** | Delegação canónica | `CustomerOrderFulfillmentService` delega picking para `pickingCoordinator` e ciclo de vida para `lifecycleService`. |
| **COFSD-04** | Preservação de contratos | Preservação integral dos métodos públicos `submit`, `printForPicking`, `reprint`, `getPickingDetails`, `printForPickingA4`, `completeSeparationWithConfirmation`, `completeSeparation`, `bill` e `events`. |
