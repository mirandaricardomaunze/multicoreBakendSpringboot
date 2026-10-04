# Harness — Posicionamento de Acções em Tabelas

1. Abrir uma lista em cada domínio: Comercial, Compras, Stock, Contabilidade, Configuração,
   Recursos Humanos, Fiscal, Financeiro e Plataforma.
2. Confirmar que pesquisa e filtros aparecem no topo do card da tabela.
3. Confirmar que as acções da lista aparecem à direita da mesma barra ou no cabeçalho da página
   quando forem estritamente globais.
4. Confirmar que o rodapé contém apenas paginação, contagem, totais ou estado informativo.
5. Confirmar que barras com mais de três acções usam menus e que cada menu tem no máximo cinco
   opções.
6. Confirmar altura uniforme de 38 px entre pesquisa, selects, botões e menus.
7. Confirmar que diálogos mantêm `Cancelar` e `Guardar/Confirmar` no rodapé.
8. Em Facturação, confirmar uma única faixa superior dentro do card: `Actualizar`, `Mais acções` e
   `Emitir`; pesquisa, estado e período ficam na fila imediatamente abaixo.
9. Confirmar que a paginação de Facturação segue a mesma ordem das Notas: tamanho/contagem à
   esquerda, página ao centro e navegação à direita.
10. Repetir a verificação em Clientes, Pedidos, Compras, Fornecedores, Contas a Pagar, Reposição,
    Cotações, Guias, Notas, Recibos, Contas Correntes, Promoções, Notificações, CRM, Stock, Fiscal,
    RH, Plataforma, Inventário Físico e Gestão de Utilizadores.

## Automação

`TableActionPlacementHarnessTest` verifica a composição do toolbar canónico e impede a
reintrodução dos rodapés de acção conhecidos nas listas migradas.

```powershell
mvn -q "-Dtest=TableActionPlacementHarnessTest,DesktopThinContextTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```
