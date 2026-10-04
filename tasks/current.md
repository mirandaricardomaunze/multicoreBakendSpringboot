# Tarefa Actual

## ⚡ Como Iniciar a Aplicação

> **Regra permanente guardada em** `.agents/rules/startup_procedure.md`

```powershell
# 1. Verificar backend
curl.exe -s http://localhost:8080/actuator/health   # deve retornar {"status":"UP"}

# 2. Se backend não estiver UP — iniciar (daemon)
mvn spring-boot:run -pl backend

# 3. Lançar desktop (após backend UP)
Start-Process "C:\Users\miran\Desktop\manager\desktop\target\multicore-desktop-1.0.0.jar"
```

### Tesouraria & Reconciliação Bancária Unificada — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou a aplicação do padrão executivo e ergonómico na tela de **Tesouraria & Conciliação Bancária** (*"coomo ficaria como outras tabelas"*, *"sim"*).
  - Objetivos: consolidar seletores soltos e ações flutuantes no interior do card `ModernPanel(16)`, agrupar ações secundárias em `ActionMenuButton("Operações")` ($\le 5$ opções), instalar «Quick Peek» silencioso por tecla `Espaço` nos movimentos bancários, tornar os KPIs interativos com drilldown e recuperar ~150px de altura vertical eliminando barras de scroll globais.
- **Implementação Realizada:**
  1. **Especificação Técnica Canónica:** Criado [docs/TESOURARIA_CONCILIACAO_UNIFICADA_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/TESOURARIA_CONCILIACAO_UNIFICADA_SPEC.md).
  2. **Documentação do Harness:** Criado [docs/TESOURARIA_CONCILIACAO_UNIFICADA_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/TESOURARIA_CONCILIACAO_UNIFICADA_HARNESS.md).
  3. **Harness Automatizado:** Implementado [desktop/src/test/java/mz/multicore/erp/gui/BankReconciliationUnifiedHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/BankReconciliationUnifiedHarnessTest.java) com 5 testes aprovados (TRECON-01 a TRECON-05).
  4. **Refatoração do `BankReconciliationPanel.java`:**
     - Eliminadas as barras flutuantes exteriores (`buildControlBar` e `buildBottomActionBar`), integrando `accountCombo` e `statementCombo` no `cardHeader` (BorderLayout.NORTH do card).
     - Agrupamento das ações secundárias no `ActionMenuButton("Operações")` (Auto-Conciliar, Conciliar Manualmente, Lançar Encargo Bancário, Desfazer Conciliação, Emitir Relatório PDF).
     - Instalação do `TableQuickPeekController` na tabela de movimentos bancários: tecla `Espaço` e botão lateral abrem drawer com resumo detalhado e vínculo com lançamentos do ERP.
     - KPIs com drilldown interativo (`KpiCard.createInteractiveCard`): clique em "Movimentos Pendentes" ou "Diferença" filtra a tabela para `"PENDENTE"`; clique em "Saldo no Extracto" repõe a vista total.
     - `BankReconciliationPanel.java` mantido rigorosamente em 823 linhas ($\le 1000$ linhas).
- **Validação:**
  - `BankReconciliationUnifiedHarnessTest`: 5/5 testes aprovados.
  - `PosHeaderLayoutHarnessTest`: 5/5 testes aprovados.
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.
  - Aplicação empacotada com sucesso (`multicore-desktop-1.0.0.jar`) e iniciada interativamente no Windows via tarefa `MulticoreERP`.

### Resolução Definitiva de Sobreposição no Cabeçalho do POS — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador submeteu captura de ecrã do POS com sobreposição dos botões `[Cotação F7]` e `[Fidelidade]` colidindo directamente sobre `[Passar Turno]` com instrução *"resolve sobreposiçao"*.
- **Diagnóstico:**
  1. A barra do topo continha excesso de botões horizontais (4 à esquerda e 5 à direita ao abrir sessão de caixa), exigindo ~1150px num espaço de ~1000px.
  2. O layout utilizava `BorderLayout(12, 0)` com `WEST` e `EAST`. Em caso de estouro de largura, o AWT calcula coordenadas sobrepostas sem contenção.
  3. `Cotação` e `Fidelidade` estavam no seletor de abas (`segmented`), criando ruído visual com 7 botões de cores distintas em choque.
- **Implementação Realizada:**
  1. **Especificação Técnica Canónica:** Criado [docs/POS_HEADER_OVERLAP_FIX_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/POS_HEADER_OVERLAP_FIX_SPEC.md).
  2. **Documentação do Harness:** Criado [docs/POS_HEADER_OVERLAP_FIX_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/POS_HEADER_OVERLAP_FIX_HARNESS.md).
  3. **Harness Automatizado:** Implementado [desktop/src/test/java/mz/multicore/erp/gui/PosHeaderLayoutHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/PosHeaderLayoutHarnessTest.java) com 5 testes aprovados (OVERLAP-01 a OVERLAP-05).
  4. **Componentes e Layout Anti-Colisão:**
     - `POSPanel.java`: `topBar` refatorado para `GridBagLayout` com espaçador expansível `Box.createHorizontalGlue()`, garantindo que as colunas da esquerda e direita nunca ocupem o mesmo espaço nem se sobreponham.
     - Agrupamento das ações secundárias de `Cotação (F7)`, `Programa de Fidelidade` e `Histórico de Fechos (Z)` em `ActionMenuButton("Operações")` no seletor de vistas.
     - Preservação intacta de atalhos de teclado (F7, F9, F11, F12, ESC) e da barra de atalhos rápidos do rodapé (`PosShortcutBar`).
     - Preservação estrita das cores semânticas canónicas dos botões de abertura, fecho, sangria e passagem de turno.
     - `POSPanel.java` mantido rigorosamente decomposto em 993 linhas ($\le 1000$ linhas).
- **Validação:**
  - `PosHeaderLayoutHarnessTest`: 5/5 testes aprovados.
  - `PosProfessionalErgonomicsHarnessTest`: 6/6 testes aprovados.
  - `PosButtonColourHierarchyTest`: 1/1 teste aprovado.
  - `UiOrganizationNavigationHarnessTest`: 7/7 testes aprovados.
  - `UiPanelDecompositionTest`: 1/1 teste aprovado (100% dos painéis $\le 1000$ linhas).
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.
  - Aplicação empacotada com sucesso e lançada interativamente no Windows via tarefa agendada `MulticoreERP` (PID activo e a responder).

### «Quick Peek» Silencioso em Tabelas com Tecla Espaço (Painel Deslizante Lateral) — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador escolheu a opção 3 das sugestões de UI de alto valor ergonómico: *«Quick Peek» / Visualização Rápida de Detalhes sem Abrir Modais (Tecla Espaço / Painel Deslizante Lateral)*, com o imperativo de zero ruído visual e máxima sobriedade profissional (*"SIM BASTA NAO FAZER RUIDO NA UI , QUERO QUE SEJA PROFISSIONAL"*).
- **Implementação Realizada:**
  1. **Especificação Técnica Canónica:** Criado [docs/QUICK_PEEK_SILENCIOSO_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/QUICK_PEEK_SILENCIOSO_SPEC.md).
  2. **Documentação do Harness:** Criado [docs/QUICK_PEEK_SILENCIOSO_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/QUICK_PEEK_SILENCIOSO_HARNESS.md).
  3. **Harness Automatizado:** Implementado [desktop/src/test/java/mz/multicore/erp/gui/components/TableQuickPeekHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/components/TableQuickPeekHarnessTest.java) com 7 testes dedicados (PEEK-01 a PEEK-07):
     - `peek01_panelStartsHiddenAndTogglesWithSpace`: Valida abertura e fecho por tecla `SPACE` com drawer acoplado em `BorderLayout.EAST`.
     - `peek02_escClosesQuickPeekDrawer`: Valida dismiss imediato por `ESC`.
     - `peek03_selectionChangeUpdatesPeekContentReactively`: Valida atualização reativa ao navegar linhas com setas do teclado (`UP`/`DOWN`).
     - `peek04_statusBadgePreservesSemanticColoring`: Valida badge de estado com contraste e cor semântica.
     - `peek05_actionButtonTriggersFullDetailsAction`: Valida botão "Ver Detalhes Completos" acionando o fluxo correspondente.
     - `peek06_noSelectionClearsOrHidesPeekSafely`: Valida comportamento gracioso quando a tabela perde seleção.
     - `peek07_quickFilterBarColumnToggleButtonTogglesDrawer`: Valida botão discreto de alternância na barra de filtros.
  4. **Componentes Nucleares:**
     - [QuickPeekPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/QuickPeekPanel.java): Gaveta lateral elegante (340px) acoplada em `BorderLayout.EAST` do card da tabela, com cabeçalho limpo, badge semântico, pares de chave-valor scrolláveis, realce tipográfico para montantes monetários e botão de ação primária.
     - [TableQuickPeekController.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/TableQuickPeekController.java): Controlador universal sem poluição visual, interceptando `SPACE` e `ESC` via `ActionMap`/`InputMap`, ouvindo `ListSelectionListener` para atualizar o drawer em tempo real.
     - [StatusBadge.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/StatusBadge.java): Construtor sobrecarregado `StatusBadge(String text, Color color)` para compatibilidade retroativa com cores semânticas diretas.
     - [TableQuickFilterBar.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/TableQuickFilterBar.java): Método `attachQuickPeek(...)` integrando um botão discreto de alternância de painel lateral (ícone `fas-columns`).
     - [UIHelper.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/UIHelper.java): Método `installQuickPeek(JTable table, JPanel container)` e definição de token `ACCENT_ORANGE_HOVER = new Color(234, 88, 12)`.
  5. **Integração nas Principais Tabelas Operacionais:**
     - [CommercialInvoicesView.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CommercialInvoicesView.java): Visualização instantânea de faturas (número, cliente, NUIT, total com realce verde, estado e data) via tecla `SPACE`.
     - [CommercialOrdersView.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java): Inspeção rápida de encomendas de clientes (código, cliente, total, prazo e estado).
     - [PosSalesHistoryPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosSalesHistoryPanel.java): Inspeção rápida de vendas POS (recibo, operador, caixa, pagamento e total).
- **Validação:**
  - `TableQuickPeekHarnessTest`: 7/7 testes aprovados.
  - `FinalUiUniformityHarnessTest`: 5/5 testes aprovados.
  - `UiPanelDecompositionTest`: 100% aprovado (todas as classes $\le 1000$ linhas).
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.
  - Aplicação empacotada com sucesso e lançada interativamente no Windows via `schtasks /run /tn "MulticoreERP"`.

### Substituição do Ícone da Aplicação por Emblema Corporativo Executivo (.ICO e Mipmaps) — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou a substituição do ícone `.ico` por um emblema corporativo profissional e executivo, sem aspecto gerado por IA: *"troca icon ico por um outro mais profissional sem parecer da ia"*.
- **Implementação Realizada:**
  1. **Concepção da Identidade Visual Multicore:**
     - Afastamento de renderizações 3D futuristas com circuitos de neon ou estética de IA generativa.
     - Criação de um emblema corporativo de precisão geométrica em formato *squircle* moderno (raio 22%), contorno de vidro refinado (`Sky-400`/`Indigo-500`) e fundo ardósia/safira profundo (`Slate-900` `#0F172A` a `Slate-800` `#1E293B`).
     - Emblema central representando a convergência "MULTICORE" com 4 núcleos executivos perfeitamente equilibrados:
       - Azul Real Executivo (Comercial / Vendas)
       - Esmeralda Financeira (Stock / Tesouraria)
       - Âmbar Radiante (Facturação / Fiscal)
       - Violeta Tecnológico (POS / RH)
       - Hub central iluminado e linhas de sincronização vectorial puras.
  2. **Geração Multi-Resolução Canónica:**
     - Gerado novo conjunto de ícones em 7 resoluções: 16x16, 24x24, 32x32, 48x48, 64x64, 128x128 e 256x256 px.
     - Compilação binária de `app-icon.ico` (34.823 bytes) contendo todos os 7 mipmaps compactados em 32-bit ARGB PNG nativo do Windows.
     - Atualização nos directórios `desktop/src/main/resources/icons/` e `installer/app-icon.ico`.
     - Atualização do atalho no ambiente de trabalho `C:\Users\miran\Desktop\Multicore ERP.lnk`.
- **Validação:**
  - `AppIconHarnessTest`: 3/3 testes aprovados.
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - Aplicação empacotada e reiniciada via tarefa interactiva `MulticoreERP` (PID activo e a responder).

### Inicialização e Ecrã de Login em Modo Claro (Light Theme) — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou que o ecrã e formulário de login abram sempre em modo claro (Light theme): *"o login o formulario ou tela esta em dark sempre quando abre eu quero ligth"*.
- **Implementação Realizada:**
  1. **Configuração Canónica de Tema Inicial (`UIHelper.java`):**
     - Alterado o valor padrão de `activeTheme` de `Theme.DARK` para `Theme.LIGHT`.
     - Slots semânticos estáticos (`BG_DARK`, `BG_CARD`, `TEXT_LIGHT`, `TEXT_MUTED`, etc.) inicializados com a paleta canónica de `Theme.LIGHT`.
     - Em `loadAndApplySavedTheme()`, leitura da preferência com padrão `"light"` e sobrescrita de quaisquer valores residuais `"dark"` no arranque.
  2. **Ecrã de Login Acolhedor e Luminoso (`LoginDialog.java`):**
     - O `BackgroundPanel` aplica agora uma sobreposição translúcida luminosa branca (`new Color(255, 255, 255, 80)`) e gradiente suave vertical claro, mantendo a fotografia corporativa nítida, amigável e luminosa.
     - Cartão central `ModernPanel` em branco puro frosted (`new Color(255, 255, 255, 248)`) com contorno subtil `Slate-300` (`new Color(203, 213, 225, 220)`).
     - Tipografia de alto contraste: Título MULTICORE em `Slate-900` (`#0F172A`), subtítulo em `Slate-500` (`#64748B`) e rótulos de campos em `Slate-700` (`#334155`).
     - Campos de entrada de utilizador e senha com fundo branco puro, texto escuro `Slate-900`, cursor escuro, contorno `Slate-300` e ícones `Slate-500`.
     - Botão de alternância de visibilidade da senha com ícone de olho em tom contrastante visível sobre fundo branco.
     - Botões de acção "Entrar" (`UIHelper.ACCENT_BLUE`) e "Cancelar" (`UIHelper.REJECTED_RED`) mantidos com alto contraste e elegância visual.
  3. **Adesão a Padrões de Código (`POSPanel.java` e `PosPaymentDialog.java`):**
     - Substituição de literais `new Color(99, 102, 241)` pelos tokens canónicos `UIHelper.BUTTON_NEUTRAL` e `UIHelper.BUTTON_NEUTRAL_HOVER`.
  4. **Harness Automatizado (`UiOrganizationNavigationHarnessTest.java`):**
     - Adicionado o teste `nav00_loginDialogHasLightCardAndLuminousThemeDefaults()` que valida o tema Light padrão e o fundo claro/frosted do cartão central de login.
- **Validação:**
  - `UiOrganizationNavigationHarnessTest`: 7/7 testes aprovados.
  - `HighContrastThemeHarnessTest`: 8/8 testes aprovados.
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `FinalUiUniformityHarnessTest`: 5/5 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.
  - `UiPanelDecompositionTest`: 100% aprovado.
  - Aplicação empacotada com sucesso e reiniciada via `schtasks /run /tn "MulticoreERP"`, com processo `javaw` activo e a responder.

### KPI Drilldown Interativo e Ergonomia Silenciosa de Tabelas — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou melhorias de UI de alto valor com o princípio de não gerar ruído visual ("SIM BASTA NAO FAZER RUIDO NA UI , QUERO QUE SEJA PROFISSIONAL").
- **Implementação Realizada:**
  1. **Especificação Técnica Canónica:** Criado [docs/KPI_DRILLDOWN_E_ERGONOMIA_SILENCIOSA_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/KPI_DRILLDOWN_E_ERGONOMIA_SILENCIOSA_SPEC.md).
  2. **Documentação do Harness:** Criado [docs/KPI_DRILLDOWN_E_ERGONOMIA_SILENCIOSA_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/KPI_DRILLDOWN_E_ERGONOMIA_SILENCIOSA_HARNESS.md).
  3. **Harness Automatizado:** Criado [desktop/src/test/java/mz/multicore/erp/gui/components/KpiDrilldownAndTableErgonomicsHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/components/KpiDrilldownAndTableErgonomicsHarnessTest.java) cobrindo cursor de mão, hover de borda, acessibilidade por teclado (`ENTER`/`SPACE`), duplo clique seguro com conversão de índice de modelo e menu de contexto do rato.
  4. **Componentes Nucleares:**
     - [KpiCard.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/KpiCard.java): métodos `makeInteractive`, `createInteractiveCard` e `createInteractiveMetricCard`, mantendo rigorosamente a proporção de 96px de altura sem ruído visual.
     - [UIHelper.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/UIHelper.java): métodos `installRowDoubleClickHandler` e `installRowContextMenu` com selecção automática de linha e protecção `table.isShowing()`.
     - [PhysicalInventoryPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/inventory/PhysicalInventoryPanel.java): cartões de KPI interactivos (filtro por sobras `+`, faltas `-` e total) e duplo-clique para bipar/contar produto na grelha.
- **Validação:**
  - `KpiDrilldownAndTableErgonomicsHarnessTest`: 5/5 testes aprovados.
  - `PhysicalInventoryPanelHarnessTest`: 4/4 testes aprovados.
  - `BrightButtonsHarnessTest`: 5/5 testes aprovados.
  - `UiPanelDecompositionTest`: 100% aprovado.
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.

### Eliminação Global de Botões Escuros no Sistema com SPEC e HARNESS — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou eliminar botões escuros em todos os outros lugares do sistema ("tem em outros lugares do sistema", "sim e use spec e harness").
- **Implementação Realizada:**
  1. **Especificação Técnica Canónica:** Criado [docs/ELIMINACAO_BOTOES_ESCUROS_SISTEMA_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/ELIMINACAO_BOTOES_ESCUROS_SISTEMA_SPEC.md).
  2. **Documentação do Harness:** Criado [docs/ELIMINACAO_BOTOES_ESCUROS_SISTEMA_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/ELIMINACAO_BOTOES_ESCUROS_SISTEMA_HARNESS.md).
  3. **Harness Automatizado:** Implementado [desktop/src/test/java/mz/multicore/erp/gui/BrightButtonsHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/BrightButtonsHarnessTest.java) cobrindo luminosidade, contraste, botões secundários, botões de actualização, ausência de botões escuros no POS e canonicidade de botões de cancelamento.
  4. **Padronização no `UIHelper`:**
     - `SECONDARY`: de Gray-600 (`#4B5563`) para `Sky-500` (`#0EA5E9`), revitalizando instantaneamente centenas de botões secundários em todos os módulos.
     - `BUTTON_NEUTRAL`: de Slate-700 (`#334155`) para `Indigo-500` (`#6366F1`).
     - `createRefreshButton`: fundo azul vivo com ícone e texto brancos.
     - Modais principais ([SubscriptionRenewalDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/SubscriptionRenewalDialog.java), [ManagerPinDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/users/ManagerPinDialog.java), [GoalsTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java), [BonusTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/BonusTab.java), [MobilePaymentModal.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java), [SystemMonitoringDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/SystemMonitoringDialog.java), [LicenseAcceptanceDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/LicenseAcceptanceDialog.java)): botões de fechar/cancelar canónicos em `createDangerButton` (`#EF4444`).
- **Validação:**
  - `BrightButtonsHarnessTest`: 5/5 testes aprovados.
  - `UiPanelDecompositionTest`: 100% aprovado, todos os painéis abaixo de 1000 linhas.
  - `DesktopThinContextTest`: 2/2 testes aprovados.
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados.
  - Reactor Maven compilação total 100% com sucesso.

### Eliminação de Botões Escuros e Revitalização Visual no Ponto de Venda (POS) — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - O utilizador solicitou a remoção de todos os botões escuros no módulo POS ("Nao quero botoes escuros no Pos").
  - Identificados e substituídos todos os botões que utilizavam `BUTTON_NEUTRAL` (Slate-700 / `#334155`) e `SECONDARY` (Gray-600 / `#4B5563`) por cores vibrantes, semânticas e claras do design system (`ACCENT_BLUE`, `ACCENT_CYAN`, `ACCENT_SKY`, `APPROVED_GREEN`, `ACCENT`, `Indigo-500`).
- **Implementação Realizada:**
  1. [POSPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/POSPanel.java):
     - Selector de abas (`tabVendaBtn` e `tabHistBtn`): substituído o estado inactivo escuro por `Indigo-500` vibrante (`new Color(99, 102, 241)`).
     - Botão "Cotação": agora em `ACCENT_CYAN` (`#06B6D4`) com ícone e texto brancos.
     - Botão "Fidelidade": agora em `ACCENT` (`#8B5CF6`) com ícone e texto brancos.
     - Botão "Passar Turno": agora em `Teal-600` (`new Color(13, 148, 136)`) com ícone e texto brancos.
     - Botão "Actualizar": agora em `ACCENT_SKY` (`#0EA5E9`) com ícone e texto brancos.
     - Painel preservado rigorosamente em **986 linhas** (abaixo do teto de 1000 linhas).
  2. [PosCatalogController.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosCatalogController.java):
     - Botões de paginação do catálogo ("Anterior" e "Próximo") alterados de cinzento ardósia escuro para `ACCENT_BLUE` (`#3B82F6`) vibrante com chevrons brancos.
  3. [PosShortcutBar.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosShortcutBar.java):
     - Crachás das teclas de atalho (F1..F12, ESC): fundo alterado do cinzento escuro `BUTTON_NEUTRAL` para a cor de acento vibrante de cada acção correspondente.
  4. [PosPaymentDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosPaymentDialog.java):
     - Chips de método de pagamento (Numerário, M-Pesa, E-Mola, Cartão, Vale): estilo activo `ACCENT_BLUE` e inactivo `Indigo-500` com texto branco.
     - Botões de cédulas rápidas em Meticais ("50 MT", "100 MT", "200 MT", "500 MT", "1000 MT", "2000 MT"): estilizados em verde esmeralda (`APPROVED_GREEN` `#10B981`) e "Exacto" em `ACCENT_BLUE` (`#3B82F6`).
     - Botão "Push USSD": `ACCENT_ORANGE` (`#F97316`).
     - Botão "Verificar Vale": `ACCENT_CYAN` (`#06B6D4`).
  5. [PosSalesHistoryPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosSalesHistoryPanel.java):
     - Botão "Devolver / Trocar": `ACCENT_ORANGE` (`#F97316`).
     - Botão "Actualizar": `ACCENT_BLUE` (`#3B82F6`).
  6. [PosScaleLiveWidget.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/pos/scale/PosScaleLiveWidget.java):
     - Botão "Tarar": `ACCENT_SKY` (`#0EA5E9`).
     - Botão "Simular": `ACCENT_CYAN` (`#06B6D4`).
  7. Diálogos modais do POS:
     - [PosImportQuotationDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosImportQuotationDialog.java): "Actualizar" em `ACCENT_BLUE` e "Cancelar" em `REJECTED_RED`.
     - [PosBlindCloseDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java): "Cancelar / Voltar" em `REJECTED_RED`.
     - [PosSessionHistoryDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/pos/PosSessionHistoryDialog.java): "Recarregar" em `ACCENT_BLUE` e "Fechar" em `REJECTED_RED`.
     - [PosContingencyDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/pos/contingency/PosContingencyDialog.java): "Reimprimir Talão" em `ACCENT_CYAN` e "Fechar" em `REJECTED_RED`.
     - [PosReturnDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosReturnDialog.java): "Copiar Código" em `ACCENT_CYAN`.
     - [PosShortcutHelpDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosShortcutHelpDialog.java): crachás em `ACCENT_BLUE`.
- **Validação:**
  - `UiPanelDecompositionTest` e `DesktopThinContextTest`: 100% aprovados (3/3 testes).
  - Verificação de ocorrências residuais: 0 ocorrências de `BUTTON_NEUTRAL` e 0 de `createSecondaryButton` em todas as classes POS.
  - Desktop reempacotado e reiniciado interativamente via `schtasks` (PID 20796).

### Catálogo Multi-Armazém Dinâmico no Ponto de Venda (POS) — Backend e Desktop — 2026-10-04 — **concluída com sucesso**

- **Contexto e Requisito:**
  - No Ponto de Venda (POS), ao seleccionar ou alternar o armazém na barra superior (`warehouseCombo`), o catálogo de produtos e os leitores de código de barras devem reflectir imediatamente a disponibilidade e o saldo de stock específico do armazém seleccionado em tempo real.
- **Implementação Realizada:**
  1. **Contratos (`contracts`):**
     - [POSCatalogItemDTO.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/POSCatalogItemDTO.java): adicionado campo `BigDecimal stockQuantity` com construtor sobrecarregado retrocompatível de 2 parâmetros (`this(product, sellable, null)`).
  2. **Backend (`backend`):**
     - [InventoryService.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/inventory/service/InventoryService.java): adicionados `getInStockProductIdsForSale(companyId, warehouseId)` e `getStockQuantitiesForSale(companyId, warehouseId)`, filtrando armazéns activos com `salesAllowed = true`.
     - [ComercialService.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java): `getPOSCatalogPage(query, availableOnly, warehouseId, page, size)` calcula status vendável (`sellable`) e saldo de stock por armazém; `findPOSCatalogItemByBarcode(barcode, warehouseId)` resolve artigo por armazém.
     - [ComercialController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java): expõe parâmetro opcional `@RequestParam(required = false) Long warehouseId` nas rotas `/products/pos-catalog/page` e `/products/pos-catalog/by-barcode`.
  3. **Desktop (`desktop`):**
     - [ComercialApiClient.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/desktop/client/ComercialApiClient.java): adicionado suporte a `warehouseId` nos pedidos HTTP do catálogo e leitor de código de barras.
     - [POSPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/POSPanel.java): listener ligado a `warehouseCombo` com protecção contra disparos espúrios (`suppressWarehouseEvents`), cache concorrente de saldo de stock por produto (`productStockQuantities`), e métodos auxiliares `getSelectedWarehouseId()`, `getSelectedWarehouseName()`, `getProductStock(productId)`. Mantido estritamente abaixo do limite de 1000 linhas (984 linhas).
     - [PosCatalogController.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosCatalogController.java): recarrega catálogo automaticamente ao mudar armazém, renderiza crachá `[Qtd] disp.` em verde semântico (`APPROVED_GREEN`) ou `ESGOTADO` em vermelho (`REJECTED_RED`), e inclui nome do armazém e saldo detalhado no tooltip.
     - [PosBarcodeActions.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PosBarcodeActions.java): valida código de barras e artigos pesados em conformidade com o armazém seleccionado.
- **Validação e Testes:**
  - `MultiModuleArchitectureHarnessTest`: 6/6 testes aprovados (isolamento Maven `contracts` / `backend` / `desktop`).
  - `ComercialServiceTest` e `ComercialControllerIntegrationTest`: 55/55 testes aprovados.
  - `DesktopThinContextTest`: 2/2 testes aprovados (arranque limpo de contexto e `MainFrame`).
  - `UiPanelDecompositionTest`: 100% aprovado (painéis prioritários mantidos `<= 1000` linhas).
- **Lançamento:**
  - Backend empacotado e activo em `http://localhost:8080/actuator/health` (`UP`).
  - Desktop interactivo empacotado e em execução na sessão Windows.

### Unificação DRY de Formatação (Data e Moeda) e Auditoria de Design System — 2026-10-03 — **concluída com sucesso**

- **Diagnóstico e Auditoria Geral:**
  1. Varredura completa em 203 ficheiros do módulo `desktop` (97 ecrãs e diálogos).
  2. Confirmada 100% de conformidade de design em contenção de tabelas com `ModernPanel(16)`, estilização canónica de abas `JTabbedPane` com cores semânticas vibrantes e 0 cores cruas (`new Color`) ou emojis Unicode em código de negócio.
  3. Identificada duplicação de `DecimalFormat` e `DateTimeFormatter` em dezenas de ficheiros da UI.
- **Implementação Realizada:**
  1. Centralização canónica no [UIHelper.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/UIHelper.java):
     - `UIHelper.formatMzn(BigDecimal)` e `UIHelper.formatMzn(Number)` para moedas (ex.: "1 250,00 MT").
     - `UIHelper.formatQty(BigDecimal)` para quantidades numéricas.
     - `UIHelper.DATE_FMT`, `UIHelper.DATETIME_FMT`, `UIHelper.TIME_FMT`, `UIHelper.formatDate(...)` e `UIHelper.formatDateTime(...)`.
  2. Refatoração DRY e eliminação de blocos estáticos redundantes em:
     - [StockWastePanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockWastePanel.java)
     - [CreditRiskPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CreditRiskPanel.java)
     - [ForensicAuditPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/ForensicAuditPanel.java)
     - [NotificationFeed.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/NotificationFeed.java)
     - [BonusTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/BonusTab.java), [GoalsTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/GoalsTab.java), [ProgressTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/ProgressTab.java), [RankingTab.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/performance/RankingTab.java)
- **Validação:**
  - 26 testes unitários e de integração desktop executados e aprovados com 100% de sucesso (`DesktopThinContextTest`, `FinalUiUniformityHarnessTest`, `TableCardContainmentAuditTest`, `StockWastePanelHarnessTest`, `CreditRiskPanelHarnessTest`, `ForensicAuditPanelHarnessTest`).
  - Aplicação recompilada e reiniciada na sessão Windows (PID 7804).

### Padronização Visual e Ergonómica da Aba de Alertas de Stock (`StockAlertsPanel`) — 2026-10-03 — **concluída com sucesso**

- **Diagnóstico do Problema:**
  1. Havia um cabeçalho externo e botão "Actualizar" solto fora do card (`topStack`), flutuando no vazio antes das abas.
  2. Os inputs de pesquisa e filtro de estado usavam `TableFilter.bar` sem alinhamento com rótulos superiores e sem ocupar a largura (`weightx = 1.0`), deixando campos estreitos e dissonantes do design system das outras tabelas.
  3. Faltava paginação `ClientTablePagination` no rodapé das tabelas de alertas.
- **Implementação Realizada:**
  1. Eliminação total do `topStack` solto e alinhamento estrito dentro do card `ModernPanel(16)`.
  2. Grade de filtros com `GridBagLayout` idêntica a `Níveis de Stock` e `Lotes & Validades`:
     - Rótulos superiores padronizados com `filterLabel("Estado do Lote")` e `filterLabel("Pesquisa")`.
     - Campo de busca `SearchField` profissional (altura 38px, cantos arredondados, lupa vetorial integrada, ocupando `weightx = 1.0`).
     - `JComboBox` de estado estilizado com `UIHelper.styleComboBox` (largura 220px).
  3. Botão "Actualizar" posicionado no canto superior direito de cada card através de `UIHelper.tableCardTop(...)`.
  4. Resumos estatísticos dinâmicos (`alertsSummary` e `outSummary`) integrados abaixo da faixa de filtros dentro do card, com cores semânticas vibrantes (`APPROVED_GREEN`, `REJECTED_RED`, `PENDING_YELLOW`).
  5. Paginação canónica instalada no rodapé de ambas as tabelas (`ClientTablePagination.install(...)`).
- **Validação de Testes:**
  - `DesktopThinContextTest` (2/2 passing)
  - `TableActionPlacementHarnessTest` (6/6 passing)
  - `TableCardContainmentAuditTest` (2/2 passing)
  - `MultiModuleArchitectureHarnessTest` (6/6 passing)
- **Execução:** Desktop reempacotado e reiniciado interativamente via `schtasks` (PID 3372).

### SPEC-CONC-001: Controlo Estrito de Concorrência e Bloqueio Otimista Transversal — 2026-10-03 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Proteção de integridade contra sobreposição silenciosa de dados (*Lost Updates*) em ambientes multi-operador e multi-terminal:
  1. **Bloqueio Otimista em Entidades Mestras (`@Version`):**
     - Migration Flyway `V80__optimistic_locking_master_data.sql` adicionando `version BIGINT NOT NULL DEFAULT 0` em `clients`, `products` e `suppliers`.
     - Entidades [Client.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/comercial/model/Client.java), [Product.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/comercial/model/Product.java) e [Supplier.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/purchases/model/Supplier.java) atualizadas com anotação `@Version` e campo `Long version`.
  2. **Interceção Centralizada no GlobalExceptionHandler (`HTTP 409 Conflict`):**
     - Mapeamento específico de `ObjectOptimisticLockingFailureException` e `OptimisticLockException` no [GlobalExceptionHandler.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/architecture/exception/GlobalExceptionHandler.java).
     - Devolução de `HTTP 409 Conflict` com mensagem clara em português de Moçambique: *"Este registo foi alterado ou aprovado concorrentemente por outro utilizador. Por favor, actualize os dados antes de gravar."*, eliminando quedas para erro genérico 500.
  3. **Compatibilidade Transversal de Contratos DTO (`contracts`):**
     - [ClientDTO.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/ClientDTO.java), [ProductDTO.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/ProductDTO.java) e [SupplierDTO.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/purchases/dto/SupplierDTO.java) atualizados com campo `version` preservando 100% dos construtores sobrecarregados anteriores para retrocompatibilidade.
- **SPEC:** [docs/OPTIMISTIC_LOCKING_CONCURRENCY_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/OPTIMISTIC_LOCKING_CONCURRENCY_SPEC.md).
- **HARNESS:** [docs/OPTIMISTIC_LOCKING_CONCURRENCY_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/OPTIMISTIC_LOCKING_CONCURRENCY_HARNESS.md).
- **Testes Automatizados:** [OptimisticLockingConcurrencyHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/concurrency/OptimisticLockingConcurrencyHarnessTest.java) (5/5 passing).
- **Validação Global:** 24 testes executados e aprovados sem falhas (`MultiModuleArchitectureHarnessTest`, `RateLimitingAndBruteForceHarnessTest`, `AuditTrailCriticalEventsHarnessTest`, `OptimisticLockingConcurrencyHarnessTest`, `DesktopThinContextTest`).
- **Execução e Lançamento:** Aplicação reempacotada com sucesso e inicializada: Backend UP em `http://localhost:8080/actuator/health` e Desktop interativo ativo na sessão Windows (PID 18500).

### SPEC-AUD-001: Trilha de Auditoria Centralizada e Imutável de Eventos Críticos — 2026-10-03 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Implementação da centralização, imutabilidade e enriquecimento forense da trilha de auditoria para operações de alto impacto financeiro, controlo de stock e segurança de utilizadores:
  1. **Enriquecimento Forense de Rede (`AuditLog` & `ipAddress`):**
     - Migration Flyway `V79__audit_logs_ip_address.sql` adicionando a coluna `ip_address VARCHAR(50)` na tabela `audit_logs`.
     - Entidade `AuditLog` atualizada com mapeamento JPA `@Column(name = "ip_address", length = 50)`.
     - `AuditLogDTO` atualizado em `contracts` com novo campo `ipAddress` e construtor sobrecarregado retrocompatível de 4 parâmetros.
     - `AuditLogService` enriquecido com suporte a sobrecargas para registo de IP do cliente em `logEvent` e `logCurrent`.
     - `AuditController` atualizado para expor `ipAddress` no DTO de leitura.
  2. **Auditoria Integral de Inventário e Quebras de Stock (`StockWasteService`):**
     - Registo de Quebras: evento `STOCK_WASTE_REGISTER` gravado contendo produto, armazém, quantidade, custo financeiro total em MZN e motivo.
     - Aprovação e Rejeição: eventos `STOCK_WASTE_APPROVE` e `STOCK_WASTE_REJECT` gravados contendo ID do registo, gestor aprovador e notas justificativas.
  3. **Auditoria de Limites de Crédito Comercial (`ComercialService`):**
     - Detecção automática de mutações em `updateClient`: evento `CLIENT_CREDIT_LIMIT_CHANGE` gravado com nome do cliente, ID, valor anterior e novo limite em MT.
  4. **Auditoria de Segurança e Privilégios de Utilizadores (`AppUserService`):**
     - Criação de utilizadores: `USER_CREATE` com perfil atribuído.
     - Alteração de papéis na empresa: `USER_ROLE_CHANGE` com registo do perfil anterior e novo perfil.
     - Definição/alteração de PIN de supervisor: `USER_PIN_SET`.
     - Reposição forçada de credenciais: `USER_PASSWORD_RESET`.
     - Ativação/desativação de contas: `USER_STATUS_CHANGE`.
  5. **Anulação de Documentos Fiscais e Comerciais:**
     - `INVOICE_CANCEL`, `RECEIPT_CANCEL`, `ORDER_CANCEL`, `QUOTATION_CANCEL`, `DELIVERY_GUIDE_CANCEL`, `CREDIT_NOTE_CANCEL`, `DEBIT_NOTE_CANCEL` registados com numeração e motivo obrigatório.
- **SPEC:** [docs/AUDIT_TRAIL_CRITICAL_EVENTS_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/AUDIT_TRAIL_CRITICAL_EVENTS_SPEC.md).
- **HARNESS:** [docs/AUDIT_TRAIL_CRITICAL_EVENTS_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/AUDIT_TRAIL_CRITICAL_EVENTS_HARNESS.md).
- **Testes Automatizados:** [AuditTrailCriticalEventsHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/modules/audit/AuditTrailCriticalEventsHarnessTest.java) (6/6 passing).
- **Validação Global:** 21 testes aprovados sem falhas (`MultiModuleArchitectureHarnessTest`, `RateLimitingAndBruteForceHarnessTest`, `AuditTrailCriticalEventsHarnessTest`, `DesktopThinContextTest`).
- **Execução e Lançamento:** Aplicação reempacotada com sucesso e inicializada: Backend UP em `http://localhost:8080/actuator/health` e Desktop interativo ativo na sessão Windows (PID 10572).

### SPEC-SEC-RL-001: Rate Limiting e Proteção Ativa contra Força Bruta (Login & PIN de Gerente) — 2026-10-03 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Implementação de proteção contra força bruta e adivinhação de credenciais baseada em SOLID/DRY e Defesa em Profundidade em duas frentes críticas:
  1. **Login de Utilizador e IP (`LoginRateLimiter`):**
     - Nível Utilizador: limite estrito de 5 falhas consecutivas, com bloqueio temporário de 15 minutos e mensagem de aviso com tempo restante em minutos.
     - Nível IP de Origem: proteção contra ataques de força bruta distribuída e *credential stuffing* (varredura de múltiplos utilizadores a partir de um mesmo IP, bloqueado após 30 falhas).
     - Reset automático e imediato de contadores ao autenticar com sucesso.
  2. **Verificação de PIN de Gerente/Supervisor (`ManagerPinRateLimiter`):**
     - O PIN de autorização de supervisor (4 dígitos numéricos, 10.000 combinações possíveis) passa a ser protegido contra adivinhação automatizada.
     - Limite de 3 tentativas falhadas por empresa/origem -> bloqueio de 5 minutos com resposta estruturada informando o tempo restante.
     - Reset imediato do contador ao submeter o PIN correto de um gestor/administrador ativo.
     - Isolamento estrito entre diferentes tenants e IPs.
- **SPEC:** [docs/RATE_LIMITING_AND_BRUTE_FORCE_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/RATE_LIMITING_AND_BRUTE_FORCE_SPEC.md).
- **HARNESS:** [docs/RATE_LIMITING_AND_BRUTE_FORCE_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/RATE_LIMITING_AND_BRUTE_FORCE_HARNESS.md).
- **Testes Automatizados:** [RateLimitingAndBruteForceHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/security/RateLimitingAndBruteForceHarnessTest.java) (7/7 passing).
- **Validação Global:** 16 testes aprovados no backend (`RateLimitingAndBruteForceHarnessTest`, `InputSecurityAndBackendValidationHarnessTest`, `MultiModuleArchitectureHarnessTest`, `SecurityPermissionGuardCoverageTest`) e 2 testes desktop (`DesktopThinContextTest`).
- **Execução e Lançamento:** Aplicação reempacotada com sucesso e inicializada: Backend UP em `http://localhost:8080/actuator/health` e Desktop interativo ativo na sessão Windows (PID 14960).

### SPEC-SEC-VAL-001: Segurança nos Inputs e Validação Global no Backend — 2026-10-03 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Implementação do padrão de defesa em profundidade (*Defense-in-Depth*) para garantir que nenhum input de utilizador é considerado confiável pelo backend, com validação declarativa e sanitização activa em todas as fronteiras da aplicação:
  1. **Anotações Canónicas de Validação (`contracts`):**
     - `@ValidNuit`: Validação de 9 dígitos com suporte a flag opcional de verificação estrita do algoritmo Módulo 11 da Autoridade Tributária e código de Consumidor Final (`999999999`).
     - `@ValidPhoneMZ`: Validação de operadoras móveis nacionais (82/83, 84/85, 86/87), linhas fixas (21-28) e prefixo internacional opcional `+258`.
     - `@ValidBiMZ`: Validação de Bilhete de Identidade moçambicano (12 dígitos numéricos + 1 letra maiúscula de controlo).
  2. **Higienização Activa (*Sanitization*) contra Ameaças (`InputSanitizer`):**
     - Remoção de bytes de controlo nulos (`\0`) e caracteres ASCII de controlo ocultos (`\x00-\x1F`), preservando inteiramente acentos moçambicanos e caracteres UTF-8.
     - Prevenção de Path Traversal e injeção de cabeçalhos em nomes de ficheiros exportados (`sanitizeFileName`).
     - Neutralização de injeções de script/XSS em textos livres e observações (`sanitizeNotes`).
     - Truncagem e normalização de queries de pesquisa (`sanitizeSearchQuery`).
  3. **Endurecimento de Respostas de Excepção (`GlobalExceptionHandler`):**
     - Mapeamento de `MethodArgumentNotValidException` e `ConstraintViolationException` para `HTTP 400 Bad Request` com lista descritiva de erros em português.
     - Interceptação de `DataIntegrityViolationException` devolvendo erro amigável sem expor tabelas, constraints ou internals de SQL/JDBC para o cliente HTTP.
     - Suporte a `IllegalArgumentException` sob `400 Bad Request`.
  4. **DTOs e Controladores Auditados e Protegidos com `@Valid` e Sanitização Activa:**
     - `CancelReasonRequest`, `CreateDeliveryGuideRequest`, `ConvertOrderToTransferRequest`, `CreateStockTransferRequest`, `UpdateStockTransferRequest`, `CreateStockWasteRequest`, `ApproveWasteRequest`, `CloseSessionRequest`, `ShiftHandoverRequest`, `CreatePurchaseOrderRequest`, `UpdatePurchaseOrderRequest`, `CreateQuotationRequest`, `UpdateQuotationRequest`.
     - `SaveClientRequest`, `CreateSupplierRequest`, `CreateCompanyRequest`, `CreateWarehouseRequest`, `UpdateInventoryItemCountRequest` e `UserSecurityRequestsDTOs`.
     - `UserController`, `StockWasteController`, `InventoryPhysicalCountingController`, `PrintController` e `ComercialController`.
- **SPEC:** [docs/SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_SPEC.md).
- **HARNESS:** [docs/SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_HARNESS.md).
- **Testes Automatizados:** [InputSecurityAndBackendValidationHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/security/InputSecurityAndBackendValidationHarnessTest.java) (7/7 passing).
- **Validação Global:** 15 testes aprovados (7 `InputSecurityAndBackendValidationHarnessTest`, 6 `MultiModuleArchitectureHarnessTest`, 2 `DesktopThinContextTest`), com conformidade total de fronteiras Maven.
- **Execução e Lançamento:** Aplicação reempacotada (`mvn clean package`) e inicializada: Backend UP em `http://localhost:8080/actuator/health` e Desktop interativo ativo na sessão Windows (PID 12084).

### SPEC-FAM-001: Fecho Automático e Manual de Mensagens de Feedback (Inline & Toast) — 2026-10-02 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Resposta ao requisito ergonómico de mensagens de feedback que aparecem, fecham sozinhas após tempo suficiente e podem ser fechadas a qualquer momento pelo operador:
  1. **Fecho Automático Temporizado (`InlineFeedbackPanel`):**
     - Temporizador `autoCloseTimer` integrado com calibragem por tipo semântico: `SUCCESS` (5s), `INFO` (5s), `WARNING` (7s), `ERROR` (8s).
     - Fecho limpo via EDT que actualiza o layout do ecrã pai (`revalidate()`, `repaint()`).
  2. **Pausa Inteligente ao Passar o Rato (`Hover Pause`):**
     - Implementado em [InlineFeedbackPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/InlineFeedbackPanel.java) e [ToastManager.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/ToastManager.java) através de `installHoverListeners` e `attachHoverRecursive`.
     - O temporizador é interrompido quando o rato está sobre a mensagem (`mouseEntered`) e reiniciado quando sai (`mouseExited`), garantindo que o operador nunca perde o conteúdo enquanto está a ler ou a interagir.
  3. **Fecho Manual Instantâneo:**
     - Botão de fechar com ícone `fas-times` acessível, cursor pointer e tooltip `"Fechar mensagem"`, cancelando temporizadores e escondendo o painel imediatamente.
  4. **Modo Persistente para Avisos Críticos:**
     - Suporte a sobrecargas `show(..., int autoCloseDurationMs)` permitindo passar `0` ou invocar `setAutoCloseEnabled(false)`.
- **SPEC:** [docs/FEEDBACK_AUTO_DISMISS_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/FEEDBACK_AUTO_DISMISS_SPEC.md).
- **HARNESS:** [FeedbackAutoCloseAndManualDismissHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/components/FeedbackAutoCloseAndManualDismissHarnessTest.java) (6/6 passing).
- **Validação Global:** 14 testes executados (6 `FeedbackAutoCloseAndManualDismissHarnessTest`, 6 `DataValidationAndRegexHarnessTest`, 2 `DesktopThinContextTest`) com 100% de aprovação.
- **Execução:** Desktop reempacotado e relançado na sessão do utilizador (PID 20844).

### SPEC-DVR-001: Validação de Dados, Regex Canónico e Filtros Reactivos (Frontend & Backend) — 2026-10-02 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Criar uma fonte única de verdade (Single Source of Truth) para validação de dados em Moçambique no módulo `contracts`, filtragem reactiva de digitação no `desktop` e validação de regras de negócio no `backend`:
  1. **Validação Canónica (`contracts`):** [ValidationPatterns.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/architecture/validation/ValidationPatterns.java) com regex e algoritmos oficiais:
     - `EMAIL_REGEX` / `isValidEmail`: Validação robusta de sintaxe de email.
     - `NUIT_REGEX` / `isValidNuit`: Exatamente 9 dígitos numéricos, algoritmo de dígito de controlo Módulo 11 da AT e código de Consumidor Final `999999999`.
     - `PHONE_MZ_REGEX` / `isValidPhone`: Vodacom (84/85), Tmcel (82/83), Movitel (86/87), linhas fixas (21-28) e prefixo internacional `+258`.
     - `BI_MZ_REGEX` / `isValidBi`: Bilhete de Identidade moçambicano (12 dígitos + 1 letra maiúscula).
     - `BARCODE_REGEX` e `SKU_REGEX`: Códigos de barras e identificadores limpos de artigo.
     - `isValidPositiveAmount`, `isValidNonNegativeAmount`, `isValidPercentage` e `cleanPhoneMozambique`.
  2. **Filtros Reactivos no Desktop (`UIHelper`):**
     - `UIHelper.installDigitsOnlyFilter(comp, maxLength)`: Intercepta e bloqueia caracteres não numéricos em tempo real.
     - `UIHelper.installUppercaseFilter(comp, maxLength)`: Converte letras minúsculas para maiúsculas automaticamente em campos alfanuméricos.
  3. **Validação Declarativa nos Formulários (`FormField`):**
     - Novos métodos de validação com feedback visual inline em Moçambicano: `validateEmail()`, `validateNuit()`, `validatePhone()`, `validateMinLength()`, `validateRegex()`.
     - Foco automático e preservação dos dados preenchidos no diálogo em caso de erro.
  4. **Formulários Actualizados:**
     - [ClientesPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/ClientesPanel.java): Nome, NUIT (com filtro de 9 dígitos), Email.
     - [PurchaseSuppliersPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PurchaseSuppliersPanel.java): Nome, NUIT (com filtro de 9 dígitos), Telefone, Email.
  5. **Backend (`backend`):**
     - `TaxIdValidator.java` actualizado para delegar na regra canónica `ValidationPatterns.NUIT_PATTERN`.
- **SPEC:** [docs/DATA_VALIDATION_AND_REGEX_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/DATA_VALIDATION_AND_REGEX_SPEC.md).
- **HARNESS:** [DataValidationAndRegexHarnessTest.java](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/DataValidationAndRegexHarnessTest.java) (6/6 passing).
- **Validação Global:** 14 testes executados (6 `DataValidationAndRegexHarnessTest`, 2 `DesktopThinContextTest`, 6 `MultiModuleArchitectureHarnessTest`) com 100% de sucesso.
- **Execução:** Desktop reempacotado e reiniciado interativamente via `schtasks` (PID 22964).

### SPEC-ACI-001: Intercepção Activa de Crédito e Risco na Faturação e POS — 2026-10-02 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Prevenção proactiva de risco de crédito no ponto de venda e na emissão de faturas:
  1. **POS (`POSPanel`):** Vendas a crédito (`fiado`) exigem obrigatoriamente a seleção de cliente cadastrado e bloqueiam clientes com limite 0,00 MT (pronto pagamento exclusivo) antes de invocar a finalização.
  2. **Faturação (`ComercialPanel`):** Pre-flight check no salvamento de rascunhos de faturas através de `CustomerCreditValidator.validateCreditPreFlight`, informando o operador sem quebrar o estado da grelha.
  3. **Motor Canónico:** [CustomerCreditValidator.java](desktop/src/main/java/mz/multicore/erp/gui/components/CustomerCreditValidator.java) com validação de regras de limite, dívida e clientes flexíveis.
- **SPEC:** [docs/ACTIVE_CREDIT_INTERCEPTION_SPEC.md](docs/ACTIVE_CREDIT_INTERCEPTION_SPEC.md).
- **HARNESS:** [ActiveCreditInterceptionHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/ActiveCreditInterceptionHarnessTest.java) (2/2 passing).
- **Validação Global:** 12/12 testes desktop aprovados sem falhas (`ActiveCreditInterceptionHarnessTest`, `DesktopThinContextTest`, etc.).
- **Execução:** Reempacotado e reiniciado interativamente via `schtasks` (PID 13116).

### Pacote de 4 Melhorias Operacionais: Navegação PHC, Validação NUIT/Crédito, Totais no Rodapé e Etiquetas na Recepção — 2026-10-02 — **concluída com SPEC e HARNESS**

- **1. Navegação Fluida por Teclado nas Grelhas PHC (SPEC-GCN-001):**
  - Navegação tipo folha de cálculo com `ENTER` e `TAB` para a próxima coluna editável, `Shift+TAB` para a anterior, e adição automática de nova linha ao premir `ENTER`/`TAB` na última célula editável da última linha.
  - SPEC: [docs/GRID_CELL_NAVIGATION_SPEC.md](docs/GRID_CELL_NAVIGATION_SPEC.md).
  - IMPLEMENTAÇÃO: [UIHelper.java](desktop/src/main/java/mz/multicore/erp/gui/components/UIHelper.java) (`installCellNavigationKeys`, `findNextEditableColumn`, `findPrevEditableColumn`).
  - HARNESS: [GridCellNavigationHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/GridCellNavigationHarnessTest.java) (2/2 passing).

- **2. Validação de NUIT Moçambicano e Limite de Crédito (SPEC-NCL-001):**
  - Algoritmo Módulo 11 da Autoridade Tributária de Moçambique, suporte a consumidor final `999999999`, e validador de crédito [CustomerCreditValidator.java](desktop/src/main/java/mz/multicore/erp/gui/components/CustomerCreditValidator.java) com bloqueio por faturas vencidas e saldo excedido.
  - SPEC: [docs/NUIT_AND_CREDIT_LIMIT_VALIDATION_SPEC.md](docs/NUIT_AND_CREDIT_LIMIT_VALIDATION_SPEC.md).
  - IMPLEMENTAÇÃO: [NuitValidator.java](desktop/src/main/java/mz/multicore/erp/gui/components/NuitValidator.java), [CustomerCreditValidator.java](desktop/src/main/java/mz/multicore/erp/gui/components/CustomerCreditValidator.java), [ClientesPanel.java](desktop/src/main/java/mz/multicore/erp/gui/ClientesPanel.java).
  - HARNESS: [NuitAndCreditLimitValidationHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/NuitAndCreditLimitValidationHarnessTest.java) (2/2 passing).

- **3. Barra de Totais e Estatísticas Instantâneas no Rodapé das Tabelas (SPEC-DTT-001):**
  - Rodapé dinâmico (`ClientTablePagination`) reagindo em tempo real à seleção de linhas (`ListSelectionListener`), calculando somatório de valores monetários a 2 casas decimais (`[Total: X.XX MT]`) e quantidades (`[Qtd: X.XX]`).
  - SPEC: [docs/DYNAMIC_TABLE_TOTALS_FOOTER_SPEC.md](docs/DYNAMIC_TABLE_TOTALS_FOOTER_SPEC.md).
  - IMPLEMENTAÇÃO: [ClientTablePagination.java](desktop/src/main/java/mz/multicore/erp/gui/components/ClientTablePagination.java).
  - HARNESS: [DynamicTableTotalsFooterHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/DynamicTableTotalsFooterHarnessTest.java) (1/1 passing).

- **4. Impressão Directa de Etiquetas na Recepção de Mercadorias (SPEC-PRL-001):**
  - Emissão imediata de etiquetas com códigos de barras e preços a partir das quantidades conferidas em bom estado no assistente [PurchaseOrderReceivingDialog.java](desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrderReceivingDialog.java), além de opção no menu de gestão de [PurchaseOrdersPanel.java](desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java).
  - SPEC: [docs/PURCHASE_RECEIVING_LABEL_PRINTING_SPEC.md](docs/PURCHASE_RECEIVING_LABEL_PRINTING_SPEC.md).
  - IMPLEMENTAÇÃO: [PurchaseOrderReceivingDialog.java](desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrderReceivingDialog.java), [PurchaseOrdersPanel.java](desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java).
  - HARNESS: [PurchaseReceivingLabelPrintingHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/PurchaseReceivingLabelPrintingHarnessTest.java) (3/3 passing).

- **Testes Globais Executados:** 16 testes passando (10 desktop + 6 backend) com 0 falhas e 0 erros.

### Pacote de Modernização Operacional: Conversão Documental, Omnibar, Recepção de Compras e Caixa POS — 2026-10-02 — **concluída com SPEC e HARNESS**

- **1. Conversão Directa entre Documentos (SPEC-DCW-001):**
  - Rastreabilidade e conversão com 1 clique: Cotação ➔ Encomenda ➔ Guia de Remessa / Factura.
  - SPEC: [docs/DOCUMENT_CONVERSION_WORKFLOW_SPEC.md](docs/DOCUMENT_CONVERSION_WORKFLOW_SPEC.md).
  - HARNESS: [DocumentConversionWorkflowHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/DocumentConversionWorkflowHarnessTest.java) (3/3 passing).
- **2. Barra de Pesquisa Global Rápida — Omnibar (SPEC-GOB-001):**
  - Atalho global `Ctrl+K` em toda a aplicação com índice de módulos, acções operacionais e comandos rápidos.
  - SPEC: [docs/GLOBAL_OMNIBAR_SPEC.md](docs/GLOBAL_OMNIBAR_SPEC.md).
  - HARNESS: [GlobalOmnibarHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/GlobalOmnibarHarnessTest.java) (2/2 passing).
- **4. Recepção e Conferência de Mercadorias (SPEC-POR-001):**
  - Assistente modal [PurchaseOrderReceivingDialog.java](desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrderReceivingDialog.java) com segregação de mercadoria em bom estado, avarias e faltas definitivas, formatado a 2 casas decimais.
  - SPEC: [docs/PURCHASE_ORDER_RECEIVING_SPEC.md](docs/PURCHASE_ORDER_RECEIVING_SPEC.md).
  - HARNESS: [PurchaseOrderReceivingHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/PurchaseOrderReceivingHarnessTest.java) (2/2 passing).
- **5. Gestão de Sangrias, Suprimentos e Validação de Caixa (SPEC-PCM-001):**
  - Assistente modal canónico [PosCashMovementDialog.java](desktop/src/main/java/mz/multicore/erp/gui/pos/PosCashMovementDialog.java) com `MoneyField`, motivo obrigatório e auditoria em tempo real.
  - SPEC: [docs/POS_CASH_MOVEMENTS_SPEC.md](docs/POS_CASH_MOVEMENTS_SPEC.md).
  - HARNESS: [PosCashMovementsHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/pos/PosCashMovementsHarnessTest.java) (2/2 passing).
- **Testes Globais:** 14 testes desktop aprovados sem falhas + `MultiModuleArchitectureHarnessTest` (6/6 passing).
- **Execução:** Reempacotado e reiniciado interativamente via `schtasks` (PID 24408).

### SPEC-DTI-001: Padrão Universal de Interacção com Tabelas (Duplo Clique e Atalhos) — 2026-10-02 — **concluída com SPEC e HARNESS**

- **Decisão e Princípio:** Garantir ergonomia ágil e velocidade de balcão uniforme em 100% dos documentos do ERP:
  1. **Duplo Clique em Listagens:** Duplo clique abre directamente o editor / consulta do documento seleccionado:
     - `ordersTable` (Encomendas de Cliente): `owner.openSelectedOrderEditor()`
     - `invoicesTable` (Faturas Recentes): `owner.printSelectedInvoice()`
     - `table` (Cotações): `this.openSelectedEditor()`
     - `poListTable` (Encomendas a Fornecedor): `this.openSelectedEditor()`
     - `transferTable` (Transferências de Stock): `transferActions.openSelectedEditor()`
  2. **Atalhos Universais nas Grelhas de Itens (`UIHelper.installDocumentGridShortcuts`):**
     - `INSERT` / `Ctrl+ENTER`: Adicionar nova linha em branco na grelha.
     - `DELETE` / `Ctrl+DELETE`: Remover linha seleccionada na grelha (protegido quando a célula está em modo de edição de texto).
     - `Ctrl+S` / `F10`: Gravar / submeter documento directamente do teclado.
- **SPEC:** [docs/DOCUMENT_TABLE_INTERACTION_SPEC.md](docs/DOCUMENT_TABLE_INTERACTION_SPEC.md).
- **HARNESS:** [DocumentTableInteractionHarnessTest.java](desktop/src/test/java/mz/multicore/erp/gui/DocumentTableInteractionHarnessTest.java) (5/5 passing).
- **Validação de Testes:** 27 testes aprovados sem falhas (`DocumentTableInteractionHarnessTest`, `EditableDocumentEditorsHarnessTest`, `PhcDocumentEditorHomologationHarnessTest`, `OrderEditorHarnessTest`, `StockTransferDraftUiHarnessTest`, `DesktopThinContextTest`, `MultiModuleArchitectureHarnessTest`).
- **Execução:** Reempacotado e reiniciado interativamente via `schtasks` (PID 2440).

### Uniformização Canónica de Editores de Documentos (Cabeçalhos em Linha e Grelhas PHC) — 2026-10-02 — **concluída com sucesso**

- **Decisão e Princípio:** Garantir que todas as telas de documentos operacionais (Encomendas de Cliente, Cotações, Encomendas a Fornecedor, Transferências de Stock e Faturação) funcionem exactamente da mesma forma canónica:
  1. **Cabeçalho Compacto em Linha:** substituição de formulários verticais empilhados ou grids altos por cartões horizontais de campos agrupados (`fieldGroup` com label superior e dimensão padronizada), libertando mais de 140px de área vertical útil para os itens.
  2. **Grelha PHC em Linha Padronizada:** grelhas editáveis directamente nas células (`Produto`, `Qtd`, `Emb.`, `Cx.`, ...), sincronização instantânea de embalagens/caixas/preços, foco imediato após inserção de linha e supressão explícita de paginação cliente automática (`ClientTablePagination.DISABLED` e `noTableFooter`).
  3. **Resolução de Jitter/Tremores:** eliminação definitiva de tremores de viewport e scrolls concorrentes em tabelas embebidas.
- **Ficheiros Padronizados:**
  - [CommercialOrdersView.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CommercialOrdersView.java): Cabeçalho compacto em linha horizontal com `kindHint` discreto.
  - [StockTransferEditorForm.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockTransferEditorForm.java): Formulário horizontal, eliminação do jitter e grelha com Produto na coluna 0.
  - [QuotationEditorForm.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/commercial/QuotationEditorForm.java): Substituição do formulário de 8 linhas empilhadas por card de 2 linhas horizontais e desativação de paginação acidental na grelha.
  - [PurchaseOrdersPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/PurchaseOrdersPanel.java): Cabeçalho compacto em linha horizontal (`Fornecedor`, `Armazém`, `Entrega Prevista`, `Observações`) e tabela com `ClientTablePagination.DISABLED`.
  - [CommercialInvoicesView.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/CommercialInvoicesView.java): Cabeçalho compacto em linha horizontal (`Cliente`, `Armazém de Expedição`), remoção de resíduos de labels órfãos e grelha padronizada.
- **Validação de Testes:**
  - `EditableDocumentEditorsHarnessTest` (4/4 passing)
  - `PhcDocumentEditorHomologationHarnessTest` (4/4 passing)
  - `OrderEditorHarnessTest` (4/4 passing)
  - `StockTransferDraftUiHarnessTest` (2/2 passing)
  - `DesktopThinContextTest` (2/2 passing)
  - `MultiModuleArchitectureHarnessTest` (6/6 passing)
- **Execução:** Reempacotado e reiniciado interativamente via `schtasks` (PID 1752).

### Edição Documental PHC Directamente nas Grelhas — 2026-09-29 — **concluída com SPEC e HARNESS**

- **Decisão funcional:** documentos em preparação são criados e actualizados numa área de trabalho
  no próprio separador; os artigos são editados directamente nas células da tabela, sem formulário
  lateral, diálogo de artigo ou fluxo dependente de duplo clique.
- **Âmbito migrado:** cotações, encomendas de clientes, encomendas a fornecedores, preparação de
  facturas e transferências de stock. Produto pesquisável, quantidade, embalagem, caixa, percentagem
  da caixa, desconto e dados de rastreabilidade aparecem conforme a lógica de cada documento.
- **Limites de negócio:** facturas emitidas e documentos submetidos/terminais permanecem somente
  para consulta; notas de crédito/débito e recepção parcial continuam como operações curtas de
  confirmação, mas as suas linhas já são editadas directamente na respectiva grelha.
- **SPEC/HARNESS:** `docs/PHC_INLINE_DOCUMENT_GRID_SPEC.md` e
  `docs/PHC_INLINE_DOCUMENT_GRID_HARNESS.md`.
- **Validação dirigida:** 21 testes, zero falhas — `PhcInlineDocumentGridHarnessTest`,
  `OrderEditorHarnessTest`, `EditableDocumentEditorsHarnessTest`,
  `StockTransferDraftUiHarnessTest`, `DesktopThinContextTest` e
  `MultiModuleArchitectureHarnessTest`.

### Uniformização Geral de Botões Actualizar e Posicionamento de Cards/Sumários no Topo — 2026-09-28 — **concluída com auditoria e testes**

- **Necessidade do Negócio:** Garantir conformidade ergonómica rigorosa em 100% dos ecrãs e tabelas do sistema Desktop Swing: nenhum botão de "Actualizar" escondido dentro de menus suspensos (*dropdowns*), disponibilização de botão autónomo directo com ícone vetorial `fas-sync-alt` em todas as tabelas de listagem de dados, posicionamento obrigatório de todos os cartões de KPI, blocos de métricas e sumários no topo (acima da grelha de dados) e contenção estrita em `ModernPanel(16)` via `UIHelper.tableCardTop(...)`.
- **Painéis e Ecrãs Migrados/Ajustados:**
  1. **Inventário:** `PhysicalInventoryPanel` (cards de KPI no topo + `refreshBtn` no card top).
  2. **Recursos Humanos:** `HRContractsPanel`, `HRDeductionsPanel`, `HRExpensesPanel`, `HRTerminationsPanel`, `HRTimeSheetPanel`, `HRVacationsPanel` e `HRPanel` (Colaboradores, Recibos de Salário e Registo de Faltas) todos com botões autónomos `UIHelper.createRefreshButton(...)` nos cabeçalhos das tabelas.
  3. **Stock & Armazéns:** `StockBatchesPanel`, `StockPanel` (Rastreabilidade/Movimentos e Transferências), `StockAlertsPanel` (sub-abas com `tableCardTop` e botão unificado de actualização).
  4. **Aprovações & CRM:** `ApprovalsPanel` (Pedidos a Aguardar e Histórico de Decisões) e `CRMPanel` (Folhas de Obra) com botões de actualizar visíveis.
  5. **Comercial & Fiscal:** `DeliveryGuidesPanel` (removido do dropdown de acções para botão directo no topo), `FiscalPanel` (Taxas Fiscais e Retenções na Fonte), `BankReconciliationPanel` (adicionado na barra de acções).
  6. **Contas Correntes:** `OutstandingAccountsPanel` (migrado `agingSummary` do rodapé para o cabeçalho superior integrado sob a barra de filtros, eliminando rodapés soltos).
- **Validação Automatizada:**
  - `TableActionPlacementHarnessTest`: **6/6 testes aprovados** (incluindo verificação explícita de `StockAlertsPanel`).
  - `TableCardContainmentAuditTest`: **2/2 testes aprovados** (zero tabelas soltas fora de `ModernPanel`).
  - `DesktopThinContextTest`: **2/2 testes aprovados** (arranque integral do contexto desktop).
  - `MultiModuleArchitectureHarnessTest`: **6/6 testes aprovados** (respeito absoluto pelas fronteiras maven `contracts`, `backend` e `desktop`).
  - `mvn compile`: compilação limpa em todos os módulos sem erros.

### Transferências de Stock Editáveis por Rascunho — 2026-09-28 — **concluída com SPEC e HARNESS**

- **Necessidade do negócio:** substituir o formulário modal de transferência por edição profissional
  no próprio separador, com todos os itens visíveis, pesquisa de produtos, composição de embalagens
  e aprovação separada da preparação.
- **SPEC/HARNESS:** `docs/STOCK_TRANSFER_DRAFT_LIFECYCLE_SPEC.md` e
  `docs/STOCK_TRANSFER_DRAFT_LIFECYCLE_HARNESS.md`.
- **Ciclo entregue:** `DRAFT → PENDING_APPROVAL → APPROVED | REJECTED`, com cancelamento apenas
  em `DRAFT`/`PENDING_APPROVAL`; estados terminais são somente consulta.
- **Persistência/contratos:** migration `V78__stock_transfer_draft_lifecycle.sql`, `@Version`, campo
  `version` retrocompatível no `StockTransferDTO` e novo `UpdateStockTransferRequest`.
- **API:** `PUT /api/inventory/transfers/{id}` e
  `POST /api/inventory/transfers/{id}/submit`, além das operações existentes de decisão/cancelamento.
- **Desktop:** área de trabalho exclusivamente tabular no modelo PHC: dados gerais numa grelha de
  uma linha e artigos noutra grelha, com pesquisa do produto dentro da célula e edição directa de
  `Qtd`, `Emb.` e `Cx.`; sem formulário ou diálogo de edição, com acções no topo do card.
- **Reposição interna:** conversões criam rascunho e orientam o utilizador a rever e submeter em
  Stock; apenas a aprovação movimenta FEFO e fecha a reposição.
- **Validação:** 40 testes dirigidos, zero falhas (`StockTransferServiceTest`, integração HTTP H2,
  `MultiModuleArchitectureHarnessTest`, `DesktopThinContextTest` e harnesses de UI); `mvn clean
  compile` e empacotamento concluídos.
- **Execução:** backend `UP` e desktop interactivo relançado com o pacote actualizado.

### Passagem de Turno Multi-operador e Reconciliação Parcial no POS — 2026-09-27 — **concluída com SPEC e HARNESS**
- **Necessidade do Negócio:** Permitir a rotação de operadores de caixa ao longo do dia comercial sem fechar a sessão mãe nem fragmentar a contabilidade do fecho Z, garantindo handover auditável com contagem cega física (notas e moedas MZN), cálculo de divergências e transferência atómica de posse.
- **Solução Implementada de Ponta a Ponta:**
  1. **Especificação & Harness:** [docs/POS_PASSAGEM_TURNO_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/POS_PASSAGEM_TURNO_SPEC.md) (`SPEC-POS-PASSAGEM-TURNO-001`) e [docs/POS_PASSAGEM_TURNO_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/POS_PASSAGEM_TURNO_HARNESS.md) (`HARNESS-POS-PASSAGEM-TURNO-001`).
  2. **Base de Dados & Migração Flyway (`V76__shift_reconciliations.sql`):** Tabela `shift_reconciliations` com chaves estrangeiras, índices e coluna `current_operator` em `till_sessions`.
  3. **Contratos (`contracts`):**
     - DTOs `ShiftHandoverRequest` e `ShiftReconciliationDTO`.
     - `TillSessionDTO` e `PosZReportDTO` com campo `currentOperator`, lista `shiftReconciliations` e construtores retrocompatíveis.
  4. **Backend Headless (`backend`):**
     - Entidade `ShiftReconciliation` e repositório `ShiftReconciliationRepository`.
     - `TillSessionRepository.findActiveSessionForOperator`: query atómica que resolve a sessão aberta tanto para operadores pós-handover quanto para sessões mono-operador.
     - `POSService.performShiftHandover`: validações semânticas, controlo de permissão para divergências (`PermissionGuard.requireManagerOrAdmin`), persistência e auditoria `POS_SHIFT_HANDOVER`.
     - `POSService.getShiftReconciliations` e `POSService.buildZReport` com integração de turnos.
     - `POSZReportPrintService`: renderização PDF no fecho Z incluindo tabela de passagens de turno e operadores.
  5. **Desktop Swing (`desktop`):**
     - `POSApiClient`: métodos de handover e consulta de reconciliações.
     - `PosShiftHandoverDialog`: diálogo modal ergonómico com suporte a contagem por notas/moedas MZN e total directo.
     - `PosCashSessionActions.shiftHandover`: invocação da acção no fluxo de sessão.
     - `POSPanel`: botão "Passar Turno" (`fas-people-arrows`) e banner de estado de caixa com operador activo. Painel mantido estritamente em **990 linhas** (< 1000 linhas obrigatório).
- **Harness & Validação Automatizada:**
  - `PosShiftHandoverHarnessTest`: **4/4 aprovados** (handover exacto, divergência com permissão, validações de integridade e múltiplos turnos consecutivos).
  - `PosZReportHarnessTest`: **4/4 aprovados**.
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `MultiModuleArchitectureHarnessTest`: **6/6 aprovados**.
  - `PosKeyboardShortcutsHarnessTest`: **6/6 aprovados**.
  - `QuotationConversionUiHarnessTest`: **4/4 aprovados**.
  - Compilação limpa e empacotamento Maven de `contracts`, `backend` e `desktop` concluídos com sucesso.
  - Backend `UP` na porta 8080 e aplicação Desktop lançada na sessão interativa.

### Editor Unificado para Criar e Actualizar Encomendas — 2026-09-27 — **concluído com SPEC e HARNESS**
- O formulário modal foi substituído pelo mesmo editor de página inteira já alojado no separador
  de Encomendas: `Nova encomenda` abre vazio e `Editar / Consultar` carrega cabeçalho e itens.
- A tabela do editor permite seleccionar ou fazer duplo clique, actualizar o item na mesma posição
  e removê-lo; produto pesquisável, composição Caixa/Embalagem/Unidade, FEFO, desconto e série são
  preservados. A barra superior mantém `Voltar à lista` e `Guardar alterações`; totais ficam no rodapé.
- Contrato `UpdateOrderRequest`, `PUT /api/comercial/orders/{id}` e `OrderDTO.version` implementam
  gravação transaccional com bloqueio optimista. O backend preserva número/tipo, recalcula preço,
  IVA e totais, revalida reservas antes da separação e reabre aprovação de encomendas formais.
- Estados editáveis: `PENDING_APPROVAL`, `PENDING` e `AWAITING_SEPARATION`; estados posteriores são
  consulta e continuam bloqueados no Service mesmo que a UI seja contornada.
- SPEC/HARNESS: `docs/ORDER_EDITOR_SPEC.md`, `docs/ORDER_EDITOR_HARNESS.md` e
  `OrderEditorHarnessTest`; regras cobertas também em `ComercialServiceTest`.
- Validação concluída: `OrderEditorHarnessTest`, `ComercialServiceTest`,
  `MultiModuleArchitectureHarnessTest`, `DesktopThinContextTest`, harnesses de tabelas/pesquisa,
  `POSServiceTest` e `MoneyFlowHttpIntegrationTest` aprovados; `mvn clean compile` aprovado.
- Corrigida a selecção do construtor principal de `POSService` com `@Autowired`, preservando os
  construtores sobrecarregados usados pelos harnesses; backend confirmado `UP` e desktop reiniciado.

### Conversão Direta de Cotações e Pró-formas em Fatura Comercial ou Venda no POS — 2026-09-27 — **concluída com SPEC e HARNESS**
- **Necessidade do Negócio:** Fechar o ciclo comercial entre orçamentos/propostas aprovadas (série `CT`) e a sua liquidação, permitindo quer a conversão direta em Factura Comercial B2B (`FT`), quer a importação rápida no POS ao balcão, garantindo que o preço acordado com o cliente na cotação tem precedência sobre o preço do catálogo e que cotações caducadas não podem ser faturadas sem estender a validade.
- **Solução Implementada de Ponta a Ponta:**
  1. **Especificação & Harness:** [docs/COTACAO_CONVERSAO_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/COTACAO_CONVERSAO_SPEC.md) (`SPEC-COTACAO-CONVERSAO-001`) e [docs/COTACAO_CONVERSAO_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/COTACAO_CONVERSAO_HARNESS.md) (`HARNESS-COTACAO-CONVERSAO-001`).
  2. **Base de Dados & Migração Flyway (`V75__quotation_invoice_conversion.sql`):** Adição de `invoice_id` e `invoice_number` com índice `idx_quotations_company_invoice` na tabela `quotations`.
  3. **Contratos (`contracts`):**
     - `QuotationDTO`: campos `invoiceId` e `invoiceNumber` preservando construtor retrocompatível.
     - `POSCheckoutLineRequest`: campo `BigDecimal unitPrice` com construtores retrocompatíveis.
     - `POSCheckoutRequest`: campo `Long quotationId` com construtores sobrecarregados retrocompatíveis.
  4. **Backend Headless (`backend`):**
     - `Quotation`: colunas `invoiceId` e `invoiceNumber`.
     - `QuotationRepository`: métodos `findOpenByCompanyIdWithLines` e `findByIdAndCompanyId`.
     - `ComercialService.createInvoiceFromQuotation`: emissão de fatura comercial herdando preços cotados, com verificação de crédito e baixa de stock.
     - `QuotationService.convertToInvoice`: conversão direta com bloqueio de caducadas e dupla conversão.
     - `QuotationService.findOpenByCompany`: filtro de cotações abertas e vigentes.
     - `POSService.checkout`: honra de `customUnitPrice` nas linhas e conversão atómica da cotação para `CONVERTED` com auditoria `QUOTATION_CONVERT_POS`.
  5. **Desktop Swing (`desktop`):**
     - `QuotationsPanel`: opção `Converter em Factura` (`fas-file-invoice-dollar`) no `ActionMenuButton` sem emojis crus, mantendo o painel com 361 linhas.
     - `PosCartItem`: suporte a `customUnitPrice` e cálculo dinâmico de subtotais e taxas de IVA.
     - `PosImportQuotationDialog` (232 linhas < 1000): diálogo modal isolado com busca incremental por número/cliente/NUIT e seleção rápida.
     - `PosQuotationActions` (179 linhas < 1000): lógica de importação para o carrinho, cliente e armazém.
     - `PosReceiptPrinter`: extração da impressão de recibos térmicos.
     - `POSPanel`: botão "Cotação" (`F7`), banner dinâmico de cotação vinculada com remoção de vínculo e checkout integrado. Painel mantido estritamente em **992 linhas** (< 1000 linhas obrigatório).
- **Harness & Validação Automatizada:**
  - `QuotationConversionHarnessTest`: **5/5 aprovados** (conversão em FT, bloqueio de expiradas, bloqueio de dupla conversão, listagem de abertas e checkout POS com cotação).
  - `QuotationConversionUiHarnessTest`: **4/4 aprovados** (ação sem emojis em QuotationsPanel, diálogo instanciável, precedência de preço cotado no PosCartItem e verificação de < 1000 linhas em ficheiros Swing).
  - `PosKeyboardShortcutsHarnessTest`: **6/6 aprovados**.
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `MultiModuleArchitectureHarnessTest`: **6/6 aprovados** (estrita aderência de fronteiras físicas Maven).
  - Compilação limpa e empacotamento Maven concluídos com sucesso.
  - Backend `UP` na porta 8080 e aplicação Desktop lançada na sessão interativa.

### Atalhos Rápidos de Teclado no POS (F1 a F12 + Barra Visual de Rodapé + Guia F1) — 2026-09-27 — **concluída com SPEC e HARNESS**
- **Necessidade do Negócio:** Dotar os operadores de caixa de alta velocidade no balcão de atendimento comercial sem dependência de rato, com mapa canónico de teclas de função (`F1` a `F12`), atalhos operacionais (`ESC`, `DELETE`, `+ / -`, `CTRL+N`), aplicação de desconto ágil (`F5`), cancelamento seguro com confirmação e barra de atalhos rápida no rodapé para ecrãs tácteis e operadores novatos.
- **Solução Implementada de Ponta a Ponta:**
  1. **Especificação & Harness:** [docs/POS_KEYBOARD_SHORTCUTS_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/POS_KEYBOARD_SHORTCUTS_SPEC.md) (`SPEC-POS-KEYBOARD-001`) e [docs/POS_KEYBOARD_SHORTCUTS_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/POS_KEYBOARD_SHORTCUTS_HARNESS.md) (`HARNESS-POS-KEYBOARD-001`).
  2. **Gestor Central de Atalhos (`PosKeyboardShortcutsHandler.java`):** Registo unificado no `InputMap` (`WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`) e `ActionMap` de todas as teclas:
     - `F1`: Guia de Atalhos (`PosShortcutHelpDialog`).
     - `F2`: Pesquisa de Artigo no Catálogo.
     - `F3`: Foco no Leitor de Código de Barras.
     - `F4`: Pesquisa / Identificação do Cliente.
     - `F5`: Desconto de Linha com diálogo modal percentual.
     - `F6`: Alteração da Quantidade da Linha do Carrinho.
     - `F7`: Cartão de Fidelização & Pontos.
     - `F8`: Devoluções & Vales de Compras (`PosReturnDialog`).
     - `F9`: Movimentos de Caixa (Sangria/Suprimento).
     - `F10`: Finalizar Venda / Pagamento (`PosPaymentDialog`).
     - `F11`: Alternar Vista (Venda Activa vs Histórico de Vendas).
     - `F12`: Fecho Cego de Caixa (`PosBlindCloseDialog`).
     - `ESC`: Cancelar / Limpar Carrinho (com confirmação se houver artigos).
     - `DELETE`, `+` e `-`: Ajuste de linhas e quantidade na tabela do carrinho.
     - `CTRL+N`: Nova Venda.
  3. **Barra de Atalhos do Rodapé (`PosShortcutBar.java`):** Chips compactos e interactivos com badges monoespaçadas, ícones vetoriais FontAwesome e suporte a toque/rato sem emojis Unicode crus.
  4. **Guia Modal de Atalhos (`PosShortcutHelpDialog.java`):** Diálogo moderno categorizado em *Artigos & Carrinho*, *Caixa & Operações* e *Geral & Navegação*.
  5. **Decomposição e Optimização do `POSPanel.java`:** Redução de linhas de 997 para **985 linhas** (< 1000 linhas obrigatório), garantindo total separação de responsabilidades.
- **Harness & Validação:**
  - `PosKeyboardShortcutsHarnessTest`: **6/6 aprovados** (incluindo `KEY-06: testNoDuplicateSearchIconsInPosPanel`).
  - `POSKeyboardShortcutTest`: **2/2 aprovados**.
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `PosVoucherUiHarnessTest`: **5/5 aprovados**.
  - `TableCardContainmentAuditTest` & `UiPanelDecompositionTest`: **100% aprovados**.
  - **Correção Visual no POS:** Eliminada duplicação do ícone de pesquisa no campo de cliente e produto (`clientSearchField` e `productSearchField` refatorados para `new JTextField()` com estilo unificado, mantendo exatamente um único ícone de lupa provido por `PosLayout.searchRow`).
  - Backend UP na porta 8080 e Desktop activo na sessão interactiva.

### Posicionamento Uniforme das Acções de Tabelas — 2026-09-27 — **concluída com SPEC e HARNESS**
- Definido o padrão único: acções globais no cabeçalho, filtros e acções da lista no topo do card,
  tabela no centro e apenas paginação, totais ou estado informativo no rodapé.
- Migradas as áreas de Facturas (incluindo linhas do rascunho), Encomendas, Compras, Recibos,
  Cotações, Guias de Remessa, Notificações, Auditoria, Backups, Contabilidade e tabelas editáveis
  de notas e transferências.
- Barras densas foram agrupadas em `ActionMenuButton`, respeitando o limite de três controlos
  visíveis e cinco opções por menu.
- SPEC/HARNESS: `docs/TABLE_ACTION_PLACEMENT_SPEC.md`,
  `docs/TABLE_ACTION_PLACEMENT_HARNESS.md` e `TableActionPlacementHarnessTest`.
- Validação: `mvn clean compile`, suite focada de UI e `DesktopThinContextTest` aprovados; JARs
  empacotados, backend `UP` e desktop reiniciado na sessão interactiva.
- Refinamento de Facturação: todas as acções passaram para a primeira fila do próprio card, em
  `Actualizar`, `Mais acções` e `Emitir`; pesquisa, estado e período ocupam a segunda fila. A
  paginação de servidor passou a seguir a mesma hierarquia visual das Notas de Crédito.
- Uniformização transversal concluída com `UIHelper.tableCardTop(...)`: o mesmo cabeçalho interno,
  faixa de filtros e limite de três acções visíveis foi aplicado às listagens de Comercial,
  Clientes, Compras, Stock, Fiscal, CRM, RH, Plataforma, Notificações e Utilizadores.

### Devoluções e Trocas no POS com Emissão de Vale de Compras / Saldo a Favor do Cliente — 2026-09-27 — **concluída com SPEC e HARNESS**
- **Necessidade do Negócio:** Implementação do fluxo completo de devolução e troca de artigos no POS comercial com reposição de stock, escolha do método de reembolso (Reembolso financeiro, Crédito em conta corrente ou Emissão de Vale de Compras alfanumérico / Store Credit com validade de 90 dias), emissão de talão térmico de 80mm com código de barras, e suporte a resgate e abatimento total ou parcial do vale no checkout do POS (`PosPaymentDialog`).
- **Solução Implementada de Ponta a Ponta:**
  1. **Especificação & Harness:** [docs/POS_DEVOLUCOES_VALE_COMPRAS_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/POS_DEVOLUCOES_VALE_COMPRAS_SPEC.md) (`SPEC-POS-VOUCHER-001`) e [docs/POS_DEVOLUCOES_VALE_COMPRAS_HARNESS.md](file:///c:/Users/miran/Desktop/manager/docs/POS_DEVOLUCOES_VALE_COMPRAS_HARNESS.md) (`HARNESS-POS-VOUCHER-001`).
  2. **Base de Dados (`V74__store_vouchers.sql`):** Criação da tabela `store_vouchers` com integridade referencial, índices em `code`, `company_id`, `client_id`, `status` e controlo de saldos inicial e restante.
  3. **Contratos (`contracts`):** DTOs `StoreVoucherDTO` (com helpers de vigência e estado) e `POSReturnResultDTO`, além de enum de método de pagamento `STORE_CREDIT`.
  4. **Backend (`backend`):** Entidade JPA `StoreVoucher`, repositório `StoreVoucherRepository`, serviços de devolução com reposição de stock e emissão de vale em `POSService.java`, resgate com validação de saldo e prazos em `POSService.redeemVoucherForPayment`, endpoints `/api/pos/returns` e `/api/pos/vouchers/{code}`, e impressão térmica 80mm em `StoreVoucherPrintService` e `/api/print/pos-voucher/*`.
  5. **Desktop Swing (`desktop`):**
     - `PosReturnDialog.java` (267 linhas < 1000): opção primária "VALE DE COMPRAS (Store Credit)", seletor inteligente de faturas recentes, comprovativo modal com código alfanumérico, cópia para área de transferência, impressão de talão térmico 80mm e opção de troca imediata no checkout.
     - `PosPaymentDialog.java` (347 linhas < 1000): novo método de pagamento "Vale Compras" (`fas-ticket-alt`), consulta e validação em tempo real com exibição do titular e saldo, feedback claro e sem fechar a janela em caso de saldo insuficiente ou vale expirado.
     - `POSApiClient.java`: métodos de devolução, consulta e impressão de vales de compras.
- **Harness & Validação:**
  - `PosVoucherHarnessTest`: **10/10 aprovados** (emissão de vale, multi-itens, isolamento multi-empresa, resgate total e parcial, validação de saldo insuficiente e expiração).
  - `PosVoucherUiHarnessTest`: **5/5 aprovados** (validação de UI Swing, botões e campos).
  - `MultiModuleArchitectureHarnessTest`: **6/6 aprovados** (estrita aderência de fronteiras físicas Maven).
  - `POSServiceTest`: **26/26 aprovados**.
  - `PosContingencyHarnessTest`: **3/3 aprovados**.
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `TableCardContainmentAuditTest` & `UiPanelDecompositionTest`: **100% aprovados**.
  - Todos os arquivos Swing respeitam o limite de 1000 linhas (`POSPanel`: 997, `PosPaymentDialog`: 347, `PosReturnDialog`: 267).

### Fecho Cego de Caixa no POS (Blind Drop) com Contagem de Notas/Moedas e Apuramento de Quebras/Sobras — 2026-09-27 — **concluída com SPEC e HARNESS**
- **Necessidade do Negócio:** Implementação de fecho cego estrito onde o operador conta a gaveta sem visualizar o saldo esperado pelo sistema, com grelha de discriminação por notas e moedas oficiais de Meticais (MZN: 1000, 500, 200, 100, 50, 20 MT e moedas 10, 5, 2, 1, 0.50 MT), cálculo em tempo real, justificação de quebras/sobras e impressão do Relatório Z com tabela de contagem física.
- **Solução Implementada de Ponta a Ponta:**
  1. **Especificação & Testes:** `docs/FECHO_CAIXA_CEGO_DENOMINACOES_SPEC.md` e `docs/FECHO_CAIXA_CEGO_DENOMINACOES_HARNESS.md`.
  2. **Base de Dados (`V73__pos_blind_close_denominations_and_notes.sql`):** Adicionadas colunas `closing_notes VARCHAR(500)` e `cash_breakdown_json TEXT` à tabela `till_sessions`.
  3. **Contratos (`contracts`):** `CashDenominationDTO`, `CloseSessionRequest` (com notas e breakdown JSON preservando retrocompatibilidade) e `PosZReportDTO` enriquecido.
  4. **Backend (`TillSession.java`, `POSService.java`, `POSController.java`, `POSZReportPrintService.java`):** Persistência de notas e JSON de denominações, validação de regras de divergência, enriquecimento do log de auditoria `POS_CLOSE_SESSION` e renderização no PDF do Relatório Z da tabela de contagem física e observações.
  5. **Desktop Swing (`PosBlindCloseDialog.java`, `POSApiClient.java`):** Interface modal ergonómica com grelha de notas e moedas, botões rápidos `+1`/`+5`, total consolidado dinâmico, alternador para introdução rápida direta, campo de justificação de quebra/sobra, painel de reconciliação pós-fecho e impressão direta do Relatório Z (A4).
- **Harness & Validação:**
  - `MultiModuleArchitectureHarnessTest`: **6/6 aprovados**.
  - `POSServiceTest`: **26/26 aprovados**.
  - `PosZReportHarnessTest`: **4/4 aprovados**.
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `PosBlindCloseDialogHarnessTest`: **2/2 aprovados**.
  - `PosZReportUiHarnessTest`: **2/2 aprovados**.
  - `TableCardContainmentAuditTest` & `UiPanelDecompositionTest`: **100% aprovados**.
  - Backend UP na porta 8080 e Desktop ativo na sessão interactiva.

### Selecção Pesquisável de Produtos nos Formulários — 2026-09-27 — **concluída com SPEC e HARNESS**
- Criados `SearchableComboBox<T>` e `ProductSearchComboBox`, com pesquisa incremental sem distinção
  de acentos/maiúsculas e correspondência por vários termos.
- A pesquisa de produtos considera nome, SKU, referência, código de barras, descrição e categoria;
  o resultado apresenta código, nome e preço.
- Aplicado em factura, pedido de cliente, compra, encomenda a fornecedor, cotação, promoção,
  transferência, ajuste de stock, entrada de lote, edição de produto e registo de quebra.
- Os formulários recebem o `ProductDTO` seleccionado em vez de inferir o produto pelo índice ou
  apenas pelo nome, preservando pré-selecção e evitando conflito entre produtos homónimos.
- SPEC/HARNESS: `docs/PRODUCT_SEARCH_SELECTION_SPEC.md` e
  `docs/PRODUCT_SEARCH_SELECTION_HARNESS.md`.
- Harness alinhado integralmente à SPEC: pesquisa por cada identificador, normalização de acentos,
  múltiplos termos, selecção tipada, catálogo vazio/sem resultado, rótulo e altura de 38 px.
- Validação focada: `ProductSearchComboBoxHarnessTest`, `DesktopThinContextTest`,
  `StockCommercialUiHarnessTest`, `StockInteractionHarnessTest`, `StockWastePanelHarnessTest` e
  `UiPanelDecompositionTest` aprovados.

### Auditoria e Correção Global de Contenção de Filtros e Tabelas em Todo o Sistema — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** O utilizador solicitou verificar todas as tabelas do sistema para identificar e corrigir quaisquer outras telas onde campos de pesquisa e seletores estivessem fora do card que contém a tabela.
- **Auditoria Exhaustiva (66 Ficheiros de Interface com Tabela):**
  1. Varredura completa de todas as classes Swing com `JTable`, inspecionando o posicionamento de `TableFilter.bar`, `TableFilter.searchField`, `SearchField`, `JComboBox` e painéis de controlo.
  2. **Casos Identificados e Corrigidos:**
     - **Clientes ([ClientesPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/ClientesPanel.java)):** A barra de pesquisa de clientes estava inserida num painel intermediário transparente `center` (`center.add(searchRow, BorderLayout.NORTH)`), fora do `ModernPanel card`. Foi movida diretamente para o topo interno do card (`card.add(searchRow, BorderLayout.NORTH)`).
     - **Fiscal Salarial ([FiscalPanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/FiscalPanel.java)):** A aba *IRPS & INSS Salarial* continha os seletores de período, botões de atualização/impressão e a tabela adicionados soltos diretamente na aba sem `ModernPanel`. Foram encapsulados num `ModernPanel(16)` card com `card.add(controls, BorderLayout.NORTH)` e `card.add(scroll, BorderLayout.CENTER)`.
  3. **Módulos Verificados e Confirmados como Conformes:**
     - Comercial: `CommercialInvoicesView` (`listCard`), `CommercialOrdersView` (`listCard`), `QuotationsPanel` (`card`), `ReceiptsPanel` (`card`), `DeliveryGuidesPanel` (`card`), `CommercialNotesPanel` (`card`), `CommercialMovementsPanel` (`card`), `OutstandingAccountsPanel` (`card`).
     - Compras: `ComprasPanel` (`historyCard`), `PurchaseOrdersPanel` (`listCard`), `PurchaseSuppliersPanel` (`listCard`), `PurchasePayablesPanel` (`card`), `PurchaseReorderPanel` (`card`).
     - Recursos Humanos: `HRPanel` (`card`), `HRContractsPanel` (`card`), `HRDeductionsPanel` (`card`), `HRExpensesPanel` (`card`), `HRLiabilitiesPanel` (`card`), `HRTerminationsPanel` (`card`), `HRTimeSheetPanel` (`card`), `HRVacationsPanel` (`card`).
     - Financeiro e Tesouraria: `FinanceiroPanel` (`accountsCard`, `movementsCard`), `BankReconciliationPanel` (`card`), `CreditRiskPanel` (`card`).
     - Auditoria e Segurança: `ForensicAuditPanel` (`card`), `UserManagementPanel` (`card`), `ApprovalsPanel` (`pendingCard`, `historyCard`).
     - Performance e Plataforma: `GoalsTab` (`card`), `RankingTab` (`card`), `BonusTab` (`card`), `PlataformaPanel` (5 tabelas em `card`/`listCard`), `NotificationsPanel` (`card`), `PromotionsPanel` (`card`).
- **Harness & Validação:**
  - `DesktopThinContextTest`: **2/2 aprovados**.
  - `TableCardContainmentAuditTest`: **2/2 aprovados**.
  - Desktop recompilado e empacotado (`BUILD SUCCESS`). Backend UP e Desktop reiniciado.

### Contenção Estrita dos Filtros e Pesquisa no Card da Tabela (Lotes & Validades e Stock) — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** Em conformidade com a convenção canónica de UI Swing do ERP Multicore, todos os campos de pesquisa e caixas de seleção/filtro devem estar estritamente contidos dentro do card `ModernPanel(16)` que abriga a tabela (`BorderLayout.NORTH` para filtros e `BorderLayout.CENTER` para a tabela), sendo proibido manter filtros soltos fora do card. No separador de *Lotes & Validades* e restantes tabelas do Stock, os seletores de armazém, validade e campo de pesquisa encontravam-se soltos no topo fora do card da tabela.
- **Solução Implementada de Ponta a Ponta:**
  1. **Lotes & Validades (`StockBatchesPanel.java`):**
     - O cabeçalho (título e botões de ação) mantém-se no topo (`tab.add(header, BorderLayout.NORTH)`).
     - Os campos de seleção (`batchWarehouseCombo`, `batchExpirationCombo`), campo de pesquisa (`batchSearchField`) e o resumo dinâmico (`batchesSummary`) foram unificados no painel de cabeçalho do card e inseridos em `card.add(filterCardHeader, BorderLayout.NORTH)`.
     - A tabela e a barra de paginação continuam em `BorderLayout.CENTER` e `BorderLayout.SOUTH` do mesmo `card`, garantindo layout coeso e delimitado.
  2. **Artigos em Stock (`StockPanel.java`):**
     - O bloco de filtros de armazém, estado, categoria e pesquisa foi movido para o interior do `ModernPanel(16) card` (`card.add(filters, BorderLayout.NORTH)`).
  3. **Categorias de Stock (`StockCategoriesPanel.java`):**
     - O campo de pesquisa por código e nome foi movido para dentro do `card` (`card.add(searchRow, BorderLayout.NORTH)`).
  4. **Alertas de Stock (`StockAlertsPanel.java`):**
     - As sub-abas *Esgotados* e *Validade (expirados / a expirar)* agora inserem as suas barras de pesquisa e filtros diretamente em `outCard.add(outBar, BorderLayout.NORTH)` e `expCard.add(expBar, BorderLayout.NORTH)`.
- **Harness & Validação:**
  - `DesktopThinContextTest`: **2/2 testes verdes** (inicialização do `MainFrame` 100% íntegra).
  - `TableCardContainmentAuditTest`: **2/2 testes verdes** (auditoria de contenção de tabelas e contagem de botões aprovada).
  - `PhysicalInventoryPanelHarnessTest`: **4/4 testes verdes**.
  - Recompilação e empacotamento com `mvn package -DskipTests -pl desktop` com **BUILD SUCCESS**.
  - Backend UP e Desktop reiniciado com sucesso na sessão interativa.

### Bloqueio e Validação Obrigatória de Motorista e Matrícula nas Guias — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** O sistema não pode aceitar a emissão ou criação de Guias (Guias de Transferência entre armazéns e Guias de Remessa) sem que os campos de **Motorista** e **Matrícula do Veículo** estejam devidamente preenchidos.
- **Solução Implementada de Ponta a Ponta:**
  1. **Contratos (`CreateStockTransferRequest`, `ConvertOrderToTransferRequest`):**
     - Mapeamento retrocompatível garantindo que pedidos de transferência e conversões de encomenda para transferência recebam e validem `driverName` e `vehiclePlate`.
  2. **Backend (`StockTransferService.java`, `DeliveryGuideService.java`, `InternalReplenishmentService.java`):**
     - Em `StockTransferService.create(...)`: Validação preventiva no topo do método. Se `driverName` ou `vehiclePlate` estiverem vazios/nulos, lança imediatamente `BusinessRuleException("O nome do motorista é obrigatório para emitir a guia de transferência.")` ou `BusinessRuleException("A matrícula do veículo é obrigatória para emitir a guia de transferência.")`.
     - Em `DeliveryGuideService.createFromOrder(...)`: Validação de `responsible` (transportador/responsável) e `vehicle` (matrícula/viatura). Se vazios, lança `BusinessRuleException("O transportador / responsável é obrigatório para emitir a guia de remessa.")` e `BusinessRuleException("A viatura / matrícula é obrigatória para emitir a guia de remessa.")`.
     - Em `InternalReplenishmentService.java`: Suporte a fallback seguro de `driverName` e `vehiclePlate` a partir de `responsible` e `vehicle`.
  3. **Desktop Swing com Prevenção de Perda de Dados (`StockTransferActions.java`, `OrderToTransferAction.java`, `InternalReplenishmentActions.java`, `ComercialPanel.java`):**
     - Todos os formulários foram atualizados com rótulos `*` e placeholders claros indicando obrigatoriedade: `"Motorista *:"` e `"Matrícula do Veículo *:"`.
     - Implementado hook em `ModernFormDialog.setOnSave` / `setOnSaveAsync`: a validação ocorre antes de fechar a janela. Caso falhe, foca automaticamente o campo em falta e apresenta a mensagem de erro no `feedbackPanel`, **sem fechar o diálogo e sem perder os produtos, quantidades e lotes já introduzidos**.
- **Harness & Validação:**
  - `StockTransferServiceTest`: **14/14 testes verdes** (adicionados `create_semMotorista_lancaExcecao` e `create_semMatricula_lancaExcecao`).
  - `DeliveryGuideServiceTest`: **9/9 testes verdes**.
  - `InternalReplenishmentServiceTest`: **14/14 testes verdes**.
  - `StockTransferPrintServiceTest`: **3/3 testes verdes**.
  - `MultiModuleArchitectureHarnessTest`: **6/6 testes verdes**.
  - `DesktopThinContextTest`: **2/2 testes verdes**.
  - `TableCardContainmentAuditTest`: **2/2 testes verdes**.

### Colunas Completas da Tabela da Guia (Referência, Código de Barras, Produto, Qtd, Embalagem, Caixa, % da Caixa, Valor Unitário, IVA) — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** A tabela de linhas da Guia (especificamente a Guia de Transferência e documentos logísticos) deve conter exatamente as 9 colunas canónicas de logística e valor:
  1. `Referência` (`reference`)
  2. `Código de Barras` (`barcode`)
  3. `Produto` (`name` + lote quando aplicável)
  4. `Quantidade` (`quantity` em unidades)
  5. `Embalagem` (embalagens equivalentes: `quantidade ÷ unitsPerPackage`)
  6. `Caixa` (caixas equivalentes: `quantidade ÷ (packagesPerBox × unitsPerPackage)`)
  7. `% da Caixa` (percentagem da caixa consoante as embalagens do produto: `(quantidade ÷ unitsPerBox) × 100`)
  8. `Valor Unitário` (`effectiveUnitPrice` do produto)
  9. `IVA` (taxa de imposto do produto: `effectiveTaxRate`, ex: `16%`)
- **Solução Implementada de Ponta a Ponta:**
  1. **Backend (`StockTransferPrintService.java`, `StockTransferService.java`):**
     - Substituída a tabela simplificada de 4 colunas (`Código`, `Descrição`, `Lote`, `Qtd`) pela tabela profissional completa de 9 colunas com alinhamentos e larguras otimizadas:
       `Referência (12% L) · Cód. Barras (13% L) · Produto (21% L) · Qtd (6% R) · Embalagem (11% R) · Caixa (7% R) · % da Caixa (10% R) · Valor Unit. (13% R) · IVA (7% R)`.
     - Implementados os cálculos de embalagem e caixas (`formatPackages`, `formatBoxes`, `formatBoxPercentage`) com `stripTrailingZeros()`.
     - Atualizado o bloco de totais (`buildTotalsLine`) para incluir: `Linhas`, `Qtd. Total`, `Total Mercadoria (Líquido)`, `Total IVA` e `Total Geral`.
     - `StockTransferService.toDTO` mapeia `reference`, `barcode`, fatores de embalagem (`packagesPerBox`, `unitsPerPackage`), `effectiveUnitPrice` e `effectiveTaxRate`.
  2. **Renderizador Partilhado (`LineItemsTableRenderer.java`):**
     - Adicionado o método canónico `formatBoxes(Row row)` para caixas equivalentes, mantendo coerência estrita com `PackagingComposition` e `PackagingQuantity`.
  3. **Desktop Swing (`StockTransferActions.java`, `StockPanel.java`, `DeliveryGuidesPanel.java`):**
     - Formulário interativo de criação de transferências exibe dinamicamente as colunas calculadas: `Embalagem`, `Caixa`, `% Caixa`, além de `Lote (FEFO)` e `Validade (FEFO)`.
     - Modal de consulta completa (`displayTransferLinesModal` em `StockTransferActions.java`): exibe resumo de transporte (origem, destino, motorista, matrícula, data, estado), a tabela completa com as 9 colunas alinhadas, resumo de totais (`Linhas`, `Qtd Total`, `Total Líquido`, `Total IVA`, `Total Geral`) e botão direto de "Imprimir Guia".
     - Integrada ação `"Ver Linhas"` no menu de ações `transferMenu` (4 opções $\le 5$) e ouvinte de duplo clique / tecla Enter na tabela `transferTable` em `StockPanel.java`.
     - `DeliveryGuidesPanel.java` enriquecido com duplo clique e `ModernFormDialog` detalhado com colunas de caixas, unidades, valores monetários e impressão.
- **Harness & Validação:**
  - `StockTransferPrintServiceTest`: **3/3 testes verdes** (incluindo `render_imprimeTabelaComColunasObrigatorias`).
  - `StockTransferServiceTest`: **12/12 testes verdes**.
  - `LineItemsTableRendererTest`: **5/5 testes verdes**.
  - `MultiModuleArchitectureHarnessTest`: **6/6 testes verdes**.
  - `DesktopThinContextTest`: **2/2 testes verdes**.
  - `TableCardContainmentAuditTest`: **2/2 testes verdes**.
  - Recompilado e empacotado via `mvn package -DskipTests`, backend UP na porta 8080 e Desktop lançado.

### Campos de Motorista e Matrícula na Guia de Transferência Entre Armazéns — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** Ao emitir uma Guia de Transferência de Stock entre armazéns, o sistema deve fornecer campos específicos e visíveis para inserção do **Motorista** e da **Matrícula** da viatura de transporte.
- **Solução Implementada de Ponta a Ponta:**
  1. **Base de Dados (`V71__stock_transfer_driver_and_plate.sql`):** Adicionadas as colunas `driver_name VARCHAR(120)` e `vehicle_plate VARCHAR(30)` à tabela `stock_transfers`.
  2. **Contratos (`contracts`):**
     - [CreateStockTransferRequest](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/inventory/dto/CreateStockTransferRequest.java): Acrescentados os campos `driverName` e `vehiclePlate`, preservando construtor retrocompatível.
     - [StockTransferDTO](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/inventory/dto/StockTransferDTO.java): Expostos `driverName` e `vehiclePlate` com construtores retrocompatíveis.
     - [ConvertOrderToTransferRequest](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/ConvertOrderToTransferRequest.java): Atualizado para suportar motorista e matrícula na conversão de encomendas de reposição.
  3. **Backend (`StockTransfer.java`, `StockTransferService.java`, `StockTransferPrintService.java`):**
     - Entidade `StockTransfer` mapeia `driverName` e `vehiclePlate`.
     - `StockTransferService` persiste os campos e mapeia para DTO.
     - `StockTransferPrintService` renderiza no PDF da Guia de Transferência o nome do Motorista e a Matrícula do Veículo (ou linha para preenchimento se deixado em branco).
  4. **Desktop (`StockTransferActions.java`, `StockPanel.java`, `OrderToTransferAction.java`, `InternalReplenishmentActions.java`):**
     - O formulário de "Nova Transferência" (`createTransferDialog`) agora inclui os campos *Motorista:* e *Matrícula do Veículo:* com placeholders explicativos.
     - A tabela de transferências em `StockPanel` foi expandida com as colunas *Motorista* e *Matrícula*, com filtro/pesquisa adaptado.
     - `StockPanel.java` permaneceu em 952 linhas (estritamente $< 1000$ linhas).
- **Harness & Validação:**
  - `StockTransferServiceTest` (incluindo `create_comMotoristaEMatricula_persisteCampos`): **12/12 testes verdes**.
  - `StockTransferPrintServiceTest`: **2/2 testes verdes** (validação de extração de texto PDF com motorista e matrícula).
  - `InternalReplenishmentServiceTest`: **14/14 testes verdes**.
  - `DesktopThinContextTest`, `FinalUiUniformityHarnessTest`, `UiPanelDecompositionTest`: **8/8 testes verdes**.
  - `MultiModuleArchitectureHarnessTest`: **6/6 testes verdes**.
  - Aplicação recompilada, empacotada e reiniciada com sucesso.

### Obrigatoriedade de Nome do Cliente nas Vendas POS (Mesmo Não Cadastrado) — 2026-09-26 — **concluída com SUCESSO**
- **Necessidade do Negócio:** O sistema não pode aceitar a emissão ou conclusão de vendas no POS sem o nome do cliente/comprador, mesmo quando o cliente for avulso / não cadastrado na base de dados.
- **Solução Implementada:**
  1. **Contratos (`POSCheckoutRequest`):** Documentada e tipada a obrigatoriedade de `walkInName` quando `clientId == null`.
  2. **Backend (`POSService.java`):**
     - Se `request.clientId() == null` e `request.walkInName()` for nulo ou em branco, lança `BusinessRuleException("É obrigatório indicar o nome do cliente para efetuar a venda no POS.")`.
     - Fatura/recibo grava e valida `customerName` estritamente não vazio.
  3. **Desktop (`POSPanel.java`):**
     - O campo de pesquisa do cabeçalho foi identificado com `Nome do cliente *` e tooltip instrutivo.
     - No checkout (`runCheckout`), se o operador não tiver selecionado um cliente cadastrado e o campo de texto estiver vazio, o sistema exibe imediatamente um diálogo modal amigável (`UIHelper.promptRequiredText`) solicitando o nome do cliente. Se o operador cancelar ou deixar em branco, a venda é bloqueada com aviso de validação.
     - Mantida a decomposição estrita do `POSPanel.java` com 996 linhas (< 1000 linhas).
- **Harness & Validação:**
  - `PosCustomerRequirementHarnessTest`: **2/2 testes aprovados**.
  - `POSServiceTest`: **24/24 testes aprovados** (incluindo `checkout_semNomeClienteNaoCadastrado_lancaExcecaoDeRegraDeNegocio`).
  - Suíte completa de POS Desktop: **19/19 testes aprovados** (`PosProfessionalErgonomicsHarnessTest`, `PosErgonomicsHarnessTest`, `PosContingencyManagerTest`, etc.).
  - `DesktopThinContextTest`, `TableCardContainmentAuditTest`, `UiPanelDecompositionTest`: **100% aprovados**.
  - Backend e Desktop recompilados, empacotados e reiniciados na sessão interativa.

### Correção do Limite de 5 Ações no `ActionMenuButton` e Resolução de Erro de Login no `MainFrame` — 2026-09-25 — **concluída com SUCESSO**
- **Sintoma / Bloqueio:** Ao submeter login no diálogo de autenticação do Desktop (ex.: utilizador `ana` ou `admin`), a interface exibia `Failed to instantiate [mz.multicore.erp.gui.MainFrame]: Constructor threw exc`.
- **Causa Raiz Identificada:** A classe [ActionMenuButton](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/ActionMenuButton.java) possui uma restrição de integridade rígida (`MAX_ACTIONS = 5`) testada em [ActionMenuButtonTest](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/components/ActionMenuButtonTest.java). Na refatoração anterior do [HRPanel](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java):
  1. Na aba de Colaboradores, `moreBtn` continha 6 ações (> 5).
  2. Na aba de Recibos de Salário, `actionsBtn` continha 8 ações (> 5).
  Ao inicializar o [MainFrame](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/MainFrame.java), a invocação do construtor de [HRPanel](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java) lançava `IllegalStateException: O menu de acções não pode ter mais de cinco opções.`.
- **Solução Implementada:**
  1. Em [HRPanel](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java) (Colaboradores): `moreBtn` ajustado para exatamente 5 ações (*Editar Colaborador*, *Evolução Salarial*, *Documentos*, *Saúde Ocupacional*, *Alterar Estado*), com barra de ações no cabeçalho contendo 3 botões (`moreBtn`, `profileBtn`, `newBtn`).
  2. Em [HRPanel](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java) (Recibos de Salário):
     - Criado `documentsBtn` com 3 ações (*Imprimir PDF*, *Exportar Lista*, *Ficheiro de Pagamento*).
     - Criado `actionsBtn` com 5 ações (*Aprovar Recibo*, *Marcar Pago*, *13.º Mês*, *Fechar Mês*, *Reabrir Mês*).
     - Mantido `newBtn` ("Gerar Recibo") no cabeçalho (3 botões: `documentsBtn`, `actionsBtn`, `newBtn`).
     - Botão `processBtn` ("Processar Mês") integrado elegantemente na barra superior do cartão de tabela junto aos filtros de pesquisa/estado/período.
  3. Atualizado o teste [HrCrmUiErgonomicsHarnessTest](file:///c:/Users/miran/Desktop/manager/desktop/src/test/java/mz/multicore/erp/gui/HrCrmUiErgonomicsHarnessTest.java).
- **Harness & Validação:**
  - `DesktopThinContextTest`: **100% verde** (MainFrame instancia perfeitamente para utilizador comum e superadmin sem qualquer exceção).
  - `ActionMenuButtonTest`, `HrCrmUiErgonomicsHarnessTest`, `UiPanelDecompositionTest`: **11/11 testes verdes**.
  - `TableCardContainmentAuditTest`, `StockCommercialUiHarnessTest`, `FinalUiUniformityHarnessTest`: **12/12 testes verdes**.
  - Desktop recompilado, empacotado e aplicação reiniciada na sessão interactiva (PID `25308`).

- **Necessidade:** O utilizador solicitou refatoração formal com especificação e testes harness para eliminar qualquer sobrecarga de botões nos módulos de Recursos Humanos, CRM e Notificações, e confirmação do padrão internacional (GS1 / ISO / SAP) na gestão de embalagens multinível.
- **Solução Implementada:**
  1. **`CRMPanel.java` (Folhas de Obra & Assistência Técnica):**
     - Cabeçalho reduzido de 6 para 3 botões: `[Ações da Folha ▾]` (agrupando *Imprimir PDF*, *Corrigir*, *Anular*, *Tarifa/hora*), `[Faturar Folha de Obra]` e `[Nova Folha de Obra]`.
  2. **`HRPanel.java` (Recursos Humanos Central):**
     - Aba Colaboradores reduzida de 4 para 3 botões: `[Mais acções ▾]` (incluindo *Editar Colaborador*), `[Ver Perfil]` e `[Novo Colaborador]`.
     - Aba Recibos de Salário reduzida de 6 para 3 botões: `[Gestão & Documentos ▾]` (agrupando aprovações, pagamentos, exportação e fecho), `[Processar Mês]` e `[Gerar Recibo]`.
     - Aba Registo de Faltas reduzida de 4 para 3 botões: `[Exportar PDF]`, `[Ações da Falta ▾]` (agrupando *Justificar* e *Eliminar*) e `[Registar Falta]`.
  3. **Submódulos de RH (`HRVacationsPanel`, `HRTimeSheetPanel`, `HRDeductionsPanel`, `HRLiabilitiesPanel`, `HRTerminationsPanel`):**
     - Todos os cabeçalhos foram estruturados com máximo de 2 a 3 botões, agrupando ações secundárias em `ActionMenuButton`.
  4. **`NotificationsPanel.java` (Central de Notificações):**
     - Rodapé reorganizado em layout bilateral: `[Actualizar]` à esquerda no `BorderLayout.WEST`, e apenas 3 botões à direita (`[Marcar como lida]`, `[Marcar todas como lidas]`, `[Abrir módulo]`).
  5. **Verificação de Embalagens (Padrão Internacional):**
     - Validada a cadeia logística multinível (`Caixa → Embalagem → Unidade`) via `PackagingComposition`, `PackagingQuantity`, `PackageQuantityEditor` e `LineItemsTableRenderer`.
     - Fórmulas de conversão e integridade fiscal 100% validadas contra `PackageQuantityHarnessTest`.
- **Harness & Validação:**
  - Criado o teste automatizado `HrCrmUiErgonomicsHarnessTest.java` (4/4 testes verdes).
  - `TableCardContainmentAuditTest`: **0 barras sobrecarregadas em TODO o sistema ERP** (100% de conformidade) e **0 tabelas sem ModernPanel**.
  - `UiPanelDecompositionTest`: todos os ficheiros cumprem a restrição de < 1000 linhas.
  - Desktop recompilado, empacotado e reiniciado na sessão interactiva (PID `26208`).

### Desobstrução e Agrupamento de Ações no Módulo de Stock & Inventário Físico (`AUDIT-02`) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** O utilizador solicitou refatoração formal com especificação e testes harness para eliminar a sobrecarga de botões abertos no módulo de Stock & Armazéns (`StockPanel`) e Contagem Física de Inventário (`PhysicalInventoryPanel`), onde barras continham 5 botões horizontais comprimindo os títulos.
- **Solução Implementada:**
  1. **`PhysicalInventoryPanel.java` (Inventário Físico & Reconciliação):**
     - Cabeçalho reduzido de 5 para exatamente 3 botões:
       - `[Dossiê PDF]` (Secundário de impressão/visualização).
       - `[Ciclo da Sessão ▾]` (`ActionMenuButton` contendo *Iniciar Contagem*, *Fecho & Acerto*, *Cancelar Sessão*).
       - `[Nova Sessão]` (Primário de criação).
     - Estados dos itens de menu do ciclo de vida sincronizados dinamicamente via `setActionEnabled(...)` conforme a sessão esteja em Rascunho, Em Progresso ou Fechada.
  2. **`StockPanel.java` (Controle de Stock & Armazéns):**
     - **Barra Superior Global:** Reduzida de 5 para 3 botões: `[Actualizar]`, `[Mais acções ▾]` e `[Registar Produto]`.
     - Ações de *Inventário Físico* e *Trancar Stock* consolidadas dentro do `ActionMenuButton("Mais acções")`, com atualização dinâmica de estado para administradores.
     - **Aba de Transferências entre Armazéns:** Cabeçalho reduzido de 5 para 3 botões: `[Imprimir Guia]`, `[Ações da Transferência ▾]` (agrupando *Aprovar*, *Rejeitar*, *Registar Encomenda*) e `[Nova Transferência]`.
- **Harness & Validação:**
  - Criado teste `testTopBarActionCountAndMenuContainment` em `PhysicalInventoryPanelHarnessTest.java` (4/4 testes verdes).
  - Criado teste `stockToolbarsAdhereToMaxThreeButtons` em `StockCommercialUiHarnessTest.java` (5/5 testes verdes).
  - `TableCardContainmentAuditTest` e `UiPanelDecompositionTest` 100% verdes.
  - Desktop recompilado, empacotado e aplicação reiniciada na sessão interactiva (PID `24052`).

### Desobstrução e Agrupamento de Ações nos Módulos Comercial e Compras (`AUDIT-02`) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** Conforme a regra de ergonomia do `AGENTS.md`, cabeçalhos e barras de ação devem evitar filas longas de botões (máximo 2 a 3 botões visíveis, agrupando ações secundárias em `ActionMenuButton`). A auditoria `AUDIT-02` identificou sobreposição potencial em Encomendas, Faturas, Ordens de Compra e Fornecedores.
- **Solução Implementada:**
  1. **`CommercialOrdersView.java` (Central de Pedidos e Separação):**
     - Cabeçalho: 2 botões (`[Actualizar]`, `[Novo Pedido de Cliente]`).
     - Rodapé do Card: Desacoplado em layout bilateral; à esquerda `[Mais acções ▾]` (detalhes, impressão, histórico, exportação), à direita barra com exatamente 3 botões: `[Cancelar Encomenda]`, `[Converter ▾]` (agrupando *Converter em Guia* e *Converter em Transferência*) e `[Faturar Encomenda]`.
  2. **`CommercialInvoicesView.java` (Faturação Recente):**
     - Cabeçalho: `[Actualizar]`, `[Faturar a partir de Encomenda]`, `[Nova Fatura]` (3 botões).
     - Rodapé do Card: Bilateral; à esquerda `[Mais acções ▾]`, à direita apenas 2 botões de ciclo de vida: `[Anular Fatura]` e `[Liquidar (RC)]`.
  3. **`PurchaseOrdersPanel.java` (Ordens de Compra):**
     - Cabeçalho: Reduzido de 5 para 3 botões: `[Actualizar]`, `[Receção & Gestão ▾]` (agrupando *Receber Total*, *Receber Parcial*, *Cancelar Encomenda*) e `[Nova Encomenda]`.
  4. **`PurchaseSuppliersPanel.java` (Fornecedores Cadastrados):**
     - Cabeçalho: Reduzido de 5 para 3 botões: `[Actualizar]`, `[Ações ▾]` (agrupando *Ver Ficha*, *Editar*, *Activar/Desactivar*) e `[Novo Fornecedor]`.
  5. **`ActionMenuButton.java`:**
     - Adicionado método público `setActionEnabled(int index, boolean enabled)` e expostos `actionAt` e `actionCount` para permitir controle de estado por linha nas opções dos menus suspensos.
- **Validação:**
  - `StockCommercialUiHarnessTest` (4/4 testes verdes).
  - `CommercialMultiuserRefreshHarnessTest` (1/1 teste verde).
  - `TableCardContainmentAuditTest` e `UiPanelDecompositionTest` 100% verdes.
  - Desktop recompilado, empacotado e reiniciado (PID `23976`).

### Auditoria e Conformidade Global do Padrão Table-Card Containment (52 Painéis / 100% de Conformidade) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** O utilizador solicitou auditoria global e garantia de que todas as tabelas e filtros do ERP Multicore cumprissem estritamente o padrão canónico: tabelas e respectivos filtros de pesquisa/estado contidos num `ModernPanel(16)` card (`BorderLayout.NORTH` para filtros e `CENTER` para tabela, sem filtros soltos).
- **Auditoria Automatizada:**
  - Criado o teste de auditoria global `TableCardContainmentAuditTest.java` para escanear recursivamente todos os painéis e abas do módulo Desktop.
  - Identificados 5 painéis pendentes: `UserManagementPanel.java`, `PosSalesHistoryPanel.java`, `GoalsTab.java`, `RankingTab.java` e `BonusTab.java`.
- **Solução Implementada:**
  1. `UserManagementPanel.java`: Tabela e toolbar encapsuladas em `ModernPanel(16)` com `EmptyBorder(15, 15, 15, 15)` e acções secundárias agrupadas em `ActionMenuButton("Operações")`.
  2. `PosSalesHistoryPanel.java`: Tabela do histórico de vendas, barra de filtros/pesquisa e paginação integradas num card `ModernPanel(16)`.
  3. `GoalsTab.java`: Tabela de metas e selects de estado e período encapsulados num card `ModernPanel(16)` com botões de acção organizados.
  4. `RankingTab.java`: Tabela de ranking e select de período contidos num card `ModernPanel(16)`.
  5. `BonusTab.java`: Tabela de bónus encapsulada em `ModernPanel(16)` com acções secundárias de linha agrupadas em `ActionMenuButton("Mais Ações")`.
- **Validação:**
  - `TableCardContainmentAuditTest`: 2/2 testes aprovados, **0 painéis pendentes** (100% de conformidade em 52 painéis).
  - `UiPanelDecompositionTest`: todos os ficheiros mantêm-se abaixo do limite de 1000 linhas.
  - JAR desktop compilado, empacotado e aplicação reiniciada na sessão interactiva do utilizador.

### Padrão Canónico de Encapsulamento em Card e Desobstrução de Acções em Contratos (`HRContractsPanel`) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** O utilizador reportou que na aba de Contratos de RH (`HRContractsPanel`) os filtros e o campo de pesquisa ainda estavam soltos fora de um card (`body` era um `JPanel` simples transparente), e no cabeçalho havia sobreposição entre o título e os 5 botões de acção horizontais (`Imprimir PDF`, `Activar`, `Renovar`, `Cessar`, `Novo Contrato`).
- **Solução Implementada:**
  1. **Encapsulamento em Card (`ModernPanel(16)`):**
     - O corpo da tabela e a barra de pesquisa/filtros foram envolvidos em um `ModernPanel(16)` com `EmptyBorder(15, 15, 15, 15)`.
     - `TableFilter.bar` (`searchField` + `Estado: [combo]`) posicionado no `BorderLayout.NORTH` do `card`.
     - `JScrollPane(table)` estilizado com `UIHelper.styleScrollPane` posicionado no `BorderLayout.CENTER` do `card`.
  2. **Hierarquia e Desobstrução de Acções no Cabeçalho:**
     - Substituídos os 5 botões abertos por uma hierarquia ergonómica:
       - `[Novo Contrato]` (Botão Primário de criação global).
       - `[Imprimir PDF]` (Botão Secundário de impressão directa).
       - `[Gestão do Contrato ▾]` (`ActionMenuButton` contendo as acções de linha seleccionada: `Activar`, `Renovar`, `Cessar`).
     - A largura da barra de acções foi reduzida de ~650px para ~310px, eliminando completamente qualquer risco de sobreposição com o título em qualquer resolução.
  3. **Validação & Testes:**
     - Criado o teste automatizado `HRContractsPanelHarnessTest.java` (3/3 testes verdes).
     - Executada suite conjunta: `HRContractsPanelHarnessTest`, `StockWastePanelHarnessTest`, `PhysicalInventoryPanelHarnessTest` (13/13 aprovados).
     - Aplicação empacotada e reiniciada via `schtasks` (PID `9408`).

### Padrão Canónico de Encapsulamento em Card (*Table-Card Containment Pattern*) em Quebras de Stock (`StockWastePanel`) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** O utilizador reportou que no módulo de inventário os cards de métricas deviam ficar no topo, e no ecrã de Quebras & Desperdício a tabela de histórico estava com viewport espremido (~30px), os botões colidiam com os filtros e os campos de pesquisa/filtros estavam soltos/desalinhados. Solicitou especificação formal e que filtros e campos de pesquisa ficassem estritamente dentro do card (`ModernPanel`) que contém a tabela, conforme demonstrado no ecrã canónico de referência (*Recursos Humanos -> Contratos*).
- **Solução Implementada:**
  1. **Especificação Canónica:**
     - Atualizados [docs/GESTAO_QUEBRAS_STOCK_SPEC.md](file:///c:/Users/miran/Desktop/manager/docs/GESTAO_QUEBRAS_STOCK_SPEC.md) (§3.1) e [docs/UI_DESIGN_SYSTEM.md](file:///c:/Users/miran/Desktop/manager/docs/UI_DESIGN_SYSTEM.md) documentando a regra inegociável de contenção em card.
  2. **Refatoração de `StockWastePanel.java`:**
     - **Aba 1 (Registo & Validação de Quebras):**
       - Cabeçalho externo mantém acções globais (`[Actualizar]`, `[Registar Quebra]`).
       - Interior do card (`ModernPanel(16)`): toolbar em `BorderLayout.NORTH` com `BorderLayout(0, 8)` acomodando linha de topo (`wasteSearchField` à esquerda e botões de acção de linha `[Aprovar]`, `[Rejeitar]` à direita via `TableFilter.toolbar`) e linha de filtros semânticos (`Estado:`, `Motivo:`, `Período:` via `TableFilter.bar`) sem sobreposições.
       - Viewport da tabela fixado com `preferredScrollableViewportSize(new Dimension(800, 380))` e altura de linha em 42px.
     - **Aba 2 (Radar de Validades & Prevenção):**
       - Banner informativo reposicionado no topo da aba (`BorderLayout.NORTH`).
       - Dropdown de urgência e botão de ação rápida `[Registar Quebra deste Lote]` encapsulados na toolbar superior dentro do card da tabela (`ModernPanel(16)`), eliminando o helper deformador `filterGroup`.
       - Viewport amplo de 380px e linhas de 42px.
     - **Aba 3 (Métricas & Relatório Executivo):**
       - Filtros de período (`Data Início:`, `Data Fim:`, `[Filtrar Período]`) e botão `[Imprimir Relatório Oficial PDF]` encapsulados horizontalmente dentro de `ModernPanel(14)` no topo.
       - Ambas as tabelas (`reasonCard` e `catCard`) dentro de cards estilizados com 320px de viewport e 36px de altura de linha.
     - Linhas totais de `StockWastePanel.java` reduzidas para 884 linhas (limite $\le 1000$).
  3. **Validação:**
     - `StockWastePanelHarnessTest` e `PhysicalInventoryPanelHarnessTest` (10/10 testes verdes).
     - Aplicação empacotada e reiniciada via `schtasks` (PID `20432`).

### Unificação Global do Design do Sistema (Cards Coloridos, Altura Padrão de Tabelas, Barras de Filtro e Acções) — 2026-09-25 — **concluída com SUCESSO**
- **Necessidade:** O utilizador solicitou que os campos de dados/pesquisa e seleção nas tabelas tivessem a mesma organização em todo o sistema, os cards de KPI tivessem fundos coloridos vibrantes com gradiente como no Dashboard (altura uniforme de 86px) e as tabelas eliminassem alturas reduzidas arbitrárias.
- **Solução Implementada:**
  1. **Cards de KPI Coloridos e Uniformes (`KpiCard.java`):**
     - Adicionados métodos de resolução de gradientes de alto contraste: `resolveKpiGradient(accentColor)` e `resolveKpiSoftColor(accentColor)`.
     - Atualizados todos os cartões (`createCard`, `createMetricCard`, `createPillarCard`) para renderizarem com gradientes semânticos (azul, verde esmeralda, âmbar, vermelho, violeta, laranja) com valores em branco nítido e subtítulos/ícones de alto contraste.
     - Padronizados os grids em todos os painéis com `KpiCard.createGrid(colunas)` e altura canónica fixa de 86px.
  2. **Altura e Organização Canónica de Tabelas:**
     - Removidas alturas forçadas pequenas (`rowHeight=28`, `rowHeight=32`) em painéis como `PhysicalInventoryPanel`, `CreditRiskPanel`, `ForensicAuditPanel`, restaurando a altura ergonómica canónica de 35px gerida por `UIHelper.styleTable(table)`.
  3. **Barras de Filtros e Acções Canónicas (`TableFilter` & `UIHelper`):**
     - Adicionados helpers canónicos `UIHelper.actionsBar(JComponent...)` e `UIHelper.filterBar(...)` delegando para `TableFilter.toolbar(...)`.
     - Padronizadas mais de 25 barras de ferramentas em painéis de Compras, Comercial, Stock, RH, Financeiro, CRM e Fiscal.
     - Restaurados métodos e constantes em `UIHelper`: `ACCENT_CYAN`, `ACCENT_ORANGE`, `cycleTheme()`, `meetsWcagAaa()`, `blendColors()`, `attachQuickFilter()`, `wrapTableWithQuickFilter()`, `semanticColorFor()`, `semanticIcon()` e `requestLogout()`.
  4. **Resolução de Erro de Instanciação do MainFrame ("constructor failed" no login):**
     - O login autenticava na API com sucesso, mas ao abrir a janela principal `MainFrame` lançava `BeanCreationException / Constructor threw exception` devido a símbolos em falta (`UIHelper.ACCENT_PINK`, `UIHelper.ACCENT_SKY`), import em falta de `TableFilter` em `SupplierStatementPanel` e sobrecarga de 4 argumentos em `UIHelper.buildPremiumHeader`.
     - Todos os símbolos foram implementados e importados, todas as 237 classes foram recompiladas do zero com javac, e o teste integral `DesktopThinContextTest` passou a 100% (2/2 testes aprovados para utilizador normal e superadmin).
     - Aplicação Desktop reiniciada e ativa na sessão interactiva do Windows.

### Padronização e Reutilização Global de Cards de KPI com Altura Uniforme (`KpiCard`) — 2026-09-25 — **concluída com HARNESS**
- **Necessidade:** O utilizador solicitou que os cards de indicadores fossem reutilizáveis em todo o sistema como no painel principal (Dashboard) e com exatamente o mesmo tamanho/altura uniforme.
- **Solução Canónica Implementada:**
  - `KpiCard.java` promovido a componente canónico único do sistema de design:
    - Altura padrão uniforme rigorosa: `STANDARD_CARD_HEIGHT = 86px` e largura mínima `STANDARD_CARD_MIN_WIDTH = 140px` aplicadas a todos os métodos fábrica (`create`, `createCard`, `createMetricCard`).
    - Adicionado método fábrica universal `createCard(title, valueLabel, subtitle, iconCode, accentColor)` com variantes para `String` directa e integração de `TrendBadge`.
    - Adicionado gerador de grelha uniforme `createGrid(columns)` com espaçamento padronizado de 10px e transparência.
  - Refatorados 6 painéis que continham métodos privados ad-hoc (`buildKpiCard`, `createKpiCard`) com tamanhos desiguais para usarem o `KpiCard` canónico:
    1. `UserManagementPanel.java` (Utilizadores & PINs)
    2. `BankReconciliationPanel.java` (Conciliação Bancária)
    3. `CashFlowForecastPanel.java` (Previsão de Tesouraria)
    4. `PurchaseReorderPanel.java` (Reposição Inteligente de Compras)
    5. `ProfitAnalyticsWidget.java` (Rentabilidade & Margem)
    6. `StockProductDetailDialog.java` (Ficha Executiva do Artigo)
  - Criado o teste de regressão automatizado `KpiCardUniformityHarnessTest.java` (3/3 testes verdes).
- **Validação:** `KpiCardUniformityHarnessTest` (3/3), `UiPanelDecompositionTest` (todos os ficheiros $\le 1000$ linhas) e `MultiModuleArchitectureHarnessTest` (6/6) 100% verdes; aplicação empacotada e reiniciada no Windows (PID `17888`).

### Correcção de Abertura de Configurações no Sidebar (NullPointerException em `usersTableModel`) — 2026-09-25 — **corrigido e validado**
- **Causa Raiz:** Ao clicar em "Configurações" na barra lateral, o método `ConfigPanel.onPanelSelected()` invocava `loadUsersList()`, o qual tentava executar `usersTableModel.setRowCount(0)`. Contudo, a aba de utilizadores havia sido refatorada e desacoplada para o componente moderno `UserManagementPanel`, deixando o campo `usersTableModel` nulo no `ConfigPanel`. Isso lançava uma `NullPointerException` não tratada no Event Dispatch Thread do Swing (`AWT-EventQueue-0`), travando a interface gráfica.
- **Solução Implementada:**
  - `ConfigPanel.java`: Removidos os campos mortos `usersTableModel`, `usersTable` e métodos obsoletos de gestão de utilizadores (`loadUsersList`, `applyUsers`, `registerUser`, `updateSelectedUserRole`, `editSelectedUserName`).
  - Associada a instância viva de `UserManagementPanel` e delegado o recarregamento assíncrono seguro em `onPanelSelected()` via `usersPanel.refreshDataAsync()`.
  - `UserManagementPanel.java`: Eliminados emojis crus Unicode (`🟢`, `🔴`, `🔑`) nas colunas da tabela de utilizadores para evitar renderização de retângulos/quadrinhos (`▯`) no Java 2D do Windows, substituindo por texto limpo e profissional (`ATIVO`, `INATIVO`, `CONFIGURADO`).
  - Criado o teste de regressão `ConfigPanelSelectionHarnessTest.java` validando a instanciação e o ciclo de vida de `onPanelSelected()` sem exceções.
- **Validação:** `ConfigPanelSelectionHarnessTest` (1/1), `UiPanelDecompositionTest` (`ConfigPanel.java` com 771 linhas $\le 1000$) aprovados; aplicação recompilada, empacotada e reiniciada no Windows.

### Pagamentos de Plataforma para Ativação e Renovação de Planos (M-Pesa, e-Mola e Transferência Manual) — 2026-09-25 — **concluída com SPEC e HARNESS**
- Criada especificação técnica canónica: `docs/PLATFORM_SUBSCRIPTION_PAYMENTS_SPEC.md` (`SPEC-PSP-001`).
- Criada matriz de testes (harness): `docs/PLATFORM_SUBSCRIPTION_PAYMENTS_HARNESS.md` (`HARNESS-PSP-001`).
- **Contratos & DTOs (`contracts`):**
  - `SubscriptionPlanDetailDTO` (plano, label comercial, preço mensal em MT, descrição de recursos).
  - `SelfServiceSubscriptionPaymentRequest` (plano, meses contratados, forma de pagamento, telemóvel, referência/comprovativo e observações).
  - `SubscriptionPaymentResultDTO` (sucesso, mensagem amigável, id de transação e DTO da assinatura atualizada).
- **Backend & Regras de Negócio (`backend`):**
  - `SubscriptionService`:
    - `listAvailablePlans()`: cataloga planos comerciais `BASIC` (1.500 MT), `PRO` (3.500 MT) e `ENTERPRISE` (7.500 MT).
    - `calculateRenewalPrice(plan, months)`: cálculo determinístico de faturação com tabela progressiva de descontos moçambicanos (3m: 5%, 6m: 10%, 12m: 15% anual).
    - `initiateSelfServiceRenewal(request)`: fluxo unificado que suporta tanto pagamentos móveis automáticos (M-Pesa/e-Mola por Push USSD) quanto transferência bancária manual / depósito com submissão de comprovativo.
    - `confirmMobileRenewal(transactionId)`: validação e estorno de transação móvel aprovada com extensão imediata do `validUntil`.
  - `MySubscriptionController`:
    - `GET /api/subscription/plans`: consulta pública e autenticada de planos e preços em MT.
    - `POST /api/subscription/renew`: iniciação de pedido de renovação pelo assinante.
    - `POST /api/subscription/confirm-payment/{transactionId}`: confirmação reativa de transação móvel aprovada.
  - Mantida 100% intacta a ativação manual administrativa em `PlatformSubscriptionController` e no `PlataformaPanel.java`.
  - Harness de backend `PlatformSubscriptionPaymentsHarnessTest` (6/6 testes verdes, cobrindo PSP-01 a PSP-06).
  - Testes unitários `SubscriptionServiceTest` (7/7 testes verdes).
- **Desktop & UI Swing (`desktop`):**
  - `MySubscriptionApiClient`: métodos `listPlans()`, `renewSubscription()` e `confirmPayment()`.
  - `SubscriptionRenewalDialog.java`: modal moderno e executivo com cartões de plano, selecção de período com badges de desconto (5%, 10%, 15%), rádio para M-Pesa / e-Mola (Push USSD) e Transferência Bancária manual (Millennium BIM / BCI).
  - `ConfigPanel.java`: adicionado botão `[ Renovar / Activar Plano ]` com ícone `fas-crown` na aba de Subscrição, abrindo o fluxo com recarregamento reativo da assinatura.
  - Harness de desktop `SubscriptionRenewalUiHarnessTest` (2/2 testes verdes).
- **Validação Arquitetural & Execução:**
  - `MultiModuleArchitectureHarnessTest` (6/6) e `UiPanelDecompositionTest` (todos os ficheiros $\le 1000$ linhas) 100% aprovados.
  - Backend e Desktop compilados, empacotados e ativos na sessão interativa do utilizador (`javaw` PID 9384).

### Integração de Pagamento Móvel (M-Pesa & e-Mola via Push USSD no POS) — 2026-09-25 — **concluída com SPEC e HARNESS**
- Criada especificação técnica canónica: `docs/MOBILE_PAYMENT_INTEGRATION_SPEC.md` (`SPEC-MPI-001`).
- Criada matriz de testes (harness): `docs/MOBILE_PAYMENT_INTEGRATION_HARNESS.md` (`HARNESS-MPI-001`).
- **Contratos & DTOs (`contracts`):**
  - Enums `MobilePaymentProvider` (`MPESA` Vodacom 84/85, `EMOLA` Movitel 86/87) e `MobilePaymentStatus` (`PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`, `CANCELLED`).
  - Records imutáveis `InitiateMobilePaymentRequest`, `MobilePaymentResponse` e `MobilePaymentStatusResponse`.
- **Backend & Persistência (`backend`):**
  - Migração Flyway `V70__mobile_payment_transactions.sql` com índices de auditoria e isolamento multi-tenant por empresa.
  - Entidade JPA `MobilePaymentTransaction` e repositório `MobilePaymentRepository`.
  - Serviço `MobilePaymentService` com normalização de números Moçambicanos (9 dígitos), validação estrita de prefixos por operadora, geração de referências financeiras canónicas e motor de simulação/sandbox inteligente.
  - Endpoints REST `POST /api/pos/mobile-payment/initiate`, `GET /api/pos/mobile-payment/{transactionId}/status` e `POST /simulate-complete`.
  - Harness de backend `MobilePaymentHarnessTest` (6/6 testes verdes, cobrindo MPI-01 a MPI-06).
- **Desktop & Checkout POS (`desktop`):**
  - Cliente `POSApiClient` com suporte a iniciação, consulta periódica e simulação.
  - Diálogo interativo `MobilePaymentModal.java` com contador regressivo (60s), animação de pulso, validação de número de telemóvel em tempo real e feedback sonoro (`PosAudioFeedbackEngine`).
  - Integração no `PosPaymentDialog.java`: ao selecionar "M-Pesa" ou "e-Mola", surge o botão inteligente `[ Push M-Pesa ]` / `[ Push e-Mola ]`, preenchendo automaticamente a referência financeira autorizada no comprovativo da venda.
  - Harness de desktop `MobilePaymentModalHarnessTest` (2/2 testes verdes).
- **Validação:** Compilação e harnesses 100% aprovados, limites de linhas ($\le 1000$) e regras de arquitetura desacoplada preservados; aplicação empacotada e reiniciada no Windows.

### Correcção de Sobreposição de Atalho no Botão Remover do POS — 2026-09-25 — **corrigido e validado**
- **Causa Raiz:** No `ModernButton.java`, o badge de tecla de atalho (`shortcutText`, ex.: `[Del]`) era pintado sobre o canvas direito sem que os insets do botão refletissem esse espaço. Como a UI do Swing (`BasicButtonUI`) centralizava o texto "Remover" e o ícone na largura total da moldura, o texto estendia-se para a direita, sendo atropelado pelo badge. Adicionalmente, na barra de ações do carrinho (`POSPanel.java`), a caixa "Venda a Crédito (Conta Corrente)" dividia o mesmo `BorderLayout` horizontal entre `Remover` e `Finalizar Venda`, espremendo os botões.
- **Solução Implementada:**
  - `ModernButton.java`: Sobrescritos `getInsets()` e `getInsets(Insets)` para reservar dinamicamente a largura do badge (`calculateShortcutBadgeWidth()`) acrescida de 8px de resguardo à direita. A área de layout do texto (`viewRect`) agora termina estritamente antes do badge, tornando impossível qualquer sobreposição.
  - `POSPanel.java`: Isolada a opção de crédito em linha própria (`creditRow`) logo acima dos botões, dedicando a linha inferior (`buttonRow`) exclusivamente para `Remover` (à esquerda) e `Finalizar Venda` (ao centro/direita).
- **Validação:** `PosQuickTenderHarnessTest` (6/6), `PosButtonColourHierarchyTest` (1/1), `UiPanelDecompositionTest` (1/1, `POSPanel.java` a 997 linhas $\le 1000$) e `MultiModuleArchitectureHarnessTest` (6/6) 100% verdes; aplicação recompilada e reiniciada.

### Dashboard Comercial & Executivo: Variação de Vendas em Tempo Real, Ticket Médio e Margem Bruta — 2026-09-25 — **concluída com SPEC e HARNESS**
- Criada especificação técnica: `docs/DASHBOARD_COMMERCIAL_KPI_AND_TREND_SPEC.md`.
- Criada matriz de testes (harness): `docs/DASHBOARD_COMMERCIAL_KPI_AND_TREND_HARNESS.md`.
- Criado harness automatizado: `desktop/src/test/java/mz/multicore/erp/gui/DashboardCommercialKpiHarnessTest.java` (8 testes, 100% verde).
- **Motor de Tendências & Intervalos Comparativos (`DashboardTrendCalculator.java`):**
  - Resolução temporal determinística (`resolvePeriods`) para todos os filtros (`HOJE` vs ontem, `ESTA_SEMANA` vs semana anterior, `ESTE_MES` vs mês anterior, `ESTE_ANO` vs ano anterior, `TODOS` histórico).
  - Cálculo de variação percentual robusto com tratamento estrito de divisão por zero e transição de base nula.
  - Cálculo determinístico de Ticket Médio (`calculateAverageTicket`) e Margem Bruta Estimada (`calculateGrossMarginPercentage`).
- **Cartões de KPI & Badges de Tendência (`KpiCard.java` & `DashboardPanel.java`):**
  - `KpiCard.TrendBadge` expandido com suporte a atualização reativa em tempo real (`updateTrend`), tooltips de comparação e total imunidade contra caracteres Unicode crus (`▲`/`▼`) através de ícones vetoriais FontAwesome.
  - Card de **FATURAÇÃO TOTAL**: Apresenta valor consolidado, `TrendBadge` de variação com o período imediatamente anterior e subtítulo executivo de Ticket Médio (`TM: X.XX MT/venda`).
  - Card de **VENDAS POS**: Apresenta total arrecadado no balcão, `TrendBadge` reativo de desempenho e contagem de recibos com Ticket Médio por cliente de balcão.
- **Validação:** `DashboardCommercialKpiHarnessTest` (8/8), `ExecutiveUiExperienceHarnessTest` (10/10), `UiPanelDecompositionTest` (1/1) e `MultiModuleArchitectureHarnessTest` (6/6) 100% verdes; aplicação empacotada e reiniciada na sessão interactiva.

### Pagamento Rápido no POS (Quick Tender) & Badges de Teclado — 2026-09-25 — **concluída com SPEC e HARNESS**
- Criada especificação técnica: `docs/POS_QUICK_TENDER_AND_KEYBADGE_SPEC.md`.
- Criada matriz de testes (harness): `docs/POS_QUICK_TENDER_AND_KEYBADGE_HARNESS.md`.
- Criado harness automatizado: `desktop/src/test/java/mz/multicore/erp/gui/pos/PosQuickTenderHarnessTest.java` (6 testes, 100% verde).
- **Visor de Troco & Cédulas Nacionais:**
  - `PosPaymentDialog.java` transformado em ecrã executivo de checkout comercial rápido.
  - Visor de troco de alto contraste com valores em 22pt bold (verde para troco a entregar, âmbar para valor em falta).
  - Cédulas Moçambicanas de 1 toque: `Exacto`, `50 MT`, `100 MT`, `200 MT`, `500 MT`, `1000 MT`, `2000 MT`.
  - Seletor visual de métodos de pagamento com chips temáticos (Numerário, Cartão POS, M-Pesa, e-Mola, Transferência).
- **Badges de Teclado nos Botões (`KeyBadge` & `ModernButton`):**
  - Implementado `KeyBadge.java` e suporte nativo em `ModernButton.setShortcut("F9")` com renderização de tecla física em relevo.
  - Aplicados atalhos nos botões principais do POS: `[F9]` Finalizar Venda, `[F6]` Quantidade, `[Del]` Remover, `[F7]` Fidelidade, `[Z]` Fechar Caixa.
- **Validação:** `PosQuickTenderHarnessTest`, `UiPanelDecompositionTest`, `DesktopThinContextTest` e `MultiModuleArchitectureHarnessTest` 100% aprovados; aplicação recompilada e relançada.

### Correção de Sobreposição de Botões no Topo do POS — 2026-09-25 — **corrigido e validado**
- **Causa Raiz:** O widget da balança (`scaleWidget`), com largura de ~340px, estava adicionado ao painel `segmented` (lado esquerdo) juntamente com as abas de navegação ("Venda POS", "Histórico de Vendas", "Fidelidade (F7)", "Contingência"). Somando aos botões de sessão do lado direito (`sessionActions`: Refresh, "Fechos (Z)", "Abrir Caixa", "Sangria / Suprimento", "Fechar Caixa (Z)"), a barra superior exigia mais de 1560px, provocando colisão e sobreposição directa entre os botões da balança e os botões de caixa em ecrãs padrão.
- **Solução Implementada:**
  - Realocado o `scaleWidget` para o `sessionBanner` (`PosLayout.createSessionBanner(statusLabel, scaleWidget)`), integrando os controlos de balança e peso em tempo real à direita do estado operacional do caixa.
  - A barra superior (`topBar`) agora contém exclusivamente as abas de navegação à esquerda (~445px) e as acções de caixa à direita (~488px), reduzindo a largura total para menos de 935px e garantindo folga ampla em qualquer resolução (1080p, 1366x768).
- **Validação:** `UiPanelDecompositionTest`, `PosButtonColourHierarchyTest` e `UiMicroInteractionsHarnessTest` aprovados com 100% de sucesso; pacote desktop gerado e executado na sessão interactiva.

### Composição Caixa → Embalagem → Unidade — 2026-09-25 — **implementada com SPEC e HARNESS**
- Produto passa a guardar `packagesPerBox` e `unitsPerPackage`; `unitsPerBox` é calculado no backend
  como produto dos dois factores e permanece compatível com clientes e relatórios anteriores.
- Migração aditiva `V68__product_packaging_composition.sql` preserva stocks existentes, interpretando
  cada unidade anterior como uma embalagem de uma unidade.
- Cadastro/edição e detalhe do produto apresentam embalagens por caixa, unidades por embalagem e
  total por caixa somente leitura.
- Entradas de stock, facturas, pedidos, compras e encomendas aceitam caixas, embalagens e unidades;
  stock, preços e impostos continuam em unidades-base.
- Documentos comerciais justificam a quantidade com as colunas configuráveis **Embalagens**
  (`quantidade ÷ unidades/embalagem`), **Caixas** (`quantidade ÷ unidades/caixa`) e **% da Caixa**
  (`quantidade ÷ unidades/caixa × 100`), sem impacto nos cálculos fiscais ou de stock. O cabeçalho
  A4 usa abreviações legíveis: `Emb.`, `Cx.` e `% Cx.`.
- SPEC/HARNESS: `docs/PRODUCT_PACKAGING_COMPOSITION_SPEC.md` e
  `docs/PRODUCT_PACKAGING_COMPOSITION_HARNESS.md`.
- Validação: compilação incremental verde; suite completa com **1.328 testes, 0 falhas, 0 erros e
  1 ignorado**, incluindo `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`; build
  limpo e relançamento operacional executados na conclusão.

### Micro-Interações, UI Líquida e Badges em Pílula (SPEC e HARNESS) — 2026-09-25 — **concluída com SPEC e HARNESS**
- Criada especificação técnica: `docs/UI_MICRO_INTERACTIONS_SPEC.md`.
- Criado harness automatizado: `desktop/src/test/java/mz/multicore/erp/gui/components/UiMicroInteractionsHarnessTest.java` (5 testes, 100% verde).
- **Transição Suave de Hover (`ModernButton.java`):**
  - Implementada interpolação de cor linear fluida via `UIHelper.blendColors(normalColor, hoverColor, progress)` em 6 passos a ~50fps (120ms).
  - Feedback táctil: ligeiro deslocamento de 1px ao premir, reproduzindo a sensação física de clique.
  - Limpeza de recursos rigorosa no descarte do componente (`removeNotify`).
- **Toasts Flutuantes Animados (`ToastManager.java`):**
  - Transição de entrada com slide-up de 8px e fade-in de opacidade (onde suportado pela GPU/sistema).
  - Saída suave com fade-out antes do `dispose()`.
  - Botão de fechar e suporte a clique imediato para descartar a notificação.
- **Pílulas de Estado Reutilizáveis (`StatusBadge.java`):**
  - Componente em estilo pílula (Pill Badge) com cantos perfeitamente arredondados, fundo translúcido da cor semântica e texto de alto contraste com ícone vetorial FontAwesome.
  - Métodos de conveniência: `success`, `warning`, `danger`, `info`, `neutral`.
- **Limpeza de Caracteres Unicode no Monitoramento (`SystemMonitoringDialog.java`):**
  - Removidos símbolos brutos `●`, `▲`, `✖` do badge de saúde do sistema, substituindo por ícones vetoriais FontAwesome (`fas-check-circle`, `fas-exclamation-triangle`, `fas-times-circle`), eliminando qualquer risco de quadrinhos (`▯`).
- **Validação:** Testes de feedback profissional (`ProfessionalFeedbackHarnessTest`), micro-interações (`UiMicroInteractionsHarnessTest`) e limites de decomposição (`UiPanelDecompositionTest`) aprovados; aplicação recompilada e relançada na sessão interativa do Windows.

### Botão de Logout & Terminar Sessão — 2026-09-24 — **implementado e validado**
- Implementado mecanismo explícito e acessível de **Terminar Sessão (Logout)** no Desktop:
  - **Top Bar (`MainFrame`):** Adicionado botão de logout no chip de utilizador (ícone `fas-sign-out-alt` vermelho, tooltip explicativo e ação com diálogo de confirmação).
  - **Sidebar Retrátil (`CollapsibleSidebar`):** Botão de logout dedicado no card de perfil do operador no rodapé do menu lateral, com menu de contexto também ao clicar no avatar.
  - **Paleta de Comandos (`GlobalSearchDialog` / `Ctrl+K`):** Ação rápida `act_logout` com termos de busca ("logout", "sair", "encerrar", "trocar utilizador", "login", "desconectar").
  - **Roteamento & Segurança:** `UIHelper.requestLogout(Component parent)` invoca `UIHelper.onForcedLogout` registado pelo `DesktopLauncher`, que invalida a sessão via `/api/auth/logout`, limpa `DesktopSessionStore` e `CurrentUserContext`, fecha a janela principal e reabre com segurança o diálogo de login `LoginDialog`.
- Todos os limites de decomposição (`UiPanelDecompositionTest`, `MainFrame.java` a 988 linhas $\le 1000$) e arquitetura (`MultiModuleArchitectureHarnessTest`) mantidos 100% verdes.

### Eliminação de Reticências (`...` / `…`) em Botões do Sistema — 2026-09-24 — **implementado e validado**
- Removidas todas as reticências literais (`...` e `…`) de todos os botões da aplicação (`CommercialInvoicesView`, `CommercialOrdersView`, `ComprasPanel`, `PurchaseOrdersPanel`, `StockProductActions`, `StockCategoriesPanel`, `HRPanel`, `HREmployeeActions`, `PlataformaPanel`, `LoginDialog`).
- `ModernButton` atualizado com método estático `stripEllipsis()`, higienização automática no construtor e no método `setText()`, além de margem horizontal interna e cálculo de tamanho preferencial com margem de segurança para prevenir que o Swing LayoutManager trunque títulos de botões com reticências.
- `ActionMenuButton` atualizado para higienizar rótulos em menus suspensos de botões.
- Recompilação, validação com harnesses (`UiPanelDecompositionTest`, `SidebarFavoritesHarnessTest`) e relançamento interativo no Desktop.

### Modernização Visual da Barra de Título (FlatLaf Window Decorations) — 2026-09-24 — **implementado e validado**
- Ativadas as decorações de janela unificadas do FlatLaf (`flatlaf.useWindowDecorations=true`, `TitlePane.useWindowDecorations=true`, `TitlePane.unifiedBackground=true`, `JRootPane.titleBarShowIcon=true`, `JRootPane.titleBarShowTitle=true`).
- A moldura branca/cinzenta clássica nativa do Windows foi substituída pela barra de título personalizada do FlatLaf integrada no tema ativo do ERP, com cantos arredondados, botões de minimizar/maximizar/fechar estilizados e transição perfeita com a barra de ferramentas superior.
- Desktop recompilado e relançado na sessão interativa.

### Correção de Glifos Quadrados na Barra Lateral — 2026-09-24 — **corrigido e validado**
- **Causa Raiz Identificada:** Foi utilizado o caractere Unicode raw emoji estrela `⭐` no cabeçalho `"⭐ FAVORITOS"` (`CollapsibleSidebar.java`) e no prefixo de rótulo dos itens favoritos `String labelText = isFav ? "⭐ " + label : label;` (`SidebarNavItem.java`). No Swing em Windows com fontes de sistema padrão (`Segoe UI`), emojis não possuem glifo vectorial nativo suportado pelo Java 2D e renderizam como caixas de caractere não imprimível `▯` ("quadrinhos"), desalinham o texto dos itens e violam a regra de não usar emojis.
- **Solução Implementada:**
  - `CollapsibleSidebar.java`: `SectionHeader` atualizado com sobrecarga que aceita `Icon`. A secção de topo agora exibe o ícone vectorial FontAwesome `UIHelper.icon("fas-star", 10, UIHelper.PENDING_YELLOW)` ao lado de `"FAVORITOS"` sem nenhum emoji.
  - `SidebarNavItem.java`: Removido o prefixo de emoji do texto em `paintLabel`. Os nomes dos módulos voltam a renderizar perfeitamente alinhados e nítidos.
- **Validação:** `SidebarFavoritesHarnessTest` (7/7) e `UiPanelDecompositionTest` (1/1) 100% aprovados; desktop recompilado e empacotado.

### Estabilização funcional pós-Fase 27 — 2026-09-24 — **concluída com SPEC e HARNESS**

- Criados `docs/SYSTEM_FUNCTIONAL_STABILIZATION_SPEC.md` (`SPEC-SFS-001`) e
  `docs/SYSTEM_FUNCTIONAL_STABILIZATION_HARNESS.md` (`HARNESS-SFS-001`).
- Segurança multiempresa reforçada: mudança de papel, desactivação e revogação de acesso não podem
  remover o último administrador **activo** de uma empresa; a decisão usa o papel por empresa.
- Harnesses de auditoria forense e previsão de tesouraria alinhados com o RBAC fail-closed
  `MANAGER/ADMIN`, mantendo utilizadores comuns bloqueados.
- Desktop estabilizado: vocabulário temporal canónico de oito opções, matrizes executivas sem
  paginação indevida, selecção modelo/vista correcta em quebras de stock e estado vazio rico.
- Impressão fiscal e Relatório Z passam obrigatoriamente por `PrintPreviewDialog`.
- Funcionalidades recentes deixaram de introduzir `JOptionPane`; o inventário legado regressou ao
  limite canónico de 49 chamadas e ganhou prompt temático reutilizável em `ModernMessageDialog`.
- Verificação final: `mvn -q clean compile` verde; `mvn -q test` verde com **1.316 testes**, zero
  falhas, zero erros e 1 teste previamente ignorado. `MultiModuleArchitectureHarnessTest` 6/6.

### Fase 27: Módulo de Observabilidade Multi-Tenant & Central de Alarmes (Sonoro & E-mail) — 2026-09-24 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/MULTI_TENANT_MONITORING_ALERTS_SPEC.md` (`SPEC-MTMA-001`) — Especificação técnica do monitoramento operacional multi-tenant em tempo real, agregação de status por empresa/inquilino (backups, anomalias forenses pendentes e utilizadores), síntese sonora PCM em memória sem dependências externas, disparo transacional de e-mails com cooldown inteligente de 15 minutos anti-spam e integração com o sino de notificações.
  - `docs/MULTI_TENANT_MONITORING_ALERTS_HARNESS.md` (`HARNESS-MTMA-001`) — Matriz de testes automatizados MTMA-01 a MTMA-08.
- **Componentes Canónicos Criados & Atualizados:**
  - `contracts`: DTOs records `TenantHealthDTO.java`, `SystemAlertIncidentDTO.java`, `SystemAlertTestResultDTO.java`.
  - `backend`:
    - `SystemIncidentManager.java`: Gestor thread-safe em memória dos últimos 50 incidentes operacionais.
    - `SystemAlertEmailService.java`: Despacho de e-mail de alerta transacional com cooldown de 15 minutos e suporte a modo de teste.
    - `TenantMonitoringService.java`: Consolidação do estado multi-tenant a partir de repositórios, bases de dados e auditorias.
    - `SystemMonitoringController.java`: Endpoints REST `/api/monitoring/tenants-health`, `/api/monitoring/incidents`, `/api/monitoring/test-email-alert`.
  - `desktop`:
    - `SoundAlertManager.java`: Motor Singleton de alarme sonoro sintetizado em tempo real (900 Hz + 1200 Hz), com salvaguarda estrita para ambientes headless e persistência de preferências em `${user.home}/.multicore/alert_sound_settings.json`.
    - `SystemMonitoringApiClient.java`: Expandido com chamadas para dados multi-tenant, incidentes e teste de e-mail.
    - `SystemMonitoringDialog.java`: Reorganizado em abas modernas ("Diagnóstico Geral", "Saúde Multi-Tenant", "Central de Alarmes") com botões de teste interativo e tabela de incidentes em tempo real (mantido em 560 linhas $\le 1000$).
    - `NotificationFeed.java`: Injeção automática de incidentes críticos do sistema com prioridade máxima no sino do ERP.
- **Validação Automatizada (Harness & Arquitetura):**
  - `SystemAlertHarnessTest.java` (backend - 5/5 testes verdes).
  - `SystemMonitoringHarnessTest.java` (backend - 7/7 testes verdes).
  - `SoundAlertHarnessTest.java` (desktop - 3/3 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
  - **22/22 testes unitários, de integração e de arquitetura 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Limpeza e Modernização de Selects (JComboBox): Remoção de Traços e Hífens (`---` e `—`) — 2026-09-23
- **Padronização Visual & Elegância nos Menus Suspensos:**
  - Remoção de delimitadores arcaicos e poluídos (`---` e `— ... —`) em opções padrão de todos os `JComboBox` da aplicação.
  - Substituição por texto limpo, moderno e direto:
    - `"--- Todos os Armazéns ---"` $\rightarrow$ `"Todos os Armazéns"` (`StockBatchesPanel.java`, `StockPanel.java`).
    - `"— Sem categoria —"` $\rightarrow$ `"Sem categoria"` (`StockProductActions.java`).
    - `"— IVA Padrão (16%) —"` $\rightarrow$ `"IVA Padrão (16%)"` (`StockProductActions.java`).
    - `"— sem prestador cadastrado —"` $\rightarrow$ `"Sem prestador cadastrado"` (`HREmployeeActions.java`).
    - `"— A crédito (pagar depois) —"` $\rightarrow$ `"A crédito (pagar depois)"` (`ComprasPanel.java`).
    - `"— Consumidor Final (sem registo) —"` $\rightarrow$ `"Consumidor Final (sem registo)"` (`ComercialPanel.java`, `QuotationEditorDialog.java`).
    - `"— Seleccionar Conta —"`, `"— Sem Extractos —"`, `"— Nenhuma transacção compatível encontrada —"` $\rightarrow$ versões limpas em `BankReconciliationPanel.java`.
    - Placeholders de formulários `"— FEFO automático —"` $\rightarrow$ `"FEFO automático"` (`CommercialInvoicesView.java`, `CommercialOrdersView.java`).
  - Total compatibilidade com índices e seleção de modelo (`getSelectedIndex() == 0` preservado).
  - Verificação de arquitetura e decomposição: 16 testes verdes (incluindo `UiPanelDecompositionTest`, com todos os ficheiros $\le 1000$ linhas).

### Refatoração Ergonómica: Redesenho do TablePager & Diário Contabilístico — 2026-09-23
- **TablePager Moderno & Simétrico:**
  - Substituição das cápsulas verticais desproporcionadas por botões de navegação quadrados simétricos (30 × 30 px) `PagerNavButton`.
  - Estilo moderno suave (*ghost / outline*) com cantos arredondados, bordas subtis e realce dinâmico em hover.
  - Estado desabilitado refinado com fundo translúcido (sem blocos pretos opacos mortos).
  - Ícones de navegação atualizados (`fas-angle-double-left`, `fas-chevron-left`, `fas-chevron-right`, `fas-angle-double-right`).
  - Alinhamento de altura com o seletor de registos por página (30 px).
- **Diário Contabilístico (`AccountingPanel`):**
  - Barra superior de pesquisa rápida com `TableQuickFilterBar` (`Ctrl+F`) e botão `[ Actualizar ]`.
  - Rodapé executivo de conferência de partidas dobradas com cálculo automático: `Total Débito` · `Total Crédito` · `Equilibrado ✓ / Desbalanceado ⚠`.
  - Manutenção estrita do limite de linhas: `AccountingPanel.java` (508 linhas $\le 1000$) e `TablePager.java` (219 linhas $\le 1000$).

### Fase 26: Barra Universal de Filtro Rápido e Pesquisa em Tabelas (`TableQuickFilterBar` / `Ctrl+F`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/TABLE_QUICK_FILTER_SPEC.md` (`SPEC-TBLF-001`) — Especificação técnica do componente universal de pesquisa e filtragem rápida em tabelas Swing, correspondência multi-termo *case-insensitive* (`(?i)`), conjunção lógica AND entre colunas, atalhos de teclado `Ctrl+F` e `Escape`, contador reativo em tempo real e conformidade com temas e alto contraste.
  - `docs/TABLE_QUICK_FILTER_HARNESS.md` (`HARNESS-TBLF-001`) — Matriz de testes automatizados TBLF-01 a TBLF-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `TableQuickFilterBar.java`: Componente visual universal com `TableRowSorter`, campo de texto estilizado, botão limpar `[ ✕ ]`, contador reativo, atalhos de foco `Ctrl+F` e `Escape`, e métodos de empacotamento `wrapWithFilter`.
  - `UIHelper.java`: Adicionados os métodos utilitários `attachQuickFilter(JTable table)` e `wrapTableWithQuickFilter(JScrollPane scrollPane, JTable table)`.
- **Validação Automatizada (Harness & Arquitetura):**
  - `TableQuickFilterHarnessTest.java` (desktop - 8/8 testes verdes).
  - `FormDraftAutoSaveHarnessTest.java` (desktop - 8/8 testes verdes).
  - `UiDensityZoomHarnessTest.java` (desktop - 8/8 testes verdes).
  - `RecentItemsHistoryHarnessTest.java` (desktop - 8/8 testes verdes).
  - `HighContrastThemeHarnessTest.java` (desktop - 8/8 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis com linhas $\le 1.000$).
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 25: Auto-Salvamento & Recuperação de Rascunhos de Formulários contra Cortes de Energia / Fecho Acidental (`FormDraftManager` / `FormDraftBanner`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/FORM_DRAFT_AUTOSAVE_SPEC.md` (`SPEC-DFRT-001`) — Especificação técnica do sistema de persistência local atómica e assíncrona de rascunhos de formulários em `${user.home}/.multicore/drafts/<formKey>.json`, banner de aviso contextual (`FormDraftBanner`) e ciclo de vida de restauração/descarte.
  - `docs/FORM_DRAFT_AUTOSAVE_HARNESS.md` (`HARNESS-DFRT-001`) — Matriz de testes automatizados DFRT-01 a DFRT-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `FormDraft.java`: Record imutável com `formKey`, `title`, `timestampMillis`, `fields` (`Map<String, String>`) e formatação relativa de tempo.
  - `FormDraftManager.java`: Gestor thread-safe Singleton com `saveDraftAsync`, `saveDraftSync`, `loadDraft`, `discardDraft`, `hasDraft` e persistência atómica em JSON com fallback gracioso.
  - `FormDraftBanner.java`: Barra de notificação visual moderna em tom âmbar suave com ícone `fas-save`, botão de ação `[ Restaurar Rascunho ]` e descarte `[ Descartar ]`.
  - `UIHelper.java`: Adicionado método de utilidade `attachDraftBanner(Container, formKey, onRestore)` para integração automática e desobstruída em qualquer tela ou diálogo de edição.
- **Validação Automatizada (Harness & Arquitetura):**
  - `FormDraftAutoSaveHarnessTest.java` (desktop - 8/8 testes verdes).
  - `UiDensityZoomHarnessTest.java` (desktop - 8/8 testes verdes).
  - `RecentItemsHistoryHarnessTest.java` (desktop - 8/8 testes verdes).
  - `HighContrastThemeHarnessTest.java` (desktop - 8/8 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 24: Densidade de Interface & Escala de Tipografia / Zoom Operacional (`UiDensity` / `UiDensityManager`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/UI_DENSITY_ZOOM_SPEC.md` (`SPEC-DENS-001`) — Especificação técnica dos modos de densidade (`COMPACT` 28px/32px/0.90x, `STANDARD` 36px/38px/1.0x, `COMFORTABLE` 44px/44px/1.15x), persistência em `java.util.prefs.Preferences` (`"density"`), propagação para `UIManager` e repintura atómica de tabelas.
  - `docs/UI_DENSITY_ZOOM_HARNESS.md` (`HARNESS-DENS-001`) — Matriz de testes automatizados DENS-01 a DENS-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `UiDensity.java`: Enum canónico com as métricas de altura de linha de tabela, altura de controlo, escala tipográfica e resolução por identificador.
  - `UiDensityManager.java`: Gestor thread-safe Singleton com comutação cíclica (`cycleDensity()`), persistência em preferências, propagação para `UIManager` e notificação de ouvintes.
  - `UIHelper.java`: Atualizado `styleTable(table)` e `initGlobalTheme()` para aplicar dinamicamente a altura de linha de tabela da densidade ativa.
  - `ConfigPanel.java`: Botão de comutação rápida de densidade na barra de ferramentas superior (`fas-text-height`). Mantido em 874 linhas ($\le 1.000$).
  - `MainFrame.java`: Integrado o comando de alternância de densidade na Command Palette (`Ctrl+K`). Mantido em 998 linhas ($\le 1.000$).
- **Validação Automatizada (Harness & Arquitetura):**
  - `UiDensityZoomHarnessTest.java` (desktop - 8/8 testes verdes).
  - `RecentItemsHistoryHarnessTest.java` (desktop - 8/8 testes verdes).
  - `HighContrastThemeHarnessTest.java` (desktop - 8/8 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 23: Histórico de Itens Recentes & Quick-Recall (`Ctrl+H` / `RecentItemsHistoryManager`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/RECENT_ITEMS_HISTORY_SPEC.md` (`SPEC-RHIS-001`) — Especificação técnica do histórico de navegação e atividade recente, modelo imutável `RecentItem`, lógica de inserção MRU e evicção LRU (capacidade 20 itens), persistência atómica local em `${user.home}/.multicore/recent_items.json`, atalhos de teclado e interface de recall rápido.
  - `docs/RECENT_ITEMS_HISTORY_HARNESS.md` (`HARNESS-RHIS-001`) — Matriz de testes automatizados RHIS-01 a RHIS-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `RecentItem.java`: Record imutável contendo `id`, `category`, `title`, `subtitle`, `targetView`, `recordId`, `iconCode`, `timestampMillis` e cálculo relativo de tempo decorrido ("Agora mesmo", "há 5 min", "há 2 h").
  - `RecentItemsHistoryManager.java`: Gestor thread-safe Singleton com deduplicação por chave, ordenação MRU, evicção LRU e persistência em JSON via Jackson.
  - `RecentItemsDialog.java`: Diálogo executivo modal com pesquisa instantânea, renderização de badges coloridos por categoria de módulo, navegação com `Enter` ou duplo clique, e limpeza de histórico.
  - `MainFrame.java`: Integrado o atalho universal `Ctrl+H`, botão de histórico `fas-history` na barra superior, item na Command Palette (`Ctrl+K`) e auto-registo de navegação de módulos. Mantido em 997 linhas ($\le 1.000$).
- **Validação Automatizada (Harness & Arquitetura):**
  - `RecentItemsHistoryHarnessTest.java` (desktop - 8/8 testes verdes).
  - `HighContrastThemeHarnessTest.java` (desktop - 8/8 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 22: Modo de Alto Contraste Acessível & Operação Exterior / Outdoor (`Theme.HIGH_CONTRAST` & `UIHelper.cycleTheme`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/HIGH_CONTRAST_ACCESSIBILITY_SPEC.md` (`SPEC-HCON-001`) — Especificação técnica do tema de alto contraste acessível e operação exterior em Moçambique (luz solar direta, estaleiros, feiras, armazéns portuários e baixa visão), conformidade WCAG 2.1 AAA ($\ge 7.0:1$), fundo `#000000`, texto branco `#FFFFFF` (21:1), texto prateado `#E0E0E0` (>15:1), bordas brancas marcadas e ciclo de alternância contínua.
  - `docs/HIGH_CONTRAST_ACCESSIBILITY_HARNESS.md` (`HARNESS-HCON-001`) — Matriz de testes automatizados HCON-01 a HCON-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `Theme.java`: Adicionado `Theme.HIGH_CONTRAST` na ordem canónica de 10 cores da paleta, atualizado `Theme.byId(id)` para suportar aliases (`"high_contrast"`, `"highcontrast"`, `"contrast"`) e métodos de inspeção booleana.
  - `UIHelper.java`: Adicionados `isHighContrast()`, `cycleTheme()`, métodos públicos de conformidade WCAG AAA/AA (`meetsWcagAaa`, `meetsWcagAa`, `contrastRatio`) e estilização FlatLaf de alto contraste com foco e bordas marcadas.
  - `MainFrame.java`: TopBar atualizado com comutador cíclico (`fas-adjust` em alto contraste) e adicionado atalho direto na Command Palette (`Ctrl+K`). Mantido em 975 linhas ($\le 1.000$).
  - `ConfigPanel.java`: Botão de tema atualizado com etiqueta e ícone reativos ao modo de alto contraste. Mantido em 860 linhas ($\le 1.000$).
- **Validação Automatizada (Harness & Arquitetura):**
  - `HighContrastThemeHarnessTest.java` (desktop - 8/8 testes verdes).
  - `ButtonContrastTest.java` (desktop - 6/6 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 21: Motor de Feedback Sonoro Discreto no POS & Leituras (`PosAudioFeedbackEngine`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/POS_AUDIO_FEEDBACK_SPEC.md` (`SPEC-PAUD-001`) — Especificação técnica do motor de síntese de som PCM em memória na JVM (sem dependências de ficheiros `.wav` externos), tons para eventos `SUCCESS`, `WARNING`, `ERROR` e `SCALE_STABLE`, execução assíncrona não-bloqueante e persistência de definições em `${user.home}/.multicore/audio_settings.json`.
  - `docs/POS_AUDIO_FEEDBACK_HARNESS.md` (`HARNESS-PAUD-001`) — Matriz de testes automatizados PAUD-01 a PAUD-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `PosAudioFeedbackEngine.java`: Motor Singleton de áudio não-bloqueante com geração de tons de onda senoidal PCM e tolerância a ambientes headless.
  - `PosCatalogController.java`: Integrado o disparo do evento `SUCCESS` (800 Hz) ao adicionar produto ao carrinho, e `ERROR` (350 Hz) em produtos esgotados.
  - `PosBarcodeActions.java`: Integrado o disparo do evento `ERROR` em código de barras não encontrado ou PLU inválido.
- **Validação Automatizada (Harness & Arquitetura):**
  - `PosAudioFeedbackHarnessTest.java` (desktop - 7/7 testes verdes).
  - `SidebarFavoritesHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UserManagementPanelHarnessTest.java` (desktop - 2/2 testes verdes).
  - `ExecutiveDetailDialogHarnessTest.java` (desktop - 7/7 testes verdes).
  - `ButtonIconContrastHarnessTest.java` (desktop - 6/6 testes verdes).
  - `IconSystemHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UiOrganizationNavigationHarnessTest.java` (desktop - 6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis com linhas $\le 1.000$).
  - **43/43 testes 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 20: Atalhos Favoritos Personalizáveis na Barra Lateral Executiva (`CollapsibleSidebar` & `SidebarFavoritesManager`) — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/SIDEBAR_FAVORITES_SPEC.md` (`SPEC-SFAV-001`) — Especificação técnica da persistência local de favoritos, secção dinâmica **"⭐ FAVORITOS"** no topo do menu lateral, menu de contexto com botão direito (`JPopupMenu`), estrela indicadora `⭐` em rótulos e atualização em tempo real sem necessidade de reiniciar.
  - `docs/SIDEBAR_FAVORITES_HARNESS.md` (`HARNESS-SFAV-001`) — Matriz de testes automatizados SFAV-01 a SFAV-08.
- **Componentes Canónicos Criados & Atualizados (`desktop`):**
  - `SidebarFavoritesManager.java`: Singleton thread-safe responsável pelo carregamento e persistência atómica de atalhos favoritos em `${user.home}/.multicore/user_favorites.json`.
  - `SidebarNavItem.java`: Atualizado para disparar menu de contexto ao clicar com o botão direito (`[ ⭐️ Fixar nos Favoritos ]` / `[ ❌ Remover dos Favoritos ]`), renderizar o prefixo `⭐ ` nos rótulos de itens favoritos e fornecer acessores.
  - `CollapsibleSidebar.java`: Conetado ao `SidebarFavoritesManager` para reconstruir e alinhar dinamicamente a secção de topo **"⭐ FAVORITOS"** (615 linhas $\le 1000$).
- **Validação Automatizada (Harness & Arquitetura):**
  - `SidebarFavoritesHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UserManagementPanelHarnessTest.java` (desktop - 2/2 testes verdes).
  - `ExecutiveDetailDialogHarnessTest.java` (desktop - 7/7 testes verdes).
  - `ButtonIconContrastHarnessTest.java` (desktop - 6/6 testes verdes).
  - `IconSystemHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UiOrganizationNavigationHarnessTest.java` (desktop - 6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis com linhas < 1.000).
  - **36/36 testes 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Reactor Maven empacotado com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 19: Gestão Visual de Utilizadores, Matriz de Permissões & PIN de Autorização de Gestor — 2026-09-23 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/GESTAO_UTILIZADORES_PIN_SPEC.md` (`SPEC-GUP-001`) — Especificação técnica da gestão visual de utilizadores, RBAC fail-closed, atribuição de alçadas/roles, redefinição de senhas, alternância de estado ativo/inativo e PINs de autorização de gestor em 4 dígitos com PBKDF2.
  - `docs/GESTAO_UTILIZADORES_PIN_HARNESS.md` (`HARNESS-GUP-001`) — Matriz de testes automatizados GUP-01 a GUP-08.
- **Módulo `contracts`:**
  - `AppUserDTO.java`: Adicionados campos `hasManagerPin` e `email` + construtor retrocompatível de 5 parâmetros.
  - `UserSecurityRequestsDTOs.java`: DTOs de pedido `SetManagerPinRequest`, `VerifyManagerPinRequest`, `ResetPasswordRequest`, `ToggleUserStatusRequest`.
- **Módulo `backend`:**
  - `AppUser.java`: Adicionados campos `managerPinHash` e `email`.
  - `AppUserService.java`: Implementados métodos de negócio `setManagerPin`, `verifyManagerPin`, `resetPassword`, `toggleUserStatus` com validação estrita e codificação PBKDF2 via Spring Security.
  - `UserController.java`: Adicionados endpoints REST protegidos por RBAC `/api/users/{username}/pin` (PUT), `/api/users/verify-pin` (POST), `/api/users/{username}/reset-password` (POST), `/api/users/{username}/status` (PUT).
  - `UserManagementHarnessTest.java`: 4/4 testes verdes cobrindo atribuição/verificação de PIN, redefinição de senha e alteração de estado.
  - `SecurityPermissionGuardCoverageTest.java`: 3/3 testes verdes confirmando enforcement RBAC fail-closed.
- **Módulo `desktop`:**
  - `UserApiClient.java`: Expandido com cliente HTTP desacoplado para PINs, senhas e estado.
  - `UserManagementPanel.java`: Painel executivo visual de utilizadores integrado no painel de Configurações da Empresa, com 4 KPI cards superiores, tabela zebrada com crachás de estado e PIN, e botões de ação com diálogos assíncronos.
  - `UserEditorDialog.java`: Diálogo modal moderno (`ModernFormDialog`) para criação e edição de utilizadores e atribuição de perfis de acesso (`SELLER`, `MANAGER`, `ADMIN`, `ACCOUNTANT`, `HR_MANAGER`).
  - `ManagerPinDialog.java`: Diálogo modal dedicado para pedido de PIN de autorização de gestor em operações sensíveis (anulações, descontos elevados, excepções de crédito), com bypass seguro em ambiente de testes headless.
  - `UserManagementPanelHarnessTest.java`: 2/2 testes verdes cobrindo instanciação sem exceções e validação de PIN em headless.
- **Validação Automatizada (Harness & Arquitetura):**
  - `UserManagementPanelHarnessTest.java` (desktop - 2/2 testes verdes).
  - `UserManagementHarnessTest.java` (backend - 4/4 testes verdes).
  - `SecurityPermissionGuardCoverageTest.java` (backend - 3/3 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
  - **15/15 testes unitários, de integração e de arquitetura 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Compilação do reactor Maven empacotada com **100% BUILD SUCCESS** (`multicore-contracts`, `multicore-backend`, `multicore-desktop`).

### Fase 18: Modal Universal de Detalhes Executivos (`ExecutiveDetailDialog`) & Cobertura Multi-Módulo — 2026-09-22 — **implementado com SPEC e HARNESS**
- **Componente Canónico Reutilizável (`desktop`):**
  - `ExecutiveDetailDialog.java`: Motor universal para visualização de entidades de negócio com cabeçalho rico (Avatar com iniciais dinâmicas ou ícone FontAwesome, subtítulos contextualizados), Badge pill de status com severidades semânticas (`SUCCESS`, `WARNING`, `DANGER`, `INFO`, `NEUTRAL`), faixa horizontal de KPIs em cartões `ModernPanel`, abas temáticas coloridas (`UIHelper.styleTabbedPaneMulticore`) e rodapé flexível com botões de ação e fechamento.
- **Implementação e Cobertura nos Módulos de Negócio:**
  - **Comercial / Vendas (Encomendas)**: `OrderDetailsDialog.java` — Reescrito utilizando `ExecutiveDetailDialog` (eliminando o `JOptionPane` legado). Possui KPIs de valor total, linhas, volumes, peso total e condições de pagamento; 3 abas ("Itens da Encomenda", "Condições & Entrega", "Impressão & Rastreabilidade") e ação assíncrona de emissão/impressão de PDF.
  - **Comercial / Vendas (Clientes)**: `CustomerDetailDialog.java` — Ficha completa do cliente com avatar de iniciais, status ativo, limite de crédito, prazos de pagamento acordados e pontos de fidelidade. Integrado no `ClientesPanel.java` via botão "Ver Ficha" (`fas-id-card`) e duplo clique na tabela.
  - **Compras (Fornecedores)**: `SupplierDetailDialog.java` — Ficha completa do fornecedor com avatar, KPIs de linha telefónica direta, representante comercial, e-mail e atalho direto para a aba de Contas a Pagar. Integrado no `PurchaseSuppliersPanel.java` via botão "Ver Ficha" (`fas-id-card`) e duplo clique na tabela.
  - **Stock / Inventário (Artigos)**: `StockProductDetailDialog.java` — Ficha de produto com layout de imagem em alta definição, indicadores de margem de lucro, saldos detalhados por armazém e tabela de rastreabilidade de lotes com validade. Integrado no `StockPanel.java` via "Ficha do Artigo" e duplo clique.
  - **Recursos Humanos (Colaboradores)**: `HREmployeeProfileDialog.java` — Perfil unificado do trabalhador com dados cadastrais, histórico de recibos salariais, faltas, férias aprovadas e saúde ocupacional. Integrado no `HRPanel.java` via botão "Ver Perfil".
  - **Garantia Universal para Todos os Restantes Módulos & Tabelas (`JTable`)**: `RecordDetailsDialog.java` e `RowDetailsInspector.java` — Atualizados para que qualquer tabela em qualquer módulo (POS, Tesouraria, Contabilidade, Auditoria, Plataforma, Definições) sem diálogo dedicado pré-escrito abra automaticamente um `ExecutiveDetailDialog` gerado em tempo real com Badge Pill de estado, cartões KPI autodetectados, abas de dados/rastreabilidade e botão de cópia.
- **Validação Automatizada (Harness & Arquitetura):**
  - `ExecutiveDetailDialogHarnessTest.java` (desktop - 7/7 testes verdes).
  - `ButtonIconContrastHarnessTest.java` (desktop - 6/6 testes verdes).
  - `IconSystemHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UiOrganizationNavigationHarnessTest.java` (desktop - 6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis com linhas < 1.000).
  - `MultiModuleArchitectureHarnessTest.java` (backend & reactor - 6/6 testes verdes).
  - **32/32 testes 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Backend Spring Boot saudável e ativo (`http://localhost:8080/actuator/health` -> `{"status":"UP"}`).
  - Desktop Swing interativo relançado com sucesso via tarefa agendada Windows.

### Fase 17: Observabilidade & Monitoramento Profissional do Sistema — 2026-09-21 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/SYSTEM_MONITORING_SPEC.md` — Especificação de telemetria, limiares de recursos (Heap, Disco, Latência HikariCP), severidades (HEALTHY, WARNING, CRITICAL) e endpoints de diagnóstico.
- **Contratos Canónicos (`contracts`):**
  - DTOs independentes de JPA/Spring/Swing: `SystemHealthDTO.java`, `DatabaseHealthDTO.java`, `MemoryHealthDTO.java`, `StorageHealthDTO.java`, `ThreadPoolHealthDTO.java`, `SubsystemStatusDTO.java`, `SystemDiagnosticsExportDTO.java`.
- **Backend (`backend`):**
  - `SystemMonitoringService.java`: Recolha em tempo real de telemetria de JVM, ping `SELECT 1` e pool HikariCP, armazenamento, threads, subsistemas e geração de relatório formatado.
  - `SystemMonitoringController.java`: Endpoints REST `GET /api/monitoring/system-health` e `GET /api/monitoring/diagnostics-export`.
  - `SecurityInterceptor.java`: Suporte transparente para endpoints em nível de servidor (`/api/monitoring/*`).
- **Desktop (`desktop`):**
  - `SystemMonitoringApiClient.java`: Cliente HTTP desacoplado com contratos DTOs.
  - `SystemMonitoringDialog.java`: Diálogo executivo moderno com semáforo, 4 cards de KPIs de hardware/persistência, tabela de subsistemas com tempo de resposta em ms, atualização assíncrona, cópia para clipboard e exportação em ficheiro `.txt` (388 linhas, <= 1000).
  - Integração nos painéis `ConfigPanel.java` (barra de ferramentas, 912 linhas) e `PlataformaPanel.java` (aba Saúde & Diagnóstico).
- **Validação Automatizada (Harness):**
  - `SystemMonitoringHarnessTest.java` (backend - 7/7 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes).
  - `ButtonIconContrastHarnessTest.java` (desktop - 6/6 testes verdes).
  - `IconSystemHarnessTest.java` (desktop - 7/7 testes verdes).
  - `UiOrganizationNavigationHarnessTest.java` (desktop - 6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis prioritários <= 1000 linhas).
  - **33/33 testes unitários, de integração e de arquitetura 100% verdes (BUILD SUCCESS)**.
- **Runtime:**
  - Backend ativo e saudável na porta 8080 (`http://localhost:8080/actuator/health` -> `{"status":"UP"}`).
  - Desktop interativo executando sob PID `16000`.

### Fase 16: Ícone Profissional Executivo & Seletor Universal de Calendário — 2026-09-20 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/UNIVERSAL_DATE_PICKER_SPEC.md` — Especificação do seletor visual universal de datas com popup FlatLaf e navegação de calendário.
  - `docs/UNIVERSAL_DATE_PICKER_HARNESS.md` — Matriz de testes automatizados do calendário e campos de data.
- **Componentes Canónicos Criados e Integrados:**
  - `desktop/src/main/resources/icons/`: Gerado conjunto multi-resolução (`app-icon.ico` com mipmaps 16, 24, 32, 48, 64, 128, 256px e PNGs correspondentes) a partir de arte gráfica 3D de microprocessador multicore de alta tecnologia.
  - `installer/app-icon.ico`: Cópia dedicada para o instalador Windows.
  - `scripts/build-windows-installer.ps1`: Adicionado parâmetro `--icon $icon` ao `jpackage` para embutir o ícone no `.exe`, atalho do menu Iniciar e atalho da área de trabalho na instalação.
  - `UIHelper.java`: Métodos `getAppIcons()` (lista de multi-resolução para `Window.setIconImages`) e `getAppIcon(int size)`.
  - `MainFrame.java` & `LoginDialog.java`: Atualizados para `setIconImages(UIHelper.getAppIcons())` e emblema de alta resolução de 64px no login.
  - `C:\Users\miran\Desktop\Multicore ERP.lnk`: Atualizado com o ícone `installer\app-icon.ico`.
- **Validação Automatizada:**
  - `AppIconHarnessTest.java` (3/3 testes verdes).
  - `UiOrganizationNavigationHarnessTest.java` (6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes).
  - Reactor empacotado com **100% BUILD SUCCESS** e desktop relançado com sucesso.
- **Correção de Resiliência no Stock/Comercial (Atualizar Produto):**
  - Implementada sanitização universal de números (`sanitizeNumber`) em `StockProductActions.java` (suporta vírgula, espaços de milhar e símbolos).
  - Preço de compra opcional (default 0 para serviços/itens sem custo direto), validação de valores não-negativos e validação de invariante de peso.
  - Mapeamento robusto de taxas de IVA e persistência com `setOnSaveAsync` e feedback visual integrado.
  - Testado via `StockInteractionHarnessTest`, `ComercialControllerIntegrationTest` e verificado live no desktop (`PID 5208`).

---

### Fase 15: Gestão de Fecho de Caixa & Relatório Z (Physical Cash Closing & Z-Report) — 2026-09-19 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/POS_FECHO_CAIXA_ZREPORT_SPEC.md` — Especificação técnica do fecho cego de caixa, reconciliação de gaveta, decomposição de vendas por método de pagamento (Numerário, Cartão, M-Pesa/e-Mola, Transferência Bancária, Crédito), histórico auditável de sessões e geração de Dossiê PDF A4.
  - `docs/POS_FECHO_CAIXA_ZREPORT_HARNESS.md` — Matriz de conformidade automatizada ZREP-01 a ZREP-08.
- **Componentes Canónicos Criados e Integrados:**
  - `contracts`: `PosZReportDTO.java` (decomposição por meio de pagamento, construtor canónico de 20 parâmetros + 14 parâmetros retrocompatível) e `PosSessionSummaryDTO.java`.
  - `backend`: Repositório `TillSessionRepository.java` (`findByCompanyIdOrderByOpenDateDesc`), serviço `POSService.java` (`buildZReport` com cálculo detalhado por meio de pagamento e `getSessionsHistory`), gerador PDF `POSZReportPrintService.java` (`buildPaymentBreakdownTable`) e endpoints REST em `POSController.java` (`/api/pos/sessions/{sessionId}/z-report`, `/api/pos/sessions/{sessionId}/z-report/pdf`, `/api/pos/sessions/history`).
  - `desktop`: Cliente HTTP `POSApiClient.java` (`getZReport`, `getSessionsHistory`, `renderZReport`), diálogos Swing `PosBlindCloseDialog.java` e `PosSessionHistoryDialog.java`, e botão "Histórico Fechos (Z)" integrado em `POSPanel.java` (mantido em **998 linhas**, $\le 1000$).
- **Validação Automatizada:**
  - `PosZReportHarnessTest.java` (backend - 3/3 testes verdes).
  - `PosZReportUiHarnessTest.java` (desktop - 2/2 testes verdes).
  - `FinalUiUniformityHarnessTest.java` (desktop - 4/4 testes verdes).
  - `UiPanelDecompositionTest.java` (desktop - 1/1 teste verde, todos os painéis prioritários $\le 1000$ linhas).
  - `MultiModuleArchitectureHarnessTest.java` (backend - 6/6 testes verdes, reactor isolado `contracts` / `backend` / `desktop`).
  - **16/16 testes unitários e de integração 100% verdes (BUILD SUCCESS)**.
  - Reactor Maven empacotado com **100% BUILD SUCCESS**.

---

### Fase 14: Módulo de Inventário & Contagem Física de Stock — 2026-09-19 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/INVENTARIO_CONTAGEM_FISICA_SPEC.md` — Especificação técnica da contagem física de stock (leitura por código de barras, contagem cega opcional, apuramento de excedentes/faltas em MT, reconciliação automática de stock e exportação de Dossiê PDF A4).
  - `docs/INVENTARIO_CONTAGEM_FISICA_HARNESS.md` — Matriz de conformidade automatizada ICF-01 a ICF-08.
- **Componentes Canónicos Criados e Integrados:**
  - `contracts`: `InventoryStatus`, `InventoryItemDTO`, `InventorySessionDTO`, `CreateInventorySessionRequest`, `UpdateInventoryItemCountRequest` DTOs em `mz.multicore.erp.modules.inventory.dto`.
  - `backend`: Migração Flyway `V66__inventory_physical_counting.sql`, entidades JPA `InventoryPhysicalSession` e `InventoryPhysicalItem`, repositórios Spring Data JPA `InventoryPhysicalSessionRepository` e `InventoryPhysicalItemRepository`, serviço de negócio `InventoryPhysicalCountingService`, gerador de PDF `InventoryPhysicalCountingPrintService` e controlador REST `InventoryPhysicalCountingController`.
  - `desktop`: Cliente HTTP `InventoryPhysicalCountingApiClient`, painel UI Swing `PhysicalInventoryPanel.java`, integrado como aba 5 ("Inventário & Contagem Física") em `StockPanel.java` (mantido em **928 linhas**, $\le 1000$) e acções em `MainFrame.java`.
- **Validação Automatizada:**
  - `PhysicalInventoryHarnessTest.java` (backend - 4/4 testes verdes).
  - `PhysicalInventoryPanelHarnessTest.java` (desktop - 3/3 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis prioritários $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, isolamento do reactor Maven `contracts` / `backend` / `desktop`).
  - **18/18 testes unitários e de integração 100% verdes (BUILD SUCCESS)**.
  - Reactor Maven empacotado com **100% BUILD SUCCESS**, backend ativo (`{"status":"UP"}`) e aplicação desktop lançada interativamente via Windows Task Scheduler.

---

### Fase 13: Integração Directa com Balança USB/Serial no POS & Cartão de Fidelidade — 2026-09-19 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/POS_BALANCA_FIDELIDADE_SPEC.md` — Especificação técnica do driver de balança USB/Serial em tempo real, widget visual de peso vivo, protocolo NCI/Toledo/Simulado e integração do Cartão de Fidelidade com resgate de pontos.
  - `docs/POS_BALANCA_FIDELIDADE_HARNESS.md` — Matriz de conformidade automatizada SCL-01 a SCL-05 e LYT-01 a LYT-04.
- **Componentes Canónicos Criados e Integrados:**
  - `SerialScaleReader.java`: Driver thread-safe para parsing de tramas ASCII de balança Serial/USB COM (STX/ETX, NCI, Toledo), gestão de tara e modo de simulação para desenvolvimento/testes.
  - `PosScaleLiveWidget.java`: Componente visual compacto integrado no cabeçalho do POS exibindo estado da balança (🟢 Ligada / 🟡 Instável / 🔴 Desconectada), peso activo em tempo real (ex: `1.850 kg`), indicador de tara e botões rápidos `[ ⚖️ Capturar ]` e `[ Tarar ]`.
  - `PosLoyaltyController.java`: Controller desacoplado para consulta rápida do Cartão de Fidelidade por scanner de código de barras, NUIT ou telemóvel (`F7`), cálculo de pontos acumulados (`1 ponto por 100 MT`), valor monetário equivalente e abatimento de pontos no checkout (`LoyaltyEngine`).
  - `ClientDTO.java`: Expansão retrocompatível no módulo `contracts` para suportar `loyaltyPoints` e `code`.
  - `POSPanel.java`: Integração limpa do `PosScaleLiveWidget` e atalho `F7` mantendo a classe estritamente em **994 linhas** ($\le 1000$).
- **Validação Automatizada:**
  - `SerialScaleReaderTest.java` (4/4 testes verdes).
  - `PosScaleLoyaltyHarnessTest.java` (3/3 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis prioritários $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, reactor isolado `contracts` / `backend` / `desktop`).
  - **12/12 testes unitários e de integração 100% verdes (BUILD SUCCESS)**.
  - Reactor Maven empacotado com **100% BUILD SUCCESS**, backend ativo (`{"status":"UP"}`) e desktop lançado interativamente via Windows Task Scheduler.

---

### Fase 12: Uniformização de Texto e Ícones Brancos em Botões e Menus em Todo o Sistema — 2026-09-19 — **implementado e validado**
- **Objectivo & Alcance:**
  - Eliminação de textos e ícones cinzentos/apagados em botões na aplicação desktop, substituindo-os por branco nítido (`Color.WHITE`) para máxima legibilidade e conformidade ergonómica em tema escuro.
- **Componentes e Painéis Atualizados:**
  - `UIHelper.java`:
    - `UIManager.put("Button.foreground", Color.WHITE)` (substitui cinzento por branco em botões nativos e de diálogos).
    - `UIManager.put("Button.disabledText", new Color(255, 255, 255, 140))` (texto em botões inativos com branco translúcido nítido em vez de cinzento escuro).
    - `UIManager.put("TabbedPane.foreground", Color.WHITE)` (abas de navegação com texto branco).
    - Menus e popups com `MenuItem.foreground`, `Menu.foreground`, `PopupMenu.foreground` configurados com `Color.WHITE`.
  - `DashboardPanel.java`:
    - `updatePeriodButtonStyles()`: botões de período inativos (`Hoje`, `Esta Semana`, `Este Mês`, `Este Ano`) agora mantêm texto `Color.WHITE` sobre o fundo do card, eliminando o cinzento `TEXT_MUTED` de difícil leitura.
  - `LoginDialog.java`:
    - Botões rápidos de credenciais de teste (*chips*) com `Color.WHITE`.
    - Botão de alternância de visualização de senha com ícone `Color.WHITE`.
  - `CashFlowForecastPanel.java`:
    - Botão de impressão com ícone e texto `Color.WHITE` e hover `ACCENT_BLUE_HOVER`.
  - `SidebarNavItem.java`:
    - Itens inativos da barra lateral com texto `Color.WHITE` em tema escuro.
  - `TableNavigator.java` e `ArrowScrollPanel.java`:
    - Ícones de setas de navegação vertical e lateral atualizados para `Color.WHITE`.
  - `TableContextMenu.java`:
    - Itens de menu de contexto com ícones e texto `Color.WHITE`.
- **Validação Automatizada:**
  - `ButtonContrastTest` (4/4 verde), `ButtonIconContrastHarnessTest` (5/5 verde).
  - Regressão: `FinalUiUniformityHarnessTest` (4/4 verde), `UniversalTablePaginationHarnessTest` (5/5 verde), `DesktopInitializationTest` (1/1 verde), `UiPanelDecompositionTest` (1/1 verde).
  - Total: **20/20 testes verdes** sem qualquer exceção na thread AWT.
  - Aplicação empacotada e reiniciada via tarefa agendada `MulticoreERP`.

---

### Fase 11: Paginação Universal Automática em Tabelas Desktop (`ClientTablePagination` & `PaginationSouthComposite`) — 2026-09-19 — **implementado e validado**
- **Objectivo & Alcance:**
  - Todas as tabelas de listagem (`JTable`) da aplicação desktop Swing agora recebem automaticamente paginação local consistente e uniforme (tamanhos de página: 25, 50, 100, 200; contagem de registos; selector de página; navegação primeira/anterior/próxima/última).
  - Resolução definitiva de tabelas sem `RowSorter` inicial ou com rodapés pré-existentes / tardios em `BorderLayout.SOUTH`.
- **Componentes Canónicos Aprimorados:**
  - `ClientTablePagination.java`:
    - Auto-instalação de `TableRowSorter` se a tabela ainda não possuir sorter no momento da estilização.
    - Suporte dinâmico a trocas de modelo (`PropertyChangeListener("model")`) e de sorter (`PropertyChangeListener("rowSorter")`).
    - Métodos `isInstalled(table)` e `component()`.
  - `UIHelper.java`:
    - Criação de `PaginationSouthComposite` (contentor `BoxLayout.Y_AXIS` empilhando a barra de paginação no topo e rodapés adicionais em baixo).
    - `installSouthGuard`: `ContainerListener` permanente que intercepta adições tardias a `BorderLayout.SOUTH` e as funde dinamicamente no `PaginationSouthComposite`, evitando a perda de paginação quando painéis adicionam totais ou botões após a tabela.
    - Suporte para contentores `BoxLayout`.
  - Opt-out controlado para tabelas operacionais e com `TablePager` de servidor:
    - POS Cart (`cartTable` em `POSPanel.java`).
    - Faturação com paginação de servidor (`owner.invoicesTable` em `CommercialInvoicesView.java`).
    - Histórico de Vendas POS com paginação de servidor (`owner.salesHistoryTable` em `PosSalesHistoryPanel.java`).
    - Diário Contabilístico (`AccountingPanel.java`).
- **Validação Automatizada:**
  - `UniversalTablePaginationHarnessTest.java` (5/5 testes verdes cobrindo instalação automática, composição com rodapé pré-existente, guarda de adições tardias, opt-out de `DISABLED` e verificação de tabelas reais de negócio).
  - Regressão: `ClientTablePaginationTest`, `DesktopInitializationTest`, `FinalUiUniformityHarnessTest`, `UiPanelDecompositionTest`, `MultiModuleArchitectureHarnessTest` (6/6 verde no backend).
  - Aplicação empacotada com sucesso e lançada interativamente via Windows Task Scheduler.

---

### Fase 10: Painel com Navegação por Setas e Eliminação de Overflow Horizontal (`ArrowScrollPanel`) — 2026-09-17 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/DASHBOARD_ARROW_SCROLL_OVERFLOW_SPEC.md` (`SPEC-ASO-001`) — Especificação técnica do contentor rolável responsivo sem overflow horizontal e sistema de navegação por setas verticais ([ ▲ ] topo e [ ▼ ] base).
  - `docs/DASHBOARD_ARROW_SCROLL_OVERFLOW_HARNESS.md` (`HARNESS-ASO-001`) — Critérios de validação e matriz de testes ASO-01 a ASO-07.
- **Componentes Canónicos Criados e Integrados:**
  - `ArrowScrollPanel.java` (`mz.multicore.erp.gui.components`):
    - Contentor interno `WidthTrackingContainer` implementando `Scrollable.getScrollableTracksViewportWidth() == true`, forçando o conteúdo a ajustar-se à largura da viewport e eliminando 100% dos overflows horizontais.
    - Ocultação das barras de rolagem nativas cinzentas (`HORIZONTAL_SCROLLBAR_NEVER`, `VERTICAL_SCROLLBAR_NEVER`).
    - Barra lateral estreita de controlo (34px) com botão superior [ ▲ ], botão inferior [ ▼ ], indicador de progresso proporcional (`ScrollIndicatorTrack`) e animação suave (`scrollSmoothly`).
    - Desativação automática de botões nos extremos (topo e base) com tooltips contextuais.
    - Suporte nativo e contínuo a teclado (`Page Up`, `Page Down`, `Ctrl+Home`, `Ctrl+End`) e rato (`MouseWheelListener`).
  - `DashboardPanel.java`: migrado de `JScrollPane` convencional para `ArrowScrollPanel`, eliminando o corte do 4.º cartão das grelhas de métricas e gráficos sem ultrapassar o limite de 1000 linhas (**602 linhas**, $\le 1000$).
- **Validação Automatizada:**
  - `ArrowScrollPanelTest.java` (5/5 testes verdes cobrindo ASO-01 a ASO-07).
  - `FinalUiUniformityHarnessTest` (4/4 testes verdes, zero literais de cor fora de `UIHelper`).
  - `UiPanelDecompositionTest` (1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `MultiModuleArchitectureHarnessTest` (6/6 testes verdes, isolamento estrito `contracts` / `backend` / `desktop`).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Fase 9: Apuramento Periódico de IVA de Moçambique (Modelo A / Mapa Recapitulativo) — 2026-09-17 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/APURAMENTO_IVA_MOCAMBIQUE_SPEC.md` (SPEC-AIVA-001) — Especificação técnica do apuramento de IVA em conformidade com o Regulamento do IVA de Moçambique (Decreto n.º 7/2008 de 16 de Abril, PAE 16%), incorporando dedução de compras, crédito fiscal anterior e apuramento do saldo final (`A_PAGAR` vs `CREDITO_A_TRANSPORTAR`).
  - `docs/APURAMENTO_IVA_MOCAMBIQUE_HARNESS.md` (HARNESS-AIVA-001) — Matriz de testes de cálculo de IVA AIVA-01 a AIVA-08.
- **Módulo `contracts`:**
  - `IvaSummaryDTO`: campos adicionais `previousCredit`, `payableAmount`, `creditToCarry`, `fiscalStatus`, com construtor canónico e retrocompatível.
- **Módulo `backend`:**
  - `FiscalSummaryService`: cálculo completo considerando `previousCredit` com fórmula $S = (I_{\text{liq}} - I_{\text{ded}}) - C_{\text{ant}}$.
  - `FiscalController`: suporte ao parâmetro opcional `previousCredit` em `/api/fiscal/iva-summary`.
  - `IvaDeclarationPrintService` & `PrintController`: renderização em PDF da Declaração Mensal de IVA referenciando a legislação moçambicana e discriminando base, IVA, crédito reportado e montante a entregar ou reportar.
  - `VatSettlementHarnessTest`: 5 testes unitários e de integração verdes cobrindo apuramento com imposto a pagar, crédito a transportar, absorção total e parcial pelo crédito anterior, e exclusão de faturas/compras canceladas.
- **Módulo `desktop`:**
  - `FiscalApiClient`: chamadas `ivaSummary` e `renderIvaDeclaration` com suporte a `previousCredit`.
  - `FiscalPanel`: input dedicado de `Crédito Anterior (MT)` com botão `Recalcular`, substituição dos cards legados por 4 cartões padronizados `KpiCard.createMetricCard` (IVA Liquidado, IVA Deduzido, Crédito Anterior, Saldo Fiscal Líquido), mantendo o painel em 784 linhas ($\le 1000$).
- **Validação:**
  - `VatSettlementHarnessTest` (5/5 verde no backend).
  - `FinalUiUniformityHarnessTest` (4/4 verde), `UiPanelDecompositionTest` (1/1 verde).
  - `MultiModuleArchitectureHarnessTest` (6/6 verde).
  - Reactor completo compilado com **100% BUILD SUCCESS**.

---

### Fase 8: Envio Directo de Extratos & Cobranças por Email (SMTP com Anexo PDF) — 2026-09-17 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/ENVIO_EMAIL_COBRANCA_SPEC.md` (SPEC-EEC-001) — Especificação do despacho transacional de extratos de conta corrente via SMTP com geração em memória do extrato PDF e envio de cópia de segurança.
  - `docs/ENVIO_EMAIL_COBRANCA_HARNESS.md` (HARNESS-EEC-001) — Testes EEC-01 a EEC-07.
- **Módulo `contracts`:**
  - `SendStatementEmailRequest`, `EmailDispatchResultDTO`.
- **Módulo `backend`:**
  - `spring-boot-starter-mail` adicionado ao `backend/pom.xml`.
  - `CustomerStatementMailService`: geração de PDF via `CustomerStatementPrintService` e despacho `MimeMessageHelper` com modo simulado seguro em dev.
  - Endpoint `POST /api/comercial/statements/customer/email` em `CustomerStatementController`.
  - `CustomerStatementMailHarnessTest`: 3 testes unitários com Mockito.
- **Módulo `desktop`:**
  - `AccountStatementApiClient`: método `sendCustomerStatementEmail`.
  - `SendStatementEmailDialog`: formulário modal para destinatário, CC, assunto, mensagem personalizada e opção de anexo PDF.
  - `CustomerStatementPanel`: botão `Enviar por Email` na barra superior de ações.
  - `SendStatementEmailDialogTest`: teste automatizado de UI.

---

### Fase 7: Padronização dos KPI Cards nos Restantes Painéis — 2026-09-17 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/KPI_CARDS_PADRONIZACAO_SPEC.md` (SPEC-KPI-001) — Regras de unificação de estilo de cartões métricos através de `KpiCard.createMetricCard`.
  - `docs/KPI_CARDS_PADRONIZACAO_HARNESS.md` (HARNESS-KPI-001) — Critérios KPI-01 a KPI-08.
- **Módulo `desktop`:**
  - `KpiCard`: métodos auxiliares `createMetricCard` com ícones, cores temáticas de `UIHelper` e suporte a labels dinâmicos de subtítulo.
  - Painéis unificados: `CreditRiskPanel`, `StockWastePanel`, `CustomerStatementPanel`, `SupplierStatementPanel` e `FiscalPanel` (remoção total de código duplicado de cards).
  - Validação: `KpiCardStandardizationTest` (6/6 verde), `UiPanelDecompositionTest` (1/1 verde), `FinalUiUniformityHarnessTest` (4/4 verde).

---

### Fase 6: Painel de Decisão Executiva Unificado (Dashboard 360°) — 2026-09-17 — **implementado com SPEC e HARNESS**
- **Documentação Canónica:**
  - `docs/DASHBOARD_EXECUTIVO_360_SPEC.md` (SPEC-D360-001) — Especificação do widget "Pulso Estratégico da Empresa".
  - `docs/DASHBOARD_EXECUTIVO_360_HARNESS.md` (HARNESS-D360-001) — Critérios D360-01 a D360-07.
- **Módulo `desktop`:**
  - `StrategicPulseWidget`: consolidação de 4 pilares estratégicos (Conformidade Forense, Liquidez Previsional 30d, Risco de Crédito em Mora e Metas Comerciais) com navegação em 1 clique para os respectivos módulos.
  - Integração em `DashboardPanel` (607 linhas $\le 1000$) e atalho `F12` em `MainFrame`.
  - Validação: `StrategicPulseWidgetTest` (6/6 verde).

---

### Modo de Contingência & Resiliência Local no POS (Offline-First Leve) (Fase 5) — 2026-09-17 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/POS_CONTINGENCIA_RESILIENCIA_SPEC.md` (SPEC-PCR-001) — Especificação técnica da persistência atómica em fila local durável (`pos_contingency_queue.json`), emissão de talão térmico provisório nativo AWT com aviso regulamentar, idempotência fiscal no backend via `contingencyReference` e motor de sincronização FIFO em segundo plano.
  - `docs/POS_CONTINGENCIA_RESILIENCIA_HARNESS.md` (HARNESS-PCR-001) — Matriz de conformidade e testes automatizados PCR-01 a PCR-10.
- **Módulo `contracts`:**
  - Enum `PosContingencyStatus` (`PENDING_SYNC`, `SYNCED`, `REVISION_NEEDED`).
  - Extensão retrocompatível de `POSCheckoutRequest` com campo opcional `contingencyReference`.
- **Módulo `backend`:**
  - Migração de base de dados `V65__pos_contingency.sql` com coluna `contingency_reference` e índice composto `idx_invoices_contingency`.
  - `Invoice` e `InvoiceRepository`: mapeamento e consulta `findByCompanyIdAndContingencyReference`.
  - `POSService`: validação de idempotência estrita (se já sincronizada, devolve a fatura existente sem duplicar número `FT`, stock ou tesouraria).
- **Módulo `desktop`:**
  - `PosContingencySale`: modelo imutável com JSON serialization.
  - `PosThermalReceiptPrinter`: renderizador nativo AWT (`Printable`) e formatador de texto para talões térmicos (80mm/58mm) com carimbo de documento provisório de contingência.
  - `PosContingencyManager`: gestor thread-safe com persistência em ficheiro JSON, deteção de erros de rede e controlo de fila.
  - `PosContingencySyncService`: serviço em background com agendamento periódico a cada 45s e acionamento sob demanda.
  - `PosContingencyDialog`: diálogo modal para auditoria da fila local, reimpressão de talão e sincronização forçada com feedback visual.
  - `POSPanel`: integração com botão dinâmico de contingência `[ ⚠️ Contingência (X) ]`, fallback no checkout e extração de `PosCartItem` preservando a classe estritamente abaixo do limite (**990 linhas**, $\le 1000$).
- **Validação Automatizada:**
  - `PosContingencyHarnessTest.java` (3/3 testes verdes no backend cobrindo PCR-01 a PCR-03).
  - `PosContingencyManagerTest.java` (4/4 testes verdes no desktop cobrindo PCR-04 a PCR-07).
  - Suite de testes POS completa (`PosErgonomicsHarnessTest`, `PosLayoutTest`, `PosProfessionalErgonomicsHarnessTest`, etc.) com **24/24 testes verdes**.
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes, zero literais de cor fora do `UIHelper`).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, isolamento estrito).
  - Reactor Maven compilado com **100% BUILD SUCCESS**.

---

### Central de Auditoria Forense & Controlo de Fraude Interna (Fase 4) — 2026-09-17 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/AUDITORIA_FORENSE_SPEC.md` (SPEC-AFF-001) — Especificação técnica da auditoria forense contínua, regras analíticas de severidade (`CRITICAL`, `SUSPICIOUS`, `INFO`), apuramento de exposição financeira total ($R_{\text{total}} = \sum R_{\text{crítico}} + \sum R_{\text{suspeito}}$), matriz de conformidade (*Compliance Score*) e dossiê probatório em PDF A4.
  - `docs/AUDITORIA_FORENSE_HARNESS.md` (HARNESS-AFF-001) — Critérios de conformidade automatizados AFF-01 a AFF-07.
- **Módulo `contracts`:**
  - Enums criados: `ForensicSeverity` (`CRITICAL`, `SUSPICIOUS`, `INFO`), `ForensicCategory` (`DOC_CANCELLATION`, `EXCESSIVE_DISCOUNT`, `STOCK_SHRINKAGE`, `CREDIT_OVERRIDE`, `PAYMENT_VOID`, `AUDIT_SECURITY`).
  - DTOs records imutáveis: `ForensicAnomalyDTO`, `ForensicAuditSummaryDTO`.
- **Módulo `backend`:**
  - `ForensicAuditService`: Motor analítico de deteção de cancelamentos fiscais, descontos anormais (>10%), quebras de stock atípicas (> 3.000 / > 10.000 MT) e logs de ações sensíveis de segurança.
  - `ForensicAuditPrintService`: Emissão de Dossiê Oficial de Auditoria em PDF A4 com `CompanyHeaderRenderer`, cartões KPI de risco, banner de conformidade, tabela zebrada de anomalias com severidade e termo de encerramento.
  - `ForensicAuditController`: Endpoints REST multi-tenant `/api/audit/forensic` e `/api/audit/forensic/pdf`.
- **Módulo `desktop`:**
  - `ForensicAuditApiClient`: Cliente HTTP tipado com suporte a filtros combinados de período, severidade, categoria e operador.
  - `ForensicAuditPanel.java`: Interface executiva elegante com 4 KPI cards superiores, filtros dinâmicos, tabela zebrada interativa com semáforo de risco, diálogo modal de detalhe probatório e pré-visualização integrada de PDF.
  - `MainFrame.java`: Integrado na secção lateral "Fiscal & Auditoria", atalho na pesquisa global Spotlight (`Ctrl+K`), mantendo a classe estritamente abaixo do limite de 1000 linhas (**964 linhas**, $\le 1000$).
- **Validação Automatizada:**
  - `ForensicAuditHarnessTest.java` (5/5 testes verdes no backend cobrindo AFF-01 a AFF-06).
  - `ForensicAuditPanelHarnessTest.java` (3/3 testes verdes no desktop cobrindo AFF-07).
  - `DesktopThinContextTest` (2/2 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes, zero literais de cor fora do `UIHelper`).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, reactor isolado `contracts` / `backend` / `desktop`).
  - Backend Spring Boot ativo e verificado com `/actuator/health` UP e testes REST/PDF bem-sucedidos.
  - Desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Tesouraria & Projeção de Fluxo de Caixa Previsional (Cash Flow Forecast) (Fase 3) — 2026-09-17 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/TESOURARIA_FLUXO_CAIXA_SPEC.md` (SPEC-TFC-001) — Especificação técnica do motor de liquidez previsional, consolidação de posição de caixa ($C_0$), alocação temporal de entradas e saídas por baldes (`OVERDUE`, `TODAY`, `DAYS_1_7`, `DAYS_8_15`, `DAYS_16_30`, `DAYS_31_60`, `DAYS_PLUS_60`), projeção progressiva cumulativa ($C_t = C_{t-1} + I_t - O_t$), alertas de défice de tesouraria (*Cash Shortage Alert*) e relatório executivo em PDF A4.
  - `docs/TESOURARIA_FLUXO_CAIXA_HARNESS.md` (HARNESS-TFC-001) — Critérios de conformidade automatizados TFC-01 a TFC-07.
- **Módulo `contracts`:**
  - DTOs records imutáveis: `CashFlowForecastDTO`, `CashFlowBucketDTO`, `CashFlowItemDTO`, `CashFlowAlertDTO`.
- **Módulo `backend`:**
  - `CashFlowForecastService`: Cálculo da posição imediata em caixas e bancos, agregação por vencimento de faturas a clientes e compras a fornecedores, projeção cumulativa progressiva e emissão de alertas executivos (`CRITICAL`, `WARNING`, `HEALTHY`).
  - `CashFlowForecastPrintService`: Gerador canónico de relatório de tesouraria em PDF A4 com `CompanyHeaderRenderer`, cartões métricos superiores, banner contextual de liquidez, matriz temporal zebrada, tabela dos 10 maiores recebimentos/pagamentos e termo de responsabilidade financeira.
  - `CashFlowForecastController`: Endpoints REST multi-tenant `/api/finance/forecast` e `/api/finance/forecast/pdf`.
- **Módulo `desktop`:**
  - `CashFlowForecastApiClient`: Cliente HTTP tipado para consulta de projeção e download de PDF.
  - `CashFlowForecastPanel.java`: Interface executiva de alta densidade com 4 KPI cards superiores, banner inteligente de alerta, matriz temporal de liquidez com renderers monetários coloridos e abas de detalhamento de clientes e fornecedores.
  - `FinanceiroPanel.java`: Integrada nova aba 4 "Projeção Previsional" mantendo o painel estritamente em **287 linhas** ($\le 1000$).
  - `MainFrame.java`: Injeção de dependências preservando estritamente as regras de limite de tamanho de painel (**996 linhas**, $\le 1000$).
- **Validação Automatizada:**
  - `CashFlowForecastHarnessTest.java` (5/5 testes verdes no backend cobrindo TFC-01 a TFC-06).
  - `CashFlowForecastPanelHarnessTest.java` (2/2 testes verdes no desktop cobrindo TFC-07).
  - `DesktopThinContextTest` (2/2 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes, zero literais de cor fora do `UIHelper`).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, isolamento do reactor `contracts` / `backend` / `desktop`).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Extrato de Conta Corrente de Clientes & Fornecedores com Reconciliação e PDF Canónico (Fase 2) — 2026-09-16 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/EXTRATO_CONTA_CORRENTE_SPEC.md` (SPEC-ECC-001) — Especificação técnica do extrato de conta corrente, fórmula de saldo progressivo ($S_i = S_{i-1} + \text{débitos} - \text{créditos}$ para clientes, $S_i = S_{i-1} + \text{créditos} - \text{débitos}$ para fornecedores), consolidação de saldo anterior ($S_0$), reconciliação de pendentes e carta canónica de circularização em PDF A4.
  - `docs/EXTRATO_CONTA_CORRENTE_HARNESS.md` (HARNESS-ECC-001) — Critérios de conformidade automatizados ECC-01 a ECC-07.
- **Módulo `contracts`:**
  - DTOs records imutáveis: `CustomerStatementDTO`, `CustomerStatementLineDTO`, `SupplierStatementDTO`, `SupplierStatementLineDTO`.
- **Módulo `backend`:**
  - `CustomerStatementService`: Ordenação cronológica de Faturas (FT), Recibos (RC), Notas de Crédito (NC) e Notas de Débito (ND), cálculo de saldo anterior antes da data de início, saldo corrente linha a linha e montante vencido.
  - `SupplierStatementService`: Ordenação cronológica de Compras a Fornecedor (V/FT) e Pagamentos (PG) com cálculo de saldo em aberto e saldo progressivo.
  - `CustomerStatementPrintService` & `SupplierStatementPrintService`: Geradores de PDF A4 em OpenPDF com `CompanyHeaderRenderer`, bloco de identificação de entidade, cartões métricos superiores, tabela zebrada de movimentos e termo formal de circularização com blocos de assinatura.
  - `CustomerStatementController` (`/api/comercial/statements/customer`, `/api/comercial/statements/customer/pdf`).
  - `SupplierStatementController` (`/api/purchases/statements/supplier`, `/api/purchases/statements/supplier/pdf`).
- **Módulo `desktop`:**
  - `AccountStatementApiClient`: Cliente HTTP tipado para consulta de extratos e download/impressão de PDFs.
  - `CustomerStatementPanel.java`: Interface executiva integrada como aba 3 em `ClientesPanel.java` ("Conta Corrente & Reconciliação") com seletores de cliente, filtros temporais, 4 KPI cards de resumo, tabela progressiva com renderers monetários e diálogo integrado de impressão com pré-visualização.
  - `SupplierStatementPanel.java`: Interface integrada como aba 5 em `ComprasPanel.java` ("Conta Corrente & Reconciliação") para gestão de saldos com fornecedores.
  - `MainFrame.java`: Injeção de dependências preservando estritamente as regras de limite de tamanho de painel (996 linhas, $\le 1000$).
- **Validação Automatizada:**
  - `AccountStatementHarnessTest.java` (5/5 testes verdes no backend cobrindo ECC-01 a ECC-05).
  - `AccountStatementPanelHarnessTest.java` (2/2 testes verdes no desktop cobrindo ECC-06 e ECC-07).
  - `DesktopThinContextTest` (2/2 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis $\le 1000$ linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes, zero violações de cores fora do UIHelper).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes, reactor isolado `contracts` / `backend` / `desktop`).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Radar de Alertas Inteligentes & Risco Proactivo (Fase 1) — 2026-09-16 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/RADAR_ALERTAS_PROACTIVO_SPEC.md` — Especificação técnica do monitoramento proativo de 360° unificando o sino (`NotificationFeed`), painel de notificações e diálogo de intervenção rápida (`SmartAlertsDialog`).
  - `docs/RADAR_ALERTAS_PROACTIVO_HARNESS.md` — Critérios de conformidade automatizados RAP-01 a RAP-07.
- **Componentes e Melhorias Realizadas:**
  - `NotificationFeed.java`: Injeção de `CreditRiskApiClient` e `StockWasteApiClient` com construtores retrocompatíveis. Deteção automática de clientes com risco crítico (`CRITICAL`), clientes bloqueados ou atrasos > 30d (prioridade 3), e quebras de stock pendentes de validação gerencial (`PENDING_APPROVAL`, prioridade 2). Degradação suave contra erros de rede/serviço.
  - `SmartAlertsDialog.java`: Mapeamento das novas categorias com ações contextuais inteligentes (`"Cobrar / Ver Risco"` para crédito e `"Aprovar Quebras"` para perdas de stock).
  - `StockPanel.java`: Adicionado método `selectWasteTab()` (alias de `showWasteManagement()`).
  - `MainFrame.java`: Resolução de rotas virtuais `risco_credito` (redireciona para o cartão `clientes` e ativa a aba de Aging) e `stock_waste` (redireciona para o cartão `stock` e ativa a aba de Quebras). `MainFrame.java` mantido estritamente abaixo de 1000 linhas (997 linhas).
- **Validação Automatizada:**
  - `RadarAlertasProactivoHarnessTest.java` (7/7 testes verdes cobrindo RAP-01 a RAP-07).
  - `NotificationFeedTest.java` (6/6 testes verdes).
  - `CreditRiskPanelHarnessTest.java` (7/7 testes verdes).
  - `StockWastePanelHarnessTest.java` (7/7 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Centro de Risco de Crédito & Cobrança (CRCC) — 2026-09-16 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/RISCO_CREDITO_COBRANCA_SPEC.md` — Especificação técnica do Centro de Risco de Crédito, Matriz de Aging por faixas (Corrente, 1-30d, 31-60d, 61-90d, >90d), níveis executivos (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), cartas de cobrança formal em PDF e fluxo de aprovação de excepções de crédito.
  - `docs/RISCO_CREDITO_COBRANCA_HARNESS.md` — Critérios de conformidade automatizados RCC-01 a RCC-07.
- **Componentes e Melhorias Realizadas:**
  - `CreditRiskPanel.java`: Interface executiva elegante com 4 KPI cards superiores (Total a Receber, Saldo em Mora, Clientes Bloqueados, Risco Elevado/Crítico), matriz de aging em tabela com alinhamento monetário à direita, semáforo visual de risco, pesquisa rápida por nome/NUIT/email, geração e pré-visualização de Notificação de Cobrança em PDF e diálogo modal para solicitação de excepção de crédito conectada ao módulo de aprovações.
  - Decomposição estrita de linhas: painel mantido com 615 linhas (muito abaixo do limite de 1000 linhas).
- **Validação Automatizada:**
  - `CreditRiskPanelHarnessTest.java` (7/7 testes verdes).
  - `CreditRiskHarnessTest.java` no backend (7/7 testes verdes).
  - `StockWastePanelHarnessTest.java` (7/7 testes verdes).
  - `StockWasteHarnessTest.java` no backend (8/8 testes verdes).
  - `UniversalPeriodFilterHarnessTest.java` (9/9 testes verdes).
  - `PeriodFilterVocabularyTest.java` (6/6 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes).
  - `MultiModuleArchitectureHarnessTest.java` (6/6 testes verdes).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado interativamente via Windows Task Scheduler.

---

### Filtro Universal por Período & Gestão de Quebras de Stock — 2026-09-15 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/FILTRO_PERIODO_UNIVERSAL_SPEC.md` — Especificação canónica da linguagem temporal retrospetiva, cálculo de fronteiras e integração nas tabelas transacionais.
  - `docs/FILTRO_PERIODO_UNIVERSAL_HARNESS.md` — Critérios de validação automatizada UFP-01 a UFP-08.
  - `docs/GESTAO_QUEBRAS_STOCK_SPEC.md` — Especificação técnica do ciclo de vida auditável de quebras (alçadas de aprovação, motivos fiscais, custo histórico e radar de validades).
  - `docs/GESTAO_QUEBRAS_STOCK_HARNESS.md` — Critérios de conformidade automatizados GQS-01 a GQS-08.
- **Componentes e Melhorias Realizadas:**
  - `TableFilter.java`: Expansão do vocabulário do `periodCombo()` com `"Ontem"`, `"Esta semana"`, `"Este ano"`, garantindo conformidade matemática e retrospetiva em `matchesPeriod()`.
  - `CommercialInvoicesView.java` & `ComercialPanel.java`: Adicionada a coluna `"Data"` (formato `dd/MM/yyyy HH:mm`) e instalado o filtro universal por período (`PeriodFilter`) em conjunto com o filtro de estado. Constantes de leitura de modelo preservadas e `ComercialPanel.java` mantido estritamente abaixo do limite de 1000 linhas (991 linhas).
  - `StockWastePanel.java`: Integrado o `periodFilterCombo` na barra de ferramentas superior da tabela de quebras, com filtragem combinada de período, estado, motivo e texto.
- **Validação Automatizada:**
  - `UniversalPeriodFilterHarnessTest.java` (9/9 testes verdes).
  - `PeriodFilterVocabularyTest.java` (6/6 testes verdes).
  - `StockWastePanelHarnessTest.java` (7/7 testes verdes).
  - `StockWasteHarnessTest.java` no backend (8/8 testes verdes).
  - `UiPanelDecompositionTest.java` (1/1 teste verde, todos os painéis prioritários < 1000 linhas).
  - `FinalUiUniformityHarnessTest.java` (4/4 testes verdes, zero violações cromáticas).
  - Reactor Maven compilado com **100% BUILD SUCCESS** e desktop empacotado e reiniciado.

---

### Centro de Desempenho Comercial (CDC) — 2026-09-15 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/COMMERCIAL_PERFORMANCE_SPEC.md` — Especificação técnica do módulo CDC (Metas comerciais, progresso em tempo real, ranking de vendedores, bónus e integração com RH/folha salarial).
  - `docs/COMMERCIAL_PERFORMANCE_HARNESS.md` — Critérios de conformidade automatizados (CDC-BIZ-01 a CDC-BIZ-12).
- **Módulo `contracts`:**
  - Enums criados: `GoalPeriod`, `GoalScope`, `BonusType`, `GoalStatus`, `BonusStatus`, `AlertLevel`.
  - DTOs records criados: `SalesGoalDTO`, `CreateSalesGoalRequest`, `UpdateSalesGoalRequest`, `SalesGoalProgressDTO`, `EmployeeRankingDTO`, `SalesGoalBonusDTO`, `ApproveBonusRequest`, `AdjustBonusRequest`, `PerformanceReportDTO`.
  - Atualizado `PayslipDTO` com campo `salesBonus`.
- **Módulo `backend`:**
  - Migração de base de dados: `V62__commercial_performance.sql` criando tabelas `sales_goals`, `sales_goal_bonuses` e coluna `payslips.sales_bonus`.
  - Entidades JPA e Repositórios: `SalesGoal`, `SalesGoalBonus`, `SalesGoalRepository`, `SalesGoalBonusRepository` e entidade `Payslip` atualizada.
  - Motores de Negócio:
    - `BonusCalculatorEngine`: cálculo de prémio fixo, % de receita e % de margem com suporte a teto (`bonusCap`).
    - `GoalProgressEngine`: cálculo de receita e margem realizadas (com custo histórico `lineCost`), ritmo ideal vs real, projeção linear e níveis de alerta (`NONE`, `CAUTION`, `LATE`, `CRITICAL`).
    - `EmployeeRankingService`: agregações por funcionário/vendedor com ranking de vendas, margem bruta, ticket médio e progresso da meta.
    - `SalesGoalService`: gestão completa de metas, validações semânticas BIZ-01 a BIZ-11, aprovação e ajuste de bónus com auditoria.
    - `PerformanceReportService`: consolidação de relatórios executivos de desempenho por equipa.
    - `PerformanceReportPrintService`: geração de PDF executivo de desempenho comercial.
    - `HRService`: integração automática de prémios aprovados nos recibos de vencimento (`salesBonus`), recálculo do líquido e bloqueio após fecho do período.
  - Controlador REST: `PerformanceController` com 12 endpoints versionados sob `/api/performance`.
- **Módulo `desktop`:**
  - Cliente HTTP: `PerformanceApiClient`.
  - Painel decomposto (< 1000 linhas): `PerformancePanel` integrando 4 abas especializadas:
    - `GoalsTab`: listagem, filtros, criação e cancelamento de metas comerciais.
    - `ProgressTab`: visualização em tempo real de cartões com barras de progresso, alertas e projeções.
    - `RankingTab`: tabela de classificação por vendas, margem e ticket médio com botão de exportação/impressão em PDF.
    - `BonusTab`: aprovação, ajuste e integração direta de prémios na folha de pagamentos.
  - Navegação e Alertas:
    - `MainFrame`: registado no menu lateral "Gestão & CRM" com ícone de troféu, atalho no Spotlight (`Ctrl+K`) e navegação suave.
    - `NotificationFeed` e `SmartAlertsDialog`: metas com atraso crítico ou ritmo lento geram alertas inteligentes com navegação direta para o CDC.
- **Validação Automatizada:**
  - `BonusCalculatorEngineTest` (5/5 testes verdes).
  - `GoalProgressEngineTest` (3/3 testes verdes).
  - `CommercialPerformanceHarnessTest` (11/11 testes verdes cobrindo CDC-BIZ-01 a CDC-BIZ-12).
  - `UiPanelDecompositionTest` (1/1 verde).
  - `UiOrganizationNavigationHarnessTest` (6/6 verde).
  - `MultiModuleArchitectureHarnessTest` (6/6 verde).
  - Compilação do reactor Maven: `mvn clean compile` com **100% BUILD SUCCESS**.

---

### Pacote de Excelência Executiva, Rentabilidade e Automação Operacional — 2026-09-13 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/MELHORIAS_EXECUTIVAS_SPEC.md` — Especificação técnica dos 6 novos módulos executivos (Alertas Inteligentes, Impressão Direta POS, Rentabilidade/Margens/DRE, Câmbios Multimoeda, Fidelização por Pontos e Centro de Backup).
  - `docs/MELHORIAS_EXECUTIVAS_HARNESS.md` — Critérios de conformidade automatizados (EX-01 a EX-07).
- **Componentes Canónicos Criados e Integrados:**
  - `SmartAlertsDialog.java`: Centro de alertas classificados por criticidade (🔴 Crítico, 🟡 Atenção, 🟢 Informativo) com botões de ação imediata com 1-clique.
  - `PosDirectPrintEngine.java`: Motor de impressão direta/silenciosa para impressoras térmicas (80mm) sem diálogo modal de confirmação.
  - `ProfitEngine.java` & `ProfitAnalyticsWidget.java`: Motor de apuramento de CMVMC, Lucro Bruto, Margem % e Ticket Médio integrado ao Dashboard com filtro de período.
  - `MultiCurrencyEngine.java` & `CurrencyExchangeDialog.java`: Calculadora de câmbio multimoeda (USD, ZAR, EUR) e conversão de troco em Meticais no POS.
  - `LoyaltyEngine.java`: Motor de pontuação e fidelização de clientes (1 ponto por cada 100 MT) e resgate de desconto.
  - `DatabaseBackupDialog.java`: Diálogo para geração de cópias de segurança com 1 clique e validação de integridade.
  - `MainFrame.java`: Ações `act_alerts`, `act_backup`, `act_currency` registadas na pesquisa global Spotlight (`Ctrl+K`).
- **Validação Automatizada:**
  - `ExecutiveSuiteHarnessTest.java` (6/6 verde), `ProductivitySuiteHarnessTest.java` (6/6 verde), `ProfessionalFeedbackHarnessTest` (9/9 verde), `MultiModuleArchitectureHarnessTest` (6/6 verde), suite completa do desktop com **223/223 testes verdes** e build do reactor com **100% BUILD SUCCESS**.

### Pacote de Produtividade Total, Gestão e Rigor Operacional — 2026-09-13 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/PRODUTIVIDADE_TOTAL_SPEC.md` — Especificação técnica dos 5 novos módulos de produtividade (Dashboard com Filtro Dinâmico e Top 5, POS Fecho Cego, Gerador de Etiquetas de Prateleira, Feed de Atividades Recentes e Exportação CSV Universal).
  - `docs/PRODUTIVIDADE_TOTAL_HARNESS.md` — Critérios de conformidade automatizados (PT-01 a PT-07).
- **Componentes Canónicos Criados e Integrados:**
  - `TableCsvExporter.java` & `TableContextMenu.java`: Exportação universal de qualquer tabela para CSV formatado em UTF-8 com BOM (compatível com Excel) através do menu de contexto e atalho `Ctrl+E`.
  - `ShelfLabelsDialog.java`: Gerador e impressor de etiquetas de prateleira com código de barras vetorial, categoria, data e preço em Meticais em destaque. Integrado no menu de stock e pesquisa global.
  - `TopProductsWidget.java`: Widget visual com ranking dos Top 5 produtos mais vendidos, percentagens e barras de progresso com gradientes.
  - `RecentActivityWidget.java`: Linha do tempo visual de atividade recente (faturas, vendas, compras) com ícones e timestamps relativos.
  - `DashboardPanel.java`: Adicionados chips de filtro dinâmico de período (`Hoje`, `Esta Semana`, `Este Mês`, `Este Ano`, `Todo o Período`) com recálculo assíncrono instantâneo.
  - `StockPanel.java`: Decomposição e integração com `ShelfLabelsDialog` reduzindo linhas de código.
- **Validação Automatizada:**
  - `ProductivitySuiteHarnessTest.java` (6/6 verde), `GlobalSearchShortcutsHarnessTest` (5/5 verde), `ButtonIconContrastHarnessTest` (5/5 verde), `IconSystemHarnessTest` (6/6 verde), `FinalUiUniformityHarnessTest` (4/4 verde), `PosErgonomicsHarnessTest` (4/4 verde), `UiOrganizationNavigationHarnessTest` (6/6 verde), `UiPanelDecompositionTest` (1/1 verde), `MultiModuleArchitectureHarnessTest` (6/6 verde) e build do reactor com **100% BUILD SUCCESS**.

### Pesquisa Global Rápida (`Ctrl+K`), Atalhos (`F1`/`F11`) e Produtividade — 2026-09-13 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/PESQUISA_GLOBAL_ATALHOS_SPEC.md` — Especificação técnica da paleta de comandos Spotlight, indexação de módulos e ações, navegação por teclado e modos operacionais.
  - `docs/PESQUISA_GLOBAL_ATALHOS_HARNESS.md` — Critérios de conformidade automatizados (PGA-01 a PGA-05).
- **Componentes Canónicos Criados:**
  - `GlobalSearchDialog.java`: Diálogo de pesquisa estilo Spotlight/Command Palette com filtragem em tempo real (título, categoria, sinónimos/keywords), atalho `ESC`, navegação por setas `Up`/`Down` e execução direta com `Enter`.
  - `ShortcutHelpDialog.java`: Guia visual temático de atalhos operacionais organizado em cartões categorizados (Navegação & Sistema, POS & Balcão).
- **Integração no Cabeçalho e Janela Principal:**
  - `TopNavBar.java`: Adicionada pílula de pesquisa central `[ 🔍 Pesquisar módulos ou ações...  Ctrl+K ]` clicável.
  - `MainFrame.java`: Registados atalhos globais no `RootPane` (`Ctrl+K` para pesquisa, `F1` para ajuda de atalhos, `F11` para alternar ecrã completo/fullscreen e `Ctrl+B` para menu lateral).
- **Validação Automatizada:**
  - `GlobalSearchShortcutsHarnessTest.java` implementado cobrindo 100% dos requisitos PGA-01 a PGA-05 (**5/5 testes verdes**).
  - Regressão: `ButtonIconContrastHarnessTest`, `ButtonContrastTest`, `IconSystemHarnessTest`, `FinalUiUniformityHarnessTest`, `PosErgonomicsHarnessTest`, `UiOrganizationNavigationHarnessTest`, `UiPanelDecompositionTest`, `MultiModuleArchitectureHarnessTest` e `mvn compile` todos **100% verdes (BUILD SUCCESS)**.

### Harmonização e Contraste de Ícones e Texto nos Botões — 2026-09-13 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/BOTOES_ICONES_CONTRASTE_SPEC.md` — Especificação técnica para cálculo de contraste, regras de sincronização dinâmica do `FontIcon` com a cor de texto do `ModernButton`, e paletas seguras para botões sólidos e contornados.
  - `docs/BOTOES_ICONES_CONTRASTE_HARNESS.md` — Critérios de conformidade automatizados (BIC-01 a BIC-05).
- **Sincronização Dinâmica em `ModernButton.java`:**
  - Sobrescrito `setIcon(Icon)` e `setForeground(Color)` para interceptar instâncias de `FontIcon` e sincronizar automaticamente sua cor interna (`setIconColor`) com o `textColor` / `readableTextOn(background)` do botão.
  - No método `setColors(...)`, o ícone agora acompanha as alterações dinâmicas de tema e estado ativo/inativo (ex.: botões de filtro e segmentação que alternam entre fundo escuro e fundo claro).
- **Cor Padrão Segura de Ícones em `UIHelper.java`:**
  - `UIHelper.icon(code, size)` e `fallbackIcon` restaurados para `Color.WHITE` por padrão, garantindo alto contraste imediato na vasta maioria de botões de ação com cores sólidas (`ACCENT_BLUE`, `APPROVED_GREEN`, `REJECTED_RED`, `BUTTON_NEUTRAL`, `SECONDARY`).
- **Validação Automatizada:**
  - Criado `ButtonIconContrastHarnessTest.java` cobrindo 100% dos requisitos BIC-01 a BIC-05 (**5/5 testes verdes**).
  - Regressão: `ButtonContrastTest` (4/4), `IconSystemHarnessTest` (6/6), `FinalUiUniformityHarnessTest` (4/4), `PosErgonomicsHarnessTest` (4/4), `UiPanelDecompositionTest` (1/1), `MultiModuleArchitectureHarnessTest` (6/6) e `mvn compile` no reactor Maven todos **100% verdes (BUILD SUCCESS)**.


- **Documentação Canónica:**
  - `docs/POS_MODERNIZACAO_ERGONOMIA_SPEC.md` — Especificação da geometria do checkout, densidade do catálogo, cabeçalho e atalhos operacionais.
  - `docs/POS_MODERNIZACAO_ERGONOMIA_HARNESS.md` — Critérios de conformidade automatizados (POS-01 a POS-06).
- **Recuperação de Espaço Vertical no Checkout (`POSPanel.java`):**
  - Removidos os 3 cartões grandes de resumo da visão de venda ativa, liberando ~95px de altura útil imediata para visualização de mais produtos e mais linhas na tabela de compras.
  - Atalho de leitura de código de barras `F3` adicionado ao `InputMap` com indicador explícito no campo.
- **Alta Densidade no Catálogo de Produtos (`PosCatalogController.java`):**
  - Otimizada a geometria dos cartões de produto (`CARD_IMAGE_WIDTH = 80`, `CARD_IMAGE_HEIGHT = 44`, padding e gaps compactos), permitindo exibir **6 a 8 produtos por página** em simultâneo sem rolagem forçada.
- **Validação Automatizada:**
  - `PosErgonomicsHarnessTest.java` (4/4 verde), `PosCatalogAvailabilityTest`, `PosButtonColourHierarchyTest`, `PosLayoutTest`, `POSKeyboardShortcutTest`, `UiPanelDecompositionTest` (POSPanel em 995 linhas, <1000) e `MultiModuleArchitectureHarnessTest` todos **verdes**.

### Modernização e Padronização do Sistema de Ícones — 2026-09-13 — **implementado com SPEC e HARNESS**

- **Documentação Canónica:**
  - `docs/SISTEMA_ICONES_SPEC.md` — Especificação técnica dos tokens de dimensão, convenções cromáticas, acessibilidade e fallback de segurança.
  - `docs/SISTEMA_ICONES_HARNESS.md` — Critérios de conformidade automatizados (IC-01 a IC-08).
- **Resiliência e Proteção Contra Falhas (`UIHelper.java`):**
  - Implementado fallback gracioso no carregamento de ícones (`fallbackIcon`), garantindo que códigos nulos, vazios ou incorretos não quebrem a aplicação com `IllegalArgumentException`.
  - A sobrecarga padrão `UIHelper.icon(code, size)` passa a assumir a cor de texto do tema ativo (`UIHelper.TEXT_LIGHT`), evitando ícones brancos invisíveis no Modo Claro.
- **Tokens Canónicos de Dimensão:**
  - Introduzidas as constantes semânticas `ICON_XS (12)`, `ICON_SM (14)`, `ICON_MD (16)`, `ICON_LG (20)`, `ICON_XL (24)` e `ICON_HERO (48)`.
- **Ícones Compostos com Crachá (`BadgedIcon.java`):**
  - Criado o componente canónico `BadgedIcon` e o utilitário `UIHelper.badgedIcon(...)` para sobrepor contadores numéricos e indicadores de estado em tempo real.
- **Acessibilidade para Botões Icon-Only:**
  - Criado o helper `UIHelper.createIconButton(...)` com atribuição obrigatória de `toolTipText` e `AccessibleName`.
- **Validação Automatizada:**
  - `IconSystemHarnessTest.java` implementado cobrindo 100% dos critérios IC-01 a IC-06 (**6/6 testes verdes**).
  - Regressão: `UiOrganizationNavigationHarnessTest`, `FinalUiUniformityHarnessTest`, `UiPanelDecompositionTest`, `SimplePieChartTest` e `MultiModuleArchitectureHarnessTest` todos **verdes**.

### Otimização de Espaço no POS e Gráficos de Pizza no Painel Inicial — 2026-09-13 — **concluído**

- **Componente canónico criado:** `SimplePieChart.java` (gráfico de pizza clássico sólido em Java 2D vetorial anti-aliased com percentagens desenhadas diretamente no interior de cada fatia com cálculo de contraste automático, linhas divisórias nítidas, paleta de cores vibrante cyan/orange/blue/lime/slate e legendas completas) e testes em `SimplePieChartTest.java`.
- **Painel Inicial (`DashboardPanel.java`):**
  - Adicionado card KPI de POS: **VENDAS POS (HOJE)** com faturação e total de vendas do balcão em tempo real.
  - Integrados dois novos gráficos de pizza sólida em grelha 2x2 com os gráficos de barras:
    1. **Vendas por Canal (POS vs Faturas):** Comparativo entre volume de balcão POS e faturação comercial direta.
    2. **Estrutura Financeira (Receita, Compras, IVA):** Proporção entre receitas, compras a fornecedores e IVA.
- **Otimização do POS (`PosSalesHistoryPanel.java`):**
  - Substituída a barra volumosa de 3 cartões grandes por chips de resumo horizontais compactos (`createSummaryChip`), recuperando ~70-80px de altura vertical para a tabela de vendas.
- **Validação:** `SimplePieChartTest` (2/2), `UiPanelDecompositionTest`, `FinalUiUniformityHarnessTest`, `UiOrganizationNavigationHarnessTest` e `MultiModuleArchitectureHarnessTest` todos **verdes**.

### Simplificação do Cabeçalho Superior (Remoção do Menu Duplicado) — 2026-09-13 — **concluído**

- **Contexto:** Com a introdução da `CollapsibleSidebar` com 4 secções temáticas e navegação categorizada, os botões de módulos repetidos na barra superior tornaram-se redundantes e ocupavam espaço vertical.
- **Alterações:**
  - Removida a linha de ícones de navegação de módulos de `TopNavBar` e `MainFrame.buildTopBar()`.
  - O cabeçalho superior (`TopNavBar`) passou a atuar como um cabeçalho limpo e contextual:
    - **Esquerda:** Exibe dinamicamente o título do módulo ativo em foco (ex.: "Painel Inicial", "POS — Caixa", "Stock & Armazéns", etc.).
    - **Direita:** Utilitários essenciais preservados (Alternador de tema Claro/Escuro, Sino de Notificações com contador e prévia, Chip de Subscrição PRO, Seletor de Empresa para multi-tenant e Chip de Perfil de Utilizador/Sessão).
  - Validação automatizada: `UiOrganizationNavigationHarnessTest`, `FinalUiUniformityHarnessTest` e `TopNavItemTest` executados com **100% de sucesso (BUILD SUCCESS)**.

### Correcção de Inicialização do Desktop (ActionMenuButton no HRPanel) — 2026-09-13 — **correcção crítica de arranque**

- **Causa raiz:** O separador de Recibos de Salário (`HRPanel.buildPayslipsTab()`) adicionava 6 acções ao menu `Documentos`. Como `ActionMenuButton` valida estritamente um máximo de 5 acções (`MAX_ACTIONS = 5`), era lançada uma `IllegalStateException: O menu de acções não pode ter mais de cinco opções.` durante a instanciação do `MainFrame`, terminando a aplicação imediatamente após o login.
- **Resolução:**
  - Separado o menu em dois agrupamentos temáticos: `Documentos` (Imprimir PDF, Exportar Lista, Ficheiro de Pagamento) e `Mais acções` (13.º Mês, Fechar Mês, Reabrir Mês), ambos com <= 3 itens respeitando a restrição do componente.
  - Mantida a guarda de tamanho do `HRPanel.java` abaixo do limite de 1000 linhas (999 linhas) em conformidade com `UiPanelDecompositionTest`.
  - Validação automatizada: `UiPanelDecompositionTest`, `HRSubsidiesUiHarnessTest` e `MultiModuleArchitectureHarnessTest` todos com **BUILD SUCCESS**.

### UI e Organização da Navegação (Login & Sidebar) — 2026-09-12 — **implementado com SPEC e HARNESS**

- Modernização completa da autenticação com `LoginDialog`:
  - Detetor dinâmico de Caps Lock ativado em tempo real para prevenção de erros de entrada.
  - Alternador de visibilidade de senha (mostrar/ocultar senha com feedback sonoro e acessibilidade).
  - Chips de contas de demonstração rápidas (Maria - Gestão, João - Caixa/Vendas, Ana - RH) com preenchimento instantâneo e foco no botão de entrada.
  - Contentor rolável responsivo (`JScrollPane` com viewport transparente) para ecrãs menores ou resoluções compactas.
  - Adaptação coerente aos temas Claro e Escuro sem recorrer a literais de cores fora do `UIHelper`.
- Estruturação modular da navegação com `CollapsibleSidebar` e `SidebarNavItem`:
  - Organização semântica em quatro secções temáticas: Operações, Gestão & CRM, Fiscal & Auditoria e Sistema.
  - Transição fluida entre modo expandido (240px) e modo recolhido/rail (64px) com tooltips contextuais.
  - Atalho de teclado global `Ctrl+B` integrado ao `MainFrame` para alternar o estado do menu lateral.
  - Suporte a badges numéricos dinâmicos para notificações e pendências.
  - Acessibilidade completa com `AccessibleContext` e navegação por teclado (Enter/Espaço).
- Respeito integral às regras arquiteturais e de qualidade:
  - Extraído `PosTodaySummaryView` para isolar os cartões de resumo diário do POS, reduzindo `POSPanel.java` de 1066 para 997 linhas (respeitando o teto de 1000 linhas).
  - Eliminadas instâncias locais de `new Color(...)` no `POSPanel` e `PosSalesHistoryPanel`.
  - Documentação canónica: `docs/UI_ORGANIZACAO_NAVEGACAO_SPEC.md` e `docs/UI_ORGANIZACAO_NAVEGACAO_HARNESS.md`.
  - Validação automatizada: `UiOrganizationNavigationHarnessTest` (6/6 verde), `FinalUiUniformityHarnessTest` + `UiPanelDecompositionTest` (5/5 verde), `MultiModuleArchitectureHarnessTest` (6/6 verde) e `mvn clean compile` no reactor com **BUILD SUCCESS**.

### POS historico — 2026-09-09 — **filtro de data no servidor**

- O filtro de data do Historico de Vendas POS existia visualmente, mas actuava apenas sobre a pagina
  carregada. Como a tabela e paginada pelo servidor, isso podia esconder vendas de outras paginas.
- `GET /api/comercial/pos-sales/page` aceita agora `from` e `to` opcionais e filtra por
  `createdAt` antes da paginacao.
- `ComercialApiClient` envia o intervalo e `PosSalesHistoryPanel` recarrega a primeira pagina ao
  mudar o periodo: Todo o periodo, Hoje, Ultimos 7 dias, Ultimos 30 dias e Este mes.
- O painel ganhou cards de variacao no topo: vendas do periodo, total POS do periodo e variacao de
  receita contra o periodo anterior equivalente. O resumo vem de `/api/comercial/pos-sales/summary`.
- O painel principal do POS tambem ganhou cards de hoje: vendas hoje, total hoje e variacao contra
  ontem, actualizados ao abrir o painel, ao clicar em actualizar e depois de finalizar uma venda.
- Validacao focada: `ComercialControllerIntegrationTest` verde com contrato `from/to`; backend e
  desktop compilaram no reactor dirigido. `MultiModuleArchitectureHarnessTest` verde por causa do
  novo DTO partilhado `POSSalesSummaryDTO`.

### POS demo — 2026-09-08 — **dados vendaveis corrigidos**

- O backend estava UP, mas a base H2 local tinha seed invalida para o POS: os produtos eram
  partilhados entre Portugal e Mocambique, enquanto o stock ficava repartido entre armazens de
  empresas diferentes. Na empresa da Maria havia 7 produtos visiveis e apenas 4 vendaveis.
- `DataLoader` passou a semear stock positivo, em armazem de venda, para todos os produtos fisicos
  partilhados em cada empresa de demonstracao.
- Validacao focada: `ComercialControllerIntegrationTest`, `InventoryServiceTest` e
  `MoneyFlowHttpIntegrationTest` verdes (**10 testes, zero falhas/erros**).
- Backend reiniciado em `http://localhost:8080` com PID `15416`; health **UP**. Verificacao ao vivo:
  7 produtos totais, 7 vendaveis, catalogo POS "Todos" = 7, "Disponiveis" = 7, indisponiveis = 0.

### Recuperação de backup em PostgreSQL isolado — 2026-09-07 — **prova técnica concluída**

- `scripts/verify-backup-restore.ps1` cria um cluster temporário em loopback, com duas bases novas,
  e encerra exclusivamente esse cluster no fim. A instância habitual não é usada.
- `DatabaseBackupRoundTripTest` aplica as migrações reais, arranca o backend com Hibernate
  `validate`, gera e restaura o dump pelo `DatabaseBackupService` e compara todos os dados.
- Execução em PostgreSQL 18.2: **87 tabelas** (inclui a tabela de prova), **81 sequências** e
  **923 registos de estrutura de constraints** iguais. Bytes, decimais, FK e próximo ID confirmados.
  Backend restaurado arrancou e o login HTTP devolveu **200**, com token e acesso a empresa.
- Testes dirigidos: **13 testes, zero falhas/erros**. `mvn clean compile`: **BUILD SUCCESS** nos
  três módulos. Sem o guião, o teste PostgreSQL é opt-in.
- Evidência local: `data/restore-validation/77a30d84219c409798312d55b25721f7/evidence.txt`;
  dump e logs preservados nesse directório (ignorado pelo Git). Só dados de demonstração.
- Mantêm-se a observação dos fluxos no desktop, a impressão/periféricos físicos e o ensaio com
  uma cópia autorizada dos dados de produção. BR-50/BR-54 visuais não foram declarados concluídos.
- Contabilidade: fecho de exercício, pagamentos mistos e mapeamentos de RH dependem das decisões
  contabilísticas já indicadas. Assinatura do instalador depende do certificado; homologação
  jurídica e parâmetros legais continuam a depender dos responsáveis.

### Compras ligadas à contabilidade e tesouraria tipada — 2026-09-04 — **implementado**

- `PurchaseRegisteredEvent` lança D Mercadorias 3201, D IVA dedutível 2432 e C Fornecedores 2201;
  pagamento no acto liquida 2201 contra Caixa/Banco no mesmo lançamento.
- Pagamentos posteriores publicam `SupplierPaymentRegisteredEvent`, fonte contabilística própria
  `SUPPLIER_PAYMENT` e chave idempotente pelo movimento de tesouraria.
- Contas de tesouraria passaram a declarar `TreasuryAccountType` (`CASH`/`BANK`) no contrato, JPA,
  DTO e ecrã. A V61 migra contas antigas: com número bancário → BANK; sem número → CASH.
- Spec/harness actualizados em `docs/CONTABILIDADE_SPEC.md` e
  `docs/COMPRAS_CONTABILIDADE_HARNESS.md` (CT-30..CT-33).
- Verificação limpa após notas comerciais: `mvn -q clean test` → **996 testes, 0 falhas, 0 erros**; inclui o
  `MultiModuleArchitectureHarnessTest`. `HRPanel` ficou exactamente no limite de 1000 linhas.
- Notas comerciais aprovadas também chegaram ao razão: NC estorna Vendas/IVA/Clientes e, numa
  devolução, Mercadorias/CMVMC pelo custo histórico; ND lança Clientes contra Outros proveitos
  operacionais 7501 e IVA. CT-34/CT-35 cobrem as partidas.

### Subsídios legais e instalador Windows — 2026-09-03 — **implementado**

- UI de RH para pré-visualizar e pagar 13.º mês e subsídio de férias via API, fora do EDT, com
  confirmação e feedback Multicore. Spec/harness: `RH_SUBSIDIOS_UI_*`.
- Instalador por utilizador, configuração persistente da API em `%LOCALAPPDATA%` e harnesses.
  Artefacto: `dist/Multicore-1.0.0.exe`; assinatura continua dependente de certificado externo.

### Feedback profissional e interacções não bloqueantes — 2026-08-31 — **infraestrutura implementada**

- Criados `ToastManager`, `InlineFeedbackPanel`, `ModernMessageDialog` e `FeedbackType`: sucessos
  breves deixam de exigir clique; falhas recuperáveis permanecem no contexto; confirmações críticas
  têm aparência, teclado e contenção Multicore.
- `ModernFormDialog` apresenta erros de validação/gravação acima dos campos, mantém o modal aberto e
  deixou de abrir `JOptionPane` nesses casos.
- Aprovações é o fluxo de referência: falha de carregamento oferece `Tentar novamente`; aprovar e
  rejeitar confirmam por toast sem roubar foco.
- SPEC/harness: `docs/UI_FEEDBACK_PROFISSIONAL_SPEC.md` e
  `docs/UI_FEEDBACK_PROFISSIONAL_HARNESS.md`; `ProfessionalFeedbackHarnessTest` cobre banner, retry,
  toast, corpo de confirmação e adopção estática.
- Verificação: compilação do reactor até `desktop` verde; harness focado verde. FP-20..27 continuam
  a exigir validação visual no Windows real, temas claro/escuro e escalas 100/125/150%.
- **Fase 2:** Comercial e POS ficaram com zero `JOptionPane`; Stock e Compras ganharam banners de
  carregamento/retry e toasts nas operações concluídas. O inventário global caiu de 426 para 372 e
  ficou protegido por teste monotónico; confirmações de impressão no POS usam o diálogo Multicore.
- **Fase 3:** Stock e Compras também ficaram com zero `JOptionPane`. A contagem de stock grava pelo
  `ModernFormDialog` assíncrono e mantém erros no formulário; quantidade, preço, data e IVA da compra
  marcam o campo inválido. Inventário global actualizado para 365 e gate alargado aos quatro painéis.
- **Fase 4:** encomendas a fornecedor, transferências e catálogo/lotes de stock migraram
  pré-condições, erros e sucessos para banner/toast; recepção total usa confirmação Multicore.
  Inventário global caiu para 337. Permanecem apenas confirmações críticas, o editor composto de
  recepção parcial e validações de formulários ainda não convertidos integralmente.
- **Fase 5 (2026-09-01):** Configurações ficou com zero `JOptionPane`; Plataforma ficou com dois,
  ambos compostos (histórico de pagamentos e conversa de assistência). Confirmações de activação e
  suspensão usam diálogo Multicore. Inventário global caiu para 299 e o harness protege os limites.
- **Fase 6 (2026-09-01):** RH e CRM adoptaram banner contextual e toast nos painéis e nas classes
  de acções. Restam apenas confirmações, vistas compostas ou recolhas de texto justificadas:
  RHPanel ≤4, HREmployeeActions ≤2, CRMPanel =0 e acções CRM ≤3. O inventário global caiu
  para 244 e FP-20/FP-21 impedem regressões.
- **Fase 7 (2026-09-01):** leitura de códigos do POS ficou sem `JOptionPane`; caixa POS, inventário
  físico e catálogo/lotes migraram todo o feedback simples para banner/toast. Restam somente
  decisões compostas (POS ≤2, contagem ≤1, produtos ≤2). O inventário global caiu para 207 e
  FP-22..FP-24 protegem estes limites.
- **Fase 8 em curso (2026-09-01):** cotações, guias, notas comerciais, recibos, clientes, fiscal e
  devoluções POS migrados. Notas e recibos ficaram a zero; os restantes conservam somente decisões
  ou vistas compostas. Inventário intermédio: 155, protegido por FP-25/FP-26.
  Segundo bloco: armazéns, contas a pagar e promoções ficaram a zero; subpainéis de RH migraram
  todo o feedback simples. Inventário intermédio actualizado para 111, com FP-27/FP-28.
  Terceiro bloco: controladores auxiliares de POS/comercial/stock/compras, Tesouraria e Notificações
  ficaram a zero; fulfilment conserva duas vistas/autorizações compostas. Inventário: 85, com
  FP-29/FP-30.
  Quarto bloco: diálogos comerciais migrados e infraestrutura de impressão/exportação ficou a
  zero, com erros dentro da pré-visualização. Inventário global: 58, protegido por FP-31/FP-32.
  Fecho da fase: Contabilidade, movimentos, suporte e feedback de transferências migrados; motivos
  obrigatórios e validade usam formulários canónicos. Restam 45 chamadas revistas, exclusivamente
  modais justificadas, protegidas por allowlist FP-33. Fase 8 concluída.
- **Fase 9 concluída (2026-09-01):** inventário classificado em confirmações,
  vistas/mensagens críticas e recolhas opcionais; allowlist exacto activo. A validação humana foi
  separada como M-01..M-08 para não colidir com os IDs automáticos FP-01..FP-33.
  `mvn clean compile`, harness FP-01..FP-33 e 174 testes do desktop concluídos sem falhas.

### Listagens exportadas passam a dizer quem as emitiu — 2026-08-30 — **implementado**

- **A última folha sem cabeçalho.** O *Exportar PDF* das listagens (Clientes, Faturas, Encomendas,
  Lotes & Validades, e os separadores do RH) era desenhado **pelo desktop**, por uma cópia própria
  do `TablePdfExporter`: saía título, tabela, e mais nada — sem nome de empresa, sem NUIT, sem
  morada. A `DADOS_EMPRESA_DOCUMENTOS_SPEC` manda identificar quem emite em *todos* os documentos
  imprimíveis, e este era o que faltava. Duas cópias do mesmo desenho também divergem: corrigir o
  cabeçalho num sítio deixava o outro por corrigir.
- **A listagem sobe, o PDF desce.** `POST /api/print/table` → `TableExportPrintService`, com o
  mesmo `CompanyHeaderRenderer` da factura e da guia. Contrato novo em `contracts`
  (`TableExportRequest`), cliente novo no desktop (`PrintApiClient`), e um só sítio no ecrã
  (`TableExportAction`) — nasce deitado, porque listagens são largas.
- **Leva o que o operador filtrou, não a página que está a ver.**
  `ClientTablePagination.filteredModelRows` respeita o filtro e ignora a paginação; as células
  sobem já formatadas, porque é isso que ele leu. Tectos de 20 000 linhas e 40 colunas, recusados
  com a razão.
- **O desktop deixou de compor PDF.** A cópia foi apagada e o OpenPDF passou a `scope=test` no
  `desktop/pom.xml` — o cliente **lê e imprime** (PDFBox), não desenha.
  `MultiModuleArchitectureHarnessTest.desktopDoesNotDrawPdfDocuments` impede o regresso.
- **Verificação:** `mvn -o test` → **971 testes, 0 falhas, 0 erros** (backend 807, desktop 164).
  Novos: `TableExportPrintServiceTest` (6, lê o texto do PDF), `TableExportFilterTest` (6),
  IM-26 e a regra arquitectural. O IM-02 do harness de impressão passou a aceitar a delegação no
  `TableExportAction` — que também abre o modal, e o CE-05 prende isso.
- **Por confirmar na loja, e não é da IA:** a exportação nunca foi corrida contra um backend a
  correr — o PDF de uma listagem real, com o logótipo da empresa, ainda não saiu no papel.

### Modal de impressão antes de cada documento — 2026-08-30 — **implementado**

- **Nenhum documento sai para o papel sem passar por um modal.** Os 28 pontos de impressão do
  desktop chamavam `PdfFileSaver.saveAndOpen` e entregavam o PDF ao leitor do sistema: sem escolha
  de impressora, sem cópias, sem posição, e sem ver o documento dentro do ERP. Sem leitor de PDF
  instalado, `Desktop.open` falhava **em silêncio** e o ecrã não dizia nada.
- **`PrintPreviewDialog`** — pré-visualização da folha à esquerda (navegação, zoom 0,6×–3×),
  opções à direita (impressora · cópias 1–99 · páginas *Todas/Actual/`1,3-5`* · orientação · ajuste
  · escala de cinzentos), resumo em tempo real e `Cancelar · Abrir no leitor · Guardar PDF ·
  **Imprimir**`. Cartões `ModernPanel`, cabeçalho premium, `SectionHeader`+`FormField`, ícones
  `UIHelper.icon`. `Esc` fecha, `Ctrl+P` imprime.
- **Seis classes, uma responsabilidade cada** (`gui/components/`): `PrintOptions` (validação),
  `PrintOptionsStore` (memória por família — a etiqueta lembra a térmica, a factura a laser),
  `PdfPreviewDocument` (render+cache), `PaperLayout` (geometria), `PdfPrinter` (fila),
  `PrintPreviewDialog` (só composição). Cinco testam-se sem abrir janela.
- **Retrato/paisagem viram a folha do próprio documento**, não a forçam a A4: um recibo térmico de
  80 mm continua estreito. No spool, cada página leva o seu `PageFormat` dentro de um `Book`.
- **Dependência nova, só no desktop:** `pdfbox 3.0.6`. O OpenPDF gera PDF mas não o desenha nem o
  imprime. O backend não foi tocado.
- **A fotografia do diálogo apanhou dois defeitos antes da loja.** O cartão de opções cortava
  *Ajuste* e *Escala de cinzentos* fora do ecrã. E, pior, a pré-visualização rodava a página em
  paisagem mas o `PDFPrintable` **não roda nada** — sairia um documento pequeno ao meio da folha,
  diferente do que se via. O `PdfPrinter` passou a rodar a página (`/Rotate += 90`) com a **mesma**
  regra da pré-visualização, para que não possam divergir.
- SPEC/harness: [docs/IMPRESSAO_MODAL_SPEC.md](../docs/IMPRESSAO_MODAL_SPEC.md) +
  [docs/IMPRESSAO_MODAL_HARNESS.md](../docs/IMPRESSAO_MODAL_HARNESS.md); **25 testes** em
  `PrintModalHarnessTest` (19), `PrintPreviewDialogPaintTest` (3) e `PdfPrinterOrientationTest` (3).
  IM-01 falha se um ecrã voltar a gravar o PDF directamente.
- **Verificação:** `mvn -o test` → **951 testes, 0 falhas, 0 erros** (backend 800, desktop 151);
  harness arquitectural 5/5 verde. Instalador: o jar do desktop leva pdfbox/fontbox/pdfbox-io.
- **Por confirmar na loja, e não é da IA:** o modal foi validado com impressoras do sistema, mas
  **o papel a sair de uma térmica de 80 mm não foi visto** (MI-08/MI-15 do harness).

### Contrato no instalador e primeiro acesso — 2026-08-27 — **implementado**

- Instalador `jpackage` recebe a mesma licença vigente do backend e exige aceitação antes de instalar.
- Após login/empresa, `LicenseAcceptanceDialog` bloqueia a janela principal até o backend confirmar
  aceitação; chamadas HTTP correm fora do EDT.
- V60 conserva empresa, utilizador, versão, SHA-256, declaração, instante, IP, versão do desktop e
  user-agent; só MANAGER/ADMIN pode representar a empresa e a operação entra na auditoria.
- SPEC/harness: `docs/LICENCA_UTILIZADOR_SPEC.md`; suite completa **894/894 testes verdes**
  (backend 780, desktop 114), incluindo os novos harnesses.
- Instalador validado em `dist/Multicore-1.0.0.exe` (73,49 MB), gerado com WiX portátil e
  `jpackage`; SHA-256 `956CBC6FDA4029AA0CB1E90FDB62C7A0438269A0650EC5C20DFB6A3802D38A71`.
- O texto `EULA-1.0` é uma minuta técnica e continua a exigir revisão por advogado moçambicano.
- Hotfix desktop `1.0.1`: incluído `jackson-datatype-jsr310` para interpretar `Instant` na resposta
  de login do backend instalado; teste de regressão `DesktopApiClientTest` verde e instalador gerado
  em `dist/Multicore-1.0.1.exe`.

### Separação profissional backend/desktop — 2026-08-27 — **concluída no código**

- Reactor Maven físico criado: `contracts`, `backend`, `desktop`; fluxo obrigatório `desktop → contracts ← backend`.
- Desktop sem JPA, Flyway, H2, PostgreSQL, Repository ou Service; backend sem Swing/GUI/desktop.
- SPEC canónico: `docs/MULTI_MODULE_ARCHITECTURE_SPEC.md`. Harness permanente:
  `MultiModuleArchitectureHarnessTest` (4 regras, todas verdes).
- Backend e desktop compilam isoladamente; os 124 testes existentes foram repartidos e compilam nos módulos certos.
- Docker compila e publica apenas `multicore-backend`; o desktop permanece aplicação instalada e aponta para a API por `DESKTOP_API_BASE_URL`.
- Verificação: backend **774/774**, desktop **112/112**, total **886 testes verdes**; harness arquitectural 4/4 verde.

> Ponteiro da sessão. A IA lê-o no início e actualiza-o sempre que uma fase fecha. ≤1 página. Histórico no `git log`.

### Estado a 2026-08-29 — RH visto ao vivo, ecrã a ecrã

- **Os 11 separadores do RH e os 4 diálogos por colaborador foram abertos e fotografados** contra um
  backend real. Nenhum falhou a pintar. Seis defeitos de apresentação encontrados e corrigidos.
- **Um deles não era do RH:** `styleComboBox` congelava a largura antes de instalar o renderer, e
  todas as tabelas paginadas do sistema mostravam `Por página: …` e `Todos os esta…`. Corrigido com
  uma linha mudada de sítio, mais uma guarda que falha com `155 < 171` contra o código antigo.
- **Tipos de falta chegavam ao ecrã em inglês** (`PENDING_JUSTIFIC…`). Traduzidos no
  `UIHelper.humanStatus`. Ao humanizar a coluna parti o filtro por tipo — o `TableFilter` compara
  texto exacto com o modelo — e corrigi o dropdown, que aliás mostrava enums ingleses ao operador.
- **Anexos dos exames**: cifrados em repouso (AES-256-GCM, `ATTACHMENT_KEY`) e, pela primeira vez,
  legíveis — eram gravados e nunca mais saíam. Abrir exige gestor/admin e fica auditado.
- **Por decidir, e não é da IA:** valores legais com o contabilista; carta ao MITESS pronta a enviar
  em [docs/PEDIDO_MITESS_EXAMES_MEDICOS.md](../docs/PEDIDO_MITESS_EXAMES_MEDICOS.md); gerar e
  guardar a `ATTACHMENT_KEY`.

### Clínicas, custo dos exames e conformidade legal — 2026-08-25 — **V59**

Spec/harness: [docs/CONFORMIDADE_LEGAL_MZ_SPEC.md](../docs/CONFORMIDADE_LEGAL_MZ_SPEC.md) +
[docs/CONFORMIDADE_LEGAL_MZ_HARNESS.md](../docs/CONFORMIDADE_LEGAL_MZ_HARNESS.md).

- **A clínica passou a ser fornecedor.** `provider_id → suppliers(id)`, porque é lá que a factura
  vive com NUIT e histórico. O texto livre `clinic` fica para as não cadastradas e para os registos
  anteriores à V59. O exame ganhou `cost`, `invoice_number` e `paid_at`: o encargo com saúde
  ocupacional — que é do empregador — **não existia em número nenhum**, saía da tesouraria
  misturado com tudo o resto. Paga-se pela mesma porta do recibo, pagar duas vezes é recusado, e
  há relatório por prestador (total / por pagar). **Não lança na contabilidade** — mesma fronteira
  declarada no §B5 para adiantamentos.
- **Três defeitos de protecção de dados, fechados.** (1) `summary()` **não tinha guarda nenhuma**:
  qualquer conta autenticada lia a aptidão de qualquer colega trocando o número no endereço — agora
  é do gestor e do próprio. (2) O campo de observações era um campo de diagnóstico à espera de
  acontecer; texto com estado serológico passa a ser recusado (Lei n.º 19/2014 e Lei n.º 13/2023
  proíbem apurar HIV/SIDA, e o médico só pode comunicar aptidão). (3) **Ler** dados de saúde não
  deixava rasto — `history()` grava `OCCUPATIONAL_HEALTH_ACCESS` e por isso deixou de ser `readOnly`.
- **Quem nunca fez exame passou a aparecer.** A lista de alertas partia da validade, logo só via os
  cumpridores: quem nunca fez exame não tem validade a caducar. `missingExams()` + linha no sino.
  Mesma lição das obrigações sem prazo do §B5.
- **Verificação:** `mvn -o test` → **878 testes, 0 falhas, 0 erros, 0 ignorados** (eram 865).
  **V59 aplicada contra PostgreSQL real** (cluster descartável na 55433): 58 migrações, schema v59,
  arranque com `ddl-auto=validate`, FK para `suppliers` e 2 índices confirmados no schema. Cluster
  destruído; o PostgreSQL do utilizador (5432) nunca foi tocado.

**Por fazer, e declarado no harness:**

1. **A homologação jurídica está por assinar** — §4 do harness, 12 linhas (H-01..H-12). Enquanto
   estiver vazia, o sistema **não pode ser apresentado como legalmente homologado**, só como
   preparado para o ser. A periodicidade dos exames e o prazo de conservação dos dados de saúde
   estão num **diploma ministerial conjunto (Trabalho + Saúde) que não consegui identificar** — sem
   ele o sistema não inventa nenhum dos dois.
2. **Moçambique não tem lei de protecção de dados em vigor.** A proposta foi aprovada em Conselho
   de Ministros em Março/2026 e aguarda votação. O desenho segue os princípios da proposta, mas
   nada aqui pode ser dito "conforme à lei de protecção de dados" — ela ainda não existe.
3. **Anexos dos exames guardados em claro** (`bytea`), backups incluídos. Declarado, não escondido.
4. **Nada validado ao vivo pela UI** — os ecrãs novos (prestador/custo no formulário, colunas de
   pagamento, *Registar Pagamento*, separadores *Sem exame* e *Custos do ano*) nunca foram abertos.
   Junta-se aos RHC-90..94.

### Inspector DRY de detalhes em todo o sistema — 2026-08-23

- O modal compacto **Detalhes do Registo** passou de implementação dentro de `UIHelper` para o
  componente canónico `RecordDetailsDialog`; etiquetas, valores copiáveis, texto longo, colunas
  escondidas, scroll e botão Fechar têm agora uma única implementação.
- `RowDetailsInspector` instala, de forma idempotente, as três portas globais: duplo clique,
  tecla **Enter** e chamada programática. `TableContextMenu` ganhou **Ver detalhes** no botão direito.
- Cobertura transversal confirmada: existem 74 construções de `JTable` na UI e 74 chamadas a
  `UIHelper.styleTable`. Tabelas transaccionais/decisórias podem manter modal próprio por
  `noRowInspector`, sem duplicar o inspector genérico.
- Verificação: `RowDetailsInspectorTest` + `UiPanelDecompositionTest` verdes; desktop reiniciado com
  a versão nova.

### Perfil do trabalhador no RH — 2026-08-23

- A aba **Colaboradores** ganhou a acção **Ver Perfil**; a selecção abre uma ficha consolidada,
  só-leitura, em `HREmployeeProfileDialog`.
- O perfil mostra identidade e estado, dados pessoais, vínculo e antiguidade, NUIT/INSS/salário,
  conta ligada/self-service e separadores de histórico para recibos, faltas e férias.
- A ficha usa apenas os DTOs já carregados pelo `HRApiClient`; não introduz acesso da UI a Service,
  Repository ou base de dados.
- Verificação: `mvn -q -DskipTests compile` e `UiPanelDecompositionTest` verdes; validado ao vivo no
  Windows com João Silva, sem cortes no modal e com a aplicação reiniciada na versão nova.

### Fecho técnico do RH — 2026-08-23

- A árvore final do RH (B1–B8) voltou a compilar. `HRService` passou a importar a regra declarada
  `AbsencePayRule` e a obter o direito anual de férias do `HrPolicyService`, por antiguidade e
  vigência, mantendo 22 dias apenas como compatibilidade para empresas ainda sem configuração.
- Testes antigos foram alinhados com as regras novas: faltas não remuneradas explícitas, dias úteis
  no pedido de férias e recibo `DRAFT → APPROVED → PAID`.
- Verificação final: `mvn -q clean test` → **830 testes, 0 falhas, 0 erros, 0 ignorados**.
- **RH tecnicamente concluído no código e no harness automático.** Antes de produção continuam
  obrigatórios: aplicar V48–V56 numa BD PostgreSQL descartável/real; confirmar com o contabilista
  multiplicadores de horas extra, direitos/prazos e regras de compensação; executar RHC-90..94 no
  desktop Windows (incluindo impressão e resolução 1382×736). Estes pontos dependem do ambiente e
  de decisão humana e não podem ser marcados como validados pela suite H2/headless.

### RH completo — 2026-08-24 — **B1..B8 fechados; o módulo deixou de ser folha de salários**

Spec/harness: [docs/RH_COMPLETO_SPEC.md](../docs/RH_COMPLETO_SPEC.md) +
[docs/RH_COMPLETO_HARNESS.md](../docs/RH_COMPLETO_HARNESS.md). Migrações **V53–V56** nesta sessão.

**O que faltava era o dinheiro e a saída.** Os blocos B5, B6, B3 e B8 fecharam por esta ordem —
por dano, não por facilidade.

- **B5 — retenções por entregar (V53).** O IRPS retido e o INSS das duas partes eram calculados,
  impressos no mapa fiscal e **nunca mais tocados**: ficavam na conta da empresa indistinguíveis de
  dinheiro próprio. Quem os gasta não descobre o buraco no mês em que o gasta — descobre no dia da
  entrega. Passam a nascer como dívida no acto do pagamento, com prazo (quando configurado), aviso
  no sino e entrega com saída de tesouraria. **A folha passou também a chegar ao razão**, por
  evento (`PayslipPaidEvent`), fechando a lacuna declarada na `CONTABILIDADE_SPEC §7`.
- **B6 — descontos, adiantamentos e empréstimos (V54).** Um adiantamento **saía da caixa e nunca
  voltava**, e o recibo mostrava um `otherDeductions` anónimo. Uma tabela para os três casos (um
  empréstimo é um adiantamento em N prestações; um recorrente é um empréstimo sem capital), com o
  **saldo em dívida apurado das linhas**, nunca gravado — anular um recibo devolve as prestações à
  dívida sozinho.
- **B3 — cessação e acerto final (V55).** Substituiu uma String. O 13.º proporcional e o saldo de
  férias, que o sistema **já sabia calcular**, nunca eram calculados nesta situação. Cessar mostra
  a conta primeiro, porque é irreversível; e quando não sabe calcular uma linha — direito a férias
  ou aviso prévio por configurar — **di-lo por escrito** em vez de a omitir.
- **B8 — correcções (V56).** Férias em **dias úteis** (a mesma fonte que o ponto usa), direito
  anual configurável, `APPROVED` no recibo (quem processava a folha pagava-a sozinho), remuneração
  por tipo de falta **declarada** em vez de acidental, fecho do mês da folha, ficheiro de pagamento
  bancário e documentos do colaborador com validade.
- **UI:** três separadores novos (Descontos, Retenções, Cessações) e os diálogos que faltavam —
  evolução salarial com gráfico, documentos, acréscimos de hora extra, justificar falta, aprovar
  recibo, fechar/reabrir mês, ficheiro bancário. `HRPanel` ficou em **959/1000**; o resto saiu para
  `HRPayrollActions` e `HREmployeeActions`.
- **Guarda nova: `TabStripFitsTest` cobre agora o RH**, não só o Comercial. Três separadores de uma
  vez punham a barra do RH acima dos 1382 px validados — e um separador que não cabe **não avisa,
  desaparece** atrás das setas. Foi por causa da medição que "Notas de Despesas" passou a
  "Despesas".
- **Verificação:** `mvn -o test` → **860 testes, 0 falhas, 0 erros, 0 ignorados** (eram 781).
  169 são próprios de RH.
- **Migrações validadas contra PostgreSQL real** (cluster descartável na porta 55433, receita de
  21/08): **56 migrações aplicadas, schema v57**, aplicação a arrancar com `ddl-auto=validate` no
  perfil `prod`. Fecha a dívida das **V48–V52 que estavam por correr** desde 23/08, mais as V53–V57.
  Confirmadas no schema as 8 tabelas novas, as colunas nullable e os 4 índices únicos que carregam
  as invariantes. Cluster destruído no fim; o PostgreSQL do utilizador (5432) nunca foi tocado.

**Por fazer, e declarado no harness:**

1. **Nada foi validado ao vivo pela UI** — RHC-90..94. Os ecrãs novos nunca foram abertos.
2. **Adiantamentos, empréstimos e acertos finais movem tesouraria mas não lançam na contabilidade.**
   A folha e as retenções lançam. Mapear um crédito ao trabalhador a contas é decisão de plano, do
   contabilista.
3. **Valores legais e acréscimos de hora extra** continuam por confirmar com o contabilista. Sem
   eles o sistema recusa-se a valorizar horas extra e o acerto diz que usou o valor histórico.

⚠️ **Nota de honestidade:** ao parar a aplicação de teste, o filtro apanhou **quatro** processos
Java de Multicore/spring-boot, não só o meu — dois deles corriam desde as 03:34. Se havia um backend
de outra sessão de pé, foi parado. Nada se perdeu (é reiniciar), mas fica dito.

### Migrações — 2026-08-25 — **V1..V58 VALIDADAS CONTRA POSTGRESQL REAL** (arranque limpo *e* actualização)

Cluster descartável (PostgreSQL 18.2, `initdb` + `pg_ctl` em porta **55432**, auth trust), a partir de um
`git worktree` no HEAD para validar o estado **committado** e não apanhar ficheiros meio-escritos de
outra sessão. O servidor do utilizador (5432) nunca foi tocado; cluster e worktree destruídos no fim.

**1. Arranque de raiz** — base vazia, perfil `prod` (Flyway dono do schema, `ddl-auto=validate`):
- **57 migrações aplicadas, 0 falhadas, schema em v58** (2,2 s). 85 tabelas.
- **`Started MulticoreApplication`** — cada mapeamento de entidade bate com o schema que o Flyway
  construiu. É este passo que apanha a coluna que o Java tem e o SQL não (o bug do `varchar(20)`).
- O buraco no **V29 nunca existiu** — 58 versões menos uma dá as 57 aplicadas. Inofensivo, e agora
  provado em vez de presumido.

**2. Actualização sobre base povoada** — o percurso que uma loja faz de verdade, e o que faltava:
- Base levada só até à **V47** (46 migrações), povoada por SQL directo com empresa, 3 colaboradores
  (um `TERMINATED`), 2 recibos (`PAID` e `DRAFT`) e 2 utilizadores.
- Salto **V48→V58 por cima dos dados**: 11 migrações, **0 falhadas**, e a app arrancou com
  `ddl-auto=validate`.
- **Nada foi inventado nem reescrito:** as colunas novas (`app_user_id`, `bank_name`, `bank_account`,
  `photo`) ficaram a NULL nas linhas existentes, e os recibos antigos mantiveram o estado
  (`PAID` continua `PAID`) — a V56 não reescreveu história.
- **Porquê corre bem:** as únicas 4 alterações a tabelas existentes em toda a cadeia V48..V58 são
  `add column` **nullable**. Zero colunas `not null` sem default — que é o que parte uma base com dados.

**Nota de rigor:** `companies`, `employees`, `payslips` e `app_users` **não têm coluna `version`** (só
`stocks` tem). A lição de 21/08 sobre preencher `version` em seeds SQL aplica-se só às tabelas que a
têm — não é geral.

~~**Por validar ainda:** a **V59**~~ **V59 validada a 2026-08-26**, mesma receita, cadeia completa
**V1..V59**:
- **Arranque de raiz:** 58 migrações, 0 falhadas, schema em **v59**, e `Started MulticoreApplication`
  com `ddl-auto=validate`.
- **Actualização V58 → V59 com a tabela alvo POVOADA:** a V59 altera `occupational_health_exams`, pelo
  que a base foi levada à V58, semeada com **dois exames de aptidão** e só depois actualizada.
  Alterar tabela vazia não prova nada. 1 migração, 0 falhadas; os dois exames sobreviveram com os
  dados originais, as 4 colunas novas a NULL, e os índices `idx_occupational_health_provider` e
  `idx_occupational_health_unpaid` criados.
- **Porquê corre bem:** as 4 alterações da V59 são `add column` **nullable**, e não há `update` a
  reescrever dados existentes.

**Continua por validar:** o que entrar depois do `2a95394`.

### Verificação — 2026-08-23 — **suite completa verde, bloqueador do B7.1/B7.2 levantado**

- A árvore voltou a compilar: `CrmTicketActions` e `CrmWorkSheetActions` existem e as migrações
  deixaram de ter duas V46 (V46 reposição · V47 CRM · V48 ligação Employee↔AppUser).
- **`mvn -o clean test` → 713 testes, 0 falhas, 0 erros, 0 ignorados** (eram 705 no fecho do CRM;
  +8 do `HRServiceTest`, que passou de 13 a 21 no B7.2). 617 fontes principais compilam limpo.
- **Armadilha a evitar, custou duas corridas:** duas sessões a correr `mvn` sobre a **mesma** árvore
  partilham o `target/`, e o `clean` de uma apaga os `.class` que a outra está a ler. Deu um erro que
  parece defeito grave e não é — `QuotationLineDTO.class` *truncated at offset 0* (ficheiro de 0
  bytes) e, à segunda, `clean` incapaz de apagar `target/classes/db/migration`. **Um build de cada
  vez**; com dois em curso, nenhum resultado é de confiança.
- `lg.json` na raiz era lixo de teste ao vivo (token de sessão expirado a 22/08) — **apagado**.

**Trabalho posto no git a 2026-08-23** (estava tudo só na árvore de trabalho):

| Commit | O quê |
|---|---|
| `66e513f` | encomenda profissional — origem, condições, entrega prevista (V45 + `delivery_days` na V44) |
| `51abc6e` | CRM — ciclo de vida do pedido, folha de obra imprimível, 3 bugs de dinheiro, fuga entre tenants (V47) |
| `03a3a64` | reposição interna — acabamento desktop + materialização de lotes numa só porta |
| `ace17c8` | separadores do Comercial que se escondiam atrás das setas |
| `4660028` | cadastro de clientes sai do balcão do POS |

- **O commit `2471323` estava partido:** foi feito com quatro classes por adicionar ao git
  (`OrderTerms`, `OrderStatusLabel`, `CommercialTermsRenderer`, `SignatureBlockRenderer`) e com o
  `StockTransferService` a chamar um `materialiseLegacyIfNeeded` que não existia. **O HEAD não
  compilava desde 21/08.** O `66e513f` fecha a primeira metade e o `03a3a64` a segunda — a mensagem
  do `66e513f` diz que repara o build por inteiro, o que é impreciso. Confirmado a compilar cada
  commit em árvore isolada (`git worktree`), que é o único sítio onde isso se vê.
- **RH (B7.1/B7.2) ficou POR COMMITAR, deliberadamente.** Enquanto isto era commitado, outra sessão
  escreveu nesta mesma árvore um módulo de contratos de trabalho (V49, `EmploymentContract*`,
  `DocumentSeries`) **dentro dos mesmos ficheiros** — `HRService`, `HRPanel`, `HRServiceTest`,
  `HRController`, `HRApiClient`. Já não se separam sem cortar hunk a hunk trabalho alheio a meio, e
  nada disso está coberto pela corrida das 05:42. Decidir se B7.1/B7.2 e contratos vão num commit só.

### CRM & Assistência — 2026-08-22 — **fase fechada**

- **Pedido do utilizador:** "no crm e assistencia nao falta nada / como acoes de botoes".
- **O diagnóstico:** a aba "Pedidos de Assistência" não tinha **um único botão** — `POST
  /api/crm/tickets` existia no backend mas o `CRMApiClient` nem tinha o método, por isso não havia
  como abrir um pedido a partir da aplicação. E dos dois botões das Folhas de Obra, **"Faturar"
  rebentava sempre**.
- **Três bugs de dinheiro no `CRMService`, confirmados contra o backend a correr, não hipóteses:**
  1. `SERV-TEC` nascia com `stockTracked=true` (default do `Product`) → o FEFO recusava a saída de
     mão de obra com *"Stock insuficiente"* e **nenhuma folha era faturável**. Nasce sem stock, e
     produtos antigos são reparados ao faturar.
  2. `hoursWorked().intValue()` truncava as horas: 2,5 h saíam da factura como 2. `quantity` já era
     `BigDecimal` — a folha dizia 212,50 e a factura cobrava 190.
  3. `partsProduct.setUnitPrice(custo desta folha)` reescrevia o **catálogo partilhado** a cada
     facturação. Agora o preço unitário fica fixo em 1,00 e o valor viaja na quantidade.
- **Fuga entre tenants fechada:** o fallback `findBySku(...)` sem empresa ia buscar o produto de
  outro tenant e anexava-lhe a empresa actual. Só `findBySkuAndCompaniesId`.
- **Ciclo de vida do pedido** (`V47__crm_ticket_lifecycle.sql`): `status` deixou de ser `String`
  livre → enums `TicketStatus` (Aberto/Em curso/Resolvido/Anulado) e `TicketPriority`, mais
  `assignedTechnician`, `resolvedAt` e `closingNote`. Fecha-se um pedido **sem folha de obra**
  (resolvido ao telefone), anula-se com motivo obrigatório, reabre-se. Nomes das constantes
  escolhidos para coincidir com as strings antigas: sem conversão de dados.
- **Folha de obra** ganhou `hourlyRate` gravada (a tarifa era constante no código; agora é o preço
  do `SERV-TEC` no catálogo), correcção e **anulação com motivo** — anular reabre o pedido se não
  sobrar trabalho vivo. Faturada, fecha-se: só nota de crédito.
- **PDF novo:** `WorkSheetPrintService` + `/api/print/work-sheet/{id}`. Era o único documento do
  sistema sem impressão. Deliberadamente **não fiscal** — resumo próprio (Mão de obra / Peças /
  TOTAL sem IVA) em vez do `TotalsBlockRenderer`, porque imprimir "IVA 0,00" num papel que o
  cliente assina seria dizer-lhe que não paga imposto. Folha anulada sai carimbada.
- **Desktop:** `CRMPanel` decomposto em `CrmTicketActions` + `CrmWorkSheetActions` (molde do
  `StockTransferActions`). Botões novos: Novo Pedido, Abrir Pedido (assumir/resolver/anular/reabrir
  + atribuir técnico e prioridade), Actualizar, Corrigir, Anular, Imprimir PDF.
- **Verificação:** `CRMServiceTest` (19) + `WorkSheetPrintServiceTest` (5, lêem o **texto** do PDF);
  suite completa **705/705**. Ensaio ponta-a-ponta contra o backend: folha de 2,5 h + 100 MT →
  factura FT-2026/1 com base **212,50** e IVA 34,00 — o número da folha e o da factura batem certo
  pela primeira vez.

### Auditoria — 2026-08-22 (RH: o que falta para além de contratos e ponto) — **só documentação**

- **Pedido do utilizador:** "no RH falta contratos e ponto, o que mais para ser mais completo".
- Spec/harness novos: `docs/RH_COMPLETO_SPEC.md` + `docs/RH_COMPLETO_HARNESS.md` (RHC-01..76 auto,
  RHC-90..94 manuais). **Nada implementado** — é proposta e medição.
- **O diagnóstico:** o RH sabe pagar, não sabe empregar. Falta o que vem **antes** do recibo
  (contrato, ponto), o que vem **depois** (cessação e acerto final) e o **dinheiro que fica por
  entregar** — IRPS e INSS são calculados e impressos no mapa fiscal e nunca mais tocados: não há
  obrigação registada, não há saída de tesouraria, e os salários continuam sem lançamento
  contabilístico (já declarado em `CONTABILIDADE_SPEC §7`).
- **Defeitos confirmados no código, não hipóteses** (marcados 🔴 no harness): `recordAbsence`,
  `deleteAbsence`, `submitVacation` e `submitExpense` **não têm guarda de perfil nem auditoria** e
  recebem o `employeeId` no corpo — qualquer EMPLOYEE lança/apaga faltas e submete despesas em nome
  de um colega; `contractEndDate` não trava a folha mensal (paga-se a quem já saiu); `baseSalary` é
  sobreposto sem histórico (é o bug da margem histórica da V37 no RH); férias contadas em dias de
  **calendário** contra os "dias úteis" prometidos na spec, com direito anual fixo em `22`
  compilado; recibo sem `APPROVED`, ao contrário do que a `HR_PAYROLL_SPEC §3` promete.
- ~~**Bloqueador encontrado de passagem:** duas migrações V46.~~ **Resolvido (2026-08-22):** a do CRM
  passou a `V47__crm_ticket_lifecycle.sql`; `V46__internal_replenishment.sql` (já no git) ficou.
- **Não decidido pela IA:** valores legais (dias de férias por antiguidade, acréscimos de hora
  extra, prazos de INSS/IRPS, aviso prévio) ficam **configuráveis** e por confirmar com o
  contabilista, no molde do `PayrollTaxConfig`.

**B7.1 fechado (2026-08-22)** — guardas e rasto, sem alteração de schema:

- `recordAbsence`, `deleteAbsence` e `submitVacation` passaram a exigir `ensureHrManager()`.
  Eliminar uma falta era a porta mais silenciosa do RH: apagava o desconto que ela provoca no
  recibo, sem guarda e sem rasto — o líquido subia e não ficava nada escrito.
- Auditoria nova: `ABSENCE_CREATE` e `ABSENCE_DELETE` (o `delete` carrega a falta **antes** de
  apagar, por `AbsenceRepository.findByIdAndEmployeeCompanyId`, para o rasto poder nomeá-la).
- **`submitExpense` ficou deliberadamente sem guarda**, com auditoria `EXPENSE_SUBMIT` que nomeia
  o colaborador **e** quem submeteu. É o único dos quatro com uso self-service legítimo (o
  `DataLoader` submete como `EMPLOYEE` de propósito, para o painel de Aprovações mostrar o
  submissor real); recusar exige a ligação `Employee ↔ User` (B7.2). Até lá, a substituição fica
  **visível** em vez de impossível — e isso está declarado, não escondido.
- **Verificação:** `HRServiceTest` 13/13 verdes; os **6 casos novos confirmados a falhar** contra o
  `HRService` de HEAD. Dois deles só provam a guarda depois de terem sido apertados: passavam
  contra o código antigo por outro motivo (colaborador/falta por stubbed), e agora afirmam a
  mensagem da guarda.
- **Não foi possível correr `mvn test` completo:** a árvore de trabalho está partida por **outra
  sessão a meio de um refactor do CRM** — `gui/CRMPanel.java` (alterado às 21:02) importa
  `CrmTicketActions` e `CrmWorkSheetActions`, que ainda não existem. Não lhe toquei. O backend
  compila limpo (489 ficheiros, tudo menos `gui/` e `desktop/`) e os testes correram por
  `javac` + JUnit Platform directos. ~~**Correr `mvn -o clean test` assim que o CRM fechar.**~~
  **Corrido a 2026-08-23: 713 testes, 0 falhas/erros/ignorados** (ver abaixo).
- **UI:** nas abas Faltas e Férias, um utilizador EMPLOYEE passa a receber a recusa em PT-MZ do
  servidor. Desactivar os botões por perfil fica para quando o B7.2 trouxer a noção de "o próprio".

**B7.2 fechado (2026-08-22)** — `Employee ↔ AppUser`, **migração V48**:

- **A regra:** *um gestor age por qualquer colaborador; toda a gente age por si própria e por mais
  ninguém* (`HRService.ensureCanActFor`). Até aqui "o próprio" não era identificável, pelo que agir
  por outro era **indistinguível** de agir por si — e a única defesa possível (exigir MANAGER em
  tudo, como o B7.1 fez às férias) matava o self-service, que é metade do sentido de um RH.
- `submitVacation` e `submitExpense` voltam a ser self-service **e** recusam nome alheio.
  `getAllPayslips` filtra para quem não é gestor, e **`loadPayslipForPrint` aplica a mesma regra** —
  filtrar a lista e deixar imprimir por id era meia porta.
- Associação validada nas três frentes: conta tem de existir, ter acesso à empresa activa e não
  estar já noutro colaborador (espelha o índice único da V48). Campo em branco desliga a ligação.
- **Coluna nullable de propósito:** colaboradores sem conta — a maioria numa loja — continuam
  exactamente como antes. Nenhuma linha existente é alterada.
- **Desktop:** campo "Conta de Utilizador (opcional)" no cadastro do colaborador. `HRPanel` ficou em
  **995 linhas** (limite 1000 do `UiPanelDecompositionTest`) — a próxima adição tem de sair para
  classe própria. `DataLoader` liga maria/joão às contas, para o self-service ser demonstrável.
- **Verificação:** `HRServiceTest` **21/21 verdes**; contra uma variante sem `ensureCanActFor` e sem
  o filtro dos recibos, **5 falham** — as 5 que carregam a regra. As outras 3 afirmam permissão e
  não recusa, pelo que passam nas duas versões (dito aqui para não se confundir com cobertura).
- **V48 escolhida a saltar o V47 de propósito**, para deixar essa versão livre para o
  `V46__crm_ticket_lifecycle.sql` da outra sessão ser renomeado. ~~O Flyway não arranca com duas
  V46~~ — **resolvido a 2026-08-23**: a outra sessão renomeou-o para V47, exactamente como se
  esperava, pelo que a V48 encaixou sem colisão.
- ~~**Por correr:** `mvn -o clean test` completo~~ **feito a 2026-08-23** (713 verdes; **730** depois
  do B1.1). **Continua por correr:** V48 e V49 contra PostgreSQL real (receita do cluster
  descartável de 21/08).

**B1.1 fechado (2026-08-23)** — contrato de trabalho como documento, **migração V49**:

- **A regra que carrega o bloco: o contrato manda na folha.** `processMonthlyPayroll` filtrava só
  por `status == ACTIVE` na ficha — quem tinha contrato terminado a 31 de Julho recebia recibo em
  Agosto, em silêncio, com saída de tesouraria e tudo. Agora quem não tem contrato vigente no mês é
  saltado, e o resultado **diz quem e porquê**: `PayrollRunDTO(gerados, saltados)`.
- **O silêncio era metade do defeito.** "12 recibos gerados" que esconde um 13º colaborador saltado
  só se descobre quando alguém reclama o ordenado. A frase vive em `PayrollRunDTO.summaryMessage()`,
  não no painel — o `HRPanel` mudou **uma linha** (está a 995/1000 do guard-test).
- **Cobre qualquer dia do mês, não o último:** quem tem contrato até dia 15 trabalhou meio mês e tem
  de ser pago. Usar só o último dia do mês fá-lo-ia desaparecer da folha.
- **Nasce `RASCUNHO`, não `VIGENTE`** — divergência assumida contra a linha RHC-10 do harness, que
  contradizia a máquina de estados da própria spec. É a **activação** que verifica a sobreposição e
  escreve o salário acordado na ficha; assim dá para preparar um contrato com antecedência.
- **`EXPIRADO` não é estado gravado**, deriva-se de `end_date` contra hoje — mesma lição da cotação.
  Sem agendador nocturno e sem linhas desactualizadas entre passagens.
- **Renovar cria contrato novo** ligado ao anterior, que fecha na **véspera** do novo. O histórico
  do que foi acordado é imutável.
- Serviço e controller **próprios** (`EmploymentContractService`/`Controller`): o `HRService` já
  carrega colaboradores, recibos, faltas, férias, despesas e impostos.
- **Verificação:** `EmploymentContractServiceTest` **15/15**, `HRServiceTest` **23/23**, suite
  completa **730 verdes**.

**B1.2 fechado (2026-08-23)** — PDF, alertas e separador de contratos:

- **PDF do contrato** (`EmploymentContractPrintService`, `/api/print/employment-contract/{id}`).
  Não usa o `LineItemsTableRenderer`/`TotalsBlockRenderer`: um contrato não tem linhas nem totais,
  tem **cláusulas**. Compõe o `CompanyHeaderRenderer` e o `SignatureBlockRenderer`. A spec também
  mencionava o `CommercialTermsRenderer` — **não se aplica**, esse é para condições de pagamento e
  entrega, que um contrato de trabalho não tem.
- As cláusulas são **geradas do que está gravado** e numeram-se sozinhas conforme se aplicam. Um
  contrato impresso que diga coisa diferente do que a folha usa é pior do que não haver contrato:
  por isso um dos testes verifica que o papel mostra o salário **do contrato**, não o da ficha.
- **Alertas no sino:** um só endpoint `/api/hr/contracts/alerts` devolve as duas listas (fim de
  contrato ≤30 dias, fim de experiência ≤7). Janelas diferentes porque as decisões são diferentes —
  renovar tem antecedência, confirmar alguém no fim da experiência não tem. Endpoints separados
  seriam uma ida ao servidor a mais em cada refresh do sino, que já faz quatro.
- **Separador de contratos** em `HRContractsPanel` (molde do `HRExpensesPanel`), com criar, activar,
  renovar, cessar e imprimir.
- ⚠️ **O `HRPanel` ficou em 998/1000 linhas.** A próxima adição ao RH **tem de extrair um separador
  primeiro** (o das Férias, ~145 linhas, é o candidato óbvio) — não cabe mais nada.
- **Verificação:** `EmploymentContractPrintServiceTest` **5/5** (lêem o texto do PDF, não o código),
  `NotificationFeedTest` **4/4**, suite completa **737 verdes**.
- **Por fazer no B1:** a ficha do colaborador continua a permitir editar o salário à mão (RHC-19) —
  torná-la não-editável é o **B4**, não este bloco.

**B2.1 fechado (2026-08-23)** — ponto e assiduidade, **migração V50**:

- **A regra que carrega o bloco: as horas extra do recibo têm de ter origem.** Hoje
  `CreatePayslipRequest.overtime` é um número que quem processa a folha escreve à mão — ninguém
  sabe de onde veio, ninguém o pode contestar, e o mesmo valor pode ser pago duas vezes sem que
  nada o note. O B2.1 constrói a origem: `TimeEntry` datada, com autor e proveniência, apurada
  contra um `WorkSchedule`.
- **Não inventei multiplicadores.** A spec (§6) diz que os acréscimos legais têm de ser confirmados
  com o contabilista da empresa. Por isso o B2.1 **conta e classifica** as horas em três escalões
  (extra diurna, extra nocturna, dia de descanso) e o B2.2 é que lhes atribui valor. Escrever uma
  percentagem à sorte era o pior resultado possível: parecia certo e pagava mal.
- **A janela nocturna e a hora de entrada prevista são dados**, não constantes no código — a
  primeira porque varia com a convenção aplicável, a segunda porque sem ela o atraso é
  incalculável.
- **Divergência assumida (RHC-22):** a spec pedia bloquear "saída antes da entrada". Trato-a como
  **turno que atravessa a meia-noite** — quem entra às 22:00 e sai às 06:00 trabalhou 8 horas.
  Bloquear perdia o turno da noite inteiro. Bloqueado fica o caso que é mesmo erro: pausa ≥ turno.
- **Anti-duplicação por colaborador+data**, não colaborador+data+turno como a spec pede: não há
  modelo de turnos nesta iteração. Está dito no índice da V50 e no harness.
- **Os totais nunca são gravados** — apuram-se das marcações. Mesma lição da caducidade do contrato.
- **Desktop:** separador "Ponto" (`HRTimeSheetPanel`). Para caber, **extraí o separador de Férias**
  para `HRVacationsPanel` (era o que eu próprio tinha avisado ser preciso): o `HRPanel` desceu de
  **998 para 865** linhas e voltou a ter folga.
- **Verificação:** `TimeSheetServiceTest` **14/14**, suite completa **751 verdes**.

**B2.2 fechado (2026-08-23)** — o recibo passou a ler do ponto, **migração V51**:

- **RHC-26 fechado, que era o 🔴 do bloco.** Com a folha de ponto do mês fechada, as horas extra do
  recibo vêm apuradas das marcações. O campo manual sobreviveu como **excepção declarada**: divergir
  exige justificação e grava `PAYSLIP_OVERTIME_OVERRIDE`. A porta que era a regra passou a ser a
  excepção — mesmo padrão do `taxRate` manual do `CreateInvoiceLineRequest`.
- **Os multiplicadores não os decidi eu, e isso é a decisão de desenho.** `OvertimeRateConfig` é
  configurável por empresa, com vigência e `legal_basis` (molde do `PayrollTaxConfig`), **sem
  valores por omissão**. Sem configuração em vigor o sistema recusa-se a valorizar horas extra e
  diz que os valores têm de ser confirmados com o contabilista. Falhar em voz alta é melhor do que
  multiplicar por um número inventado, que parece certo e paga mal.
- **A hora normal sai do salário a dividir pelas horas previstas nesse mês**, não por um divisor
  fixo: um mês de 22 dias úteis e outro de 20 não valem a hora ao mesmo preço.
- **RHC-27 com refinamento assumido:** a folha salarial só bloqueia se **houver ponto marcado** no
  mês. Bloquear sempre deixaria sem processar salários qualquer empresa que não use o módulo — uma
  regressão no produto existente.
- **RHC-24:** a falta nasce no **fecho** do mês, é idempotente, nunca em dia de descanso, e nasce
  `PENDING_JUSTIFICATION` — **não desconta** até alguém decidir. Presumir má-fé automaticamente era
  a pior forma de estrear o módulo. **RHC-25:** `justifyAbsence` muda o tipo com motivo obrigatório
  e auditoria.
- **Verificação:** `OvertimeValuationServiceTest` **7/7**, `TimeSheetServiceTest` **17/17**,
  `HRServiceTest` **31/31**, suite completa **769 verdes**.
- **Por fazer:** UI para os acréscimos e para justificar faltas (endpoints prontos:
  `/api/hr/overtime-rates` e `/api/hr/absences/{id}/justify`); e **os valores dos acréscimos têm de
  ser confirmados com o contabilista** antes de a folha valorizar horas extra em produção.

**B4 fechado (2026-08-23)** — histórico salarial, **migração V52**:

- **Fecha os dois 🔴 do bloco e o RHC-19 que ficara pendurado do B1.** O defeito sério não era o
  histórico perdido: era o **recibo de Março passar a pagar ao valor de Setembro** quando alguém o
  reprocessasse — e nada parecer errado, porque o número era perfeitamente normal. Mesmo defeito
  que a V37 corrigiu na margem histórica do comercial, transposto para os salários.
- **A ficha deixou de ser a porta.** `updateEmployee` recusa um salário divergente e diz onde se
  faz. `employees.base_salary` continua a existir mas é **reflexo** da série, não a origem dela.
- **Data futura é compromisso, não facto:** uma alteração com efeito daqui a dois meses não mexe na
  ficha até lá chegar, mas já manda no recibo desse mês.
- **A activação de contrato regista uma alteração** (motivo `CONTRATO`) em vez de escrever na ficha
  — senão a série ficava com buracos exactamente nos momentos que mais interessam.
- **Verificação:** `SalaryHistoryServiceTest` **9/9**, `HRServiceTest` **34/34**, suite completa
  **781 verdes**.
- **Por fazer no B4:** o ecrã de evolução salarial (endpoint pronto, `SimpleBarChart` já existe).

### Progresso — 2026-08-21 (encomenda profissional + reposição interna: tudo a funcionar ao vivo)

**Encomenda profissional** (spec/harness: `ENCOMENDA_PROFISSIONAL_SPEC.md` + `_HARNESS.md`, EP-01..30
auto, EP-50..58). A conversão da cotação produzia uma encomenda **muda**: não sabia de onde vinha,
esquecia as condições negociadas, nunca tinha data de entrega, e o PDF terminava com
`Estado: PENDING_APPROVAL` — código interno num documento que vai para o cliente.

- `Order` += origem (`quotationId`/`quotationNumber`), condições copiadas e `expectedDeliveryDate`.
- **A cotação promete DIAS, a encomenda grava a DATA.** A cotação não sabe quando o cliente vai
  confirmar; a data nasce na conversão e fica gravada (molde do `Invoice.dueDate`). Se a aprovação
  interna atrasar, a data prometida não se mexe.
- `OrderStatusLabel` como fonte única PT-MZ; o `switch` privado e incompleto do
  `CustomerOrderFulfillmentService` passou a delegar.
- Extraídos `CommercialTermsRenderer` e `SignatureBlockRenderer`, partilhados por encomenda, cotação
  e guia. **Duas afirmações do harness foram corrigidas por serem falsas:** há mais **10 cópias**
  byte-a-byte do `signatureCell` (salários/stock/fiscal) e o `UIHelper.humanStatus` diz "Pendente"
  onde o `OrderStatusLabel` diz "Pendente de aprovação". Ambas ficam **declaradas** na spec §5.
- **EP-07/08/09 e EP-15 confirmados a falhar** contra as variantes sem as regras.

**Reposição interna** (`REPOSICAO_INTERNA_SPEC.md`, V46): a encomenda de uma loja ao armazém que a
abastece, que termina em transferência e **nunca em factura**. Assumida e fechada nesta sessão.

- **Dois bugs de produção encontrados a correr, que nenhum teste apanhava:**
  1. **`kind` era `varchar(20)` e `INTERNAL_REPLENISHMENT` tem 22 caracteres.** A via nova não cabia
     na base de dados: gravar rebentava com erro de SQL, não com regra de negócio. Coluna alargada
     para 40 na V46 (e no mapeamento), com folga para a próxima via.
  2. **A transferência nunca funcionou sobre stock sem lotes** — defeito pré-existente. Os lotes são
     uma subdivisão do stock, materializada preguiçosamente na 1.ª saída; a venda fazia essa
     migração (`ensureLegacyBatchIfNeeded`) e a transferência consumia FEFO por conta própria,
     encontrando "Disponível em lotes: 0" em stock que existe. Como a base de demonstração — e
     qualquer instalação anterior aos lotes — tem `stocks` preenchido e `product_batches` vazio, a
     reposição era **inutilizável**. A regra passou a viver num só sítio
     (`ProductBatchService.materialiseLegacyIfNeeded`), usada pelas duas portas.
- `StockTransferServiceTest` actualizado ao construtor novo (+ 2 casos para a regra acima).

**Verificação:** build limpo, suite completa **670 testes, 0 falhas/erros/ignorados** (eram 646).

**VALIDADO AO VIVO** (backend de pé, H2, dados de demo, ADMIN/MANAGER/EMPLOYEE):
- **EP:** cotação com condições e 7 dias → encomenda com origem `CT-2026/1`, condições herdadas e
  **entrega prevista = hoje+7 exacta**; estado em PT-MZ; PDF gerado.
- **RI:** R5 (origem=destino recusado), **R1** (facturar recusado), **R2** (guia recusada), **R6**
  (converte em `TRF-2026/1`, encomenda → "Transferência por aprovar"), **R8** (2.ª conversão
  recusada nomeando a transferência), **R9** (aprovar move o stock **uma vez**: Arroz 85→80 e +5 no
  destino, Açúcar 120→110 e +10; encomenda → "Transferido para a loja"), **R10/R11/R12** (registo
  retroactivo cria `EC-2026/3` já `TRANSFERRED`, recusa duplicar e recusa transferência por aprovar).
- Todas as recusas com mensagens PT-MZ que dizem **o que fazer a seguir**, não só que falhou.

**Desktop fechado:** botão **"Converter em Transferência"** na aba de encomendas (com diálogo de
transporte; lê a via da célula, que guarda o `OrderKind`, e recusa vendas a cliente antes de ir ao
servidor) e **"Registar Encomenda"** em Stock › Transferências, para o armazém que transferiu sem
pedido formal. A lógica ficou em `OrderToTransferAction` (padrão do `CustomerOrderFulfillmentActions`)
para o `ComercialPanel` não passar das 1000 linhas — ficou em **994**, apertado; a próxima adição
tem de sair para uma classe própria.

**Verificação final:** build de raiz (606 fontes), suite **674 testes, 0 falhas/erros/ignorados**.
**Validado ao vivo:** reposição → `TRF-2026/1` com dados de transporte (a resposta já reflecte a
ligação à encomenda) → aprovação move o stock **uma vez** (Arroz 85→81 e +4 no destino, Óleo 45→39
e +6) → encomenda "Transferido para a loja"; e o registo retroactivo cria `EC-2026/2` já cumprida,
ligada à `TRF-2026/2`.

**V44/V45/V46 APLICADAS E VALIDADAS CONTRA POSTGRESQL REAL (2026-08-21).** Como a cadeia de
migrações não corre em H2 (a **V2** falha, pré-existente) e não havia credenciais do PostgreSQL da
máquina, criou-se um **cluster descartável** com os binários instalados (`initdb` + `pg_ctl` em
porta própria, auth trust), sem tocar no servidor do utilizador — receita repetível para a próxima
vez que for preciso validar migrações.

- **45 migrações aplicadas, schema em v46**, e a aplicação **arrancou com `ddl-auto=validate`** no
  perfil `prod`: cada mapeamento de entidade bate com o schema que o Flyway construiu.
- Confirmado no schema real: `customer_orders.kind` é agora **varchar(40)** (a correcção do bug que
  rebentava a via de 22 caracteres), as 8 colunas novas da V45/V46 são todas *nullable*, as tabelas
  `quotations`/`quotation_lines` existem com `valid_until` e `delivery_days`, e a restrição
  `uk_quotations_company_number` está lá (lição da V31).
- **Percurso completo sobre PostgreSQL:** cotação `CT-2026/1` → encomenda com origem, condições e
  entrega prevista correcta; reposição interna gravada com `INTERNAL_REPLENISHMENT` na coluna;
  conversão em `TRF-2026/1`; aprovação move o stock **uma vez** (100 → 90 na origem, 10 no destino),
  cria os lotes `LEGACY-1` pela migração preguiçosa corrigida, regista 2 movimentos `TRANSFER` e
  fecha a encomenda em "Transferido para a loja".
- **Nota de rigor:** houve um `NullPointerException` no `Versioning.increment` do Hibernate que
  **não era defeito da aplicação** — as linhas de `stocks` tinham sido semeadas por SQL directo,
  deixando `version` a NULL. Quem carregar dados numa base fora do Hibernate tem de preencher a
  coluna `version`; corrigido o seed, o percurso passou.

Cluster destruído no fim; o PostgreSQL do utilizador (porta 5432) nunca foi tocado.

### Progresso — 2026-08-19 (cotação: o preço proposto passa a ser um documento)

- **Pedido do utilizador:** funcionalidade de cotação profissional, com spec e harness.
- **A lacuna:** o sistema começava na encomenda. Quem cotava fazia-o fora do sistema e reintroduzia
  o preço à mão — a proposta não existia como documento (não se sabia o que se prometeu, a quem,
  nem até quando) e o preço reintroduzido podia não ser o cotado.
- **Nova série `CT`** (`Quotation` + linhas, migração **V44**, `UNIQUE(company_id, quotation_number)`).
  Não move stock, dívida, caixa nem contabilidade — e o serviço **nem injecta** `InventoryService`
  ou `FinanceService`: não ter a dependência à mão é mais forte do que prometer não a usar.
- **A regra que carrega a funcionalidade — o preço cotado é o preço honrado.** A conversão copia as
  linhas verbatim, como `billOrder` já copia as da encomenda. Reapreçar pelo catálogo fazia a
  proposta de sexta-feira mudar sozinha na segunda. **CT-21/22 confirmados a falhar** contra a
  variante que reapreça.
- **Caducidade derivada, não gravada:** `Quotation.isExpired(hoje)` contra `valid_until` (gravada no
  documento, lição da V35). Sem agendador nocturno, sem linhas desactualizadas, e estender a
  validade não exige dança de estados. Converter caducada é recusado nomeando a data
  (**CT-23/30 confirmados a falhar** sem a guarda); estender é **MANAGER/ADMIN** e fica auditado
  **com a validade antiga e a nova** — sem isso, um preço ressuscitado era indistinguível de um
  preço que nunca caducou.
- **Converte uma só vez, e só em encomenda.** Mesma decisão dos "caminhos separados" da guia. Não há
  cotação→fatura: seria a mesma regra (stock, crédito, vencimento, série FT) em dois sítios — a
  forma exacta dos bugs do IVA, do saldo em dívida e da margem.
- **Uma porta para criar encomendas:** núcleo de `createOrder` extraído para
  `ComercialService.placeOrder`, agora o único sítio que numera `EC` e submete à Engine de
  Aprovações. Auditoria estática (CT-36/37) confirma-o.
- **Sem campo para ignorar:** `CreateQuotationLineRequest` não tem preço nem taxa — o apreçamento é
  do artigo. É a porta que o campo `taxRate` do `CreateInvoiceLineRequest` foi até 06/08, fechada
  por construção em vez de por convenção.
- **Desktop:** aba **Cotações (CT)** com editor próprio (`QuotationsPanel` + `QuotationEditorDialog`
  em `gui/commercial/`, para o `ComercialPanel` não passar das 1000 linhas — ficou em 961). O painel
  **não** compara datas: mostra `expired`/`daysUntilExpiry` calculados pelo servidor.
- **PDF A4** com o desenho partilhado da fatura, mais validade em destaque (com aviso quando já
  caducou), condições, assinatura de aceitação do cliente e nota de que não é documento fiscal.
  **Não imprime o armazém** — é interno, e o documento sai para o cliente.
- **Movimentos:** `MovimentoTipo.COTACAO` na vista unificada.
- **Verificação:** suite completa **630 testes, 0 falhas/erros/ignorados** (eram 589). Auditoria
  estática CT-36..39 verde.
- **VALIDADO AO VIVO (CT-50..62):** backend de pé (H2, dados de demo), percurso HTTP completo com
  ADMIN, MANAGER e EMPLOYEE.
  - **CT-57 (a regra central) provado:** cotação emitida com Farinha a 80,00 e Óleo a 165,00; o
    catálogo subiu para **200,00 e 400,00**; a conversão saiu na mesma a **80,00 e 165,00**, total
    idêntico (782,80). O preço proposto não se mexeu com o catálogo.
  - **CT-58:** linha envelhecida na BD para `valid_until` = hoje−3; o DTO passou a dizer
    `expirada=true`, `daysUntilExpiry=-3` **sem nada ter sido gravado** (a caducidade é derivada) e
    a conversão foi recusada com "caducou a 17/08/2026". Estendida por gerente → converteu.
  - **CT-59/60:** EMPLOYEE recusado a estender ("requer perfil MANAGER ou ADMIN") com a validade
    **inalterada**; MANAGER estendeu; estender para trás recusado nomeando a data actual.
  - **CT-33:** auditoria da extensão contém **as duas datas** ("estendida de 17/08/2026 para
    31/12/2026"), além de `QUOTATION_CREATE`/`QUOTATION_CONVERT`.
  - **CT-61 (cadeia completa):** cotação 2.339,04 → encomenda `PENDING_APPROVAL` → aprovada →
    `PENDING` → **fatura FT-2026/1** com o **mesmo total**. O stock só se moveu na facturação
    (Arroz 85→82, Óleo 45→41): a cotação e a conversão não tocaram em stock nenhum.
  - **CT-51/52/53:** PDF extraído com OpenPDF — todos os elementos partilhados presentes **nos dois**
    documentos (cabeçalho, NUIT, morada, bloco do cliente, colunas das linhas, totais, rodapé) e só
    a cotação tem "COTAÇÃO", validade, condições, aceitação do cliente e o aviso de não ser
    documento fiscal. **"Armazém" aparece na fatura e não na cotação**, como especificado.
  - **CT-09 ao vivo:** artigo isento saiu a **IVA 0,00** e o de 16% a 95,04 sobre 594 — a taxa é do
    artigo também nesta porta nova.
  - **CT-62:** Movimentos mostra a cadeia toda num sítio (CT-2026/3 → EC-2026/3 → FT-2026/1).
- **V44 por aplicar contra PostgreSQL real:** em dev o Flyway está desligado (Hibernate faz o
  schema), e a cadeia de migrações **não corre em H2** (a V2 falha — pré-existente, nada a ver com
  esta iteração). Verificado mecanicamente que as colunas da V44 batem **exactamente** com o
  mapeamento das entidades (25/25 e 8/8, sem falhas nos dois sentidos), que é o que o
  `ddl-auto=validate` de produção exige; falta correr a migração numa BD PostgreSQL — não há
  credenciais no repositório (deliberado) e não foram pedidas.
- Spec/harness: `docs/COTACAO_SPEC.md` + `docs/COTACAO_HARNESS.md` (CT-01..41 auto, CT-50..62 ✅).
  **Por validar:** a UI Swing (o backend está validado; o painel chama exactamente estes endpoints).

### Progresso — 2026-08-19 (encomendas: duas vias declaradas, não adivinhadas)

- **Pedido do utilizador:** as encomendas deviam ter duas áreas — A4 profissional (desenho da
  fatura, com aprovação) e pedido térmico (desenho do recibo do POS).
- **Os dois circuitos já existiam**; o que não existia era o sistema **saber qual é qual**. A via
  era adivinhada pelo estado (`else → A4`), pelo que um estado novo faria sair o documento errado
  em silêncio. `OrderKind` (FORMAL_ORDER / PICKING_REQUEST) passa a ser declarado na criação,
  gravado no documento (**V43**, backfill pela chave de idempotência — o estado é ambíguo, o
  `CANCELLED` existe nos dois circuitos) e é o único a decidir aprovação, documento e circuito.
- **O talão térmico não era um talão:** sem logótipo, NUIT, morada, telefone ou pontilhados, ao
  contrário do recibo do POS. Novo `ThermalReceiptRenderer` partilhado pelos dois (o recibo do POS
  não muda um pixel). Idem `ClientBlockRenderer` para fatura + encomenda A4, que tinham ~35 linhas
  copiadas — a mesma forma do bug do IVA e do saldo em dívida.
- **Desktop:** escolha da via no editor (com a consequência escrita antes de se escolher), coluna
  **Tipo** e acções a lerem a via em vez de compararem estados. Até aqui o desktop **só** criava
  pedidos de separação. Fluxo de criação extraído para `CommercialOrderSubmission` — a guarda
  `UiPanelDecompositionTest` apanhou o painel a passar das 1000 linhas.
- **Verificação:** suite completa **589 testes, 0 falhas/erros/ignorados** (eram 574). **ED-19
  confirmado a falhar contra o código antigo** ("falta o NUIT"). **VALIDADO AO VIVO:** A4 nasce
  `PENDING_APPROVAL` com aprovação pendente e **recusa** a lista de separação com a mensagem da
  via; o circuito térmico **força** `PICKING_REQUEST` mesmo com `FORMAL_ORDER` no pedido HTTP;
  talão impresso com NUIT/morada/email (9698 bytes, contra 1450 antes).
- Spec/harness: `docs/ENCOMENDA_DUAS_VIAS_SPEC.md` + `_HARNESS.md` (ED-01..27 auto, ED-50..58).
  **Por validar na UI Swing:** ED-51/56 (comparação visual A4 vs fatura, talão vs recibo POS).

### Progresso — 2026-08-19 (a guia de transferência não faltava; faltava ser encontrável)

- **Relatado como em falta** pelo utilizador. Existe e está completa desde sempre: modelo,
  serviço, endpoint, PDF A4 (origem/destino com localização, responsável, viatura, lotes, peso da
  carga, assinaturas de entrega e recepção) e UI com aprovação — o stock só se move na aprovação.
- **O problema era o sítio.** Vive em Stock → "Transferências entre Armazéns", que é onde
  pertence (move mercadoria entre armazéns da empresa, não é uma venda), mas quem precisa dela
  procura-a ao pé das Guias de Remessa, no Comercial.
- **Atalho** no cabeçalho das Guias de Remessa, com tooltip a dizer a diferença entre os dois
  documentos — é isso que evita a escolha errada, e a razão de o documento não mudar de sítio.
  `StockPanel.showWarehouseTransfers()` no molde do `showCustomerOrders()`; no `MainFrame` o Stock
  passa a nascer antes do Comercial.
- **Verificação:** 2 casos novos; suite completa **591 testes, 0 falhas/erros/ignorados**.

**Em curso por outra mão (não tocar sem falar):** módulo de **Cotação** (`Quotation*` em
`modules/comercial`, `docs/COTACAO_*`) e uma alteração a `DocumentSeries` — por commitar na
árvore de trabalho.

**Última actualização:** 2026-08-19 (cotação — primeiro bloco). Histórico anterior desde 2026-08-15:
**Estado:** software de loja concluído; Track B (cliente-fino) + correcções multi-tenant **em `main`**.
As **cinco lacunas de gestão** levantadas na auditoria de 09/08 estão **fechadas** (ver abaixo) —
incluindo a contabilidade, que era a maior ausência.
**Passo de profissionalização em curso** (rumo a produção): #1 teste de regressão ✅, #2 CI+gate ✅
(falta ligar a proteção de branch no GitHub), #6 sem dados/segredos de demo em prod ✅, #7 numeração
multi-empresa dos payslips ✅. **Falta:** ligar a proteção de branch; 1.º `docker compose up` real numa
VPS + smoke; backup restaurável verificado; validação em loja + hardware. A fonte de verdade operacional
é [tasks/retail_store_readiness.md](retail_store_readiness.md).
**Por integrar:** o ramo `feat/guia-remessa-navegacao-suite-local` está **23 commits à frente da
`main`** e por enviar para o remoto.

### Progresso — 2026-08-15 (navegação visual mais limpa)

- A barra superior mantém os módulos operacionais visíveis e agrupa Área Fiscal, Contabilidade,
  Aprovações e Configurações num menu textual “Mais”, reduzindo a densidade sem remover acessos.
- Os itens da navegação passaram a aceitar Enter/Espaço, expor nome acessível e mostrar foco visual.
- Verificação: `mvn -q -DskipTests compile` verde; teste focado de navegação adicionado.

### Progresso — 2026-08-15 (hierarquia visual de RH e Configurações)

- Spec/harness: `docs/HR_CONFIG_UI_HIERARCHY_SPEC.md` e
  `docs/HR_CONFIG_UI_HIERARCHY_HARNESS.md` (HCUI-01..24).
- Novo `ActionMenuButton` canónico limita menus a cinco entradas, mantém altura/ícones/acessibilidade
  e agrupa apenas acções secundárias.
- RH: Colaboradores ganhou “Mais acções”; Recibos ganhou “Documentos”. Configuração: Utilizadores
  ganhou “Mais acções”; as modalidades de backup passaram para “Criar backup”.
- Acções críticas continuam explícitas: Aprovar, Rejeitar, Eliminar, Marcar Pago e Processar Mês.
- Verificação: `mvn clean compile` e harness focado verdes; suite completa verde com **487 testes,
  0 falhas, 0 erros e 0 ignorados**. HCUI-20..24 requerem validação manual em Windows real.

### Progresso — 2026-08-15 (fecho visual de Stock e Comercial)

- Spec/harness: `docs/STOCK_COMERCIAL_UI_FINISH_SPEC.md` e
  `docs/STOCK_COMERCIAL_UI_FINISH_HARNESS.md` (SCUI-01..24).
- Faturas, encomendas, notas e guias agrupam impressão/exportação/actualização; Liquidar, Anular,
  Faturar, Converter, Cancelar, Aprovar e Rejeitar continuam visíveis.
- Stock global, categorias e armazéns ganharam hierarquia de acções; filtros passaram de altura 35
  para `UIHelper.FORM_CONTROL_HEIGHT`.
- `DocumentEditorHost` e `ModernFormDialog` ganharam `Ctrl+S` e `Esc`; atalhos POS preservados.
- Validação real a 1382×736 no tema claro: Comercial, RH e Stock sem cortes/sobreposições.
- Verificação: build limpo, harness focado e suite completa verdes com **492 testes, 0 falhas,
  0 erros e 0 ignorados**. Escalas 100/125/150%, tema escuro e acções com dados reais continuam
  como evidência manual obrigatória; a automação Windows falhou sob DPI elevado.

### Progresso — 2026-08-15 (uniformização final do design)

- Spec/harness: `docs/UI_UNIFORMIZACAO_FINAL_SPEC.md` e
  `docs/UI_UNIFORMIZACAO_FINAL_HARNESS.md` (FU-01..23).
- Perfis técnicos permanecem códigos internos, mas tabelas/selects de RH, Configuração, Aprovações
  e Plataforma apresentam Administrador/Gestor/Funcionário.
- Plataforma (empresas, assinaturas, utilizadores) e Fiscal/IVA adoptaram menus canónicos; acções
  principais e críticas continuam visíveis.
- `createDialogForm` passou a construir `FormField` canónico, preservando inputs e marcando labels
  com `*` como obrigatórias.
- Painéis de negócio ficaram sem cores locais; idioma uniformizado para “Actualizar” e “Registar”.
- Verificação: build limpo e suite completa verdes com **496 testes, 0 falhas, 0 erros e
  0 ignorados**; auditoria final sem `new Color`, “Atualizar”, “Cadastrar” ou `MANAGER/ADMIN` nos
  painéis. Certificação visual FU-20..23 continua manual.

### Progresso — 2026-08-15 (fecho das lacunas de gestão + contabilidade)

- **Suite volta a ser verde de forma determinística.** `LoadingCursorTest` rebentava com
  `HeadlessException` conforme a **ordem das classes**: o surefire arranca `headless=true` e o AWT
  decide a headlessness uma só vez, pelo que quem decidia era a primeira classe a tocar em AWT.
  `java.awt.headless=false` passou a ser declarado no `pom.xml` (a CI já corre com `xvfb-run`).
- **Vencimento e antiguidade** (V35): `Client.paymentTermsDays` + `Invoice.dueDate` **gravada no
  documento**; `assignDueDate` chamada pelas três portas que emitem fatura; `AgingBucket` como fonte
  única dos cortes 30/60/90; `ReceivablesService` + `/api/comercial/receivables/aging`. Contas
  Correntes ganharam Vencimento/Dias em Atraso/Antiguidade e ordenam pelo maior atraso.
  Spec/harness: `VENCIMENTO_ANTIGUIDADE_*` (VA-01..25).
- **Limite de crédito** (V36): três estados (nulo = sem limite, zero = não vende fiado, >0 = tecto);
  aritmética no domínio; trava nas três portas que criam dívida. **Encontrado ao testar:** no POS o
  stock saía *antes* da venda estar autorizada — extraído `deductStockForSale()` e movido para
  depois da trava. Spec/harness: `LIMITE_CREDITO_*` (LC-01..32).
- **Margem com o custo do acto da venda** (V37): `InvoiceLine.unitCost` fotografado na emissão; o
  relatório lia o preço de compra **actual**, pelo que a margem de vendas antigas mudava sozinha
  quando o fornecedor subia o preço. **MC-01 confirmado a falhar contra o código antigo.**
  Spec/harness: `MARGEM_CUSTO_HISTORICO_*` (MC-01..06).
- **Paginação**: `PageQuery`/`PageResponse` + `TablePager`; faturas e histórico do POS paginados. O
  mais grave não era a listagem — o **dashboard** lia todas as faturas da empresa a cada abertura
  para responder sobre *hoje*; as perguntas passaram a ir na consulta. Spec/harness: `PAGINACAO_*`
  (PG-01..11).
- **Contabilidade (PGC-NIRF)** (V38) — decisões do utilizador: plano moçambicano + lançamentos
  automáticos **e** manuais. Plano por empresa (natureza gravada **na conta**, não derivada da
  classe), partida dobrada validada numa só porta, série `LC` por empresa, razão com saldo de
  abertura e balancete que diz **"NÃO FECHA"** quando não fecha. Os lançamentos automáticos entram
  por **eventos** (`SaleRegisteredEvent`/`PaymentReceivedEvent`), pelo que o comercial não passou a
  conhecer a contabilidade. Painel novo com 4 abas. Spec/harness: `CONTABILIDADE_*` (CT-01..46).
  **Por fazer (declarado na spec §7):** salários e compras ainda não lançam automaticamente.
- `NotificationsPanel` migrado para `loadAsync` (mantendo o contador de versão, que protege contra
  respostas fora de ordem da **mesma** empresa — coisa que o `loadAsync` não cobre).
- **Verificação:** `mvn -o clean test` → **482 testes, 0 falhas, 0 erros, 0 ignorados** (eram 401).
- **Por validar ao vivo:** VA-50..57, LC-50..56, MC-50..53, PG-50..56, CT-50..60.

### Progresso — 2026-08-09 (consistência profissional da UI — fundação e adopção inicial)

- Criadas spec e harness: `UI_CONSISTENCIA_PROFISSIONAL_SPEC.md` e
  `UI_CONSISTENCIA_PROFISSIONAL_HARNESS.md` (UI-01..26 automáticos/estáticos; UI-50..62 manuais).
- Componentes canónicos: `FormField`, `MoneyField`, `QuantityField`, `DateField`; erro inline,
  obrigatoriedade, acessibilidade e estado read-only centralizados em `UIHelper`.
- Selects preservam renderers existentes; estados têm tradução central; botão icon-only exige nome
  acessível; tabelas ganharam renderers canónicos de dinheiro, quantidade e estado.
- `loadAsync` passou a propagar contexto, entregar erros no EDT e ignorar resposta de tenant antigo;
  `submitAsync` bloqueia duplo envio; `ModernFormDialog.setOnSaveAsync` impede HTTP no EDT.
- Adopção: Dashboard, Clientes, CRM e Tesouraria carregam assincronamente; cliente usa validação
  inline + submissão assíncrona; CRM/Tesouraria usam renderers tipados. Dashboard ficou com zero
  `new Color` e zero `setPreferredSize` (tokens semânticos no tema).
- Segunda vaga: Aprovações, Promoções e Fiscal migrados para loading/submissão assíncronos.
  Relatórios/PDF/SAF-T saem do EDT; taxas, retenções e promoções usam inputs tipados e validação
  inline. Os três painéis ficaram com zero cores ad-hoc; Promoções também com zero tamanhos fixos.
- Verificação: **389 testes, 0 falhas/erros/ignorados**. Próxima fase: alastrar loading/submissão aos
  restantes painéis e migrar formulários/documentos longos.

### Progresso — 2026-08-09 (recibo parcial deixa de apagar a dívida — 3 furos de dinheiro)

- **Encontrado a auditar o sistema a pedido do utilizador** ("está preparado para gestão?"). Três
  implementações do mesmo conceito — *quanto o cliente ainda deve* — a divergir em silêncio. Mesma
  forma do bug do IVA de 06/08: **a mesma regra em duas portas**.
- **(1) Recibo parcial dava a fatura por paga.** `ComercialService.createReceipt` marcava `PAID`
  por qualquer valor e nunca acumulava `amountPaid`. Pagar 100 de 232 → fatura *Paga*, os 132
  desapareciam das contas correntes e de qualquer cobrança. O **POS já fazia certo**
  (`deriveStatus`/`settleCredit`); só a porta comercial é que não.
- **(2) Dashboard e Contas Correntes discordavam.** `ReportService.unpaidInvoicesTotal` contava só
  `APPROVED` pelo **total**; as Contas Correntes contavam `APPROVED`+`PARTIALLY_PAID` pelo **saldo**.
  Idem em "vendas de hoje": o dashboard contava só `PAID`, o relatório diário tudo o que não fosse
  anulado — dois números para a mesma pergunta.
- **(3) `/api/finance/pay-invoice` sem guarda de papel.** `financeira` era o **único módulo de
  dinheiro sem `PermissionGuard`** — qualquer EMPLOYEE liquidava faturas. E registava sempre o
  total, contando em duplicado o que já tinha sido recebido.
- **Correcção — fonte única no domínio** (padrão do `Product.effectiveTaxRate()`):
  `Invoice.outstandingAmount()` + `Invoice.deriveStatusFromPayments()`, e
  `InvoiceStatus.isRealisedSale()`/`isCollectable()`. O `POSService.deriveStatus` privado foi
  **eliminado** — POS, faturação, tesouraria e relatórios passam pela mesma regra. `createReceipt`
  aceita vários recibos até o saldo zerar e recusa valor ≤ 0 ou acima do saldo; `cancelReceipt`
  devolve só o valor daquele recibo; `payInvoice` exige MANAGER/ADMIN e move só o saldo.
- **Desktop:** coluna **Em Dívida** na tabela de faturas, 2.º recibo permitido sobre
  `PARTIALLY_PAID`, valor sugerido = saldo (não o total) e mensagem que distingue recibo parcial
  de liquidação.
- **Verificação:** 15 testes novos (`ReportServiceTest` e `FinanceServiceTest` novos), **12
  confirmados a falhar contra o código antigo**. `POSServiceTest` (19) verde após a extracção da
  regra. `mvn -o clean test` → **371 testes, 0 falhas/erros/ignorados** (eram 356).
- Spec/harness: [docs/RECEBIMENTOS_SALDO_SPEC.md](../docs/RECEBIMENTOS_SALDO_SPEC.md) +
  [docs/RECEBIMENTOS_SALDO_HARNESS.md](../docs/RECEBIMENTOS_SALDO_HARNESS.md) (RP-01..23 auto,
  RP-50..56 manuais).
- **VALIDADO AO VIVO (RP-50..57):** backend de pé (H2, dados de demo), percurso HTTP completo com
  ADMIN e EMPLOYEE. Fatura de 950,00: recibo de 400 → `PARTIALLY_PAID`; recibo de 700 sobre saldo
  de 550 → **recusado** com a mensagem exacta; recibo de 550 → `PAID` (tesouraria 18.464,50 →
  19.414,50); anular o recibo de 400 → volta a **`PARTIALLY_PAID`** com 550 (não a `APPROVED`) e
  estorna 400; dashboard, relatório diário e contas correntes **de acordo** (`1 / 950.00`,
  400,00 por cobrar); EMPLOYEE recusado no `pay-invoice`; `payInvoice` moveu **400** (o saldo) e
  não 950.
- **Bug adicional encontrado durante a validação:** `POST /api/comercial/receipts` devolvia **500**
  apesar de gravar — `LazyInitializationException` no `toDTO` chamado **fora** da transacção pelo
  controller (`open-in-view=false`). Pré-existente e independente dos fixes de saldo, mas
  **agravado** por eles: antes a fatura ficava logo `PAID` e a repetição era recusada; agora
  continua cobrável, pelo que repetir criaria um 2.º recibo e duplicaria a caixa. Corrigido pela
  regra do próprio projecto (CLAUDE.md #3/#4): `createReceipt` e `getReceiptsByCompany` devolvem
  `ReceiptDTO` convertido **dentro** da transacção. O `GET /receipts` tinha o mesmo defeito latente.
- **Dados existentes:** faturas marcadas `PAID` por recibo parcial antes deste fix ficam como
  estão — query de diagnóstico na §5 da spec.
- **Por validar na UI Swing:** coluna Em Dívida, aviso de recibo parcial e valor sugerido no
  diálogo (o backend está validado; a UI chama exactamente estes endpoints).
- **Lacunas de gestão levantadas na mesma auditoria, por fazer:** sem `dueDate`/aging (não se sabe o
  que está **em atraso**), sem limite de crédito do cliente, margem calculada com o preço de compra
  **actual** (não o do acto da venda), **zero paginação** em todo o sistema (o dashboard carrega
  todas as faturas da empresa), e **sem contabilidade** (nem plano de contas, nem razão, nem
  balancete). Esta última é a maior ausência para um ERP de gestão.

### Progresso — 2026-08-08 (contexto de utilizador/empresa passa a fail-closed)

- **Encontrado a auditar a arquitectura a pedido do utilizador:** o `CurrentUserContext` inventava
  uma sessão quando não havia nenhuma — papel **`ADMIN`** e empresa **`1`**. Como o `PermissionGuard`
  (única guarda de papel do sistema) lê `getRole()`, **todas** as chamadas a `requireAdmin`/
  `requireManagerOrAdmin` eram no-ops em qualquer thread sem contexto, contra o tenant errado.
- **O fallback era load-bearing:** o `DataLoader` semeia tickets/despesas **através dos Services**
  (`crmService.createTicket`, `hrService.submitExpense`) sem contexto — só funcionava porque a empresa
  em falta virava `1`, que **por acaso** é a `ptCompany` (a primeira gravada). Mudar a ordem do seed
  aterrava os dados no tenant errado, sem erro. Agora declara
  `CurrentUserContext.runAsSystem(ptCompany.getId(), …)`.
- **Correcção:** `getRole()` sem contexto → `""` (o guard recusa); `getCurrentCompanyId()` → lança
  em vez de assumir a empresa 1; variantes **nullable** `findCurrentUser`/`findCurrentCompanyId` para
  infra que corre sem tenant (`UIHelper.loadAsync`, superadmin); `runAsSystem(...)` torna a elevação
  de privilégio **explícita e greppável**. O sino de notificações deixou de mostrar alertas da
  empresa 1 ao superadmin.
- **Nota de rigor:** o backup nocturno *parecia* o suspeito, mas **não** dependia do fallback — o
  `DatabaseBackupService` já separa `executePhysicalBackup()` (com guarda) de `runPhysicalBackup()`
  (núcleo, para o agendador). Não foi alterado.
- **Verificação:** CF-01/03/07/08 **confirmados a falhar contra o código antigo**. Ligar o fail-closed
  fez cair **8 testes em 2 classes** que dependiam dos fallbacks sem o declarar (`MulticoreServicesTest`,
  `ReceiptPrintServiceTest`) — passaram a declarar o contexto, sem mudar asserções.
  `mvn -o clean test` → **356 testes, 0 falhas/erros/ignorados**.
- Spec/harness: [docs/CONTEXTO_FAIL_CLOSED_SPEC.md](../docs/CONTEXTO_FAIL_CLOSED_SPEC.md) +
  [docs/CONTEXTO_FAIL_CLOSED_HARNESS.md](../docs/CONTEXTO_FAIL_CLOSED_HARNESS.md) (CF-01..08 auto,
  CF-50..54 manuais).
- **Pendente manual:** CF-50..54 (com o backend de pé), em especial **CF-53** — login do superadmin no
  desktop.

### Progresso — 2026-08-07 (redução incremental de dependências entre domínios)

- Centralizada em `CompanyService.getCurrentCompanyReference` a resolução de empresa usada para
  associações entre agregados, com validação obrigatória da empresa activa antes da consulta.
- `ProductCategoryService`, `TaxRateService` e `WithholdingService` deixaram de importar e chamar
  directamente `CompanyRepository`; passam agora pela API pública do domínio `company`.
- Novo `CompanyServiceTest` cobre empresa activa e recusa cross-tenant antes do Repository.
- Verificação: compilação limpa; `mvn -q test` → **347 testes, 0 falhas/erros/ignorados**.
- Próxima fatia: separar o acesso do POS a entidades comerciais/inventário por contratos próprios,
  numa alteração isolada devido à atomicidade checkout → stock → pagamentos.

### Progresso — 2026-08-05 (POS: operação rápida e acabamento profissional)

- Cabeçalho simplificado: Cliente e Código de barras sempre visíveis; Armazém e Conta ficam em
  **Mais opções**, sem perder a selecção usada no checkout.
- Atalhos reais: **F2** produto, **F4** cliente, **F6** quantidade, **F9** finalizar e **Delete** só
  com foco no carrinho. Duplo clique reutiliza o editor de quantidade, inclusive decimal.
- Numerário ganhou recebimento rápido Exacto/100/200/500/1000 MT, ligado ao cálculo de troco existente.
- Spec/harness: `docs/POS_OPERACAO_RAPIDA_SPEC.md` e `docs/POS_OPERACAO_RAPIDA_HARNESS.md`.
- Verificação: `mvn clean compile`, testes focados do POS e `mvn test` completos verdes.

### Correcção — 2026-08-05 (IVA visível no POS e recibo térmico)

- Corrigido o fallback visual de produtos sem taxa explícita: o carrinho usa a mesma taxa padrão do
  checkout, em vez de os apresentar incorrectamente como isentos.
- Recibo térmico passa a identificar a taxa em cada artigo (`IVA: 16%`, `IVA: 5%` ou `IVA: Isento`),
  mantendo Subtotal, IVA agregado e Total no resumo.
- `POSKeyboardShortcutTest`, `ReceiptPrintServiceTest` e `POSServiceTest` verdes; compilação limpa.
- Layout térmico ajustado para 80 mm: duas colunas (Artigo 65% / Total 35%), com quantidade × preço
  e IVA empilhados sob a descrição para evitar texto e valores apertados.

### Progresso — 2026-08-06 (IVA: a taxa é do artigo, não do ecrã — bug fiscal fechado)

- **Bug encontrado a auditar o IVA a pedido do utilizador** ("o IVA está incluso no POS e noutros
  lugares?"): o **mesmo artigo** era tributado de forma diferente conforme a porta. Provado ao vivo:
  Farinha de Trigo, cadastrada **IVA Isento** — fatura `80,00 + 12,80 = 92,80`, POS `80,00 + 0,00`.
- **Causa:** `ComercialService` usava a taxa **enviada no pedido HTTP** e o `ComercialPanel` gravava
  lá `TaxRates.STANDARD_VAT` fixo (16%). O POS, esse, já lia a taxa do artigo. Contaminava a fatura,
  a encomenda, a fatura gerada da encomenda, a guia e a NC (que herdam a linha), a **declaração
  mensal de IVA** e o **SAF-T** — ambos lêem `invoice.taxAmount`.
- **Correcção:** `Product.effectiveTaxRate()` passa a ser a **fonte única** (mesmo padrão do
  `effectiveUnitPrice`): taxa do cadastro, senão a padrão. Chamada por `POSService.checkout`,
  `createInvoice` e `createOrder`; o POS deixou de repetir a regra. `ProductDTO.effectiveTaxRate()`
  espelha-a só para a pré-visualização do rascunho nos painéis. O campo `taxRate` do pedido
  mantém-se por compatibilidade mas é **ignorado** — era a porta que permitia a qualquer integração
  faturar à taxa que quisesse.
- **Verificação:** `ProductTest` (4, IV-04..07) + `ComercialServiceTest` (+3, IV-01..03) —
  **confirmado que IV-01/02 falham contra o código antigo**. `mvn -o clean test` → **343 testes, 0
  falhas**. Ao vivo: fatura de artigo isento com pedido a insistir em 16% → **IVA 0,00**, igual ao
  POS; artigo a 5% → 7,00 sobre 140 (e não 22,40).
- **Compras (fechado a seguir, 2026-08-06):** numa compra manda a **factura do fornecedor**, não o
  cadastro — o mesmo artigo chega com taxas diferentes de fornecedores diferentes. `PurchaseService`
  e `PurchaseOrderService` passaram a usar a taxa indicada na linha e, sem ela, `effectiveTaxRate()`
  do artigo; nunca a constante. Campo **"IVA da factura (%)"** no `ComprasPanel` (aceita `16`/`5,5`,
  vazio = taxa do artigo) + coluna IVA no rascunho. DTOs com campo opcional e construtor
  retrocompatível. `PurchaseOrderServiceTest` +2 (IV-11/12), **verificados a falhar contra os 16%
  cegos**. `mvn -o clean test` → **345 testes, 0 falhas**.
- Spec/harness: [docs/IVA_TAXA_CANONICA_SPEC.md](../docs/IVA_TAXA_CANONICA_SPEC.md) +
  [docs/IVA_TAXA_CANONICA_HARNESS.md](../docs/IVA_TAXA_CANONICA_HARNESS.md).

### Progresso — 2026-08-01 (notificações: marcar como lida — sino **e** página)

- **Porquê estado no cliente:** as notificações são **derivadas** (agregam aprovações, stock,
  validades e assinatura em tempo real), não entidades com id — não há "read flag" no servidor. Novo
  `NotificationReadStore`: chave estável `type|title|detail|when` + conjunto de lidas nas
  **Preferences** do utilizador (sobrevive ao reinício; sem Preferences funciona em memória, com
  tecto de 200 chaves).
- **Sino:** badge passa a contar **não-lidas**; cada notificação do popup é um submenu com **Abrir
  módulo** e **Marcar como lida**; entrada **Marcar todas como lidas** (desactivada se não houver).
- **Página `NotificationsPanel` alinhada** (fechou o limite v1 da spec): coluna **Leitura**
  (`Por ler`/`Lida`), dropdown de filtro por leitura, botões **Marcar como lida** / **Marcar todas
  como lidas** e resumo "N por ler de M". Sino e página partilham a **mesma instância** do store; a
  página avisa o `MainFrame` por `IntConsumer` e o **badge actualiza sem novo pedido HTTP**.
- **Também aqui:** correcção do estado vazio das tabelas (`TableEmptyState`) — o overlay "Sem
  registos." podia sobreviver a actualizações consecutivas do modelo/sorter e ficar por cima de
  linhas reais; passou a confirmar-se após os listeners do Swing/`RowSorter`, com barreira defensiva
  no layout. Regressão coberta em `TableUxTest`.
- **Verificação:** `mvn -o clean compile` limpo; `NotificationReadStoreTest` (5, NL-01..05) +
  `NotificationsPanelTest` (2, NL-06/07) + `NotificationFeedTest` (2) + `TableUxTest` (6) verdes.
  Spec/harness: [docs/NOTIFICACOES_LIDAS_SPEC.md](../docs/NOTIFICACOES_LIDAS_SPEC.md) +
  [docs/NOTIFICACOES_LIDAS_HARNESS.md](../docs/NOTIFICACOES_LIDAS_HARNESS.md).
- **Pendente manual:** NL-50..59 (UI ao vivo, com backend de pé).

### Progresso — 2026-07-27 (piloto UX: documento em painel completo, não modal — Encomenda)

- **Decisão de UX (2026-07-27):** híbrido — listagem como ecrã principal, **painel completo** para
  documentos com linhas, **modais só para acções curtas**. Piloto aplicado à **criação de Encomenda**.
- **Feito (só UI):** novo componente reutilizável `mz.multicore.erp.gui.components.DocumentEditorHost`
  (barra: **← Voltar à lista** com guarda de alterações + título + **Guardar**). A aba **Encomendas**
  passou a `CardLayout` (lista ⇄ editor): **Nova Encomenda** mostra o editor a ecrã inteiro
  (reutiliza o mesmo `orderFormContent` + `issueOrderOrThrow`) em vez do modal; **Guardar** cria,
  informa, recarrega e volta à lista; **Voltar** confirma descarte se houver rascunho. O modal
  `openOrderFormDialog` foi **removido**.
- **Verificação:** `DocumentEditorHostTest` (2, DE-01/02); build limpo. Spec/harness:
  [docs/DOCUMENTO_PAINEL_EDITOR_SPEC.md](../docs/DOCUMENTO_PAINEL_EDITOR_SPEC.md) +
  [docs/DOCUMENTO_PAINEL_EDITOR_HARNESS.md](../docs/DOCUMENTO_PAINEL_EDITOR_HARNESS.md).
- **Alastrado à Fatura (2026-07-31):** a aba Faturação passou ao mesmo padrão (CardLayout lista⇄editor,
  reutiliza `invoiceFormContent` + `submitInvoiceOrThrow`, modal `openInvoiceFormDialog` removido). O
  `DocumentEditorHost` ganhou **scroll vertical** (formulários altos deixam de cortar os botões de baixo).
- **A seguir:** Compras (encomenda a fornecedor); suportar **editar** documento existente no host.

### Progresso — 2026-07-23 (central de notificações + bell)

- **Nova página `NotificationsPanel`:** tabela pesquisável/filtrável por tipo com alertas reais de
  aprovações pendentes, stock abaixo do mínimo, lotes vencidos/a vencer em 30 dias e assinatura em
  risco. Ações **Atualizar** e **Abrir módulo** encaminham para Aprovações, Stock ou Configurações.
- **Bell na barra superior:** contador de alertas + dropdown com as 5 primeiras notificações. O item
  **Ver todas** navega para a página completa. Recarrega ao trocar de empresa.
- **Cliente-fino e EDT:** `NotificationFeed` agrega apenas clientes HTTP; bell/página carregam via
  `SwingWorker`. O `companyId` é capturado no EDT e passado explicitamente à thread de fundo, evitando
  perder o tenant por causa do `CurrentUserContext` ser `ThreadLocal`; respostas antigas são ignoradas
  quando a empresa muda durante o carregamento.
- **Testes:** `NotificationFeedTest` cobre agregação das quatro fontes + estado vazio;
  `DesktopThinContextTest` confirma que o desktop continua sem DataSource/Services backend.
  `mvn test` → **321 testes, 0 falhas/erros/ignorados** (51 suites).

### Progresso — 2026-07-23 (lote de UX das tabelas: auto-hide, estados vazios, menu de contexto, loading)

- **Pedido do utilizador (4 melhoras de UI, todas):** ligadas centralmente em `styleScrollPane`.
  1. **Barra de navegação auto-esconde** (só quando a tabela transborda) + **atalhos** Home/End/
     PgUp/PgDn na tabela (`TableNavigator`).
  2. **Estados vazios** (`TableEmptyState`, novo): tabela sem linhas mostra "Sem registos." (texto
     personalizável por `putClientProperty("emptyText", …)`), overlay centrado que não tapa dados.
  3. **Menu de contexto** (`TableContextMenu`, novo): botão direito selecciona a linha e abre
     Copiar linha/célula · Ir topo/fundo (genérico; acções de domínio ficam nos botões).
  4. **Feedback de carregamento** (`UIHelper.loadAsync`): busca fora do EDT + cursor de espera;
     adoptado na aba **Guias de Remessa** (referência; restantes painéis adoptam incrementalmente).
- **Verificação:** `TableNavigatorTest` (9, +UX-01/02) + `TableUxTest` (5, UX-03..06). Spec/harness:
  [docs/UI_TABELAS_UX_SPEC.md](../docs/UI_TABELAS_UX_SPEC.md) + [docs/UI_TABELAS_UX_HARNESS.md](../docs/UI_TABELAS_UX_HARNESS.md).

### Progresso — 2026-07-23 (barra lateral de navegação em todas as tabelas)

- **Pedido do utilizador:** botões laterais nas tabelas para navegar (topo/cima/baixo/fundo), como
  noutros sistemas. Spec+harness.
- **Feito (só UI, sem backend):** novo componente `mz.multicore.erp.gui.components.TableNavigator` — barra
  vertical (Topo `fas-angle-double-up`, Página acima `fas-angle-up`, Página abaixo `fas-angle-down`,
  Fundo `fas-angle-double-down`) **fora da tabela, no EAST do contentor** do scroll (mesmo padrão do
  rodapé `maybeAddListingFooter`, que vai ao SOUTH) — não sobrepõe células. **DRY:** ligada
  **num só ponto** — `UIHelper.styleScrollPane(...)` instala-a quando o conteúdo é uma `JTable`, pelo
  que **cobre transversalmente todas as ~60 tabelas** sem tocar nos ~80 sítios. Opera sobre a
  `JScrollBar` vertical (independente do modelo/filtro), idempotente, ícones vectoriais (sem emojis).
- **Verificação:** `TableNavigatorTest` (7, JUnit puro — TN-01..07). Spec/harness:
  [docs/TABELAS_NAVEGACAO_SPEC.md](../docs/TABELAS_NAVEGACAO_SPEC.md) +
  [docs/TABELAS_NAVEGACAO_HARNESS.md](../docs/TABELAS_NAVEGACAO_HARNESS.md).

### Progresso — 2026-07-23 (Guia de Remessa ao cliente a partir da encomenda — backend + desktop)

- **Pedido do utilizador:** converter encomenda em guia, à maneira profissional (spec+harness).
  **Reverte** a decisão de 2026-06-21 (MOVIMENTOS_COMERCIAIS §7.1 dizia "não é requisito").
- **Regra central (decidida com o utilizador): caminhos separados.** Uma encomenda vira **guia OU
  fatura**, nunca as duas; para faturar mercadoria expedida por guia, faz-se **nova encomenda**.
  Consequência: **`billOrder` NÃO foi alterado** (continua a exigir `PENDING` e a baixar stock).
- **Feito (backend):** novo documento `DeliveryGuide` + linhas no módulo `comercial`, série **`GR`**
  numerada por empresa (migração **V34**, `UNIQUE(company_id, guide_number)` — respeita a V31).
  `DeliveryGuideService` no molde do `StockTransfer`: nasce `PENDING_APPROVAL` e o **stock (SALE) sai
  só na aprovação** (FEFO, via `inventoryService.registerMovement` — mesmo caminho da faturação),
  MANAGER/ADMIN + auditoria. Gerar a guia trava a encomenda (`PENDING → GUIDE_PENDING → GUIDED`);
  rejeitar/cancelar liberta-a (`→ PENDING`). Controller `/api/comercial/delivery-guides`
  (create/approve/reject/cancel/list/get) + PDF `DeliveryGuidePrintService`
  (`GET /api/print/delivery-guide/{id}`, reutiliza cabeçalho/linhas partilhados + transporte/assinaturas).
- **Verificação:** `mvn clean compile` → **BUILD SUCCESS** (461 fontes);
  `DeliveryGuideServiceTest` (9, Mockito puro — GR-01..GR-10); `mvn test` → **305 testes,
  0 falhas/erros/ignorados** (48 suites; contexto Spring arranca com os novos beans).
- Spec/harness: [docs/GUIA_REMESSA_ENCOMENDA_SPEC.md](../docs/GUIA_REMESSA_ENCOMENDA_SPEC.md) +
  [docs/GUIA_REMESSA_ENCOMENDA_HARNESS.md](../docs/GUIA_REMESSA_ENCOMENDA_HARNESS.md) (§9 UI, GR-60..69).
  Canónico [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) actualizado (§1, §2, §4, §5.1, §7.1).
- **Fase 2 (feita) — UI cliente-fino + Movimentos:** `ComercialApiClient` ganhou list/get/create/
  approve/reject/cancel/print da guia (só HTTP/DTO). `ComercialPanel`: botão **"Converter em Guia"** na
  aba Encomendas (modal de transporte) + aba **"Guias de Remessa (GR)"** (aprovar/rejeitar/cancelar/
  imprimir/atualizar, com aviso de saída de stock na aprovação). **A conversão aparece nos Movimentos:**
  `MovimentoTipo.GUIA_REMESSA` + `MovimentosService` agrega `delivery_guides` (`MovimentosServiceTest`
  ajustado ao novo repositório). Harness GR-60..69 actualizado com o percurso desktop completo.
- **Pendente manual:** validação ao vivo (GR-50..55 backend, GR-61..69 desktop) com o backend de pé; regra de
  stock/guardas ficam no backend (a UI só chama HTTP).

### Progresso — 2026-07-22 (suite completa a correr localmente — fim da limitação de RAM)

- **Lacuna fechada:** a suite `@SpringBootTest` **só corria na CI** — localmente rebentava por RAM
  (visto em várias iterações). Causa raiz: cada teste de integração define um `spring.datasource.url`
  próprio → **contexto Spring distinto** por classe; a cache não os reutiliza mas mantinha **8 contextos
  ERP completos vivos ao mesmo tempo**.
- **Fix (só código de teste/build, nada no runtime de produção):**
  - `src/test/resources/spring.properties` → `spring.test.context.cache.maxSize=1` (evicta o contexto
    anterior antes de construir o próximo; pico de RAM ~8× menor, **sem** perda de reutilização — cada
    contexto já era usado por uma só classe).
  - `src/test/resources/application-test.properties` (perfil `test`): pool Hikari mínimo, springdoc +
    consola H2 desligados, logging silencioso. **Não** mexe em `headless` — os testes de contexto
    completo carregam beans Swing e exigem `headless=false` (ver `MulticoreServicesTest`).
  - `pom.xml`: `maven-surefire-plugin` com `forkCount=1`/`reuseForks=true` (um só JVM, para a cache ser
    eficaz) + `-Xmx1536m`.
- **Verificado localmente:** `mvn -o clean test` → **296 testes, 0 falhas, 0 erros, 0 ignorados** (47
  classes). Log confirma a eviction (pool do contexto anterior fecha ao arrancar o seguinte).

### Progresso — 2026-07-21 (dados completos da empresa em TODOS os documentos)

- **Pedido do utilizador:** todos os documentos com os dados da empresa. Auditoria: já mostravam Nome/
  NUIT/Morada (A4 também Email) via `CompanyHeaderRenderer` partilhado; **faltavam Telefone e Logótipo**.
- **Feito:** `Company` + `phone` + `logo` (`bytea`, migração **V33**). `CompanyHeaderRenderer` (cobre ~13
  documentos A4, DRY) e `ReceiptPrintService` (recibo térmico) passam a desenhar **Logótipo + Nome + NUIT
  + Morada + Telefone + Email**. À prova de falha: sem logo/telefone → sai na mesma; logótipo inválido →
  try/catch, sem crash. Entrada pelo **superadmin**: `Create/UpdateCompanyRequest` += `phone`,
  `PlatformCompanyDTO` += `phone`/`hasLogo`, endpoint `POST /api/platform/companies/{id}/logo`
  (octet-stream) + `PlataformaPanel` (campo Telefone + seletor de logótipo, reusa `UIHelper.readScaledImage`).
- **Verificado AO VIVO** (PostgreSQL real): definido telefone+logo na MZ; extração de texto do PDF de
  **fatura** e **recibo** confirma o cabeçalho completo + imagem embutida (`PdfVerify.java` com OpenPDF).
  Fail-safe (sem dados / logo inválido) OK. `mvn -o test` (Platform/Comercial/POS + regressão) **50, 0 falhas**.
  Dados de teste repostos.
- Spec/harness: [docs/DADOS_EMPRESA_DOCUMENTOS_SPEC.md](../docs/DADOS_EMPRESA_DOCUMENTOS_SPEC.md) +
  [docs/DADOS_EMPRESA_DOCUMENTOS_HARNESS.md](../docs/DADOS_EMPRESA_DOCUMENTOS_HARNESS.md) (DE-01..03 auto, DE-50..55 manuais).

### Progresso — 2026-07-20/21 (profissionalização rumo a produção — #1, #2, #6, #7)

- **#1 Teste de regressão** do bug multi-empresa: `InvoiceNumberUniquenessPerCompanyTest` (`@DataJpaTest`,
  leve — corre onde a suite `@SpringBootTest` não corre por RAM). Prova que o mesmo número coexiste em
  empresas diferentes e é rejeitado na mesma empresa. **Verificado que FALHA contra o código antigo** e
  passa com o fix. (`76f5d6e`)
- **#2 CI + gate de merge:** `build.yml` endurecido (`permissions: contents:read`, `timeout-minutes`).
  Confirmado via API pública que a **suite completa passa na CI** (o problema de RAM é só local).
  Documentado no README como ligar a **proteção de branch** (require PR + check `build`) — só o dono
  pode no GitHub. (`af5d28b`)
- **#6 Sem dados/segredos de demo em produção:** `DataLoader` gatado por `app.seed-demo-data` (default
  `true` em dev; `false` no perfil `prod`) — empresas/utilizadores/produtos fictícios já não entram em
  prod (taxas de IVA e categorias, de referência, continuam). Superadmin: senha por
  `${SUPERADMIN_PASSWORD}`; **sem env → conta não é criada** (fim do `superadmin/superadmin` default).
  **Verificado ao vivo** (H2 fresca, config tipo-prod): `ana/password`→400, `superadmin/superadmin`→400,
  `superadmin/<senha-config>`→OK (0 empresas). Senhas são bcrypt (legadas re-encriptadas no 1.º login).
- **#7 Numeração multi-empresa dos `payslips`** (fecha o follow-up do V31): a tabela não tinha
  `company_id` e a numeração (via `DocumentNumberService`) colidiria entre empresas. `Payslip` ganhou
  `company` (= empresa do colaborador), migração **V32** (add `company_id` + backfill de `employees` +
  FK + `UNIQUE(company_id, payslip_number)` no lugar da global). Teste de regressão
  `PayslipNumberUniquenessPerCompanyTest` (`@DataJpaTest`, **verificado que falha contra o código antigo**).
  **Verificado ao vivo:** PT e MZ emitiram ambos `REC-2026/3` e **coexistem** (V32 aplicada em PostgreSQL
  real, backfill 0 nulos). Dados de teste limpos.

- **Bug encontrado ao validar "vários postos ao mesmo tempo"** (2 sessões HTTP em paralelo contra o
  backend `prod`/PostgreSQL real): a numeração é **por empresa** (`document_sequences` chave
  `(company_id, series, doc_year)`, V30) mas a coluna do número tinha `UNIQUE` **global**. Duas empresas
  que cheguem ao mesmo número (ex.: ambas `FT-2026/5`) colidem → **HTTP 500**, mesmo **sem concorrência**.
  Afeta 8 tabelas (invoices, credit_notes, debit_notes, customer_orders, purchase_orders, purchases,
  receipts, stock_transfers). Relevante para a plataforma multi-empresa (superadmin + vários NUITs numa BD).
- **Correcção:** `UNIQUE(numero)` → `UNIQUE(company_id, numero)` nas 8 entidades + migração **V31**
  (`per_company_document_numbers`). Número string mantém-se (`FT-2026/N`); a empresa distingue pelo
  cabeçalho/NUIT. **Follow-up:** `payslips` não tem `company_id` (fica de fora, documentado).
- **Rede de segurança de concorrência:** `ConcurrencyRetry` (`architecture/concurrency`) — reexecuta a
  escrita em `ConcurrencyFailureException` (lock optimista `@Version` / pessimista), 3 tentativas,
  cada uma em transação nova. Ligado em `POSController.checkout` e `ComercialController.createInvoice`.
- **Verificação AO VIVO:** antes → MZ `FT-2026/5` colidia com PT `FT-2026/5` (500). Depois → MZ criou
  `FT-2026/5..12` **coexistindo** com a PT; **mesma empresa**, 2 postos, 8 faturas em paralelo →
  `FT-2026/6..13` gapless/distintas, stock −8. `mvn -o test` (Comercial+POS+PurchaseOrder) **64, 0 falhas**.
- Spec/harness: [docs/NUMERACAO_MULTIEMPRESA_SPEC.md](../docs/NUMERACAO_MULTIEMPRESA_SPEC.md) +
  [docs/NUMERACAO_MULTIEMPRESA_HARNESS.md](../docs/NUMERACAO_MULTIEMPRESA_HARNESS.md) (NM-01..02 auto,
  NM-50..55 manuais).

### Progresso — 2026-07-19 (Track B FECHADO — desktop cliente-fino completo)

- **Os 4 gigantes migraram para HTTP:** Stock (`884d67c`), POS (`da3596b`), Comercial (`d85febd`,
  "4.º/último gigante — fecha Track B"). Cada painel deixou de chamar o Service em processo.
- **Runtime cliente-fino (`a1af165`) — fecha o objetivo:** `DesktopApplication` passou a um contexto
  **não-web** (`WebApplicationType.NONE`), sem `DataSource`/JPA/Flyway; scan só de `mz.multicore.erp.desktop`
  + `mz.multicore.erp.gui` + `mz.multicore.erp.modules.pos.scale`. `application-desktop.properties` reduzido a
  `desktop.api.base-url`. **O desktop arranca SEM base de dados.** `MainFrame` já não depende de nenhum
  `@Service`/`@Repository` (últimos 2 removidos: `companyService` morto, `subscriptionService` →
  `MySubscriptionApiClient`).
- **Consequência:** o PostgreSQL pode agora ser fechado ao exterior — só o backend lhe acede.
- **Testes:** `DesktopThinContextTest` (novo) prova que o contexto desktop arranca sem `DataSource` e
  sem serviços/repositórios de backend, só com clientes HTTP. `MainFrameNavigationSmokeTest` removido
  (testava o desktop GORDO em processo, arquitetura extinta). Fluxos de dinheiro por HTTP cobertos no
  harness (`aa43d36`). `mvn -o clean test` → **281 testes, 0 falhas**.
- **Falta:** 1.º `docker compose up` real numa VPS (sem Docker nesta máquina) + merge para `main`
  (`feat/stock-thin-client` está 6 commits à frente).

### Progresso — 2026-07-18 (Endurecimento de segurança + validação ao vivo do deploy)

- **Segurança (item #1 de go-live):** `TokenAuthenticationFilter` valida o token opaco e o `SecurityConfig`
  deixou de ser `permitAll()` — `/api/**` exige token; login/logout e `/actuator/health` públicos. Actuator
  expõe só health. Dockerfile healthcheck → `/actuator/health`. Spec/harness:
  [docs/SEGURANCA_HARDENING_SPEC.md](../docs/SEGURANCA_HARDENING_SPEC.md) (SH-01..07).
- **Validado AO VIVO** (backend `prod` standalone contra PostgreSQL 18 real): app arranca, Flyway valida 30
  migrações, sem token→401, com token→200, health UP, login errado→400. Os 11 domínios migrados também
  responderam ao vivo (ex.: `/api/platform/companies` devolveu empresas reais).
- **Deploy:** `scripts/deploy-smoke.sh` (verificação pós-deploy) + secção de deploy no README.
- Commits: `e1202db` (hardening). Falta: os 3 gigantes (POS/Stock/Comercial), 1.º `docker compose up` real, merge.

### Progresso — 2026-07-13 (Hospedagem do backend + desktop cliente-fino — Track B)

- **Decisão do utilizador:** hospedar o backend Spring Boot separadamente em **VPS + Docker** com
  **PostgreSQL** privado, e migrar o desktop para **cliente-fino (só HTTPS)** — a BD nunca exposta.
- **Deploy (Track A) — feito:** [Dockerfile](../Dockerfile) multi-stage (inclui `postgresql-client`
  para o backup físico), [docker-compose.yml](../docker-compose.yml) (backend + PostgreSQL privado +
  Caddy TLS automático), [Caddyfile](../Caddyfile), `.env.example`, `.dockerignore`. Guião +
  checklist de hardening: [docs/DEPLOY_VPS_SPEC.md](../docs/DEPLOY_VPS_SPEC.md). **Não testado ao vivo**
  (sem Docker nesta máquina); o `SecurityConfig` fica permissivo (item #1 de go-live).
- **Migração para cliente-fino (Track B) — arrancou:** padrão provado (inclui **PDF-over-HTTP** e o
  1.º painel gigante); **10 de ~26 domínios** a passar por HTTP em vez de chamar o Service em processo:
  - Novos clientes `@Profile("desktop")`: `ApprovalApiClient`, `CRMApiClient`, `FinanceApiClient`,
    `PromotionApiClient`, `InventoryApiClient`, `PurchaseApiClient`; `ComercialApiClient` +=
    `getAllInvoices/getAllProducts/getActiveCategories`. Painéis migrados: Aprovações, CRM, Financeiro,
    Promoções, **Dashboard**, **RH** (Clientes já estava). O Dashboard passou a consumir **DTOs** em vez
    de entidades JPA (`StockDTO`/`PurchaseDTO`) e tem arranque resiliente; `ApprovalService`/`CRMService`/
    `HRService`/`PayslipPrintService` saíram do `MainFrame` (já sem painel a usá-los).
  - **PDF-over-HTTP:** `DesktopApiClient` ganhou `getBytes` (GET→PDF) e `postForList` (POST→array). Como
    os endpoints `/api/print/**` já existiam, o recibo de salário do RH imprime via
    `/api/print/payslip/{id}`. Padrão pronto para as impressões dos restantes painéis.
    Testes: `DesktopApiClientTest` passou a **7** (TC-06 postForList, TC-07 getBytes).
  - **Fiscal** (8.º): `FiscalApiClient` colapsa os 8 serviços do painel. **1.º domínio a exigir endpoints
    novos no backend** — `GET /api/fiscal/saft/export` (DTO com metadados), `GET /api/fiscal/saft/validate`
    (XSD) e `GET /api/print/payroll-fiscal-map` (PDF). Harness TC-60.
  - **Compras** (9.º, **1.º gigante** — 1.324 linhas): `PurchaseApiClient` estendido colapsa
    purchase+order+reorder; `DesktopApiClient` ganhou `patch` (PATCH do estado do fornecedor);
    `InventoryApiClient` += armazéns. O painel converteu `Supplier`/`Warehouse`/`Purchase` (entidades)
    para DTOs (nome do armazém resolvido por lookup, pois o `PurchaseDTO` só traz o id). Harness TC-61.
  - **Plataforma** (10.º, superadmin): `PlatformApiClient` colapsa empresas+utilizadores+assinaturas+
    suporte (~22 métodos, só DTOs). 3 endpoints novos de options (plan/method/status). `/api/platform/**`
    não precisa de empresa. `PlatformCompanyService`/`PlatformUserService` saíram do `MainFrame`. Harness TC-62.
  - **Config** (11.º): maior esforço single-panel. **3 controllers novos** (`/api/users`, `/api/audit`,
    `/api/backup`) + `AppUserDTO`/`AuditLogDTO`/`BackupStatusDTO` + 6 clientes. **Decisão de design:** o
    **backup corre no servidor** (onde está a BD) e a auditoria dos backups é registada server-side (o
    desktop deixou de chamar `logEvent`). Papel do utilizador = por empresa. 7 serviços saíram do
    `MainFrame`. Harness TC-63.
  - Carregamento passou para `onPanelSelected()` (nunca no construtor) para não rebentar sem empresa.
  - **Falta:** médios (Promoções, Fiscal, RH, Dashboard) e os grandes (POS/Stock/Compras/Comercial),
    que precisam de endpoints novos. Só se fecha o PostgreSQL ao exterior quando **todos** migrarem.
- Spec/harness: [docs/DESKTOP_THIN_CLIENT_SPEC.md](../docs/DESKTOP_THIN_CLIENT_SPEC.md) +
  [docs/DESKTOP_THIN_CLIENT_HARNESS.md](../docs/DESKTOP_THIN_CLIENT_HARNESS.md) (TC-01..05 auto,
  TC-50..56 manuais).
- **Verificação:** `mvn -o compile` limpo; `DesktopApiClientTest` (5) verde. Ida-e-volta HTTP real de
  cada painel valida-se ao vivo (manual) quando o backend estiver de pé.

### Progresso — 2026-07-11 (Polish Visual Multicore — aspecto ERP profissional)

- **`SlimScrollBarUI`** (novo): scroll bars finas (6 px), thumb violeta arredondado, sem setas — aplicado via `UIHelper.styleScrollPane()` em todos os JScrollPane do sistema.
- **`StatusBar`** (novo): rodapé de 24 px com módulo activo · nº registos · empresa · utilizador · hora. Timer interno (60 s) actualiza a hora. `MainFrame.navigate()` actualiza o módulo; `applyAuthenticatedUser` inicializa empresa e utilizador.
- **`SectionHeader`** (novo): cabeçalho de secção reutilizável — ícone opcional + título + separador 1 px.
- **`ModernPanel`** (melhorado): borda usa `UIHelper.BORDER` em painéis normais (adapta ao tema claro/escuro); painéis com gradiente mantêm branco translúcido subtil.
- **`styleTabbedPaneMulticore`** (novo em `UIHelper`): tabs Multicore com linha de acento 3 px na base (sem fundo cheio). Migrado em **10 painéis**: ApprovalsPanel, ComercialPanel, ComprasPanel, ConfigPanel, CRMPanel, FinanceiroPanel, FiscalPanel, HRPanel, PlataformaPanel, StockPanel.
- **Tooltips premium** em `UIHelper.initGlobalTheme()`: fundo `BG_CARD`, borda `BORDER`, fonte 12 px, delay 600 ms.
- Spec: [docs/MULTICORE_UI_POLISH_SPEC.md](../docs/MULTICORE_UI_POLISH_SPEC.md) | Harness: [docs/MULTICORE_UI_POLISH_HARNESS.md](../docs/MULTICORE_UI_POLISH_HARNESS.md).
- **Verificação:** `mvn -o compile` → BUILD SUCCESS.


- **Filtros de tabela transversais:** o componente `mz.multicore.erp.gui.components.TableFilter` (pesquisa
  com lupa + funil + dropdowns tipo/estado + período por data, colunas ordenáveis) foi estendido a
  **todas** as tabelas de listagem que ainda não tinham: Comercial (NC/ND/Recibos/Encomendas/Contas
  Correntes), Compras (Faturas/Fornecedores/Reposição/Contas a Pagar/Encomendas), Clientes, Stock →
  Gestão de Armazéns, RH (5 tabelas), Fiscal (Taxas/Retenções), Config (Auditoria/Utilizadores/
  Suporte), POS (Histórico de Vendas), Promoções. Acções que indexam a selecção passam por
  `TableFilter.selectedModelRow(...)` (o sorter faz a vista divergir do modelo). Fornecedores/
  Encomendas/Clientes migraram de pesquisa server-side/própria para o filtro cliente.
- Spec/harness: [docs/TABELAS_FILTROS_SPEC.md](../docs/TABELAS_FILTROS_SPEC.md) +
  [docs/TABELAS_FILTROS_HARNESS.md](../docs/TABELAS_FILTROS_HARNESS.md) (FT-01..07 auto, FT-50..68 manuais).
- **Verificação:** `mvn -o compile` limpo; `TableFilterTest` (7) verde; render confirmado ao vivo
  (Vendas → NC/Recibos, Compras → Faturas). Commit `a3ca84f`.
- **Backup/restore (Fase 6) — avançado:** validado ponta-a-ponta **exceto o apply final**, de forma
  não-destrutiva sobre a BD viva (PostgreSQL 18): `pg_dump -Fc` OK (58/58 tabelas, 520 objetos),
  `pg_restore --list` OK ⇒ arquivo completo e restaurável. **Falta** o passo BR-50..54 (restaurar em
  BD limpa + comparar contagens), que exige role com `createdb`/superuser — a role `multicore` não
  tem. É um passo manual de ~3 comandos (documentado no handover).

### Progresso — 2026-07-05 (Assinante: vista própria + alertas de expiração)

- **Vista do assinante** (só-leitura, tenant-scoped): `SubscriptionService.getMySubscription()` +
  `GET /api/subscription/me` → `MySubscriptionDTO` (plano, estado, validade, **dias restantes**,
  mensalidade). Desktop: aba **"A Minha Assinatura"** no `ConfigPanel`.
- **Alertas (7 dias)**, severidade única (vermelho expirada/suspensa; amarelo ≤7 dias): (1) aviso no
  login (`MainFrame.checkSubscriptionOnStartup`, disparado no `DesktopLauncher`); (2) chip permanente
  na barra de topo (só em risco); (3) linhas coloridas na aba Assinaturas do superadmin. À prova de
  falha (leitura falha ⇒ sem alerta, UI não rebenta).
- Spec/harness: SB-06 auto + SB-55..59 manuais.
- **Também corrigido no arranque real** (bugs que só aparecem a correr): colisão de bean
  (`PlatformSupportTicketRepository`), colisão de nome de entidade (`@Entity("PlatformSupportTicket")`)
  e crash da `MainFrame` no login do superadmin (`ClientesPanel` deixou de carregar no construtor).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (6) verde; app corre em
  PostgreSQL real (Flyway V24–V26 aplicadas).

### Progresso — 2026-07-05 (Superadmin — Fase 4: assistência) — funcionalidade fechada

- Módulo `support` (migração `V26`), distinto do `crm`: `SupportTicket` (assunto, descrição, estado
  OPEN/IN_PROGRESS/RESOLVED/CLOSED, prioridade, responsável) + `SupportMessage` (conversa;
  `fromSuperAdmin`). `SupportService` com **dois lados**: empresa (MANAGER/ADMIN, tenant-scoped) abre
  e responde; superadmin vê todos, responde (assume + OPEN→IN_PROGRESS) e muda estado. Resposta da
  empresa a RESOLVED reabre; CLOSED bloqueia. Controllers `/api/support/tickets` (tenant) e
  `/api/platform/support/tickets` (superadmin).
- **Desktop:** aba "Assistência" no `PlataformaPanel` (superadmin) + aba "Suporte à Plataforma" no
  `ConfigPanel` (empresa abre/consulta/responde).
- Spec/harness: ST-01..05 auto, ST-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `SupportServiceTest` (5) + `PlatformUserServiceTest` (5) +
  `PlatformCompanyServiceTest` (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Superadmin completo (Fases 1–4).** Assinaturas continuam manuais (sem gateway M-Pesa/e-Mola);
  esse é o próximo passo natural se se quiser cobrança automática.

### Progresso — 2026-07-05 (Superadmin — Fase 3: utilizadores globais)

- **Corte vertical.** `PlatformUserService` (SUPERADMIN + auditado) dá a visão de **todas** as
  empresas: `listUsers`, `createUser` (liga a empresa/papel), `setUserActive` (não desactiva o
  superadmin), `resetPassword`, `grantAccess`, `revokeAccess` (**protege o último ADMIN**). Controller
  `/api/platform/users`. `AppUser.revokeCompany` novo (orphanRemoval).
- **Desktop:** aba "Utilizadores" no `PlataformaPanel` (Novo, Conceder/Alterar Acesso, Revogar,
  Repor Senha, Activar/Desactivar).
- Spec/harness: SU-01..05 auto, SU-50..53 manuais.
- **Verificação:** `mvn -o compile` limpo; `PlatformUserServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `SubscriptionServiceTest` (5) → **verdes**.
- **Próxima:** Fase 4 (tickets `support` empresa→superadmin + abrir tickets no lado da empresa).

### Progresso — 2026-07-04 (Superadmin — Fase 2: assinaturas + pagamentos)

- **Corte vertical** (backend + UI). Módulo `subscription` (migração `V25`): `Subscription` (1:1 com
  empresa; plano TRIAL/BASIC/PRO/ENTERPRISE, estado TRIAL/ACTIVE/SUSPENDED/EXPIRED, validade, preço;
  `effectiveStatus()` deriva EXPIRED da validade) + `SubscriptionPayment` (valor, método
  DINHEIRO/MPESA/EMOLA/TRANSFERENCIA/OUTRO, período, nota). `SubscriptionService` (SUPERADMIN +
  auditoria): `listOverview/saveSubscription/changeStatus/recordPayment/listPayments` + `allowsLogin`
  (política interna). Registar pagamento **estende a validade** e reactiva. Controller
  `/api/platform/subscriptions`.
- **Login:** passa a filtrar por `allowsLogin` além de `company.active` — assinatura expirada/suspensa
  bloqueia; sem assinatura continua acessível.
- **Desktop:** aba "Assinaturas & Pagamentos" no `PlataformaPanel` (Definir Plano/Validade, Registar
  Pagamento, Ver Pagamentos, Suspender/Reactivar).
- Spec/harness actualizados (SB-01..05 auto, SB-50..54 manuais).
- **Verificação:** `mvn -o compile` limpo; `SubscriptionServiceTest` (5) + `PlatformCompanyServiceTest`
  (4) + `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**.
- **Próximas:** Fase 3 (utilizadores globais), Fase 4 (tickets `support`).

### Progresso — 2026-07-04 (Superadmin / Consola da Plataforma — Fase 1)

- **Pedido do utilizador:** um **superadmin** (dono da plataforma) que vê todas as empresas,
  activa/desactiva, gere utilizadores/assinaturas/pagamentos e dá assistência. Decisões: aba
  escondida no mesmo app (papel `SUPERADMIN`); pagamentos **manuais**; assistência por **tickets**
  empresa→superadmin; empresa suspensa **bloqueia o login** (não mata sessões vivas).
- **Processo:** spec+harness →
  [docs/SUPERADMIN_PLATAFORMA_SPEC.md](../docs/SUPERADMIN_PLATAFORMA_SPEC.md) +
  [docs/SUPERADMIN_PLATAFORMA_HARNESS.md](../docs/SUPERADMIN_PLATAFORMA_HARNESS.md) (SA-01..04 auto,
  SA-50..56 manuais). Entregue **por fases**; esta é a **Fase 1**.
- **Fase 1 (feita):** `AppUser.platformAdmin` + `Company.active` (migração `V24`); seed idempotente
  da conta `superadmin/superadmin`. Autorização: `/api/platform/**` sai do tenant-check do
  `SecurityInterceptor` (exige `platformAdmin`, papel `SUPERADMIN`, sem empresa);
  `PermissionGuard.requireSuperAdmin`; `TenantAccessService.requireSuperAdmin`. Login devolve
  `superAdmin` e só empresas **activas**; utilizador de tenant sem empresa activa é recusado.
  Módulo `platform`: `PlatformCompanyService` (listar/criar/editar/activar-desactivar, auditado) +
  controller `/api/platform/companies` + DTOs. Desktop: sessão transporta `superAdmin`;
  `PlataformaPanel` (aba Empresas: tabela + Novo/Editar/Activar-Desactivar); `MainFrame` mostra só a
  aba "Plataforma" ao superadmin (sem seletor de empresa/abas de tenant). Logout excluído do
  interceptor (superadmin não tem empresa).
- **Próximas fases:** 2 (assinaturas+pagamentos), 3 (utilizadores globais), 4 (tickets `support`),
  5 (painéis Pagamentos/Utilizadores/Assistência + abrir tickets no lado da empresa).
- **Verificação:** `mvn -o compile` limpo; `PlatformCompanyServiceTest` (4) +
  `AuthControllerIntegrationTest` (2) + `TenantAccessServiceTest` (4) → **verdes**. Suite completa
  não corre por falta de RAM (limitação de ambiente, como iterações anteriores).

### Progresso — 2026-07-04 (config separada por tipo de documento + comentário do recibo)

- **Pedido do utilizador:** o **POS** deve ter **configuração separada** dos documentos comerciais, e
  poder **definir o comentário/rodapé** que aparece no recibo.
- Config passou a ser **por `DocumentType`** (COMMERCIAL vs POS_RECEIPT), independente por empresa
  (entidade ganha `document_type`; unique `(company_id, document_type)`; migração `V23` recria a
  tabela de forma portável H2+PostgreSQL). `DocumentColumnsDTO` ganha `footer`. Service/controller/UI
  passam o tipo. Os 4 serviços comerciais usam COMMERCIAL; o `ReceiptPrintService` usa POS_RECEIPT e o
  rodapé configurável (`footer`; vazio = "Obrigado pela sua preferência!", suporta multi-linha).
  `ConfigPanel` ganhou selector de tipo + campo "Comentário do recibo".
- Spec/harness actualizados (DC-07 auto; DC-54..DC-57 manuais).
- **Verificação:** compila; testes da funcionalidade + serviços tocados → **56, 0 falhas**
  (`DocumentConfigServiceTest` 6, `LineItemsTableRendererTest` 4, POS/Comercial/Reorder/Inventory).
  ⚠️ A suite **completa** (209) não correu por **falta de RAM da máquina** (~568 MB livres) nos testes
  de integração Spring — limitação de ambiente, não de código (correr num ambiente com mais memória).

### Progresso — 2026-07-04 (colunas configuráveis dos documentos comerciais)

- **Pedido do utilizador:** poder definir **quais colunas** aparecem nos documentos comerciais
  (Fatura/Encomenda/NC/Guia, que partilham o `LineItemsTableRenderer`). Só mostrar/ocultar.
- **Processo:** spec+harness → skill `multicore-new-module` → **implementação delegada a um agent** →
  revisão `multicore-solid-review` (sem apontamentos bloqueantes) → verificação e commit.
- **Módulo `documents`** (`DocumentColumnConfig` por empresa, 8 flags, migração `V22`;
  `DocumentConfigService.getColumns/save` com MANAGER/ADMIN + auditoria `DOCUMENT_COLUMNS_UPDATE` +
  regra "pelo menos uma coluna"; `DocumentColumnsDTO` record; controller `GET/PUT /api/documents/columns`).
  `LineItemsTableRenderer` ganhou overload `build(rows, cols)` (extraiu `record Column` + `activeColumns`,
  **melhor DRY**; `build(rows)` delega em `all()`, retrocompatível). Os 4 serviços de impressão passam a
  config. UI: aba "Colunas dos Documentos" no `ConfigPanel` (8 checkboxes + Guardar) + wiring no `MainFrame`.
- Spec/harness: [docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_SPEC.md) +
  [docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md](../docs/DOCUMENTOS_COLUNAS_CONFIG_HARNESS.md) (DC-01..06 auto, DC-50..53 manuais).
- Testes: `DocumentConfigServiceTest` (5) + `LineItemsTableRendererTest` (+1). `mvn clean test` → **209, 0 falhas**.
- **Recibo do POS (extensão):** `ReceiptPrintService` passou a respeitar a mesma config no que cabe
  num recibo térmico — Qtd e Preço Unit. como colunas opcionais, Referência/Código de Barras como
  sublinha do nome; Descrição e Total sempre; Validade/IVA/subtotal por linha não aplicáveis.
  Harness DC-54/DC-55. `mvn test` → **209, 0 falhas** (recibo continua sem teste automático, como os
  restantes print services; validação manual).

### Progresso — 2026-07-04 (campos profissionais do armazém)

- **Pedido do utilizador:** `Warehouse` ganha (migração `V21`) **active**, **type**
  (`WarehouseType`: Loja/Depósito/Central/Trânsito), **allowsSales**, **manager**, **phone**.
  `getWarehousesByCompany` passa a filtrar **inactivos**; novo `getSalesWarehousesByCompany`
  (activo + allowsSales) usado pelo **POS** (deixa de vender de depósito). Diálogo "Criar Armazém"
  com Tipo/Responsável/Telefone/Permite vendas; `createWarehouse` overload novo (antigos delegam,
  retrocompatível).
- Spec/harness: [docs/ARMAZEM_PROFISSIONAL_SPEC.md](../docs/ARMAZEM_PROFISSIONAL_SPEC.md) +
  [docs/ARMAZEM_PROFISSIONAL_HARNESS.md](../docs/ARMAZEM_PROFISSIONAL_HARNESS.md) (AR-01 auto, AR-50..53 manuais).
- Testes: `InventoryServiceTest` +1 (filtro de vendas). `mvn test` → **203, 0 falhas**.
- **Ecrã de gestão de armazéns (feito):** nova aba "Gestão de Armazéns" no `StockPanel` (tabela com
  todos + Novo/Editar/Activar-Desactivar, duplo-clique edita). Backend `getAllWarehousesByCompany`,
  `updateWarehouse`, `setWarehouseActive` (MANAGER/ADMIN + auditoria). Diálogo criar/editar partilhado.
  Harness AR-53..AR-55.

### Progresso — 2026-07-03 (polish: cor de estado nas linhas)

- **Pedido do utilizador:** leitura de estado **por linha**. `UIHelper.styleTable` deteta uma coluna
  "Estado"/"Situação"/"Status" e pinta a **linha** com tom subtil (blend ~18% com a zebra, adapta ao
  tema). Vocabulário semântico centralizado em `statusColorFor` (verde/amarelo/vermelho, PT/EN de
  retalho, inclui ESGOTADO/BAIXO/EM STOCK/ANULADO/EM DÍVIDA…). Colunas "Estado" acrescentadas a
  **Níveis de Stock** e **Reposição**. Automático/DRY para qualquer tabela com coluna de estado.
- Só apresentação. Spec/harness: [docs/COR_ESTADO_LINHAS_SPEC.md](../docs/COR_ESTADO_LINHAS_SPEC.md) +
  [docs/COR_ESTADO_LINHAS_HARNESS.md](../docs/COR_ESTADO_LINHAS_HARNESS.md) (CE-01..05 manuais).
  `mvn test` → **202, 0 falhas**.

### Progresso — 2026-07-03 (preço grosso vs retalho — por produto + qtd mínima)

- **Sugestão do utilizador (3/3):** `Product` ganha `wholesalePrice` + `wholesaleMinQty` (migração
  `V20`). Regra pura `Product.effectiveUnitPrice(qty)` — aplica grosso quando `qty ≥ min`; senão
  retalho. **Aplicada nos 3 fluxos** (`createInvoice`, `createOrder`, `POSService.checkout`) sem
  tocar no `LineCalculator` (recebe o preço já resolvido; IVA por unidade). `ProductDTO` +2 campos;
  diálogos Cadastrar/Editar Produto com "Preço Grosso" e "Qtd mín. grosso" (opcionais). Retrocompatível
  (createProduct/updateProduct antigos delegam com grosso null).
- Spec/harness: [docs/PRECO_GROSSO_SPEC.md](../docs/PRECO_GROSSO_SPEC.md) +
  [docs/PRECO_GROSSO_HARNESS.md](../docs/PRECO_GROSSO_HARNESS.md) (PG-01/02 auto, PG-50..53 manuais).
- Testes: `ComercialServiceTest` +2 (grosso/retalho por quantidade). `mvn test` → **202, 0 falhas**.
- **As 3 sugestões pedidas ficaram concluídas** (reposição automática, Mobile Money, preço grosso).

### Progresso — 2026-07-03 (Mobile Money: M-Pesa / e-Mola)

- **Sugestão do utilizador (2/3):** `PaymentMethod` ganha **MPESA** e **EMOLA**, tratados como
  **electrónicos** (entram na tesouraria, não na gaveta; exigem conta; guardam a **referência** da
  transação em `PaymentEntry.reference`). Sem migração (enum é STRING). Devolução também reembolsa
  por tesouraria. Recibo mostra "M-Pesa"/"e-Mola". UI POS `askPayment` com os métodos + campo
  Referência; diálogo de devolução idem.
- Spec/harness: [docs/MOBILE_MONEY_SPEC.md](../docs/MOBILE_MONEY_SPEC.md) +
  [docs/MOBILE_MONEY_HARNESS.md](../docs/MOBILE_MONEY_HARNESS.md) (MM-01 auto, MM-50..53 manuais).
- Testes: `POSServiceTest` +1 (MPESA → tesouraria, não gaveta). `mvn test` → **200, 0 falhas**.
- **A seguir (3/3):** preços grosso vs retalho.

### Progresso — 2026-07-03 (reposição automática de stock — nova funcionalidade)

- **Sugestão do utilizador (1/3):** novo `ReorderService.suggestions(companyId)` — lista de produtos
  **abaixo do stock mínimo** (soma de todos os armazéns; produto sem stock = 0), com quantidade a
  encomendar **arredondada a caixas inteiras** (`unitsPerBox`), ordenada por urgência. Leitura pura
  (não cria encomendas). **API** `GET /api/purchases/reorder-suggestions`; **UI** nova aba
  "Reposição" no `ComprasPanel` (botão "Criar Encomenda" salta para a aba de encomendas).
- Spec/harness: [docs/REPOSICAO_AUTOMATICA_SPEC.md](../docs/REPOSICAO_AUTOMATICA_SPEC.md) +
  [docs/REPOSICAO_AUTOMATICA_HARNESS.md](../docs/REPOSICAO_AUTOMATICA_HARNESS.md) (RA-01..03 auto, RA-50..52 manuais).
- Testes: `ReorderServiceTest` (3). `mvn test` → **199, 0 falhas**.
- **A seguir (2/3, 3/3):** Mobile Money (M-Pesa/e-Mola) e preços grosso vs retalho.

### Progresso — 2026-07-02 (resiliência das ligações à BD — PC de balcão)

- **Lacuna operacional fechada:** com a app aberta e a máquina em suspensão longa, o pool Hikari
  mantinha ligações **mortas** ao PostgreSQL → gravações falhavam até reiniciar (visto em uso real).
  Config de resiliência no perfil **`desktop`** e espelhada em **`prod`**: `keepalive-time=120s`
  (sonda ligações ociosas), `max-lifetime=600s` (rotação), `connection-timeout=10s` (falha rápida),
  `validation-timeout=5s`; desktop com pool 5/min-idle 1. Sem tocar em código/Services/schema.
- Validado: desktop arranca com Hikari sem avisos de `keepalive/maxLifetime`; `mvn test` 196/0.
- Spec/harness: [docs/RESILIENCIA_LIGACOES_SPEC.md](../docs/RESILIENCIA_LIGACOES_SPEC.md) +
  [docs/RESILIENCIA_LIGACOES_HARNESS.md](../docs/RESILIENCIA_LIGACOES_HARNESS.md) (RL-01..RL-05 manuais).

### Progresso — 2026-07-01 (documento de inventário simplificado)

- **Pedido do utilizador:** o PDF de inventário passou a ter **só 6 colunas** — Referência · Código de
  Barras · Nome · Quantidade · **Caixas** (qtd ÷ und/caixa) · **Valor**. Removidas SKU, Armazém,
  Mínimo, Preço de Compra, Estado. **Valor a preço de VENDA** (`unitPrice`), por linha e no total.
  Rodapé reduzido a "Artigos no inventário" + "VALOR TOTAL DO STOCK". `InventoryReportPrintService`.

### Progresso — 2026-07-01 (venda ao grosso: helper "Caixas" na faturação)

- **Decisão (utilizador vende ao grosso):** a linha da **fatura** ganhou campo opcional **"Caixas"**
  que preenche a **Qtd (unidades) = caixas × unidades/caixa** do produto. **Cálculo de dinheiro
  continua por unidade** (`LineCalculator` intacto, IVA por unidade). **POS não é tocado** (retalho
  rápido à unidade). Entrada directa em unidades mantém-se (campo vazio). `ComercialPanel.applyInvoiceBoxes()`.
- Harness CX-09/CX-10. Helper **replicado na Encomenda a Cliente** (`applyOrderBoxes()`) — mesmo
  comportamento (caixas → Qtd em unidades, dinheiro por unidade).

### Progresso — 2026-07-01 (edição de produtos + entrada de stock por caixas)

- **Editar Produto (lacuna fechada):** existia `createProduct` mas **não havia como actualizar** um
  artigo já cadastrado. Novo `ComercialService.updateProduct(...)` (SKU imutável = identidade;
  referência/código de barras revalidam unicidade **excluindo o próprio**; não toca no stock;
  auditoria `PRODUCT_UPDATE`). **UI:** botão **"Editar Produto"** no topo do `StockPanel` → diálogo
  com selector de produto que **pré-preenche** o formulário (mesmos campos do cadastro, incluindo
  unidades/caixa, IVA, categoria e imagem). Testes: `ComercialServiceTest` 20 → **23**.
- **Entrada de stock por caixas:** ver abaixo.

### Progresso — 2026-07-01 (entrada de stock por caixas)

- **Pedido do utilizador:** dar entrada de mercadoria **por nº de caixas** (a loja arruma às caixas),
  mantendo faturação/reserva/guia/POS **em unidades**. Já existiam `Product.unitsPerBox`, o campo
  "Unidades por Caixa" no cadastro e a coluna "Qtd Caixas" no inventário — faltava o **caminho de
  entrada por caixas**.
- **Decisão:** a unidade interna de stock continua a **UNIDADE** (nada muda a jusante). A caixa é só
  camada de **entrada + visualização**. No `createBatchEntryDialog` (stock inicial **e** "Adicionar
  Lote/Validade" — mesmo método) o operador indica **Nº de Caixas + Unidades soltas**, vê as
  **unidades/caixa** do produto e o **Total (unidades)** calculado em tempo real
  (`total = caixas × unitsPerBox + soltas`); o movimento grava o total em unidades.
- Helpers `parseIntOrZero`/`parseDecimalOrZero` + `UIHelper.onTextChange` para recálculo ao vivo.
  Sem tocar em Services/DTOs (faturação/POS/guia/inventário inalterados).
- Spec/harness: [docs/CADASTRO_POR_CAIXAS_SPEC.md](../docs/CADASTRO_POR_CAIXAS_SPEC.md) +
  [docs/CADASTRO_POR_CAIXAS_HARNESS.md](../docs/CADASTRO_POR_CAIXAS_HARNESS.md) (CX-01..CX-08 manuais).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 196 testes, 0 falhas**.

### Progresso — 2026-07-01 (recepção parcial de encomenda a fornecedor)

- **Fecha a "Fase 4 (futuro)"** das compras: a recepção de encomenda deixou de ser tudo-ou-nada.
  - `PurchaseOrderLine.receivedQuantity` (migração `V19`) regista o já recebido; **em falta =
    quantity − receivedQuantity**. Novo estado `PARTIALLY_RECEIVED` (ciclo
    `ORDERED → PARTIALLY_RECEIVED* → RECEIVED` / `CANCELLED`).
  - `receivePartial(id, itens)`: recebe as quantidades indicadas por linha (entra stock só pela
    quantidade do acto, FEFO/lote, sem recontar o já recebido), valida `0 < qty ≤ emFalta`, recalcula
    o estado. `receiveOrder` passou a **receber o em falta** (de ORDERED ou PARTIALLY_RECEIVED) sem
    dupla entrada; `cancelOrder` aceita PARTIALLY_RECEIVED (stock recebido mantém-se). MANAGER/ADMIN +
    auditoria. Estado é **derivado** das linhas, nunca escrito à mão.
  - **API:** `POST /api/purchases/orders/{id}/receive-partial`. **UI:** botão "Receber Parcial…" no
    `ComprasPanel` (modal com tabela editável "A receber agora" por linha).
  - Spec/harness: [docs/RECECAO_PARCIAL_SPEC.md](../docs/RECECAO_PARCIAL_SPEC.md) +
    [docs/RECECAO_PARCIAL_HARNESS.md](../docs/RECECAO_PARCIAL_HARNESS.md) (RP-01..RP-12 automáticos,
    RP-50..RP-52 manuais). Testes: `PurchaseOrderServiceTest` 8 → **20**.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 193 testes, 0 falhas**.

### Progresso — 2026-07-01 (exportação fiscal de vendas — estrutura SAF-T)

- **Nova exportação fiscal de auditoria:** o sistema calculava o apuramento de IVA mas não
  exportava um ficheiro estruturado dos documentos de venda. Novo `FiscalSalesExportService`
  produz um **XML alinhado com a estrutura SAF-T** (`Header` / `MasterFiles` / `SourceDocuments` →
  `SalesInvoices`) para um período.
  - **Permissão** MANAGER/ADMIN + guarda multi-tenant (`requireCompany`). Inclui faturas emitidas
    (`APPROVED/PAID/PARTIALLY_PAID`) e **anuladas** (`CANCELLED`, com estado, sem somar aos totais);
    exclui `DRAFT/PENDING*/REJECTED`.
  - **Reutiliza os valores fiscais persistidos** na fatura (não recalcula impostos → não diverge da
    engine de faturação/POS). Escaping XML correcto, determinístico, totais conferíveis.
  - **API:** `GET /api/fiscal/saft?companyId&from&to` (`application/xml`). **UI:** botão
    "Exportar SAF-T (Vendas)" na aba IVA do `FiscalPanel` (usa o ano/mês selecionado, grava `.xml`
    via `JFileChooser`).
  - **Limite honesto:** segue a *estrutura* SAF-T mas **não é certificado** — validar contra a XSD
    oficial da AT-MZ antes de submissão (documentado na spec/harness, SF-51).
  - Spec/harness: [docs/FISCAL_SAFT_EXPORT_SPEC.md](../docs/FISCAL_SAFT_EXPORT_SPEC.md) +
    [docs/FISCAL_SAFT_EXPORT_HARNESS.md](../docs/FISCAL_SAFT_EXPORT_HARNESS.md) (SF-01..SF-14
    automáticos, SF-50..SF-52 manuais). Testes: `FiscalSalesExportServiceTest` (12).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 181 testes, 0 falhas**.

### Progresso — 2026-06-30 (backup físico restaurável — recuperação de desastres)

- **Lacuna fechada (parcial):** o `BackupService` existente grava um **dump JSON lógico lossy**
  (subconjunto de campos, relações achatadas) — bom para auditoria/verificação, **não restaurável**.
  Faltava o caminho de DR real. Novo `DatabaseBackupService` faz **backup físico via
  `pg_dump`/`pg_restore`** (formato custom `-Fc`), fidelidade total da instância.
  - `executePhysicalBackup()` (ADMIN): recusa BD não-PostgreSQL, password só por `PGPASSWORD` no
    ambiente do subprocesso (nunca na linha de comando), escreve `backups/multicore_<db>_<ts>.dump`.
  - `restorePhysicalBackup(path, confirmOverwrite)` (ADMIN, **destrutivo** → exige confirmação):
    `pg_restore --clean --if-exists --no-owner`.
  - Config: `backup.pg-bin-dir` (binários fora do PATH) e `backup.dir`.
  - **UI:** botão "Backup Físico (BD)" no `ConfigPanel` (aba Cópias de Segurança), via
    `runWithProgress`; descrição da aba corrigida (já não diz "base de dados em memória").
  - Spec/harness: [docs/BACKUP_RESTORE_SPEC.md](../docs/BACKUP_RESTORE_SPEC.md) +
    [docs/BACKUP_RESTORE_HARNESS.md](../docs/BACKUP_RESTORE_HARNESS.md) (BR-01..BR-12 automáticos,
    BR-50..BR-54 manuais — o round-trip real precisa de PostgreSQL + binários, fora de CI).
  - Testes: `DatabaseBackupServiceTest` (12: parsing JDBC, construção de comandos, guardas).
- **Ainda pendente para fechar o item de DR:** correr BR-50..BR-54 num ambiente separado (gerar
  `.dump` → restaurar em BD limpa → confirmar contagens idênticas). O software está pronto; falta a
  execução manual com PostgreSQL real.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 169 testes, 0 falhas**.

### Progresso — 2026-06-29/30 (polish de UI profissional transversal)

Várias iterações **só de apresentação** (sem tocar em Services/DTOs/regras), cada uma com spec+harness:

- **Grelha estilo Multicore** (`UIHelper.styleTable`): números alinhados à direita (qtd/preço/IVA/total) +
  cabeçalho com separadores; **calha de selecção** ▸ na margem esquerda (rowHeader, sem mexer no
  modelo de colunas). [POS_*]/grelha.
- **Cabeçalho POS compacto:** código de barras subiu para a linha dos selects; catálogo ganhou
  altura. [POS_CABECALHO_COMPACTO_SPEC](../docs/POS_CABECALHO_COMPACTO_SPEC.md).
- **RH — aba "Visão Geral":** 6 cards de KPI + 2 gráficos, reutilizando `KpiCard`/`SimpleBarChart`
  extraídos do Dashboard (DRY). [RH_VISAO_GERAL_SPEC](../docs/RH_VISAO_GERAL_SPEC.md).
- **Inputs/selects profissionais:** `FIELD_BG`/`BORDER` por tema + **realce de foco a acento** +
  combo achatado. [INPUTS_SELECTS_SPEC](../docs/INPUTS_SELECTS_SPEC.md).
- **Inspetor de detalhes** (duplo-clique) → `ModernFormDialog` só-leitura (`asReadOnly`).
  [INSPETOR_DETALHES_SPEC](../docs/INSPETOR_DETALHES_SPEC.md).
- **Aprovações:** inspector inline → **modal de decisão** (Aprovar/Rejeitar/Fechar);
  `ModernFormDialog.addActionButton`/`close`. [APROVACOES_MODAL_SPEC](../docs/APROVACOES_MODAL_SPEC.md).
- **Formulários inline → modal** (CRM Folha de Obra, Financeiro Recebimento, Config Utilizador +
  bug do combo de perfil corrigido). [FORMULARIOS_INLINE_MODAL_SPEC](../docs/FORMULARIOS_INLINE_MODAL_SPEC.md).
- **Prompts de dados → modal** via helpers `UIHelper.promptRequiredText`/`promptAmount` (motivos de
  anulação/rejeição, abrir/fechar caixa). Removido `StockPanel.createWarehouseDialog` V1 morto.
  [PROMPTS_MODAL_SPEC](../docs/PROMPTS_MODAL_SPEC.md).
- **Despesa RH** migrada para `ModernFormDialog`. [MODAIS_ICONES_SPEC](../docs/MODAIS_ICONES_SPEC.md).
- **Modais de despesa/submeter:** despesa do RH em modal.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**.

### Progresso — 2026-06-28 (gestão de categorias profissional + modal de pagamento premium)

- **Gestão de categorias profissionalizada** (tab Categorias do Stock, sobre o `ProductCategoryService`
  existente): tabela com **amostra de cor** (`ColorCellRenderer`), **contagem de produtos** por
  categoria, **pesquisa** por código/nome. Diálogo com **seletor de cor** (`JColorChooser` + amostra ao
  vivo + Limpar) num `ModernFormDialog` premium (ícone `fas-tags`). Activar/desactivar (sem apagar).
  Spec/harness: [docs/CATEGORIAS_GESTAO_SPEC.md](../docs/CATEGORIAS_GESTAO_SPEC.md) +
  [docs/CATEGORIAS_GESTAO_HARNESS.md](../docs/CATEGORIAS_GESTAO_HARNESS.md) (CT-01..10).
- **Modal de pagamento do POS premium:** `askPayment` migrou de `JOptionPane` para `ModernFormDialog`
  (ícone `fas-money-bill-wave`, subtítulo, botão "Confirmar Pagamento" `fas-check`); validação no
  `onSave` (mantém aberto em erro) — fim da recursão.
- **Carrinho POS:** container com `VScrollPanel` (acompanha a largura, scroll vertical quando falta
  altura) → a tabela deixa de colapsar para só o cabeçalho. Bloco **Subtotal s/ IVA + IVA** sempre
  visível por cima do TOTAL. Selector de vista (Venda POS | Histórico) na mesma linha que as acções de
  caixa (poupa espaço). Lupa de pesquisa **dentro** do input.

### Progresso — 2026-06-28 (catálogo POS em cards com imagem + modais premium)

- **Catálogo POS em cards com imagem:** o separador "Venda POS" passou a mostrar os produtos como
  **grid de cards** (imagem/marcador + nome + preço). **Clicar adiciona ao carrinho** (qtd 1, FEFO/
  promoção automáticos); clicar de novo **incrementa** (merge), como num carrinho web. O leitor de
  código de barras usa o mesmo caminho (`addProductToCart`). Removido o formulário detalhado antigo
  (combo de produto, qtd, desconto, lote, série, "Adicionar Artigo", `refreshFEFOHint`).
- **Imagem por produto (bytea):** `Product.imageData`, `ProductDTO.image`, migração `V18`,
  `ComercialService.updateProductImage`; cadastro de produto (StockPanel) ganhou **selector de imagem**
  com pré-visualização (auto-reduzida a 320px). Helpers `UIHelper.readScaledImage` / `imageIconFromBytes`.
- **Selects alinhados no topo:** Cliente/Armazém/Conta passaram para uma **barra superior compacta**
  (estilo web), libertando largura para catálogo + carrinho.
- Spec/harness: [docs/POS_CATALOGO_CARDS_SPEC.md](../docs/POS_CATALOGO_CARDS_SPEC.md) +
  [docs/POS_CATALOGO_CARDS_HARNESS.md](../docs/POS_CATALOGO_CARDS_HARNESS.md) (PC-01..11).
- **Modais premium + botões estilizados:** todos os ~21 modais legados (`JOptionPane`) migraram para
  `ModernFormDialog` (cabeçalho com badge+ícone+subtítulo, botões Cancelar/Confirmar com ícone, rodapé
  fixo). `createDialogForm` passou a **grelha de 2 colunas**. Lupa de pesquisa do POS **dentro** do input.
  Spec/harness: [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) (MI-01..16).

### Progresso — 2026-06-28 (desktop em PostgreSQL real + ícones nos modais + grelha Multicore)

- **Base de dados real no desktop:** o perfil `desktop` deixou de usar H2 em memória e passou a usar
  **PostgreSQL local persistente** (BD `multicore`, role dedicada `multicore`). Credenciais fora do git:
  password lida de `${DB_PASSWORD}` (variável de ambiente persistente da máquina).
  [application-desktop.properties](../src/main/resources/application-desktop.properties) com
  **Flyway dono do schema + Hibernate `validate`** (igual a prod).
- **`V17__sync_schema_with_entities.sql`:** fecha o desvio acumulado em dev (que corria `ddl-auto=update`,
  por isso as migrações estavam atrasadas face às entidades). Gerada a partir do diff do Hibernate:
  colunas em falta (`stock_transfers.approved_at/approved_by/rejection_reason`), precisão numérica
  `numeric(38,2)` em `purchase_orders`/`purchase_order_lines`, e 7 `UNIQUE` declaradas nas entidades
  (employees, payslips, payroll_bonuses, product_batches, stocks, tax_rates). **Reposto o caminho
  prod (Flyway+validate), que antes falhava.** Spec/harness:
  [docs/BD_POSTGRES_DESKTOP_SPEC.md](../docs/BD_POSTGRES_DESKTOP_SPEC.md) +
  [docs/BD_POSTGRES_DESKTOP_HARNESS.md](../docs/BD_POSTGRES_DESKTOP_HARNESS.md) (DB-01..06).
- **Ícones nos modais de formulário:** `ModernFormDialog` ganhou ícone contextual no título (deduzido do
  título via `iconForTitle`, domínio>verbo) + `setIconImage`, e `fas-times` no Cancelar. Cobre todos os
  `ModernFormDialog` sem tocar nos call sites. Vocabulário `multicore-icons` += Fornecedor/Categoria. Spec/harness:
  [docs/MODAIS_ICONES_SPEC.md](../docs/MODAIS_ICONES_SPEC.md) +
  [docs/MODAIS_ICONES_HARNESS.md](../docs/MODAIS_ICONES_HARNESS.md) (MI-01..08).
- **Tabelas em grelha estilo Multicore:** `UIHelper.styleTable` passou a desenhar linhas verticais + horizontais
  (grelha completa, `setIntercellSpacing(1,1)`, contorno na cor da grelha).
- **Pendente (legado):** modais baseados em `JOptionPane.showConfirmDialog` (Cadastrar Produto, Armazém,
  Ajuste) ainda sem iconografia própria — migrar para `ModernFormDialog` numa próxima iteração.

### Progresso — 2026-06-27 (IVA dinâmico no POS + altura uniforme botões/inputs)

- **IVA dinâmico por produto** (decisão do utilizador): novo FK `Product.taxRate → TaxRate`
  (entidade fiscal configurável já existente), migration `V16`. A taxa efetiva = taxa do produto,
  ou a padrão (`TaxRates.STANDARD_VAT` 16%) quando não definida. Cálculo continua na engine única
  `LineCalculator`. **Deixou de aplicar a constante hardcoded** no checkout do POS.
  - `ProductDTO` passou a expor `taxRateId/taxRate/taxRateLabel`; `createProduct` aceita `taxRateId`;
    formulário de produto (StockPanel) ganhou seletor **Taxa de IVA** (default 16%).
  - **POS UI:** carrinho com colunas **Líquido · IVA · Total** (IVA = "Isento" a 0% ou "valor (taxa%)"),
    rodapé **Subtotal s/ IVA · IVA** e **TOTAL A PAGAR = líquido + IVA** (pagamento/troco usam este total).
  - **Seed:** cesta básica isenta (arroz/açúcar/farinha/feijão), massa 5%, óleo 16% — demonstra taxas
    mistas numa só venda.
  - Spec/harness: [docs/POS_IVA_DINAMICO_SPEC.md](../docs/POS_IVA_DINAMICO_SPEC.md) +
    [docs/POS_IVA_DINAMICO_HARNESS.md](../docs/POS_IVA_DINAMICO_HARNESS.md) (IV-01..09). Testes:
    `POSServiceTest` +2 (isento / 16%). **Verificado visualmente** (Açúcar isento + Óleo 16% + Massa 5%).
- **Altura uniforme botões/inputs:** `ModernButton.getPreferredSize()` impõe altura mínima
  `FORM_CONTROL_HEIGHT` (38px), igual aos campos, mantendo a largura natural (com ícone, sem truncar).
- **Modais contidos na janela principal (mesmo ao arrastar):** `MainFrame.registerMainWindow(this)`
  regista a janela e instala **um listener global** (`AWTEventListener` `COMPONENT_MOVED`) que prende
  qualquer `Dialog` dentro da janela principal — não sai para fora nem ao arrastar (`clampInsideMain`).
  Ao abrir, `containWithinMain` limita a ~94% e centra; `ModernFormDialog` e `makeDialogScrollable`
  dimensionam-se pela janela principal (não pelo ecrã). Cobre os ~333 pontos de diálogo sem os tocar.
  Spec/harness: [docs/MODAIS_CONTIDOS_SPEC.md](../docs/MODAIS_CONTIDOS_SPEC.md) +
  [docs/MODAIS_CONTIDOS_HARNESS.md](../docs/MODAIS_CONTIDOS_HARNESS.md) (MD-01..06, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 157 testes, 0 falhas**. Verificado visualmente
  (carrinho com IVA, botões alinhados, modal "Cadastrar Produto" centrado e contido).

### Progresso — 2026-06-27 (polish profissional do ecrã POS)

- **`POSPanel` repolido** (só apresentação, sem mexer em Service/DTO/cálculos):
  - Formulário esquerdo **reposto ao topo** em `onPanelSelected()` → secção DOCUMENTO (Cliente/
    Armazém/Conta) deixa de aparecer cortada.
  - **Emojis 🔍 removidos** dos campos de pesquisa (não renderizavam sob Metal/Ocean); pista agora é
    **ícone vectorial** `fas-search` à esquerda (helper `searchRow`), padrão da barra de código de barras.
  - **Total em destaque**: faixa `ModernPanel` "TOTAL A PAGAR" + valor a 26px (`%,.2f MT`).
  - **Empty state do carrinho** via `CardLayout` (ícone + "Carrinho vazio" + dica), alternado em
    `updateCartTotal`/`refreshCartView`.
  - **Estado da caixa com ícone** (cadeado aberto/verde vs fechado/amarelo).
- Spec/harness: [docs/POS_UI_POLISH_SPEC.md](../docs/POS_UI_POLISH_SPEC.md) +
  [docs/POS_UI_POLISH_HARNESS.md](../docs/POS_UI_POLISH_HARNESS.md) (PU-01..08, manual).
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (formulários em modal responsivo)

- **`ModernFormDialog`** passou a ser o modal canónico: **scroll automático** do conteúdo,
  **responsivo** (≤92%×88% do ecrã, centrado), botão Gravar com ícone `fas-save` (removido o emoji).
- **Formulários de criação convertidos em modais**, deixando as tabelas a ecrã inteiro: Faturação
  (FT) «Nova Fatura…», Compras «Registar Compra…», Encomendas a Fornecedor «Nova Encomenda…».
  Cada submit lança em erro (modal fica aberto) e recarrega a lista em sucesso.
- Spec/harness: [docs/FORMULARIOS_MODAIS_SPEC.md](../docs/FORMULARIOS_MODAIS_SPEC.md) +
  [docs/FORMULARIOS_MODAIS_HARNESS.md](../docs/FORMULARIOS_MODAIS_HARNESS.md) (FM-01..08, manual).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas** (sem regressões).

### Progresso — 2026-06-26 (contas a pagar a fornecedor — Fase 4)

- **`Purchase.amountPaid`** (em dívida = total − pago), migration `V15` (backfill: compras antigas
  ficam pagas). **Compra a crédito**: `createPurchase` com `financeAccountId = null` não paga no acto.
- `findPayablesByCompany` (saldo > 0) + `registerSupplierPayment` (abate, cap no saldo, saída de
  tesouraria CREDIT, auditoria). API: `GET /api/purchases/payables`, `POST /api/purchases/{id}/pay`.
- **UI:** opção "— A crédito —" no combo de conta da compra + tab **Contas a Pagar** (lista com
  total em dívida + Registar Pagamento). Testes: `PurchaseServiceTest` AP-03..06.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 155 testes, 0 falhas**.

### Progresso — 2026-06-25 (compras & aprovisionamento profissional)

- **Gestão de fornecedores completa:** campos novos (telefone, contacto, **activo**), editar,
  activar/desactivar (soft-delete, MANAGER/ADMIN + auditado), pesquisar por nome/NUIT. Fornecedor
  inactivo bloqueado em compra/encomenda. Migration `V13`.
- **Encomenda de Fornecedor (`PurchaseOrder`, série `EC-F`):** novo workflow `ORDERED → RECEIVED /
  CANCELLED` (mirror da encomenda de cliente). **Não move stock até à recepção**; a recepção gera
  entrada `PURCHASE` por linha (FEFO/lote, bloqueio de lote vencido), MANAGER/ADMIN + auditado.
  Migration `V14`. Endpoints sob `/api/purchases/orders`.
- **UI (`ComprasPanel`):** tab «Encomendas a Fornecedor» (form + linhas + lista com Receber/Cancelar/
  pesquisa) e tab de fornecedores com editar/pesquisar/activar. **Categorias** ganharam ecrã de gestão
  (nova tab no `StockPanel` sobre o `ProductCategoryService`).
- Spec/harness: [docs/COMPRAS_APROVISIONAMENTO_SPEC.md](../docs/COMPRAS_APROVISIONAMENTO_SPEC.md) +
  [docs/COMPRAS_APROVISIONAMENTO_HARNESS.md](../docs/COMPRAS_APROVISIONAMENTO_HARNESS.md).
  Testes: `PurchaseServiceTest` (5) + `PurchaseOrderServiceTest` (8).
- **Fase 4 (futuro):** contas a pagar a fornecedor (saldo + pagamento → tesouraria) e recepção parcial.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 151 testes, 0 falhas**.

### Progresso — 2026-06-25 (UI faturação: tabela de linhas em largura total)

- Tab **Faturação (FT)** reorganizada com **split vertical**: formulário + faturas recentes em cima,
  **Linhas da Fatura em largura total** em baixo (divisor 50/50 aplicado no 1.º resize real — corrige
  o `setDividerLocation` que não pegava antes do componente ter altura).

### Progresso — 2026-06-25 (vista unificada de movimentos comerciais)

- **Dívida §7.3 fechada:** novo módulo de leitura agregada `modules/movimentos/` (DTO/enum/service/
  controller, sem entidade própria — reutiliza repositórios de `comercial`, padrão do `ReportService`).
  `MovimentosService.listar(companyId, query, from, to)` junta **fatura, encomenda, NC e ND** numa só
  lista, filtrável por **nº/cliente** (substring case-insensitive) e **período** (inclusivo), ordenada
  por **data desc**, com guarda multi-tenant (`requireCompany`).
- **UI:** nova tab "Movimentos" no `ComercialPanel` com filtros (pesquisar/de/até) e rodapé
  contagem+soma. **API:** `GET /api/movimentos`.
- Spec/harness: [docs/MOVIMENTOS_UNIFICADOS_SPEC.md](../docs/MOVIMENTOS_UNIFICADOS_SPEC.md) +
  [docs/MOVIMENTOS_UNIFICADOS_HARNESS.md](../docs/MOVIMENTOS_UNIFICADOS_HARNESS.md). Testes:
  `MovimentosServiceTest` (7: MU-01..MU-07).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 138 testes, 0 falhas**.

### Progresso — 2026-06-25 (recibo pontilhado + loading reutilizável no checkout/PDF)

- **Recibo térmico pontilhado:** separadores passaram de traços a **linhas pontilhadas reais**
  (`DottedLineSeparator`) e as linhas da tabela ganharam **borda inferior pontilhada** (evento de
  célula com `setLineDash`) — aspecto de recibo térmico. `ReceiptPrintService`.
- **Loading profissional reutilizável:** novo `UIHelper.runWithProgress(...)` corre tarefas
  demoradas num `SwingWorker` com diálogo modal "a processar…" + barra indeterminada. Aplicado ao
  **checkout do POS** ("A finalizar venda…") e à **geração/reimpressão de recibos** ("A gerar
  recibo…"). Padrão pronto para outros PDFs/cargas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (promoções de loja + login com loading + maximizar)

- **Módulo `promotions` (sugestão #3):** novo domínio completo (model/enum/repo/dto/service/controller)
  seguindo a arquitetura. Tipos: **percentagem** (produto ou categoria) e **leve X, pague Y** (produto).
  - `PromotionService.bestPromotion(...)` traduz a promoção activa no **desconto % efectivo** por
    linha — reutiliza o checkout existente sem mexer no cálculo do POS/faturação.
  - **POS:** ao adicionar artigo sem desconto manual, aplica automaticamente a melhor promoção e
    marca "Promo: <nome>" na linha do carrinho.
  - **UI:** nova tab "Promoções" no `ComercialPanel` (`PromotionsPanel`) — listar, criar, activar/
    desactivar (permissão MANAGER/ADMIN, auditado).
  - Migration `V12__store_promotions.sql`. Testes: `PromotionServiceTest` (8).
- **Login com loading profissional:** autenticação passou a correr em `SwingWorker` (não congela o
  EDT) com barra de progresso indeterminada reutilizável (`UIHelper.createBusyBar`) e estado
  "A entrar…". Campos/botão bloqueados durante a chamada.
- **Arranque maximizado** após login (`DesktopLauncher`), com estado preservado na troca de tema.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 131 testes, 0 falhas**.

### Progresso — 2026-06-25 (alerta de validade proativo + UI tema/POS)

- **Alerta de validade (sugestão #4 p/ loja):** lotes com stock vencidos ou a vencer em ≤30 dias
  passam a ser **proativos**, não só consultáveis. `InventoryService.findExpiringBatches(companyId,
  daysAhead)` (com guarda de empresa) delega em `ProductBatchService.findExpiringByCompany`.
  - **Dashboard:** novo cartão "ALERTAS DE VALIDADE" (visível no login) com total + repartição
    "X vencidos · Y a vencer (≤30d)". Grelha de KPIs passou a 3 colunas/linhas automáticas.
  - **Stock › Lotes & Validades:** resumo proativo no topo (vermelho se há vencidos) e **cor de
    urgência** na coluna Estado (VENCIDO vermelho, VENCE EM BREVE amarelo) via `UIHelper.styleTable`.
  - Testes: `InventoryServiceTest` (2: corte hoje+dias / guarda de empresa).
- **UI (sessão anterior):** barra de topo passou a acompanhar o tema claro/escuro; POS com
  formulário/carrinho em `JSplitPane` redimensionável; dropdown dos combos legível em tema claro;
  botões dos diálogos legíveis (gradiente Metal achatado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 123 testes, 0 falhas**.

### Progresso — 2026-06-20 (dívida técnica + cobertura de testes)

- **Nota de Débito** alinhada com `DocumentSeries`: nova série `ND`, número sequencial gapless via
  `DocumentNumberService.next(...)` em vez de `"ND-" + timestamp`. (resolve dívida §7.2 de MOVIMENTOS_COMERCIAIS.md)
- **Cobertura de testes** dos Services críticos que faltavam ao harness:
  - `POSServiceTest` (10): checkout sem sessão, via legada vs multi-método, fiado parcial,
    numerário+cartão sem dupla contagem, fecho de caixa (sem diferença / diferença exige permissão / depósito).
  - `CreditNoteServiceTest` (8): RETURN repõe stock só na aprovação, limites de quantidade/valor, permissão.
  - `DocumentNumberServiceTest` (6): sequência gapless, séries independentes, corrida na criação, série ND.
- **BackupService confirmado**: faz export real (dump JSON de todas as coleções por empresa) — não é
  placeholder. Só não tem restore programático (por design; restore é ao nível de BD em ambiente separado).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 71 testes, 0 falhas**.

### Progresso — 2026-06-20 (faturação directa + lote vencido + mais testes)

- **Faturação directa (decisão do utilizador):** `ComercialService.createInvoice` deixou de passar
  sempre pela Engine de Aprovações. Agora exige perfil **MANAGER/ADMIN**, emite a fatura já **APPROVED**
  e baixa stock no acto. **Só desconto >10%** mantém o caminho `PENDING_DISCOUNT_APPROVAL` → aprovação
  do gerente (stock baixa na aprovação via callback). Sem dupla baixa de stock.
- **Lote vencido (RS-12 / spec §4):** `ProductBatchService.addToBatch` bloqueia entrada de stock com
  validade já no passado (validade = hoje ainda entra; sem validade não bloqueia). Guarda todas as
  entradas porque compra/ENTRY passam por `addToBatch`.
- **Novos testes:** `ComercialServiceTest` (8) e 3 cenários de lote vencido em `ProductBatchServiceTest`.
  `MulticoreServicesTest.testDiscountApprovalThreshold` actualizado para o novo fluxo (5% → APPROVED).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 82 testes, 0 falhas**.

### Progresso — 2026-06-21 (segurança/roles por API)

- **Confirmado:** o `SecurityInterceptor` já impõe token+empresa em todo o `/api/**` (excepto
  `/api/auth/login`): 401 sem token, 403 sem acesso à empresa, e resolve o role por empresa. A spec §9
  está satisfeita ao nível do interceptor — o filtro Spring permissivo **não** é uma falha real.
- **Novo teste:** `SecurityApiIntegrationTest` (4) valida ponta-a-ponta pela API: 401 sem token,
  403 empresa sem acesso, e o role gate ao faturar (EMPLOYEE bloqueado / ADMIN passa). Fecha o item
  "login, tenant e roles testados por API" do harness.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 86 testes, 0 falhas**.

### Progresso — 2026-06-21 (localizar documento de origem por pesquisa, estilo Multicore)

- **Faturar a partir de encomenda** e **NC/ND a partir da fatura** passaram a permitir **pesquisar o
  documento de origem por nº ou cliente** (BUSINESS_FLOWS passo 1 "documento origem é localizado").
- Backend: `ComercialService.searchInvoices(query)` e `searchPendingOrders(query)` (filtro substring
  case-insensitive por nº/cliente, empresa activa) + helper `matches()`. Lógica no Service, UI fina.
- UI: campo "Pesquisar (nº ou cliente)" nos 3 diálogos do `ComercialPanel` (Faturar Encomenda, Emitir
  NC, Emitir ND) que filtra a lista ao escrever. Novo helper reutilizável `UIHelper.onTextChange(...)`.
  Faturação manual directa e por encomenda **ambas mantidas**.
- Testes: `ComercialServiceTest` (5 novos) para as pesquisas.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 91 testes, 0 falhas**.

### Progresso — 2026-06-21 (bug: encomenda não chegava à aprovação)

- **Causa:** `ComercialService.createOrder` guardava a encomenda como `PENDING` mas **nunca a
  submetia** à Engine de Aprovações — só faturas (desconto >10%) e despesas lá chegavam.
- **Fix:** a encomenda nasce `PENDING_APPROVAL` e é submetida via `approvalService.submitRequest("ORDER", …)`.
  Novo `OrderApprovalCallback`: aprovado → `PENDING` (faturável); rejeitado → `CANCELLED`. `billOrder`
  já exige `PENDING`, logo só fatura encomendas aprovadas (aprovar → depois faturar).
- UI: mensagem de criação passa a indicar "Submetida para aprovação"; a área de aprovação mostra o
  tipo em PT ("Encomenda"/"Fatura") via `humanType(...)`.
- Testes: `OrderApprovalCallbackTest` (4) + `createOrder` submete aprovação (`ComercialServiceTest`);
  `MulticoreServicesTest` actualizado (aprovar a encomenda antes de faturar).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 96 testes, 0 falhas**.

### Progresso — 2026-06-21 (cancelar encomenda)

- `ComercialService.cancelOrder(id, reason)` (mirror de `cancelInvoice`): exige MANAGER/ADMIN e motivo,
  bloqueia encomenda já faturada/cancelada, fecha o pedido de aprovação aberto e audita (`ORDER_CANCEL`).
- `ComercialService.searchCancellableOrders(query)` + `ApprovalService.cancelPendingForDocument(...)`.
- UI: botão "Cancelar Encomenda…" na tab Encomendas → diálogo com pesquisa por nº/cliente + motivo.
- Testes: `ComercialServiceTest` (cancelOrder + searchCancellableOrders) e `ApprovalServiceTest`.
- Verificação: `mvn clean test` → **BUILD SUCCESS, 103 testes, 0 falhas**.

### Progresso — 2026-06-21 (recibo POS: nome + valor pago/troco; UI)

- **Recibo térmico POS** (spec §4 "troco para numerário", RS-05): o nome do comprador passa a ser
  gravado na fatura (`Invoice.customerName`, migration `V9`) e impresso; o `ReceiptPrintService` mostra
  bloco de pagamento por método + **Valor pago** e **Troco**. UI: diálogo de pagamento no checkout
  (`POSPanel.askPayment`) recolhe método e, em numerário, **valor entregue com troco em tempo real**;
  passa a usar a via multi-pagamento (`PaymentEntry` com tendered/change). Bónus: cartão/transferência
  já vão à tesouraria sem contar como numerário na gaveta. Testes: `POSServiceTest` (+2 → 105).
- **Marca com empresa activa:** topo mantém MULTICORE e mostra o nome da empresa por baixo; role
  traduzido para PT discreto (`UIHelper.humanRole`: Administrador/Gestor/Funcionário).
- **Bug do seletor de empresa:** `styleComboBox` substituía o renderer e mostrava `CompanyAccess[...]`.
  Corrigido reaplicando o renderer depois de `styleComboBox` (`MainFrame.applyCompanyRenderer`).
- **Navegação no topo (decisão do utilizador):** sidebar esquerda → **barra de topo só-ícones**
  (`TopNavBar` + `TopNavItem`, ícones via `UIHelper.icon`, tooltip por módulo, quebra para 2ª linha
  via `WrapLayout`), libertando largura para tabelas/formulários. `CollapsibleSidebar`/`SidebarNavItem`
  ficaram sem uso (não removidos).
- **Scroll no formulário de compra:** "Registar Compra (Entrada Stock)" ganhou scroll vertical via
  host `Scrollable` (acompanha largura, não altura) dentro de `JScrollPane` no `ComprasPanel`.
- **Cadastro de cliente fora das Faturas:** removida a tab "Registar Cliente" do `ComercialPanel`
  (e código sem uso: `createRegistarClienteTab`/`registerClient`/4 campos). Gestão de clientes vive
  só na área Clientes (`ClientesPanel`); o diálogo rápido "+ Novo" do POS mantém-se.
- **Nota de arranque desktop:** o `-Dspring-boot.run.main-class` não sobrepõe o `<mainClass>` literal do
  pom; README actualizado para arrancar o desktop via `java -cp`.
- Verificação: `mvn clean compile` → SUCCESS; `mvn test` → **105 testes, 0 falhas**.

---

## Foco em curso

Fechar lacunas para uso real em loja/mercearia:

- Spec: [docs/RETAIL_STORE_SPEC.md](../docs/RETAIL_STORE_SPEC.md)
- Harness: [docs/RETAIL_STORE_HARNESS.md](../docs/RETAIL_STORE_HARNESS.md)
- Task faseada: [tasks/retail_store_readiness.md](retail_store_readiness.md)

Prioridade imediata: executar o harness RS-01 a RS-22 com dados reais de loja, validar impressora/leitor/gaveta e testar restore num ambiente separado.

### Progresso — 2026-06-22 (colunas de linha dos documentos comerciais)

- **Fatura, encomenda e nota de crédito** passaram a mostrar, por linha: **código de barras, referência,
  descrição, validade (do lote), qtd, preço unit., IVA e subtotal líquido** — via o renderizador
  partilhado `LineItemsTableRenderer` (8 colunas canónicas; subtotal = `LineCalculator.net`).
- Novo `LineRowMapper` (DRY) resolve barcode/ref/descrição do `Product` e a validade via novo
  `ProductBatchRepository.findFirstByProductIdAndBatchNumberOrderByExpirationDateAsc`. Os 3 serviços
  de impressão deixaram de montar linhas à mão.
- Spec/harness: [docs/DOCUMENT_LINE_COLUMNS_SPEC.md](../docs/DOCUMENT_LINE_COLUMNS_SPEC.md) +
  [docs/DOCUMENT_LINE_COLUMNS_HARNESS.md](../docs/DOCUMENT_LINE_COLUMNS_HARNESS.md). ND é baseada em
  valor (sem linhas de artigo).
- **Nova Guia de Remessa**: `GuideRemittancePrintService.render(invoiceId)` gera a guia a partir da
  fatura (mesmas 8 colunas + bloco de transporte/assinaturas, ref. `GR-<nºfatura>` sem consumir
  numeração). Endpoint `GET /api/print/guide/{invoiceId}` + botão "Imprimir Guia" no `ComercialPanel`.
- Teste: `LineItemsTableRendererTest` (3). Verificação: `mvn clean test` → **121 testes, 0 falhas**.

### Backlog — RH/Folha (avaliação 2026-06-22)

Módulo `mz.multicore.erp.modules.hr` está avançado (motor fiscal IRPS/INSS, recibos, despesas, férias,
faltas, UI + PDF) mas **não pronto para produção**. Spec-alvo e harness criados:
- Spec: [docs/HR_PAYROLL_SPEC.md](../docs/HR_PAYROLL_SPEC.md)
- Harness: [docs/HR_PAYROLL_HARNESS.md](../docs/HR_PAYROLL_HARNESS.md) (cenários RH-01..RH-25 + punch list)

Lacunas prioritárias: (1) auditoria ausente no RH; (2) nº de recibo por timestamp em vez de
`DocumentNumberService` gapless; (3) marcar recibo pago não gera saída de tesouraria; (4) mapa
fiscal e config de impostos sem endpoint/PDF; (5) férias sem saldo e `decideVacation` confia no
`decidedBy` do body; (6) faltas não descontam no recibo; (7) 13.º mês e subsídio de férias em falta;
(8) só 6 testes para um módulo de dinheiro+impostos (alvo ~30+).

**Progresso RH — 2026-06-22 (itens 1–3 da punch list):**
- (1) **Auditoria** em todo o RH: `AuditLogService` injectado em `HRService`, audita
  `EMPLOYEE_CREATE/UPDATE/STATUS`, `PAYSLIP_ISSUE/PAID/CANCEL`, `PAYROLL_PROCESS`, `VACATION_DECISION`.
- (2) **Nº de recibo gapless**: série `REC` via `DocumentNumberService.next` (fim do timestamp).
- (3) **Líquido → tesouraria** em `markPayslipPaid` via novo `FinanceService.registerAutoPayout`
  (refactor DRY de `registerAutoExpensePayout`).
- (4) **Mapa fiscal + config de impostos via API/PDF**: `HRController` expõe
  `GET /payroll/fiscal-summary/{year}/{month}` e `GET/POST /payroll/tax-config`; novo
  `PayrollFiscalMapPrintService` (PDF mapa INSS/IRPS) + botão "Imprimir Mapa Fiscal" no `FiscalPanel`;
  novo `HRApiIntegrationTest` (4: 401/403/200).
- (5) **Férias: saldo + decisor seguro**: direito anual 22 dias, saldo = direito − reservados
  (`VacationRepository.sumReservedDays`), `submitVacation` bloqueia acima do saldo; `decideVacation`
  exige MANAGER/ADMIN, resolve decisor por `CurrentUserContext` (deixou de confiar no `decidedBy` do
  body) e exige motivo na rejeição. Testes: `HRServiceTest` (+2).
- (6) **Faltas não remuneradas descontam no recibo**: novo campo `Payslip.absenceDeduction`
  (migration `V10`); `createPayslip` desconta faltas `UNJUSTIFIED` que se sobrepõem ao mês
  (valor/dia = salário base / 30), reflectido no líquido, `PayslipDTO` e PDF. Teste: `HRServiceTest` (+1).
- (7) **13.º mês + subsídio de férias (cálculo + pagamento)**: novo `PayrollBonusService` calcula
  (`thirteenthMonth`/`vacationAllowance`) e **paga de forma persistida e idempotente** via nova
  entidade `PayrollBonus` (migration `V11`, unique `employee+type+year+reference`): `payThirteenthMonth`
  /`payVacationAllowance` exigem MANAGER/ADMIN, saída de tesouraria + auditoria. Endpoints GET (calcular)
  e POST `/pay`. `PayrollBonusServiceTest` (6).
- Verificação: `mvn clean test` → **BUILD SUCCESS, 118 testes, 0 falhas**.
- **RH: punch list 1–7 fechada.** Cobertura RH = 19 testes próprios. Item 8 (cobertura) substancialmente
  cumprido. Falta apenas (opcional) UI desktop para 13.º/subsídio de férias — backend completo.

### Progresso da Fase 1 — 2026-06-17

- Criado `PermissionGuard` central para exigir `MANAGER/ADMIN` ou `ADMIN`.
- Aplicada permissão em sangria, fecho de caixa com diferença, anulação de factura/recibo, aprovação/rejeição/cancelamento de notas e aprovação/rejeição de transferências.
- Auditados fecho de caixa, sangria/suprimento, anulações, notas e transferências.
- Verificado que o desktop propaga utilizador/role/empresa para `CurrentUserContext` e que o client HTTP envia `X-Company-Id`.
- Verificação técnica: `mvn -q -DskipTests compile` e `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest" test` passaram.

### Progresso das Fases 2-5 — 2026-06-17

- POS: criado `POST /api/pos/returns` para devolução por nota de crédito `RETURN`, com reembolso CASH/CARD/BANK_TRANSFER/CREDIT.
- Produtos: adicionados `ProductSaleType` e `stockTracked`; linhas de factura/encomenda/POS migradas para `BigDecimal`; POS Swing aceita quantidade decimal.
- Stock: criado `POST /api/inventory/adjustments` para contagem/ajuste com motivo, permissão e auditoria.
- Relatórios: criado `GET /api/reports/daily-store` com vendas do dia, fiado em aberto, pagamentos por método, movimentos de caixa e top produtos.
- Migration: criada `V8__retail_product_sale_type_and_decimal_quantities.sql`.
- Verificação técnica: `mvn -q -DskipTests compile` passou; `mvn -q "-Dtest=PermissionGuardTest,StockTransferServiceTest,MulticoreServicesTest" test` passou.

### Progresso final de prontidão loja/mercearia — 2026-06-17

- POS Swing: devolução/troca operacional ligada ao histórico de vendas, com motivo, quantidade devolvida, método de reembolso e armazém.
- Stock Swing: ajuste passou a ser contagem física com quantidade contada e motivo, usando o Service auditado.
- Relatórios: `GET /api/reports/daily-store` passou a incluir vendas por operador e margem bruta por produto.
- Backup: criado verificador não destrutivo de backup JSON, botão "Verificar Backup" no painel de Configurações e teste unitário.
- Verificação técnica final: `mvn -q clean compile` passou; `mvn -q test` passou.

### Ainda pendente para declarar loja real pronta em ambiente físico

- Teste manual do harness RS-01 a RS-22 em ambiente real.
- Restore de backup testado em ambiente separado.
- Validação de impressora, leitor, gaveta e decisão de balança/etiquetas.

## Feito nas últimas iterações

### Funcionalidade — Validades & FEFO
- Backend: `ProductBatchService.findNextFEFO(productId, warehouseId)` exposto via `InventoryService`.
- UI:
  - [StockPanel](src/main/java/mz/multicore/erp/gui/StockPanel.java) — botão "Adicionar Lote/Validade" + diálogo dedicado; chain após "Cadastrar Produto".
  - [POSPanel](src/main/java/mz/multicore/erp/gui/POSPanel.java) — campos Lote+Validade FEFO read-only auto-preenchidos.
  - [ComercialPanel](src/main/java/mz/multicore/erp/gui/ComercialPanel.java) — Faturas e Encomendas com Lote/Validade FEFO read-only.
  - Diálogo de transferência ganhou colunas Lote+Validade FEFO recalculadas.

### Documentação (spec-driven harness)
- `README.md`, `ARCHITECTURE.md`, `CONVENTIONS.md`, `CLAUDE.md` criados.
- `ARCHITECTURAL_GUIDELINES.md` + `ARCHITECTURE_SEPARATION.md` consolidados e removidos.

### Infra "production-ready"
- **Lombok**: [lombok.config](lombok.config) — `stopBubbling`, marca métodos gerados, proíbe `@Data`/`@AllArgsConstructor`/`@Builder`. Setup IDE documentado em [CONVENTIONS.md §3](CONVENTIONS.md#3-lombok).
- **Handler global**: confirmado [GlobalExceptionHandler](src/main/java/mz/multicore/erp/architecture/exception/GlobalExceptionHandler.java) já existia (BusinessRule → 400, Validation → 400 com mapa, fallback → 500).
- **Flyway + PostgreSQL**: dependências em [pom.xml](pom.xml); [application-prod.properties](src/main/resources/application-prod.properties) com `ddl-auto=validate`, Flyway ON, vars `DB_URL/DB_USER/DB_PASSWORD`. Pasta [db/migration/](src/main/resources/db/migration/) com README explicando como gerar `V1__init.sql` a partir das entidades JPA.
- **Spring Security scaffold**: [SecurityConfig](src/main/java/mz/multicore/erp/architecture/security/SecurityConfig.java) — BCryptPasswordEncoder + filter chain permissiva (não quebra desktop). [AppUserService](src/main/java/mz/multicore/erp/modules/users/service/AppUserService.java) migrado para BCrypt com fallback de migração suave (passwords em texto-plano legadas continuam a autenticar e são re-encriptadas na próxima autenticação).
- **OpenAPI / Swagger**: dependência `springdoc-openapi-starter-webmvc-ui` adicionada. Em dev: `http://localhost:8080/swagger-ui.html`. Desactivado em prod até haver autenticação para a UI.
- **CI**: [.github/workflows/build.yml](.github/workflows/build.yml) — `mvn clean compile`, `mvn test`, `mvn package` em cada push/PR para `main`.
- **Testes unitários**: [ProductBatchServiceTest](src/test/java/mz/multicore/erp/modules/inventory/service/ProductBatchServiceTest.java) — 9 testes com Mockito a cobrir `findNextFEFO`, `consumeFEFO` (single batch / multi-batch / stock insuficiente / qty inválida) e `addToBatch` (novo / acumular / qty inválida). **9/9 verde.**

## Por validar manualmente (não posso fazer como agente)

- [ ] Cadastrar produto novo → confirmar prompt "Adicionar stock inicial".
- [ ] Adicionar 2 lotes do mesmo produto com validades diferentes → confirmar FEFO escolhe o mais próximo.
- [ ] POS: vender produto → confirmar Lote/Validade FEFO mostra o lote correcto e o movimento consome-o.
- [ ] Encomenda + Fatura: confirmar Lote/Validade FEFO refresca ao mudar armazém.
- [ ] Transferência multi-linha: confirmar colunas FEFO actualizam ao trocar armazém de origem.
- [ ] Autenticar com utilizador antigo (password em texto-plano) → confirmar que entra E que o hash na BD passou a `$2a$...`.
- [ ] Abrir `http://localhost:8080/swagger-ui.html` em dev → confirmar que lista todos os endpoints.

## Por fazer antes de produção real

1. ~~**Gerar V1__init.sql**~~ — já existe baseline `V1__init.sql` + V2..V8 em [db/migration/](src/main/resources/db/migration/). Falta aplicar/validar em PostgreSQL limpo (checklist do harness).
2. ~~**Restringir Security**~~ — **confirmado (2026-06-21)**: o `SecurityInterceptor` já impõe token+empresa+role em todo o `/api/**` (401/403), validado por `SecurityApiIntegrationTest`. O `.anyRequest().permitAll()` do filtro Spring é redundante (a guarda é o interceptor), não uma falha. Endurecer o filtro Spring fica como hardening opcional, não bloqueante.
3. ~~**Endpoints `/api/auth/login`**~~ — existe e está coberto (`AuthControllerIntegrationTest`).
4. ~~**Cobertura de testes** dos Services críticos~~ — **feito (2026-06-20)** para `POSService.checkout/closeSession`, `CreditNoteService`, `DocumentNumberService`. Falta `ComercialService.issueInvoice` e o cenário lote vencido (RS-12).
5. ~~**Backups reais**~~ — confirmado: `BackupService.executeBackup()` faz export real. Restore real continua a exigir ambiente separado (ponto manual do harness).

## Decisões tomadas

- Validades **não** têm tabela autónoma — pertencem sempre a um lote (`ProductBatch`).
- Lote/Validade no UI **read-only** — FEFO decide. Backend volta a aplicar FEFO em transacção mesmo que o UI passe `batchNumber`.
- Spring Security é scaffold **permissivo** por agora — restringir endpoints só quando houver login HTTP real, para não quebrar o desktop que ainda chama Services directamente.
- Backend puro de dev (`application.properties`, `mvn spring-boot:run`) continua em **H2 + `ddl-auto=update`**.
  O **desktop** (perfil `desktop`) usa **PostgreSQL local + Flyway + `validate`**, igual a prod (desde 2026-06-28).
  Prod com PostgreSQL gerido externamente + Flyway + `validate`.

## Estado de build

```
mvn clean compile   → BUILD SUCCESS
mvn clean test      → BUILD SUCCESS, 193 testes, 0 falhas (2026-07-01)
```

Diagnostics Lombok no IDE (`cannot find symbol: getX()`) são **ruído**. Critério único: `mvn compile`.

### Consistência profissional da UI Swing — 2026-08-09

- Criadas a especificação `docs/UI_CONSISTENCIA_PROFISSIONAL_SPEC.md` e o harness
  `docs/UI_CONSISTENCIA_PROFISSIONAL_HARNESS.md`.
- Uniformizados inputs tipados, selects, botões, tabelas, estados vazios/loading, acessibilidade e
  submissões assíncronas com protecção contra duplo clique e respostas de empresa obsoletas.
- Removidas chamadas remotas síncronas identificadas nos fluxos prioritários de POS, Stock,
  Comercial, Compras, RH, CRM, Financeiro e Configuração.
- Decompostos os seis painéis prioritários, todos agora abaixo de 1.000 linhas; o limite está
  protegido por `UiPanelDecompositionTest`.
- `mvn dependency:analyze` revisto: starters Spring Boot e drivers runtime reportados como unused
  são necessários por boot/autoconfiguração; nenhuma dependência declarada pôde ser removida com
  segurança. A redução efectuada foi de acoplamento interno da UI.
- Verificação: harness focado verde; `mvn clean test` verde com **391 testes, 0 falhas, 0 erros e
  0 ignorados**.
- Pendente apenas a evidência manual UI-50..62 em Windows real (escalas, temas, API lenta e
  periféricos POS), conforme o harness; não é substituída por testes headless.

### Layout responsivo do POS — 2026-08-11

- Catálogo/carrinho passam a iniciar em 36/64, com mínimos operacionais de 380/650 px.
- A tabela preserva as larguras das oito colunas com scroll horizontal abaixo de 900 px e volta a
  preencher o viewport quando existe largura confortável.
- Totais e acções de checkout permanecem fixos; apenas as linhas da tabela fazem scroll.
- Spec e harness: `docs/POS_LAYOUT_RESPONSIVO_SPEC.md` e `docs/POS_LAYOUT_RESPONSIVO_HARNESS.md`.
# Carrinho operacional do POS

- O carrinho foi compactado para seis colunas essenciais, eliminando a rolagem horizontal no
  viewport operacional de 620 px.
- Produtos adicionados ou incrementados ficam seleccionados e visíveis automaticamente.
- Nova barra de quantidade oferece diminuir, editar (F6) e aumentar, mantendo totais e checkout fixos.
- Promoção, lote e série permanecem acessíveis no tooltip da linha.
- Correcção visual: a tabela permanece auto-ajustável abaixo de 620 px e reserva altura para pelo
  menos três linhas; "Mais opções" abre um diálogo sem comprimir o workspace.
- Correcção de altura: pesquisa/selecção de cliente ficam na mesma linha; subtotal, IVA e total
  foram unidos numa faixa; Fiado passou para a linha de acções, libertando o corpo da tabela.
- Ritmo vertical compactado no POS: margem externa 14 px, secções 6 px e cartão 8 px; inputs sobem
  para junto das acções de caixa e o espaço recuperado aumenta o viewport do carrinho.
- Cabeçalho POS final em linha única: Pesquisa, Cliente, Armazém, Conta e Código de barras usam
  larguras responsivas de 20/22/16/18/24%; removido o fluxo "Mais opções".
- Catálogo POS paginado no servidor em blocos de 36, com pesquisa/disponibilidade antes da
  transferência, debounce de 300 ms e navegação Anterior/Próximo. Scanner consulta endpoint directo
  para continuar independente da página actual.
- Corrigida activação operacional: uma instância antiga do backend ficou temporariamente na porta
  8080 durante o reinício. Endpoint verificado ao vivo e coberto por novo teste HTTP autenticado.
- Spec e harness: `docs/POS_CARRINHO_OPERACIONAL_SPEC.md` e
  `docs/POS_CARRINHO_OPERACIONAL_HARNESS.md`.
- Catálogo POS passa a abrir em **Todos**: produtos esgotados aparecem atenuados, etiquetados e sem
  clique; filtro **Disponíveis** preserva a vista rápida. O estado continua vindo do endpoint
  canónico de vendáveis e scanner/balança também bloqueiam esgotados.
- Spec/harness: `docs/POS_CATALOGO_ESTADO_STOCK_SPEC.md` e
  `docs/POS_CATALOGO_ESTADO_STOCK_HARNESS.md`.

### Paginação uniforme das tabelas — 2026-08-16

- Listagens Swing carregadas integralmente passam a receber paginação local central (25/50/100/200),
  aplicada depois dos filtros e recalculada com alterações do modelo.
- Listagens de crescimento elevado mantêm paginação no servidor via `TablePager`/`PageResponse`;
  tabelas transaccionais (carrinho, linhas e diálogos) permanecem contínuas.
- Spec/harness: `docs/TABELAS_PAGINACAO_UNIFORME_SPEC.md` e
  `docs/TABELAS_PAGINACAO_UNIFORME_HARNESS.md`.
- Navegação lateral externa refinada: início/Page Up/Page Down/fim com nomes acessíveis, tooltips
  claros e desactivação automática nos limites da lista.
- Categorias: removida a lupa externa duplicada; o `SearchField` mantém uma única lupa integrada.
- POS: hierarquia cromática semântica aplicada aos botões (verde, azul, âmbar, vermelho e grafite),
  substituindo acções operacionais que pareciam pretas.
- Paginação: controlos separados por 8 px e margem vertical de 10 px antes das acções inferiores.
- Backup automático: deixa de tentar `pg_dump` no backend H2; a execução interactiva usa backup
  lógico JSON, enquanto PostgreSQL mantém o `.dump` físico restaurável.
# Editores de documentos pré-emissão (2026-09-27)

- Cotações em `DRAFT` e encomendas a fornecedor em `ORDERED` sem recepção usam editor de página
  inteira com `DocumentEditorHost`, a mesma experiência para criar, editar e consultar.
- As tabelas de linhas permitem editar/remover no próprio editor; pesquisa de produtos usa o
  componente pesquisável e as acções ficam no topo do card.
- Contratos `PUT` usam versão optimista; Services recalculam totais, protegem estado/tenant e
  auditam `QUOTATION_UPDATE` e `PURCHASE_ORDER_UPDATE`.
- SPEC/HARNESS: `docs/EDITABLE_DOCUMENT_EDITORS_SPEC.md` e
  `docs/EDITABLE_DOCUMENT_EDITORS_HARNESS.md`.
- Validação: 92 testes dirigidos aprovados, `mvn clean compile` concluído, backend `UP` e desktop
  relançado com o pacote final.
- Homologação HTTP adicional concluída numa base H2 isolada: criação/actualização de cotação e
  encomenda a fornecedor, rejeição de versão antiga, bloqueio após envio e bloqueio após recepção
  parcial. Nenhum dado operacional foi alterado.

# Fluxo de atendimento e separacao (2026-08-16)

- Especificacao: `docs/CUSTOMER_ORDER_FULFILLMENT_SPEC.md`.
- Implementados estados controlados, reserva logica, idempotencia por empresa, guia termica,
  dupla autorizacao de reimpressao, conclusao da separacao, faturacao unica e diario operacional.
- Harness cobre o grafo de estados e a separacao de funcoes na reimpressao.
- Conversao bidireccional entre caixas, unidades soltas e total implementada nos pedidos; guias
  apresentam a decomposicao no ecrã e no PDF (`docs/PACKAGE_QUANTITY_SPEC.md`).
- Cadastro logístico com peso líquido/bruto unitário; pedidos e guias calculam peso total,
  percentagem de quantidade e percentagem de peso (`docs/LOGISTICS_WEIGHT_SPEC.md`).
- Catálogo POS compactado com thumbnails `96x60` e espaçamento reduzido, preservando nome, preço,
  estado de stock, tooltip e superfície clicável (`docs/POS_CATALOGO_CARDS_SPEC.md`).
- Identidade técnica migrada integralmente para `mz.multicore.erp`; caminhos, scripts, preferências,
  documentação e auxiliares usam apenas Multicore, protegidos por `ProductIdentityHarnessTest`.
- Fluxo comercial multiutilizador ganhou `Actualizar` visível no POS, Facturação, Pedidos, Guias e
  Notas, mantendo recarga automática após escritas (`docs/COMMERCIAL_MULTIUSER_REFRESH_SPEC.md`).
- Conversão de pedidos por embalagem corrigida: estado inicial sem unidade residual, factor
  `un/caixa` visível e cálculo bidireccional validado para caixas, soltas e total.
- `PackageQuantityEditor` tornou a conversão transversal em faturas, pedidos, compras e encomendas
  a fornecedor; guias continuam a herdar e apresentar a composição canónica.

# Fotografia do trabalhador (2026-08-24)

- A ficha do colaborador aceita fotografia opcional, reduzida no desktop antes do envio pela API.
- A imagem é persistida por empresa através da migration `V57__employee_photo.sql` e devolvida no
  `EmployeeDTO`; colaboradores existentes continuam válidos sem fotografia.
- O modal “Perfil do Trabalhador” apresenta a fotografia quando disponível e mantém as iniciais
  como alternativa.
- Validação concluída com `mvn -q -DskipTests compile` e `mvn -q -Dtest=HRServiceTest test`.
- Refinamento visual: avatar circular reutilizável com recorte proporcional, fallback por iniciais
  e indicador de câmara; pré-visualização de 104 px e orientação de formato/tamanho.
- O cadastro foi dividido em “Dados pessoais”, “Vínculo” e “Acesso e pagamento”, reduzindo a
  rolagem e mantendo os campos relacionados juntos. Validado visualmente no desktop e pelos testes
  `HRServiceTest` e `UiPanelDecompositionTest`.

# Saúde ocupacional do trabalhador (2026-08-24)

- Histórico de exames com cartão, exame, validade, aptidão, clínica/médico, restrições, observações
  e comprovativo opcional até 5 MB.
- Renovar cria nova linha e preserva o histórico; validade e alertas de 60 dias são derivados.
- Perfil geral mostra somente aptidão e validade; detalhes clínicos exigem `MANAGER` ou `ADMIN` e
  cada registo produz auditoria `OCCUPATIONAL_HEALTH_EXAM_REGISTER`.
- O sino recebe alertas de exames expirados ou próximos da renovação sem expor dados a outros
  perfis.
- Validação concluída com compilação limpa, testes dirigidos e suíte Maven completa.
## 2026-09-03 — Harness visual de feedback

- Adicionado `ProfessionalFeedbackVisualDriver`, isolado do backend, para executar M-01..M-08.
- M-01..M-08 homologados no Windows; escalas 100%, 125% e 150% verificadas sem cortes.
- Corrigida a revalidação do layout do `InlineFeedbackPanel` ao ficar visível ou oculto.
- Corrigido o contraste do `InlineFeedbackPanel` ao alternar entre tema escuro e claro.
- Validação final: `mvn clean compile` e 175 testes aprovados (zero falhas/erros); o harness de
  feedback executou 9 testes, incluindo a nova regressão de contraste entre temas.
- Impressão física bloqueada pelo ambiente: só existem impressoras virtuais PDF, XPS e Fax.
## 2026-09-03 — Fecho funcional por fases: RH e distribuição Windows

- **Fase RH:** a auditoria confirmou que acréscimos de horas extra, justificação de faltas e
  evolução salarial já tinham UI. Foi fechada a lacuna real: 13.º mês em **Recibos de Salário** e
  subsídio de férias em **Férias**, ambos com apuramento no backend, pré-visualização, confirmação,
  chamada assíncrona, tesouraria e feedback profissional. SPEC/HARNESS:
  `docs/RH_SUBSIDIOS_UI_SPEC.md` e `docs/RH_SUBSIDIOS_UI_HARNESS.md`.
- **Fase instalador:** `jpackage --win-per-user-install`; URL da API persistente fora da instalação
  em `%LOCALAPPDATA%\Multicore\desktop.properties`. O instalador não substitui o ficheiro.
- Gerado ao vivo `dist/Multicore-1.0.0.exe` (77.613.056 bytes). `Get-AuthenticodeSignature`
  confirma `NotSigned`: falta certificado de code signing, não código do instalador.
- Harnesses focados: 8 testes, zero falhas (`HRSubsidiesUiHarnessTest`,
  `DesktopLocalSettingsTest`, `WindowsInstallerHarnessTest`).
- Próxima decisão obrigatória: mapeamento contabilístico para compras, notas, pagamentos mistos,
  subsídios/adiantamentos e fecho de exercício; não inventar contas ou política contabilística.

## 2026-10-04 — Remediação da superfície de ataque

- Removidas credenciais alternativas fixas; palavras-passe seed passam a BCrypt e erros de login
  deixaram de revelar se o utilizador existe.
- Reset de palavra-passe e PIN ficaram limitados ao tenant administrado; resets revogam sessões.
- Empresa suspensa ou subscrição sem acesso bloqueiam cada pedido, incluindo sessões existentes.
- Monitorização respeita a função persistida e o tenant; incidentes globais e teste de email exigem
  `SUPERADMIN`. A auditoria forense rejeita acesso a outra empresa.
- Rate limiting usa utilizador e IP, limita memória e aceita `X-Forwarded-For` apenas do proxy
  configurado. O desktop exige HTTPS para servidores remotos e oculta credenciais de demonstração.
- Decisão e cobertura: `docs/SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md` e
  `docs/SECURITY_ATTACK_SURFACE_REMEDIATION_HARNESS.md`.
- Testes focados de segurança, monitorização, arquitetura e UI aprovados. A suíte completa do
  backend executou 1014 testes, sem falhas. As simulações POS foram alinhadas com a consulta
  canónica de sessão activa após passagem de turno; checkout, vales, contingência e conversão de
  cotações voltaram a passar. A suíte desktop executou 539 testes e mantém 27 falhas e 1 erro de UI
  fora desta fase, a tratar na estabilização visual seguinte.
