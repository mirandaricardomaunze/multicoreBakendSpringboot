# Harness — SPEC-AI-MANAGEMENT-ASSISTANT-001

Executar:

```powershell
mvn -pl backend,desktop -am '-Dtest=AiManagementAssistantHarnessTest,AiManagementAssistantServiceTest,DesktopThinContextTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

O harness verifica `AI-01..AI-10`, incluindo gerador flexível, PDF profissional, pré-visualização, isolamento por
tenant, auditoria, fallback local e as fronteiras físicas
do reactor Maven. Não chama serviços externos.
