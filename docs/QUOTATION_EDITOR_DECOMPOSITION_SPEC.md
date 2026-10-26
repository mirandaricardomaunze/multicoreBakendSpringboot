# Especificação Técnica: Decomposição de QuotationEditorForm (Fase 71)

- **Document ID**: SPEC-QUOTATION-EDITOR-001
- **Fase**: 71
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Autor**: Antigravity / DeepMind

---

## 1. Contexto e Diagnóstico

A classe `QuotationEditorForm.java` (666 linhas) acumula múltiplas responsabilidades no formulário de criação e edição de cotações comerciais:
1. **Cabeçalho da Cotação (`buildHeaderForm`)**: Construção de campos de cliente (combo ou walk-in), armazém de expedição, prazo de validade em dias, condições de pagamento, prazos de entrega e observações, com sincronização de opções e validação semântica.
2. **Cálculos Matemáticos de Linha e Embalagem**: Conversão de quantidades em caixas e embalagens soltas, apuramento de descontos, taxa de IVA efetiva e total líquido com imposto.
3. **Grelha Editável Estilo PHC**: Gestão da tabela inline com atalhos F2/F3, renderizadores monetários e sincronização direta das células.

---

## 2. Arquitetura Alvo e Decomposição

```
                   ┌──────────────────────────────────────────┐
                   │         QuotationEditorForm.java         │
                   │  (Orquestrador do editor, ≤ 380 linhas)  │
                   └─────────────────────┬────────────────────┘
                                         │
           ┌─────────────────────────────┴─────────────────────────────┐
           ▼                                                           ▼
┌───────────────────────────────────────────┐ ┌───────────────────────────────────────────┐
│       QuotationEditorHeaderForm.java      │ │      QuotationEditorRowCalculator.java     │
│       (Campos de cabeçalho, ≤ 210 linhas) │ │    (Cálculos de linha/totais, ≤ 110 linhas)│
│  - Cliente, armazém, validade e prazos    │ │  - Caixas, embalagens e percentagens       │
│  - Validação semântica de cabeçalho       │ │  - IVA efetivo e totais com desconto       │
│  - Dirty tracking e selecções reativas    │ │  - Sincronização de células e totais       │
└───────────────────────────────────────────┘ └───────────────────────────────────────────┘
```

---

## 3. Limites Físicos e Requisitos Não Negociáveis

1. **Limites de Linhas**:
   - `QuotationEditorForm.java`: $\le 380$ linhas (reduzido de 666 linhas).
   - `QuotationEditorHeaderForm.java`: $\le 210$ linhas.
   - `QuotationEditorRowCalculator.java`: $\le 110$ linhas.
2. **Zero `JOptionPane`**:
   - Zero diálogos legados `JOptionPane` em todos os ficheiros criados ou alterados.
3. **Preservação Rígida de Contratos de Teste**:
   - `EditableDocumentEditorsHarnessTest`: Presença obrigatória de `UIHelper.tableCardTop("Itens da Cotação"`, `Adicionar linha`, `Remover item`, `syncLineFromGrid`, `UpdateQuotationRequest`, e ausência de `JPanel lineForm`.
   - `PhcDocumentEditorHomologationHarnessTest`: Presença de `stopCellEditing()`, `for (int row = 0; row < draftLines.size(); row++)`, `packagesPerBox()`, `unitsPerPackage()`, `multiply(unitsPerBox)`.
   - `DocumentTableInteractionHarnessTest`: Presença de `installDocumentGridShortcuts(linesTable`.
   - `PhcInlineDocumentGridHarnessTest`: Presença de `return tableEditable && (column <= 3 || column == 6)`.
   - Textos e mensagens visíveis estritamente em português de Moçambique.
