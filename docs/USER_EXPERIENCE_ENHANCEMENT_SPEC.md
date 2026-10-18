# Especificação Canónica — Melhoria de Experiência do Utilizador (UX & Ergonomia)

## 1. Contexto e Problema
O Multicore ERP é operado em ritmo acelerado por caixas, operadores de armazém, vendedores e gestores de faturação.
Identificaram-se três fricções ergonómicas na utilização quotidiana do desktop Swing:
1. **Falta de Auto-Foco em Formulários:** Ao abrir diálogos de registo ou edição (`ModernFormDialog`), o cursor não focava automaticamente o primeiro campo de texto, obrigando o utilizador a recorrer ao rato para iniciar a digitação.
2. **Tabelas Vazias Sem Orientação (Empty States):** Ao filtrar tabelas sem resultados correspondentes, a grelha apresentava um espaço em branco cinzento sem retorno visual claro sobre como reverter ou limpar os filtros, além da ausência do atalho `Esc` no campo de pesquisa para restauração imediata.
3. **Guia de Atalhos (`F1`) Desalinhado:** O diálogo de ajuda de teclado continha referências desatualizadas (`F9` em vez de `F10` para finalizar venda no POS) e omitia atalhos canónicos como `Espaço` (Espreitar linha / Quick-Peek) e `F3` (Pesquisa de catálogo).

## 2. Princípios Canónicos de UX Não-Negociáveis
1. **Operação Contínua Sem Rato:** Qualquer modal de formulário deve garantir foco imediato no primeiro elemento interativo (`JTextField`, `JComboBox`, etc.) assim que se torna visível.
2. **Restauração Rápida de Pesquisa (`Esc`):** O campo de pesquisa de qualquer tabela filtrada deve aceitar a tecla `Esc` para limpar o texto e restaurar a listagem original sem cliques.
3. **Feedback Didático de Ausência de Dados (Empty State):** Tabelas filtradas que retornam 0 linhas devem orientar o operador e disponibilizar ação de limpeza imediata dos critérios de busca.
4. **Alinhamento Rigoroso de Atalhos:** Os atalhos documentados na ajuda contextual (`F1`) devem corresponder a 100% aos bindings reais do sistema.

## 3. Matriz de Componentes e Comportamentos

| Componente | Melhoria Ergonómica | Comportamento Esperado |
| :--- | :--- | :--- |
| `ModernFormDialog` | Auto-Focus Recursivo | Procura e foca o primeiro componente editável (`JTextField`, `JComboBox`, `JSpinner`) após abertura |
| `TableFilter` | Atalho `Esc` na Pesquisa | Pressionar `Esc` no campo de busca limpa o texto (`setText("")`) e dispara a reavaliação do filtro |
| `TableFilter` | Ação `clearFilters(table)` | Método utilitário central para reiniciar a caixa de texto e repor todos os combos no índice 0 |
| `ShortcutHelpDialog` | Guia de Atalhos Canónicos | Corrige `F10 - Finalizar Venda`, adiciona `Espaço - Espreitar Linha (Quick-Peek)` e `Esc - Limpar Pesquisa / Fechar` |

## 4. Testes e Validação Obrigatória
A conformidade da especificação é protegida pelo harness dedicado `UserExperienceEnhancementHarnessTest`, validando:
1. Auto-foco funcional em `ModernFormDialog`.
2. Ação de limpeza de filtros e tecla `Esc` no `TableFilter`.
3. Consistência e integridade das categorias e atalhos em `ShortcutHelpDialog`.
4. Estabilidade do `MainFrame` via `DesktopThinContextTest`.
