# Especificação Técnica: Decomposição Modular do ApprovalService (Fase 122)

## 1. Contexto e Diagnóstico
O `ApprovalService.java` (314 linhas, `mz.multicore.erp.modules.approvals.service`) acumulava:
- Definição de limites estatutários de aprovação (`BASE_AUTO_APPROVE_LIMIT = 50.000,00 MZN`, `BASE_MANAGER_LIMIT = 250.000,00 MZN`) e avaliação descritiva de alçadas ("Green Lane", "(Requer ADMIN)", "(Requer MANAGER)").
- Submissão transacional com deduplicação de pedidos pendentes para o mesmo documento (`submitRequest`), auto-aprovação de documentos abaixo do limite e cancelamento em cascata quando o documento de origem é anulado (`cancelPendingForDocument`).
- Decisão hierárquica (`approveRequest`, `rejectRequest`) com validação estrita de perfis autorizados (Administrador vs Gestor), registo em histórico (`ApprovalHistory`) e disparo síncrono de callbacks de documento (`ApprovalCallback`).
- Sincronização automática e indolente de documentos pendentes ainda não indexados no fluxo de aprovações (transferências de stock e quebras de inventário via `ObjectProvider`).

## 2. Decisão Arquitetural
Segregação das responsabilidades em colaboradores especializados de responsabilidade única:

1. **`ApprovalEscalationPolicy.java`** ($\le 50$ linhas):
   - Constantes oficiais de limites: `BASE_AUTO_APPROVE_LIMIT` e `BASE_MANAGER_LIMIT`.
   - Método puro `evaluate(BigDecimal amount, String description)` retornando `EscalationDecision(String requiredRole, boolean autoApproved)`.
2. **`ApprovalLifecycleService.java`** ($\le 210$ linhas):
   - Submissão de pedidos com verificação de pendentes e avaliação de escalonamento (`submitRequest`).
   - Aprovação com verificação de papéis autorizados e gravação de histórico (`approveRequest`).
   - Rejeição com motivo obrigatório (`rejectRequest`).
   - Cancelamento limpo por anulação de documento na origem (`cancelPendingForDocument`).
   - Mapeamento determinístico `toDTO(request)`.
   - Disparo determinístico de `ApprovalCallback` e registo em `ApprovalHistory`.
3. **`ApprovalQueryService.java`** ($\le 100$ linhas):
   - Consultas paginadas/ordenadas tenant-scoped (`getPendingRequests`, `getAllRequests`).
   - Sincronização sob demanda de transferências de stock em separação e quebras de inventário pendentes (`syncUnindexedPendingDocuments`).
4. **`ApprovalService.java`** ($\le 115$ linhas):
   - Fachada unificada que preserva todos os métodos públicos e constantes estáticas.
   - Construtor canónico `@Autowired` recebendo os novos colaboradores.
   - Preservação estrita dos construtores sobrecarregados retrocompatíveis (4 e 6 argumentos) para compatibilidade 100% com testes unitários legados (`ApprovalServiceTest`), mocks de módulos (`ComercialService`, `HRService`, `StockTransferService`, etc.) e integração.

## 3. Conformidade
- Redução de 314 linhas para ~105 linhas na fachada `ApprovalService.java` (redução de 209 linhas / 66.5%).
- 100% retrocompatível com `ApprovalServiceTest`, callbacks inter-módulos e controladores REST (`ApprovalController`).
- Backend headless (zero referências a Swing ou JOptionPane).
- Zero comparações inválidas de BigDecimal (`BigDecimal.ZERO.equals`).
