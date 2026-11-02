# POS antes do piloto — Especificação e harness

**Última actualização:** 2026-11-01

## Objectivo

Fechar o que o POS podia errar em dinheiro ou em dados de cliente antes de uma loja real, e permitir
atender outro cliente a meio de uma venda.

## Decisões

### Pagamento móvel

- Não existe ligação a nenhum operador. O push USSD era só simulação e confirmava o pagamento ao fim
  de 2 segundos, com uma referência inventada.
- A simulação fica desligada por omissão. `POS_MOBILE_PAYMENT_SIMULATION=true` liga-a em
  demonstrações. No perfil `prod` está fixa em `false`, sem variável de ambiente.
- Desligada, `initiate` e `simulate-complete` são recusados e nada pendente passa a confirmado.
- O POS consulta `GET /api/pos/mobile-payment/availability`. Só mostra o botão, agora "Simular
  M-Pesa" ou "Simular e-Mola", se o servidor o permitir. Sem resposta, fica oculto.
- M-Pesa e e-Mola continuam a poder ser recebidos: o caixa confere o comprovativo do cliente e indica
  a referência, que passa a ser obrigatória.
- **A referência não é verificada junto do operador:** a prova é a conferência do caixa.

### Cliente entre vendas

- Depois de uma venda, em contingência ou não, e ao limpar o carrinho, o nome escrito e "Venda a
  Crédito" são limpos.
- Com a pesquisa vazia, o selector volta a "Consumidor Final", se existir.

### Suspender e retomar

- `Ctrl+S` suspende e `Ctrl+R` retoma. Suspender é um botão de ícone nas acções do carrinho. Retomar
  é o botão "Em espera (n)", que só aparece quando há vendas suspensas. Ficam fora do menu Operações,
  que já tem as cinco opções permitidas.
- Ficam em `%LOCALAPPDATA%\MulticoreERP\data\pos_suspended_sales.json`, por posto e por empresa, e
  sobrevivem a um reinício. Qualquer operador do posto as vê, o que cobre a passagem de turno.
- Não reservam stock nem têm efeito fiscal: a venda só existe quando é finalizada.
- Guardam quantidade, desconto, lote, série, preço cotado, cliente, armazém e cotação.
- Ao retomar, preço de catálogo e IVA são os actuais. "Venda a Crédito" não é guardada.
- Só se retoma com o carrinho vazio. A venda sai da espera depois de voltar ao carrinho.
- Artigos que já não existem ficam de fora e o caixa é avisado. Se não sobrar nenhum, a venda fica em
  espera.

### Desconto de linha (F5)

- O campo é uma percentagem em todo o sistema. F5 gravava o valor em meticais nesse campo: 10% em
  500 MT tornava-se 50, isto é, 50%. Passa a gravar a percentagem.

### Autorização de desconto

- Um caixa precisa do PIN de um gerente para dar mais de 10% com F5. Gerente e administrador não.
- O PIN de cada gerente define-se na gestão de utilizadores. Sem PIN definido, o desconto é recusado.
- Saiu o PIN alternativo `1234`, que valia quando não havia ligação ao servidor.
- A validação é feita no desktop: o servidor não sabe quem autorizou, e as promoções não pedem PIN.

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| PP-01 | Iniciar pagamento sem simulação | Recusado, sem gravar |
| PP-02 | Transacção pendente sem simulação | Continua pendente, sem referência |
| PP-03 | Conclusão forçada sem simulação | Recusada |
| PP-04 | Configuração | Desligada por omissão e fixa em produção |
| PP-05 | M-Pesa ou e-Mola sem referência | Recusado; outros métodos não mudam |
| PP-06 | Depois da venda | Nome e crédito limpos, "Consumidor Final" seleccionado |
| PP-07 | Suspender | Carrinho e cliente limpos, venda guardada, botão com a contagem |
| PP-08 | Retomar | Quantidade e cliente repostos, preço actual, venda sai da espera |
| PP-09 | Sem artigos válidos ou carrinho ocupado | Venda continua em espera, carrinho intacto |
| PP-10 | Reinício e duas empresas | Vendas preservadas e separadas |
| PP-11 | Ficheiro ilegível | Posto de lado como `.corrupt` |
| PP-12 | Falha ao gravar | Venda continua no carrinho |
| PP-13 | 10% em 500 MT | Total de 450 MT |
| PP-14 | Desconto acima de 10% | PIN para o caixa, não para gerente ou administrador |

Testes: `MobilePaymentSimulationOffHarnessTest` (PP-01 a PP-04) e `PosPrePilotoHarnessTest`.

## Limites

- Integrar M-Pesa e e-Mola a sério exige contrato e credenciais de cada operador.
- As vendas em espera não passam para outro posto. Avariando o computador, perdem-se: como nada foi
  vendido, não há efeito fiscal nem de stock.
- Vendas já feitas com desconto por F5 ficaram com o desconto errado e não são corrigidas aqui.
