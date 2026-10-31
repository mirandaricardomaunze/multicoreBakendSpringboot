# Harness de Decomposição do InventoryPhysicalCountingService

`InventoryPhysicalCountingServiceDecompositionHarnessTest` garante que o ciclo de vida da contagem física e
o mapeamento/consultas permanecem extraídos da fachada `InventoryPhysicalCountingService`, preservando a
validação tenant-scoped e os limites físicos de linhas.
