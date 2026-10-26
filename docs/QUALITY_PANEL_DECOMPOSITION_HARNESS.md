# Plano de Testes & Validação Arquitetural: Decomposição do Painel de Qualidade e HST

**Documento:** `HARNESS-QUALITY-PANEL-DECOMPOSITION-001`  
**Data:** 2026-10-24  
**Harness Alvo:** `mz.multicore.erp.gui.QualityPanelDecompositionHarnessTest`  
**Referência:** `SPEC-QUALITY-PANEL-DECOMPOSITION-001`  

---

## 1. Casos de Teste do Harness

| ID | Descrição do Caso de Teste | Critério de Sucesso |
|---|---|---|
| **QD-01** | Orquestrador e Contratos Construtores | `QualityPanel` suporta construtores com 2 e 3 argumentos; linha de KPIs possui 4 cartões com altura de 96px (`KpiCard.createGrid(4, 8, 8)`). |
| **QD-02** | Estrutura de Abas e Redução Físico de Linhas | `QualityPanel.java` possui $\le 130$ linhas; `tabbedPane` contém exatamente 3 abas ("Atribuições a Colaboradores", "Radar de Validades Expiradas", "Catálogo de Equipamentos"). |
| **QD-03** | Abas Especializadas Desacopladas | `QualityAssignmentsTab` ($\le 180$ linhas), `QualityExpiredRadarTab` ($\le 140$ linhas), `QualityCatalogTab` ($\le 140$ linhas), `QualityRenewalDialog` ($\le 95$ linhas). |
| **QD-04** | Erradicação de `JOptionPane` e Modais Padrão | 0 ocorrências de `JOptionPane` em todo o pacote `mz.multicore.erp.gui.quality` e em `QualityPanel.java`. |
| **QD-05** | Integração e Sincronização de Dados | `onPanelSelected()` e `refreshAll()` recarregam os KPIs e delegam para as abas especializadas sem lançar exceções. |

---

## 2. Matriz de Cobertura de Testes Existentes

1. `QualityModuleUiHarnessTest` (3/3)
2. `DesktopThinContextTest` (2/2)
3. `MultiModuleArchitectureHarnessTest` (6/6)
4. `ProfessionalFeedbackHarnessTest` (9/9)
