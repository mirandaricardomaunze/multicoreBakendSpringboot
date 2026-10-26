# Harness de Testes — Decomposição do Assistente de Gestão
**Código:** `HARNESS-AI-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/AI_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Comandos de Execução

```powershell
mvn test -pl desktop "-Dtest=AiPanelDecompositionHarnessTest,TableCardContainmentAuditTest,DesktopThinContextTest"
```

## 2. Validações Automáticas

1. **`DECOMP-01: Limite de Linhas do Orquestrador`**
   Garante que `AiManagementAssistantPanel.java` possui `<= 150` linhas.
2. **`DECOMP-02: Limite de Linhas das Sub-Views`**
   Garante que cada uma das sub-views (`AiAnalysisView`, `AiOperationalDraftsView`, `AiDocumentCreationView`, `AiDocumentHistoryView`, `AiDocumentComparisonDialog`) possui `<= 250` linhas.
3. **`DECOMP-03: Contenção de Tabelas em ModernPanel`**
   Auditoria estrita de que todas as sub-views contendo `JTable` usam `ModernPanel` como card de contenção.
4. **`DECOMP-04: Ausência de Paginação Artificial em Evidências`**
   Verifica que `AiAnalysisView` desativa expressamente a paginação artificial com `ClientTablePagination.DISABLED = true`.
5. **`DECOMP-05: Integridade do Contexto Desktop`**
   Execução do `DesktopThinContextTest` validando a instanciação do `MainFrame` com todas as abas e injeções de cliente API.
