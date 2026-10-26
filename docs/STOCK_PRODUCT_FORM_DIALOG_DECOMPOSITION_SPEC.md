# Especificação Técnica: Decomposição de StockProductFormDialog

- **Document ID**: SPEC-STOCK-PRODUCT-FORM-DIALOG-DECOMPOSITION-001
- **Fase**: 82
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/StockProductFormDialog.java` (440 linhas)

---

## 1. Contexto e Diagnóstico

O ficheiro `StockProductFormDialog.java` é responsável pelas interfaces modais de cadastro e atualização de artigos no catálogo de inventário. Com **440 linhas**, acumula:
1. **Montagem de Componentes e Layouts**:
   - Campos de SKU, referência, código de barras, nome, IVA, preços (venda, compra, grosso), pesos e embalagens multinível.
   - Componente visual com seletor e redimensionamento de imagens (`createImageChooserPanel`).
   - Sincronização em tempo real de embalagens (`bindPackagingFields`).
2. **Validações Semânticas e Sanitização Numérica**:
   - Parsing numérico com sanitização de vírgula/ponto (`sanitizeNumber`, `parsePositiveOrNull`).
   - Validação de obrigatoriedade e regras de negócio (peso bruto $\ge$ líquido, preços não negativos, etc.).
3. **Coordenação Assíncrona com API**:
   - `dialog.setOnSaveAsync` para criação (`createProduct`) e atualização (`updateProduct`), seguidos de upload de imagem e opção de entrada inicial de stock.

---

## 2. Arquitetura Proposta e Divisão de Responsabilidades

```
mz.multicore.erp.gui/
├── StockProductFormDialog.java           (Orquestrador canónico: <= 175 linhas)
│   ├── Métodos públicos showCreate(...) e showEdit(...)
│   ├── Gestão do ciclo de vida modal via ModernFormDialog
│   ├── .setConfirmButton("Actualizar", "fas-save") e dialog.setOnSaveAsync
│   └── Diálogo de oferta de adição inicial de stock (StockBatchEntryDialog.show)
├── StockProductFormBuilder.java          (Montagem de formulários e layout: <= 150 linhas)
│   ├── Construção de formulários de criação e edição com campos estilizados
│   ├── Componente de imagem (createImageChooserPanel) com preview 96x96
│   └── Ligação de campos de embalagem multinível (bindPackagingFields)
└── StockProductFormValidator.java        (Validação semântica e parsing: <= 140 linhas)
    ├── Validação semântica de campos obrigatórios e regras de peso e preços
    ├── Sanitização e parsing numérico (sanitizeNumber, parsePositiveOrNull, parseRequiredPositiveInt)
    └── Conversão para PackagingComposition
```

---

## 3. Metas Físicas de Linhas

| Classe | Linhas Antes | Meta Teto |
|---|---|---|
| `StockProductFormDialog.java` | 440 linhas | $\le 195$ linhas |
| `StockProductFormBuilder.java` | Nova classe | $\le 150$ linhas |
| `StockProductFormValidator.java` | Nova classe | $\le 140$ linhas |

---

## 4. Requisitos Não Negociáveis

1. **Preservação Integral de Contratos**:
   - `StockProductFormDialog.showCreate(owner, categories, vatRates)`
   - `StockProductFormDialog.showEdit(owner, preselectedProductId, categories, vatRates, products)`
   - Preservar literais `.setConfirmButton("Actualizar", "fas-save")` e `dialog.setOnSaveAsync`.
2. **Zero Diálogos Legados `JOptionPane`**.
3. **Validação de Limites Decimais**:
   - Uso de `.signum() < 0` e `.compareTo(...)`.
4. **Mensagens em Português de Moçambique**.
