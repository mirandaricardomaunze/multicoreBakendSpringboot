# HARNESS — Validação do Sistema de Ícones

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing  
**Classe de teste:** `mz.multicore.erp.gui.components.IconSystemHarnessTest`

---

## Critérios de Conformidade Automatizados

| ID | Regra | Teste Automatizado | Estado |
|---|---|---|:---:|
| **IC-01** | `UIHelper.icon(...)` não lança exceção com códigos inválidos, nulos ou vazios, devolvendo fallback visual seguro. | `testFallbackOnInvalidOrNullCode` | ✅ |
| **IC-02** | Os tokens canónicos de tamanho (`ICON_XS`, `ICON_SM`, `ICON_MD`, `ICON_LG`, `ICON_XL`, `ICON_HERO`) estão definidos em `UIHelper` com valores 12, 14, 16, 20, 24 e 48. | `testCanonicalSizeTokensExistAndMatch` | ✅ |
| **IC-03** | `UIHelper.icon(...)` com cor explícita respeita a cor indicada; sobrecarga padrão devolve `Icon` dimensionado. | `testIconCreationAndSizing` | ✅ |
| **IC-04** | `UIHelper.iconImage(...)` gera `BufferedImage` válido e com as dimensões exatas solicitadas sem lançar exceções. | `testIconImageGeneration` | ✅ |
| **IC-05** | `BadgedIcon` e `UIHelper.badgedIcon(...)` calculam dimensões e suportam renderização com contador positivo e nulo. | `testBadgedIconDimensionsAndBehavior` | ✅ |
| **IC-06** | `UIHelper.createIconButton(...)` cria botão com ícone, `toolTipText` e `AccessibleName` configurados. | `testIconButtonAccessibility` | ✅ |
| **IC-07** | O sistema não utiliza `new Color(...)` fora das fronteiras canónicas permitidas e respeita a hierarquia do design system. | `FinalUiUniformityHarnessTest` | ✅ |
| **IC-08** | Suite completa e testes de arquitetura multi-módulo permanecem 100% verdes. | `MultiModuleArchitectureHarnessTest` | ✅ |
