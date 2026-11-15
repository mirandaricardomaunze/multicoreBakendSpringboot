# Escala tipográfica única e diálogos temáticos

Estado: **feito** · 2026-11-14 · Harness: `desktop/.../components/TypographyHarnessTest.java`

## Problema

O desktop tinha 557 `new Font(...)` espalhados por 184 ficheiros, com 20 tamanhos diferentes
(7 a 34 px). Dois títulos do mesmo tipo saíam com 13 px num ecrã e 15 px noutro. Havia também
29 `JOptionPane` nativos em 16 ficheiros, que abriam com o aspecto do Windows por cima do tema escuro.

## Regras

| ID    | Regra |
|-------|-------|
| TY-01 | Todo o texto no ecrã usa um papel de `Typography`; ninguém escreve `new Font(...)` fora da escala. |
| TY-02 | `deriveFont` só muda o estilo (itálico, negrito); um tamanho fixo (`15f`) é proibido. |
| TY-03 | A escala tem 8 tamanhos fechados: 9, 10, 11, 12, 13, 16, 20, 30. |
| TY-04 | Hierarquia: legenda < corpo < ênfase < título < métrica < destaque. |
| TY-05 | Nenhum `JOptionPane` fora do `ModernMessageDialog`. |
| TY-06 | O erro global (`DesktopApplication`) usa `ModernMessageDialog.showSafely`, que só recorre ao diálogo nativo se o próprio tema falhar. |
| TY-07 | `ModernMessageDialog.choose` exige pelo menos uma opção. |
| TY-08 | As tabelas (`UiTableCellStyler`) também usam a escala: linhas em `EMPHASIS`, cabeçalho em `EMPHASIS_BOLD`. |

## Papéis

| Papel | Tamanho | Uso | Absorveu |
|-------|---------|-----|----------|
| `BADGE` | 9 negrito | contador em ícones | 7–9 negrito |
| `KEYCAP` | 10 mono negrito | teclas (F2, Esc) | mono ≤ 11 negrito |
| `MONO` | 12 mono | logs, CSV, tramas da balança | Consolas, Monospaced |
| `CAPTION` / `_BOLD` | 11 | notas, rodapés, legendas de KPI | 10, 11 |
| `BODY` / `_BOLD` | 12 | campos, rótulos, botões | 12 |
| `EMPHASIS` / `_BOLD` | 13 | linhas de tabela, títulos de secção | 13, 14 |
| `TITLE` / `_REGULAR` | 16 | títulos de diálogo e ecrã | 15–18 |
| `METRIC` | 20 negrito | valores de KPI, totais em diálogos | 19–24 |
| `HERO` | 30 negrito | total do POS, peso ao vivo | 26–34 |

## Excepções nomeadas (no harness)

- Impressos dimensionados para papel: `ShelfLabelCardRenderer`, `PosThermalReceiptPrinter`,
  `StockTransferVolumeLabelsDialog`.
- Tamanho calculado: `CircularAvatar` (acompanha o diâmetro) e `KpiTextFit` (encolhe até caber).
- `SidebarNavItem`: glifo de recurso desenhado em Segoe UI Symbol.
- Pacote `gui/tv`: o Painel Executivo tem escala própria, para ser lido à distância.

## Diálogos temáticos

O `ModernMessageDialog` cobre todos os casos que antes iam para o `JOptionPane`:
`show`, `confirm`, `prompt`, `choose` (várias acções, a última é a principal),
`showContent` (tabela ou log só para leitura) e `confirmContent` (formulário próprio).
O depósito de fecho de caixa passou a usar uma lista de contas em vez de um botão por conta.
