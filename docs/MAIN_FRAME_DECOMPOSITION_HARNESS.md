# Documentação do Harness: Decomposição de MainFrame (Fase 91)

## 1. Objectivo

Documentar as garantias automatizadas fornecidas pelo teste de harness `MainFrameDecompositionHarnessTest` e pelos testes interligados (`DesktopThinContextTest`, `GlobalOmnibarHarnessTest`, `SellerExperienceHarnessTest`, `SellerLeanStartupHarnessTest`).

## 2. Casos de Teste

1. **MFD-01 (Limites Físicos Estritos):**
   - `MainFrame.java` $\le 750$ linhas (reduzido de 991 linhas).
   - `MainFrameNotificationCoordinator.java` $\le 150$ linhas.
   - `MainFrameTopBarBuilder.java` $\le 130$ linhas.
2. **MFD-02 (Zero Legado JOptionPane):**
   - Ausência absoluta de `JOptionPane.` em todos os ficheiros da família `MainFrame*`.
3. **MFD-03 (Delegação Canónica para Colaboradores):**
   - Valida delegação de construção de TopNavBar e chip para `MainFrameTopBarBuilder`.
   - Valida delegação de sino de notificações e contagem para `MainFrameNotificationCoordinator`.
4. **MFD-04 (Preservação de Atalhos e Experiência do Vendedor):**
   - Preservados os atalhos de teclado (Ctrl+K, Ctrl+B, Ctrl+H, F1, F11, F12), rotas do vendedor (SELLER) e card registry.
