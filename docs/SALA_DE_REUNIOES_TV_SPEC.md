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
   - Todas as cores vivem em `ExecutiveTvTheme` (única fonte de tokens do pacote).
   - **Tema Escuro (Slate):** fundo `#090E1A`, cabeçalho `#0D1322`, cartões `#11192A` com contorno `#212D45`.
   - **Tema Claro (Executive Light):** fundo `#F1F5F9`, cartões brancos com contorno `#E2E8F0`.
   - Cartões arredondados (raio 16) desenhados à mão: o contorno tem o mesmo raio do fundo.
   - Alternância pelo ícone de sol/lua no cabeçalho.
2. **Cabeçalho em 2 Linhas (Zero Sobreposição):**
   - **Linha 1:** ícone e título «Painel Executivo», linha de contexto (unidade · período) e relógio grande com a data por extenso.
   - **Linha 2:** período num controlo segmentado (`Hoje`, `7 dias`, `Mês`, `Ano`), selector de unidade no tema do painel,
     botão `Rodízio` (com contagem quando activo) e acções só com ícone e dica: Metas, Vendedores, Tema, Ecrã inteiro, Fechar.
3. **Rodapé curto:** atalhos à esquerda; à direita, o estado real da sincronização
   («Actualizado às HH:mm» ou «Sem ligação ao servidor»). Os dados são lidos num `SwingWorker`, fora do EDT.
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

- Cinzento neutro — *sem meta definida (SEM META)*. Sem meta não há «ALERTA» nem «meta superada».

Divisões discretas em **25%**, **50%** e **75%** no trilho e, nos períodos de vários dias, um **marcador do ritmo
esperado** (triângulo e traço): onde a barra devia estar hoje se as vendas fossem constantes. Os números ficam fora
da barra, em rótulos grandes. Estado e cores vêm de `GoalStatus`.

---

## 4. Modos de Apresentação
1. **Visão Global Consolidada:**
   - Cartão principal: faturado em destaque, percentagem da meta, barra com ritmo esperado e quanto falta
     (ou botão «Definir metas» quando não há meta).
   - KPIs 2×2 à direita (`ExecutiveTvMetrics`): Faturas e ticket médio, Ritmo diário, Previsão de fecho ao ritmo
     actual e Necessário por dia nos dias restantes (ou «Acima da meta» / «Meta não definida»).
   - Cartões por unidade de negócio (estado, faturado, % e meta; `FORA DA META` para unidades excluídas), com estado
     vazio quando não há unidades activas.
   - Participação nas vendas: anel com legenda (nome e %); acima de 5 unidades agrupa em «Outras».
   - Top 5 artigos com posição, valor e barra relativa ao primeiro.
2. **Visão Focada por Unidade de Negócio:**
   - O mesmo cartão principal e KPIs aplicados à unidade (`ExecutiveTvSummaryBuilder`, partilhado).
   - Top artigos e últimas faturas da unidade.
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
- **`F11`**: Alternar ecrã inteiro / janela.
- **`Espaço`**: Iniciar ou Pausar o Rodízio Automático de ecrãs.
- **`ESC`**: Sair do Painel e voltar ao ERP.
