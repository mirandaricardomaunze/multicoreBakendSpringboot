# Spec — Gaveta de dinheiro do POS

**Data:** 2026-11-10 · **Estado:** feito, por validar com a gaveta da loja piloto
**Relacionado:** `docs/POS_PRE_PILOTO_SPEC.md`, `docs/ACEITACAO_LOJA_PILOTO_SECTORES.md`

## Decisões

1. A gaveta liga-se à impressora de talões (cabo RJ11) e abre com o impulso ESC/POS `ESC p m 25 250`
   (`1B 70 m 19 FA`), com `m` = 0 (pino 2, o mais comum) ou 1 (pino 5).
2. **Abre sozinha no fim de uma venda paga em numerário.** Cartão, transferência e crédito (fiado) não a abrem.
3. A abertura corre em segundo plano: uma gaveta que não abre nunca atrasa nem desfaz a venda.
4. Configuração **por posto** (ficheiro local, como a balança): activa, impressora e pino
   (`desktop.drawer.enabled`, `desktop.drawer.printer`, `desktop.drawer.pin`).

## Interface

Configurações › **Gaveta deste Posto**: activar, escolher a impressora, o pino, Gravar e **Testar Abertura** (envia o
impulso agora).

## Testes

`CashDrawerTest` (impulso por pino; só vendas a numerário com gaveta configurada abrem) e
`SectorFoundationUiHarnessTest` SA-UI-03 (separador no fim das Configurações).

## Limites

- Não há botão para abrir a gaveta sem venda (troco, sangria): de propósito, para não haver aberturas sem registo.
- Gavetas USB ou com driver próprio (sem impressora) não são suportadas.
