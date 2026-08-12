# Contexto de utilizador/empresa — fail-closed

**Última actualização:** 2026-08-08
**Estado:** implementado; testes automáticos CF-01..CF-08 verdes. Continua o item de
**profissionalização rumo a produção** e complementa o [SEGURANCA_HARDENING_SPEC.md](SEGURANCA_HARDENING_SPEC.md),
que fechou a fronteira **HTTP**. Esta iteração fecha a fronteira **dentro do processo**.

## Objectivo

O `CurrentUserContext` inventava uma sessão quando não havia nenhuma. Duas linhas:

```java
// papel
return new UserSession("SYSTEM", "ADMIN");   // sem contexto → ADMIN

// empresa
return companyId == null ? 1L : companyId;   // sem contexto → empresa 1
```

Isto é **fail-open**: código sem contexto não falha — corre com privilégios máximos, contra o tenant
errado, em silêncio. Passa a **fail-closed**: sem contexto não há papel e não há empresa; quem precisa
de correr sem utilizador tem de o **pedir explicitamente**.

## Porque é que isto importa (a prova)

O `PermissionGuard` é a única guarda de papel do sistema e lê o papel do contexto:

```
PermissionGuard.requireAdmin(...) → CurrentUserContext.getRole() → sem contexto → "ADMIN" → passa
```

Ou seja, **todas** as chamadas a `requireAdmin`/`requireManagerOrAdmin`/`isManagerOrAdmin` eram
no-ops em qualquer thread sem contexto.

E o fallback era **load-bearing** — havia funcionalidade a depender dele. O
[`DataLoader`](../src/main/java/com/phcpro/architecture/DataLoader.java), que corre no arranque como
`CommandLineRunner` (sem contexto), semeia tickets e despesas **através dos Services**
(`crmService.createTicket`, `hrService.submitExpense`), e esses resolvem a empresa por
`getCurrentCompanyId()`. Funcionava só porque:

1. o papel em falta virava `ADMIN`, e
2. a empresa em falta virava `1`, que **por acaso** é a `ptCompany` — a primeira a ser gravada.

Bastava mudar a ordem de gravação das empresas no seed para os dados de demonstração aterrarem no
tenant errado, sem erro nenhum. É exactamente o tipo de acoplamento invisível que o fail-open produz.

> **Nota de rigor:** o backup automático nocturno *parece* o suspeito óbvio, mas **não** dependia do
> fallback — o `DatabaseBackupService` já separa `executePhysicalBackup()` (com guarda, para chamadas
> interactivas) de `runPhysicalBackup()` (núcleo sem guarda, para o agendador). Esse caminho já estava
> correcto e não foi alterado.

### Limite honesto — o que *não* é

Auditadas as fronteiras HTTP: **não foi encontrado nenhum exploit vivo** por esta via. `/api/**` exige
token (`SecurityConfig`) **e** `X-Company-Id` (`SecurityInterceptor`); `/api/platform/**` corre com
utilizador definido e o código de plataforma (`PlatformCompanyService`, `PlatformUserService`) passa o
`companyId` **explicitamente**, nunca pelo contexto. O defeito é de **postura**, não uma porta aberta
conhecida: o comportamento por omissão era conceder em vez de recusar, portanto qualquer caminho novo
— um `@Scheduled`, um `@Async`, uma exclusão no interceptor, um consumidor de fila amanhã — nascia com
ADMIN na empresa 1 sem ninguém dar por isso. É o inverso do que
[ARCHITECTURE.md §10](../ARCHITECTURE.md#10-quando-estás-na-dúvida) pede.

## Como funciona agora

### 1. Sem contexto = sem papel

`getCurrentUser()` continua a devolver um objecto (não rebenta), mas o papel é **vazio**:

| | antes | agora |
|---|---|---|
| `getUsername()` sem contexto | `"SYSTEM"` | `"SYSTEM"` (inalterado — é só a etiqueta de auditoria) |
| `getRole()` sem contexto | `"ADMIN"` | `""` → `PermissionGuard` **recusa** |

O nome mantém-se porque é benigno: `AuditLogService.logEvent` já trata `null` como `"SYSTEM"`. O que
era perigoso era o **papel** vir agarrado a ele.

### 2. Sem contexto = sem empresa

`getCurrentCompanyId()` deixa de inventar `1L` e passa a delegar em `requireCurrentCompanyId()` —
lança `BusinessRuleException("Selecione uma empresa antes de continuar.")`. Os ~80 chamadores em
`modules/` são todos consultas *tenant-scoped* que corriam sob o interceptor (que garante o header),
por isso **o caminho feliz não muda**; muda só o caminho que estava silenciosamente errado.

Para infra que legitimamente corre sem tenant há variantes **nullable**, que nunca lançam:

```java
CurrentUserContext.findCurrentUser()       // UserSession ou null
CurrentUserContext.findCurrentCompanyId()  // Long ou null
```

Usadas onde não há tenant por desenho — propagação de contexto para threads de fundo
(`UIHelper.loadAsync`) e o superadmin, que não tem empresa.

### 3. Elevação de privilégio passa a ser explícita

Novo `CurrentUserContext.runAsSystem(...)` (`Runnable`/`Supplier<T>`, com e sem empresa): instala
`("SYSTEM", "ADMIN")` durante a execução e **repõe o contexto anterior** no fim, mesmo que a tarefa
lance. Trabalho automático que precisa de privilégios passa a declará-lo:

```java
// DataLoader — antes: dependia do fallback assumir ADMIN + empresa 1, invisível
// agora: a elevação e o tenant estão escritos, são greppáveis e têm porquê
CurrentUserContext.runAsSystem(ptCompany.getId(), () -> seedTicketsAndExpenses(...));
```

A empresa é **argumento**, precisamente para não voltar a ser adivinhada.

Regra: `runAsSystem` é para **trabalho automático da própria aplicação** (cron, arranque). Nunca para
servir um pedido HTTP — aí o contexto vem do `SecurityInterceptor` e representa um utilizador real.

## Peças

| Ficheiro | Alteração |
|---|---|
| `architecture/security/CurrentUserContext.java` | fallbacks removidos; `findCurrentUser`/`findCurrentCompanyId`; `runAsSystem` |
| `architecture/DataLoader.java` | povoamento declara `runAsSystem(ptCompany.getId(), …)` em vez de depender do default |
| `gui/components/UIHelper.java` | `loadAsync` captura contexto com as variantes nullable |
| `gui/MainFrame.java` | sino/notificações não carregam sem empresa (superadmin) em vez de assumir a 1 |
| `architecture/security/CurrentUserContextTest.java` | novo — CF-01..CF-06 |
| `architecture/security/PermissionGuardTest.java` | +CF-07/08 (guarda recusa sem contexto) |
| `MulticoreServicesTest.java`, `ReceiptPrintServiceTest.java` | passam a **declarar** o contexto em que já corriam (ver harness) |

## Limite honesto

- `runAsSystem` corre com `ADMIN`, não com um papel `SYSTEM` próprio. Um papel dedicado (com só as
  permissões que o cron precisa) é o passo seguinte; hoje só há três papéis e `PermissionGuard` é
  baseado em `Set<String>`.
- O contexto continua `ThreadLocal`. Threads criadas à mão (`@Async`, `SwingWorker`) continuam a ter de
  propagar à mão — `UIHelper.loadAsync` já o faz. Com `getCurrentCompanyId()` fail-closed, esquecer-se
  agora **falha em voz alta** em vez de ler a empresa errada, que é precisamente o objectivo.
