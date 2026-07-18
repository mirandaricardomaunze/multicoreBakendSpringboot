# Spec — Polish profissional do ecrã POS

> Melhoria de UX/visual do [POSPanel](../src/main/java/com/phcpro/gui/POSPanel.java), alinhada com
> [UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md) e [CONVENTIONS.md §11/§12](../CONVENTIONS.md). Sem mexer em
> regras de negócio nem na camada de serviço — só apresentação.

**Última actualização:** 2026-06-27

## Problema

O ecrã de Ponto de Venda tinha defeitos que prejudicavam a percepção profissional:

1. **Secção DOCUMENTO cortada** — o formulário esquerdo abria com scroll a meio, escondendo Cliente,
   Armazém e Conta de Tesouraria. O operador não via para quem nem de onde estava a vender.
2. **Emojis 🔍 em placeholders** — viola o design system (ícones devem ser vectoriais). Pior: como o
   L&F é Metal/Ocean (não FlatLaf), `JTextField.placeholderText` **não renderiza** — os campos de
   pesquisa apareciam como caixas vazias sem qualquer pista de afinidade.
3. **Total subvalorizado** — a informação nº1 de um POS estava a 18px encostada a um canto.
4. **Carrinho vazio sem estado** — área grande em branco, sem orientação ao operador.
5. **Estado da caixa pouco legível** — texto simples, sem ícone de estado.

## Decisões

- **Só apresentação.** Nenhuma alteração a `POSService`, DTOs ou cálculos. O total continua a ser
  somado pelos `CartItem.getSubtotal()` já existentes; o checkout não muda.
- **Pista de pesquisa = ícone vectorial** (`fas-search`, cor `TEXT_MUTED`) à esquerda do campo, no
  mesmo padrão da barra de código de barras (`fas-barcode`). Funciona sob Metal, é colourável e
  consistente. Os emojis são removidos.
- **Total em destaque** num `ModernPanel` (faixa de acento) com legenda "TOTAL A PAGAR" e valor a
  26px — formatação `%,.2f MT` (locale PT, [CONVENTIONS.md §12](../CONVENTIONS.md)).
- **Empty state do carrinho** via `CardLayout` (cartão "vazio" vs tabela), alternado de forma
  centralizada sempre que o carrinho muda (`updateCartTotal`).
- **DOCUMENTO sempre visível** — o scroll do formulário é reposto ao topo em `onPanelSelected()`.
- **Estado da caixa com ícone** — cadeado aberto/verde (aberta) vs fechado/amarelo (fechada).

## Não-objetivos

- Não reorganizar o layout de raiz (sem grelha de produtos por toque nem pagamento embutido — isso
  seria um redesign, fora deste âmbito).
- Não introduzir FlatLaf nem nova biblioteca.
- Não tocar no separador Histórico de Vendas além do necessário.
</content>
