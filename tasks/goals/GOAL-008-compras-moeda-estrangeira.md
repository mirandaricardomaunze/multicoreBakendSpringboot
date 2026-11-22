---
id: GOAL-008
title: "Comprar e pagar fornecedores em USD, ZAR e EUR com câmbio e diferença cambial"
status: IN_PROGRESS
priority: HIGH
spec: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md
harness: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_HARNESS.md
verify: ["CompanyBaseCurrencyTest", "ExchangeRateRulesTest", "ForeignCurrencyPurchaseTest", "ForeignCurrencyPostingTest", "ExchangeDifferenceTest", "ExchangeDifferenceAccountsTest", "ForeignCurrencyHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "ForeignCurrencyUiHarnessTest"]
blockedBy: []
---

# Objectivo

O comprador regista encomendas e compras na moeda do fornecedor (USD, ZAR, EUR), com o câmbio do
dia gravado no documento. O stock, o custo médio e a contabilidade ficam correctos em meticais, e o
pagamento feito mais tarde, a outro câmbio, lança sozinho a diferença cambial.
Regras em `docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md` §2–§3 e §4.5.

## Critérios de aceitação

- [x] FX-01 — moeda base por empresa, MZN, imutável após movimentos — `CompanyBaseCurrencyTest`
- [x] FX-02 — moedas aceites e regras do câmbio — `ExchangeRateRulesTest`
- [x] FX-03 — câmbio proposto pela data ou anterior; sem câmbio exige escrita — `ExchangeRateRulesTest`
- [x] FX-04 — corrigir câmbio não altera documentos gravados — `ExchangeRateRulesTest`
- [x] FX-11 — contas 6601 e 7601 semeadas, também em empresas existentes — `ExchangeDifferenceAccountsTest`
- [x] FX-05 — encomenda e compra com moeda e câmbio; antigas com câmbio 1 — `ForeignCurrencyPurchaseTest`
- [x] FX-06 — total em moeda base e custo médio convertidos — `ForeignCurrencyPurchaseTest`
- [x] FX-08 — contas a pagar e extracto com moeda e dívida na moeda — `ForeignCurrencyPurchaseTest`
- [x] FX-07 — lançamento da compra em moeda base — `ForeignCurrencyPostingTest`
- [x] FX-09 — pagamento: câmbio do pagamento, baixa ao câmbio do documento, sem resíduos — `ExchangeDifferenceTest`
- [x] FX-10 — diferença cambial em 6601/7601, lançamento equilibrado — `ExchangeDifferenceTest`
- [x] FX-12 — endpoints de câmbios, compra e pagamento; ADMIN para câmbios — `ForeignCurrencyHttpIntegrationTest`
- [ ] FX-13 — PDFs com moeda e câmbio — `ForeignCurrencyHttpIntegrationTest`
- [ ] FX-UI-03 — tabela de câmbios e moeda base em Configurações — `ForeignCurrencyUiHarnessTest`
- [ ] FX-UI-01 — moeda e câmbio em encomenda/compra; contas a pagar com moeda — `ForeignCurrencyUiHarnessTest`
- [ ] FX-UI-02 — pagamento com câmbio e diferença cambial visível — `ForeignCurrencyUiHarnessTest`

## Fora de âmbito

- Custos de importação (GOAL-009).
- Vendas em moeda estrangeira, tesouraria em moeda estrangeira, reavaliação cambial, câmbio
  automático (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-22 · FX-01 · `Company.baseCurrency` (MZN por omissão, V140 põe MZN nas existentes), `CurrencyCode` com a lista fechada MZN/USD/ZAR/EUR e `CompanyBaseCurrencyService` que só deixa mudar a moeda base sem compras, facturas nem movimentos de stock (sondas `CompanyMovementProbe` em cada módulo dono); `CompanyBaseCurrencyTest` 7/7.
- 2026-11-22 · FX-02 · novo módulo `currency` com `ExchangeRate` (tabela `exchange_rates`, V141, chave empresa+moeda+data e `CHECK (rate > 0)`) e `ExchangeRateService.create/list`: só MZN/USD/ZAR/EUR, a moeda base fica fora da tabela (câmbio sempre 1), câmbio > 0 com no máximo 4 casas (gravado com 4), um por empresa, moeda e data, só na empresa activa; `ExchangeRateRulesTest` 9/9.
- 2026-11-22 · FX-03 · `ExchangeRateService.propose` devolve `ProposedExchangeRateDTO` com o câmbio da data ou da data anterior mais recente (moeda base sempre 1; sem câmbio, `manualRateRequired`) e `resolveDocumentRate` usa o câmbio escrito (validado como na tabela) ou o proposto, e recusa com "Escreva o câmbio do documento" quando não há nenhum; `ExchangeRateRulesTest` 15/15.
- 2026-11-22 · FX-04 · `ExchangeRateService.correct` (`CorrectExchangeRateRequest`) corrige só o valor de um câmbio da empresa activa, com as mesmas regras da tabela; o documento guarda o valor (fotografia) devolvido por `resolveDocumentRate`, nunca uma FK para `exchange_rates` (verificado nas migrações), por isso a correcção só afecta documentos novos; `ExchangeRateRulesTest` 20/20.
- 2026-11-22 · FX-11 · `PgcNirfChart` semeia 66 "Custos financeiros" > 6601 "Diferenças de câmbio desfavoráveis" e 76 "Proveitos financeiros" > 7601 "Diferenças de câmbio favoráveis" (constantes `EXCHANGE_LOSS`/`EXCHANGE_GAIN`) nas empresas novas; a migração V142 acrescenta-as às empresas que já têm plano, só as que faltam e repetível (provada em H2 modo PostgreSQL); `ExchangeDifferenceAccountsTest` 4/4. Nota: em dev (H2 sem Flyway) as empresas existentes recebem-nas pelo botão "semear plano", que já só cria as contas em falta.
- 2026-11-22 · FX-05 · `PurchaseOrder` e `Purchase` guardam `currency` e `exchangeRate` (fotografia, 4 casas; V143 acrescenta as colunas e põe os documentos existentes na moeda base da empresa com câmbio 1, repetível, provada em H2 modo PostgreSQL); `PurchaseCurrencyResolver` decide moeda e câmbio (sem moeda = moeda base com câmbio 1; estrangeira = câmbio escrito ou o proposto por `ExchangeRateService.resolveDocumentRate`), usado na encomenda e na compra directa; a compra criada pela recepção herda moeda e câmbio da encomenda; pedidos e DTOs de encomenda/compra ganharam `currency`/`exchangeRate` com construtores retrocompatíveis; `ForeignCurrencyPurchaseTest` 10/10.
- 2026-11-22 · FX-05 (gate) · diff-guard: moeda/câmbio e `applyDocumentCurrency` extraídos para a superclasse `@MappedSuperclass CurrencyDocument` (DG-10, sem duplicar entre `Purchase` e `PurchaseOrder`) e `PurchaseCurrencyResolver` com um só construtor, sem `@Autowired` (DG-07); testes do gate verdes.
- 2026-11-22 · FX-06 · `CurrencyDocument` ganhou `baseTotalAmount` (V144 acrescenta a coluna a encomendas e compras e preenche as existentes com `ROUND(total × câmbio, 2)`, repetível, provada em H2 modo PostgreSQL), `applyTotal` (grava o total e o total em moeda base juntos, 2 casas HALF_UP, usado na encomenda criada/alterada, na compra directa, na compra da recepção e no saldo inicial) e `toBaseUnitCost` (preço × câmbio sem arredondar); a compra directa e a recepção da encomenda dão entrada no stock e no custo médio ao custo convertido; `PurchaseDTO`/`PurchaseOrderDTO` expõem `baseTotalAmount`; `ForeignCurrencyPurchaseTest` 18/18. Nota para FX-07/FX-09: o evento contabilístico e o pagamento imediato da compra directa ainda usam o total na moeda do documento.
- 2026-11-22 · FX-08 · `Purchase.getBaseAmountPaid`/`getBaseOutstanding` dão o pago e a dívida em moeda base ao câmbio do documento (liquidada, o pago é exactamente o total em moeda base, sem resíduos) e `CurrencyDocument.baseTotal` cobre documentos sem a coluna preenchida (H2 de dev); `PayableDTO` ganhou `currency`, `exchangeRate`, `baseTotalAmount`, `baseOutstanding`; o extracto soma débitos, créditos e saldo em moeda base, cada linha traz `currency`, `exchangeRate` e `documentAmount`, e `SupplierStatementDTO.outstandingByCurrency` (`CurrencyBalanceDTO`) dá a dívida por moeda; a unidade de negócio da compra passou para `PurchaseBusinessUnit` (sem duplicar entre contas a pagar e extracto); construtores retrocompatíveis; `ForeignCurrencyPurchaseTest` 21/21. Nota para FX-09: a baixa da dívida em moeda base já é calculada ao câmbio do documento a partir de `amountPaid` (moeda do documento).
- 2026-11-22 · FX-07 · `PurchaseRegisteredEvents.of` (partilhado pela compra directa e pela compra da recepção da encomenda, sem duplicar a construção do evento) publica o facto contabilístico em moeda base ao câmbio do documento: total = total em moeda base, IVA = IVA × câmbio (2 casas HALF_UP) e líquido = total − IVA, para o lançamento ficar sempre equilibrado; mesmas contas de hoje (`ProcurementPostingHandler` não mudou); o pagamento no acto da compra directa sai da tesouraria e do lançamento pelo total em moeda base; `ForeignCurrencyPostingTest` 4/4. Nota para FX-09: falta o pagamento posterior (`SupplierPaymentRegisteredEvent`) com câmbio do pagamento.
- 2026-11-22 · FX-09 · `SupplierPaymentService.registerSupplierPayment(..., paymentRate)` (a assinatura antiga delega com câmbio vazio) resolve o câmbio do pagamento pelo `PurchaseCurrencyResolver` (escrito, ou o proposto pela tabela para a data do pagamento; sem nenhum recusa sem mexer em nada; compra na moeda base fica com câmbio 1 e paga como hoje); a tesouraria sai valor × câmbio do pagamento (`DocumentCurrency.toBaseAmount`); `Purchase.applyPayment` abate o valor na moeda do documento e devolve a baixa da dívida em moeda base ao câmbio do documento (diferença do saldo em moeda base antes/depois: proporcional nos parciais e, na liquidação, exactamente o que restava — SPEC §3.4 clarificado); `SupplierPaymentRegisteredEvent` ganhou `supplierAmount` (construtor antigo mantido, = `amount`); mensagem de saldo excedido com a moeda do documento; `ExchangeDifferenceTest` 7/7. Nota para FX-10: o `ProcurementPostingHandler.postSupplierPayment` ainda debita 2201 e credita a tesouraria por `amount`; tem de debitar 2201 por `supplierAmount` e lançar a diferença em 6601/7601.
- 2026-11-22 · FX-10 · `ProcurementPostingHandler.postSupplierPayment` debita 2201 pela baixa da dívida (`supplierAmount`, câmbio do documento), credita 1101/1201 pelo que saiu da tesouraria (`amount`, câmbio do pagamento) e lança a diferença a débito de 6601 (perda) ou a crédito de 7601 (ganho); sem diferença (moeda base ou evento antigo sem `supplierAmount`) fica com as duas linhas de hoje; lançamento sempre equilibrado e, na liquidação em parciais, 2201 debitado soma exactamente o crédito da compra em moeda base; `ExchangeDifferenceTest` 12/12.
- 2026-11-22 · FX-12 · `ExchangeRateController` em `/api/exchange-rates` (GET lista, GET `/proposal?currency&date`, POST criar, PUT `/{id}` corrigir), sempre na empresa activa; `ApiRolePolicy` classifica a área: criar/corrigir só ADMIN, consultar MANAGER (quem regista compras); `POST /api/purchases/{id}/pay` aceita `exchangeRate` opcional (câmbio do pagamento; vazio = proposto) via `PurchaseService.registerSupplierPayment(..., paymentRate)`; a compra em USD por HTTP usa o câmbio da tabela e grava o total em moeda base; `ForeignCurrencyHttpIntegrationTest` 2/2, `ApiRolePolicyEndpointCoverageHarnessTest` e `ApiRolePolicyTest` verdes. Nota: a guarda ADMIN dos câmbios está só na política HTTP (o Service não verifica perfil para não partir `ExchangeRateRulesTest`, que corre sem utilizador).
