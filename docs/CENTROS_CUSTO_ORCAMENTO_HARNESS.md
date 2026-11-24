# Harness — Centros de custo e orçamento

> Cenários que provam [CENTROS_CUSTO_ORCAMENTO_SPEC.md](CENTROS_CUSTO_ORCAMENTO_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-24

## Centros de custo (GOAL-010)

| ID | Garantia | Teste |
|---|---|---|
| CC-01 | Centro por empresa com código único, tipo WAREHOUSE/MANUAL; não se apaga com lançamentos; inactivo ou de outra empresa recusa lançamentos (§3.1) | `CostCenterRulesTest` (`cc01_*`) · `MigrationPostgresDialectHarnessTest` |
| CC-02 | Criar armazém cria o seu centro; armazéns existentes recebem centro; centro de armazém acompanha nome e estado (§3.1) | `CostCenterWarehouseSyncTest` (`cc02_*`) · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| CC-03 | Cada empresa tem `ADM — Administração` como centro por omissão, alterável para outro centro activo; o por omissão não se desactiva nem apaga (§3.1) | `CostCenterRulesTest` (`cc03_*`) · `MigrationPostgresDialectHarnessTest` |
| CC-04 | Linha de classe 6/7 exige centro; classe 1–5 recusa centro; centro de outra empresa ou inactivo recusado; a porta de gravação aplica a regra (§3.2) | `JournalLineCostCenterTest` (`cc04_*`) |
| CC-05 | Lançamentos automáticos herdam o centro do armazém de origem (venda, POS, notas, quebras, ajustes, produção, custos de importação, câmbio de compras); sem armazém ou sem centro activo usam o centro por omissão; estorno mantém o centro da original (§3.2) | `AutomaticPostingCostCenterTest` (`cc05_*`) |
| CC-06 | Lançamento manual e despesa de tesouraria aceitam centro; vazio em 6/7 usa o por omissão; linhas antigas migradas (§3.2) | `JournalLineCostCenterTest` (`cc06_*`) · `MigrationPostgresDialectHarnessTest` |
| CC-07 | Resultados por centro: proveitos, custos, resultado; total igual à demonstração de resultados; partidas 6/7 sem centro em "Sem centro" (§3.3) | `CostCenterResultReportTest` (`cc07_*`) |
| CC-08 | Razão e balancete com filtro opcional por centro: vazio = completo; com centro só as partidas dele, incluindo o saldo inicial do razão (§3.3) | `CostCenterResultReportTest` (`cc08_*`) |
| CC-09 | Endpoints de centros (`/api/accounting/cost-centers`, ADMIN/MANAGER para gerir) e relatório (`/cost-center-result`, total igual à demonstração); empresa activa; a lista traz sempre o centro por omissão (§3.1, §3.3) | `CostCenterHttpIntegrationTest` (`cc09_*`) · `ApiRolePolicyEndpointCoverageHarnessTest` |
| CC-UI-01 | Configurações › Centros de Custo: tabela (código, nome, tipo, estado, por omissão), criar/editar só manuais, apagar nunca o por omissão, tornar por omissão só activo, gerir só ADMIN/MANAGER (§3.1, §3.3) | `CostCenterUiHarnessTest` (`ccUi01_*`) |
| CC-UI-02 | Lançamento manual e despesa pedem o centro com o por omissão pré-seleccionado (só activos; sem lista, "por omissão" que deixa o backend decidir); no lançamento o centro só segue nas partidas 6/7 (§3.2) | `CostCenterUiHarnessTest` (`ccUi02_*`) |
| CC-UI-03 | Contabilidade › Resultados por centro (última aba): período, tabela por centro com "Sem centro" e linha Total igual à demonstração, exportação PDF/Excel/CSV (§3.3) | `CostCenterUiHarnessTest` (`ccUi03_*`) |

## Orçamento (GOAL-011)

| ID | Garantia | Teste |
|---|---|---|
| OR-01 | Orçamento por empresa e ano; linhas só de contas movimentáveis 6/7, centro activo, mês 1–12, valor ≥ 0, únicas (§4.1) | `BudgetRulesTest` (`or01_*`) · `MigrationPostgresDialectHarnessTest` |
| OR-02 | Copiar do ano anterior com ajuste %, 2 casas, só células vazias (§4.1) | `BudgetRulesTest` (`or02_*`) |
| OR-03 | Real por conta × centro × mês com sinal por classe (§4.2) | `BudgetVarianceTest` (`or03_*`) |
| OR-04 | Desvio e desvio %, favorável/desfavorável por classe; orçado zero sem % (§4.2) | `BudgetVarianceTest` |
| OR-05 | Alertas a 90% e 100% dos custos do centro no mês, um por limiar, centro e mês (§4.3) | `BudgetAlertTest` |
| OR-06 | Compra/despesa que passa um limiar devolve aviso e grava na mesma (§4.3) | `BudgetAlertTest` |
| OR-07 | Endpoints de orçamento e real vs orçado; ADMIN/MANAGER; empresa activa; auditoria `BUDGET_CHANGE` (§4.1, §4.2) | `BudgetHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| OR-UI-01 | Contabilidade › Orçamento: grelha anual, copiar do ano anterior, gravar (§4.4) | `BudgetUiHarnessTest` |
| OR-UI-02 | Contabilidade › Real vs Orçado com desvios em `StatusTone`; alertas no sino (§4.3, §4.4) | `BudgetUiHarnessTest` |
