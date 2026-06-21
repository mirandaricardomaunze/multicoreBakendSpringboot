# Tarefa Actual

> Ponteiro da sessão. A IA lê-o no início e actualiza-o sempre que uma fase fecha. ≤1 página. Histórico no `git log`.

**Última actualização:** 2026-06-20
**Estado:** software principal de prontidão para loja/mercearia concluído e testado. O que resta depende de validação manual/hardware/restore em ambiente separado. A fonte de verdade operacional é [tasks/retail_store_readiness.md](retail_store_readiness.md).

### Progresso — 2026-06-20 (dívida técnica + cobertura de testes)

- **Nota de Débito** alinhada com `DocumentSeries`: nova série `ND`, número sequencial gapless via
  `DocumentNumberService.next(...)` em vez de `"ND-" + timestamp`. (resolve dívida §7.2 de MOVIMENTOS_COMERCIAIS.md)
- **Cobertura de testes** dos Services críticos que faltavam ao harness:
  - `POSServiceTest` (10): checkout sem sessão, via legada vs multi-método, fiado parcial,
    numerário+cartão sem dupla contagem, fecho de caixa (sem diferença / diferença exige permissão / depósito).
  - `CreditNoteServiceTest` (8): RETURN repõe stock só na aprovação, limites de quantidade/valor, permissão.
  - `DocumentNumberServiceTest` (6): sequência gapless, séries independentes, corrida na criação, série ND.
- **BackupService confirmado**: faz export real (dump JSON de todas as coleções por empresa) — não é
  placeholder. Só não tem restore programático (por design; restore é ao nível de BD em ambiente separado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 71 testes, 0 falhas**.

### Progresso — 2026-06-20 (faturação directa + lote vencido + mais testes)

- **Faturação directa (decisão do utilizador):** `ComercialService.createInvoice` deixou de passar
  sempre pela Engine de Aprovações. Agora exige perfil **MANAGER/ADMIN**, emite a fatura já **APPROVED**
  e baixa stock no acto. **Só desconto >10%** mantém o caminho `PENDING_DISCOUNT_APPROVAL` → aprovação
  do gerente (stock baixa na aprovação via callback). Sem dupla baixa de stock.
- **Lote vencido (RS-12 / spec §4):** `ProductBatchService.addToBatch` bloqueia entrada de stock com
  validade já no passado (validade = hoje ainda entra; sem validade não bloqueia). Guarda todas as
  entradas porque compra/ENTRY passam por `addToBatch`.
- **Novos testes:** `ComercialServiceTest` (8) e 3 cenários de lote vencido em `ProductBatchServiceTest`.
  `MulticoreServicesTest.testDiscountApprovalThreshold` actualizado para o novo fluxo (5% → APPROVED).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 82 testes, 0 falhas**.

### Progresso — 2026-06-21 (segurança/roles por API)

- **Confirmado:** o `SecurityInterceptor` já impõe token+empresa em todo o `/api/**` (excepto
  `/api/auth/login`): 401 sem token, 403 sem acesso à empresa, e resolve o role por empresa. A spec §9
  está satisfeita ao nível do interceptor — o filtro Spring permissivo **não** é uma falha real.
- **Novo teste:** `SecurityApiIntegrationTest` (4) valida ponta-a-ponta pela API: 401 sem token,
  403 empresa sem acesso, e o role gate ao faturar (EMPLOYEE bloqueado / ADMIN passa). Fecha o item
  "login, tenant e roles testados por API" do harness.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 86 testes, 0 falhas**.

---

## Foco em curso

Fechar lacunas para uso real em loja/mercearia:

- Spec: [docs/RETAIL_STORE_SPEC.md](../docs/RETAIL_STORE_SPEC.md)
- Harness: [docs/RETAIL_STORE_HARNESS.md](../docs/RETAIL_STORE_HARNESS.md)
- Task faseada: [tasks/retail_store_readiness.md](retail_store_readiness.md)

Prioridade imediata: executar o harness RS-01 a RS-22 com dados reais de loja, validar impressora/leitor/gaveta e testar restore num ambiente separado.

### Progresso da Fase 1 — 2026-06-17

- Criado `PermissionGuard` central para exigir `MANAGER/ADMIN` ou `ADMIN`.
- Aplicada permissão em sangria, fecho de caixa com diferença, anulação de factura/recibo, aprovação/rejeição/cancelamento de notas e aprovação/rejeição de transferências.
- Auditados fecho de caixa, sangria/suprimento, anulações, notas e transferências.
- Verificado que o desktop propaga utilizador/role/empresa para `CurrentUserContext` e que o client HTTP envia `X-Company-Id`.
- Verificação técnica: `mvn -q -DskipTests compile` e `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest" test` passaram.

### Progresso das Fases 2-5 — 2026-06-17

- POS: criado `POST /api/pos/returns` para devolução por nota de crédito `RETURN`, com reembolso CASH/CARD/BANK_TRANSFER/CREDIT.
- Produtos: adicionados `ProductSaleType` e `stockTracked`; linhas de factura/encomenda/POS migradas para `BigDecimal`; POS Swing aceita quantidade decimal.
- Stock: criado `POST /api/inventory/adjustments` para contagem/ajuste com motivo, permissão e auditoria.
- Relatórios: criado `GET /api/reports/daily-store` com vendas do dia, fiado em aberto, pagamentos por método, movimentos de caixa e top produtos.
- Migration: criada `V8__retail_product_sale_type_and_decimal_quantities.sql`.
- Verificação técnica: `mvn -q -DskipTests compile` passou; `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest,MulticoreServicesTest" test` passou.

### Progresso final de prontidão loja/mercearia — 2026-06-17

- POS Swing: devolução/troca operacional ligada ao histórico de vendas, com motivo, quantidade devolvida, método de reembolso e armazém.
- Stock Swing: ajuste passou a ser contagem física com quantidade contada e motivo, usando o Service auditado.
- Relatórios: `GET /api/reports/daily-store` passou a incluir vendas por operador e margem bruta por produto.
- Backup: criado verificador não destrutivo de backup JSON, botão "Verificar Backup" no painel de Configurações e teste unitário.
- Verificação técnica final: `mvn -q clean compile` passou; `mvn -q test` passou.

### Ainda pendente para declarar loja real pronta em ambiente físico

- Teste manual do harness RS-01 a RS-22 em ambiente real.
- Restore de backup testado em ambiente separado.
- Validação de impressora, leitor, gaveta e decisão de balança/etiquetas.

## Feito nas últimas iterações

### Funcionalidade — Validades & FEFO
- Backend: `ProductBatchService.findNextFEFO(productId, warehouseId)` exposto via `InventoryService`.
- UI:
  - [StockPanel](src/main/java/com/phcpro/gui/StockPanel.java) — botão "Adicionar Lote/Validade" + diálogo dedicado; chain após "Cadastrar Produto".
  - [POSPanel](src/main/java/com/phcpro/gui/POSPanel.java) — campos Lote+Validade FEFO read-only auto-preenchidos.
  - [ComercialPanel](src/main/java/com/phcpro/gui/ComercialPanel.java) — Faturas e Encomendas com Lote/Validade FEFO read-only.
  - Diálogo de transferência ganhou colunas Lote+Validade FEFO recalculadas.

### Documentação (spec-driven harness)
- `README.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `CLAUDE.md` criados.
- `ARCHITECTURAL_GUIDELINES.md` + `ARCHITECTURE_SEPARATION.md` consolidados e removidos.

### Infra "production-ready"
- **Lombok**: [lombok.config](lombok.config) — `stopBubbling`, marca métodos gerados, proíbe `@Data`/`@AllArgsConstructor`/`@Builder`. Setup IDE documentado em [CONVENTIONS.md §3](CONVENTIONS.md#3-lombok).
- **Handler global**: confirmado [GlobalExceptionHandler](src/main/java/com/phcpro/architecture/exception/GlobalExceptionHandler.java) já existia (BusinessRule → 400, Validation → 400 com mapa, fallback → 500).
- **Flyway + PostgreSQL**: dependências em [pom.xml](pom.xml); [application-prod.properties](src/main/resources/application-prod.properties) com `ddl-auto=validate`, Flyway ON, vars `DB_URL/DB_USER/DB_PASSWORD`. Pasta [db/migration/](src/main/resources/db/migration/) com README explicando como gerar `V1__init.sql` a partir das entidades JPA.
- **Spring Security scaffold**: [SecurityConfig](src/main/java/com/phcpro/architecture/security/SecurityConfig.java) — BCryptPasswordEncoder + filter chain permissiva (não quebra desktop). [AppUserService](src/main/java/com/phcpro/modules/users/service/AppUserService.java) migrado para BCrypt com fallback de migração suave (passwords em texto-plano legadas continuam a autenticar e são re-encriptadas na próxima autenticação).
- **OpenAPI / Swagger**: dependência `springdoc-openapi-starter-webmvc-ui` adicionada. Em dev: `http://localhost:8080/swagger-ui.html`. Desactivado em prod até haver autenticação para a UI.
- **CI**: [.github/workflows/build.yml](.github/workflows/build.yml) — `mvn clean compile`, `mvn test`, `mvn package` em cada push/PR para `main`.
- **Testes unitários**: [ProductBatchServiceTest](src/test/java/com/phcpro/modules/inventory/service/ProductBatchServiceTest.java) — 9 testes com Mockito a cobrir `findNextFEFO`, `consumeFEFO` (single batch / multi-batch / stock insuficiente / qty inválida) e `addToBatch` (novo / acumular / qty inválida). **9/9 verde.**

## Por validar manualmente (não posso fazer como agente)

- [ ] Cadastrar produto novo → confirmar prompt "Adicionar stock inicial".
- [ ] Adicionar 2 lotes do mesmo produto com validades diferentes → confirmar FEFO escolhe o mais próximo.
- [ ] POS: vender produto → confirmar Lote/Validade FEFO mostra o lote correcto e o movimento consome-o.
- [ ] Encomenda + Fatura: confirmar Lote/Validade FEFO refresca ao mudar armazém.
- [ ] Transferência multi-linha: confirmar colunas FEFO actualizam ao trocar armazém de origem.
- [ ] Autenticar com utilizador antigo (password em texto-plano) → confirmar que entra E que o hash na BD passou a `$2a$...`.
- [ ] Abrir `http://localhost:8080/swagger-ui.html` em dev → confirmar que lista todos os endpoints.

## Por fazer antes de produção real

1. ~~**Gerar V1__init.sql**~~ — já existe baseline `V1__init.sql` + V2..V8 em [db/migration/](src/main/resources/db/migration/). Falta aplicar/validar em PostgreSQL limpo (checklist do harness).
2. ~~**Restringir Security**~~ — **confirmado (2026-06-21)**: o `SecurityInterceptor` já impõe token+empresa+role em todo o `/api/**` (401/403), validado por `SecurityApiIntegrationTest`. O `.anyRequest().permitAll()` do filtro Spring é redundante (a guarda é o interceptor), não uma falha. Endurecer o filtro Spring fica como hardening opcional, não bloqueante.
3. ~~**Endpoints `/api/auth/login`**~~ — existe e está coberto (`AuthControllerIntegrationTest`).
4. ~~**Cobertura de testes** dos Services críticos~~ — **feito (2026-06-20)** para `POSService.checkout/closeSession`, `CreditNoteService`, `DocumentNumberService`. Falta `ComercialService.issueInvoice` e o cenário lote vencido (RS-12).
5. ~~**Backups reais**~~ — confirmado: `BackupService.executeBackup()` faz export real. Restore real continua a exigir ambiente separado (ponto manual do harness).

## Decisões tomadas

- Validades **não** têm tabela autónoma — pertencem sempre a um lote (`ProductBatch`).
- Lote/Validade no UI **read-only** — FEFO decide. Backend volta a aplicar FEFO em transacção mesmo que o UI passe `batchNumber`.
- Spring Security é scaffold **permissivo** por agora — restringir endpoints só quando houver login HTTP real, para não quebrar o desktop que ainda chama Services directamente.
- Flyway desactivado em dev (H2 + `ddl-auto=update`); activo em prod com `validate`.

## Estado de build

```
mvn clean compile   → BUILD SUCCESS
mvn clean test      → BUILD SUCCESS, 86 testes, 0 falhas (2026-06-21)
```

Diagnostics Lombok no IDE (`cannot find symbol: getX()`) são **ruído**. Critério único: `mvn compile`.
