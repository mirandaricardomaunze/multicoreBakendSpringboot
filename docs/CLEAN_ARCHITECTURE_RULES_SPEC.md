# Regras Obrigatórias de Arquitectura, Clean Code e UI — verificação automática

Status: **canonical**
Effective date: 2026-11-10
Harness: `CleanArchitectureHarnessTest`, `scripts/ralph/diff-guard.ps1` e o perfil de qualidade de `scripts/ralph/gate.ps1`

As regras em si vivem em `AGENTS.md`, `ARCHITECTURE.md`, `CONVENTIONS.md` e `docs/UI_DESIGN_SYSTEM.md`.
Este documento só diz **como** cada regra é verificada sem depender da boa vontade da IA.

## 1. Três mecanismos

| Mecanismo | Julga | Quando |
|---|---|---|
| Testes globais (`CleanArchitectureHarnessTest` + harness existentes) | todo o código | cada iteração do gate e `mvn test` |
| Ratchet (baseline congelada) | dívida legada | só pode diminuir; ligação nova falha |
| Diff-guard | só as linhas que a iteração acrescentou | cada iteração do Ralph loop |

## 2. Testes globais — `CleanArchitectureHarnessTest`

| ID | Regra | Estado hoje |
|---|---|---|
| CA-01 | Controller nunca importa Repository | 0 violações |
| CA-02 | Controller não manipula `@Entity` (o Service devolve DTO) | baseline: `AuditController`, `UserController` |
| CA-03 | Injecção só por construtor (sem `@Autowired`/`@Inject` em campo) | 0 |
| CA-04 | Tipos HTTP (`ResponseEntity`, `web.bind`, servlet) só em controllers | 0 |
| CA-05 | Repository é interface de persistência | 0 |
| CA-06 | SRP: ficheiro Java ≤ 600 linhas | baseline: `MainFrame.java` ≤ 744 |
| CA-07 | Sem acoplamento novo ao Repository de **outro** módulo (usar o Service dono) | baseline de 395 ligações em `backend/src/test/resources/architecture/` |
| CA-08 | `contracts` sem Swing, JPA nem services | 0 |

Já existentes e agora obrigatórios em cada iteração: `MultiModuleArchitectureHarnessTest` (fronteiras
contracts/backend/desktop), `EnterpriseModulesArchitectureHarnessTest` (DTO record, sem float em dinheiro),
`ApiRolePolicyEndpointCoverageHarnessTest`, `MigrationPostgresDialectHarnessTest`.

## 3. UI uniforme — perfil de qualidade do gate

`UiPrecisionHarnessTest` (sem cores locais nem mistura de idioma), `FinalUiUniformityHarnessTest`,
`EnterpriseModulesUiUniformityHarnessTest`, `IconSystemHarnessTest`, `KpiCardUniformityHarnessTest`,
`ButtonIconContrastHarnessTest` e `DesktopThinContextTest`.

## 4. Diff-guard — linhas novas

| ID | Proibido em linhas novas | Alternativa |
|---|---|---|
| DG-01 | `new Color(` no desktop fora de UIHelper/Theme/Palette | tokens do tema |
| DG-02 | Emojis e símbolos Unicode no desktop | `UIHelper.icon(...)` |
| DG-03 | `new JComboBox<`, `JOptionPane.` | `TableFilter.combo`, `UIHelper.createComboBox`, diálogos canónicos |
| DG-04 | `@Disabled`, `assumeTrue(false)` | corrigir o teste |
| DG-05 | `System.out`, `printStackTrace` | logger SLF4J |
| DG-06 | `BigDecimal.ZERO.equals`, `double`/`float` no backend/contracts | `compareTo`/`signum`, `BigDecimal` |
| DG-07 | `@Autowired`, `@Inject` | construtor com campos `final` |
| DG-08 | Apagar testes; alterar harness protegido ou scripts do loop; **acrescentar** linhas a uma baseline (apagar é permitido: é pagar dívida) | só o responsável |
| DG-09 | Controller a importar Repository | Service |
| DG-10 | DRY: o mesmo bloco de ≥ 6 linhas úteis acrescentado duas vezes | extrair método/classe partilhada |

## 5. O que não é mecânico

SOLID fino, nomes e clareza não se medem com regex. Para isso, cada iteração Ralph faz uma
auto-revisão com a skill `multicore-solid-review` sobre o próprio diff antes de terminar
(ver `scripts/ralph/PROMPT.md`), e a suite completa corre quando um goal fecha.

## 6. Alterar uma baseline

Só o responsável, em commit próprio. A baseline só encolhe: quando um acoplamento legado é removido,
apagar a linha. Regenerar com `-Darchitecture.baseline.write=true` apenas para limpar entradas
resolvidas, nunca para aceitar violações novas.
