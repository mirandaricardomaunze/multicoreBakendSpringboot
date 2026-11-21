# Harness — Importação de dados de outro ERP

> Cenários que provam [IMPORTACAO_DADOS_SPEC.md](IMPORTACAO_DADOS_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-21 (IMP-01 a IMP-05, IMP-CL-01 a IMP-CL-03, IMP-FO-01, IMP-AR-01 a IMP-AR-03, IMP-06 a IMP-11, IMP-UI-01 e IMP-UI-02 verdes — GOAL-006 concluído; `PostgresSchemaParityHarnessTest` só corre via `scripts/verify-backup-restore.ps1`; GOAL-007: IMP-ST-01 verde)

## Leitura do ficheiro (comum)

| ID | Garantia | Teste |
|---|---|---|
| IMP-01 | Lê UTF-8 com e sem BOM e, se não for UTF-8 válido, Windows-1252, com acentos intactos (§3.2) | `CsvImportReaderTest` ✅ |
| IMP-02 | Detecta `;` ou `,` no cabeçalho; aspas protegem separador e `""` (§3.2) | `CsvImportReaderTest` ✅ |
| IMP-03 | Colunas reconhecidas sem maiúsculas/acentos/espaços; obrigatória em falta recusa o ficheiro; a mais é anunciada (§3.2) | `CsvImportReaderTest` ✅ |
| IMP-04 | Números `1.234,56` e `1234.56`; datas `dd/mm/aaaa` e `aaaa-mm-dd`; outro formato é erro da linha com nº de linha (§3.2) | `ImportValueParserTest` ✅ |
| IMP-05 | Acima de 2 MB ou 5 000 linhas o ficheiro é recusado; linhas vazias ignoradas (§3.2) | `CsvImportReaderTest` ✅ |

## Cadastros (GOAL-006)

| ID | Garantia | Teste |
|---|---|---|
| IMP-CL-01 | Cliente: NUIT obrigatório e válido, email opcional; linha inválida é `ERRO` com coluna e mensagem PT-MZ (§4.1) | `ClientImportServiceTest` ✅ |
| IMP-CL-02 | Cliente: NUIT da empresa → `JÁ EXISTE`; NUIT de outra empresa → `ASSOCIADO` sem alterar dados; NUIT repetido no ficheiro → `ERRO` (§4.1, §3.3) | `ClientImportServiceTest` ✅ |
| IMP-CL-03 | Email do cliente opcional também no registo manual; migração retira `NOT NULL` de `clients.email` (§4.1) | `ClientOptionalEmailTest` ✅ · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` ✅ |
| IMP-FO-01 | Fornecedor: NUIT obrigatório e válido; NUIT da empresa → `JÁ EXISTE`; repetido no ficheiro → `ERRO` (§4.2) | `SupplierImportServiceTest` ✅ |
| IMP-AR-01 | Artigo: IVA por percentagem ou código existente; inexistente ou ambíguo → `ERRO` (§4.3) | `ProductImportServiceTest` ✅ |
| IMP-AR-02 | Artigo: categoria inexistente é criada só na confirmação e listada na pré-visualização (§4.3) | `ProductImportServiceTest` ✅ |
| IMP-AR-03 | Artigo: SKU existente → `JÁ EXISTE`; código de barras/referência de outro artigo → `ERRO` (§4.3) | `ProductImportServiceTest` ✅, `ProductLookupTest` ✅ |
| IMP-06 | Pré-visualizar não grava nada; confirmar revalida, recusa tudo com qualquer erro e grava o resto numa só transacção (§3.1) | `DataImportHttpIntegrationTest` ✅ |
| IMP-07 | Tudo na empresa activa; outra empresa não vê nem é alterada (§3.3) | `DataImportHttpIntegrationTest` ✅ |
| IMP-08 | Cada confirmação gera um `DATA_IMPORT` na auditoria com entidade, ficheiro e totais (§3.1) | `DataImportHttpIntegrationTest` ✅ |
| IMP-09 | Modelo CSV descarregável por entidade (`GET /api/data-import/{clients\|suppliers\|products}/template`): UTF-8 com BOM, `;`, só cabeçalho (§3.1) | `DataImportHttpIntegrationTest` ✅ |
| IMP-10 | Rotas de importação só para ADMIN e classificadas na `ApiRolePolicy` (§3.3) | `ApiRolePolicyEndpointCoverageHarnessTest` ✅ · `DataImportHttpIntegrationTest` ✅ · `ApiRolePolicyTest` ✅ |
| IMP-11 | O módulo de importação escreve só pelos serviços donos, sem Repository alheio — nem por import, import `*` ou nome completo (§3.3) | `DataImportModuleIsolationHarnessTest` ✅ · `ModuleDecouplingHarnessTest` ✅ |
| IMP-UI-01 | Ecrã "Importar dados": entidade, modelo, ficheiro, pré-visualização em tabela; Confirmar só sem erros (§3.4) | `DataImportPanelHarnessTest` ✅ |
| IMP-UI-02 | Formulário de cliente aceita email vazio (§4.1) | `DataImportPanelHarnessTest` ✅ |

## Saldos iniciais (GOAL-007)

| ID | Garantia | Teste |
|---|---|---|
| IMP-ST-01 | Stock: SKU, armazém e (se aplicável) validade obrigatórios; quantidade > 0, custo ≥ 0; referência inexistente → `ERRO` (§5.1) | `StockOpeningImportServiceTest` ✅ |
| IMP-ST-02 | Stock: artigo com movimentos nesse armazém → `ERRO`; reimportar não duplica (§5.1) | `StockOpeningImportServiceTest` |
| IMP-ST-03 | Stock: grava `ENTRY` com lote e saldo pelo ledger; sem lançamento contabilístico (§5.1) | `StockOpeningImportServiceTest` |
| IMP-SI-01 | Documento SI: numeração `SI-aaaa/n` por empresa; guarda `numero_original`; chave cliente+nº original → `JÁ EXISTE` (§5.2) | `OpeningReceivableTest` |
| IMP-SI-02 | SI: valor > 0, vencimento ≥ data, data não futura; cliente inexistente → `ERRO` (§5.2) | `OpeningReceivableTest` |
| IMP-SI-03 | SI fora do SAF-T, do IVA e dos relatórios de vendas (§5.2) | `OpeningReceivableFiscalExclusionTest` |
| IMP-SI-04 | SI entra no extracto, na antiguidade pelo vencimento, na exposição de crédito e na mora (§5.2) | `OpeningReceivableStatementTest` |
| IMP-SI-05 | SI liquida-se com recibos normais, total ou parcial (§5.2) | `OpeningReceivableSettlementTest` |
| IMP-SIF-01 | SIF: mesmas regras de chave, valores, datas e numeração `SIF-aaaa/n` (§5.3) | `OpeningPayableTest` |
| IMP-SIF-02 | SIF aparece em contas a pagar e paga-se com a acção de pagamento a fornecedor, total ou parcial (§5.3) | `OpeningPayableTest` |
| IMP-12 | Endpoints de stock inicial, SI e SIF: pré-visualizar/confirmar, tudo ou nada, empresa activa, ADMIN (§3) | `OpeningBalancesImportHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| IMP-UI-03 | Ecrã "Importar dados" oferece stock inicial, dívidas de clientes e dívidas a fornecedores (§3.4) | `DataImportPanelHarnessTest` |
