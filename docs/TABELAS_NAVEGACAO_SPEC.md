# SPEC — Barra lateral de navegação nas tabelas

**Criado em:** 2026-07-23
**Camada:** UI Swing (`com.phcpro.gui.components`)
**Sem backend, sem migração** — apresentação apenas.

---

## 1. Objectivo

Todas as tabelas de listagem passam a ter, **na lateral direita**, uma barra com botões que facilitam
navegar a lista longa (como noutros sistemas):

- **Ir para o topo** (`fas-angle-double-up`)
- **Página acima** (`fas-angle-up`)
- **Página abaixo** (`fas-angle-down`)
- **Ir para o fundo** (`fas-angle-double-down`)

## 2. Princípio de desenho — DRY, um só ponto de injeção

O projecto tem ~80 tabelas em ~15 painéis. **Não** se copiam botões painel a painel. Em vez disso:

- Novo componente reutilizável **`TableNavigator`** (`gui/components`).
- A barra é instalada **centralmente** dentro de **`UIHelper.styleScrollPane(JScrollPane)`** —
  já chamado em ~60 tabelas. Como quase todas as tabelas passam por lá, **uma alteração cobre-as todas**.
- Instala **apenas** quando o conteúdo do scroll é uma `JTable` (não em formulários/catálogos).
- Idempotente: não duplica se `styleScrollPane` for chamado duas vezes (client property `installed`).

## 3. Comportamento (navegação = scroll, não muda a selecção)

Opera sobre a `JScrollBar` vertical do próprio scroll (independente do modelo da tabela):

| Botão            | Acção                                                            |
|------------------|-----------------------------------------------------------------|
| Topo             | `value = minimum`                                                |
| Página acima     | `value -= visibleAmount` (uma página; a `BoundedRangeModel` faz clamp) |
| Página abaixo    | `value += visibleAmount`                                         |
| Fundo            | `value = maximum` (clampa a `maximum - extent`)                  |

Não altera a selecção nem o filtro — é pura navegação visual. Não interfere com `TableFilter`.

## 4. Posicionamento — **fora da tabela**, ao lado (não sobrepõe)

A barra fica **fora** da tabela, na região **EAST do contentor** que aloja o scroll (à direita da
moldura da tabela) — **nunca por cima das células**. É o mesmo padrão que o rodapé de listagem já
usa (`UIHelper.maybeAddListingFooter` adiciona ao SOUTH); aqui adiciona-se ao EAST.

- Anexada só quando o scroll está num contentor `BorderLayout` com o scroll no **CENTER** e o **EAST
  livre** (adiada via `HierarchyListener` até o contentor existir). Se o EAST estiver ocupado, não se
  intromete.
- Cluster de 4 botões num cartão arredondado (`BG_CARD` + `BORDER`), centrado na vertical, com folga
  à esquerda a separar da tabela.

## 5. Estética (CONVENTIONS)

- Ícones **vectoriais** via `UIHelper.icon("fas-…")` — **nunca emojis** (regra do projecto).
- Botões compactos, sem borda, realce ao passar o rato (`ACCENT`); acessíveis por teclado (foco + tooltip PT).

## 6. Camadas / ficheiros

| Ficheiro | Papel |
|----------|-------|
| `gui/components/TableNavigator.java` | Componente + helpers de scroll (`top/pageUp/pageDown/bottom`) |
| `gui/components/UIHelper.java` (`styleScrollPane`) | Ponto de injeção central |

## 7. Fora de âmbito

- Navegação horizontal (as tabelas ajustam-se à largura; o problema é a altura).
- Tabelas que **não** passam por `styleScrollPane` (raras) — não recebem a barra; se aparecer alguma,
  basta garantir que chama `styleScrollPane` ou `TableNavigator.install(scroll)`.
