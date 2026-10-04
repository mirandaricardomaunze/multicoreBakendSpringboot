# HARNESS — Validação de KPI Drilldown Interativo e Ergonomia Invisível de Tabelas

**Criado em:** 2026-10-04  
**Camada:** Testes automatizados Swing (`mz.multicore.erp.gui.components.KpiDrilldownAndTableErgonomicsHarnessTest`)  
**Módulo:** `desktop`

---

## 1. Objectivo do Teste

Validar programaticamente que:
1. `KpiCard.makeInteractive` configura correctamente o cursor de mão (`HAND_CURSOR`), tooltip e focabilidade sem alterar a proporção de 96px de altura.
2. O clique de rato e as teclas `ENTER` / `SPACE` activam a acção de drilldown registada.
3. O duplo-clique na tabela acciona a função de detalhe com o índice correcto do modelo convertido.
4. O menu de contexto de linha selecciona a linha clicada e invoca o provedor de popup.
5. Todos os painéis prioritários mantêm conformidade com o limite estrito de 1000 linhas.

---

## 2. Casos de Teste Automatizados

- `testMakeInteractiveConfiguresHandCursorAndAccessibility()`
- `testInteractiveCardTriggersActionOnLeftClick()`
- `testInteractiveCardTriggersActionOnEnterOrSpaceKey()`
- `testInstallRowDoubleClickHandlerTranslatesModelIndex()`
- `testInstallRowContextMenuSelectsRowAndProvidesPopup()`
