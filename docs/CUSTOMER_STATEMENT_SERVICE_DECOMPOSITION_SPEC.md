# Especificação Técnica: Decomposição Modular do CustomerStatementService (Fase 123)

## 1. Contexto e Diagnóstico
O `CustomerStatementService.java` (304 linhas, `mz.multicore.erp.modules.comercial.service`) acumulava:
- Coleta de 4 tipos de documentos comerciais (Faturas `FT`, Notas de Débito `ND`, Recibos `RC`, Liquidação direta em fatura e Notas de Crédito `NC`).
- Apuração do saldo vencido (`overdue`) para faturas com `effectiveDueDate` anterior ao fim do período com saldo em aberto.
- Cálculo do saldo de abertura antes da data inicial (`openingBalance = debit - credit`), saldo progressivo corrente linha a linha (`running`), totais de débitos e créditos e saldo de fecho (`closingBalance`).
- Ordenação cronológica estrita null-safe e montagem do DTO `CustomerStatementDTO`.

## 2. Decisão Arquitetural
A decomposição segregou as responsabilidades mantendo 100% da fidelidade e integridade contabilística:

1. **`CustomerStatementMovement.java`** ($\le 30$ linhas):
   - Record imutável representando o movimento normalizado (data, tipo, número de documento, descrição, referência, débito, crédito, timestamp, ID e nome da unidade de negócio).
2. **`CustomerStatementCollector.java`** ($\le 195$ linhas):
   - Ingestão das 5 origens de eventos:
     1. Facturas de venda efectiva (`isRealisedSale`).
     2. Notas de Débito aprovadas.
     3. Recibos válidos (ignora `CANCELLED`).
     4. Liquidações directas em faturas sem recibo autónomo (`directSettlement = totalPaid - receiptSum`).
     5. Notas de Crédito aprovadas.
   - Apuração do montante vencido (`overdue`).
3. **`CustomerStatementCalculator.java`** ($\le 95$ linhas):
   - Ordenação cronológica composta: `date` $\rightarrow$ `timestamp` $\rightarrow$ `documentNumber`.
   - Cálculo do saldo de abertura anterior à data `from`.
   - Cálculo do saldo progressivo linha a linha dentro da janela temporal `[from, to]`.
   - Consolidação de totais e saldo de fecho (`closingBalance`).
4. **`CustomerStatementService.java`** ($\le 85$ linhas):
   - Fachada orquestradora fina validando o cliente e o tenant da sessão.
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 5 repositórios mantido para compatibilidade total com o `@InjectMocks` do `AccountStatementHarnessTest`.

## 3. Conformidade
- Redução de 304 linhas para 70 linhas na fachada `CustomerStatementService.java` (redução de 234 linhas / 77.0%).
- 100% compatível com `AccountStatementHarnessTest`, `CustomerStatementPrintService` e `CustomerStatementController`.
- Zero uso de `BigDecimal.ZERO.equals` (uso estrito de `compareTo` e `signum`).
- Backend headless (zero referências a Swing ou JOptionPane).
