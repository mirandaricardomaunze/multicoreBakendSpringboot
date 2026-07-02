# Spec — Modais contidos na janela principal

> Todos os diálogos modais da aplicação desktop ficam **dentro da janela principal**: ao abrir e
> também **ao serem arrastados** — nunca saem para fora dela. Alinhado com
> [UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md).

**Última actualização:** 2026-06-27

## Problema

Os modais (`JDialog`/`JOptionPane`) são janelas de topo do SO: podiam abrir maiores que a janela
principal (dimensionados ao **ecrã**) e podiam ser **arrastados para fora** da aplicação, ficando
parcial ou totalmente fora da janela principal — aspeto pouco profissional e confuso.

## Decisão

- **Referência = janela principal.** `MainFrame` regista-se via `UIHelper.registerMainWindow(this)`.
  `UIHelper.mainArea()` devolve os limites da janela principal (ou o ecrã, como recurso, antes de
  estar pronta).
- **Conter ao abrir:** `UIHelper.containWithinMain(dialog)` limita o modal a ~94% da janela e
  centra-o sobre ela. Usado pelo `ModernFormDialog`; `UIHelper.makeDialogScrollable` passou a
  dimensionar-se pela janela principal (não pelo ecrã).
- **Conter ao arrastar (novo):** `registerMainWindow` instala **um** listener global
  (`Toolkit.addAWTEventListener`, `COMPONENT_MOVED`). Sempre que um `Dialog` se move, é reposicionado
  para ficar totalmente dentro da janela principal (efeito "encosta na margem"). Um guarda
  (`clampingModal`) evita recursão do `setLocation`. Cobre **todos** os modais sem tocar nos ~333
  pontos de chamada de diálogos.

## Não-objetivos

- Não converter modais em `JInternalFrame`/overlay (continuam janelas do SO, apenas confinadas).
- Não confinar a própria janela principal nem janelas top-level (login/arranque).
- Diálogos sem barra de título (ex.: barra de progresso `runWithProgress`) não são arrastáveis;
  ficam centrados na mesma.

## Notas técnicas

- O listener filtra por `source instanceof Dialog && != mainWindow` — barato e abrangente.
- `clampInsideMain` usa `mainArea()` em coordenadas de ecrã; se o modal for maior que a janela
  (não deve, é limitado a 94%) encosta ao canto superior-esquerdo.
</content>
