# Formato numérico visível — Especificação e harness

**Última actualização:** 2026-10-31

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

## Limites

- No backend, FN-02 só vê chamadas com o formato na mesma linha de `String.format`.
- Um texto `1.500` sem vírgula é lido como 1,5: valores com milhares têm de ser mostrados com decimais.
- Formatos sem separador de milhares (`%.2f`) ficam fora: dependem do locale do processo.
- A suite do desktop corre com `pt-BR` em `argLine`, para ser igual em qualquer máquina.
- `TabStripFitsTest` é ignorado sem a fonte Segoe UI: a largura só é válida no Windows, onde o
  desktop é instalado.
