# PHC UI Polish — Especificação Visual (SPEC)

## Objectivo

Elevar o Multicore ERP a um nível visual indistinguível de um ERP comercial de topo (PHC CS, Primavera, Sage).
Intervenção puramente visual — sem alterações a Services, DTOs, repositórios ou regras de negócio.

---

## 1. Tabs com Linha de Acento PHC (`styleTabbedPanePHC`)

| Propriedade | Valor |
|---|---|
| Tab activa — fundo | Blend 50 % BG_DARK / BG_CARD (subtil) |
| Tab activa — linha de acento | 3 px, `ACCENT` (Violet-500), CAP_ROUND, y = base da tab − 2 px |
| Tab inactiva — fundo | `BG_DARK` (transparente) |
| Tab inactiva — texto | `TEXT_MUTED` |
| Tab activa — texto | `TEXT_LIGHT` |
| Separador tab/conteúdo | 1 px `GRID` na base da faixa de tabs |
| Aplicação | Todos os painéis internos (11 ficheiros) |
| **Excepção** | `TopNavBar` mantém o estilo preenchido original |

---

## 2. StatusBar no Rodapé (`StatusBar.java`)

| Propriedade | Valor |
|---|---|
| Altura | 24 px |
| Fundo | Tema escuro: `#0F172A` / Tema claro: `#F1F5F9` |
| Linha de topo | 1 px `BORDER` — separador do conteúdo |
| Fonte | Segoe UI / Inter, 11 px, `TEXT_MUTED` |
| Lado esquerdo | ícone `fas-layer-group` · **módulo activo** · separador · ícone `fas-list` · **nº registos** |
| Lado direito | ícone `fas-building` · **empresa** · separador · ícone `fas-user-circle` · **utilizador** · separador · ícone `fas-clock` · **hora** |
| Actualização da hora | Timer interno, 60 s, no EDT |
| Wiring | `MainFrame.navigate()` → `statusBar.setModule(modName)` |

---

## 3. ScrollBars Finas (`SlimScrollBarUI.java`)

| Propriedade | Valor |
|---|---|
| Espessura | 6 px + 2 px de padding = 8 px total |
| Thumb — cor | `ACCENT` com 60 % alpha (153/255) |
| Thumb — forma | `fillRoundRect` com raio = min(largura, altura) — totalmente arredondado |
| Track | Transparente |
| Setas | Removidas (dimensão zero) |
| Aplicação | `UIHelper.styleScrollPane()` — todos os JScrollPane do sistema |

---

## 4. ModernPanel com Borda Adaptada ao Tema

| Situação | Borda |
|---|---|
| `isGradient = true` (KPI cards) | `new Color(255, 255, 255, 20)` — branco translúcido subtil |
| `isGradient = false` (painéis normais) | `UIHelper.BORDER` — adapta ao tema claro/escuro |
| Stroke | 1 px (era 1.5 px) — mais fino, mais profissional |

---

## 5. Tooltips Premium

| Propriedade | Valor |
|---|---|
| Fundo | `BG_CARD` |
| Texto | `TEXT_LIGHT` |
| Fonte | Segoe UI / Inter, 12 px, plain |
| Borda | `LineBorder(BORDER, 1, true)` + `EmptyBorder(4, 8, 4, 8)` |
| Delay inicial | 600 ms |
| Dismiss delay | 8 000 ms |

---

## 6. Novos Componentes Auxiliares

### `SectionHeader.java`
- Cabeçalho de secção reutilizável: ícone opcional + título bold 13 px + linha separadora 1 px `GRID` na base
- Substitui `JLabel` soltos nos tópicos de cada secção

---

## Regras de Aplicação

1. `styleTabbedPanePHC` aplica-se a **todos os painéis internos**; a barra de topo mantém `styleTabbedPane`
2. `styleScrollPane` instala automaticamente `SlimScrollBarUI` — sem alterações nas chamadas existentes
3. `ModernPanel` com gradiente (2-cor) mantém borda branca; sólido usa `UIHelper.BORDER`
4. `StatusBar` só existe no `MainFrame` (não se duplica nos painéis)
