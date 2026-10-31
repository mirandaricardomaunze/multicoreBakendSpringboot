# Formato numérico visível — Especificação e harness

**Última actualização:** 2026-10-31

## Problema

Valores formatados sem locale dependem da máquina. O mesmo valor sai `1.500,50` num Windows em
português e `1,500.50` num Windows em inglês ou no contentor Docker do backend. A CI em Linux expôs
o defeito, que afecta ecrãs, Relatório Z, etiquetas, vales e PDFs de inventário.

## Decisão

- O formato visível é `1.500,50`, definido por `DisplayLocale.NUMBERS` em `contracts`.
- `pt-MZ` não é usado como padrão: o JDK escreve-o com espaço (`1 500,50`), o que mudaria todos os
  valores no ecrã. Adoptá-lo é uma decisão de negócio ainda por tomar. Os poucos pontos que já
  usam `pt-MZ` explicitamente mantêm-se.
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

## Limites

- No backend, FN-02 só vê chamadas com o formato na mesma linha de `String.format`.
- Formatos sem separador de milhares (`%.2f`) ficam fora: dependem do locale do processo.
- A suite do desktop corre com `pt-BR` em `argLine`, para ser igual em qualquer máquina.
- `TabStripFitsTest` é ignorado sem a fonte Segoe UI: a largura só é válida no Windows, onde o
  desktop é instalado.
