# Spec — Superadmin / Consola da Plataforma

> Um papel **acima** do tenant que gere as empresas do produto: vê todas, activa/desactiva,
> gere assinaturas/pagamentos, gere utilizadores globalmente e responde a pedidos de assistência.

**Última actualização:** 2026-07-05
**Estado:** Fases 1–4 completas (papel + estado, assinaturas + pagamentos, utilizadores globais,
assistência). Funcionalidade fechada.

## Problema

O sistema é multi-tenant mas o papel mais alto que existe é o **ADMIN de uma empresa**
([UserRole](../src/main/java/mz/multicore/erp/modules/users/model/UserRole.java): EMPLOYEE/MANAGER/ADMIN).
Não há forma de o **dono da plataforma** (quem vende o ERP) ver o conjunto de empresas, suspender
quem não paga, ou dar assistência transversal. `Company` não tinha sequer estado (activa/inactiva).

## Papel de plataforma

- `AppUser` ganha a flag **`platformAdmin`** (boolean, default false) — **ortogonal** aos papéis por
  empresa. Não se mete `SUPERADMIN` no `UserRole` (que é sempre por-tenant); a flag é global.
- Uma conta superadmin de arranque (`superadmin`) é semeada **sem pertencer a nenhuma empresa**.
- No contexto de execução, o superadmin corre com `CurrentUserContext` de papel **`SUPERADMIN`** e
  **sem** empresa activa.

## Autorização (backend)

O [SecurityInterceptor](../src/main/java/mz/multicore/erp/architecture/security/SecurityInterceptor.java)
normal exige token **e** `X-Company-Id` e valida acesso do utilizador àquela empresa — inadequado
para um papel de plataforma. Por isso:

- Pedidos a **`/api/platform/**`** seguem um caminho próprio: valida token → carrega utilizador →
  exige `platformAdmin` → `CurrentUserContext` fica com papel `SUPERADMIN` e **sem** empresa. Sem
  `X-Company-Id`.
- `PermissionGuard.requireSuperAdmin(operação)` protege os serviços da plataforma.

## Fase 1 — Empresas + estado

- `Company` ganha **`active`** (boolean, default true; migração `V24`).
- Empresa **inactiva bloqueia o login** dos seus utilizadores (mensagem clara). Quem já está dentro
  mantém a sessão até sair (decisão do utilizador: bloquear no login, não matar sessões vivas).
- **Login** passa a devolver `superAdmin` (bool) e a **lista de empresas activas**. Utilizador não
  superadmin **sem nenhuma empresa activa** → login recusado. Superadmin pode ter zero empresas.
- `PlatformCompanyService` (guardado por superadmin, com auditoria):
  - `listCompanies()` — todas as empresas com `active` e nº de utilizadores.
  - `setCompanyActive(id, active)` — activar/desactivar (auditoria `PLATFORM_COMPANY_STATUS`).
  - `createCompany(...)` / `updateCompany(...)` — onboarding/edição de empresa
    (auditoria `PLATFORM_COMPANY_CREATE` / `PLATFORM_COMPANY_UPDATE`).
- Controller `PlatformCompanyController` — `GET/POST/PUT /api/platform/companies`,
  `PATCH /api/platform/companies/{id}/active`.

## Fase 1 — Desktop

- Login/sessão desktop transportam `superAdmin`.
- `MainFrame`: a aba **"Plataforma"** só aparece para superadmin; para superadmin **não** se mostra o
  seletor de empresa nem as abas de tenant. Trata o superadmin **sem empresa** (evita o
  `companies.get(0)`).
- `PlataformaPanel` → sub-aba **Empresas**: tabela (Nome, NUIT, Email, Nº utilizadores, Estado) com
  **Novo / Editar / Activar-Desactivar**.
- O superadmin usa os serviços em processo (perfil desktop), como os restantes painéis — não precisa
  de cliente HTTP dedicado.

## Fase 2 — Assinaturas + pagamentos (feita)

Módulo `subscription` (migração `V25`), tudo guardado por SUPERADMIN e auditado:

- **`Subscription`** (1:1 com empresa via `company_id` único): `plan`
  (`PlanType`: TRIAL/BASIC/PRO/ENTERPRISE), `status`
  (`SubscriptionStatus`: TRIAL/ACTIVE/SUSPENDED/EXPIRED), `startDate`, `validUntil`, `monthlyPrice`.
  **EXPIRED é derivado** (`effectiveStatus()`): validade no passado conta como expirada sem job;
  SUSPENDED (manual) prevalece.
- **`SubscriptionPayment`**: `amount`, `method`
  (`PaymentMethod`: DINHEIRO/MPESA/EMOLA/TRANSFERENCIA/OUTRO), `paidAt`, período coberto
  (`periodStart`/`periodEnd`), `note`. Registar um pagamento **estende `validUntil`** até ao fim do
  período e põe a assinatura **ACTIVE**.
- `SubscriptionService`: `listOverview`, `saveSubscription` (plano/preço/validade),
  `changeStatus` (suspender/reactivar), `recordPayment`, `listPayments`, e **`allowsLogin`**
  (política interna, sem guard). Controller `/api/platform/subscriptions`.
- **Login:** além de `company.active`, filtra por `allowsLogin(companyId)` — empresa com assinatura
  expirada/suspensa deixa de ser acessível; sem assinatura continua acessível (retrocompatível).
- **Desktop:** aba **"Assinaturas & Pagamentos"** no `PlataformaPanel` — tabela (Empresa, Plano,
  Estado, Válida até, Preço/mês, Nº pagamentos) + Definir Plano/Validade, Registar Pagamento,
  Ver Pagamentos, Suspender/Reactivar.

## Fase 2b — Vista do assinante + alertas de expiração (feita)

Para o assinante **não ser surpreendido** por uma expiração:

- **Vista do assinante (só-leitura, tenant-scoped):** `SubscriptionService.getMySubscription()` +
  endpoint `GET /api/subscription/me` devolvem `MySubscriptionDTO` (plano, estado efectivo,
  validade, **dias restantes**, mensalidade) da empresa activa — sem privilégios de superadmin.
  Desktop: aba **"A Minha Assinatura"** no `ConfigPanel` (cartão com os valores; estado e dias com
  cor). Sem plano ⇒ "Sem assinatura definida — contacte o suporte".
- **Alertas (antecedência de 7 dias)** — regra única de severidade
  (`-1` expirada/suspensa → vermelho; `0` expira em ≤7 dias → amarelo; `1` ok → sem alerta):
  1. **Aviso no login** — `MainFrame.checkSubscriptionOnStartup()` mostra um aviso **uma vez** ao
     arrancar (só para empresa, não superadmin), disparado pelo `DesktopLauncher` após a janela abrir.
  2. **Chip permanente** na barra de topo — só aparece em risco (amarelo/vermelho), com os dias.
  3. **Destaque no superadmin** — na aba Assinaturas, linhas a amarelo (≤7 dias) / vermelho
     (expirada/suspensa), delegando no renderer do tema (só troca a cor do texto).
- Tudo à prova de falha: se a leitura da assinatura falhar, a UI não rebenta (sem alerta).

## Fase 3 — Utilizadores globais (feita)

`PlatformUserService` (SUPERADMIN + auditado) dá a visão de **todas** as empresas (o
`AppUserService` é limitado à empresa activa):

- `listUsers` (com acessos por empresa), `createUser` (liga já a uma empresa/papel),
  `setUserActive` (não desactiva o superadmin), `resetPassword`, `grantAccess` (conceder/mudar papel),
  `revokeAccess` (**protege o último ADMIN** da empresa). Controller `/api/platform/users`.
- `AppUser.revokeCompany(companyId)` (orphanRemoval trata da eliminação).
- **Desktop:** aba **"Utilizadores"** no `PlataformaPanel` — tabela (Utilizador, Nome, Empresas &
  Papéis, Estado) + Novo, Conceder/Alterar Acesso, Revogar Acesso, Repor Senha, Activar/Desactivar.

## Fase 4 — Assistência (feita)

Módulo `support` (migração `V26`), **distinto do `crm`** (que é assistência ao cliente da empresa —
aqui a empresa é o cliente da plataforma):

- **`SupportTicket`** (empresa, assunto, descrição, `status`
  OPEN/IN_PROGRESS/RESOLVED/CLOSED, `priority` LOW/NORMAL/HIGH/URGENT, `assignee`) +
  **`SupportMessage`** (conversa: autor, `fromSuperAdmin`, corpo).
- `SupportService` com **dois lados**: empresa (MANAGER/ADMIN, limitada à empresa activa) abre e
  responde; superadmin vê todos, responde (assume + OPEN→IN_PROGRESS) e muda estado. Resposta da
  empresa a um pedido RESOLVED reabre-o; pedido CLOSED bloqueia novas mensagens.
- Controllers: **`/api/support/tickets`** (tenant-scoped) e **`/api/platform/support/tickets`**
  (superadmin).
- **Desktop:** aba **"Assistência"** no `PlataformaPanel` (superadmin: ver/responder/mudar estado) e
  aba **"Suporte à Plataforma"** no `ConfigPanel` (empresa: abrir pedido, ver conversa, responder).

## Não-objetivos

- Sem gateway de pagamento automático (M-Pesa/e-Mola) nesta iteração — pagamentos são manuais.
- Não mata sessões já abertas quando a empresa é suspensa (só bloqueia novos logins).
- O superadmin não impersona/entra numa empresa nesta fase.
</content>
</invoke>
