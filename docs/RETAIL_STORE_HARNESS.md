# Harness - Validação de Loja e Mercearia

Este harness guia testes manuais e automatizados antes de declarar o sistema apto para loja/mercearia. Cada cenario deve ser executado com uma empresa activa, utilizador identificado e dados controlados.

## Dados base

Criar ou confirmar:

- Empresa `Loja Demo`.
- Utilizadores: `admin`, `gerente`, `caixa`, `stock`.
- Armazem `Loja`.
- Conta de tesouraria `Caixa Geral`.
- Cliente `Consumidor Final` e cliente fiado `Cliente Fiado`.
- Fornecedor `Fornecedor Demo`.
- Produto normal: `Arroz 1kg`, stockavel, codigo de barras, stock minimo.
- Produto perecivel: `Iogurte`, stockavel, lote e validade.
- Produto por peso: `Tomate kg`, quantidade decimal.
- Produto nao-stockavel: `Servico Entrega`.

## Matriz de cenarios

| ID | Area | Cenario | Resultado esperado | Evidencia |
|----|------|---------|--------------------|-----------|
| RS-01 | Login | Caixa autentica e selecciona empresa | Token activo, empresa no contexto, UI abre | Screenshot/log |
| RS-02 | Permissao | Caixa tenta anular venda | Operacao bloqueada ou exige aprovacao | Mensagem PT |
| RS-03 | POS | Abrir sessao com saldo inicial | Sessao `OPEN`, operador correcto | ID sessao |
| RS-04 | POS | Ler produto por codigo de barras | Produto entra no carrinho sem duplicar erro | Carrinho |
| RS-05 | POS | Venda numerario com troco | Factura paga, troco calculado, movimento de caixa | Factura/caixa |
| RS-06 | POS | Venda cartao | Factura paga, tesouraria recebe cartao, caixa fisico nao duplica | Transaccao |
| RS-07 | POS | Pagamento misto numerario/cartao | Soma valida, movimentos separados | Pagamentos |
| RS-08 | POS | Fiado parcial ou total | Factura fica `PARTIALLY_PAID` ou em aberto | Estado factura |
| RS-09 | POS | Venda sem sessao aberta | Bloqueio com mensagem clara | Mensagem PT |
| RS-10 | Stock | Venda acima do stock | Bloqueio, nenhum documento parcial | Stock igual |
| RS-11 | FEFO | Dois lotes com validades diferentes | Venda consome validade mais proxima | Movimento/lote |
| RS-12 | Validade | Entrada com lote vencido | Bloqueio salvo decisao explicita | Mensagem PT |
| RS-13 | Devolucao | Devolver venda com reposicao de stock | Nota de credito, stock volta, caixa/tesouraria rastreia | NC/stock |
| RS-14 | Troca | Trocar produto por outro | Documento origem preservado, diferenca tratada | NC/nova venda |
| RS-15 | Caixa | Sangria acima do saldo | Bloqueio, caixa nao muda | Mensagem PT |
| RS-16 | Caixa | Fecho com diferenca | Diferenca registada e auditada | Sessao/auditoria |
| RS-17 | Compras | Receber compra com lote e validade | Stock entra no armazem e lote aparece | Stock/lote |
| RS-18 | Stock | Contagem fisica com ajuste | Ajuste exige motivo e audita diferenca | Movimento |
| RS-19 | Relatorio | Relatorio diario de caixa | Totais batem com vendas e pagamentos | PDF/tela |
| RS-20 | Backup | Backup e restore em ambiente teste | Dados restaurados e conferidos | Caminho/log |
| RS-21 | Impressao | Imprimir factura/recibo | Falha de impressao nao corrompe venda | PDF/print |
| RS-22 | Tenant | Utilizador de outra empresa tenta aceder dados | Acesso negado | 403/mensagem |

## Testes automatizados esperados

Adicionar ou manter testes para:

- `POSService.checkout`: venda sem sessao, stock insuficiente, multi-pagamento, fiado, FEFO.
- `POSService.closeSession`: saldo esperado, diferenca, deposito de fecho.
- `CreditNoteService`: devolucao com e sem reposicao de stock, limite contra documento origem.
- `InventoryService`/`ProductBatchService`: ajuste, lote vencido, FEFO multi-lote.
- `StockTransferService`: estados, duplicacao de movimentos, permissao.
- `DocumentNumberService`: sequencia por serie e empresa quando aplicavel.
- `SecurityInterceptor`/`TenantAccessService`: token, empresa e role.
- `BackupService`: exporta dados obrigatorios e fluxo de restore quando implementado.

## Endpoints implementados para o harness

- `POST /api/pos/returns` cobre RS-13 e a parte de devolucao de RS-14.
- `POST /api/inventory/adjustments` cobre RS-18.
- `GET /api/reports/daily-store` cobre RS-19 com vendas, fiado, pagamentos, movimentos de caixa, top produtos, vendas por operador e margem bruta.
- `BackupService.verifyBackup(...)` e o botão "Verificar Backup" cobrem a verificação não destrutiva de RS-20. O restore real continua a exigir ambiente separado.

## Checklist de pre-producao

- [x] `mvn -q clean compile` passa.
- [x] `mvn -q test` passa.
- [ ] Migration PostgreSQL aplicada em base limpa.
- [ ] Login, tenant e roles testados por API.
- [ ] POS testado com leitor de codigo de barras real.
- [ ] Impressora real testada com factura/recibo.
- [ ] Fecho de caixa validado por operador e gerente.
- [ ] Backup restaurado num ambiente separado.
- [ ] Relatorios diarios batem com documentos e tesouraria.
- [ ] `tasks/current.md` actualizado com estado e bloqueios reais.

## Regra de falha

Se qualquer cenario que mexe em dinheiro, stock, fiscal, permissao ou auditoria falhar, a fase nao esta pronta para loja real. Corrigir no Service dono da regra e repetir o cenario.
