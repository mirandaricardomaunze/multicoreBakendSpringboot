# Cores do cromo que seguem os três temas

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/ThemeColorsHarnessTest.java`

## Problema

O menu lateral, a barra de topo, a pesquisa global, os paginadores e vários gráficos escolhiam a cor
com `UIHelper.isLight() ? new Color(..) : new Color(..)`. São só dois ramos: o tema **Alto Contraste**
caía no ramo escuro e ficava com fundos azul-ardósia e bordas de pouco contraste, precisamente onde
devia ter preto puro e branco.

## Regras

| ID    | Regra |
|-------|-------|
| CT-01 | No Alto Contraste o cromo é preto, as bordas fortes são brancas e o texto/ícones do cromo lêem-se a 7:1 ou mais. |
| CT-02 | Cada token tem valor próprio em claro, escuro e alto contraste. |
| CT-03 | Texto e ícones do cromo têm contraste de leitura (≥ 4.5:1) em todos os temas. |
| CT-04 | Ninguém escreve `isLight() ? new Color(..) : new Color(..)`: usa `ThemeColors` ou os tokens do `UIHelper`. Excepções nomeadas: `KpiCardPalette` (famílias de acento dos KPI) e `ProfitAnalyticsWidget` (tons de estado). |

## Tokens (`ThemeColors`)

| Token | Uso | Claro | Escuro | Alto contraste |
|-------|-----|-------|--------|----------------|
| `chromeBg` | menu lateral, barra de topo | branco | 24,32,47 | preto |
| `chromeBorder` | linha entre cromo e conteúdo | 226,232,240 | 45,55,72 | branco |
| `hairline` | separadores, grelha de gráficos | 226,232,240 | 51,65,85 | 160 |
| `outline` | moldura de pesquisa, teclas | 203,213,225 | 71,85,105 | branco |
| `textSoft` | texto secundário do cromo | 100,116,139 | 203,213,225 | 224 |
| `control` | ícones de controlos activos | 51,65,85 | 226,232,240 | branco |
| `controlSubtle` | controlos desactivados, rótulos discretos | 148,163,184 | 100,116,139 | 170 |
| `subtleFill` | fundo de chips, trilhos, hover | 241,245,249 | 30,41,59 | 40 |
| `hoverOverlay` / `restOverlay` | véu de hover / repouso | preto translúcido | branco translúcido | branco mais forte |

Os fundos, o texto e as bordas gerais continuam no `Theme`/`UIHelper` (`BG_CARD`, `TEXT_LIGHT`,
`BORDER`, `TABLE_HEADER_BG`, `ROW_ALT`…). Os pares que já eram exactamente esses valores passaram a
usá-los directamente.
