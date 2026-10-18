# Robustez multiutilizador e capacidade operacional

Estado: canonico  
Data: 2026-10-11

## Objectivo

Garantir que varios postos podem operar simultaneamente sem duplicar documentos, vender stock
inexistente, perder actualizacoes, misturar empresas ou derrubar o backend por saturacao. Esta SPEC
nao altera regras fiscais, contabilisticas, de stock, caixa ou pagamentos; define as proteccoes
tecnicas que envolvem essas regras.

## Invariantes obrigatorios

1. O desktop continua cliente fino e nunca guarda estado de negocio partilhado ou acede a BD.
2. O backend HTTP permanece stateless; identidade e empresa activa sao resolvidas por pedido.
3. Toda operacao critica e atomica numa transaccao de Service.
4. Stock, lotes, tesouraria e documentos editaveis detectam actualizacoes concorrentes com
   `@Version`, lock de BD ou restricao unica adequada.
5. A numeracao de documentos e serializada na BD por empresa, serie e ano. Locks em memoria ou
   `synchronized` nao podem ser a garantia de integridade, pois deixam de funcionar com replicas.
6. Repeticao de uma operacao de rede nao pode criar um segundo efeito. Fluxos que ja recebem uma
   chave de idempotencia devem preserva-la; novos comandos criticos devem ganhar chave persistida
   antes de serem considerados prontos para escala horizontal.
7. Conflitos de versao devolvem HTTP 409 com mensagem accionavel. Falhas transitórias so podem ser
   repetidas quando cada tentativa executar numa nova transaccao.
8. Listagens operacionais crescentes usam paginacao server-side. Endpoints legados sem paginacao
   ficam apenas por compatibilidade e nao devem ser usados por novos ecras.
9. Capacidade e configurada por ambiente e validada por carga; aumentar threads ou pool sem medir a
   BD e proibido.
10. Readiness inclui a capacidade de servir dependencias; liveness nao deve reiniciar o processo
    apenas porque a BD teve uma falha transitoria.

## Baseline de producao

- PostgreSQL privado e Flyway dono do schema.
- Hikari: `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` e `DB_POOL_CONNECTION_TIMEOUT_MS`.
- Tomcat: `HTTP_MAX_THREADS`, `HTTP_MIN_SPARE_THREADS`, `HTTP_MAX_CONNECTIONS` e
  `HTTP_ACCEPT_COUNT`.
- Shutdown gracioso com `SHUTDOWN_TIMEOUT`.
- Actuator interno: health, readiness, liveness, metrics e Prometheus. O Caddy publico continua a
  encaminhar exclusivamente `/api/*`.
- Uma instancia e o baseline funcional. Alta disponibilidade exige pelo menos duas replicas e
  todas as garantias criticas na BD, nunca em memoria local.

## Matriz de concorrencia

| Area | Garantia minima | Evidencia permanente |
|---|---|---|
| Numeracao | lock pessimista por empresa/serie/ano e unique composta | harness + teste do service |
| POS/factura | transaccao, stock versionado, retry transitorio e idempotencia | harness + integracao |
| Sessão POS | mutex de empresa na abertura; lock da sessão no fecho | harness + relatório Z |
| Stock/lotes | `@Version`; reserva com `PESSIMISTIC_WRITE` | harness + integracao concorrente |
| Tesouraria | `@Version`, transaccao e auditoria | harness + integracao concorrente |
| Recebimentos | lock da factura/recibo antes de saldo, estado ou tesouraria | harness + testes comerciais |
| Mestres/documentos editaveis | `@Version` e HTTP 409 | harness de optimistic locking |
| Multiempresa | empresa derivada do utilizador e queries tenant-scoped | testes de isolamento |

## Gates de capacidade

### Consultas de contas a receber

- A verificação de crédito agrega o saldo na BD por empresa, cliente e estados derivados de
  `InvoiceStatus.collectableStatuses()`, sem carregar entidades. Saldo negativo conta como zero,
  exactamente como `Invoice.outstandingAmount()`; não há cache de dívida.
- O mapa de antiguidade lê apenas documentos cobráveis com saldo positivo e obtém o cliente na
  mesma consulta, sem carregar linhas de factura nem consultar cada cliente separadamente.
- `ReceivablesQueryHarnessTest` executa as consultas numa BD H2 isolada, compara o agregado com
  a regra do domínio para todos os estados e verifica o número de statements/entidades carregadas.
  Isto não substitui o ensaio de carga PostgreSQL nem prova concorrência na concessão de crédito.

Nenhum numero de utilizadores pode ser anunciado sem ensaio PostgreSQL. O ensaio deve misturar
login, pesquisa paginada, POS, factura, reserva de stock, pagamento e relatorio, registando:

- taxa de sucesso e duplicacoes (duplicacoes aceites: zero);
- stock/tesouraria/numero finais, reconciliados com os pedidos confirmados;
- latencia p50, p95 e p99;
- conflitos 409, retries e deadlocks;
- Hikari activo/pendente, threads HTTP, CPU, memoria e queries lentas.

Gate inicial recomendado: 50 utilizadores activos durante 30 minutos, p95 inferior a 2 segundos nas
operacoes normais, nenhum 5xx inesperado, nenhuma duplicacao e reconciliacao exacta. O teste progride
para 100 apenas depois de o gate anterior passar.

## Divida controlada e sequencia de adopcao

1. Converter as listagens ainda completas para endpoints paginados, mantendo contratos legados.
2. Generalizar chave de idempotencia persistida para comandos criticos ainda sem ela.
3. Acrescentar integracoes concorrentes reais em PostgreSQL para cada linha da matriz.
4. Automatizar carga fora da suite Maven funcional e guardar resultados por versao.
5. So depois dimensionar pool/threads e activar duas ou mais replicas.

Cada conversao deve ser uma alteracao pequena do dominio dono, com teste proprio. Nao se faz uma
reescrita massiva que misture regras de POS, stock, fiscalidade e caixa.

### Progresso da adopcao

- Tesouraria: `GET /api/finance/transactions/page` pagina na base de dados, aplica limite global de
  200 registos e preserva `GET /api/finance/transactions` para desktops anteriores. Integrado na UI
  do `FinanceiroPanel` (aba Histórico de Fluxo de Caixa) através de `TablePager` com desativação
  da paginação em memória. Protegido por `TreasuryPaginationHarnessTest`.
- Stock: `GET /api/inventory/movements/page` pagina o histórico na base de dados, mantém isolamento
  por empresa e preserva `GET /api/inventory/movements`. Integrado na UI do `StockPanel` (aba
  Movimentos & Rastreabilidade) através de `TablePager`. Protegido por
  `StockMovementPaginationHarnessTest`.
- Movimentos agregados: diagnosticado; exige projecao SQL/indice de movimentos sobre oito origens.
  Cortar uma lista ja materializada nao conta como paginacao e e expressamente proibido.
- Recebimentos: liquidação directa, emissão de recibo e anulação de recibo usam locks pessimistas
  tenant-scoped antes de validar saldo/estado ou movimentar tesouraria. Protegido por
  `InvoicePaymentConcurrencyHarnessTest`.
- Sessões POS: a abertura bloqueia a empresa antes de verificar/criar a sessão activa; o fecho
  bloqueia a sessão tenant-scoped antes do estado, reconciliação e depósito. Protegido por
  `PosSessionConcurrencyHarnessTest` e `PosZReportHarnessTest`.
- Encomendas de Cliente: `GET /api/comercial/orders/page` pagina na base de dados com `LEFT JOIN FETCH`
  do cliente e unidade de negócio, ordenado por data decrescente. Integrado na UI do `ComercialPanel` /
  `CommercialOrdersView` através de `TablePager` com desativação da paginação em memória. Protegido por
  `OrderPaginationHarnessTest`.
- Índices Compostos de Elevada Escala: migração `V90__scalability_performance_composite_indexes.sql`
  adiciona índices B-Tree compostos tenant-scoped (`company_id` + data/estado/série) sobre `invoices`,
  `customer_orders`, `stock_movements`, `stocks`, `treasury_transactions`, `purchases`, `journal_entries` e
  `product_companies`. A V90 foi corrigida ainda local, após confirmação do utilizador: datas de
  facturas/encomendas são `created_at`; produtos ligam-se ao tenant via `product_companies`.
  Inclui `(company_id, client_id, status)` para crédito. O harness aplica o SQL ao schema JPA H2 e
  verifica reaplicação; planos PostgreSQL e a cadeia completa Flyway ainda exigem validação própria.
- JDBC Batching no Hibernate: ativado em `application.properties` com `spring.jpa.properties.hibernate.jdbc.batch_size=25`,
  `order_inserts=true`, `order_updates=true` e `batch_versioned_data=true`. Configuração não é prova
  de ganho: entidades com IDs `IDENTITY` não beneficiam de batching de inserts como entidades com
  sequência. Medir statements e latência antes de anunciar redução de round-trips.

