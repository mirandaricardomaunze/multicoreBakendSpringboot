# Especificação de Decomposição - CollapsibleSidebar (Fase 89)

## 1. Contexto e Motivação
A classe `CollapsibleSidebar.java` possuía 722 linhas no pacote `mz.multicore.erp.gui.components`, acumulando num único ficheiro:
- Renderização do emblema 3D com gradiente e anti-aliasing (`LogoBadge`);
- Construção do botão de alternância com chevron vetorial e acessibilidade (`ToggleButton`);
- Renderização e divisão de secções semânticas da barra lateral (`SectionHeader` e `HairlineDivider`);
- Card de utilizador no rodapé com avatar circular, iniciais, ponto de status online esmeralda, botão de logout e menu popup contextual (`UserProfileCard`);
- Orquestração da barra retrátil, animação suave de transição de largura (240px $\leftrightarrow$ 64px), sincronização de temas e gestão de favoritos.

## Empresa activa e densidade

- A empresa guardada é escolhida antes de construir a sidebar: o nome e o menu seguem a empresa activa.
- Cada item mede 36px, para caberem mais módulos em ecrãs de 720px.
- O nome da empresa ocupa duas linhas em vez de ser cortado.
- Uma seta (`SidebarScrollHint`) avisa que há mais módulos abaixo e, ao clicar, desce o menu.
- A Tesouraria fica no grupo "Finanças & Fiscal", junto da Contabilidade e da Área Fiscal.
- Teste: `SidebarActiveCompanyHarnessTest`.

## 2. Princípios de Arquitetura e Limites Físicos
- **Modularização Estrita no Pacote `mz.multicore.erp.gui.components`:**
  - `SidebarLogoBadge.java`: Emblema 3D da aplicação no topo da sidebar com gradientes e ícone vetorial ($\le 60$ linhas).
  - `SidebarToggleButton.java`: Botão de alternância com chevron vetorial `<`/`>` anti-serrilhado e acessibilidade ($\le 85$ linhas).
  - `SidebarSectionHeader.java`: Cabeçalho de secção semântica com divisória adaptativa para modos expandido e recolhido ($\le 95$ linhas).
  - `SidebarUserProfileCard.java`: Mini-card de operador ativo com avatar circular, iniciais dinâmicas, status online (#10B981), botão e menu de logout ($\le 165$ linhas).
  - `SidebarFavoritesCoordinator.java`: Gestão e reconstrução de itens favoritos da barra lateral ($\le 100$ linhas).
  - `CollapsibleSidebar.java`: Orquestrador magro responsável pela estrutura visual, barra de scroll, favoritos, listeners e animação de largura ($\le 280$ linhas).
- **Semântica Visual e Localização:**
  - Strings estritamente em português de Moçambique (pt-MZ).
  - Ícones vetoriais FontAwesome via `UIHelper.icon(...)`.
  - Zero ocorrências de `JOptionPane`.
  - Adaptação dinâmica a temas claro e escuro.
- **Compatibilidade:**
  - Preservação integral da API pública canónica e constantes `EXPANDED_WIDTH = 240`, `COLLAPSED_WIDTH = 64`.
  - Conformidade estrita com `UiOrganizationNavigationHarnessTest` e `SidebarFavoritesHarnessTest`.
