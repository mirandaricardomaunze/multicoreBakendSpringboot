# Plano de Testes & Harness: Atalhos Rápidos de Teclado no POS

**Identificador:** `HARNESS-POS-KEYBOARD-001`  
**Referência:** `SPEC-POS-KEYBOARD-001` (`docs/POS_KEYBOARD_SHORTCUTS_SPEC.md`)  
**Módulos Testados:** `desktop`  

---

## 1. Casos de Teste Automatizados

| ID | Cenário | Condição de Entrada | Resultado Esperado |
|---|---|---|---|
| **KEY-01** | Registo completo de teclas F1 a F12 | Inicialização de `PosKeyboardShortcutsHandler` | Todas as teclas F1 a F12 mapeadas no `InputMap` e `ActionMap` do painel. |
| **KEY-02** | F1 abre diálogo de ajuda | Disparo da acção `posHelp` | `PosShortcutHelpDialog` exibe todas as teclas categorizadas. |
| **KEY-03** | F2 foca pesquisa de produto | Disparo da acção `posProductSearch` | `productSearchField` ganha foco e texto seleccionado. |
| **KEY-04** | F3 foca código de barras | Disparo da acção `posBarcodeSearch` | `barcodeField` ganha foco. |
| **KEY-05** | F4 foca cliente | Disparo da acção `posClientSearch` | `clientSearchField` ganha foco. |
| **KEY-06** | F5 desconto em linha | Artigo no carrinho | Aplica percentagem de desconto no `CartItem` e recalcula total. |
| **KEY-07** | F8 abre devoluções | Disparo de `posReturn` | `PosReturnDialog` é invocado. |
| **KEY-08** | F10 finaliza venda | Disparo de `posCheckout` | `runCheckout()` invocado (com compatibilidade para F9). |
| **KEY-09** | F12 abre fecho de caixa | Disparo de `posCloseSession` | Diálogo de fecho cego é acionado. |
| **KEY-10** | Barra visual de atalhos (`PosShortcutBar`) | Renderização do rodapé | Barra renderiza botões para F1 a F12 com ícones e acções clicáveis sem emojis crus. |

---

## 2. Testes de Regressão Obrigatórios

1. `DesktopThinContextTest` (garante inicialização completa do contexto Spring do Desktop e do `MainFrame`).
2. `TableCardContainmentAuditTest` (garante contenção estrita de tabelas em `ModernPanel`).
3. `UiPanelDecompositionTest` (garante que `POSPanel.java` permanece < 1000 linhas).
4. `MultiModuleArchitectureHarnessTest` (garante integridade modular).
