# Spec — Operação rápida e acabamento profissional do POS

**Última actualização:** 2026-08-05

## Objectivo

Reduzir cliques e ruído visual no `POSPanel`, preservando integralmente as regras de negócio,
DTOs, cálculo de IVA, promoções, stock, caixa e checkout no backend.

## Decisões

- Atalhos na vista POS: **F2** pesquisa produto, **F4** pesquisa cliente, **F6** altera quantidade e
  **F9** finaliza a venda. **Delete** remove uma linha apenas quando a tabela tem foco.
- Duplo clique numa linha abre o mesmo editor de quantidade. Quantidades decimais continuam aceites
  para artigos vendidos ao peso; zero e valores negativos são rejeitados.
- Cliente e código de barras permanecem no cabeçalho principal. Armazém e conta, normalmente
  predefinidos, ficam em **Mais opções** e continuam disponíveis antes do checkout.
- O pagamento em numerário oferece recebimento rápido por valor exacto ou notas de 100/200/500/1000
  MT. O cálculo e validação do troco continuam no fluxo existente.
- Todos os controlos adjacentes usam altura visual de 38px e componentes do design system.

## Não-objectivos

- Não alterar permissões de desconto, pagamento misto, regras de caixa ou contratos HTTP.
- Não adicionar dependências visuais nem substituir o Look & Feel.
- Não automatizar validação de hardware físico.
