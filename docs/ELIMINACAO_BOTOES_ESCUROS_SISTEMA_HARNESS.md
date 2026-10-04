# HARNESS — Validação de Botões Vivos e Eliminação de Botões Escuros

**Criado em:** 2026-10-04  
**Camada:** Testes automatizados Swing (`mz.multicore.erp.gui.BrightButtonsHarnessTest`)  
**Módulo:** `desktop`

---

## 1. Objectivo do Teste

Garantir por via de asserções automatizadas que:
1. `UIHelper.SECONDARY` e `UIHelper.BUTTON_NEUTRAL` não contêm tons cinzentos escuros ou ardósia opaca (luminância $\ge 0.35$ e componentes RGB vivas).
2. `UIHelper.createSecondaryButton` e `UIHelper.createRefreshButton` produzem botões com texto branco e fundo vivo (Sky / Indigo).
3. Botões de cancelamento em formulários e diálogos chave utilizam `createDangerButton` em vez de botões escuros.
4. Nenhuma classe do pacote `mz.multicore.erp.gui` referencia cores cinzentas escuras literais para botões.

---

## 2. Casos de Teste Automatizados (`BrightButtonsHarnessTest`)

- `secondaryAndNeutralButtonColorsAreVibrantNotDark()`:
  - Valida que `UIHelper.SECONDARY` e `UIHelper.BUTTON_NEUTRAL` possuem luminosidade suficiente e não são tons escuros como `new Color(75, 85, 99)` ou `new Color(51, 65, 85)`.
- `createSecondaryButtonInstantiatesVibrantButton()`:
  - Assegura que um botão criado por `createSecondaryButton` possui texto branco, ícone contrastante e fundo claro.
- `createRefreshButtonInstantiatesBrightActionButton()`:
  - Assegura que o botão de actualização padrão possui ícone branco `fas-sync-alt` e fundo claro (`ACCENT_SKY`).
- `noDarkButtonsInPriorityPanels()`:
  - Varre as instâncias dos componentes prioritários para confirmar conformidade cromática total.
