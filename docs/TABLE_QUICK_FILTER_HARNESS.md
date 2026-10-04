# HARNESS-TBLF-001: Matriz de Testes da Barra Universal de Filtro Rápido em Tabelas

## 1. Objectivo
Este documento define a suite de validação automatizada para o componente `TableQuickFilterBar` no módulo `desktop`, em conformidade com a especificação `SPEC-TBLF-001`.

---

## 2. Matriz de Casos de Teste

| ID do Teste | Descrição do Caso de Teste | Comportamento Esperado |
|---|---|---|
| **TBLF-01** | Inicialização e Configuração do Sorter | Ao instanciar `TableQuickFilterBar(table)`, a tabela recebe um `TableRowSorter<TableModel>` válido e o contador inicial reflecte o total de registos. |
| **TBLF-02** | Filtragem Simples Case-Insensitive | Ao digitar um termo em minúsculas (ex: `"maputo"`), registos com `"Maputo"` ou `"MAPUTO"` devem permanecer visíveis e outros devem ser ocultados. |
| **TBLF-03** | Filtragem Multi-Termo (AND Lógico) | Ao digitar múltiplos termos separados por espaços (ex: `"2026 Pendente"`), apenas linhas que contenham todos os termos em qualquer coluna devem permanecer. |
| **TBLF-04** | Limpeza de Filtro e Restauração | Chamar `clearFilter()` ou premir o botão limpar deve esvaziar o campo de texto e restaurar 100% das linhas da tabela. |
| **TBLF-05** | Métricas do Contador Reativo | As chamadas a `getFilteredCount()` e `getTotalCount()` devem reflectir com precisão a contagem da visualização filtrada e do modelo original. |
| **TBLF-06** | Reatividade a Mutações no TableModel | Ao adicionar ou remover linhas no `DefaultTableModel` em tempo de execução, o filtro e o contador devem recalcular automaticamente. |
| **TBLF-07** | Mapeamento de Atalhos de Teclado | As combinações de teclas `Ctrl+F` e `Escape` devem estar associadas às ações respetivas no `InputMap`/`ActionMap`. |
| **TBLF-08** | Limite Estrito de Linhas de Código | A classe `TableQuickFilterBar` e componentes Swing relacionados devem respeitar o limite estrito de $\le 1.000$ linhas. |

---

## 3. Critérios de Aceitação
- Todos os 8 testes devem passar com sucesso (`100% GREEN`).
- O reactor Maven deve compilar com `mvn clean test -pl desktop -Dtest=TableQuickFilterHarnessTest`.
- Nenhuma dependência externa à arquitetura multi-módulo é permitida.
