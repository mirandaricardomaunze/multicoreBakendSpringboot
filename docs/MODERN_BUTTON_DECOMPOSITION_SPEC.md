# Especificação de Decomposição: ModernButton (Fase 199)

## Objectivo
Decompor o componente de botão corporativo `ModernButton.java` (430 linhas) numa fachada de controlo de estado e animação que delega a pintura Graphics2D, o cálculo de dimensões mínimas/preferidas sem reticências e o desenho do badge de atalho a `ModernButtonPainter`.

## Colaborador Extraído
- `ModernButtonPainter.java`: UI delegada (`createButtonUI`), cálculo de largura do badge de atalho, cálculo de `PreferredSize`, salvaguarda de contraste WCAG e pintura de fundo/gradiente/badge (`paintButtonSurface`, `paintShortcutBadge`).

## Invariantes Preservadas
- Construtores (`ModernButton(String)`, `ModernButton(String, Color, Color)`), animações de hover (`animateHover`, `setHoverProgress`, `getHoverProgress`), `stripEllipsis`, `setShortcut` e `setColors` permanecem 100% compatíveis.
