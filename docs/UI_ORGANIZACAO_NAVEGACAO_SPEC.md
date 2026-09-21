# SPEC — Modernização da UI e Organização da Navegação

**Criado em:** 2026-09-12  
**Camada:** cliente desktop Swing (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`)  
**Backend:** contratos e regras de negócio permanecem inalterados.

---

## 1. Objectivo

Profissionalizar a entrada do sistema (`LoginDialog`) e a organização estrutural de navegação (`CollapsibleSidebar`, `MainFrame`), dotando a aplicação de uma navegação por categorias funcionais clara, ergonómica e moderna, que funcione perfeitamente em temas claro e escuro, responda a atalhos de teclado e não sofra cortes em escalas DPI de 100%, 125% e 150% no Windows.

---

## 2. Princípios de Organização

1. **Separação Funcional por Domínios**:
   Em vez de 14 ícones amontoados numa barra superior que quebra em duas linhas em ecrãs convencionais, os módulos são organizados em 4 categorias de negócio:
   - **OPERAÇÕES**:
     - Painel Inicial (`dashboard`)
     - POS — Caixa (`pos`)
     - Vendas & Pedidos (`comercial`)
     - Compras & Fornecedores (`compras`)
     - Stock & Armazéns (`stock`)
   - **GESTÃO & CRM**:
     - Tesouraria (`financeiro`)
     - Recursos Humanos (`hr`)
     - CRM & Assistência (`crm`)
     - Clientes (`clientes`)
   - **FISCAL & AUDITORIA**:
     - Área Fiscal (`fiscal`)
     - Contabilidade (`contabilidade`)
     - Aprovações (`approvals`)
   - **SISTEMA**:
     - Notificações (`notifications`) — com badge numérico de itens não lidos
     - Configurações (`config`)
     - *(Plataforma reservada exclusivamente ao SuperAdmin)*

2. **Densidade e Flexibilidade Operacional**:
   - A barra lateral oferece dois modos:
     - **Modo Expandido (240 px)**: exibe títulos de seção, ícones, rótulos textuais completos e badges.
     - **Modo Recolhido / Rail (64 px)**: recolhe-se para maximizar a área de trabalho (tabelas largas, checkout POS), mantendo ícones alinhados, tooltips acessíveis e badges visíveis.
   - Alternância rápida com um clique no botão de toggle ou através do atalho global `Ctrl+B`.

3. **Consistência de Tema (Light & Dark)**:
   - Todas as superfícies de navegação e login respeitam dinamicamente os tokens de `UIHelper` e `Theme`:
     - Fundo da barra, bordas, texto primário e texto secundário adaptam-se quando o utilizador alterna o tema.
     - Cores manuais `new Color(...)` são estritamente proibidas fora de definições centrais de componentes canónicos.

---

## 3. Especificação do Diálogo de Autenticação (`LoginDialog`)

1. **Estrutura Visual e Responsividade**:
   - Cartão central `ModernPanel` com cantos arredondados, sombra e espaçamento ergonómico.
   - Contido em `JScrollPane` leve ou layout que garanta que em monitores de 1366×768 e escalas DPI 125%/150% nenhum campo ou botão seja cortado.
   - Suporte aos temas claro e escuro: o fundo, cartões e inputs acompanham o `Theme` ativo.

2. **Deteção de Caps Lock**:
   - O campo de senha monitoriza o estado do Caps Lock (`KeyEvent.VK_CAPS_LOCK` / `Toolkit.getLockingKeyState`).
   - Quando o Caps Lock está ligado, apresenta uma etiqueta de aviso contextual discreta ("Caps Lock ativado") junto ao campo, prevenindo erros de digitação.

3. **Visibilidade de Senha**:
   - Botão embutido com ícone (`fas-eye` / `fas-eye-slash`) que permite ao utilizador alternar entre caracteres ocultos e visíveis.
   - Possui nome acessível (`AccessibleContext`) atualizado conforme o estado ("Mostrar senha" / "Ocultar senha").

4. **Chips Rápidos de Contas de Demonstração**:
   - Em vez de um texto passivo na base do ecrã, a caixa de diálogo disponibiliza botões/chips rápidos:
     - `Maria` (Gestora)
     - `João` (Caixa / Vendedor)
     - `Ana` (RH / Operações)
   - O clique num chip preenche imediatamente o utilizador e senha correspondentes e coloca o foco na ação "Entrar", acelerando testes e validações em loja.

5. **Feedback Não Bloqueante e Assíncrono**:
   - A submissão corre em `SwingWorker` fora do EDT, com `progressBar` indeterminada e botão desativado ("A entrar…").
   - Mensagens de erro são apresentadas num painel contextual estilizado, diferenciando erros de conectividade de credenciais inválidas.

---

## 4. Especificação da Barra Lateral (`CollapsibleSidebar` e `SidebarNavItem`)

1. **Estado e Layout**:
   - Larguras canónicas: `EXPANDED_WIDTH = 240 px`, `COLLAPSED_WIDTH = 64 px`.
   - Cabeçalho: marca ("MULTICORE"), subtítulo da empresa ativa e botão de recolha/expansão.
   - Corpo: rolagem vertical suave sem barra horizontal, organizando itens sob cabeçalhos de seção discretos (ocultos em modo recolhido).
   - Rodapé: versão do sistema e chip do utilizador autenticado com perfil legível em português.

2. **Itens de Navegação (`SidebarNavItem`)**:
   - Cada item suporta: ícone FontAwesome, etiqueta textual, cor de destaque (`accent`), indicador lateral ativo e badge numérico opcional.
   - Em modo recolhido: exibe `toolTipText` com a etiqueta do módulo e nome acessível.
   - Foco e acessibilidade: navegável por Tab, acionável por Enter ou Espaço, emitindo evento de clique.
   - Cores de fundo e texto derivadas de `UIHelper.isLight()`.

3. **Integração no `MainFrame`**:
   - A barra lateral é alojada em `BorderLayout.WEST`.
   - O topo (`BorderLayout.NORTH`) conserva uma barra de contexto e utilitários:
     - Rótulo de contexto do módulo atual / breadcrumb.
     - Seletor de empresa (quando a sessão possui mais de 1 empresa autorizada).
     - Sino de notificações com badge.
     - Alternador de tema claro/escuro.
     - Chip de utilizador e perfil.
   - O atalho de teclado `Ctrl+B` alterna o estado recolhido/expandido da barra lateral.

---

## 5. Critérios de Não Regressão

1. `POSPanel.java`, `StockPanel.java`, `ComercialPanel.java`, `ComprasPanel.java`, `HRPanel.java` e `ConfigPanel.java` devem permanecer estritamente abaixo de 1000 linhas (`UiPanelDecompositionTest`).
2. Nenhum painel do pacote `mz.multicore.erp.gui` pode conter instanciação manual de cores `new Color(...)`, termos em inglês técnico misturados ou posicionamentos absolutos (`FinalUiUniformityHarnessTest`).
3. O isolamento arquitetural obrigatório `desktop → contracts ← backend` deve ser preservado a 100% (`MultiModuleArchitectureHarnessTest`).
