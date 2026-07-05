# Harness — Superadmin / Consola da Plataforma

> Cenários para [SUPERADMIN_PLATAFORMA_SPEC.md](SUPERADMIN_PLATAFORMA_SPEC.md).
> Fase 1: SA-01..SA-04 auto + SA-50..SA-56 manuais.
> Fase 2: SB-01..SB-05 auto (`SubscriptionServiceTest`) + SB-50..SB-54 manuais.
> Fase 3: SU-01..SU-05 auto (`PlatformUserServiceTest`) + SU-50..SU-53 manuais.

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

## Automático — Fase 2 (`SubscriptionServiceTest`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| SB-01 | `recordPayment` com período até dentro de 1 mês numa assinatura EXPIRED. | `validUntil` estende-se; estado passa a ACTIVE; pagamento gravado. |
| SB-02 | `recordPayment` com valor ≤ 0. | `BusinessRuleException`; nada gravado. |
| SB-03 | `allowsLogin` numa empresa **sem** assinatura. | `true` (retrocompatível). |
| SB-04 | `allowsLogin` com assinatura expirada (validade no passado) e com suspensa. | `false` em ambos. |
| SB-05 | `saveSubscription` sem papel SUPERADMIN. | `BusinessRuleException`. |

## Manuais — Fase 2 (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| SB-50 | Plataforma → **Assinaturas** → seleccionar empresa → **Definir Plano/Validade** (plano PRO, preço, validade futura). | Tabela mostra plano/estado ACTIVA/validade. |
| SB-51 | **Registar Pagamento** (valor, método M-Pesa, período até data futura). | Pagamento gravado; validade estende-se; estado ACTIVA. |
| SB-52 | **Ver Pagamentos** da empresa. | Lista o pagamento de SB-51. |
| SB-53 | Definir validade no **passado** (ou **Suspender**). Sair. Tentar login de um utilizador dessa empresa. | Login recusado (assinatura expirada/suspensa). |
| SB-54 | **Reactivar** / registar pagamento que cobre o futuro. Repetir login. | Login entra. |

## Automático — Fase 3 (`PlatformUserServiceTest`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| SU-01 | `setUserActive(user, false)`. | Utilizador fica inactivo; gravado. |
| SU-02 | `setUserActive(superadmin, false)`. | `BusinessRuleException` (não desactiva o superadmin). |
| SU-03 | `revokeAccess` do único ADMIN de uma empresa. | `BusinessRuleException`; acesso mantém-se. |
| SU-04 | `revokeAccess` de ADMIN quando há outro ADMIN. | Acesso removido; gravado. |
| SU-05 | `listUsers` sem papel SUPERADMIN. | `BusinessRuleException`. |

## Manuais — Fase 3 (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| SU-50 | Plataforma → **Utilizadores** → **Novo Utilizador** (empresa + papel). | Aparece na tabela com a empresa/papel. |
| SU-51 | Seleccionar → **Conceder/Alterar Acesso** a outra empresa. | Coluna "Empresas & Papéis" passa a listar as duas. |
| SU-52 | **Revogar Acesso** / **Repor Senha** / **Activar-Desactivar**. | Reflectido na tabela; utilizador desactivado não entra no login. |
| SU-53 | Tentar desactivar a conta `superadmin`. | Bloqueado com aviso. |

## Verificação

- `mvn clean test` → verde (SA-01..04 `PlatformCompanyServiceTest`; SB-01..05 `SubscriptionServiceTest`;
  SU-01..05 `PlatformUserServiceTest`). Flyway aplica `V24`/`V25`; `DataLoader` semeia `superadmin`.
</content>
