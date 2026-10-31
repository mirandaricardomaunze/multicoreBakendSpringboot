# Especificação Técnica de Decomposição - CreditNoteService (Fase 116)

## 1. Contexto & Diagnóstico
O serviço `CreditNoteService.java` (360 linhas) no módulo `backend` (`mz.multicore.erp.modules.comercial.service`) agregava em um único componente:
- Injeção de 7 dependências (`CreditNoteRepository`, `InvoiceRepository`, `WarehouseRepository`, `InventoryService`, `DocumentNumberService`, `AuditLogService`, `ApplicationEventPublisher`).
- Criação transacional de Notas de Crédito (`create`), validação de motivo (`RETURN` vs outros), validação rigorosa de quantidades já devolvidas por linha (`qtySold - alreadyReturned`), precificação e impostos herdados com `LineCalculator.compute`, e barreira de teto (`validateNotExceedsInvoiceValue`).
- Ciclo de vida operacional: aprovação com perfil gestor/admin (`approve`), devolução física em stock (`returnStockForApprovedNote`) com FEFO/lote via `InventoryService`, publicação de `CreditNoteApprovedEvent` com `returnedCost` para contabilidade, rejeição (`reject`) e cancelamento (`cancel`) com rasto no `AuditLogService`.
- Consultas tenant-scoped (`findByCompany`, `findById`, `loadForPrint`), mapa de saldos restantes por linha (`getReturnedQuantitiesByInvoiceLine`) e projeção DTO determinística com agregação de unidade de negócio mista.

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `CreditNoteService.java`: $\le 105$ linhas (meta atual: ~80 linhas, redução $\ge 75\%$).
- Colaboradores:
  - `CreditNoteCreationService.java`: $\le 175$ linhas (responsabilidade única: validação de elegibilidade de linhas, precificação herdada e emissão).
  - `CreditNoteLifecycleService.java`: $\le 165$ linhas (responsabilidade única: aprovação com retorno de stock, eventos contábeis, rejeição, cancelamento e auditoria).
  - `CreditNoteQueryService.java`: $\le 145$ linhas (responsabilidade única: consultas tenant-scoped, mapa de devoluções por fatura e projeção DTO).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `CreditNoteService`.
- Preserva construtor canónico `@Autowired` recebendo os 3 colaboradores especializados.
- Preserva construtor retrocompatível de 7 argumentos para 100% de compatibilidade com `CreditNoteServiceTest` e controladores REST.
- Verificação rigorosa contra `BigDecimal.ZERO.equals(...)`: uso obrigatório de `compareTo(BigDecimal.ZERO) == 0`.
- Zero dependência Swing ou `JOptionPane` no backend.
