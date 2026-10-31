# Especificação Técnica: Decomposição Modular do BankReconciliationService (Fase 107)

## 1. Contexto e Diagnóstico
O `BankReconciliationService.java` acumulava 460 linhas de código no backend (`mz.multicore.erp.modules.financeira.service`), acumulando:
- Validação e ingestão de extratos bancários de tesouraria (`importStatement`), criação das linhas `BankStatementItem` e encerramento formal de períodos (`closeStatement`).
- Algoritmo de correspondência preliminar e contínua (*auto-matching*), incluindo matching exato por referência e heurística de montante exato com janela de data $\pm 3$ dias com verificação de unicidade de candidato.
- Conciliação manual (`manualMatch`), anulação de reconciliação (`unmatch`) e criação imediata de despesas/encargos bancários vinculados (`createAndMatchExpense`).
- Consultas analíticas de extrato, linhas e apuração do resumo executivo (`BankReconciliationSummaryDTO`) comparando o saldo do extrato com o saldo do razão contabilístico do sistema.

## 2. Decisão Arquitetural
A decomposição segregou as responsabilidades em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`BankStatementImportService.java`** (100 linhas $\le 120$):
   - Validação de cabeçalhos e linhas do extrato importado.
   - Persistência atómica de `BankStatement` com linhas e execução do auto-match preliminar.
   - Encerramento formal do período reconciliado (`closeStatement`).

2. **`BankReconciliationMatcher.java`** (230 linhas $\le 240$):
   - Motor de reconciliação automática em dois passos:
     1. Referência textual limpa + montante exato.
     2. Montante exato + janela temporal $\pm 3$ dias (apenas quando candidato único e inequívoco).
   - Reconciliação manual de linhas com transações específicas de tesouraria (`manualMatch`).
   - Desfazer de conciliações (`unmatch`).
   - Lançamento direto e reconciliação imediata de despesas/comissões bancárias (`createAndMatchExpense`).
   - Atualização do estado global do extrato (`updateStatementStatus`).

3. **`BankReconciliationQueryService.java`** (162 linhas $\le 175$):
   - Consultas de extratos por empresa e conta bancária (`getStatements`, `getStatement`).
   - Consulta ordenada de itens do extrato (`getStatementItems`).
   - Resumo executivo de conciliação (`getSummary`), apurando saldos no banco vs sistema, itens pendentes e última reconciliação fechada.
   - Mapeamentos determinísticos para DTOs (`toDTO`, `toItemDTO`).

4. **`BankReconciliationService.java`** (90 linhas $\le 110$):
   - Fachada executiva que orquestra as chamadas delegando aos 3 serviços especializados.
   - Construtor canónico `@Autowired` de 3 colaboradores e construtor retrocompatível de 6 dependências preservado para testes legados (`BankReconciliationHarnessTest`).

## 3. Conformidade
- Redução de 460 para 90 linhas no orquestrador (redução de 370 linhas / 80.4%).
- 100% retrocompatível com `BankReconciliationHarnessTest` e `BankReconciliationPrintService`.
- Zero uso de `BigDecimal.ZERO.equals` (uso estrito de `compareTo` e `signum`).
- Mensagens ao utilizador em português de Moçambique.
