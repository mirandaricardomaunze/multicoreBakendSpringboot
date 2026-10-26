# Especificação Arquitetural: Decomposição de ShelfLabelsDialog

- **Document ID**: SPEC-SHELF-LABELS-DIALOG-DECOMPOSITION-001
- **Fase**: 79
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/components/ShelfLabelsDialog.java` (500 linhas)

---

## 1. Contexto e Justificação

O `ShelfLabelsDialog.java` é o diálogo modal de geração, visualização e impressão de etiquetas de prateleira, gôndola, embalagem multinível (Caixa → Embalagem → Unidade) e rastreabilidade de lotes de fabrico (OP/Validade). Suporta formatos Folha A4 (grelha 3x8), Bobina Térmica (80mm) e Etiquetas Individuais (50x30mm).

### Dívidas Técnicas Identificadas
1. **Monolito de Apresentação e Impressão (500 linhas)**: Acopla num único diálogo a interface Swing com filtragem, a renderização gráfica 2D dos cartões de etiqueta (com código de barras simulado e badge multinível) e o motor de impressão de `PrinterJob`.
2. **Alta densidade gráfica**: O método `createSingleLabelCard` contém dezenas de linhas de desenho customizado que devem ser extraídas para um renderizador especializado.

---

## 2. Desenho Arquitetural da Decomposição

A arquitetura desacopla a responsabilidade visual de layout do desenho gráfico da etiqueta e da impressão física:

```
+---------------------------------------------------------------------------------+
| ShelfLabelsDialog (Orquestrador JDialog <= 210 linhas)                          |
| - Layout em abas/painéis (Split esquerdo: Lista de artigos; Direito: Preview)   |
| - Controlos de Nível, Formato, Cópias, Lote/OP e Validade                       |
| - Enum PackagingLevel e métodos estáticos show/showWithBatch                    |
| - Delegação canónica para ShelfLabelCardRenderer e ShelfLabelPrintEngine        |
+---------------------------------------------------------------------------------+
                 |                                               |
                 v                                               v
+------------------------------------+          +---------------------------------+
| ShelfLabelCardRenderer             |          | ShelfLabelPrintEngine           |
| (<= 160 linhas)                    |          | (<= 85 linhas)                  |
| - createSingleLabelCard(...)       |          | - printLabels(...)              |
| - Desenho de fundo e cantos (240x130)|        | - Configuração de PrinterJob    |
| - Código de barras 2D simulado     |          | - Paginação e grelha Printable  |
| - Composição multinível de embalagem|         | - Feedback via ToastManager     |
| - Formatação de preço e lote       |          |                                 |
+------------------------------------+          +---------------------------------+
```

---

## 3. Limites Físicos Estritos

| Classe | Teto de Linhas | Responsabilidade |
|---|---|---|
| `ShelfLabelsDialog.java` | $\le 210$ | Orquestrador do diálogo, filtros de produto e controlos de emissão. |
| `ShelfLabelCardRenderer.java` | $\le 160$ | Renderizador gráfico 2D das etiquetas de produto e embalagem multinível. |
| `ShelfLabelPrintEngine.java` | $\le 85$ | Motor de paginação e impressão via `PrinterJob`. |

---

## 4. Requisitos de Qualidade e Preservação de Contratos

1. **Preservação de Métodos Estáticos e Enums**:
   - `ShelfLabelsDialog.PackagingLevel` mantido.
   - `public static JPanel createSingleLabelCard(ProductDTO product)` mantido.
   - `public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level)` mantido.
   - `public static JPanel createSingleLabelCard(ProductDTO product, PackagingLevel level, String batchNumber, String expiryDate)` mantido.
   - Dimensões do cartão estritamente 240x130 pixels.
   - Construtores com 2 e 4 argumentos preservados.
2. **Zero `JOptionPane`**: Mantido uso estrito de `ToastManager` e `UIHelper`.
