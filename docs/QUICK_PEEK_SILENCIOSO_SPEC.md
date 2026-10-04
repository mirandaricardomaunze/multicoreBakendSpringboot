# SPEC: «Quick Peek» Silencioso em Tabelas Multicore ERP (Tecla Espaço)

## 1. Contexto e Motivação
Em sistemas ERP corporativos tradicionais, inspecionar os detalhes de um registo (ex.: linhas de fatura, dados do cliente, lotes de stock, endereços de entrega) exige frequentemente dar duplo-clique e abrir um diálogo modal pesado (`JDialog`). Isso bloqueia a tela, escurece a interface e obriga o operador a fechar o modal com `ESC` ou rato antes de passar para a próxima linha.

Inspirado em padrões de excelência de interface moderna (macOS Quick Look com barra de espaço, Linear, SAP Fiori Side-by-Side Inspection, IntelliJ Quick Definition), o **«Quick Peek» Silencioso** permite visualizar todos os dados vitais da linha selecionada num painel lateral contextual (*drawer*), sem modais e sem interrupção do fluxo de navegação por teclado.

---

## 2. Princípios de Desenho e Ergonomia Silenciosa

1. **Zero Ruído Visual (Zero Visual Clutter):**
   - O painel lateral permanece completamente invisível (`visible = false`, ocupando 0px) até ser invocado.
   - Não há popups invasivos, sobreposições de ecrã ou diálogos modais bloqueadores.
2. **Navegação Contínua por Teclado:**
   - **Tecla Espaço (`SPACE`):** Abre ou fecha instantaneamente o Quick Peek da linha selecionada.
   - **Setas Cima / Baixo (`UP` / `DOWN`):** Com o Quick Peek aberto, navegar pelas linhas da tabela atualiza o painel lateral em tempo real sem qualquer recarga de ecrã ou tremor.
   - **Tecla `ESC`:** Fecha imediatamente o painel lateral e devolve o foco total à tabela.
3. **Acesso Opcional por Rato:**
   - Botão discreto no cabeçalho/barra de filtros com ícone de olho (`fas-eye`) e tooltip executivo `"Espreitar detalhes (Espaço)"`.
   - Botão de fechar discreto (`fas-times`) no topo do painel.
4. **Layout Adaptativo e Responsivo:**
   - Largura compacta (~320px) posicionada no lado direito do cartão da tabela (`BorderLayout.EAST`).
   - Respeita o tema ativo (`Theme.LIGHT` ou `Theme.DARK`), utilizando cartões internos e tipografia canónica de `UIHelper`.

---

## 3. Arquitetura dos Componentes

```
┌────────────────────────────────────────────────────────────────────────┐
│ Card da Tabela (BorderLayout)                                          │
│                                                                        │
│ ┌──────────────────────────────────────┐  ┌──────────────────────────┐ │
│ │ Tabela de Dados (CENTER)             │  │ QuickPeekPanel (EAST)    │ │
│ │                                      │  │                          │ │
│ │ ┌─ Linha 1                           │  │ [Badge] Fatura FT 2026/01│ │
│ │ ├► Linha 2 [SELECIONADA]             │  │ Total: 45 200,00 MT      │ │
│ │ └─ Linha 3                           │  │ Cliente: Mozaic Lda      │ │
│ │                                      │  │ Data: 04/10/2026         │ │
│ │                                      │  │ Estado: [Aprovado]       │ │
│ │ (Navegação UP/DOWN atualiza o painel)│  │                          │ │
│ │ (Espaço / ESC abre e fecha)          │  │ [Ação: Abrir Completo]   │ │
│ └──────────────────────────────────────┘  └──────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Componentes:
- **`QuickPeekPanel` (`mz.multicore.erp.gui.components.QuickPeekPanel`):**
  - Painel lateral especializado que renderiza dados canónicos de uma linha de tabela (identificador, status badge, KPIs de valor, campos chave, itens e ações rápidas).
- **`TableQuickPeekController` (`mz.multicore.erp.gui.components.TableQuickPeekController`):**
  - Controlador universal que acopla a qualquer `JTable` e seu contentor de cartão:
    - Escuta tecla `SPACE` e `ESC`.
    - Escuta `ListSelectionListener` para atualizar o painel reativamente conforme o operador sobe/desce na grelha.
    - Suporta extrator genérico baseado em metadados de colunas ou `QuickPeekDataProvider` customizado.

---

## 4. Regras Não-Negociáveis
- **Tamanho de código:** Todos os ficheiros criados/alterados devem respeitar o limite estrito $\le 1000$ linhas.
- **Aderência arquitetural:** Nenhuma dependência direta de backend/JPA/Repository no desktop; os dados exibidos vêm da própria tabela ou do DTO em cache.
- **Acessibilidade:** Nomes acessíveis para leitores de tela em todos os botões e áreas do painel.
- **Testes Automatizados:** Teste de harness completo cobrindo tecla espaço, navegação reativa de linhas, tecla ESC e compatibilidade de temas.
