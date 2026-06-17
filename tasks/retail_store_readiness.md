# Task - Prontidao para Loja e Mercearia

**Criado em:** 2026-06-17  
**Estado:** Software operacional implementado e testado; pendente validação manual em loja, restore real e hardware  
**Fonte:** `docs/RETAIL_STORE_SPEC.md` e `docs/RETAIL_STORE_HARNESS.md`

## Objectivo

Fechar as lacunas que impedem o Multicore de operar com seguranca numa loja/mercearia real, preservando POS, stock, fiscal, caixa, tenant e auditoria.

## Contexto actual

Ja existe base relevante: POS, sessao de caixa, multi-pagamento, fiado, produtos com codigo de barras, stock, lotes, validades, FEFO, compras, fornecedores, facturas, notas, tesouraria, PDFs, auth HTTP inicial, tenant context e migration baseline. O risco principal agora e transformar scaffold em comportamento operacional validado.

## Ordem de execucao

### Fase 1 - Seguranca e permissao efectiva

- [x] Confirmar que `/api/**` exige token e empresa via `SecurityInterceptor`, mantendo `/api/auth/login` publico.
- [x] Confirmar que o desktop preenche `CurrentUserContext` e o client HTTP envia `X-Company-Id`.
- [x] Criar guard central de permissao por role para operacoes sensiveis.
- [x] Aplicar permissao em sangria, fecho com diferenca, anulacao de factura/recibo, notas e transferencias.
- [x] Auditar fecho de caixa, sangria/suprimento, anulacoes, aprovacoes/rejeicoes/cancelamentos de notas e transferencias.
- [x] Testar guard de permissao e transferencia de stock.
- [x] Definir matriz operacional inicial: EMPLOYEE vende/consulta, MANAGER aprova operações sensíveis, ADMIN gere backup/utilizadores.
- [ ] Auditar tentativas bloqueadas quando houver politica definida.

### Fase 2 - Devolucao e troca POS

- [x] Definir fluxo unico: documento origem -> nota de credito -> impacto em stock/caixa/tesouraria.
- [x] Criar endpoint `POST /api/pos/returns`.
- [x] Exigir motivo e permissao `MANAGER/ADMIN`.
- [x] Registar reembolso por CASH, CARD, BANK_TRANSFER ou CREDIT.
- [x] Repor stock via nota de credito `RETURN`.
- [x] Criar UX Swing de devolucao/troca: seleccao da venda, quantidades devolvidas, motivo, metodo de reembolso e armazem. Troca operacional fecha por devolucao + nova venda.

### Fase 3 - Produtos de mercearia

- [x] Adicionar `ProductSaleType`: UNIT, BOX, WEIGHT, SERVICE.
- [x] Adicionar `stockTracked` para produto nao-stockavel/servico.
- [x] Migrar linhas comerciais para quantidade decimal (`BigDecimal`).
- [x] Ajustar POS Swing para aceitar quantidade decimal.
- [x] Garantir que produto sem stock nao cria movimento fisico.
- [x] Criar migration `V8__retail_product_sale_type_and_decimal_quantities.sql`.
- [ ] Especificar codigo de barras de peso/preco variavel antes de integrar balanca.

### Fase 4 - Stock operacional

- [x] Implementar ajuste/contagem por endpoint `POST /api/inventory/adjustments`.
- [x] Exigir motivo e permissao `MANAGER/ADMIN`.
- [x] Auditar ajuste de stock.
- [x] Manter alertas existentes de stock minimo e validade proxima no dashboard/stock.
- [x] Criar UX de contagem fisica no Swing a partir do ajuste de stock, com quantidade contada e motivo.
- [ ] Repetir cenarios FEFO do harness em ambiente real.

### Fase 5 - Relatorios e fecho diario

- [x] Criar endpoint `GET /api/reports/daily-store`.
- [x] Incluir vendas do dia, fiado em aberto, pagamentos por metodo, movimentos de caixa e top produtos.
- [x] Ajustar top produtos para quantidades decimais.
- [x] Relatorio por operador em detalhe.
- [x] Margem bruta por produto.
- [ ] Conferir totais contra facturas, pagamentos, caixa e tesouraria em validação manual.

### Fase 6 - Backup/restore e hardware

- [x] Implementar verificacao nao destrutiva de backup JSON por empresa, estrutura e secoes obrigatorias.
- [x] Criar UX Swing para verificar backup seleccionado e auditar a verificacao.
- [ ] Testar restore em ambiente separado.
- [ ] Validar impressora real, leitor de codigo de barras e rotina de gaveta.
- [ ] Documentar decisao de balanca e etiquetas.

## Definition of done

- Spec e harness actualizados quando uma decisao muda.
- Fases fechadas com testes automatizados quando a regra for critica.
- Cenarios do harness marcados com evidencia.
- `mvn -q clean compile` passa.
- `mvn -q test` passa.
- Nenhum endpoint sensivel fica sem tenant, permissao ou auditoria.
