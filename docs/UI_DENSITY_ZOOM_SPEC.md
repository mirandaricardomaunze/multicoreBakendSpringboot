# Especificação Técnica: Densidade de Interface & Escala de Tipografia / Zoom Operacional (SPEC-DENS-001)

## 1. Visão Geral & Objetivos
Esta especificação define o subsistema de **Densidade de Interface & Escala de Tipografia / Zoom Operacional** (`UiDensity` e `UiDensityManager`).
O objetivo é proporcionar flexibilidade visual e operacional aos postos de trabalho do Multicore ERP em Moçambique, desde portáteis com ecrãs compactos de 13"-14" (utilizados frequentemente em balcões de lojas, armazéns e caixas móveis) até monitores amplos de secretária de 24"-27" da administração e contabilidade.

## 2. Níveis Canónicos de Densidade (`UiDensity`)

| Densidade | ID | Altura de Linha de Tabela | Altura de Controlo / Input | Escala Tipográfica | Caso de Uso Primário |
|---|---|---|---|---|---|
| **COMPACT** | `"compact"` | **28 px** | **32 px** | **0.90x** (90%) | Ecrãs de 13"-14", POS móvel, maximização de linhas visíveis sem scroll (+30% densidade) |
| **STANDARD** | `"standard"` | **36 px** | **38 px** | **1.00x** (100%) | Padrão equilibrado recomendado para uso diário |
| **COMFORTABLE** | `"comfortable"` | **44 px** | **44 px** | **1.15x** (115%) | Monitores 24"-27", touchscreens, baixa visão e alvos de clique amplos |

## 3. Arquitetura do Gestor de Densidade (`UiDensityManager`)
1. **Singleton & Thread-Safety**:
   - `UiDensityManager.getInstance()` gere a densidade ativa com acesso seguro e concorrente.
2. **Ciclo de Densidades (`cycleDensity()`)**:
   - Transita circularmente: `STANDARD` $\rightarrow$ `COMPACT` $\rightarrow$ `COMFORTABLE` $\rightarrow$ `STANDARD`.
3. **Persistência em Preferências**:
   - Chave `"density"` no nó `java.util.prefs.Preferences` (`"mz/multicore/erp/ui"`).
   - Carregado automaticamente no arranque da aplicação pelo `UIHelper.initGlobalTheme()`.
4. **Aplicação no Sistema Swing & Look & Feel**:
   - Atualiza `UIManager.put("Table.rowHeight", density.tableRowHeight)`.
   - Repinta as tabelas e janelas abertas preservando a integridade dos renderizadores e temas.
   - Fornece gancho de ouvintes de alteração (`addChangeListener(Runnable)`).

## 4. Integração na Interface Gráfica
1. **ConfigPanel**:
   - Botão de comutação na barra superior com etiqueta reativa ao modo atual (ex.: `"Densidade: Padrão (Clique p/ Compacto)"`).
2. **Command Palette (`Ctrl+K`)**:
   - Comando rápido `"Alternar Densidade da Interface (Compacto / Padrão / Confortável)"`.

## 5. Critérios de Aceitação & Conformidade
- Todos os testes da suíte `UiDensityZoomHarnessTest` passam a 100%.
- A comutação de densidade não quebra contraste WCAG nem layouts de tabelas.
- Todas as classes Swing modificadas mantêm-se estritamente $\le 1000$ linhas.
