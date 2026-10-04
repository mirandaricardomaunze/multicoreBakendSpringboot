# SPEC — Homologação dos editores documentais PHC

**ID:** SPEC-PHC-DOCUMENT-HOMOLOGATION-001  
**Estado:** obrigatório  
**Âmbito:** Comercial, Compras, Stock, impressão e desktop Swing

## Objectivo

Confirmar de ponta a ponta que os documentos editáveis usam exclusivamente a grelha para criar e
actualizar linhas, persistem pela API, voltam a abrir com os mesmos valores e respeitam a
imutabilidade depois da transição de estado. A homologação automatizada usa H2 isolado e não cria
facturas, movimentos ou encomendas na base operacional.

## Fluxos homologados

| Fluxo | Criar/actualizar | Reabrir | Estado protegido |
|---|---|---|---|
| Cotação | `DRAFT` | `GET /quotations/{id}` | `SENT` |
| Encomenda de cliente | estados editáveis da encomenda | `GET /orders/{id}` | `CANCELLED` ou fase posterior |
| Encomenda a fornecedor | `ORDERED`, antes da recepção | listagem HTTP | `PARTIALLY_RECEIVED` |
| Preparação de factura | grelha anterior à emissão | factura emitida na listagem | sem endpoint de alteração |
| Transferência de stock | `DRAFT` | `GET /transfers/{id}` | `PENDING_APPROVAL` |

## Regras de aceitação

1. `Adicionar linha` cria a linha na tabela e a pesquisa do produto ocorre na primeira célula.
2. Produto, `Qtd`, `Emb.`, `Cx.` e demais campos aplicáveis são `TableCellEditor` reais.
3. Guardar termina a célula activa antes de validar e enviar o pedido HTTP.
4. Produto inexistente, quantidade não positiva, desconto fora de 0–100 e preço negativo bloqueiam
   a gravação sem fechar o editor.
5. A quantidade HTTP é sempre a quantidade total em unidades após converter
   `Caixa → Embalagem → Unidade`.
6. A resposta do servidor é a fonte oficial para preço, IVA, totais, stock, FEFO e versão.
7. Reabrir apresenta as linhas persistidas e não uma cópia temporária da interface.
8. Documento submetido, enviado, recebido, cancelado ou emitido não volta ao modo editável.
9. PDFs e documentos configuráveis preservam `Emb.`, `Cx.` e `% Cx.` quando activados.
10. Não pode permanecer código activo que transporte uma linha da tabela para um formulário antigo.

## Evidência mínima

- Harness estrutural do desktop e arranque integral do `MainFrame`.
- Integração HTTP H2 para criação, actualização, leitura, versão optimista e bloqueio por estado.
- Testes de composição de embalagens e renderização das colunas documentais.
- Compilação Maven limpa dos módulos `contracts`, `backend` e `desktop`.
- Auditoria visual do aplicativo em execução; a autenticação é efectuada pelo utilizador quando a
  janela de login estiver presente.

