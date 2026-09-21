# HARNESS — Validação de Contraste de Botões e Ícones

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing  
**Classe de teste:** `mz.multicore.erp.gui.components.ButtonIconContrastHarnessTest`

---

## Critérios de Conformidade Automatizados

| ID | Regra | Teste Automatizado | Estado |
|---|---|---|:---:|
| **BIC-01** | `ModernButton` com cores de ação padrão (`ACCENT_BLUE`, `APPROVED_GREEN`, `REJECTED_RED`, `BUTTON_NEUTRAL`) possui texto branco e ícone branco sincronizado. | `testActionButtonIconAndTextAreWhite` | ✅ |
| **BIC-02** | `ModernButton.setColors(...)` com fundo claro atualiza simultaneamente a cor de texto e a cor do ícone para texto escuro contrastante. | `testSetColorsSynchronizesIconWithText` | ✅ |
| **BIC-03** | `UIHelper.icon(code, size)` padrão devolve ícone com cor branca (`Color.WHITE`). | `testDefaultIconColorIsWhite` | ✅ |
| **BIC-04** | `ActionMenuButton` mantém o chevron com cor branca/alto contraste correspondente ao texto. | `testActionMenuButtonChevronContrast` | ✅ |
| **BIC-05** | Todos os botões criados pelos helpers (`createPrimaryButton`, `createSuccessButton`, `createDangerButton`, `createWarningButton`, `createSecondaryButton`, `createRefreshButton`) possuem texto e ícones brancos legíveis. | `testAllHelperButtonsHaveWhiteIcons` | ✅ |
