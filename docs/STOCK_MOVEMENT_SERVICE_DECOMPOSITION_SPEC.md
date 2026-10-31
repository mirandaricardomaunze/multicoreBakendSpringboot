# Especificação Técnica de Decomposição - StockMovementService (Fase 114)

## 1. Contexto & Diagnóstico
O serviço `StockMovementService.java` (370 linhas) no módulo `backend` (`mz.multicore.erp.modules.inventory.service`) agregava múltiplas responsabilidades críticas:
- Injeção e gestão direta de 9 dependências (`StockMovementRepository`, `StockMovementOperationRepository`, `StockAdjustmentOperationRepository`, `StockRepository`, `WarehouseRepository`, `ProductRepository`, `CompanyRepository`, `ProductBatchService`, `AuditLogService`).
- Registo físico de movimentos de entrada e saída com deduplicação/consumo FEFO e loteamento (`registerMovement`, `registerEntry`, `registerExit`, `adjustStock`).
- Idempotência transacional SHA-256 para requisições de movimento (`movementFingerprint`, `StockMovementOperation`).
- Coordenação de ajustes de inventário baseados em contagem física (`adjustStock(CreateStockAdjustmentRequest)`), controle de concorrência e guarda de permissão (`PermissionGuard.requireManagerOrAdmin`).
- Idempotência transacional SHA-256 para requisições de ajuste (`adjustmentFingerprint`, `StockAdjustmentOperation`) e registo de auditoria com `AuditLogService`.
- Consultas tenant-scoped (`getMovementsByCompany`, `findMovementsByCompany`, `findMovementsPageByCompany`) e projeção DTO (`toDTO`).

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `StockMovementService.java`: $\le 140$ linhas (meta atual: ~129 linhas, redução de 65.1%).
- Colaboradores:
  - `StockMovementRegistrationService.java`: $\le 230$ linhas (responsabilidade única: execução de entradas e saídas físicas, FEFO, lotes e idempotência de movimentos).
  - `StockAdjustmentCoordinator.java`: $\le 165$ linhas (responsabilidade única: contagem física, cálculo de divergência, permissão de gestor, idempotência e auditoria de ajustes).
  - `StockMovementQueryService.java`: $\le 95$ linhas (responsabilidade única: consultas tenant-scoped, paginação e mapeamento DTO).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `StockMovementService`.
- Preserva construtor canónico de 9 dependências para retrocompatibilidade com `InventoryService` e testes unitários existentes.
- Injeção canónica `@Autowired` dos 3 colaboradores especializados.
- Verificação rigorosa contra `BigDecimal.ZERO.equals(...)`: uso obrigatório de `compareTo(BigDecimal.ZERO) == 0`.
- Zero dependência Swing ou `JOptionPane` no backend.
