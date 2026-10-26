# Especificação Canónica — Decomposição do HRLiabilitiesPanel

**ID:** `SPEC-HR-LIABILITIES-PANEL-DECOMPOSITION-001`  
**Domínio:** Recursos Humanos / Retenções Fiscais & Valores Legais  
**Alvo:** `mz.multicore.erp.gui.HRLiabilitiesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `HRLiabilitiesPanel` gerencia as retenções na fonte da folha de salários (IRPS e INSS) por entregar ao Estado de Moçambique, o cálculo e acompanhamento do custo total do trabalhador (incluindo INSS patronal), e a parametrização dos valores legais vigentes (prazos de entrega, férias, aviso prévio).

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem de tabela com Quick Peek, barra de apuramento e custo total do mês, diálogo de entrega de retenção usando o diálogo legado `JOptionPane.showInputDialog`, e modal inline extenso para configuração de políticas e valores legais de RH (`openPolicyDialog`).
- **Uso de Diálogo Legado:** Chamada direta a `JOptionPane.showInputDialog` para recolha de referência de pagamento na entrega de retenção.
- **Tamanho original:** 315 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

1. **`HRLiabilitiesPanel` (Orquestrador / View Principal):**
   - Construção do card principal `ModernPanel(16)` via `UIHelper.tableCardTop("Retenções por Entregar", bar, refreshBtn, actionsBtn, deliverBtn)`.
   - Barra de filtros unificada `TableFilter.bar(...)`.
   - Invariantes obrigatórios: `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`.
   - Barra de custo total do trabalhador via `buildCostBar()`.
   - Meta de linhas: $\le 185$ linhas.

2. **`HrPolicyConfigDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal ergonómico baseado em `ModernFormDialog` para configuração dos valores legais e prazos fiscais moçambicanos de RH.
   - Campos estilizados: `DateField`, spinners opcionais para férias, aviso prévio e dias limite de entrega de IRPS e INSS.

3. **`HrLiabilityDeliveryDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal moderno via `ModernFormDialog` substituindo o `JOptionPane.showInputDialog` para entrega de retenção ao Estado, com campo de referência estilizado e resumo em Meticais.

4. **`HRLiabilitiesOperationsCoordinator` (`mz.multicore.erp.gui`):**
   - Coordenador de operações para Quick Peek (`populatePeek`), formatação semântica de prazos (`dueLabel`) e cálculo/texto do custo total do mês (`costText`).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Contratos de Teste Existentes:**
  - `HRUnifiedHarnessTest` (HR-03): Presença obrigatória de `UIHelper.ensureHeadersFit(table)` e `TableQuickPeekController.install(table`.
  - `HrCrmUiErgonomicsHarnessTest`: Presença obrigatória de `UIHelper.tableCardTop("Retenções por Entregar", bar,`, `refreshBtn, actionsBtn, deliverBtn)`, `"Apurar Período"` e `"Valores Legais"`.
  - `TableActionPlacementHarnessTest`: Presença de `UIHelper.tableCardTop(`.
  - `ProfessionalFeedbackHarnessTest`: Eliminação total de chamadas a `JOptionPane`, redução de limite para 0 e remoção da allowlist.
