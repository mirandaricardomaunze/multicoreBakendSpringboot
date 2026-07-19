# Harness — Desktop cliente-fino (Track B)

Ver [DESKTOP_THIN_CLIENT_SPEC.md](DESKTOP_THIN_CLIENT_SPEC.md).

## Automáticos — `DesktopApiClientTest`

Cobrem a camada partilhada de que **todos** os clientes tipados dependem.

| ID    | Cenário                                                       | Esperado                                                |
|-------|---------------------------------------------------------------|---------------------------------------------------------|
| TC-01 | `get()` com sessão activa (token + empresa)                   | Pedido leva `Authorization: Bearer <token>` e `X-Company-Id`; URI = base+path; corpo parseado no DTO |
| TC-02 | `getList()` sobre um array JSON                               | Lista com todos os elementos parseados                  |
| TC-03 | `post(body)`                                                  | Método POST, `Content-Type: application/json`, resposta parseada |
| TC-04 | Resposta não-2xx com `{"message": "..."}`                     | `ApiClientException` com a mensagem do servidor         |
| TC-05 | Sem sessão (fluxo de login)                                   | Pedido **sem** cabeçalho `Authorization`                |
| TC-06 | `postForList()` sobre um POST que devolve array               | Método POST; lista parseada                             |
| TC-07 | `getBytes()` (ex.: PDF de `/api/print/**`)                    | Corpo binário devolvido; pede `Accept: application/pdf` |

## Automáticos — contexto cliente-fino & fluxos de dinheiro (HTTP real)

Provam, sem intervenção manual, o que antes estava só "a compilar". Correm na suite (`mvn test`).

| ID    | Teste                              | Cenário / Esperado                                                                 |
|-------|------------------------------------|------------------------------------------------------------------------------------|
| TC-08 | `DesktopThinContextTest`           | O contexto Spring do desktop arranca **sem `DataSource`** e sem `@Service`/`@Repository` de backend — só os clientes HTTP. (Prova que o PostgreSQL pode ser fechado ao exterior.) |
| TC-09 | `MoneyFlowHttpIntegrationTest` (fatura) | Login (ADMIN) → semear stock (ENTRADA) → **emitir fatura** desconta stock exactamente; **vender a descoberto → HTTP 400** (`consumeFEFO`+`@Version`); stock intacto após a recusa. Pelos mesmos endpoints/cabeçalhos que o desktop usa. |
| TC-10 | `MoneyFlowHttpIntegrationTest` (POS) | Abrir caixa → **checkout** devolve **`InvoiceDTO`** (id+número+total) e desconta stock. |
| TC-11 | `MoneyFlowHttpIntegrationTest` (encomenda) | Criar encomenda (`POST /orders`) e consultá-la (`GET /orders/{id}` + listagem). |

## Manuais — ida-e-volta HTTP ao vivo (desktop contra backend)

Pré-condição: backend a correr, login feito, empresa activa seleccionada.

| ID     | Cenário                                                                 | Esperado                                                            |
|--------|-------------------------------------------------------------------------|--------------------------------------------------------------------|
| TC-50  | **Aprovações** → abrir o painel                                         | Tabelas Pendentes/Histórico carregam via `/api/approvals`.         |
| TC-51  | **Aprovações** → Aprovar e Rejeitar (com motivo) um pedido             | Decisão persiste; listas refrescam; permissão MANAGER/ADMIN aplicada pelo servidor. |
| TC-52  | **CRM** → abrir; registar Folha de Obra; faturar folha                  | Tickets/folhas via `/api/crm`; criação e faturação persistem.      |
| TC-53  | **Financeiro** → abrir; registar recebimento de fatura aprovada         | Contas/movimentos via `/api/finance`; `pay-invoice` liquida e refresca saldo. |
| TC-54  | **Clientes** → listar, criar, editar, eliminar                         | CRUD via `/api/comercial/clients` (regressão do já migrado).       |
| TC-55  | Qualquer painel migrado com **sessão sem empresa** (superadmin)         | Não rebenta no arranque (carregamento preguiçoso; painel não é aberto). |
| TC-56  | Backend em baixo → abrir um painel migrado                             | Mensagem de erro amigável do `ApiClientException` (não *stacktrace*). |
| TC-57  | **Promoções** (sub-tab de Comercial) → listar; criar promoção (percentagem por produto e por categoria; "leve X, pague Y"); activar/desactivar | Lista via `/api/promotions?companyId=`; produtos via `/api/comercial/products`, categorias via `/api/product-categories?onlyActive=true`; criação/toggle persistem. |
| TC-58  | **Dashboard** (Painel Inicial) → login e navegar | 7 KPIs + 2 gráficos populam via HTTP (tesouraria, faturas, aprovações, tickets, IVA, stock baixo, validades). Compras/stock chegam como **DTOs** (`PurchaseDTO`/`StockDTO`), não entidades. Backend em baixo no arranque não bloqueia o login (dashboard fica a zeros e repovoa ao navegar). |
| TC-59  | **RH** → abrir; CRUD de funcionários; processar folha; marcar recibo pago; faltas; férias (submeter/decidir); despesas; **imprimir recibo (PDF)** | Tudo via `/api/hr/**`; o recibo PDF vem de `/api/print/payslip/{id}` (bytes via `getBytes`) e abre no visualizador. |
| TC-60  | **Fiscal** → apuramento IVA; taxas (CRUD + activar/desactivar); retenções (CRUD + entregar); mapa IRPS/INSS; **declaração IVA (PDF)**; **mapa fiscal salarial (PDF)**; **export SAF-T** (XML + contagem/total); **validar SAF-T** | Tudo via `/api/fiscal/**` e `/api/print/**`. Endpoints **novos**: `GET /api/fiscal/saft/export`, `GET /api/fiscal/saft/validate`, `GET /api/print/payroll-fiscal-map`. |
| TC-61  | **Compras** → fornecedores (criar/editar/activar-desactivar); registar compra (à vista e a crédito); contas a pagar (registar pagamento); encomendas a fornecedor (criar/receber/receber parcial/cancelar); reposição automática | Tudo via `/api/purchases/**`; produtos/contas/armazéns via os clientes respetivos. Fornecedores/compras chegam como DTOs (`SupplierDTO`/`PurchaseDTO`). Activar fornecedor usa `PATCH`. |
| TC-62  | **Plataforma** (superadmin) → empresas (criar/editar/activar); utilizadores globais (criar/editar, conceder/revogar acesso, repor senha, activar); assinaturas (definir plano, mudar estado, registar/ver pagamentos); assistência (ver tickets, responder, mudar estado) | Tudo via `/api/platform/**` (só DTOs, sem empresa). Endpoints **novos** de options: `plan-options`, `method-options`, `status-options`. |
| TC-63  | **Config** (empresa) → utilizadores (criar/editar nome/mudar papel); auditoria (ver registos); **backup** (estado auto, executar lógico/físico, listar, verificar); colunas de documentos; suporte (abrir/responder); a minha assinatura | Users via `/api/users`; auditoria via `/api/audit`; **backup corre no servidor** (`/api/backup`) e a auditoria dos backups é registada server-side (o desktop já não chama `logEvent`). Só ADMIN nos backups. |
| TC-64  | **Stock** → níveis (filtros armazém/estado/categoria/pesquisa); alertas (esgotados + validades); lotes (FEFO, entrada por caixas); movimentos; **ajuste/inventário**; **inventário físico** (nova/abrir/guardar/aplicar/cancelar contagem); transferências (criar/aprovar/rejeitar/**imprimir guia PDF**); armazéns (criar/editar/activar); categorias (CRUD + activar); **cadastrar/editar produto** (com imagem + IVA); **etiquetas PDF**; **imprimir inventário PDF**; trancar/destrancar stock (contagem cega) | Tudo via `/api/inventory/**`, `/api/comercial/**`, `/api/product-categories`, `/api/print/**`. PDFs (inventário, folha de contagem, etiquetas, guia de transferência) via `getBytes`; imagem do produto via `postBytes` (octet-stream). `Stock`/`Warehouse`/`StockMovement` chegam como DTOs enriquecidos. **Concorrência:** vender a descoberto é recusado pelo servidor (`consumeFEFO` + `@Version` em `Stock`/`ProductBatch`) — confirmar que um 2.º utilizador a esgotar o mesmo saldo recebe "Stock insuficiente". |

| TC-65  | **POS** → abrir caixa (fundo inicial); adicionar por seletor e por **leitor de código de barras** (incl. balança peso/preço); promoção aplicada à linha; **checkout** (numerário com troco, cartão, **fiado/crédito**); cliente walk-in vs registado; **imprimir recibo (PDF)**; suprimento/sangria; **fechar caixa** + **fecho Z (PDF)**; histórico de vendas + **reimprimir recibo**; **devolver/trocar** (nota de crédito) | Sessões/checkout/devoluções via `/api/pos/**`; barcode/vendáveis/pos-sales via `/api/comercial/**`; armazéns-de-venda via `/api/inventory/warehouses/sales`; melhor-promoção via `/api/promotions/best`; recibo `/api/print/receipt/{id}` e Z `/api/print/pos-z-report/{id}` via `getBytes`. `checkout` devolve **`InvoiceDTO`** (número+total). **Concorrência:** vender a descoberto é recusado pelo servidor (mesma garantia do Stock — `consumeFEFO`+`@Version`). Sessão activa: 204 → sem caixa aberta. |

| TC-66  | **Comercial** → **Faturação** (nova fatura multi-linha com IVA/desconto/lote; imprimir fatura e guia PDF; anular); **Recibos** (liquidar fatura; anular); **Encomendas** (criar; pendentes; faturar→FT; cancelar; imprimir/marcar impressa); **Notas de crédito** (criar/aprovar/rejeitar/PDF; quantidades já devolvidas); **Notas de débito** (criar/aprovar/rejeitar/PDF); **Movimentos** (mapa unificado com pesquisa/período); pagamento de **fiado** (late-payment) | Faturas/encomendas/recibos via `/api/comercial/**`; notas via `/api/credit-notes` e `/api/debit-notes`; movimentos via `/api/movimentos`; PDFs via `/api/print/**` (getBytes). `ReceiptDTO`/`OrderDTO`/`ReturnedQtyDTO`; `Company` só id (stub) no export de tabela. Emissão de fatura mantém a garantia de stock do servidor (consumeFEFO+@Version). |

## Definition of done (por domínio migrado)

- [ ] Endpoints do `@RestController` cobrem todas as chamadas do painel.
- [ ] `XxxApiClient` criado (`@Component @Profile("desktop")`), DTOs iguais aos do Service.
- [ ] Painel recebe o cliente (não o Service); leitura inicial em `onPanelSelected()`.
- [ ] `MainFrame` injecta e passa o cliente; Services partilhados mantidos onde ainda usados.
- [ ] `mvn -o compile` limpo.
- [ ] Cenário manual TC-5x do painel validado ao vivo (quando o backend estiver de pé).
