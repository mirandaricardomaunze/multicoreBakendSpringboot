# Tarefa Actual

> Ponteiro da sessão. A IA lê-o no início e actualiza-o sempre que uma fase fecha. ≤1 página. Histórico no `git log`.

**Última actualização:** 2026-07-13
**Estado:** software de loja concluído. **Em curso: hospedar o backend à parte (VPS+Docker+PostgreSQL) +
migrar o desktop para cliente-fino (HTTPS-only).** A fonte de verdade operacional é
[tasks/retail_store_readiness.md](retail_store_readiness.md).

### Progresso — 2026-07-13 (Hospedagem do backend + desktop cliente-fino — Track B)

- **Decisão do utilizador:** hospedar o backend Spring Boot separadamente em **VPS + Docker** com
  **PostgreSQL** privado, e migrar o desktop para **cliente-fino (só HTTPS)** — a BD nunca exposta.
- **Deploy (Track A) — feito:** [Dockerfile](../Dockerfile) multi-stage (inclui `postgresql-client`
  para o backup físico), [docker-compose.yml](../docker-compose.yml) (backend + PostgreSQL privado +
  Caddy TLS automático), [Caddyfile](../Caddyfile), `.env.example`, `.dockerignore`. Guião +
  checklist de hardening: [docs/DEPLOY_VPS_SPEC.md](../docs/DEPLOY_VPS_SPEC.md). **Não testado ao vivo**
  (sem Docker nesta máquina); o `SecurityConfig` fica permissivo (item #1 de go-live).
- **Migração para cliente-fino (Track B) — arrancou:** padrão provado (inclui **PDF-over-HTTP** e o
  1.º painel gigante); **10 de ~26 domínios** a passar por HTTP em vez de chamar o Service em processo:
  - Novos clientes `@Profile("desktop")`: `ApprovalApiClient`, `CRMApiClient`, `FinanceApiClient`,
    `PromotionApiClient`, `InventoryApiClient`, `PurchaseApiClient`; `ComercialApiClient` +=
    `getAllInvoices/getAllProducts/getActiveCategories`. Painéis migrados: Aprovações, CRM, Financeiro,
    Promoções, **Dashboard**, **RH** (Clientes já estava). O Dashboard passou a consumir **DTOs** em vez
    de entidades JPA (`StockDTO`/`PurchaseDTO`) e tem arranque resiliente; `ApprovalService`/`CRMService`/
    `HRService`/`PayslipPrintService` saíram do `MainFrame` (já sem painel a usá-los).
  - **PDF-over-HTTP:** `DesktopApiClient` ganhou `getBytes` (GET→PDF) e `postForList` (POST→array). Como
    os endpoints `/api/print/**` já existiam, o recibo de salário do RH imprime via
    `/api/print/payslip/{id}`. Padrão pronto para as impressões dos restantes painéis.
    Testes: `DesktopApiClientTest` passou a **7** (TC-06 postForList, TC-07 getBytes).
  - **Fiscal** (8.º): `FiscalApiClient` colapsa os 8 serviços do painel. **1.º domínio a exigir endpoints
    novos no backend** — `GET /api/fiscal/saft/export` (DTO com metadados), `GET /api/fiscal/saft/validate`
    (XSD) e `GET /api/print/payroll-fiscal-map` (PDF). Harness TC-60.
  - **Compras** (9.º, **1.º gigante** — 1.324 linhas): `PurchaseApiClient` estendido colapsa
    purchase+order+reorder; `DesktopApiClient` ganhou `patch` (PATCH do estado do fornecedor);
    `InventoryApiClient` += armazéns. O painel converteu `Supplier`/`Warehouse`/`Purchase` (entidades)
    para DTOs (nome do armazém resolvido por lookup, pois o `PurchaseDTO` só traz o id). Harness TC-61.
  - **Plataforma** (10.º, superadmin): `PlatformApiClient` colapsa empresas+utilizadores+assinaturas+
    suporte (~22 métodos, só DTOs). 3 endpoints novos de options (plan/method/status). `/api/platform/**`
    não precisa de empresa. `PlatformCompanyService`/`PlatformUserService` saíram do `MainFrame`. Harness TC-62.
  - Carregamento passou para `onPanelSelected()` (nunca no construtor) para não rebentar sem empresa.
  - **Falta:** médios (Promoções, Fiscal, RH, Dashboard) e os grandes (POS/Stock/Compras/Comercial),
    que precisam de endpoints novos. Só se fecha o PostgreSQL ao exterior quando **todos** migrarem.
- Spec/harness: [docs/DESKTOP_THIN_CLIENT_SPEC.md](../docs/DESKTOP_THIN_CLIENT_SPEC.md) +
  [docs/DESKTOP_THIN_CLIENT_HARNESS.md](../docs/DESKTOP_THIN_CLIENT_HARNESS.md) (TC-01..05 auto,
  TC-50..56 manuais).
- **Verificação:** `mvn -o compile` limpo; `DesktopApiClientTest` (5) verde. Ida-e-volta HTTP real de
  cada painel valida-se ao vivo (manual) quando o backend estiver de pé.

### Progresso — 2026-07-11 (Polish Visual PHC — aspecto ERP profissional)

- **`SlimScrollBarUI`** (novo): scroll bars finas (6 px), thumb violeta arredondado, sem setas — aplicado via `UIHelper.styleScrollPane()` em todos os JScrollPane do sistema.
- **`StatusBar`** (novo): rodapé de 24 px com módulo activo · nº registos · empresa · utilizador · hora. Timer interno (60 s) actualiza a hora. `MainFrame.navigate()` actualiza o módulo; `applyAuthenticatedUser` inicializa empresa e utilizador.
- **`SectionHeader`** (novo): cabeçalho de secção reutilizável — ícone opcional + título + separador 1 px.
- **`ModernPanel`** (melhorado): borda usa `UIHelper.BORDER` em painéis normais (adapta ao tema claro/escuro); painéis com gradiente mantêm branco translúcido subtil.
- **`styleTabbedPanePHC`** (novo em `UIHelper`): tabs PHC com linha de acento 3 px na base (sem fundo cheio). Migrado em **10 painéis**: ApprovalsPanel, ComercialPanel, ComprasPanel, ConfigPanel, CRMPanel, FinanceiroPanel, FiscalPanel, HRPanel, PlataformaPanel, StockPanel.
- **Tooltips premium** em `UIHelper.initGlobalTheme()`: fundo `BG_CARD`, borda `BORDER`, fonte 12 px, delay 600 ms.
- Spec: [docs/PHC_UI_POLISH_SPEC.md](../docs/PHC_UI_POLISH_SPEC.md) | Harness: [docs/PHC_UI_POLISH_HARNESS.md](../docs/PHC_UI_POLISH_HARNESS.md).
- **Verificação:** `mvn -o compile` → BUILD SUCCESS.


- **Filtros de tabela transversais:** o componente `com.phcpro.gui.components.TableFilter` (pesquisa
  com lupa + funil + dropdowns tipo/estado + período por data, colunas ordenáveis) foi estendido a
  **todas** as tabelas de listagem que ainda não tinham: Comercial (NC/ND/Recibos/Encomendas/Contas
  Correntes), Compras (Faturas/Fornecedores/Reposição/Contas a Pagar/Encomendas), Clientes, Stock →
  Gestão de Armazéns, RH (5 tabelas), Fiscal (Taxas/Retenções), Config (Auditoria/Utilizadores/
  Suporte), POS (Histórico de Vendas), Promoções. Acções que indexam a selecção passam por
  `TableFilter.selectedModelRow(...)` (o sorter faz a vista divergir do modelo). Fornecedores/
  Encomendas/Clientes migraram de pesquisa server-side/própria para o filtro cliente.
- Spec/harness: [docs/TABELAS_FILTROS_SPEC.md](../docs/TABELAS_FILTROS_SPEC.md) +
  [docs/TABELAS_FILTROS_HARNESS.md](../docs/TABELAS_FILTROS_HARNESS.md) (FT-01..07 auto, FT-50..68 manuais).
- **Verificação:** `mvn -o compile` limpo; `TableFilterTest` (7) verde; render confirmado ao vivo
  (Vendas → NC/Recibos, Compras → Faturas). Commit `a3ca84f`.
- **Backup/restore (Fase 6) — avançado:** validado ponta-a-ponta **exceto o apply final**, de forma
  não-destrutiva sobre a BD viva (PostgreSQL 18): `pg_dump -Fc` OK (58/58 tabelas, 520 objetos),
  `pg_restore --list` OK ⇒ arquivo completo e restaurável. **Falta** o passo BR-50..54 (restaurar em
  BD limpa + comparar contagens), que exige role com `createdb`/superuser — a role `multicore` não
  tem. É um passo manual de ~3 comandos (documentado no handover).

### Progresso — 2026-07-05 (Assinante: vista própria + alertas de expiração)

- **Vista do assinante** (só-leitura, tenant-scoped): `SubscriptionService.getMySubscription()` +
  `GET /api/subscription/me` → `MySubscriptionDTO` (plano, estado, validade, **dias restantes**,
  mensalidade). Desktop: aba **"A Minha Assinatura"** no `ConfigPanel`.
- **Alertas (7 dias)**, severidade única (vermelho expirada/suspensa; amarelo ≤7 dias): (1) aviso no
  login (`MainFrame.checkSubscriptionOnStartup`, disparado no `DesktopLauncher`); (2) chip permanente
  na barra de topo (só em risco); (3) linhas coloridas na aba Assinaturas do superadmin. À prova de
  falha (leitura falha ⇒ sem alerta, UI não rebenta).
- Spec/harness: SB-06 auto + SB-55..59 manuais.
- **Também corrigido no arranque real** (bugs que só aparecem a correr): colisão de bean
  (`PlatformSupportTicketRepository`), colisão de nome de entidade (`@Entity("PlatformSupportTicket")`)
  e crash da `MainFrame` no login do superadmin (`ClientesPanel` deixou de carregar no construtor).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (6) verde; app corre em
  PostgreSQL real (Flyway V24–V26 aplicadas).

### Progresso — 2026-07-05 (Superadmin — Fase 4: assistência) — funcionalidade fechada

- Módulo `support` (migração `V26`), distinto do `crm`: `SupportTicket` (assunto, descrição, estado
  OPEN/IN_PROGRESS/RESOLVED/CLOSED, prioridade, responsável) + `SupportMessage` (conversa;
  `fromSuperAdmin`). `SupportService` com **dois lados**: empresa (MANAGER/ADMIN, tenant-scoped) abre
  e responde; superadmin vê todos, responde (assume + OPEN→IN_PROGRESS) e muda estado. Resposta da
  empresa a RESOLVED reabre; CLOSED bloqueia. Controllers `/api/support/tickets` (tenant) e
  `/api/platform/support/tickets` (superadmin).
- **Desktop:** aba "Assistência" no `PlataformaPanel` (superadmin) + aba "Suporte à Plataforma" no
  `ConfigPanel` (empresa abre/consulta/responde).
- Spec/harness: ST-01..05 auto, ST-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `SupportServiceTest` (5) + `PlatformUserServiceTest` (5) +
  `PlatformCompanyServiceTest` (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Superadmin completo (Fases 1–4).** Assinaturas continuam manuais (sem gateway M-Pesa/e-Mola);
  esse é o próximo passo natural se se quiser cobrança automática.

### Progresso — 2026-07-05 (Superadmin — Fase 3: utilizadores globais)

- **Corte vertical.** `PlatformUserService` (SUPERADMIN + auditado) dá a visão de **todas** as
  empresas: `listUsers`, `createUser` (liga a empresa/papel), `setUserActive` (não desactiva o
  superadmin), `resetPassword`, `grantAccess`, `revokeAccess` (**protege o último ADMIN**). Controller
  `/api/platform/users`. `AppUser.revokeCompany` novo (orphanRemoval).
- **Desktop:** aba "Utilizadores" no `PlataformaPanel` (Novo, Conceder/Alterar Acesso, Revogar,
  Repor Senha, Activar/Desactivar).
- Spec/harness: SU-01..05 auto, SU-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `PlatformUserServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Próxima:** Fase 4 (tickets `support` empresa→superadmin + abrir tickets no lado da empresa).

### Progresso — 2026-07-04 (Superadmin — Fase 2: assinaturas + pagamentos)

- **Corte vertical** (backend + UI). Módulo `subscription` (migração `V25`): `Subscription` (1:1 com
  empresa; plano TRIAL/BASIC/PRO/ENTERPRISE, estado TRIAL/ACTIVE/SUSPENDED/EXPIRED, validade, preço;
  `effectiveStatus()` deriva EXPIRED da validade) + `SubscriptionPayment` (valor, método
  DINHEIRO/MPESA/EMOLA/TRANSFERENCIA/OUTRO, período, nota). `SubscriptionService` (SUPERADMIN +
  auditoria): `listOverview/saveSubscription/changeStatus/recordPayment/listPayments` + `allowsLogin`
  (política interna). Registar pagamento **estende a validade** e reactiva. Controller
  `/api/platform/subscriptions`.
- **Login:** passa a filtrar por `allowsLogin` além de `company.active` — assinatura expirada/suspensa
  bloqueia; sem assinatura continua acessível.
- **Desktop:** aba "Assinaturas & Pagamentos" no `PlataformaPanel` (Definir Plano/Validade, Registar
  Pagamento, Ver Pagamentos, Suspender/Reactivar).
- Spec/harness actualizados (SB-01..05 auto, SB-50..54 manuais).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**.
- **Próximas:** Fase 3 (utilizadores globais), Fase 4 (tickets `support`).

### Progresso — 2026-07-04 (Superadmin / Consola da Plataforma — Fase 1)

- **Pedido do utilizador:** um **superadmin** (dono da plataforma) que vê todas as empresas,
  activa/desactiva, gere utilizadores/assinaturas/pagamentos e dá assistência. Decisões: aba
  escondida no mesmo app (papel `SUPERADMIN`); pagamentos **manuais**; assistência por **tickets**
  empresa→superadmin; empresa suspensa **bloqueia o login** (não mata sessões vivas).
- **Processo:** spec+harness →
  [docs/SUPERADMIN_PLATAFORMA_SPEC.md](../docs/SUPERADMIN_PLATAFORMA_SPEC.md) +
  [docs/SUPERADMIN_PLATAFORMA_HARNESS.md](../docs/SUPERADMIN_PLATAFORMA_HARNESS.md) (SA-01..04 auto,
  SA-50..56 manuais). Entregue **por fases**; esta é a **Fase 1**.
- **Fase 1 (feita):** `AppUser.platformAdmin` + `Company.active` (migração `V24`); seed idempotente
  da conta `superadmin/superadmin`. Autorização: `/api/platform/**` sai do tenant-check do
  `SecurityInterceptor` (exige `platformAdmin`, papel `SUPERADMIN`, sem empresa);
  `PermissionGuard.requireSuperAdmin`; `TenantAccessService.requireSuperAdmin`. Login devolve
  `superAdmin` e só empresas **activas**; utilizador de tenant sem empresa activa é recusado.
  Módulo `platform`: `PlatformCompanyService` (listar/criar/editar/activar-desactivar, auditado) +
  controller `/api/platform/companies` + DTOs. Desktop: sessão transporta `superAdmin`;
  `PlataformaPanel` (aba Empresas: tabela + Novo/Editar/Activar-Desactivar); `MainFrame` mostra só a
  aba "Plataforma" ao superadmin (sem seletor de empresa/abas de tenant). Logout excluído do
  interceptor (superadmin não tem empresa).
- **Próximas fases:** 2 (assinaturas+pagamentos), 3 (utilizadores globais), 4 (tickets `support`),
  5 (painéis Pagamentos/Utilizadores/Assistência + abrir tickets no lado da empresa).
- **Verificação:** `mvn -o compile` limpo; `PlatformCompanyServiceTest` (4) +
  `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**. Suite completa
  não corre por falta de RAM (limitação de ambiente, como iterações anteriores).

### Progresso — 2026-07-04 (config separada por tipo de documento + comentário do recibo)

- **Pedido do utilizador:** o **POS** deve ter **configuração separada** dos documentos comerciais, e
  poder **definir o comentário/rodapé** que aparece no recibo.
- Config passou a ser **por `DocumentType`** (COMMERCIAL vs POS_RECEIPT), independente por empresa
  (entidade ganha `document_type`; unique `(company_id, document_type)`; migração `V23` recria a
  tabela de forma portável H2+PostgreSQL). `DocumentColumnsDTO` ganha `footer`. Service/controller/UI
  passam o tipo. Os 4 serviços comerciais usam COMMERCIAL; o `ReceiptPrintService` usa POS_RECEIPT e o
  rodapé configurável (`footer`; vazio = "Obrigado pela sua preferência!", suporta multi-linha).
  `ConfigPanel` ganhou selector de tipo + campo "Comentário do recibo".
- Spec/harness actualizados (DC-07 auto; DC-54..DC-57 manuais).
- **Verificação:** compila; testes da funcionalidade + serviços tocados → **56, 0 falhas**
  (`DocumentConfigServiceTest` 6, `LineItemsTableRendererTest` 4, POS/Comercial/Reorder/Inventory).
  ⚠️ A suite **completa** (209) não correu por **falta de RAM da máquina** (~568 MB livres) nos testes
  de integração Spring — limitação de ambiente, não de código (correr num ambiente com mais memória).

### Progresso — 2026-07-04 (colunas configuráveis dos documentos comerciais)

- **Pedido do utilizador:** poder definir **quais colunas** aparecem nos documentos comerciais
  (Fatura/Encomenda/NC/Guia, que partilham o `LineItemsTableRenderer`). Só mostrar/ocultar.
- **Processo:** spec+harness → skill `phc-new-module` → **implementação delegada a um agent** →
  revisão `phc-solid-review` (sem apontamentos bloqueantes) → verificação e commit.
- **Módulo `documents`** (`DocumentColumnConfig` por empresa, 8 flags, migração `V22`;
  `DocumentConfigService.getColumns/save` com MANAGER/ADMIN + auditoria `DOCUMENT_COLUMNS_UPDATE` +
  regra "pelo menos uma coluna"; `DocumentColumnsDTO` record; controller `GET/PUT /api/documents/columns`).
  `LineItemsTableRenderer` ganhou overload `build(rows, cols)` (extraiu `record Column` + `activeColumns`,
  **melhor DRY**; `build(rows)` delega em `all()`, retrocompatível). Os 4 serviços de impressão passam a
  config. UI: aba "Colunas dos Documentos" no `ConfigPanel` (8 checkboxes + Guardar) + wiring no `MainFrame`.
- Spec/harness: [docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md) +
  [docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md) (DC-01..06 auto, DC-50..53 manuais).
- Testes: `DocumentConfigServiceTest` (5) + `LineItemsTableRendererTest` (+1). `mvn clean test` → **209, 0 falhas**.
- **Recibo do POS (extensão):** `ReceiptPrintService` passou a respeitar a mesma config no que cabe
  num recibo térmico — Qtd e Preço Unit. como colunas opcionais, Referência/Código de Barras como
  sublinha do nome; Descrição e Total sempre; Validade/IVA/subtotal por linha não aplicáveis.
  Harness DC-54/DC-55. `mvn test` → **209, 0 falhas** (recibo continua sem teste automático, como os
  restantes print services; validação manual).

### Progresso — 2026-07-04 (campos profissionais do armazém)

- **Pedido do utilizador:** `Warehouse` ganha (migração `V21`) **active**, **type**
  (`WarehouseType`: Loja/Depósito/Central/Trânsito), **allowsSales**, **manager**, **phone**.
  `getWarehousesByCompany` passa a filtrar **inactivos**; novo `getSalesWarehousesByCompany`
  (activo + allowsSales) usado pelo **POS** (deixa de vender de depósito). Diálogo "Criar Armazém"
  com Tipo/Responsável/Telefone/Permite vendas; `createWarehouse` overload novo (antigos delegam,
  retrocompatível).
- Spec/harness: [docs/ARMAZEM_PROFISSIONAL_SPEC.md](../docs/ARMAZEM_PROFISSIONAL_SPEC.md) +
  [docs/ARMAZEM_PROFISSIONAL_HARNESS.md](../docs/ARMAZEM_PROFISSIONAL_HARNESS.md) (AR-01 auto, AR-50..53 manuais).
- Testes: `InventoryServiceTest` +1 (filtro de vendas). `mvn test` → **203, 0 falhas**.
- **Ecrã de gestão de armazéns (feito):** nova aba "Gestão de Armazéns" no `StockPanel` (tabela com
  todos + Novo/Editar/Activar-Desactivar, duplo-clique edita). Backend `getAllWarehousesByCompany`,
  `updateWarehouse`, `setWarehouseActive` (MANAGER/ADMIN + auditoria). Diálogo criar/editar partilhado.
  Harness AR-53..AR-55.

### Progresso — 2026-07-03 (polish: cor de estado nas linhas)

- **Pedido do utilizador:** leitura de estado **por linha**. `UIHelper.styleTable` deteta uma coluna
  "Estado"/"Situação"/"Status" e pinta a **linha** com tom subtil (blend ~18% com a zebra, adapta ao
  tema). Vocabulário semântico centralizado em `statusColorFor` (verde/amarelo/vermelho, PT/EN de
  retalho, inclui ESGOTADO/BAIXO/EM STOCK/ANULADO/EM DÍVIDA…). Colunas "Estado" acrescentadas a
  **Níveis de Stock** e **Reposição**. Automático/DRY para qualquer tabela com coluna de estado.
- Só apresentação. Spec/harness: [docs/COR_ESTADO_LINHAS_SPEC.md](../docs/COR_ESTADO_LINHAS_SPEC.md) +
  [docs/COR_ESTADO_LINHAS_HARNESS.md](../docs/COR_ESTADO_LINHAS_HARNESS.md) (CE-01..05 manuais).
  `mvn test` → **202, 0 falhas**.

### Progresso — 2026-07-03 (preço grosso vs retalho — por produto + qtd mínima)

- **Sugestão do utilizador (3/3):** `Product` ganha `wholesalePrice` + `wholesaleMinQty` (migração
  `V20`). Regra pura `Product.effectiveUnitPrice(qty)` — aplica grosso quando `qty ≥ min`; senão
  retalho. **Aplicada nos 3 fluxos** (`createInvoice`, `createOrder`, `POSService.checkout`) sem
  tocar no `LineCalculator` (recebe o preço já resolvido; IVA por unidade). `ProductDTO` +2 campos;
  diálogos Cadastrar/Editar Produto com "Preço Grosso" e "Qtd mín. grosso" (opcionais). Retrocompatível
  (createProduct/updateProduct antigos delegam com grosso null).
- Spec/harness: [docs/PRECO_GROSSO_SPEC.md](../docs/PRECO_GROSSO_SPEC.md) +
  [docs/PRECO_GROSSO_HARNESS.md](../docs/PRECO_GROSSO_HARNESS.md) (PG-01/02 auto, PG-50..53 manuais).
- Testes: `ComercialServiceTest` +2 (grosso/retalho por quantidade). `mvn test` → **202, 0 falhas**.
- **As 3 sugestões pedidas ficaram concluídas** (reposição automática, Mobile Money, preço grosso).

### Progresso — 2026-07-03 (Mobile Money: M-Pesa / e-Mola)

- **Sugestão do utilizador (2/3):** `PaymentMethod` ganha **MPESA** e **EMOLA**, tratados como
  **electrónicos** (entram na tesouraria, não na gaveta; exigem conta; guardam a **referência** da
  transação em `PaymentEntry.reference`). Sem migração (enum é STRING). Devolução também reembolsa
  por tesouraria. Recibo mostra "M-Pesa"/"e-Mola". UI POS `askPayment` com os métodos + campo
  Referência; diálogo de devolução idem.
- Spec/harness: [docs/MOBILE_MONEY_SPEC.md](../docs/MOBILE_MONEY_SPEC.md) +
  [docs/MOBILE_MONEY_HARNESS.md](../docs/MOBILE_MONEY_HARNESS.md) (MM-01 auto, MM-50..53 manuais).
- Testes: `POSServiceTest` +1 (MPESA → tesouraria, não gaveta). `mvn test` → **200, 0 falhas**.
- **A seguir (3/3):** preços grosso vs retalho.

### Progresso — 2026-07-03 (reposição automática de stock — nova funcionalidade)

- **Sugestão do utilizador (1/3):** novo `ReorderService.suggestions(companyId)` — lista de produtos
  **abaixo do stock mínimo** (soma de todos os armazéns; produto sem stock = 0), com quantidade a
  encomendar **arredondada a caixas inteiras** (`unitsPerBox`), ordenada por urgência. Leitura pura
  (não cria encomendas). **API** `GET /api/purchases/reorder-suggestions`; **UI** nova aba
  "Reposição" no `ComprasPanel` (botão "Criar Encomenda" salta para a aba de encomendas).
- Spec/harness: [docs/REPOSICAO_AUTOMATICA_SPEC.md](../docs/REPOSICAO_AUTOMATICA_SPEC.md) +
  [docs/REPOSICAO_AUTOMATICA_HARNESS.md](../docs/REPOSICAO_AUTOMATICA_HARNESS.md) (RA-01..03 auto, RA-50..52 manuais).
- Testes: `ReorderServiceTest` (3). `mvn test` → **199, 0 falhas**.
- **A seguir (2/3, 3/3):** Mobile Money (M-Pesa/e-Mola) e preços grosso vs retalho.

### Progresso — 2026-07-02 (resiliência das ligações à BD — PC de balcão)

- **Lacuna operacional fechada:** com a app aberta e a máquina em suspensão longa, o pool Hikari
  mantinha ligações **mortas** ao PostgreSQL → gravações falhavam até reiniciar (visto em uso real).
  Config de resiliência no perfil **`desktop`** e espelhada em **`prod`**: `keepalive-time=120s`
  (sonda ligações ociosas), `max-lifetime=600s` (rotação), `connection-timeout=10s` (falha rápida),
  `validation-timeout=5s`; desktop com pool 5/min-idle 1. Sem tocar em código/Services/schema.
- Validado: desktop arranca com Hikari sem avisos de `keepalive/maxLifetime`; `mvn test` 196/0.
- Spec/harness: [docs/RESILIENCIA_LIGACOES_SPEC.md](../docs/RESILIENCIA_LIGACOES_SPEC.md) +
  [docs/RESILIENCIA_LIGACOES_HARNESS.md](../docs/RESILIENCIA_LIGACOES_HARNESS.md) (RL-01..RL-05 manuais).

### Progresso — 2026-07-01 (documento de inventário simplificado)

- **Pedido do utilizador:** o PDF de inventário passou a ter **só 6 colunas** — Referência · Código de
  Barras · Nome · Quantidade · **Caixas** (qtd ÷ und/caixa) · **Valor**. Removidas SKU, Armazém,
  Mínimo, Preço de Compra, Estado. **Valor a preço de VENDA** (`unitPrice`), por linha e no total.
  Rodapé reduzido a "Artigos no inventário" + "VALOR TOTAL DO STOCK". `InventoryReportPrintService`.

### Progresso — 2026-07-01 (venda ao grosso: helper "Caixas" na faturação)

- **Decisão (utilizador vende ao grosso):** a linha da **fatura** ganhou campo opcional **"Caixas"**
  que preenche a **Qtd (unidades) = caixas × unidades/caixa** do produto. **Cálculo de dinheiro
  continua por unidade** (`LineCalculator` intacto, IVA por unidade). **POS não é tocado** (retalho
  rápido à unidade). Entrada directa em unidades mantém-se (campo vazio). `ComercialPanel.applyInvoiceBoxes()`.
- Harness CX-09/CX-10. Helper **replicado na Encomenda a Cliente** (`applyOrderBoxes()`) — mesmo
  comportamento (caixas → Qtd em unidades, dinheiro por unidade).

### Progresso — 2026-07-01 (edição de produtos + entrada de stock por caixas)

- **Editar Produto (lacuna fechada):** existia `createProduct` mas **não havia como actualizar** um
  artigo já cadastrado. Novo `ComercialService.updateProduct(...)` (SKU imutável = identidade;
  referência/código de barras revalidam unicidade **excluindo o próprio**; não toca no stock;
  auditoria `PRODUCT_UPDATE`). **UI:** botão **"Editar Produto"** no topo do `StockPanel` → diálogo
  com selector de produto que **pré-preenche** o formulário (mesmos campos do cadastro, incluindo
  unidades/caixa, IVA, categoria e imagem). Testes: `ComercialServiceTest` 20 → **23**.
- **Entrada de stock por caixas:** ver abaixo.

### Progresso — 2026-07-01 (entrada de stock por caixas)

- **Pedido do utilizador:** dar entrada de mercadoria **por nº de caixas** (a loja arruma às caixas),
  mantendo faturação/reserva/guia/POS **em unidades**. Já existiam `Product.unitsPerBox`, o campo
  "Unidades por Caixa" no cadastro e a coluna "Qtd Caixas" no inventário — faltava o **caminho de
  entrada por caixas**.
- **Decisão:** a unidade interna de stock continua a **UNIDADE** (nada muda a jusante). A caixa é só
  camada de **entrada + visualização**. No `createBatchEntryDialog` (stock inicial **e** "Adicionar
  Lote/Validade" — mesmo método) o operador indica **Nº de Caixas + Unidades soltas**, vê as
  **unidades/caixa** do produto e o **Total (unidades)** calculado em tempo real
  (`total = caixas × unitsPerBox + soltas`); o movimento grava o total em unidades.
- Helpers `parseIntOrZero`/`parseDecimalOrZero` + `UIHelper.onTextChange` para recálculo ao vivo.
  Sem tocar em Services/DTOs (faturação/POS/guia/inventário inalterados).
- Spec/harness: [docs/CADASTRO_POR_CAIXAS_SPEC.md](../docs/CADASTRO_POR_CAIXAS_SPEC.md) +
  [docs/CADASTRO_POR_CAIXAS_HARNESS.md](../docs/CADASTRO_POR_CAIXAS_HARNESS.md) (CX-01..CX-08 manuais).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 196 testes, 0 falhas**.

### Progresso — 2026-07-01 (recepção parcial de encomenda a fornecedor)

- **Fecha a "Fase 4 (futuro)"** das compras: a recepção de encomenda deixou de ser tudo-ou-nada.
  - `PurchaseOrderLine.receivedQuantity` (migração `V19`) regista o já recebido; **em falta =
    quantity − receivedQuantity**. Novo estado `PARTIALLY_RECEIVED` (ciclo
    `ORDERED → PARTIALLY_RECEIVED* → RECEIVED` / `CANCELLED`).
  - `receivePartial(id, itens)`: recebe as quantidades indicadas por linha (entra stock só pela
    quantidade do acto, FEFO/lote, sem recontar o já recebido), valida `0 < qty ≤ emFalta`, recalcula
    o estado. `receiveOrder` passou a **receber o em falta** (de ORDERED ou PARTIALLY_RECEIVED) sem
    dupla entrada; `cancelOrder` aceita PARTIALLY_RECEIVED (stock recebido mantém-se). MANAGER/ADMIN +
    auditoria. Estado é **derivado** das linhas, nunca escrito à mão.
  - **API:** `POST /api/purchases/orders/{id}/receive-partial`. **UI:** botão "Receber Parcial…" no
    `ComprasPanel` (modal com tabela editável "A receber agora" por linha).
  - Spec/harness: [docs/RECECAO_PARCIAL_SPEC.md](../docs/RECECAO_PARCIAL_SPEC.md) +
    [docs/RECECAO_PARCIAL_HARNESS.md](../docs/RECECAO_PARCIAL_HARNESS.md) (RP-01..RP-12 automáticos,
    RP-50..RP-52 manuais). Testes: `PurchaseOrderServiceTest` 8 → **20**.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 193 testes, 0 falhas**.

### Progresso — 2026-07-01 (exportação fiscal de vendas — estrutura SAF-T)

- **Nova exportação fiscal de auditoria:** o sistema calculava o apuramento de IVA mas não
  exportava um ficheiro estruturado dos documentos de venda. Novo `FiscalSalesExportService`
  produz um **XML alinhado com a estrutura SAF-T** (`Header` / `MasterFiles` / `SourceDocuments` →
  `SalesInvoices`) para um período.
  - **Permissão** MANAGER/ADMIN + guarda multi-tenant (`requireCompany`). Inclui faturas emitidas
    (`APPROVED/PAID/PARTIALLY_PAID`) e **anuladas** (`CANCELLED`, com estado, sem somar aos totais);
    exclui `DRAFT/PENDING*/REJECTED`.
  - **Reutiliza os valores fiscais persistidos** na fatura (não recalcula impostos → não diverge da
    engine de faturação/POS). Escaping XML correcto, determinístico, totais conferíveis.
  - **API:** `GET /api/fiscal/saft?companyId&from&to` (`application/xml`). **UI:** botão
    "Exportar SAF-T (Vendas)" na aba IVA do `FiscalPanel` (usa o ano/mês selecionado, grava `.xml`
    via `JFileChooser`).
  - **Limite honesto:** segue a *estrutura* SAF-T mas **não é certificado** — validar contra a XSD
    oficial da AT-MZ antes de submissão (documentado na spec/harness, SF-51).
  - Spec/harness: [docs/FISCAL_SAFT_EXPORT_SPEC.md](../docs/FISCAL_SAFT_EXPORT_SPEC.md) +
    [docs/FISCAL_SAFT_EXPORT_HARNESS.md](../docs/FISCAL_SAFT_EXPORT_HARNESS.md) (SF-01..SF-14
    automáticos, SF-50..SF-52 manuais). Testes: `FiscalSalesExportServiceTest` (12).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 181 testes, 0 falhas**.

### Progresso — 2026-06-30 (backup físico restaurável — recuperação de desastres)

- **Lacuna fechada (parcial):** o `BackupService` existente grava um **dump JSON lógico lossy**
  (subconjunto de campos, relações achatadas) — bom para auditoria/verificação, **não restaurável**.
  Faltava o caminho de DR real. Novo `DatabaseBackupService` faz **backup físico via
  `pg_dump`/`pg_restore`** (formato custom `-Fc`), fidelidade total da instância.
  - `executePhysicalBackup()` (ADMIN): recusa BD não-PostgreSQL, password só por `PGPASSWORD` no
    ambiente do subprocesso (nunca na linha de comando), escreve `backups/multicore_<db>_<ts>.dump`.
  - `restorePhysicalBackup(path, confirmOverwrite)` (ADMIN, **destrutivo** → exige confirmação):
    `pg_restore --clean --if-exists --no-owner`.
  - Config: `backup.pg-bin-dir` (binários fora do PATH) e `backup.dir`.
  - **UI:** botão "Backup Físico (BD)" no `ConfigPanel` (aba Cópias de Segurança), via
    `runWithProgress`; descrição da aba corrigida (já não diz "base de dados em memória").
  - Spec/harness: [docs/BACKUP_RESTORE_SPEC.md](../docs/BACKUP_RESTORE_SPEC.md) +
    [docs/BACKUP_RESTORE_HARNESS.md](../docs/BACKUP_RESTORE_HARNESS.md) (BR-01..BR-12 automáticos,
    BR-50..BR-54 manuais — o round-trip real precisa de PostgreSQL + binários, fora de CI).
  - Testes: `DatabaseBackupServiceTest` (12: parsing JDBC, construção de comandos, guardas).
- **Ainda pendente para fechar o item de DR:** correr BR-50..BR-54 num ambiente separado (gerar
  `.dump` → restaurar em BD limpa → confirmar contagens idênticas). O software está pronto; falta a
  execução manual com PostgreSQL real.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 169 testes, 0 falhas**.

### Progresso — 2026-06-29/30 (polish de UI profissional transversal)

Várias iterações **só de apresentação** (sem tocar em Services/DTOs/regras), cada uma com spec+harness:

- **Grelha estilo PHC** (`UIHelper.styleTable`): números alinhados à direita (qtd/preço/IVA/total) +
  cabeçalho com separadores; **calha de selecção** ▸ na margem esquerda (rowHeader, sem mexer no
  modelo de colunas). [POS_*]/grelha.
- **Cabeçalho POS compacto:** código de barras subiu para a linha dos selects; catálogo ganhou
  altura. [POS_CABECALHO_COMPACTO_SPEC](../docs/POS_CABECALHO_COMPACTO_SPEC.md).
- **RH — aba "Visão Geral":** 6 cards de KPI + 2 gráficos, reutilizando `KpiCard`/`SimpleBarChart`
  extraídos do Dashboard (DRY). [RH_VISAO_GERAL_SPEC](../docs/RH_VISAO_GERAL_SPEC.md).
- **Inputs/selects profissionais:** `FIELD_BG`/`BORDER` por tema + **realce de foco a acento** +
  combo achatado. [INPUTS_SELECTS_SPEC](../docs/INPUTS_SELECTS_SPEC.md).
- **Inspetor de detalhes** (duplo-clique) → `ModernFormDialog` só-leitura (`asReadOnly`).
  [INSPETOR_DETALHES_SPEC](../docs/INSPETOR_DETALHES_SPEC.md).
- **Aprovações:** inspector inline → **modal de decisão** (Aprovar/Rejeitar/Fechar);
  `ModernFormDialog.addActionButton`/`close`. [APROVACOES_MODAL_SPEC](../docs/APROVACOES_MODAL_SPEC.md).
- **Formulários inline → modal** (CRM Folha de Obra, Financeiro Recebimento, Config Utilizador +
  bug do combo de perfil corrigido). [FORMULARIOS_INLINE_MODAL_SPEC](../docs/FORMULARIOS_INLINE_MODAL_SPEC.md).
- **Prompts de dados → modal** via helpers `UIHelper.promptRequiredText`/`promptAmount` (motivos de
  anulação/rejeição, abrir/fechar caixa). Removido `StockPanel.createWarehouseDialog` V1 morto.
  [PROMPTS_MODAL_SPEC](../docs/PROMPTS_MODAL_SPEC.md).
- **Despesa RH** migrada para `ModernFormDialog`. [MODAIS_ICONES_SPEC](../docs/MODAIS_ICONES_SPEC.md).
- **Modais de despesa/submeter:** despesa do RH em modal.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**.

### Progresso — 2026-06-28 (gestão de categorias profissional + modal de pagamento premium)

- **Gestão de categorias profissionalizada** (tab Categorias do Stock, sobre o `ProductCategoryService`
  existente): tabela com **amostra de cor** (`ColorCellRenderer`), **contagem de produtos** por
  categoria, **pesquisa** por código/nome. Diálogo com **seletor de cor** (`JColorChooser` + amostra ao
  vivo + Limpar) num `ModernFormDialog` premium (ícone `fas-tags`). Activar/desactivar (sem apagar).
  Spec/harness: [docs/CATEGORIAS_GESTAO_SPEC.md](../docs/CATEGORIAS_GESTAO_SPEC.md) +
  [docs/CATEGORIAS_GESTAO_HARNESS.md](../docs/CATEGORIAS_GESTAO_HARNESS.md) (CT-01..10).
- **Modal de pagamento do POS premium:** `askPayment` migrou de `JOptionPane` para `ModernFormDialog`
  (ícone `fas-money-bill-wave`, subtítulo, botão "Confirmar Pagamento" `fas-check`); validação no
  `onSave` (mantém aberto em erro) — fim da recursão.
- **Carrinho POS:** container com `VScrollPanel` (acompanha a largura, scroll vertical quando falta
  altura) → a tabela deixa de colapsar para só o cabeçalho. Bloco **Subtotal s/ IVA + IVA** sempre
  visível por cima do TOTAL. Selector de vista (Venda POS | Histórico) na mesma linha que as acções de
  caixa (poupa espaço). Lupa de pesquisa **dentro** do input.

### Progresso — 2026-06-28 (catálogo POS em cards com imagem + modais premium)

- **Catálogo POS em cards com imagem:** o separador "Venda POS" passou a mostrar os produtos como
  **grid de cards** (imagem/marcador + nome + preço). **Clicar adiciona ao carrinho** (qtd 1, FEFO/
  promoção automáticos); clicar de novo **incrementa** (merge), como num carrinho web. O leitor de
  código de barras usa o mesmo caminho (`addProductToCart`). Removido o formulário detalhado antigo
  (combo de produto, qtd, desconto, lote, série, "Adicionar Artigo", `refreshFEFOHint`).
- **Imagem por produto (bytea):** `Product.imageData`, `ProductDTO.image`, migração `V18`,
  `ComercialService.updateProductImage`; cadastro de produto (StockPanel) ganhou **selector de imagem**
  com pré-visualização (auto-reduzida a 320px). Helpers `UIHelper.readScaledImage` / `imageIconFromBytes`.
- **Selects alinhados no topo:** Cliente/Armazém/Conta passaram para uma **barra superior compacta**
  (estilo web), libertando largura para catálogo + carrinho.
- Spec/harness: [docs/POS_CATALOGO_CARDS_SPEC.md](../docs/POS_CATALOGO_CARDS_SPEC.md) +
  [docs/POS_CATALOGO_CARDS_HARNESS.md](../docs/POS_CATALOGO_CARDS_HARNESS.md) (PC-01..11).
- **Modais premium + botões estilizados:** todos os ~21 modais legados (`JOptionPane`) migraram para
  `ModernFormDialog` (cabeçalho com badge+ícone+subtítulo, botões Cancelar/Confirmar com ícone, rodapé
  fixo). `createDialogForm` passou a **grelha de 2 colunas**. Lupa de pesquisa do POS **dentro** do input.
  Spec/harness: [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) (MI-01..16).

### Progresso — 2026-06-28 (desktop em PostgreSQL real + ícones nos modais + grelha PHC)

- **Base de dados real no desktop:** o perfil `desktop` deixou de usar H2 em memória e passou a usar
  **PostgreSQL local persistente** (BD `multicore`, role dedicada `multicore`). Credenciais fora do git:
  password lida de `${DB_PASSWORD}` (variável de ambiente persistente da máquina).
  [application-desktop.properties](../src/main/resources/application-desktop.properties) com
  **Flyway dono do schema + Hibernate `validate`** (igual a prod).
- **`V17__sync_schema_with_entities.sql`:** fecha o desvio acumulado em dev (que corria `ddl-auto=update`,
  por isso as migrações estavam atrasadas face às entidades). Gerada a partir do diff do Hibernate:
  colunas em falta (`stock_transfers.approved_at/approved_by/rejection_reason`), precisão numérica
  `numeric(38,2)` em `purchase_orders`/`purchase_order_lines`, e 7 `UNIQUE` declaradas nas entidades
  (employees, payslips, payroll_bonuses, product_batches, stocks, tax_rates). **Reposto o caminho
  prod (Flyway+validate), que antes falhava.** Spec/harness:
  [docs/BD_POSTGRES_DESKTOP_SPEC.md](../docs/BD_POSTGRES_DESKTOP_SPEC.md) +
  [docs/BD_POSTGRES_DESKTOP_HARNESS.md](../docs/BD_POSTGRES_DESKTOP_HARNESS.md) (DB-01..06).
- **Ícones nos modais de formulário:** `ModernFormDialog` ganhou ícone contextual no título (deduzido do
  título via `iconForTitle`, domínio>verbo) + `setIconImage`, e `fas-times` no Cancelar. Cobre todos os
  `ModernFormDialog` sem tocar nos call sites. Vocabulário `phc-icons` += Fornecedor/Categoria. Spec/harness:
  [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) +
  [docs/MODAIS_ICONES_HARNESS.md](../docs/MODAIS_ICONES_HARNESS.md) (MI-01..08).
- **Tabelas em grelha estilo PHC:** `UIHelper.styleTable` passou a desenhar linhas verticais + horizontais
  (grelha completa, `setIntercellSpacing(1,1)`, contorno na cor da grelha).
- **Pendente (legado):** modais baseados em `JOptionPane.showConfirmDialog` (Cadastrar Produto, Armazém,
  Ajuste) ainda sem iconografia própria — migrar para `ModernFormDialog` numa próxima iteração.

### Progresso — 2026-06-27 (IVA dinâmico no POS + altura uniforme botões/inputs)

- **IVA dinâmico por produto** (decisão do utilizador): novo FK `Product.taxRate → TaxRate`
  (entidade fiscal configurável já existente), migration `V16`. A taxa efetiva = taxa do produto,
  ou a padrão (`TaxRates.STANDARD_VAT` 16%) quando não definida. Cálculo continua na engine única
  `LineCalculator`. **Deixou de aplicar a constante hardcoded** no checkout do POS.
  - `ProductDTO` passou a expor `taxRateId/taxRate/taxRateLabel`; `createProduct` aceita `taxRateId`;
    formulário de produto (StockPanel) ganhou seletor **Taxa de IVA** (default 16%).
  - **POS UI:** carrinho com colunas **Líquido · IVA · Total** (IVA = "Isento" a 0% ou "valor (taxa%)"),
    rodapé **Subtotal s/ IVA · IVA** e **TOTAL A PAGAR = líquido + IVA** (pagamento/troco usam este total).
  - **Seed:** cesta básica isenta (arroz/açúcar/farinha/feijão), massa 5%, óleo 16% — demonstra taxas
    mistas numa só venda.
  - Spec/harness: [docs/POS_IVA_DINAMICO_SPEC.md](../docs/POS_IVA_DINAMICO_SPEC.md) +
    [docs/POS_IVA_DINAMICO_HARNESS.md](../docs/POS_IVA_DINAMICO_HARNESS.md) (IV-01..09). Testes:
    `POSServiceTest` +2 (isento / 16%). **Verificado visualmente** (Açúcar isento + Óleo 16% + Massa 5%).
- **Altura uniforme botões/inputs:** `ModernButton.getPreferredSize()` impõe altura mínima
  `FORM_CONTROL_HEIGHT` (38px), igual aos campos, mantendo a largura natural (com ícone, sem truncar).
- **Modais contidos na janela principal (mesmo ao arrastar):** `MainFrame.registerMainWindow(this)`
  regista a janela e instala **um listener global** (`AWTEventListener` `COMPONENT_MOVED`) que prende
  qualquer `Dialog` dentro da janela principal — não sai para fora nem ao arrastar (`clampInsideMain`).
  Ao abrir, `containWithinMain` limita a ~94% e centra; `ModernFormDialog` e `makeDialogScrollable`
  dimensionam-se pela janela principal (não pelo ecrã). Cobre os ~333 pontos de diálogo sem os tocar.
  Spec/harness: [docs/MODAIS_CONTIDOS_SPEC.md](../docs/MODAIS_CONTIDOS_SPEC.md) +
  [docs/MODAIS_CONTIDOS_HARNESS.md](../docs/MODAIS_CONTIDOS_HARNESS.md) (MD-01..06, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**. Verificado visualmente
  (carrinho com IVA, botões alinhados, modal "Cadastrar Produto" centrado e contido).

### Progresso — 2026-06-27 (polish profissional do ecrã POS)

- **`POSPanel` repolido** (só apresentação, sem mexer em Service/DTO/cálculos):
  - Formulário esquerdo **reposto ao topo** em `onPanelSelected()` → secção DOCUMENTO (Cliente/
    Armazém/Conta) deixa de aparecer cortada.
  - **Emojis 🔍 removidos** dos campos de pesquisa (não renderizavam sob Metal/Ocean); pista agora é
    **ícone vectorial** `fas-search` à esquerda (helper `searchRow`), padrão da barra de código de barras.
  - **Total em destaque**: faixa `ModernPanel` "TOTAL A PAGAR" + valor a 26px (`%,.2f MT`).
  - **Empty state do carrinho** via `CardLayout` (ícone + "Carrinho vazio" + dica), alternado em
    `updateCartTotal`/`refreshCartView`.
  - **Estado da caixa com ícone** (cadeado aberto/verde vs fechado/amarelo).
- Spec/harness: [docs/POS_UI_POLISH_SPEC.md](../docs/POS_UI_POLISH_SPEC.md) +
  [docs/POS_UI_POLISH_HARNESS.md](../docs/POS_UI_POLISH_HARNESS.md) (PU-01..08, manual).
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (formulários em modal responsivo)

- **`ModernFormDialog`** passou a ser o modal canónico: **scroll automático** do conteúdo,
  **responsivo** (≤92%×88% do ecrã, centrado), botão Gravar com ícone `fas-save` (removido o emoji).
- **Formulários de criação convertidos em modais**, deixando as tabelas a ecrã inteiro: Faturação
  (FT) «Nova Fatura…», Compras «Registar Compra…», Encomendas a Fornecedor «Nova Encomenda…».
  Cada submit lança em erro (modal fica aberto) e recarrega a lista em sucesso.
- Spec/harness: [docs/FORMULARIOS_MODAIS_SPEC.md](../docs/FORMULARIOS_MODAIS_SPEC.md) +
  [docs/FORMULARIOS_MODAIS_HARNESS.md](../docs/FORMULARIOS_MODAIS_HARNESS.md) (FM-01..08, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (contas a pagar a fornecedor — Fase 4)

- **`Purchase.amountPaid`** (em dívida = total − pago), migration `V15` (backfill: compras antigas
  ficam pagas). **Compra a crédito**: `createPurchase` com `financeAccountId = null` não paga no acto.
- `findPayablesByCompany` (saldo > 0) + `registerSupplierPayment` (abate, cap no saldo, saída de
  tesouraria CREDIT, auditoria). API: `GET /api/purchases/payables`, `POST /api/purchases/{id}/pay`.
- **UI:** opção "— A crédito —" no combo de conta da compra + tab **Contas a Pagar** (lista com
  total em dívida + Registar Pagamento). Testes: `PurchaseServiceTest` AP-03..06.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas**.

### Progresso — 2026-06-25 (compras & aprovisionamento profissional)

- **Gestão de fornecedores completa:** campos novos (telefone, contacto, **activo**), editar,
  activar/desactivar (soft-delete, MANAGER/ADMIN + auditado), pesquisar por nome/NUIT. Fornecedor
  inactivo bloqueado em compra/encomenda. Migration `V13`.
- **Encomenda de Fornecedor (`PurchaseOrder`, série `EC-F`):** novo workflow `ORDERED → RECEIVED /
  CANCELLED` (mirror da encomenda de cliente). **Não move stock até à recepção**; a recepção gera
  entrada `PURCHASE` por linha (FEFO/lote, bloqueio de lote vencido), MANAGER/ADMIN + auditado.
  Migration `V14`. Endpoints sob `/api/purchases/orders`.
- **UI (`ComprasPanel`):** tab «Encomendas a Fornecedor» (form + linhas + lista com Receber/Cancelar/
  pesquisa) e tab de fornecedores com editar/pesquisar/activar. **Categorias** ganharam ecrã de gestão
  (nova tab no `StockPanel` sobre o `ProductCategoryService`).
- Spec/harness: [docs/COMPRAS_APROVISIONAMENTO_SPEC.md](../docs/COMPRAS_APROVISIONAMENTO_SPEC.md) +
  [docs/COMPRAS_APROVISIONAMENTO_HARNESS.md](../docs/COMPRAS_APROVISIONAMENTO_HARNESS.md).
  Testes: `PurchaseServiceTest` (5) + `PurchaseOrderServiceTest` (8).
- **Fase 4 (futuro):** contas a pagar a fornecedor (saldo + pagamento → tesouraria) e recepção parcial.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 151 testes, 0 falhas**.

### Progresso — 2026-06-25 (UI faturação: tabela de linhas em largura total)

- Tab **Faturação (FT)** reorganizada com **split vertical**: formulário + faturas recentes em cima,
  **Linhas da Fatura em largura total** em baixo (divisor 50/50 aplicado no 1.º resize real — corrige
  o `setDividerLocation` que não pegava antes do componente ter altura).

### Progresso — 2026-06-25 (vista unificada de movimentos comerciais)

- **Dívida §7.3 fechada:** novo módulo de leitura agregada `modules/movimentos/` (DTO/enum/service/
  controller, sem entidade própria — reutiliza repositórios de `comercial`, padrão do `ReportService`).
  `MovimentosService.listar(companyId, query, from, to)` junta **fatura, encomenda, NC e ND** numa só
  lista, filtrável por **nº/cliente** (substring case-insensitive) e **período** (inclusivo), ordenada
  por **data desc**, com guarda multi-tenant (`requireCompany`).
- **UI:** nova tab "Movimentos" no `ComercialPanel` com filtros (pesquisar/de/até) e rodapé
  contagem+soma. **API:** `GET /api/movimentos`.
- Spec/harness: [docs/MOVIMENTOS_UNIFICADOS_SPEC.md](../docs/MOVIMENTOS_UNIFICADOS_SPEC.md) +
  [docs/MOVIMENTOS_UNIFICADOS_HARNESS.md](../docs/MOVIMENTOS_UNIFICADOS_HARNESS.md). Testes:
  `MovimentosServiceTest` (7: MU-01..MU-07).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 138 testes, 0 falhas**.

### Progresso — 2026-06-25 (recibo pontilhado + loading reutilizável no checkout/PDF)

- **Recibo térmico pontilhado:** separadores passaram de traços a **linhas pontilhadas reais**
  (`DottedLineSeparator`) e as linhas da tabela ganharam **borda inferior pontilhada** (evento de
  célula com `setLineDash`) — aspecto de recibo térmico. `ReceiptPrintService`.
- **Loading profissional reutilizável:** novo `UIHelper.runWithProgress(...)` corre tarefas
  demoradas num `SwingWorker` com diálogo modal "a processar…" + barra indeterminada. Aplicado ao
  **checkout do POS** ("A finalizar venda…") e à **geração/reimpressão de recibos** ("A gerar
  recibo…"). Padrão pronto para outros PDFs/cargas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (promoções de loja + login com loading + maximizar)

- **Módulo `promotions` (sugestão #3):** novo domínio completo (model/enum/repo/dto/service/controller)
  seguindo a arquitetura. Tipos: **percentagem** (produto ou categoria) e **leve X, pague Y** (produto).
  - `PromotionService.bestPromotion(...)` traduz a promoção activa no **desconto % efectivo** por
    linha — reutiliza o checkout existente sem mexer no cálculo do POS/faturação.
  - **POS:** ao adicionar artigo sem desconto manual, aplica automaticamente a melhor promoção e
    marca "Promo: <nome>" na linha do carrinho.
  - **UI:** nova tab "Promoções" no `ComercialPanel` (`PromotionsPanel`) — listar, criar, activar/
    desactivar (permissão MANAGER/ADMIN, auditado).
  - Migration `V12__store_promotions.sql`. Testes: `PromotionServiceTest` (8).
- **Login com loading profissional:** autenticação passou a correr em `SwingWorker` (não congela o
  EDT) com barra de progresso indeterminada reutilizável (`UIHelper.createBusyBar`) e estado
  "A entrar…". Campos/botão bloqueados durante a chamada.
- **Arranque maximizado** após login (`DesktopLauncher`), com estado preservado na troca de tema.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (alerta de validade proativo + UI tema/POS)

- **Alerta de validade (sugestão #4 p/ loja):** lotes com stock vencidos ou a vencer em ≤30 dias
  passam a ser **proativos**, não só consultáveis. `InventoryService.findExpiringBatches(companyId,
  daysAhead)` (com guarda de empresa) delega em `ProductBatchService.findExpiringByCompany`.
  - **Dashboard:** novo cartão "ALERTAS DE VALIDADE" (visível no login) com total + repartição
    "X vencidos · Y a vencer (≤30d)". Grelha de KPIs passou a 3 colunas/linhas automáticas.
  - **Stock › Lotes & Validades:** resumo proativo no topo (vermelho se há vencidos) e **cor de
    urgência** na coluna Estado (VENCIDO vermelho, VENCE EM BREVE amarelo) via `UIHelper.styleTable`.
  - Testes: `InventoryServiceTest` (2: corte hoje+dias / guarda de empresa).
- **UI (sessão anterior):** barra de topo passou a acompanhar o tema claro/escuro; POS com
  formulário/carrinho em `JSplitPane` redimensionável; dropdown dos combos legível em tema claro;
  botões dos diálogos legíveis (gradiente Metal achatado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 123 testes, 0 falhas**.

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

### Progresso — 2026-06-21 (localizar documento de origem por pesquisa, estilo PHC)

- **Faturar a partir de encomenda** e **NC/ND a partir da fatura** passaram a permitir **pesquisar o
  documento de origem por nº ou cliente** (BUSINESS_FLOWS passo 1 "documento origem é localizado").
- Backend: `ComercialService.searchInvoices(query)` e `searchPendingOrders(query)` (filtro substring
  case-insensitive por nº/cliente, empresa activa) + helper `matches()`. Lógica no Service, UI fina.
- UI: campo "Pesquisar (nº ou cliente)" nos 3 diálogos do `ComercialPanel` (Faturar Encomenda, Emitir
  NC, Emitir ND) que filtra a lista ao escrever. Novo helper reutilizável `UIHelper.onTextChange(...)`.
  Faturação manual directa e por encomenda **ambas mantidas**.
- Testes: `ComercialServiceTest` (5 novos) para as pesquisas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 91 testes, 0 falhas**.

### Progresso — 2026-06-21 (bug: encomenda não chegava à aprovação)

- **Causa:** `ComercialService.createOrder` guardava a encomenda como `PENDING` mas **nunca a
  submetia** à Engine de Aprovações — só faturas (desconto >10%) e despesas lá chegavam.
- **Fix:** a encomenda nasce `PENDING_APPROVAL` e é submetida via `approvalService.submitRequest("ORDER", …)`.
  Novo `OrderApprovalCallback`: aprovado → `PENDING` (faturável); rejeitado → `CANCELLED`. `billOrder`
  já exige `PENDING`, logo só fatura encomendas aprovadas (aprovar → depois faturar).
- UI: mensagem de criação passa a indicar "Submetida para aprovação"; a área de aprovação mostra o
  tipo em PT ("Encomenda"/"Fatura") via `humanType(...)`.
- Testes: `OrderApprovalCallbackTest` (4) + `createOrder` submete aprovação (`ComercialServiceTest`);
  `MulticoreServicesTest` actualizado (aprovar a encomenda antes de faturar).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 96 testes, 0 falhas**.

### Progresso — 2026-06-21 (cancelar encomenda)

- `ComercialService.cancelOrder(id, reason)` (mirror de `cancelInvoice`): exige MANAGER/ADMIN e motivo,
  bloqueia encomenda já faturada/cancelada, fecha o pedido de aprovação aberto e audita (`ORDER_CANCEL`).
- `ComercialService.searchCancellableOrders(query)` + `ApprovalService.cancelPendingForDocument(...)`.
- UI: botão "Cancelar Encomenda…" na tab Encomendas → diálogo com pesquisa por nº/cliente + motivo.
- Testes: `ComercialServiceTest` (cancelOrder + searchCancellableOrders) e `ApprovalServiceTest`.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 103 testes, 0 falhas**.

### Progresso — 2026-06-21 (recibo POS: nome + valor pago/troco; UI)

- **Recibo térmico POS** (spec §4 "troco para numerário", RS-05): o nome do comprador passa a ser
  gravado na fatura (`Invoice.customerName`, migration `V9`) e impresso; o `ReceiptPrintService` mostra
  bloco de pagamento por método + **Valor pago** e **Troco**. UI: diálogo de pagamento no checkout
  (`POSPanel.askPayment`) recolhe método e, em numerário, **valor entregue com troco em tempo real**;
  passa a usar a via multi-pagamento (`PaymentEntry` com tendered/change). Bónus: cartão/transferência
  já vão à tesouraria sem contar como numerário na gaveta. Testes: `POSServiceTest` (+2 → 105).
- **Marca com empresa activa:** topo mantém MULTICORE e mostra o nome da empresa por baixo; role
  traduzido para PT discreto (`UIHelper.humanRole`: Administrador/Gestor/Funcionário).
- **Bug do seletor de empresa:** `styleComboBox` substituía o renderer e mostrava `CompanyAccess[...]`.
  Corrigido reaplicando o renderer depois de `styleComboBox` (`MainFrame.applyCompanyRenderer`).
- **Navegação no topo (decisão do utilizador):** sidebar esquerda → **barra de topo só-ícones**
  (`TopNavBar` + `TopNavItem`, ícones via `UIHelper.icon`, tooltip por módulo, quebra para 2ª linha
  via `WrapLayout`), libertando largura para tabelas/formulários. `CollapsibleSidebar`/`SidebarNavItem`
  ficaram sem uso (não removidos).
- **Scroll no formulário de compra:** "Registar Compra (Entrada Stock)" ganhou scroll vertical via
  host `Scrollable` (acompanha largura, não altura) dentro de `JScrollPane` no `ComprasPanel`.
- **Cadastro de cliente fora das Faturas:** removida a tab "Registar Cliente" do `ComercialPanel`
  (e código sem uso: `createRegistarClienteTab`/`registerClient`/4 campos). Gestão de clientes vive
  só na área Clientes (`ClientesPanel`); o diálogo rápido "+ Novo" do POS mantém-se.
- **Nota de arranque desktop:** o `-Dspring-boot.run.main-class` não sobrepõe o `<mainClass>` literal do
  pom; README actualizado para arrancar o desktop via `java -cp`.
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **105 testes, 0 falhas**.

---

## Foco em curso

Fechar lacunas para uso real em loja/mercearia:

- Spec: [docs/RETAIL_STORE_SPEC.md](../docs/RETAIL_STORE_SPEC.md)
- Harness: [docs/RETAIL_STORE_HARNESS.md](../docs/RETAIL_STORE_HARNESS.md)
- Task faseada: [tasks/retail_store_readiness.md](retail_store_readiness.md)

Prioridade imediata: executar o harness RS-01 a RS-22 com dados reais de loja, validar impressora/leitor/gaveta e testar restore num ambiente separado.

### Progresso — 2026-06-22 (colunas de linha dos documentos comerciais)

- **Fatura, encomenda e nota de crédito** passaram a mostrar, por linha: **código de barras, referência,
  descrição, validade (do lote), qtd, preço unit., IVA e subtotal líquido** — via o renderizador
  partilhado `LineItemsTableRenderer` (8 colunas canónicas; subtotal = `LineCalculator.net`).
- Novo `LineRowMapper` (DRY) resolve barcode/ref/descrição do `Product` e a validade via novo
  `ProductBatchRepository.findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc`. Os 3 serviços
  de impressão deixaram de montar linhas à mão.
- Spec/harness: [docs/DOCUMENT_LINE_COLUMNS_SPEC.md](../docs/DOCUMENT_LINE_COLUMNS_SPEC.md) +
  [docs/DOCUMENT_LINE_COLUMNS_HARNESS.md](../docs/DOCUMENT_LINE_COLUMNS_HARNESS.md). ND é baseada em
  valor (sem linhas de artigo).
- **Nova Guia de Remessa**: `GuideRemittancePrintService.render(invoiceId)` gera a guia a partir da
  fatura (mesmas 8 colunas + bloco de transporte/assinaturas, ref. `GR-<nºfatura>` sem consumir
  numeração). Endpoint `GET /api/print/guide/{invoiceId}` + botão "Imprimir Guia" no `ComercialPanel`.
- Teste: `LineItemsTableRendererTest` (3). Verificação: `mvn clean test` → **121 testes, 0 falhas**.

### Backlog — RH/Folha (avaliação 2026-06-22)

Módulo `com.phcpro.modules.hr` está avançado (motor fiscal IRPS/INSS, recibos, despesas, férias,
faltas, UI + PDF) mas **não pronto para produção**. Spec-alvo e harness criados:
- Spec: [docs/HR_PAYROLL_SPEC.md](../docs/HR_PAYROLL_SPEC.md)
- Harness: [docs/HR_PAYROLL_HARNESS.md](../docs/HR_PAYROLL_HARNESS.md) (cenários RH-01..RH-25 + punch list)

Lacunas prioritárias: (1) auditoria ausente no RH; (2) nº de recibo por timestamp em vez de
`DocumentNumberService` gapless; (3) marcar recibo pago não gera saída de tesouraria; (4) mapa
fiscal e config de impostos sem endpoint/PDF; (5) férias sem saldo e `decideVacation` confia no
`decidedBy` do body; (6) faltas não descontam no recibo; (7) 13.º mês e subsídio de férias em falta;
(8) só 6 testes para um módulo de dinheiro+impostos (alvo ~30+).

**Progresso RH — 2026-06-22 (itens 1–3 da punch list):**
- (1) **Auditoria** em todo o RH: `AuditLogService` injectado em `HRService`, audita
  `EMPLOYEE_CREATE/UPDATE/STATUS`, `PAYSLIP_ISSUE/PAID/CANCEL`, `PAYROLL_PROCESS`, `VACATION_DECISION`.
- (2) **Nº de recibo gapless**: série `REC` via `DocumentNumberService.next` (fim do timestamp).
- (3) **Líquido → tesouraria** em `markPayslipPaid` via novo `FinanceService.registerAutoPayout`
  (refactor DRY de `registerAutoExpensePayout`).
- (4) **Mapa fiscal + config de impostos via API/PDF**: `HRController` expõe
  `GET /payroll/fiscal-summary/{year}/{month}` e `GET/POST /payroll/tax-config`; novo
  `PayrollFiscalMapPrintService` (PDF mapa INSS/IRPS) + botão "Imprimir Mapa Fiscal" no `FiscalPanel`;
  novo `HRApiIntegrationTest` (4: 401/403/200).
- (5) **Férias: saldo + decisor seguro**: direito anual 22 dias, saldo = direito − reservados
  (`VacationRepository.sumReservedDays`), `submitVacation` bloqueia acima do saldo; `decideVacation`
  exige MANAGER/ADMIN, resolve decisor por `CurrentUserContext` (deixou de confiar no `decidedBy` do
  body) e exige motivo na rejeição. Testes: `HRServiceTest` (+2).
- (6) **Faltas não remuneradas descontam no recibo**: novo campo `Payslip.absenceDeduction`
  (migration `V10`); `createPayslip` desconta faltas `UNJUSTIFIED` que se sobrepõem ao mês
  (valor/dia = salário base / 30), reflectido no líquido, `PayslipDTO` e PDF. Teste: `HRServiceTest` (+1).
- (7) **13.º mês + subsídio de férias (cálculo + pagamento)**: novo `PayrollBonusService` calcula
  (`thirteenthMonth`/`vacationAllowance`) e **paga de forma persistida e idempotente** via nova
  entidade `PayrollBonus` (migration `V11`, unique `employee+type+year+reference`): `payThirteenthMonth`
  /`payVacationAllowance` exigem MANAGER/ADMIN, saída de tesouraria + auditoria. Endpoints GET (calcular)
  e POST `/pay`. `PayrollBonusServiceTest` (6).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 118 testes, 0 falhas**.
- **RH: punch list 1–7 fechada.** Cobertura RH = 19 testes próprios. Item 8 (cobertura) substancialmente
  cumprido. Falta apenas (opcional) UI desktop para 13.º/subsídio de férias — backend completo.

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
- Backend puro de dev (`application.properties`, `mvn spring-boot:run`) continua em **H2 + `ddl-auto=update`**.
  O **desktop** (perfil `desktop`) usa **PostgreSQL local + Flyway + `validate`**, igual a prod (desde 2026-06-28).
  Prod com PostgreSQL gerido externamente + Flyway + `validate`.

## Estado de build

```
mvn clean compile   → BUILD SUCCESS
mvn clean test      → BUILD SUCCESS, 193 testes, 0 falhas (2026-07-01)
```

Diagnostics Lombok no IDE (`cannot find symbol: getX()`) são **ruído**. Critério único: `mvn compile`.
