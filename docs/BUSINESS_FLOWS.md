# Fluxos de Negocio - Multicore ERP

Este documento descreve os fluxos que a IA deve preservar. Antes de alterar POS, vendas, compras, stock, fiscal, tesouraria ou RH, confirmar o impacto aqui.

## Venda POS

1. Operador abre ou usa uma sessao de caixa activa.
2. Produtos sao adicionados ao carrinho.
3. Sistema valida preco, quantidade e stock disponivel.
4. Checkout cria documento comercial adequado.
5. Stock e consumido pelo criterio definido no dominio de inventario.
6. Movimento de caixa/tesouraria e registado.
7. Documento pode ser impresso.
8. Evento sensivel deve ser auditavel.

Regras:

- Nunca vender quantidade negativa ou zero.
- Nunca deixar checkout parcial sem erro claro.
- Se a venda mexe em stock e dinheiro, a transaccao deve ser atomica no Service.

## Factura comercial

1. Cliente e seleccionado ou criado.
2. Linhas sao validadas.
3. Impostos e totais sao calculados.
4. Numero oficial e atribuido.
5. Factura e persistida.
6. Stock e tesouraria sao actualizados quando aplicavel.
7. PDF pode ser gerado por `printing`.

Regras:

- Factura emitida nao deve ser editada livremente.
- Correcao deve usar nota de credito/debito ou anulacao controlada.
- DTO de saida nao deve expor entidades JPA.

## Nota de credito/debito

1. Documento origem e localizado.
2. Sistema valida se a nota e permitida.
3. Linhas e motivo sao registados.
4. Impacto fiscal, financeiro e stock e calculado.
5. Documento recebe numeracao propria.

Regras:

- Motivo deve ser obrigatorio.
- Nao permitir valor superior ao documento origem sem regra explicita.
- Impactos devem ser auditaveis.

## Compra e recepcao de stock

1. Compra ou recepcao e criada.
2. Linhas sao validadas.
3. Lotes e validades sao registados quando aplicavel.
4. Stock entra no armazem correcto.
5. Movimento de stock e persistido.
6. Documento pode gerar obrigacao financeira quando implementado.

Regras:

- Quantidade e custo nao podem ser negativos.
- Entrada com lote vencido deve ser bloqueada salvo regra explicita.
- StockMovement deve apontar para origem.
- Uma encomenda a fornecedor só pode ser editada em `ORDERED` e antes de qualquer recepção.
- A actualização da encomenda não movimenta stock; recepção e cancelamento continuam operações
  próprias e auditáveis.

## Cotação comercial

1. Cliente ou comprador ocasional é identificado.
2. Linhas, validade e condições comerciais são registadas.
3. O Service valida tenant, produtos e armazém e recalcula impostos e totais.
4. A cotação pode ser enviada, aceite, recusada, convertida ou cancelada pelo fluxo próprio.

Regras:

- Só uma cotação em `DRAFT` pode ser editada livremente.
- Cotações enviadas ou decididas abrem apenas em consulta; alterações exigem nova revisão ou
  operação de domínio adequada.
- A actualização preserva número, data e estado e deve ficar auditada.

## Transferencia de stock

1. Utilizador escolhe armazem origem e destino.
2. Sistema valida stock disponivel.
3. Transferencia nasce `DRAFT`, sem reservar ou movimentar stock.
4. Utilizador revê cabeçalho e linhas e submete para `PENDING_APPROVAL`.
5. Gestor ou administrador aprova ou rejeita.
6. Só a aprovação consome FEFO na origem e entra no destino.
7. Movimento fica rastreavel.

Regras:

- Origem e destino nao podem ser iguais.
- Só `DRAFT` pode ser editado; versão optimista impede sobrescrita concorrente.
- `APPROVED`, `REJECTED` e `CANCELLED` são estados terminais e apenas consultáveis.
- Transferencia nao deve duplicar movimentos ao confirmar duas vezes.
- Cancelamento deve respeitar o estado actual.

## Fecho de caixa

1. Operador informa valores de fecho.
2. Sistema compara esperado vs contado.
3. Diferenca e registada.
4. Sessao e fechada.
5. Reabertura exige permissao ou aprovacao.

Regras:

- Sessao fechada nao recebe novas vendas.
- Diferencas devem ser auditadas.
- Fecho nao deve apagar movimentos.

## Folha salarial

1. Funcionario activo e carregado.
2. Salario base, subsidios, ausencias e descontos sao calculados.
3. IRPS e outras retencoes sao aplicadas.
4. Payslip e gerado.
5. Resumo fiscal fica disponivel.

Regras:

- Calculos fiscais devem estar em Services testaveis.
- Configuracao fiscal efectiva deve ser rastreavel.
- Valores monetarios usam `BigDecimal`.

## Impressao/PDF

1. Service de negocio gera ou consulta DTO.
2. Service de `printing` monta o documento.
3. Cabecalho, linhas, totais e rodape usam blocos reutilizaveis.

Regras:

- PDF nao deve conter regra de negocio.
- PDF nao altera stock, caixa nem fiscal.
- Falha de impressao nao deve corromper o documento emitido.
