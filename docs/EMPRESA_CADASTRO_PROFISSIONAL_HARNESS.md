# Harness: registo profissional de empresas

Spec: [EMPRESA_CADASTRO_PROFISSIONAL_SPEC.md](EMPRESA_CADASTRO_PROFISSIONAL_SPEC.md).

## Automáticos

| ID | Teste | Garante |
|----|-------|---------|
| CP-01 | `PlatformCompanyOnboardingIntegrationTest.cp01` | Registo completo: perfil, administrador ADMIN, avaliação de 30 dias e conta BANK |
| CP-02 | `PlatformCompanyOnboardingServiceTest.cp02` | Utilizador repetido falha antes de criar a empresa |
| CP-03 | `PlatformCompanyOnboardingIntegrationTest.cp03` | Falha na assinatura desfaz a empresa e o administrador (sem `@Transactional` no teste) |
| CP-04 | `…ServiceTest.cp04` | Senha fraca falha antes de criar a empresa |
| CP-05 | `…ServiceTest.cp05` | Sem SUPERADMIN, bloqueado |
| CP-06/07 | `…ServiceTest.cp06/cp07` | Papel ADMIN, plano por omissão, plano escolhido e conta bancária aplicados |
| CP-08/09 | `PlatformCompanyServiceTest` | O perfil grava-se limpo na criação; editar sem perfil mantém-no |
| CP-10..14 | `CompanyIdentityTextTest` | Empresa antiga igual a antes; nome comercial e razão social; linha legal; morada; contactos |
| CP-20..23 | `CompanyContractsValidationTest` | CAE, website, telefone e capital validados; edição valida email e telefone |
| CP-30 | `WalkInClientProviderTest` WI-04 | O "Consumidor Final" de uma empresa nova é criado depois do commit |

## Manuais (ao vivo, desktop + backend)

| ID | Passos | Esperado |
|----|--------|----------|
| CP-50 | Plataforma → Empresas → Registar Empresa; tentar "Seguinte" sem NUIT | Aviso no passo 1, campo marcado |
| CP-51 | Completar os 4 passos com banco e logótipo | Empresa na lista com localidade; o administrador entra na sessão; a assinatura aparece como Avaliação |
| CP-52 | Factura A4 dessa empresa | Nome comercial, razão social, linha legal, morada composta e conta bancária no pagamento |
| CP-53 | Editar a empresa e abrir directamente o passo 3 | Campos preenchidos; NUIT bloqueado; "Guardar" visível |
| CP-54 | Registar com um utilizador que já existe | Erro no diálogo; nenhuma empresa criada |

## Evidência

- 2026-11-08: suite do desktop com 1192 testes verdes. Na suite do backend (1818), 108 testes falharam
  com `NoClassDefFoundError`, porque outra sessão recompilou o mesmo `target/` a meio da corrida.
  As 28 classes afectadas, mais `TableExportPrintServiceTest` e `ApiRolePolicyEndpointCoverageHarnessTest`,
  voltaram a correr sozinhas: 261 testes, 0 falhas. Os testes manuais CP-50 a CP-54 ainda não foram feitos.
