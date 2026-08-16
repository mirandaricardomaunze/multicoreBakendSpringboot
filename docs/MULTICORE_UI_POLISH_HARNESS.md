# Multicore UI Polish — Harness de Verificação Manual

## Pré-condições

- Servidor Spring Boot em execução (porta 8080 por defeito)
- Utilizador de teste: `maria` / `password` (ou qualquer conta válida)

---

## H-1: StatusBar no Rodapé

**Como verificar:**

1. Iniciar o desktop — o rodapé deve estar visível com 24 px de altura, fundo escuro, separador de 1 px
2. O lado esquerdo deve mostrar `· Painel Inicial` (antes de autenticar: campos vazios)
3. Após login: lado direito deve mostrar a empresa e o nome do utilizador
4. Navegar para **Vendas & Faturação** → texto esquerdo passa a `Vendas & Faturação`
5. Navegar para **Stock & Armazéns** → texto passa a `Stock & Armazéns`
6. Esperar até próximo minuto (ou forçar ticks no Timer) → hora actualiza sem piscar

**Critérios de aceitação:**

- [ ] Barra sempre visível em todos os módulos
- [ ] Nome do módulo actualiza ao navegar
- [ ] Empresa e utilizador corretos após login
- [ ] Hora no formato `HH:mm`
- [ ] Fundo adapta ao tema claro (cinza claro) e escuro (azul-marinho)

---

## H-2: Tabs com Linha de Acento Multicore

**Como verificar:**

1. Abrir **Vendas & Faturação** → tabs visíveis (Faturas, Encomendas, …)
2. Tab activa: **sem fundo sólido preenchido**, apenas um fundo subtil (meio-tom)
3. Na base da tab activa: linha violeta de ~3 px (cor `ACCENT`)
4. Tab inactiva: texto em cinza (`TEXT_MUTED`), sem qualquer destaque
5. Separador horizontal visível entre a faixa de tabs e o conteúdo
6. Repetir em: **Compras**, **Stock**, **RH**, **Fiscal**, **Config**, **Plataforma**

**Critérios de aceitação:**

- [ ] Linha de acento visível na tab activa (não fundo cheio)
- [ ] Tabs inactivas em cinza
- [ ] Separador entre tabs e conteúdo
- [ ] Barra de topo (módulos) **não alterada** — mantém fundo preenchido original

---

## H-3: ScrollBars Finas

**Como verificar:**

1. Abrir **Vendas & Faturação** → tabela de faturas com dados suficientes para scroll
2. Scroll bar vertical: deve ser fina (~8 px total), thumb violeta arredondado, sem setas
3. Rolar com o rato → thumb move-se suavemente
4. Redimensionar janela para forçar scroll horizontal → barra horizontal também fina
5. Verificar tema claro → thumb ainda visível (alpha 60 % sobre fundo claro)

**Critérios de aceitação:**

- [ ] Espessura ≤ 8 px (visualmente "fina")
- [ ] Thumb arredondado, sem setas
- [ ] Cor do thumb = Violet com transparência
- [ ] Scroll funciona correctamente (não congela o EDT)

---

## H-4: ModernPanel Borda Adaptada ao Tema

**Como verificar:**

1. Abrir o **Painel Inicial** em tema **escuro** → KPI cards têm borda branca subtil (gradiente)
2. Trocar para tema **claro** (ícone sol/lua na barra de topo)
3. KPI cards → borda ainda visível mas discreta (branco translúcido subtil sobre gradiente)
4. Outros painéis (ex.: cards de filtros no Comercial) → borda cinza `BORDER` do tema claro

**Critérios de aceitação:**

- [ ] Borda dos KPI cards visível em ambos os temas
- [ ] Borda dos painéis normais segue `UIHelper.BORDER` (cinza em claro, subtil em escuro)

---

## H-5: Tooltips Premium

**Como verificar:**

1. Passar o rato por cima de um botão com tooltip (ex.: botão de imprimir no Comercial)
2. Aguardar 600 ms → tooltip aparece
3. Fundo deve ser `BG_CARD` (escuro/cinza conforme tema), borda discreta, fonte 12 px

**Critérios de aceitação:**

- [ ] Tooltip com fundo do tema (não fundo branco padrão do Metal L&F)
- [ ] Delay de 600 ms (não instantâneo)
- [ ] Texto legível (contraste adequado)

---

## Compile Check

```
mvn -o compile
```

Saída esperada: `BUILD SUCCESS` sem warnings de compilação.

---

## Verificação de Regressão

- Duplo-clique numa linha de tabela → inspector de detalhes ainda abre
- Formulários de criação/edição → campos ainda funcionais
- POS → caixa operacional sem alterações visuais indesejadas
- Tema claro ↔ escuro → troca sem erros
