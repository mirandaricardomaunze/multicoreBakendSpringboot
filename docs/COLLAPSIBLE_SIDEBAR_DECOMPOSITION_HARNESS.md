# Harness de Testes - CollapsibleSidebarDecomposition (Fase 89)

## 1. Objectivos do Teste
Validar que a decomposição de `CollapsibleSidebar` respeita:
1. Limites físicos de linhas:
   - `CollapsibleSidebar.java` $\le 280$ linhas;
   - `SidebarLogoBadge.java` $\le 60$ linhas;
   - `SidebarToggleButton.java` $\le 85$ linhas;
   - `SidebarSectionHeader.java` $\le 95$ linhas;
   - `SidebarUserProfileCard.java` $\le 165$ linhas;
   - `SidebarFavoritesCoordinator.java` $\le 100$ linhas.
2. Isolamento de responsabilidades e ausência de chamadas proibidas:
   - Zero ocorrências de `JOptionPane`;
   - Zero emojis Unicode no código;
   - Botão de logout visível e funcional.
3. Integridade e funcionamento do componente em modo headless:
   - Instanciação de `CollapsibleSidebar` com secções e itens;
   - Alternância entre estados expandido (240px) e recolhido (64px);
   - Inclusão e reconstrução de favoritos;
   - Notificação de ouvintes de recolhimento.
4. Coexistência harmónica com os testes globais:
   - `UiOrganizationNavigationHarnessTest`;
   - `SidebarFavoritesHarnessTest`;
   - `DesktopThinContextTest`;
   - `MultiModuleArchitectureHarnessTest`.
