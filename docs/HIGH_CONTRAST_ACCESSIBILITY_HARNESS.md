# Matriz de Testes & Harness: Modo de Alto Contraste Acessível (HARNESS-HCON-001)

Este documento especifica a suíte de validação automatizada para o Modo de Alto Contraste Acessível (`HighContrastThemeHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **HCON-01** | Integridade da Paleta `Theme.HIGH_CONTRAST` | Contém 10 cores canónicas não-nulas na ordem exata de `palette()` | 🟢 PASS |
| **HCON-02** | Resolução por Identificadores | `Theme.byId` resolve `"high_contrast"`, `"highcontrast"`, `"contrast"` para a mesma instância | 🟢 PASS |
| **HCON-03** | WCAG 2.1 AAA em Texto Primário | Razão de contraste de `textPrimary` sobre `bg` e `card` é $\ge 7.0:1$ (real: $\ge 20:1$) | 🟢 PASS |
| **HCON-04** | WCAG 2.1 AAA em Texto Muted | Razão de contraste de `textMuted` sobre `bg` e `card` é $\ge 7.0:1$ (real: $\ge 15:1$) | 🟢 PASS |
| **HCON-05** | Legibilidade Dinâmica (`readableTextOn`) | Retorna texto legível ($\ge 4.5:1$) em todas as superfícies e bordas de destaque | 🟢 PASS |
| **HCON-06** | Ciclo Circular de Temas | `UIHelper.cycleTheme()` transiciona na ordem `DARK -> LIGHT -> HIGH_CONTRAST -> DARK` | 🟢 PASS |
| **HCON-07** | Aplicação de Slots em `UIHelper` | `applyTheme(HIGH_CONTRAST)` atualiza `BG_DARK`, `BORDER`, `TEXT_LIGHT` e `isHighContrast()` | 🟢 PASS |
| **HCON-08** | Decomposição e Linhas de Código | Todas as classes Swing modificadas (`Theme`, `UIHelper`, `MainFrame`, `ConfigPanel`) $\le 1000$ linhas | 🟢 PASS |
