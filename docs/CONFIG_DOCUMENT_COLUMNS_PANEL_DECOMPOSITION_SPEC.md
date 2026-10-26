# SPEC-CONFIG-DOCUMENT-COLUMNS-PANEL-DECOMPOSITION-001: Decomposição Modular de Colunas de Documentos

## 1. Visão Geral & Contexto Operacional

A interface `ConfigDocumentColumnsPanel.java` (no módulo desktop) é responsável pela parametrização das colunas visíveis nos documentos oficiais da empresa (Faturas, Encomendas, Guias de Remessa, etc.) e texto de rodapé de recibos POS.

Actualmente com 198 linhas de código, a classe acumula a construção do formulário de predefinições e caixas de seleção, a lógica de aplicação de modelos predefinidos ("Comercial Executivo", "Logístico / Armazém", "Todas as Colunas"), e os ciclos de leitura/gravação assíncrona via `DocumentConfigApiClient`.

Esta especificação define a extração das operações de colunas para um colaborador especializado:
- `ConfigDocumentColumnsOperations.java`: Mapeador de predefinições, construtor de DTO e executor assíncrono de leitura/gravação com auditoria.
- `ConfigDocumentColumnsPanel.java`: Painel orquestrador conciso focado na montagem visual dos seletores e botões de ação.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `ConfigDocumentColumnsPanel.java` | 198 | $\le 125$ | Orquestrador de visualização, formulário de opções e predefinições |
| `ConfigDocumentColumnsOperations.java` | 0 (Novo) | $\le 105$ | Aplicação de predefinições, DTOs e persistência assíncrona com auditoria |

---

## 3. Contratos e Invariantes Preservados

1. **Construtor e Métodos:**
   - Package-private `ConfigDocumentColumnsPanel(ConfigPanel owner)`.
   - `public JPanel buildPanel()`.
   - `public void refresh()`.
   - Package-private `void loadDocumentColumns()`.
2. **Layout Canónico e UX:**
   - Seletor de tipo de documento (`DocumentType.values()`).
   - 3 botões de predefinições com ícones semânticos (`"Comercial Executivo"`, `"Logístico / Armazém"`, `"Todas as Colunas"`).
   - 11 checkboxes de colunas (`colBarcode`, `colReference`, `colDescription`, `colExpiry`, `colQuantity`, `colPackages`, `colBoxes`, `colBoxPercentage`, `colUnitPrice`, `colTax`, `colSubtotal`).
   - Campo de rodapé para recibo POS.
3. **Segurança e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`, utilizando `ToastManager` e diálogos canónicos.
