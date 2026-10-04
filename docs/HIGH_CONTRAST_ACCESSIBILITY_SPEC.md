# Especificação Técnica: Modo de Alto Contraste Acessível & Operação Exterior (SPEC-HCON-001)

## 1. Visão Geral & Objetivos
Esta especificação define o **Modo de Alto Contraste Acessível & Operação Exterior** (`Theme.HIGH_CONTRAST`), desenhado para:
1. **Ambientes de Alta Luminosidade / Outdoor**: Terminais POS, balcões de atendimento ao ar livre, armazéns, estaleiros, feiras e postos de combustível em Moçambique onde a luz solar direta compromete a visibilidade de temas com paletas suaves ou pastéis.
2. **Acessibilidade Visual (WCAG 2.1 Nível AAA)**: Garantir contraste superior ao padrão mínimo internacional (razão $\ge 7.0:1$) para operadores com baixa visão, cataratas, daltonismo ou fadiga visual prolongada.
3. **Identidade Visual Nítida**: Uso de fundos pretos profundos (`#000000`), textos brancos puros (`#FFFFFF`), bordas brancas bem marcadas e elementos de realce de alta distinção.

## 2. Paleta Canónica `Theme.HIGH_CONTRAST`
O tema expõe as 10 cores fundamentais do sistema na ordem canónica de `palette()`:

| Slot | Cor Hex | Cor RGB | Propósito Visual | Contraste sobre Fundo |
|---|---|---|---|---|
| `bg` | `#000000` | `rgb(0, 0, 0)` | Fundo geral da janela | Base |
| `card` | `#0A0A0A` | `rgb(10, 10, 10)` | Fundo de superfícies e cartões | Base |
| `textPrimary` | `#FFFFFF` | `rgb(255, 255, 255)` | Texto principal e cabeçalhos | **21.0:1** (WCAG AAA) |
| `textMuted` | `#E0E0E0` | `rgb(224, 224, 224)` | Texto secundário e legendas | **~15.8:1** (WCAG AAA) |
| `grid` | `#505050` | `rgb(80, 80, 80)` | Linhas de tabela e separadores | Visibilidade nítida |
| `tableHeaderBg` | `#121212` | `rgb(18, 18, 18)` | Cabeçalhos de tabelas e listas | Distinção de secção |
| `rowAlt` | `#181818` | `rgb(24, 24, 24)` | Linhas alternadas de tabelas (zebra) | Conforto de leitura |
| `fieldBg` | `#000000` | `rgb(0, 0, 0)` | Fundo de campos de texto e inputs | Previne ofuscamento |
| `border` | `#FFFFFF` | `rgb(255, 255, 255)` | Bordas de botões, cartões e campos | Delimitação sob sol |
| `selectionBg` | `#0066CC` | `rgb(0, 102, 204)` | Linha ou item selecionado | Alto contraste com branco |

## 3. Arquitetura & Comutação em Tempo Real
1. **Identificadores Aceites**: O método `Theme.byId(id)` reconhece `"high_contrast"`, `"highcontrast"` e `"contrast"`.
2. **Ciclo de Temas (`UIHelper.cycleTheme()`)**: Transita circularmente:
   `DARK` $\rightarrow$ `LIGHT` $\rightarrow$ `HIGH_CONTRAST` $\rightarrow$ `DARK`.
3. **Métodos Auxiliares**:
   - `UIHelper.isHighContrast()`: indica se o tema atual é de alto contraste.
   - `UIHelper.contrastRatio(Color a, Color b)`: cálculo exato da luminância relativa WCAG.
   - `UIHelper.meetsWcagAaa(Color fg, Color bg)`: valida se o contraste é $\ge 7.0:1$.
4. **Comutação Rápida no TopBar e Command Palette**:
   - TopBar exibe ícone `fas-adjust` quando em Alto Contraste.
   - Command Palette (`Ctrl+K`) inclui comando rápido para ativação imediata do Modo Alto Contraste.
   - `ConfigPanel` reflete o estado atual no botão de temas.

## 4. Critérios de Aceitação & Conformidade
- Todos os testes da suíte `HighContrastThemeHarnessTest` passam a 100%.
- Todas as combinações de texto primário e secundário sobre `bg` e `card` cumprem rigorosamente WCAG 2.1 AAA ($\ge 7.0:1$).
- Todas as classes Swing modificadas permanecem estritamente $\le 1000$ linhas.
