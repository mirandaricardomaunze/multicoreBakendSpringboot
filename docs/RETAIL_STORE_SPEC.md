# Spec - Prontidao para Loja e Mercearia

Este documento define o escopo minimo para o Multicore operar com seguranca numa loja, mercearia ou minimercado. Complementa `BUSINESS_FLOWS.md`, `SECURITY_AND_AUDIT.md`, `DATABASE.md` e `TESTING_STRATEGY.md`.

## Objectivo

Tornar o sistema apto para venda diaria em balcao, com controlo de caixa, stock, validade, devolucoes, compras e relatorios operacionais sem quebrar as regras fiscais, de stock e de tenant.

## Perfis alvo

- Caixa: vende, recebe pagamentos, consulta produtos e fecha apenas a sua sessao quando permitido.
- Gerente: aprova descontos sensiveis, sangrias, devolucoes, ajustes de stock e fechos com diferenca.
- Administrador: configura empresa, utilizadores, impostos, series, permissoes e integracoes.
- Stock/compras: recebe mercadoria, corrige lotes, executa contagens e gere fornecedores.

## Capacidades obrigatorias

### 1. POS de balcao

- Abrir sessao de caixa por operador e empresa.
- Ler produto por codigo de barras, SKU ou pesquisa por nome.
- Vender por unidade e por quantidade decimal.
- Suportar pagamentos em numerario, cartao, transferencia e fiado.
- Calcular troco para numerario.
- Bloquear venda sem sessao aberta.
- Bloquear venda com stock insuficiente quando o produto controla stock.
- Consumir lote por FEFO quando houver validade.
- Gerar documento comercial numerado e imprimivel.
- Registar movimento de caixa e origem financeira.

### 2. Devolucao, troca e anulacao

- Devolucao deve partir de documento original.
- Motivo e obrigatorio.
- Nota de credito e o mecanismo normal para devolver valor.
- Reposicao de stock so acontece quando a mercadoria volta fisicamente em condicao vendavel.
- Devolucao de numerario/cartao/credito deve ficar rastreavel.
- Operacoes sensiveis exigem permissao ou aprovacao.

### 3. Produtos de mercearia

- Produto tem SKU, nome, preco de venda, custo, categoria e codigo de barras quando aplicavel.
- Produto pode ser vendido por unidade, caixa ou peso.
- Produtos por peso devem aceitar quantidade decimal e, quando houver balanca, codigo PLU ou codigo de barras de peso/preco variavel.
- Produto pode ter stock minimo e alerta de reposicao.
- Produto perecivel deve suportar lote e validade.
- Produto nao-stockavel deve poder ser vendido sem criar movimentos fisicos.

### 4. Stock e validades

- Entradas de compra criam stock no armazem correcto.
- Entradas com lote vencido sao bloqueadas salvo regra explicita.
- FEFO e aplicado no backend em transaccao.
- Ajustes de stock exigem motivo e auditoria.
- Contagem fisica deve comparar stock contado contra stock esperado.
- Produtos proximos da validade devem aparecer em alerta.

### 5. Compras e fornecedores

- Fornecedor tem nome, NUIT quando aplicavel, contactos e empresa.
- Compra pode receber linhas com lote, validade, custo e armazem.
- Compra deve actualizar stock e, quando for pagamento imediato, tesouraria.
- Deve existir caminho para compra a credito/conta a pagar antes de producao plena.
- Recepcao parcial e devolucao ao fornecedor sao desejaveis para fase seguinte.

### 6. Caixa e tesouraria

- Fecho compara valor esperado contra valor contado.
- Diferenca de caixa fica registada e auditavel.
- Sangria e suprimento exigem motivo.
- Sangria nao pode deixar saldo fisico negativo.
- Deposito de fecho move numerario para conta de tesouraria quando aplicavel.
- Relatorio diario deve separar numerario, cartao, transferencia e fiado.

### 7. Relatorios minimos

- Vendas por dia, operador, forma de pagamento e categoria.
- Top produtos vendidos.
- Produtos com stock baixo.
- Produtos proximos da validade.
- Sessoes de caixa abertas e fechadas.
- Fiado em aberto por cliente.
- Margem bruta por produto quando custo estiver disponivel.

### 8. Hardware operacional

- Leitor de codigo de barras deve funcionar como entrada de teclado no POS.
- Impressao de recibo/factura deve ser testada com impressora real.
- Gaveta de dinheiro deve ter estrategia documentada, mesmo que a primeira fase seja manual.
- Balanca deve ser especificada antes de implementar integracao directa.
- Etiquetas de preco/codigo de barras sao desejaveis para reposicao e prateleira.

### 9. Seguranca, tenant e auditoria

- Todos os endpoints `/api/**`, excepto login, exigem token valido e empresa seleccionada.
- Empresa enviada pelo cliente nunca e aceite sem validar acesso.
- Operacoes sensiveis validam role/permissao no Service.
- Auditoria cobre login, logout, devolucao, anulacao, desconto sensivel, sangria, ajuste de stock, transferencia, fecho com diferenca e alteracao de utilizador.
- DTOs nunca expoem password, hash, token ou entidades JPA.

### 10. Operacao e resiliencia

- PostgreSQL em producao usa Flyway com migrations versionadas.
- Backup precisa de restore testado, nao apenas exportacao.
- Falhas de impressao nao podem desfazer venda ja confirmada.
- O sistema deve orientar o operador com mensagens claras em portugues de Mocambique.
- Operacoes longas no desktop usam `SwingWorker` ou cliente HTTP sem bloquear a UI.

## Fora do escopo inicial

- Marketplace/e-commerce.
- Multimoeda.
- Integração fiscal certificada externa, salvo decisao explicita.
- Fidelizacao complexa por pontos.
- Previsao automatica de compras.
- Modo offline completo. Pode ser fase futura, se a loja depender de internet instavel.

## Prioridade de entrega

1. Segurança e permissões efectivas.
2. Devoluções/trocas no POS.
3. Produtos por peso e regras de unidade.
4. Contagem/ajuste de stock com motivo e auditoria.
5. Relatorios diarios de caixa, fiado, validade e reposicao.
6. Backup/restore operacional.
7. Hardware real: impressora, leitor, gaveta e balanca.

## Criterios de aceite

- `mvn clean compile` passa.
- Testes automatizados cobrem regras criticas novas.
- Harness em `RETAIL_STORE_HARNESS.md` tem os cenarios principais validados.
- Nenhuma operacao sensivel atravessa tenant ou permissao.
- POS, stock e tesouraria permanecem atomicos nos Services.
- `tasks/current.md` aponta para o estado real da fase em curso.
