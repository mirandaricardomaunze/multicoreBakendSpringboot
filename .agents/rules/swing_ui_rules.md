# Regras de UI Swing, Ícones e Sessão — Multicore ERP

## 1. Proibição Estrita de Emojis e Símbolos Unicode Brutos (Prevenção de "Quadrinhos" `▯`)

### O Problema
No Java Swing sobre Windows, as fontes padrão do sistema (`Segoe UI`, `Dialog`, `Tahoma`) manipuladas pelo motor gráfico Java 2D **não possuem glifos nativos para emojis ou símbolos estendidos** (como `⭐`, `⭐️`, `✅`, `❌`, `📦`, `🔍`, etc.).
Quando estes caracteres são inseridos em Strings de `JLabel`, `JButton`, `JTabbedPane`, títulos de secção ou `paintComponent()`, o Java 2D falha ao resolver o glifo e renderiza uma **caixa retangular vazia / quadrinho (`▯`)**, quebrando a estética profissional e desalinhando a tipografia da interface.

### Regra Obrigatória
1. **Nunca usar caracteres Unicode de emoji em texto de interface:**
   - Proibido: `"⭐ FAVORITOS"`, `"⭐ " + label`, `"[ ⭐️ Fixar ]"`, `"[ ❌ Remover ]"`.
   - Permitido: `"FAVORITOS"` acompanhado de `UIHelper.icon("fas-star", 10, UIHelper.PENDING_YELLOW)`.
2. **Utilizar sempre ícones vetoriais Ikonli FontAwesome 5 Solid:**
   - `UIHelper.icon("fas-star", size, color)`
   - `UIHelper.semanticIcon("fas-save", size)`
3. **Alinhamento de Rótulos em Menus e Listas:**
   - Nunca prefixar emojis ou marcadores de texto antes do nome de itens (`label`). A presença de prefixos de tamanhos variáveis desalinha a coluna de texto em relação aos outros itens da lista.

---

## 2. Acessibilidade e Fluxo Canónico de Logout (Terminar Sessão)

Todo o ambiente de trabalho principal (`MainFrame`) deve manter pontos de saída de sessão visíveis, intuitivos e acessíveis:
1. **Chip de Utilizador no Topo (`MainFrame`):** Botão com ícone `fas-sign-out-alt` em tom vermelho de alerta (`UIHelper.REJECTED_RED`) e tooltip *"Terminar Sessão (Logout)"*.
2. **Card de Perfil no Rodapé do Menu Lateral (`CollapsibleSidebar`):** Botão explícito de logout ao lado do nome em modo expandido e menu de contexto ao clicar no avatar em modo recolhido.
3. **Paleta de Comandos (`Ctrl+K` / `GlobalSearchDialog`):** Ação rápida `act_logout` indexada para termos como `"logout"`, `"sair"`, `"encerrar"`, `"trocar utilizador"`.
4. **Execução de Logout:**
   - Invocar sempre `UIHelper.requestLogout(Component parent)`.
   - Apresenta diálogo de confirmação ao operador.
   - Comunica com o backend via `/api/auth/logout` para revogar o token.
   - Limpa `DesktopSessionStore` e `CurrentUserContext`.
   - Fecha a janela principal e reabre com segurança o diálogo canónico de início de sessão (`LoginDialog`).

---

## 3. Limite de Linhas por Classe Swing ($\le 1000$ Linhas)

- O teste arquitetural `UiPanelDecompositionTest` impõe que **nenhuma classe Swing exceda 1000 linhas**.
- Ao adicionar novas opções, menus ou atalhos em `MainFrame.java`, condensar declarações multiline anteriores em linhas únicas concisas para manter margem segura abaixo de 1000 linhas.

---

## 4. Proibição de Reticências (`...` ou `…`) em Botões e Acções

1. **Rótulos Diretos e Conclusivos:**
   - Botões devem conter rótulos diretos e imperativos sem reticências finais: `"Nova Fatura"`, `"Faturar Encomenda"`, `"Registar Compra"`, `"Nova Encomenda"`, `"Escolher Imagem"`, `"Anexar Comprovativo"`.
   - Proibido usar sufixos como `"Nova Fatura…"`, `"Nova Encomenda..."` ou `"Escolher..."`.
2. **Defesa em Camadas no `ModernButton`:**
   - O construtor e o método `setText()` do `ModernButton` invocam automaticamente `stripEllipsis()`, eliminando qualquer reticência residual.
   - O `ActionMenuButton.addAction()` também higieniza os rótulos de itens de menu suspenso.
   - `ModernButton.getPreferredSize()` assegura margem horizontal mínima para prevenir que o Swing LayoutManager trunque rótulos com reticências.

---

## 5. Padrões Canónicos para Cartões de KPI e Métricas (`KpiCard`)

### Regras de Fundo e Cor ("Fundo Denso", "Sem Gradiente", "Paleta Coordenada")
1. **Sem Gradientes:** Proibido utilizar fundos com gradientes lineares (`GradientPaint`) em cartões de KPI.
2. **Fundo Denso e Sólido Obrigatório:**
   - O fundo de cada cartão de métrica deve ter cor sólida, densa, saturada e opaca (tonalidades 700/800 de Tailwind/Material, ex.: Azul `#16368A`, Verde Esmeralda `#065C44`, Roxo `#521E9B`, Ciano `#0B6673`, Âmbar `#8A4408`, Laranja `#98340C`, Vermelho `#94141C`, Ardósia `#243042`).
   - Proibido usar fundos pastéis claros, brancos lavados ou cores translúcidas que se misturem com o fundo escuro/claro da página ou tornem o cartão desbotado.
3. **Paleta Cromática Temática:**
   - Cada métrica deve possuir a sua própria identidade de cor através de `KpiCard.resolvePalette(...)`, variando harmonicamente na mesma grelha (ex.: Vendas em Azul/Esmeralda, Faturação em Roxo, Alertas em Vermelho/Laranja, Pendências em Âmbar).
4. **Hierarquia de Contraste e Tipografia:**
   - **Valor em Destaque:** Sempre em branco puro (`Color.WHITE`), `Font.BOLD`, tamanho 19-20.
   - **Título e Ícone:** Em tom pastel claro correspondente à família da cor (tons 100), tamanho 10-11 bold.
   - **Legenda Contextual:** Em tom pastel correspondente (tons 200), tamanho 10-11 plain.
   - **Bordas:** Linha sutil coordenada com transparência na mesma família de cor (`card.border`).

### Altura Padrão e Espaçamento ("Altura Boa para se Ver Dados")
1. **Altura Canónica de 96px:** Todos os cartões de KPI devem definir e respeitar `STANDARD_CARD_HEIGHT = 96` (e largura mínima de 140px).
2. **Prevenção de Cortes no Windows:** O padding padrão deve ser `EmptyBorder(10, 14, 10, 14)`. Isso garante folga vertical suficiente para que o título, o valor, a pílula de variação (`TrendBadge`) e a legenda nunca se sobreponham nem sejam cortados pelo Java 2D, inclusive em ecrãs com escalamento DPI de 125% ou 150%.

---

## 6. Padrão Canónico de Encapsulamento em Card (Table-Card Containment Pattern)

### O Problema
Tabelas ou barras de filtros adicionadas diretamente ao `BorderLayout.CENTER` ou `NORTH` de um separador (`JPanel` transparente) resultam em elementos soltos e flutuantes sobre o fundo cinzento, sem coesão visual e sujeitos a desalinhamento com o restante sistema de design.

### Regra Obrigatória
1. **Encapsulamento Rígido no `ModernPanel(16)`:**
   - O cabeçalho da página (`header`) contém apenas o título e a barra de ações (`tab.add(header, BorderLayout.NORTH)`).
   - Toda a tabela e os seus respectivos filtros (`SearchField`, `JComboBox`, seletores de armazém/validade/estado, resumos e badges) devem residir **estritamente dentro do `ModernPanel(16)` card**:
     ```java
     tab.add(header, BorderLayout.NORTH);

     ModernPanel card = new ModernPanel(16);
     card.setLayout(new BorderLayout());
     card.setBorder(new EmptyBorder(15, 15, 15, 15));

     // Filtros e resumos no topo INTERNO do card
     JPanel filterBar = ...; // ou TableFilter.bar(...)
     filterBar.setBorder(new EmptyBorder(0, 0, 12, 0));
     card.add(filterBar, BorderLayout.NORTH);

     // Tabela no CENTRO do card
     card.add(scrollPane, BorderLayout.CENTER);

     // Paginação ou totais no RODAPÉ do card
     card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

     tab.add(card, BorderLayout.CENTER);
     ```
2. **Proibição Estrita do Anti-Padrão `topStack`:**
   - É expressamente proibido agrupar `header` + `filters` num `topStack`, `headerWrap` ou `outWrap` adicionado à raiz do separador enquanto a tabela fica sozinha num card inferior. Os filtros pertencem ao card da tabela.
3. **Dimensionamento Amplo e Prevenção de Tabelas Espremidas:**
   - Tabelas operacionais devem ter sempre altura de linha entre 36px e 42px (`table.setRowHeight(42)`).
   - O viewport deve ter altura preferencial ampla fixada: `table.setPreferredScrollableViewportSize(new Dimension(800, 380))` e `scroll.setMinimumSize(new Dimension(200, 280))` para impedir que `ArrowScrollPanel` comprima a tabela para faixas de 30px.
4. **Disposição dos KPIs em Telas Densas:**
   - Em ecrãs dominados por tabelas de alta densidade (como Inventário Físico), a grelha de cartões de KPI pode ser posicionada no rodapé (`BorderLayout.SOUTH`) ou no topo com altura contida (96px), garantindo a mesma altura de tabela vista nos outros módulos do ERP.

---

## 7. Hierarquia de Botões no Cabeçalho e Prevenção de Colisão (Header Action Menu Hierarchy)

### O Problema
Quando um ecrã coloca 4 ou 5 botões de largura total lado a lado no cabeçalho (`BorderLayout.EAST`), a largura acumulada ultrapassa 600px. Em ecrãs normais ou quando o utilizador tem o menu lateral expandido, os botões colidem e sobrepõem-se ao título (`BorderLayout.WEST`), tornando os botões e títulos ilegíveis.

### Regra Obrigatória
1. **Máximo de 2 a 3 Botões Abertos no Cabeçalho:**
   - O cabeçalho deve conter apenas:
     - **Botão Primário:** Criação global (`[Novo ...]`, ex.: `[Novo Contrato]`, `[Registar Quebra]`).
     - **Botão Secundário Directo:** Impressão ou acção universal rápida (`[Imprimir PDF]`, `[Actualizar]`).
2. **Agrupamento de Acções de Linha em `ActionMenuButton`:**
   - Acções que operam sobre o registo seleccionado (ex.: *Activar*, *Renovar*, *Cessar*, *Aprovar*, *Rejeitar*, *Justificar*) devem ser agrupadas num botão de menu suspenso:
     ```java
     ActionMenuButton manageBtn = UIHelper.createActionMenuButton("Gestão do Contrato")
             .addAction("Activar", UIHelper.icon("fas-check", 14), () -> withSelection(this::activateContract))
             .addAction("Renovar", UIHelper.icon("fas-redo", 14), () -> withSelection(this::renewContract))
             .addAction("Cessar", UIHelper.icon("fas-ban", 14), () -> withSelection(this::terminateContract));
     ```
   - Isso reduz a barra de acções de ~650px para ~310px, garantindo tolerância a qualquer resolução de ecrã sem sobreposições.
3. **Limite Rígido de 5 Ações por `ActionMenuButton` (`MAX_ACTIONS = 5`):**
   - **Regra de Ouro:** NUNCA adicionar mais de 5 ações a uma única instância de `ActionMenuButton`. O método `addAction(...)` lança intencionalmente `IllegalStateException: O menu de acções não pode ter mais de cinco opções.` se uma 6.ª opção for registada.
   - **Decomposição quando houver > 5 Ações Secundárias:**
     - Se uma tela possuir mais de 5 ações secundárias, **dividir em menus temáticos complementares** (ex.: `[Documentos ▾]` para relatórios/impressão com $\le 5$ ações, e `[Operações ▾]` ou `[Gestão ▾]` para aprovação/ciclo de vida com $\le 5$ ações).
     - Manter no máximo 2 a 3 botões totais no cabeçalho (`UIHelper.actionsBar(menu1, menu2, primaryBtn)`).
     - Acções de execução de lote (ex.: `[Processar Mês]`) devem ser posicionadas na barra de ferramentas do cartão da tabela (`TableFilter.toolbar(filtrosEsquerda, botoesDireita)`), logo acima dos dados.

---

## 8. Protocolo de Diagnóstico e Explicação Prévia com Spec

### Regra Obrigatória
Quando o utilizador apontar problemas de layout, desalinhamento, colisão de botões ou estrutura de tabelas:
1. **Não alterar código precipitadamente.**
2. **Apresentar diagnóstico claro:** Apontar exactamente onde está a ausência de card (`ModernPanel`), onde ocorre a sobreposição de botões ou por que o viewport colapsou.
3. **Apresentar especificação técnica comparativa:** Demonstrar a solução canónica proposta (tabela comparativa Antes vs. Proposta).
4. **Obter confirmação do utilizador antes de editar ficheiros.**

---

## 9. Prevenção de Regressão no Login do Desktop (`MainFrame`)

### O Problema
Erros de configuração de layout, chamadas inválidas a componentes (como exceder o limite de 5 ações no `ActionMenuButton`) ou falhas em construtores de painéis Swing (`HRPanel`, `StockPanel`, etc.) compilam normalmente com `mvn clean compile`, mas explodem em tempo de execução quando o utilizador efetua login, impedindo a instanciação do `MainFrame` com a mensagem `Failed to instantiate [mz.multicore.erp.gui.MainFrame]: Constructor threw exc`.

### Regra Obrigatória
Sempre que forem alterados cabeçalhos, botões, construtores ou inicialização de qualquer painel Swing:
1. Executar obrigatoriamente o teste de contexto fino do Desktop:
   ```powershell
   mvn test -pl desktop "-Dtest=DesktopThinContextTest,ActionMenuButtonTest,HrCrmUiErgonomicsHarnessTest"
   ```
2. Confirmar que `DesktopThinContextTest.mainFrameInstantiatesCleanlyForNormalUserAndSuperAdmin` passa com sucesso (100% verde) antes de empacotar com `mvn package` e reiniciar a aplicação.

---

## 10. Validação Não-Destrutiva em Diálogos Modais (`ModernFormDialog`)

### O Problema
Em janelas modais complexas (ex.: Emissão de Guias de Transferência, Guias de Remessa, Faturação ou Ajustes), o operador gasta tempo selecionando armazéns, pesquisando artigos, digitando quantidades e escolhendo lotes FEFO. Se a validação dos campos de transporte (motorista, matrícula) ou do cliente for feita após fechar o diálogo ou por listeners avulsos com `dialog.dispose()`, uma omissão fecha a janela e descarta todo o trabalho e linhas introduzidas, gerando frustração extrema.

### Regra Obrigatória
1. **Validação no Hook `setOnSave` / `setOnSaveAsync`:**
   - A validação de campos obrigatórios de cabeçalho DEVE ser executada dentro do callback de salvamento do `ModernFormDialog`:
     ```java
     dialog.setOnSave(() -> {
         String motorista = driverField.getText().trim();
         if (motorista.isEmpty()) {
             driverField.requestFocusInWindow();
             throw new IllegalArgumentException("O nome do motorista é obrigatório.");
         }
         String matricula = plateField.getText().trim();
         if (matricula.isEmpty()) {
             plateField.requestFocusInWindow();
             throw new IllegalArgumentException("A matrícula do veículo é obrigatória.");
         }
         // Submissão da operação...
     });
     ```
2. **Prevenção de Perda de Dados:**
   - Quando `IllegalArgumentException` é lançada dentro de `setOnSave`, o `ModernFormDialog` captura o erro internamente, apresenta a mensagem no seu `feedbackPanel` com destaque vermelho e mantém a janela totalmente aberta, preservando intactas todas as linhas, produtos e lotes inseridos.
