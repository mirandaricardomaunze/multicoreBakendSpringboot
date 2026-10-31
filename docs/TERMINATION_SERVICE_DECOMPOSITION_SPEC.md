# Especificação Técnica: Decomposição Modular do TerminationService (Fase 110)

## 1. Contexto e Diagnóstico
O `TerminationService.java` acumulava 405 linhas de código no backend (`mz.multicore.erp.modules.hr.service`), agregando regras trabalhistas de cessação de contrato e apuramento de direitos (Lei n.º 13/2023):
- **Cálculo de Proporcionais e Acerto:** Salário proporcional aos dias trabalhados no mês da saída, 13.º mês proporcional, férias vencidas e não gozadas (com apuramento de direito por antiguidade ou aviso de fallback histórico), compensação contratual, desconto de aviso prévio não cumprido e dedução de adiantamentos/empréstimos pendentes.
- **Ciclo de Vida da Cessação:** Validações de unicidade e datas, atribuição de número de série oficial (`DocumentSeries.TERMINATION`), fecho do contrato de trabalho, atualização do estado do colaborador para `TERMINATED`, liquidação do acerto na tesouraria (`FinanceService.registerAutoPayout`) e auditoria contínua.
- **Consultas e Relatórios de Impressão:** Listagens para o ecrã de RH e carregamento detalhado com linhas para o serviço de impressão de termo de rescisão (`TerminationPrintService`).

## 2. Decisão Arquitetural
A decomposição segrega o serviço em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`TerminationSettlementCalculator.java`** (179 linhas $\le 210$):
   - Motor de cálculo financeiro do acerto rescisório (`compute` e `preview`).
   - Apuramento do salário até à saída, 13.º mês proporcional e saldo de férias.
   - Aplicação de regras de aviso prévio e descontos de passivos em folha (`PayrollDeductionService`).
   - Gestão de avisos amigáveis em português de Moçambique quando valores dependem de configuração não preenchida.

2. **`TerminationLifecycleService.java`** (131 linhas $\le 145$):
   - Execução transacional da rescisão (`terminate`): validação de colaborador, salvaguarda do contrato, emissão de série documental e carimbo de cessação no colaborador.
   - Liquidação financeira (`paySettlement`): barreira contra pagamento de saldos negativos, transição para `PAGO` e saída em tesouraria via `FinanceService`.
   - Registo formal de eventos de auditoria no `AuditLogService`.

3. **`TerminationQueryService.java`** (75 linhas $\le 100$):
   - Consultas da carteira de cessações da empresa ativa (`list`).
   - Carregamento com linhas para impressão (`loadForPrint`).
   - Mapeamentos determinísticos DTO (`toDTO`, `previewDTO`, `lineDTO`).

4. **`TerminationService.java`** (110 linhas $\le 125$):
   - Fachada unificada delegando nos três colaboradores especializados.
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 11 dependências garantindo 100% de compatibilidade com os testes unitários (`TerminationServiceTest`).

## 3. Conformidade
- Redução de 405 para 110 linhas no orquestrador (redução de 295 linhas / 72.8%).
- 100% retrocompatível com `TerminationServiceTest`, `TerminationController` e `TerminationPrintService`.
- Zero uso de `BigDecimal.ZERO.equals`.
- Mensagens ao utilizador em português de Moçambique.
