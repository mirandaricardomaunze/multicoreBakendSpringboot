# Harness de Testes — Decomposição da Plataforma Superadmin
**Código:** `HARNESS-PLATAFORMA-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/PLATAFORMA_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Comandos de Execução

```powershell
mvn test -pl desktop "-Dtest=PlataformaPanelDecompositionHarnessTest,TableCardContainmentAuditTest,DesktopThinContextTest"
```

## 2. Validações Automáticas

1. **`DECOMP-01: Limite de Linhas do Orquestrador`**
   Garante que `PlataformaPanel.java` possui `<= 130` linhas.
2. **`DECOMP-02: Limite de Linhas das Sub-Views`**
   Garante que cada uma das sub-views (`PlatformCompaniesView`, `PlatformSubscriptionsView`, `PlatformUsersView`, `PlatformSupportTicketsView`, `PlatformClientVersionsView`, `PlatformSystemMonitoringView`) possui `<= 300` linhas.
3. **`DECOMP-03: Contenção de Tabelas em ModernPanel`**
   Auditoria estrita de que todas as sub-views contendo `JTable` usam `ModernPanel` como card de contenção.
4. **`DECOMP-04: Integridade do Contexto Desktop e MainFrame`**
   Execução do `DesktopThinContextTest` validando a instanciação e transição para o perfil Superadmin.
