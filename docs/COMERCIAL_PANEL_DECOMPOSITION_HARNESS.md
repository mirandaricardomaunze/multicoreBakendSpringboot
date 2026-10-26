# Harness de Testes — Decomposição do Painel Comercial (ComercialPanel)
**Código:** `HARNESS-COMERCIAL-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/COMERCIAL_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Comandos de Execução

```powershell
mvn test -pl desktop "-Dtest=ComercialPanelDecompositionHarnessTest,DocumentConversionWorkflowHarnessTest,PhcDocumentEditorHomologationHarnessTest,OrderEditorHarnessTest,SellerExperienceHarnessTest,ActiveCreditInterceptionHarnessTest,TabStripFitsTest,DesktopThinContextTest"
```

E conformidade arquitetural geral:
```powershell
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```

## 2. Validações Automáticas

1. **`DECOMP-01: Limite de Linhas do ComercialPanel`**
   Garante que `ComercialPanel.java` possui $\le 450$ linhas.
2. **`DECOMP-02: Limite de Linhas dos Componentes Especializados`**
   Garante que:
   - `CommercialInvoiceOperations.java` possui $\le 250$ linhas.
   - `CommercialOrderOperations.java` possui $\le 250$ linhas.
   - `CommercialMetadataCoordinator.java` possui $\le 150$ linhas.
3. **`DECOMP-03: Preservação de Âncoras e Conversões`**
   Valida que `convertSelectedOrderToGuide()`, `convertSelectedOrderToTransfer()` e `billSelectedOrder()` existem e funcionam.
4. **`DECOMP-04: Testes de Ajuste de Separadores e Ausência de JOptionPane`**
   Valida a integridade com as suites de testes existentes.
