# Especificação Técnica: Decomposição Modular do StockWasteService (Fase 106)

## 1. Contexto e Diagnóstico
O `StockWasteService.java` acumulava 445 linhas de código no backend (`mz.multicore.erp.modules.inventory.service`), acumulando:
- Registo de quebras de inventário com chave de idempotência SHA-256 e validação de impressão digital.
- Regras de auto-aprovação com limiar de 2.500 MZN (`APPROVAL_THRESHOLD`), abate físico de stock no inventário (`inventoryService.registerMovement`) com suporte a lotes FEFO e integração com o motor central de aprovações (`ApprovalService`).
- Registo de trilha de auditoria formal (`AuditLogService.logEvent`) para submissão, aprovação e rejeição de quebras.
- Radar de lotes com validade próxima do vencimento (`getExpiringBatchesRadar`), categorização em níveis de criticidade (`VENCIDO`, `CRÍTICO`, `ALTO`, `MÉDIO`, `ATENÇÃO`) e cálculo de perda potencial.
- Resumo executivo de perdas por motivo, categoria de artigo e apuração do rácio percentual de quebras sobre a receita de vendas (`wasteRate`).

## 2. Decisão Arquitetural
A decomposição segregou as responsabilidades em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`StockWasteLifecycleService.java`** (275 linhas $\le 285$):
   - Registo transacional de quebras (`registerWaste`) com idempotência SHA-256 (`wasteFingerprint`).
   - Apuração do custo unitário do artigo (`resolveUnitCost`).
   - Avaliação da alçada de aprovação (<= 2.500 MZN ou perfil Gerente/Admin -> auto-aprova).
   - Abate físico de stock (`deductStockForWaste`) e tramitação para aprovação administrativa.
   - Decisão de aprovação ou rejeição (`approveWaste`) com cancelamento de pedidos pendentes e auditoria formal.

2. **`StockWasteAnalyticsService.java`** (145 linhas $\le 155$):
   - Radar analítico de lotes a expirar (`getExpiringBatchesRadar`) com níveis de alerta e cálculo de perda potencial em MZN.
   - Resumo executivo (`getSummary`) de quebras aprovadas agrupadas por motivo e categoria, comparadas com a faturação líquida do período para cálculo do rácio de quebra sobre vendas.

3. **`StockWasteQueryService.java`** (58 linhas $\le 75$):
   - Consultas de quebras por empresa e por estado (`findByCompany`, `findByCompanyAndStatus`).
   - Mapeamento analítico determinístico para `StockWasteDTO` (`toDTO`).

4. **`StockWasteService.java`** (126 linhas $\le 135$):
   - Fachada orquestradora enxuta.
   - Preservação integral dos construtores sobrecarregados de 7, 8 e 9 argumentos para garantir 100% de compatibilidade com testes legados (`StockWasteHarnessTest`, `AuditTrailCriticalEventsHarnessTest`) e injeção canónica `@Autowired` de 3 colaboradores.

## 3. Conformidade
- Redução de 445 para 110 linhas no orquestrador (redução de 335 linhas / 75.3%).
- Compatibilidade reversa completa com `StockWasteHarnessTest` e `AuditTrailCriticalEventsHarnessTest`.
- Zero uso de `BigDecimal.ZERO.equals` (uso de `compareTo` e `signum`).
- Mensagens e relatórios em português de Moçambique.
