# Especificação Técnica: Decomposição Modular do CreditRiskService (Fase 105)

## 1. Contexto e Diagnóstico
O `CreditRiskService.java` acumulava 459 linhas de código no backend (`mz.multicore.erp.modules.comercial.service`), acumulando:
- Apuração e classificação da matriz de aging de faturas a receber (`CORRENTE`, `ATE_30`, `DE_31_A_60`, `DE_61_A_90`, `MAIS_DE_90`), determinação de níveis de risco (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) e bloqueio comercial.
- Resumo executivo da carteira (`CreditRiskSummaryDTO`) e lista analítica de clientes (`ClientCreditRiskDTO`).
- Validação rígida de bloqueio para concessão de crédito por mora excessiva (> 30 dias) e por esgotamento de limite de crédito (`assertCreditAllowed`).
- Submissão e tramitação de pedidos formais de excepção de crédito (`requestCreditException`) para a Direção/Gerência via `ApprovalService`.
- Emissão e estruturação documental de Notificações Formais de Cobrança (`DebtCollectionNoticeDTO`) com discriminação de faturas vencidas, NIBs bancários e carta de intimação formal.

## 2. Decisão Arquitetural
A decomposição segregou as responsabilidades em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`CreditRiskAssessmentService.java`** (249 linhas $\le 255$):
   - Avaliação e apuração de faixas de maturidade (`AgingBucket`).
   - Gestão do acumulador interno `ClientRiskAccumulator` com cálculo de atraso máximo, mais velho vencimento e total em mora.
   - Determinação determinística de níveis de risco (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) e critérios de bloqueio.
   - Projeção de resumo executivo (`getSummary`) e listagem ordenada de risco de clientes (`getClientsRisk`).

2. **`CreditControlService.java`** (137 linhas $\le 150$):
   - Validação prévia de crédito (`assertCreditAllowed`), aplicando teto de 30 dias de mora e limite financeiro.
   - Verificação de excepções aprovadas pela gerência (`CREDIT_EXCEPTION`).
   - Submissão e formalização de pedidos de excepção de crédito (`requestCreditException`).

3. **`DebtCollectionNoticeService.java`** (122 linhas $\le 135$):
   - Construção da notificação formal de cobrança (`getDebtCollectionNotice`).
   - Filtragem e ordenação cronológica de faturas em mora, discriminação de contas correntes bancárias (BIM, BCI, Standard Bank) e texto legal de interpelação amigável.

4. **`CreditRiskService.java`** (92 linhas $\le 120$):
   - Fachada enxuta delegando aos 3 serviços.
   - Construtor canónico `@Autowired` e construtor retrocompatível de 5 dependências preservado para testes legados (`CreditRiskHarnessTest`).

## 3. Conformidade
- Redução de 459 para 92 linhas no orquestrador (redução de 367 linhas / 80.0%).
- 100% retrocompatível com `CreditRiskHarnessTest` e `DebtCollectionNoticePrintService`.
- Zero uso de `BigDecimal.ZERO.equals` (uso estrito de `compareTo` e `signum`).
- Mensagens e relatórios em português de Moçambique.
