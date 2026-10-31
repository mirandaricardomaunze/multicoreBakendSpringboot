# Especificação de Decomposição — UIHelper (Fase 197)

## Objetivo
Decompor o monólito utilitário `UIHelper` (`3.031` linhas) em 5 colaboradores coesos no pacote `mz.multicore.erp.gui.components`, mantendo 100% dos campos públicos/package-private, constantes de paleta/tipografia, classes internas (`PaginationSouthComposite`) e assinaturas estáticas na fachada `UIHelper`.

## Colaboradores Extraídos (`mz.multicore.erp.gui.components`)
1. `UiThemeAndWindowSupport` — Ciclo de vida do tema FlatLaf/Multicore, contraste WCAG 2.1, interpolação de cores e contenção geométrica de janelas/modais na viewport útil.
2. `UiTabbedAndTableStyler` — Estilização de abas (`JTabbedPane`), tabelas (`JTable`), calha de seleção, paginação automática inferior, scroll panes e navegação/atalhos de grelha documental.
3. `UiIconAndImageSupport` — Ícones vetoriais FontAwesome/Ikonli, cores semânticas de domínio, cabeçalhos premium de modais, ícones multi-resolução da aplicação e redimensionamento de imagens.
4. `UiAsyncTaskSupport` — Execução assíncrona fora do EDT (`SwingWorker`) com propagação de `CurrentUserContext`, contagem de cursores de espera por janela e proteção contra duplo clique.
5. `UiFormControlAndFormatSupport` — Estilização corporativa de inputs/selects (`JTextField`, `JComboBox`, `JSpinner`, `JTextArea`), fábricas de botões/barras, formulários em grelha e formatação/traduções PT-MZ.
