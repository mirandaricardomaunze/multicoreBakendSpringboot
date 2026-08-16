# Spec — Ícones nos modais de formulário

> Todos os modais de formulário (`ModernFormDialog`) mostram um **ícone vetorial contextual**
> no título e ícones nos botões de ação, em vez de só texto. Alinhado com a skill
> [`.claude/skills/multicore-icons`](../.claude/skills/multicore-icons/SKILL.md) e com
> [MODAIS_CONTIDOS_SPEC.md](MODAIS_CONTIDOS_SPEC.md).

**Última actualização:** 2026-06-28

## Problema

Os modais de criação/edição abriam só com texto no cabeçalho ("Novo Fornecedor", "Emitir Nova
Fatura"…). Sem um glifo de domínio o aspeto era menos profissional e menos legível de relance —
ao contrário do Multicore, onde cada documento/ficha tem um ícone identificador.

## Decisão

- **Ponto central = `ModernFormDialog`.** Toda a iconografia vive no componente; os ~6 pontos de
  chamada não precisam de mudar (mesma filosofia de [MODAIS_CONTIDOS_SPEC.md](MODAIS_CONTIDOS_SPEC.md)
  — cobrir todos sem tocar nos call sites).
- **Ícone no título.** O `JLabel` do título recebe `UIHelper.icon(code, 22, ACCENT_BLUE)` com
  `iconTextGap = 12`. O mesmo glifo é usado como `setIconImage(...)` da janela (barra de título do SO).
- **Ícone deduzido do título.** Novo construtor `ModernFormDialog(parent, title, iconCode, content)`
  permite ícone explícito; o construtor de 3 argumentos chama `iconForTitle(title)`, uma heurística
  que mapeia palavras do título para o vocabulário canónico da skill `multicore-icons`:
  - domínio ganha sobre verbo (ex.: "Editar Fornecedor" → `fas-truck`, não `fas-edit`);
  - fatura→`fas-file-invoice`, fornecedor→`fas-truck`, encomenda→`fas-file-signature`,
    compra/entrada→`fas-download`, categoria→`fas-tags`, cliente→`fas-address-book`,
    produto→`fas-boxes`, armazém→`fas-warehouse`, utilizador→`fas-user-plus`;
  - verbo genérico: editar→`fas-edit`; novo/nova/cadastrar/registar/adicionar/criar→`fas-plus`;
  - omissão → `fas-file-alt`.
- **Botões de ação.** Gravar já tinha `fas-save`; Cancelar passa a ter `fas-times`. Tamanho 14 px
  (convenção da skill para ícones em botão).

## Atualização 2026-06-28 — cabeçalho premium em TODOS os modais

- **Cabeçalho premium partilhado** (`UIHelper.buildPremiumHeader`): badge quadrado de cantos
  arredondados (acento sólido + ícone branco) à esquerda, **título** + **subtítulo** à direita,
  **divisória** por baixo. Mesma identidade visual em todos os modais.
- **`ModernFormDialog`** passou a usar este cabeçalho + divisória premium acima dos botões; novo
  construtor com **subtítulo**. `iconForTitle` foi promovido a `UIHelper` (público, partilhado).
- **Modais legados** baseados em `JOptionPane.showConfirmDialog(...)` (Transferência, Cadastrar
  Produto, Armazém, Ajuste, Recibo, NC/ND, Colaborador, Férias, Falta, Promoção, Cliente, …) ganham
  o **mesmo cabeçalho** via `UIHelper.premiumDialogContent(icon, titulo, subtitulo, form)` — troca
  directa de `makeDialogScrollable(form)` na chamada, **sem mexer no fluxo OK/Cancel**. Cobre os ~21
  call sites em 8 painéis.

## Atualização 2026-06-28 (2) — migração total + grelha + botões estilizados

- **Todos os ~21 modais legados migraram de `JOptionPane` para `ModernFormDialog`.** Deixaram de usar
  os botões nativos OK/Cancel: agora têm **botões estilizados com ícones** (Cancelar `fas-times` +
  confirmação `fas-save`/`fas-check`/`fas-money-bill-wave`…). O fluxo `int opt = showConfirmDialog(...);
  if (opt != OK_OPTION)` passou a `boolean ok = new ModernFormDialog(...).showDialog(); if (!ok)`.
  Os botões vivem no rodapé fixo do diálogo → **nunca são cortados** por modais altos.
- **Botão de confirmação configurável:** `ModernFormDialog.setConfirmButton(label, iconCode)` para os
  casos em que "Gravar" não é a acção certa — ex.: Receber (`fas-money-bill-wave`), Pagar, Emitir
  (`fas-check`), Confirmar.
- **Inputs em grelha:** `UIHelper.createDialogForm` passou a dispor os pares (label, campo) numa
  **grelha de 2 colunas** (label sobre campo em cada célula) — aspecto profissional, mais compacto e
  menos altura. Variante `createDialogForm(int columns, …)` para outras larguras.

## Não-objetivos

- Não inventar ícones fora do catálogo FontAwesome 5 Free Solid nem fora do vocabulário da skill.
- Não alterar a lógica de validação/persistência de cada formulário (só o invólucro do diálogo mudou).

## Notas técnicas

- `UIHelper.icon(code, size, color)` e `UIHelper.iconImage(code, size, color)` já existem; nada de
  novas dependências (Ikonli + FontAwesome 5 já no `pom.xml`).
- Acrescentos ao vocabulário da skill nesta iteração: **Categoria → `fas-tags`**,
  **Fornecedor → `fas-truck`**.
