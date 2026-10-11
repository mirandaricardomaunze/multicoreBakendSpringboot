# Harness — SPEC-AI-OPERATIONAL-DRAFT-001

Executar:

```powershell
mvn -pl backend,desktop -am '-Dtest=AiOperationalDraftHarnessTest,MultiModuleArchitectureHarnessTest,DesktopThinContextTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

O harness verifica `OD-01..OD-08` sem base de dados, emissão ou fornecedor externo.
