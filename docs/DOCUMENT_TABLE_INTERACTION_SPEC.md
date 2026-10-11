# SPEC-DTI-001: Padrão Universal de Interacção com Tabelas de Documentos (Duplo Clique e Atalhos de Teclado)

**Estado:** Aprovado  
**Data:** 2026-10-02  
**Módulos Abrangidos:** `desktop` (Comercial, Compras, Stock/Inventário)  
**Harness de Validação:** `mz.multicore.erp.gui.DocumentTableInteractionHarnessTest`

---

## 1. Contexto e Motivação

No Multicore ERP, os operadores emitem e consultam centenas de documentos diários (Encomendas de Cliente, Cotações, Encomendas a Fornecedor, Guias de Transferência e Faturas).
Para garantir velocidade operacional equivalente a sistemas de balcão e ERPs internacionais (PHC, SAP, Primavera), todas as tabelas de listagem e todas as grelhas de itens devem comportar-se da mesma forma previsível:
1. **Navegação Rápida:** Duplo clique na linha de uma listagem abre de imediato o editor / consulta do documento seleccionado, sem obrigar o operador a abrir menus de contexto.
2. **Produtividade de Teclado (Headless/Hands-on-Keyboard):** Digitação contínua nas grelhas de itens sem retirar as mãos do teclado para clicar em «Adicionar linha» ou «Remover item».

---

## 2. Requisitos Não Negociáveis

### DTI-01: Duplo Clique e Inspeção Unificada em Tabelas de Listagem de Documentos
Toda a tabela de listagem de documentos deve utilizar a porta unificada do `RowDetailsInspector` (instalada via `UIHelper.styleTable(table)`) para duplo clique e tecla Enter:
- **Inspeção sem Redirecionamento:** O duplo clique em tabelas de listagem (`ordersTable`, `invoicesTable`, `table` em `QuotationsPanel`, `poListTable`, `transferTable`) abre a visualização/inspeção de detalhes ou acção configurada em `customDetailAction`, sem alternar concorrentemente para a página de formulário/editor.
- **Edição Preservada:** A edição de documentos é acionada através dos botões e menus "Editar / Consultar" (`openSelectedOrderEditor`, `openSelectedEditor`, etc.), prevenindo a perda de contexto ou navegação involuntária para o formulário ao fechar janelas de detalhes.
- **Proteção Anti-Concorrência:** Proibido registar ouvintes concorrentes de duplo clique com ações de formulário sobre tabelas que já possuem `styleTable`.

### DTI-02: Atalhos Universais em Grelhas de Linhas de Documentos
Todas as grelhas de itens em preparação de documentos (`CommercialOrdersView`, `QuotationEditorForm`, `PurchaseOrdersPanel`, `StockTransferEditorForm`, `CommercialInvoicesView`) devem registar os seguintes atalhos no seu `InputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)`:
- **`INSERT` / `Ctrl + ENTER`:** Invoca a acção de adicionar nova linha em branco na tabela e coloca o foco para selecção de produto.
- **`DELETE` / `Ctrl + DELETE`:** Quando a célula não se encontra em modo de edição de texto (`!table.isEditing()`), remove a linha actualmente seleccionada na tabela.
- **`Ctrl + S` / `F10`:** Dispara a gravação/submissão do documento a partir da grelha ou do painel host.

### DTI-03: Protecção de Edição de Texto
O atalho `DELETE` só pode remover a linha se a célula não estiver activamente em modo de edição (ex.: o utilizador a apagar caracteres dentro de um `JTextField` ou `JComboBox` editor não perde a linha inteira da tabela).

### DTI-04: API Centralizada no `UIHelper`
Para prevenir duplicação de código e inconsistências, os atalhos e duplo clique são instalados via métodos utilitários canónicos:
- `UIHelper.installDoubleClick(JTable table, Runnable onDoubleClick)`
- `UIHelper.installDocumentGridShortcuts(JTable table, Runnable onAdd, Runnable onRemove, Runnable onSave)`

---

## 3. Matriz de Cobertura dos Editores

| Documento | Tabela de Listagem (Duplo Clique) | Grelha de Itens (Atalhos Teclado) |
| :--- | :--- | :--- |
| **Encomendas de Cliente** | `owner.ordersTable` → `openSelectedOrderEditor` | `owner.orderLinesTable` → `Insert`, `Delete`, `Ctrl+S` |
| **Cotações de Venda** | `table` → `openSelectedEditor` | `linesTable` → `Insert`, `Delete`, `Ctrl+S` |
| **Encomendas a Fornecedor** | `poListTable` → `openSelectedEditor` | `poLinesTable` → `Insert`, `Delete`, `Ctrl+S` |
| **Transferências de Stock** | `transferTable` → `openSelectedTransfer` | `linesTable` → `Insert`, `Delete`, `Ctrl+S` |
| **Faturação de Venda** | `invoicesTable` → `printSelectedInvoice` | `linesTable` → `Insert`, `Delete`, `Ctrl+S` |
