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

## Definition of done (por domínio migrado)

- [ ] Endpoints do `@RestController` cobrem todas as chamadas do painel.
- [ ] `XxxApiClient` criado (`@Component @Profile("desktop")`), DTOs iguais aos do Service.
- [ ] Painel recebe o cliente (não o Service); leitura inicial em `onPanelSelected()`.
- [ ] `MainFrame` injecta e passa o cliente; Services partilhados mantidos onde ainda usados.
- [ ] `mvn -o compile` limpo.
- [ ] Cenário manual TC-5x do painel validado ao vivo (quando o backend estiver de pé).
