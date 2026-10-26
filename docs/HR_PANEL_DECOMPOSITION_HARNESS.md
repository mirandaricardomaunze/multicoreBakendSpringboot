# Harness de Testes — Decomposição do Painel de Recursos Humanos (HRPanel)
**Código:** `HARNESS-HR-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/HR_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Comandos de Execução

```powershell
mvn test -pl desktop "-Dtest=HrPanelDecompositionHarnessTest,HRUnifiedHarnessTest,HRSubsidiesUiHarnessTest,HrCrmUiErgonomicsHarnessTest,TabStripFitsTest,HRContractsPanelHarnessTest,DesktopThinContextTest"
```

E conformidade arquitetural geral:
```powershell
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```

## 2. Validações Automáticas

1. **`DECOMP-01: Limite de Linhas do HRPanel`**
   Garante que `HRPanel.java` possui $\le 450$ linhas.
2. **`DECOMP-02: Limite de Linhas dos Componentes Especializados`**
   Garante que:
   - `HROverviewTabPanel.java` possui $\le 200$ linhas.
   - `HREmployeeFormDialog.java` possui $\le 200$ linhas.
   - `HRPayslipFormDialog.java` possui $\le 120$ linhas.
   - `HRAbsenceFormDialog.java` possui $\le 100$ linhas.
3. **`DECOMP-03: Preservação de Âncoras e Quick Peek`**
   Valida que `ensureHeadersFit` e `TableQuickPeekController` continuam vinculados.
4. **`DECOMP-04: Testes de Ajuste de Separadores e Ausência de JOptionPane`**
   Valida a integridade com as suites de testes existentes.
