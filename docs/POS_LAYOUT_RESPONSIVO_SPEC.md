# Spec — Layout responsivo do POS

**Última actualização:** 2026-08-11

## Objectivo

Garantir que o catálogo, o carrinho e as acções de checkout permanecem operacionais ao redimensionar
a janela do POS, sem aumentar os inputs nem comprimir colunas até ficarem ilegíveis.

## Problema

- O catálogo arrancava com largura fixa de 400 px, independentemente da largura disponível.
- As oito colunas do carrinho eram comprimidas quando faltava espaço horizontal.
- O scroll vertical envolvia todo o cartão do carrinho; em janelas baixas, totais e checkout podiam
  sair da área visível.
- O carrinho aceitava apenas 400 px de largura mínima, insuficiente para operação confortável.

## Decisões

- O divisor inicia em **36% para o catálogo e 64% para o carrinho** e distribui o resize na mesma
  proporção. O operador continua a poder arrastar o divisor.
- O catálogo tem mínimo de **380 px** e o carrinho mínimo de **650 px**.
- A partir de **900 px** de viewport, as colunas adaptam-se à largura disponível. Abaixo disso,
  preservam as larguras definidas e a tabela oferece scroll horizontal.
- Apenas a tabela faz scroll. Subtotal, IVA, total, opção de fiado e botões permanecem fixos no fundo.
- Inputs e combos mantêm a altura canónica de **38 px**; regras de checkout e contratos não mudam.

## Não-objectivos

- Alterar colunas, cálculos, pagamentos, permissões ou contratos HTTP.
- Ocultar informação fiscal do carrinho.
- Substituir Swing ou introduzir uma nova biblioteca visual.

## Critérios de aceitação

- Em 1366×768 ou superior, catálogo e carrinho ficam simultaneamente utilizáveis.
- Ao estreitar a janela, os textos das colunas não são esmagados; surge scroll horizontal.
- Ao reduzir a altura, o rodapé de checkout continua visível e a tabela reduz o viewport vertical.
- O divisor continua arrastável sem deixar qualquer lado abaixo do seu mínimo enquanto houver espaço.

