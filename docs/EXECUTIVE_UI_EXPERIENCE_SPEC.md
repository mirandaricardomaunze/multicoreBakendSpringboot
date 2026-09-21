# Suíte de Experiência Executiva do UI — Especificação Técnica

## 1. Visão Geral
Esta especificação define os padrões de modernização visual e ergonómica aplicados na aplicação Desktop do Multicore ERP, complementando o motor Look & Feel FlatLaf 3.7.2 com recursos visuais de classe executiva.

## 2. Pilares de Experiência

### 2.1 Gráficos Vetoriais Modernos (`SimpleBarChart` e `SimplePieChart`)
- **Gradientes Suaves:** Barras desenhadas com `GradientPaint` partindo do tom base aclarado no topo até à cor canónica na base.
- **Cantos Arredondados:** Acabamento arredondado no topo de cada barra com anti-aliasing activo.
- **Linhas de Grelha de Referência:** Três linhas de benchmark horizontais pontilhadas a 25%, 50% e 75% da escala máxima para fácil leitura de valores.
- **Tooltips Interativos:**
  - Em `SimpleBarChart`: Ao mover o cursor sobre uma barra, exibe tooltip formatado com a etiqueta e o valor monetário completo em Meticais (ex: `Receitas: 1.250.400,00 MT`).
  - Em `SimplePieChart`: Ao mover o cursor sobre uma fatia, destaca a fatia activa e exibe tooltip com a etiqueta, valor em MT e percentagem.

### 2.2 Indicadores de Tendência nos Cards de KPI (`TrendBadge`)
- **Componente Reutilizável:** `TrendBadge` integrado a `KpiCard`:
  - Crescimento positivo: `▲ +X.X%` com fundo translúcido esmeralda (`Color(16, 185, 129, 35)`) e texto `APPROVED_GREEN`.
  - Queda/decréscimo: `▼ -X.X%` com fundo translúcido rosa/vermelho (`Color(239, 68, 68, 35)`) e texto `REJECTED_RED`.
  - Estabilidade: `— 0.0%` com fundo neutro translúcido.
- **Apresentação:** Integrado verticalmente ao lado ou abaixo do valor do KPI nos painéis executivos.

### 2.3 Pesquisa Global Rápida na Barra de Topo (`TopNavBar` & `Ctrl+K`)
- **Gatilho Visual:** Um botão estilo *pill* na barra superior: `[ 🔍 Pesquisar em todo o ERP… (Ctrl+K) ]`.
- **Atalho de Teclado:** Registo de atalho global `Ctrl+K` na janela principal (`MainFrame`), abrindo o `GlobalSearchDialog` (Command Palette / Spotlight) instantaneamente de qualquer ecrã do sistema.

### 2.4 Backdrop Modal Translúcido (`ModernFormDialog`)
- **Efeito de Foco:** Ao abrir qualquer formulário modal baseado em `ModernFormDialog`, instala temporariamente no `glassPane` da janela pai um véu translúcido escurecido (`Color(0, 0, 0, 95)`).
- **Restauração Limpa:** Ao fechar ou dispensar o modal, o `glassPane` original é imediatamente restaurado e ocultado, sem deixar resíduos de eventos ou vazamento de memória.

## 3. Conformidade Arquitetural
- Todo o código reside exclusivamente no módulo `desktop`.
- Módulos `backend` e `contracts` permanecem 100% livres de referências de UI Swing ou FlatLaf.
- Princípio de Responsabilidade Única (SRP) e DRY preservados em todos os componentes.
