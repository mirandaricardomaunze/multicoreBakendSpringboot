# SPEC-TBLF-001: Barra Universal de Filtro Rápido e Pesquisa em Tabelas (`TableQuickFilterBar` / `Ctrl+F`)

## 1. Contexto e Motivação
Nos módulos operacionais do Multicore ERP (Comercial, Compras, POS, Stock, RH e Financeiro), os operadores e gestores analisam tabelas com dezenas ou centenas de registos (ex.: listagem de faturas, catálogo de produtos, saldos de stock, histórico de transações e colaboradores).
A localização de uma linha específica através de scrolling vertical e horizontal consome tempo e aumenta a probabilidade de erro operacional.
Para proporcionar ergonomia e velocidade de trabalho profissional sem quebrar os padrões de arquitetura multi-módulo nem exigir alterações invasivas nos modelos de negócio existentes, esta especificação define a **Barra Universal de Filtro Rápido e Pesquisa em Tabelas (`TableQuickFilterBar`)**.

---

## 2. Requisitos Funcionais

### 2.1. Ativação e Atalhos de Teclado
- **Atalho de Acesso `Ctrl+F`**:
  - Quando a tabela ou qualquer componente do painel tem o foco, premir `Ctrl+F` (ou `Cmd+F` em macOS) deve focar imediatamente o campo de texto de pesquisa (`filterField`), selecionando todo o texto pré-existente para substituição imediata.
- **Atalho de Limpeza/Escape `Escape`**:
  - Quando o foco está no campo de pesquisa e este contém texto, premir `Escape` limpa imediatamente o filtro e restaura a visualização integral das linhas da tabela.
  - Se o campo já estiver vazio, premir `Escape` devolve o foco para a tabela (`JTable`).
- **Navegação `Enter` / `Down`**:
  - Premir `Enter` ou tecla `Seta Abaixo` no campo de pesquisa move o foco de seleção diretamente para a primeira linha visível da tabela filtrada.

### 2.2. Motor de Filtragem Multi-Termo (*Case-Insensitive*)
- O texto inserido no campo de pesquisa é dividido em múltiplos termos independentes (*tokens*) separados por espaços em branco.
- Cada token é tratado como uma expressão literal escapada via `java.util.regex.Pattern.quote(token)`.
- A regra de correspondência usa a flag `(?i)` (*case-insensitive*) e pesquisa em **todas** as colunas visíveis da tabela.
- Múltiplos tokens operam sob conjunção lógica (**AND**): uma linha só é visível se contiver **todos** os tokens digitados, mesmo que em colunas distintas (ex.: `2026 Maputo Pago` encontra linhas onde o ano, cidade e estado constem em colunas separadas).
- Se o campo de texto estiver vazio ou apenas contiver espaços, a regra de filtro é anulada (`sorter.setRowFilter(null)`), exibindo 100% dos registos originais.

### 2.3. Contador Reativo de Registos
- A barra de filtro deve manter um rótulo de contagem em tempo real:
  - Formato padrão com filtro ativo: `"Exibindo X de Y registos"` (onde `X` é a contagem visível atual `table.getRowCount()` e `Y` é o total de linhas do modelo `table.getModel().getRowCount()`).
  - Quando o filtro não está ativo ou todas as linhas estão visíveis: `"Total: Y registos"`.
- O contador deve atualizar-se automaticamente via:
  - Alterações no campo de texto (`DocumentListener`).
  - Notificações de inserção, remoção ou alteração de dados no modelo subjacente (`TableModelListener`).

### 2.4. Ações e Controlos de Interface
- **Ícone Semântico**: Ícone de lupa `fas-search` à esquerda do campo de texto.
- **Campo de Texto Estilizado**: Placeholder / texto de orientação *"Filtrar registos... (Ctrl+F)"*.
- **Botão de Limpeza Rápida `[ ✕ ]`**:
  - Ícone `fas-times` ou `✕`.
  - Visível e ativo quando há texto digitado; ao clicar, limpa o texto e re-foca o campo.
- **Acessibilidade & Alto Contraste**:
  - Em modo de Alto Contraste (`Theme.HIGH_CONTRAST`), as bordas são marcadas em branco `#FFFFFF` e o contraste de texto satisfaz WCAG 2.1 AAA ($\ge 7.0:1$).

---

## 3. Arquitetura e Estrutura de Classes

```
mz.multicore.erp.gui.components/
├── TableQuickFilterBar.java       (JPanel universal de pesquisa com TableRowSorter e DocumentListener)
└── UIHelper.java                  (Métodos utilitários attachQuickFilter e wrapTableWithQuickFilter)
```

### 3.1. Assinatura Canónica de `TableQuickFilterBar`
```java
public class TableQuickFilterBar extends JPanel {
    public TableQuickFilterBar(JTable table);
    public static TableQuickFilterBar attach(JTable table);
    public static JPanel wrapWithFilter(JTable table);
    public static JPanel wrapWithFilter(JScrollPane scrollPane, JTable table);

    public void setFilterText(String text);
    public String getFilterText();
    public void clearFilter();
    public int getFilteredCount();
    public int getTotalCount();
    public void focusSearch();
    public JTable getTable();
    public JTextField getFilterField();
    public JButton getClearButton();
    public JLabel getCountLabel();
}
```

---

## 4. Integração Não-Invasiva
O componente utiliza a infraestrutura padrão do Swing `TableRowSorter<TableModel>`, garantindo que:
1. O `TableModel` original nunca é modificado nem recriado.
2. A ordenação por clique nos cabeçalhos (`JTableHeader`) continua a funcionar normalmente em conjunto com a filtragem.
3. Se a tabela já tiver um `TableRowSorter`, este é reutilizado sem perder configurações prévias de comparadores de colunas.
