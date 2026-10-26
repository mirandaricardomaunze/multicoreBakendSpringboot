# Harness de Verificação de Decomposição Modular: QuotationsPanel

**Identificador:** `HARNESS-QUOTATIONS-PANEL-DECOMPOSITION-002`  
**Documento Relacionado:** [QUOTATIONS_PANEL_DECOMPOSITION_SPEC.md](QUOTATIONS_PANEL_DECOMPOSITION_SPEC.md)  
**Data:** 2026-10-24  

---

## 1. Escopo de Validação

Validação estrutural, ergonómica e de conformidade do painel `QuotationsPanel.java` e seus coordenadores colaboradores.

---

## 2. Matriz de Critérios

| ID | Descrição | Limite / Expectativa | Ficheiro de Teste |
|---|---|---|---|
| `QD-01` | Redução do orquestrador principal | $\le 210$ linhas | `QuotationsPanelDecompositionHarnessTest.java` |
| `QD-02` | Colaboradores desacoplados e limites | $\le 230$ e $\le 110$ linhas | `QuotationsPanelDecompositionHarnessTest.java` |
| `QD-03` | Ausência total de diálogos legados | 0 chamadas a `JOptionPane` | `QuotationsPanelDecompositionHarnessTest.java` |
| `QD-04` | Construtores preservados | 5 e 6 argumentos | `QuotationsPanelDecompositionHarnessTest.java` |
| `QD-05` | Contratos de conversão e layout | `tableCardTop`, `ensureHeadersFit`, `Converter em Factura`, etc. | `QuotationsPanelDecompositionHarnessTest.java` |
| `UI-CONV-01` | Ação de conversão e ausência de emojis | `Converter em Factura`, `convertToInvoice`, sem emojis | `QuotationConversionUiHarnessTest.java` |
| `DCW-01` | Fluxo de conversão para fatura e encomenda | `Converter em Factura`, `convertToInvoice` | `DocumentConversionWorkflowHarnessTest.java` |

---

## 3. Comandos de Execução

```powershell
Get-Process javaw -ErrorAction SilentlyContinue | Stop-Process -Force
mvn test -pl desktop "-Dtest=QuotationsPanelDecompositionHarnessTest,QuotationConversionUiHarnessTest,DocumentConversionWorkflowHarnessTest,CommercialOperacionalUnifiedHarnessTest,TableActionPlacementHarnessTest,DesktopInitializationTest,DesktopThinContextTest"
mvn test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
