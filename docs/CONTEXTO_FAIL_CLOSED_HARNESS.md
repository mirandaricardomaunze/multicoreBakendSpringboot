# Harness — contexto fail-closed

Casos de verificação da [CONTEXTO_FAIL_CLOSED_SPEC.md](CONTEXTO_FAIL_CLOSED_SPEC.md).
`CF-01..CF-08` são automáticos; `CF-50..CF-54` exigem o backend de pé.

## Automáticos

| # | Caso | Esperado | Onde |
|---|---|---|---|
| CF-01 | `getRole()` sem contexto | `""` (nunca `"ADMIN"`) | `CurrentUserContextTest` |
| CF-02 | `getUsername()` sem contexto | `"SYSTEM"` (etiqueta de auditoria mantém-se) | `CurrentUserContextTest` |
| CF-03 | `getCurrentCompanyId()` sem contexto | lança `BusinessRuleException` (nunca `1L`) | `CurrentUserContextTest` |
| CF-04 | `findCurrentCompanyId()` / `findCurrentUser()` sem contexto | `null`, sem lançar | `CurrentUserContextTest` |
| CF-05 | `runAsSystem` | corre com papel `ADMIN` e **repõe** o contexto anterior no fim | `CurrentUserContextTest` |
| CF-06 | `runAsSystem` com excepção | propaga a excepção **e** repõe/limpa o contexto na mesma | `CurrentUserContextTest` |
| CF-07 | `requireAdmin()` sem contexto | lança `BusinessRuleException` (antes: passava) | `PermissionGuardTest` |
| CF-08 | `isManagerOrAdmin()` sem contexto | `false` (antes: `true`) | `PermissionGuardTest` |

**CF-01, CF-03, CF-07 e CF-08 foram verificados a falhar contra o código antigo** — é o ponto todo do
exercício. Repostas só as duas linhas dos fallbacks, com o resto do código novo no lugar:

```
CurrentUserContextTest.getRole_semContexto_naoDevolvePapelPrivilegiado  expected: <> but was: <ADMIN>
CurrentUserContextTest.getCurrentCompanyId_semContexto_lanca            nothing was thrown
PermissionGuardTest.requireAdmin_semContexto_lanca…                     nothing was thrown
PermissionGuardTest.isManagerOrAdmin_semContexto_devolveFalse           expected: <false> but was: <true>
```

## O que a suite completa revelou

Ligar o fail-closed fez cair **8 testes em 2 classes** que dependiam dos fallbacks sem o declarar —
exactamente o efeito pretendido, e a melhor prova de que o acoplamento invisível existia:

| Classe | Dependia de | Correcção |
|---|---|---|
| `MulticoreServicesTest` (7 testes) | ADMIN + empresa 1 implícitos ao chamar Services fora de HTTP | `@BeforeEach` que declara `("admin-teste","ADMIN")` + empresa 1; `@AfterEach` limpa |
| `ReceiptPrintServiceTest` (1 teste) | o default `1L` casar com o mock `getColumns(1L, …)` | declara `setCurrentCompanyId(1L)`; `@AfterEach` limpa |

Nenhum destes testes mudou de asserção — só passaram a **dizer** o contexto em que já corriam.

## Regressão que não pode partir

| # | Caso | Esperado | Resultado |
|---|---|---|---|
| CF-R1 | Suite completa (`mvn -o clean test`) | 0 falhas — o caminho feliz (HTTP com token + `X-Company-Id`) não muda | ✅ **356 testes, 0 falhas** |
| CF-R2 | `ScheduledBackupServiceTest` | backup automático inalterado (já usava o núcleo sem guarda) | ✅ |
| CF-R3 | `DesktopThinContextTest` | contexto desktop continua a arrancar sem `DataSource` | ✅ |

## Manuais (backend de pé)

| # | Caso | Como | Esperado |
|---|---|---|---|
| CF-50 | Pedido normal | `GET /api/comercial/products` com token + `X-Company-Id: 2` | 200, produtos da empresa 2 (inalterado) |
| CF-51 | Sem header de empresa | mesmo pedido sem `X-Company-Id` | 401 do interceptor (inalterado) |
| CF-52 | Backup nocturno | `backup.schedule.cron` para daqui a 1 min, backend a correr | backup gerado; auditoria `BACKUP_AUTO_OK` |
| CF-53 | Superadmin no desktop | login `superadmin` → aba Plataforma | abre sem erro; **sem** notificações de tenant (antes lia as da empresa 1) |
| CF-54 | Utilizador de tenant no desktop | login normal → navegar pelos painéis | inalterado; sino e listagens carregam a empresa activa |

## Nota de execução

`CurrentUserContext` é `ThreadLocal` e os testes correm em JVM partilhada
(`forkCount=1`/`reuseForks=true`, ver `pom.xml`). Os testes novos fazem `clear()` em `@BeforeEach` **e**
`@AfterEach` para não contaminar as suites seguintes.
