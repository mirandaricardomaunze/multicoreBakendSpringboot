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

Estado por Service (verde em `mvn test`, 71 testes):

- [x] `POSService.checkout`/`closeSession`: sem sessao, via legada vs multi-metodo, fiado parcial,
  numerario+cartao sem dupla contagem, fecho sem diferenca, diferenca exige permissao, deposito de
  fecho na tesouraria — `POSServiceTest` (10).
- [x] `CreditNoteService`: devolucao RETURN repoe stock so na aprovacao, motivo nao-RETURN nao mexe
  stock, limite de quantidade vs ja devolvido, valor vs fatura, permissao MANAGER/ADMIN — `CreditNoteServiceTest` (8).
- [x] `ProductBatchService`: FEFO single/multi-lote, stock insuficiente, ajuste de lote, **bloqueio de
  entrada de lote vencido (RS-12)** — `ProductBatchServiceTest` (12).
- [x] `ComercialService`: faturação directa (APPROVED + baixa stock), desconto >10% exige aprovação,
  faturação de encomenda, anulação com reposição, permissão MANAGER/ADMIN — `ComercialServiceTest` (8).
- [x] `StockTransferService`: estados, stock so na aprovacao, permissao — `StockTransferServiceTest` (9).
- [x] `DocumentNumberService`: sequencia gapless por serie/ano, series independentes, corrida na
  criacao, serie ND — `DocumentNumberServiceTest` (6).
- [x] `TenantAccessService`/isolamento por empresa — `TenantAccessServiceTest`, `TenantIsolationIntegrationTest`.
- [x] login/token via API — `AuthControllerIntegrationTest`.
- [x] **segurança ponta-a-ponta por API (spec §9):** 401 sem token, 403 empresa sem acesso, role
  gate ao faturar (EMPLOYEE bloqueado / ADMIN passa) — `SecurityApiIntegrationTest` (4). O enforcement
  vive no `SecurityInterceptor` (token+empresa em todo o `/api/**` excepto login); o filtro Spring
  permissivo não é uma falha — a guarda é o interceptor.
- [x] `BackupService`: export e verificacao nao destrutiva — `BackupServiceTest`. Restore real continua a exigir ambiente separado.

Regra de **lote vencido (RS-12)** implementada em `ProductBatchService.addToBatch`: entrada de stock
com validade já no passado é bloqueada com `BusinessRuleException` (validade hoje ainda entra; produto
sem validade não é afectado). Guarda todas as entradas porque a compra/ENTRY passa por `addToBatch`.

## Endpoints implementados para o harness

- `POST /api/pos/returns` cobre RS-13 e a parte de devolucao de RS-14.
- `POST /api/inventory/adjustments` cobre RS-18.
- `GET /api/reports/daily-store` cobre RS-19 com vendas, fiado, pagamentos, movimentos de caixa, top produtos, vendas por operador e margem bruta.
- `BackupService.verifyBackup(...)` e o botão "Verificar Backup" cobrem a verificação não destrutiva de RS-20. O restore real continua a exigir ambiente separado.

## Checklist de pre-producao

- [x] `mvn -q clean compile` passa.
- [x] `mvn -q test` passa (86 testes).
- [x] Login, tenant e roles testados por API — `SecurityApiIntegrationTest`.
- [ ] Migration PostgreSQL aplicada em base limpa. *(operacional — requer instância Postgres)*
- [ ] POS testado com leitor de codigo de barras real. *(hardware)*
- [ ] Impressora real testada com factura/recibo. *(hardware)*
- [ ] Fecho de caixa validado por operador e gerente. *(manual)*
- [ ] Backup restaurado num ambiente separado. *(manual)*
- [ ] Relatorios diarios batem com documentos e tesouraria. *(manual)*
- [x] `tasks/current.md` actualizado com estado e bloqueios reais.

## Regra de falha

Se qualquer cenario que mexe em dinheiro, stock, fiscal, permissao ou auditoria falhar, a fase nao esta pronta para loja real. Corrigir no Service dono da regra e repetir o cenario.
