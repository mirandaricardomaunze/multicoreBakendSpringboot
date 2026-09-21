# Especificação Canónica: Eliminação de Overflow e Navegação Vertical por Setas no Painel
**Código:** SPEC-ASO-001  
**Módulo Principal:** `desktop` (`mz.multicore.erp.gui.components.ArrowScrollPanel`, `mz.multicore.erp.gui.DashboardPanel`)  
**Data:** 2026-09-17  
**Estado:** APROVADO / EM IMPLEMENTAÇÃO  

---

## 1. Contexto e Diagnóstico do Problema
1. **Overflow Horizontal (Clipping de Cartões e Gráficos):**
   - No `DashboardPanel`, o contentor principal era encapsulado num `JScrollPane` convencional sobre um `JPanel` padrão que não implementava `javax.swing.Scrollable`.
   - Quando a resolução horizontal ou a presença da barra lateral reduz a largura disponível da viewport para menos de ~1150px, o painel interior mantinha o seu `preferredWidth` fixo sem se contrair.
   - Com `HORIZONTAL_SCROLLBAR_NEVER`, o lado direito da interface sofria *clipping* forçado (cortando o 4.º cartão da grelha de KPIs, o 4.º cartão do Pulso Estratégico e a margem direita dos gráficos).
2. **Ergonomia do Scroll Vertical:**
   - A barra de rolagem vertical nativa do Swing é visualmente intrusiva, cinzenta e consome espaço útil sem integrar o design system do ERP.
   - O utilizador propôs substituir a barra de rolagem por **botões em cima e em baixo tipo seta** para navegar verticalmente no painel de forma elegante e intuitiva.

---

## 2. Decisões Arquiteturais e Requisitos

### R1. Eliminação Estrita do Overflow Horizontal (Width-Tracking)
- O contentor encapsulado deve implementar `javax.swing.Scrollable` com:
  - `getScrollableTracksViewportWidth() == true`: obriga o conteúdo a dimensionar-se rigorosamente à largura da viewport (100%), garantindo que as grelhas `GridLayout(0, 4)` e `GridLayout(1, 4)` se ajustem proporcionalmente sem corte em qualquer resolução.
  - `getScrollableTracksViewportHeight() == false`: permite a expansão vertical natural necessária para conter todos os blocos executivos.
  - `getScrollableUnitIncrement(...) == 24` e `getScrollableBlockIncrement(...) == 200`.

### R2. Componente Canónico Reutilizável: `ArrowScrollPanel`
- Criação do componente `mz.multicore.erp.gui.components.ArrowScrollPanel` em `desktop`:
  - **Área Central:** `JScrollPane` com `VERTICAL_SCROLLBAR_NEVER` e `HORIZONTAL_SCROLLBAR_NEVER`, mantendo o scroll natural pelo rato (*mouse wheel*).
  - **Calha de Navegação Direita (Vertical Arrow Rail):** Faixa estreita (~34px) com:
    - **Botão Superior (Topo):** `ModernButton` com ícone `fas-chevron-up` ("▲ Rolar para Cima").
    - **Indicador de Progresso (Centro):** Calha vertical minimalista estilizada em tons de `UIHelper` com cápsula proporcional indicando a posição relativa no painel, permitindo clique para salto rápido.
    - **Botão Inferior (Fundo):** `ModernButton` com ícone `fas-chevron-down` ("▼ Rolar para Baixo").
  - **Feedback Dinâmico de Estado:**
    - O botão superior desativa-se quando o utilizador está no topo (`y <= 0`).
    - O botão inferior desativa-se quando o utilizador atinge o fim do conteúdo.
    - Se a altura do conteúdo couber integralmente na viewport sem necessidade de rolagem, os controlos são suavemente desativados/ocultados.
  - **Suavidade e Interação:**
    - Clique único: desloca ~260px com animação suave.
    - Pressão contínua: rolagem suave contínua através de `Timer` interno.

---

## 3. Conformidade com Invariantes do Projecto
- Painéis mantidos estritamente abaixo do limite de 1000 linhas (`UiPanelDecompositionTest`).
- Cores 100% canónicas de `UIHelper` (`UIHelper.BG_CARD`, `UIHelper.GRID`, `UIHelper.ACCENT_BLUE`, `UIHelper.TEXT_LIGHT`, `UIHelper.TEXT_MUTED`), sem criação ad-hoc de `new Color(...)` fora do tema.
- Independência estrita no reactor Maven (`contracts` $\leftarrow$ `backend` / `desktop`).
