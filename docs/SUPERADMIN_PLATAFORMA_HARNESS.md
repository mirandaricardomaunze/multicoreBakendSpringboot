# Harness — Superadmin / Consola da Plataforma

> Cenários para [SUPERADMIN_PLATAFORMA_SPEC.md](SUPERADMIN_PLATAFORMA_SPEC.md).
> Fase 1: SA-01..SA-04 automáticos (`PlatformCompanyServiceTest`, `AuthController`/login);
> SA-50..SA-56 manuais (UI).

**Última actualização:** 2026-07-04

## Automático — Fase 1

| ID    | Cenário | Esperado |
|-------|---------|----------|
| SA-01 | `setCompanyActive(id, false)` com papel SUPERADMIN. | Empresa fica `active=false`; auditoria `PLATFORM_COMPANY_STATUS`. |
| SA-02 | `setCompanyActive(...)` com papel ADMIN (não superadmin). | `BusinessRuleException` (sem permissão). |
| SA-03 | `listCompanies()` devolve todas as empresas com nº de utilizadores e estado. | Inclui activas e inactivas; contagem correcta. |
| SA-04 | `createCompany(...)` com NUIT repetido. | `BusinessRuleException` (NUIT já existe). |

> Bloqueio de login por empresa inactiva é validado manualmente (SA-52); a regra vive no
> `AuthController`/mapeamento de login (empresas activas + recusa se não superadmin sem empresa).

## Manuais (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| SA-50 | Entrar como `superadmin` / `superadmin`. | Abre com a aba **Plataforma**; **sem** seletor de empresa nem abas de tenant (POS, Vendas, …). |
| SA-51 | Plataforma → **Empresas**: ver a tabela. | Lista todas as empresas com Nome, NUIT, Email, Nº utilizadores, Estado. |
| SA-52 | Seleccionar uma empresa → **Desactivar**. Sair. Tentar entrar com um utilizador dessa empresa. | Login recusado com "empresa suspensa / sem empresa activa". |
| SA-53 | Reactivar a empresa. Repetir o login do utilizador. | Login entra normalmente. |
| SA-54 | Empresas → **Novo**: Nome + NUIT + Email. | Empresa criada e visível na tabela. |
| SA-55 | Empresas → **Editar** (ou duplo-clique) → mudar Email/Nome → gravar. | Alterações reflectidas. |
| SA-56 | Entrar como `ana` (ADMIN de tenant). | **Não** existe aba Plataforma; comportamento normal inalterado. |

## Verificação

- `mvn clean test` → verde (SA-01..SA-04 em `PlatformCompanyServiceTest`). Flyway aplica `V24` no
  arranque; `DataLoader` semeia a conta `superadmin` de forma idempotente.
</content>
