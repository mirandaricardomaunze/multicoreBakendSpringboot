---
id: GOAL-009
title: "Frete, alfândega e despachante repartidos no custo dos artigos importados"
status: IN_PROGRESS
priority: HIGH
spec: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md
harness: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_HARNESS.md
verify: ["LandedCostDocumentTest", "LandedCostAllocationTest", "LandedCostStockEffectTest", "LandedCostPostingTest", "LandedCostHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "LandedCostUiHarnessTest"]
blockedBy: []
---

# Objectivo

Depois de receber uma importação, o comprador regista os custos de frete, seguro, direitos,
despachante e IVA aduaneiro num documento CI. O sistema reparte-os pelos artigos, sobe o custo médio
do stock que ainda existe, manda a parte já vendida para CMV e cria as dívidas a cada fornecedor.
Regras em `docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md` §4.

Pré-requisito: GOAL-008 em `DONE` (moeda, câmbio e pagamento com diferença cambial). Se não estiver,
a iteração termina com `NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [x] LC-03 — o que entra no custo e o que vai a IVA dedutível — `LandedCostAllocationTest`
- [x] LC-04 — repartição por valor, peso ou quantidade — `LandedCostAllocationTest`
- [x] LC-05 — arredondamento com resíduo na maior quota — `LandedCostAllocationTest`
- [x] LC-01 — documento CI numerado e ligado a compras recebidas — `LandedCostDocumentTest`
- [x] LC-02 — linhas de custo geram dívida a cada fornecedor — `LandedCostDocumentTest`
- [x] LC-06 — parte para o stock no custo médio, resto a CMV, vendas passadas intactas — `LandedCostStockEffectTest`
- [x] LC-07 — lançamento 3201/6101/2432 contra 2201, equilibrado — `LandedCostPostingTest`
- [x] LC-08 — pré-visualização e confirmação por HTTP, empresa activa, perfis de compras — `LandedCostHttpIntegrationTest`
- [ ] LC-UI-01 — ecrã Compras › Custos de Importação — `LandedCostUiHarnessTest`

## Fora de âmbito

- Anular um CI confirmado, vendas e tesouraria em moeda estrangeira (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-22 · LC-03 · `LandedCostType` (contracts, com `capitalizable()`), `LandedCostCharge` (custo sem IVA em moeda base vs IVA dedutível; IVA aduaneiro todo a dedutível) e `LandedCostCalculator.totals`; `LandedCostAllocationTest` 5/5 verde.
- 2026-11-22 · LC-04 · `LandedCostAllocationMethod` (contracts: VALOR por omissão, PESO, QUANTIDADE) e `LandedCostCalculator.allocate` (quota proporcional, PESO = `netUnitWeightKg × quantidade`, linha sem peso ou base zero é `BusinessRuleException`); `LandedCostAllocationTest` 10/10 verde. Resíduo de cêntimos fica para LC-05.
- 2026-11-22 · LC-05 · `LandedCostCalculator.allocate` arredonda as quotas a 2 casas HALF_UP e põe o resíduo de cêntimos na linha de maior base (empate: a primeira), soma sempre igual ao total; regra do empate escrita na SPEC §4.3; `LandedCostAllocationTest` 13/13 verde.
- 2026-11-22 · LC-01 · `LandedCostDocument` (série `CI` em `DocumentSeries.LANDED_COST`, número por empresa, data, método com VALOR por omissão, compras ligadas por ordem), migração V145 e `LandedCostDocumentService.register` (só compras `COMPLETED` da empresa activa, Saldo Inicial recusado, várias CI por compra, perfis MANAGER/ADMIN); `LandedCostDocumentTest` 10/10 verde. Linhas de custo ficam para LC-02.
- 2026-11-22 · LC-02 · `LandedCostDocumentCharge` (tipo, fornecedor, moeda/câmbio, valor, taxa de IVA %) resolvida por `LandedCostChargeResolver` (fornecedor e taxa da empresa, câmbio pela regra do §3, IVA aduaneiro sem taxa); cada linha cria a dívida `PurchaseDocumentKind.LANDED_COST` `<CI>-n` em `purchases` (sem armazém nem stock, sem evento), listada em contas a pagar e paga por `SupplierPaymentService` ("Custos de Importação …"); migração V146; regra de gravação na SPEC §4.1; `LandedCostDocumentTest` 18/18 verde.
- 2026-11-22 · LC-06 · `LandedCostStockEffect` reparte o custo capitalizável na confirmação do CI e, por linha, `LandedCostCalculator.splitByStock` dá parte para o stock = C × min(1, S/Q) e o resto a CMV; `AverageCostUpdater.absorbCost` sobe o médio (por artigo na empresa) sem mexer em quantidades; `LandedCostAllocation` fotografa quota/Q/S/partes/médio antes e depois (migração V147) e sai no DTO; regra de gravação na SPEC §4.4; `LandedCostStockEffectTest` 7/7 verde.
- 2026-11-22 · LC-07 · `LandedCostDocumentService` publica `LandedCostConfirmedEvent` (valores em moeda base) e `LandedCostPostingService` (accounting) lança `JournalSource.LANDED_COST`: débito 3201 (parte do stock) e 6101 (parte vendida) das quotas fotografadas, 2432 pela diferença, crédito 2201 por dívida `<CI>-n`; sem duplicados nem sem plano de contas; regra de gravação na SPEC §4.4; `LandedCostPostingTest` 5/5 verde.
- 2026-11-22 · LC-08 · `LandedCostController` (`POST /api/purchases/landed-costs/preview` e `POST /api/purchases/landed-costs`, área MANAGER/ADMIN de compras); `LandedCostDocumentService.preview` reutiliza as validações da confirmação e `LandedCostStockEffect.preview` simula o médio (`AverageCostUpdater.costAfterAbsorbing`) sem gravar; `LandedCostPreviewDTO`; regra de gravação na SPEC §4.3; `LandedCostHttpIntegrationTest` 3/3 verde.
