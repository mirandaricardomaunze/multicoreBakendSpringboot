# Formato numérico visível — Especificação e harness

**Última actualização:** 2026-11-06

## Problema

Valores formatados sem locale dependem da máquina. O mesmo valor sai `1.500,50` num Windows em
português e `1,500.50` num Windows em inglês ou no contentor Docker do backend. A CI em Linux expôs
o defeito, que afecta ecrãs, Relatório Z, etiquetas, vales e PDFs de inventário.

## Decisão

- O formato visível é `1.500,50`, definido por `DisplayLocale.NUMBERS` em `contracts`.
- **Padrão único, decidido em 2026-10-31:** ecrãs, tabelas e documentos usam ponto no milhar. O
  `pt-MZ` do JDK não é usado para números porque escreve `1 500,50`. Continua válido para datas.
- **Leitura do texto mostrado:** passa só por `MoneyField.normalize`. Um ponto isolado sem vírgula
  continua a ser decimal (`1.5`), como nos campos digitados.
- **Desktop:** `DesktopApplication.main` fixa `Locale.Category.FORMAT` antes de criar a interface,
  porque há mais de 140 chamadas sem locale.
- **Fora da tabela:** quem repete uma célula (export PDF, painel de espreitar) usa `CellText`: o
  texto do renderer da coluna, ou o formato base quando a coluna não tem renderer.
- **Mensagens e documentos do backend:** `DisplayNumbers.money` (`1.460,00`) e `DisplayNumbers.qty`
  (`2,5`), em `contracts`. `MoneyFormat` delega nele.
- **Backend:** cada chamada indica o locale. O padrão global **não** muda, para que
  `String.format("%.2f")` não passe a produzir vírgula em exportações como o XML do SAF-T.

## Harness

| Id | Cenário | Espera-se | Teste |
|----|---------|-----------|-------|
| FN-01 | `1500.5` com `DisplayLocale.NUMBERS` | `1.500,50 MT` | `DisplayNumberFormatHarnessTest` |
| FN-02 | `String.format` com `%,` e sem locale no backend | Reprova com ficheiro e linha | `DisplayNumberFormatHarnessTest` |
| FN-03 | Arranque do desktop | Formato fixado antes da interface | `DesktopNumberLocaleHarnessTest` |
| FN-04 | Valor formatado e lido de volta | Mesmo número | `DesktopNumberLocaleHarnessTest` |
| FN-05 | Leitura de `1.500,00`, `1 500,00`, `1500,5`, `1.234.567`, `1.5` | Número correcto | `DesktopNumberLocaleHarnessTest` |
| FN-06 | Separador de espaço no desktop | Reprova | `DesktopNumberLocaleHarnessTest` |
| FN-07 | `toPlainString()` num documento impresso (`modules/printing`) | Reprova com ficheiro e linha; usar `MoneyFormat.formatPlain` / `formatQty` | `DisplayNumberFormatHarnessTest` |
| FN-08 | Listagem exportada para PDF | Cada célula leva o texto do renderer, como no ecrã | `TableExportFilterTest` CE-07 |
| FN-09 | Painel de espreitar (Espaço) com valor ou quantidade | `1.460,00 MT` / `4,00`, como a coluna; nenhum `String.valueOf(...getValueAt` num `PeekItem` | `QuickPeekCellTextHarnessTest` |
| FN-10 | Painel de espreitar com filtro ou ordenação activos | Mostra a linha seleccionada; o populador não volta a converter a linha | `QuickPeekCellTextHarnessTest` |
| FN-11 | `BusinessRuleException` com `toPlainString()` ou `%.2f` | Reprova com ficheiro e linha; usar `DisplayNumbers` | `DisplayNumberFormatHarnessTest` |

## Limites

- No backend, FN-02 só vê chamadas com o formato na mesma linha de `String.format`.
- Um texto `1.500` sem vírgula é lido como 1,5: valores com milhares têm de ser mostrados com decimais.
- FN-11 não detecta um `BigDecimal` concatenado directamente (`"(" + total + ")"`): o tipo não se vê
  no texto. As mensagens existentes foram revistas à mão em 2026-11-06.
- Ficheiros para máquinas (SAF-T, ficheiro bancário, chaves de idempotência) mantêm o ponto.
- Formatos sem separador de milhares (`%.2f`) ficam fora: dependem do locale do processo.
- A suite do desktop corre com `pt-BR` em `argLine`, para ser igual em qualquer máquina.
- `TabStripFitsTest` é ignorado sem a fonte Segoe UI: a largura só é válida no Windows, onde o
  desktop é instalado.
