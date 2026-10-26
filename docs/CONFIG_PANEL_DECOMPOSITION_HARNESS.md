# Harness Canónico: Decomposição de `ConfigPanel` (Fase 10)

**Identificador:** `HARNESS-CONFIG-PANEL-DECOMPOSITION-001`  
**Referência:** [CONFIG_PANEL_DECOMPOSITION_SPEC.md](CONFIG_PANEL_DECOMPOSITION_SPEC.md)  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Casos de Teste do Harness

O harness `ConfigPanelDecompositionHarnessTest` valida:

| ID | Cenário | Validação |
| :--- | :--- | :--- |
| `decomp01_strictLineLimits` | Limites físicos estritos de linhas | `ConfigPanel.java` $\le 450$, `ConfigBackupsPanel.java` $\le 220$, `ConfigDocumentColumnsPanel.java` $\le 200$, `ConfigSubscriptionPanel.java` $\le 150$. |
| `decomp02_zeroLegacyDialogs` | Ausência de diálogos modais bloqueadores | 0 chamadas a `JOptionPane` em todos os ficheiros do domínio de configurações. |
| `decomp03_tabStructurePreserved` | Integridade das abas corporativas | `JTabbedPane` com 6 abas (Auditoria, Backups, Utilizadores, Colunas, Suporte, Assinatura). |
| `decomp04_appearanceBarPreserved` | Barra superior de temas e densidade | Presença dos botões de ciclo de tema, ciclo de densidade e monitorização. |

---

## 2. Comandos de Verificação

```powershell
mvn test -pl desktop "-Dtest=ConfigPanelDecompositionHarnessTest,ConfigPanelSelectionHarnessTest,TableActionPlacementHarnessTest,ProfessionalFeedbackHarnessTest,DesktopThinContextTest"
mvn surefire:test -pl backend "-Dtest=MultiModuleArchitectureHarnessTest"
```
