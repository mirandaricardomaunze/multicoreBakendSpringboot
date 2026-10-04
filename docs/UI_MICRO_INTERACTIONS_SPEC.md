# Especificação Técnica: Micro-Interações e Transições Visuais (UI Líquida)

## 1. Visão Geral e Objectivo
Esta especificação define o comportamento das micro-interações, animações suaves e componentes táteis no Multicore ERP (Java Swing Desktop), transformando interacções bruscas em transições visuais fluidas ("liquid UI"), preservando rigorosamente o desempenho e garantindo que o EDT (Event Dispatch Thread) nunca seja bloqueado.

---

## 2. Princípios de Design e Ergonomia

1. **Sem Cortes Abruptos:** A passagem do cursor sobre botões (hover) deve transitar a cor de forma contínua através de interpolação linear (blend) em vez de troca instantânea.
2. **Feedback Táctil:** Ao pressionar um botão, um ligeiro deslocamento (1px vertical) e escurecimento visual confirmam a acção antes do disparo do listener.
3. **Notificações Flutuantes Vivas:** Toasts surgem com animação suave de subida e desvanecimento (fade-in), mantendo um indicador de tempo e suporte a fecho instantâneo ao clique.
4. **Badges em Pílula (Pills):** Elementos de estado e categorias usam bordas totalmente arredondadas, fundo semitransparente na cor semântica e texto de alto contraste.
5. **Zero Emojis / Símbolos Unicode Brutos:** Toda a iconografia deve usar FontAwesome vetorial via `UIHelper.icon(...)`, eliminando riscos de renderização de quadrinhos (`▯`) no Windows.
6. **Desempenho e Limpeza de Recursos:** Todos os temporizadores de animação utilizam taxas de atualização eficientes (50–60 fps, 16–20ms por frame) com duração ultra-curta (100–120ms) e cancelamento obrigatório em `removeNotify()`.

---

## 3. Componentes e Comportamentos

### 3.1 `ModernButton` (Transição Suave de Hover & Clique)
- **Temporizador de Fade:** Duração de ~120ms (6 passos de 20ms).
- **Interpolação de Cor:** `UIHelper.blendColors(normalColor, hoverColor, progress)`.
- **Efeito Pressionado:** Quando o modelo está pressionado, aplica um deslocamento de 1px e tonalidade ativa.
- **Headless Safe:** Em ambientes sem interface gráfica (testes unitários), o progresso ajusta-se imediatamente sem aguardar pelo timer.

### 3.2 `ToastManager` (Slide & Fade)
- **Estrutura:** Janela flutuante ancorada à janela principal com cantos arredondados (`ModernPanel`), ícone semântico, texto legível e botão de fecho rápido.
- **Entrada:** Fade-in de opacidade (quando suportado pelo ambiente gráfico) e deslocamento vertical suave de 8px.
- **Saída:** Fade-out progressivo antes da destruição da janela (`dispose()`).

### 3.3 `StatusBadge` (Pílulas de Estado Reutilizáveis)
- Fundo semitransparente (alpha ~15-25%) na cor do estado (Verde, Âmbar, Vermelho, Azul, Cinzento).
- Bordas em cápsula/pílula (`arc = height`).
- Texto em negrito com contraste superior a 4.5:1 (WCAG AA).
- Métodos de conveniência: `createSuccess(text)`, `createWarning(text)`, `createDanger(text)`, `createInfo(text)`.

---

## 4. Estratégia de Testes (Harness)
A conformidade é assegurada por `UiMicroInteractionsHarnessTest`:
- Verificação matemática da mistura de cores (`blendColors`).
- Validação do ciclo de vida da animação de botões e paragem em `removeNotify()`.
- Verificação do contraste e geometria do `StatusBadge`.
- Validação do empacotamento e acessibilidade das notificações flutuantes.
