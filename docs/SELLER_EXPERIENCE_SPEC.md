# Experiência operacional do Vendedor

## Diagnóstico

O painel geral foi desenhado para gestão e agrega tesouraria, fiscalidade, aprovações, compras e
stock. Apresentá-lo ao perfil `SELLER` cria ruído, pedidos HTTP recusados e risco de exposição de
informação que não é necessária para vender.

## Decisão

O Vendedor entra num painel próprio, orientado à execução e separado do dashboard de gestão. A UI
continua sem autoridade de segurança: `ApiRolePolicy` e as guardas dos Services permanecem a fonte
de verdade.

## Conteúdo do painel

- Saudação nominal e estado visível da sessão de caixa.
- KPIs numa linha única, usando `KpiCard.STANDARD_CARD_HEIGHT`:
  - valor vendido na sessão própria;
  - atendimentos concluídos na sessão própria;
  - encomendas pendentes da operação comercial;
  - cotações abertas para seguimento.
- Acções rápidas: nova venda POS, nova encomenda, nova cotação, clientes, produtos e notificações.
- Nenhum saldo de tesouraria, resultado fiscal, salário, aprovação, compra ou configuração.

## Navegação e mensagens

1. A barra lateral apresenta apenas Painel, POS, Pedidos, Clientes, CRM e Notificações.
2. A pesquisa global do Vendedor contém exclusivamente destinos permitidos e acções comerciais.
3. Tentativas programáticas de abrir uma área proibida mostram uma orientação clara para solicitar
   apoio ao gestor, em vez de falhar silenciosamente.
4. Os botões têm altura e alinhamento uniformes e usam apenas ícones FontAwesome.

## Privacidade operacional

O valor e o número de vendas são obtidos pelo relatório da sessão de caixa do utilizador autenticado.
Não são apresentados totais financeiros globais nem desempenho de outros vendedores.

## Critérios do harness

- Existe um painel dedicado e a `MainFrame` selecciona-o para `SELLER`.
- O painel usa `KpiCard.createGrid(4, ...)` e não cria KPIs privados com `ModernPanel`.
- As seis acções rápidas estão presentes em português de Moçambique.
- A pesquisa do Vendedor não contém finanças, RH, compras, stock administrativo, fiscal ou config.
- A navegação proibida produz feedback e a autorização continua no backend.
