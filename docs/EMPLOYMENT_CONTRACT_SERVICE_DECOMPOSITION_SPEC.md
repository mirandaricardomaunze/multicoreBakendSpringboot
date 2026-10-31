# Especificação Técnica de Decomposição - EmploymentContractService (Fase 118)

## 1. Contexto & Diagnóstico
O serviço `EmploymentContractService.java` (354 linhas) no módulo `backend` (`mz.multicore.erp.modules.hr.service`) agregava múltiplas responsabilidades:
- Injeção e gestão direta de 6 dependências (`EmploymentContractRepository`, `EmployeeRepository`, `CompanyRepository`, `DocumentNumberService`, `SalaryHistoryService`, `AuditLogService`).
- Regras de validação estritas da Lei Laboral moçambicana (Lei 13/2023) para contratos a termo vs sem termo, motivos legais de termo e coerência do período experimental (`validateDates`).
- Garantia da invariante canônica de não-sobreposição de contratos vigentes no tempo (`ensureNoOverlap`).
- Ciclo de vida operacional: emissão de rascunhos (`createContract`), passagem a vigorar (`activateContract`) com sincronização automática do histórico salarial no momento exato do início (`syncEmployeeFromContract`), renovação estruturada com cessação do vínculo anterior na véspera (`renewContract`), cessação definitiva (`terminateContract`) e auditoria formal via `AuditLogService`.
- Consultas tenant-scoped (`getAllContracts`, `getContractsOfEmployee`, `findContractInPeriod`), radar analítico de alertas de fim de contrato e período experimental (`getContractAlerts`) e projeção DTO determinística (`toDTO`).

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `EmploymentContractService.java`: $\le 125$ linhas (meta atual: ~95 linhas, redução $\ge 65\%$).
- Colaboradores:
  - `ContractValidationService.java`: $\le 110$ linhas (responsabilidade única: validações de datas legais, parsing de tipo e barreira contra sobreposições vigentes).
  - `ContractLifecycleService.java`: $\le 220$ linhas (responsabilidade única: criação, ativação com sincronização salarial, renovação, rescisão e auditoria).
  - `ContractQueryService.java`: $\le 140$ linhas (responsabilidade única: consultas tenant-scoped, radar de alertas e mapeamento DTO).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `EmploymentContractService`.
- Preserva constantes públicas `CONTRACT_ENDING_ALERT_DAYS = 30` e `PROBATION_ENDING_ALERT_DAYS = 7`.
- Preserva construtor canónico `@Autowired` recebendo os colaboradores especializados.
- Preserva construtor retrocompatível de 6 parâmetros para 100% de compatibilidade com `EmploymentContractServiceTest` e controladores REST.
- Verificação rigorosa contra `BigDecimal.ZERO.equals(...)`.
- Zero dependência Swing ou `JOptionPane` no backend.
