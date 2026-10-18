# Especificação: Painel Executivo para Sala de Reuniões e TV (War Room TV Dashboard)

## 1. Visão Geral e Propósito
O **Painel Executivo da Sala de Reuniões** (*TV Dashboard / War Room*) foi desenvolvido para atender à necessidade da administração e equipas comerciais de acompanhar o desempenho de vendas em tempo real numa **televisão de grande porte ou projetor em tela cheia (Full Screen)**.

O ecrã permite visualizar com máxima clareza:
- O progresso de cada **Unidade de Negócio** (ex.: Unilever, BAT, Distribuição Alimentar, Bebidas, etc.) contra a sua **Meta Mensal estipulada**.
- Barras de progresso com **percentagens dinâmicas** e códigos de cores semânticos.
- O **Ritmo Diário (*Run-Rate*)** de vendas necessário para atingir o objetivo antes do fecho do mês.
- Ranking dos **Top Artigos Campeões** de venda e distribuição de faturamento (*Market Share* interno).

---

## 2. Princípios de Design para Ecrãs Grandes (3 a 6 Metros de Distância)
1. **Contraste Nobre e Anti-Ofuscamento (Dark Slate & Executive Light):**
   - **Tema Escuro (Dark Slate):** Fundo em `Slate 900` (`#0F172A`) e cartões em `Slate 800` (`#1E293B`) com bordas `Slate 700` (`#334155`).
   - **Tema Claro (Executive Light):** Fundo em `Slate 100` (`#F1F5F9`) e cartões brancos (`#FFFFFF`) com bordas `Slate 200` (`#E2E8F0`).
   - Alternância instantânea através do botão **`[ Tema Claro / Tema Escuro ]`** no cabeçalho.
2. **Cabeçalho Estruturado em 2 Linhas (Zero Sobreposição):**
   - **Linha 1:** Marca `MULTICORE ERP`, Badge da Sala, Relógio Digital em tempo real e Controles de Janela (`Tema`, `Metas`, `Tela Cheia`, `Sair`).
   - **Linha 2:** Filtros de Período (`Hoje`, `Esta Semana`, `Mês Atual`, `Este Ano`) e Seletor de Unidade com `[ Rodízio TV (15s) ]`.
3. **Tipografia de Alta Definição e Isenção de Emojis Unicode:**
   - Valores principais em fontes nítidas (18px a 24px).
   - Proibido o uso de emojis Unicode que renderizam como retângulos `▯` no Java 2D do Windows; status textual limpo com badges semânticos e ícones FontAwesome.
4. **Tela Cheia Real (Full Screen sem bordas do Windows):**
   - Alternância via tecla **`F11`** ou botão dedicado na interface.
   - Tecla **`ESC`** para fechar ou sair do modo de apresentação.

---

## 3. Barras de Progresso e Cores Semânticas (`GoalProgressBar`)
A percentagem da meta atingida define a cor e o estado do negócio:
- Vermelho Alerta (`#EF4444` / `#DC2626`) — *Abaixo de 50% (ALERTA)*.
- Âmbar / Atenção (`#F59E0B` / `#D97706`) — *50.0% a 79.9% (ATENÇÃO)*.
- Azul Safira (`#3B82F6` / `#2563EB`) — *80.0% a 99.9% (NO RITMO)*.
- Verde Esmeralda (`#10B981` / `#059669`) — *100% ou mais (META SUPERADA)*.

Marcadores discretos pontilhados em **25%**, **50%** e **75%** ajudam a balizar visualmente se o negócio já superou as metas quinzenais e semanais.

---

## 4. Modos de Apresentação
1. **Visão Global Consolidada:**
   - Super barra de progresso da empresa no topo.
   - Grelha multicard com todas as Unidades de Negócio ativas lado a lado (destacando unidades participantes com metas ativas e unidades desmarcadas como `FORA DA META`).
   - Gráfico de donut com o share de vendas de cada unidade.
   - Top 5 Artigos mais faturados.
2. **Visão Focada por Unidade de Negócio:**
   - Foco na linha de distribuição selecionada (ex.: Unilever).
   - Metas, realizados, superávit e faturas recentes daquela divisão.
3. **Rodízio Automático / Slideshow TV (15 segundos):**
   - Transição periódica contínua sem necessidade de toque em teclado ou rato.
   - Ideal para TV permanente em salas de reuniões, recepção ou gabinete da administração.

---

## 5. Gestão de Metas e Módulos de Negócio (`BusinessUnitGoalManager`)
- **Seleção Modular de Negócios:** O administrador pode selecionar via caixas de seleção (`JCheckBox`) quais unidades de negócio / módulos participam das metas.
- **Ações em Lote:**
  - `[ Marcar Todos ]` e `[ Desmarcar Todos ]` para rápida configuração de campanhas.
  - `[ Somar na Meta Global ]`: Calcula automaticamente a soma exata de todas as metas dos negócios selecionados, preenchendo a Meta Global da Empresa.
- **Validação Segura:** Inputs numéricos com tratamento sem fechar a janela em caso de digitação incorreta.
- **Persistência Permanente:** Configurações guardadas no sistema operacional via Java Preferences, atualizando o painel de TV em tempo real ao gravar.

---

## 6. Atalhos da Sala de Reuniões
- **`F11`**: Alternar Tela Cheia / Janela.
- **`Espaço`**: Iniciar ou Pausar o Rodízio Automático de ecrãs.
- **`ESC`**: Sair do Painel e voltar ao ERP.
