# Design do ecrã POS — Especificação e harness

**Última actualização:** 2026-10-31

## Objectivo

Reduzir o que compete pela atenção do caixa, sobretudo em 1366×720, sem alterar regras de venda,
pagamentos, stock ou documentos fiscais. Substitui, no que colidir, as decisões de layout de
`POS_CABECALHO_COMPACTO_SPEC.md` e `POS_PROFISSIONAL_ERGONOMIA_SPEC.md`.

## Decisões

### Barra superior

- "Venda POS" e "Histórico de Vendas" funcionam como separadores: só o activo tem cor forte.
- "Actualizar" passa a botão só com ícone.
- "Fechar Caixa" fica no extremo direito, separado das outras acções e sem fundo vermelho.
- O distintivo de "Fechar Caixa" passa a `F12`, a tecla real. Mostrava `Z`, que não faz nada.

### Balança

- Oculta por omissão. Liga-se por posto em `%LOCALAPPDATA%\Multicore\desktop.properties`, com
  `desktop.pos.scale-enabled=true`.
- **Não existe driver de porta série:** `SerialScaleReader` trabalha sempre em simulação. O ponto
  deixa de ficar verde nesse modo, porque sugeria uma balança ligada.
- "Simular" só aparece a `ADMIN` e `MANAGER`.
- Ler artigos pesados por código de barras não muda.

### Cabeçalho da venda

- Ordem: código de barras, cliente, armazém, conta. O código de barras é o primeiro e o mais largo,
  e recebe o foco ao entrar no POS e depois de cada venda.
- A pesquisa e o selector de cliente ficam juntos, sob um único rótulo "Cliente".
- O asterisco sai: "Consumidor Final" já cumpre a regra. O nome só é pedido quando não há cliente
  seleccionado, e essa validação não muda.
- Sem resultados, o selector indica "Novo cliente: usar o nome escrito".
- A conta deixa de mostrar o saldo ao caixa.
- Selectores cortados mostram o texto completo na dica. Um NUIT só com zeros não é mostrado.

### Carrinho

- Sem artigos, "Remover", "−", "Quantidade", "+" e "Finalizar Venda" ficam desactivados.
- "Finalizar Venda" exige também a caixa aberta. O atalho `F10` continua a explicar o que falta.

### Catálogo

- O número de colunas acompanha a largura e nunca há deslocação horizontal.
- A paginação só aparece com mais de uma página.
- Stock inteiro sem decimais (`118 disp.`) e com letra maior.

### Moldura da aplicação

- Ao entrar no POS com a janela abaixo de 1600 px, a barra lateral recolhe. Ao sair, volta como
  estava, se tiver sido o POS a recolhê-la.
- Um favorito deixa de aparecer também na sua secção, por isso nunca há dois itens marcados.
- O utilizador sai da barra de estado e a versão sai da barra lateral.
- A faixa da caixa mostra o nome da sessão ("Ana Costa"), não o login.
- **Mantêm-se os dois botões de sair:** são exigidos por `.agents/rules/swing_ui_rules.md`, §2.

### Dados de demonstração

- Nomes dos produtos com acentos correctos. Só se aplica a bases novas: os dados existentes não são
  migrados.
- A empresa de Portugal mantém-se, porque a demonstração cobre duas empresas: também existe
  "Multicore Moçambique Lda".

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| PD-01 | Ordem do cabeçalho | Código de barras primeiro e com a maior largura |
| PD-02 | Rótulos do cabeçalho | Um só "Cliente", sem asterisco |
| PD-03 | Cliente sem resultados | Indicação de novo cliente, sem cliente seleccionado |
| PD-04 | NUIT só com zeros e conta | Sem zeros e sem saldo no texto |
| PD-05 | Carrinho vazio e cheio | Acções desactivadas e depois activas |
| PD-06 | Caixa fechada com artigos | "Finalizar Venda" desactivado |
| PD-07 | Largura do catálogo | Colunas crescem, com um mínimo de 2 |
| PD-08 | 1296×620 | Pelo menos 6 cartões inteiros, sem deslocação horizontal |
| PD-09 | Uma e várias páginas | Paginação oculta e depois visível |
| PD-10 | Stock inteiro e decimal | `118 disp.` e `1,5 disp.` |
| PD-11 | Balança | Oculta sem configuração; "Simular" só para gestão |
| PD-12 | Simulação | Sem ponto verde nem "Estável" |
| PD-13 | Fechar Caixa | Distintivo `F12`, sem vermelho de fundo |
| PD-14 | Barra lateral | Recolhe abaixo de 1600 px; favorito não duplica |
| PD-15 | Nome do operador | Nome da sessão para o próprio; login para outro operador |

Teste: `PosDesignHarnessTest`. Os testes do cabeçalho em `PosLayoutTest` e
`PosProfessionalErgonomicsHarnessTest` passam a exigir 4 colunas.

## Limites

- Nomes muito longos de armazém ou conta continuam cortados, com o texto completo na dica.
- O teste de layout usa as fontes da máquina que o corre.
- O nome de um cliente avulso depois da venda é tratado em `POS_PRE_PILOTO_SPEC.md`.
- A imagem do produto vem do cadastro: uma imagem errada corrige-se em Stock.
