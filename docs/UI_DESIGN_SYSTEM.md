# UI Design System - Swing

Este documento define padroes visuais e de interaccao para o cliente desktop Swing.

## Principios

- UI deve ser operacional, densa e clara.
- O utilizador deve conseguir trabalhar rapido com teclado, tabelas e dialogos previsiveis.
- Nao duplicar estilos; usar helpers e componentes existentes.
- Operacoes longas nunca bloqueiam o EDT.

## Componentes base

Usar:

- `UIHelper` para cores, icones, tabelas, dialogs e estilos.
- `ModernButton` para botoes.
- `ModernPanel` para paineis.
- `ModernFormDialog` quando o formulario encaixa no padrao existente.

Evitar:

- `new Color(...)` ad-hoc em paineis.
- Emojis ou símbolos Unicode brutos (como ⭐, ✅, ❌) em botões, abas, títulos, secções ou labels funcionais (o Java 2D no Windows renderiza retângulos/quadrinhos `▯` por falta de glifo nativo).
- Layouts que dependem de tamanhos magicos sem responsividade.
- Regras de negocio dentro de listeners Swing.

## Icones

Padrao:

```java
UIHelper.icon("fas-save", 14)
UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK)
UIHelper.semanticIcon("fas-users", 16)
```

Regras Obrigatorias:

- Usar Ikonli FontAwesome 5 Solid.
- Icone deve reforcar a accao: guardar, imprimir, procurar, apagar, aprovar.
- **Ícones Coloridos Semânticos em Abas e Ações**:
  - Abas de navegação (`JTabbedPane.addTab(...)`) e itens de ação rápida (`ActionMenuButton`) devem obrigatoriamente utilizar ícones vetoriais coloridos pela semântica do módulo ou função (`UIHelper.MODULE_*`, `UIHelper.ACCENT_*`, `UIHelper.APPROVED_GREEN`, `UIHelper.PENDING_YELLOW`, `UIHelper.REJECTED_RED` ou `UIHelper.semanticIcon(...)`).
  - É proibido usar ícones monocromáticos cinzentos/pretos (`UIHelper.TEXT_LIGHT`, `UIHelper.TEXT_SECONDARY`, `UIHelper.TEXT_MUTED`) em abas de navegação ou cabeçalhos de diálogo.
  - Diálogos de formulário (`ModernFormDialog`) herdam e exibem automaticamente o badge colorido temático via `UIHelper.buildPremiumHeader(...)` baseado no código do ícone e módulo.
- Botoes destrutivos devem ser visualmente distintos e confirmar quando houver risco.

## Formularios

- Labels em portugues de Mocambique.
- Campos seguem ordem do fluxo real de trabalho.
- Campos obrigatorios devem ser claros.
- Datas de input usam `yyyy-MM-dd` enquanto nao houver date picker oficial.
- Datas apresentadas usam `dd/MM/yyyy`.
- Validacao de regra fica no Service; UI apenas mostra mensagem clara.
- Todos os inputs e selects partilham a altura canónica de 38 px (`UIHelper.FORM_CONTROL_HEIGHT`).
- Selects de filtro em barras de ferramentas têm largura mínima consistente (ex.: 160 px) e altura de 38 px.

## Tabelas

- Usar `DefaultTableModel` com `isCellEditable` definido.
- Aplicar `UIHelper.styleTable(table)`.
- Colunas monetarias alinhadas e formatadas.
- Quantidades com precisao consistente.
- Evitar carregar listas enormes sem filtro/paginacao quando o volume crescer.
- **Tabelas Espaçosas & Eliminação de Confinamento Vertical**:
  - Evitar dividir a janela verticalmente com `JSplitPane.VERTICAL_SPLIT` quando há mais de uma tabela; adotar layout de página fluida com `ArrowScrollPanel`.
  - Tabelas de horizonte fixo (ex.: matrizes de 5 a 7 períodos) devem ter altura total calculada (`linhas * rowHeight + headerHeight`) com `setPreferredScrollableViewportSize` para exibição imediata e limpa de 100% dos registos sem barra de scroll interna.
- **Botões de Ação com Ícone**: Largura mínima recomendada de 130 px para ações com texto e ícone (ex.: *"Actualizar"* via `UIHelper.createRefreshButton(...)`) para prevenir reticências (`...`) sob qualquer densidade de píxeis.


### Filtros de Tabela e Navegação Temporal (Spec)
- **Tabelas com Datas Operacionais:** Toda a tabela que contenha datas de eventos (emissão de documentos, movimentos de stock, transações de tesouraria, ordens de compra e cotações) deve obrigatoriamente disponibilizar:
  1. Campo de pesquisa textual universal (`TableFilter.searchField(...)`).
  2. Filtro de estado/tipo relevante via `TableFilter.ColumnFilter` quando o domínio tiver múltiplos estados operacionais.
  3. Filtro de período temporal via `TableFilter.periodCombo()` e `TableFilter.PeriodFilter`, cobrindo no mínimo: *"Todo o período"*, *"Hoje"*, *"Últimos 7 dias"*, *"Últimos 30 dias"*, *"Este mês"*.
- **Posicionamento Canónico na Barra de Ferramentas:**
  `TableFilter.bar(searchField, TableFilter.label("Estado:"), statusCombo, TableFilter.label("Data:", "fas-calendar-alt"), periodCombo)`
- **Uniformidade Geométrica:** Todos os controlos da barra de filtros têm altura de 38 px (`UIHelper.FORM_CONTROL_HEIGHT`).
- **Padrão de Encapsulamento em Card (*Table-Card Containment*):** Os campos de pesquisa, filtros de coluna e ações contextuais da tabela devem estar obrigatoriamente encapsulados **dentro** do `ModernPanel` que hospeda a tabela (`BorderLayout.NORTH`), logo acima do cabeçalho da tabela, nunca flutuando soltos no painel pai fora do card. O card compõe assim uma unidade visual integral: [Filtros/Pesquisa no Topo] -> [Tabela no Centro] -> [Paginação no Rodapé].
- **Posicionamento de Acções:** Acções da tabela ficam à direita da barra superior via
  `UIHelper.filterBar(...)` / `TableFilter.toolbar(...)`. O `BorderLayout.SOUTH` do card é reservado
  exclusivamente a paginação, contagem, totais e estado informativo; botões de acção são proibidos
  nessa zona. Cabeçalhos de página podem conter até três acções estritamente globais. Ver
  `docs/TABLE_ACTION_PLACEMENT_SPEC.md`.


## Dialogos

- Usar `UIHelper.createDialogForm(...)` quando aplicavel.
- Dialogos altos devem usar `UIHelper.makeDialogScrollable(...)`.
- Titulos: "Sucesso", "Erro", "Aviso", "Confirmar".
- Mensagens devem orientar a correccao, nao culpar o utilizador.

## Threading

- Chamadas demoradas, relatorios, PDFs, imports e operacoes remotas usam `SwingWorker`.
- UI deve mostrar estado de carregamento ou desactivar a accao durante execucao.
- Nunca actualizar componentes Swing fora do EDT.

## Separacao de responsabilidades

Painel Swing pode:

- Montar UI.
- Ler valores dos campos.
- Chamar Service ou, no futuro, Client HTTP.
- Apresentar DTOs.
- Mostrar mensagens.

Painel Swing nao pode:

- Calcular imposto, stock, salario ou totais oficiais.
- Persistir directamente.
- Chamar Repository.
- Decidir permissao sensivel sem Service.
- Conter regra que precise sobreviver a migracao para backend.
