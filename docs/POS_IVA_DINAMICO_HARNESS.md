# Harness — IVA dinâmico no POS (taxa por produto)

> Validação da [spec](POS_IVA_DINAMICO_SPEC.md). IV-01..04 são testes automáticos
> (`POSServiceTest`); os restantes são UX/Swing de verificação manual.

**Última actualização:** 2026-06-27

## Automático (`mvn test`)

| ID    | Teste                                                          | Esperado                                                        |
|-------|---------------------------------------------------------------|----------------------------------------------------------------|
| IV-01 | `checkout_produtoIsento_naoAplicaIva`                          | Produto isento → linha `taxRate=0`, total da linha = líquido.   |
| IV-02 | `checkout_produtoComIva16_aplicaIvaDinamico`                   | Produto 16% → linha `taxRate=0.16`, total = líquido × 1,16.     |
| IV-03 | Testes POS existentes (numerário, fiado, multi-método)         | Sem regressões (produto sem taxa cai na padrão 16%).           |
| IV-04 | `mvn clean test`                                               | BUILD SUCCESS, 0 falhas.                                        |

## Manual (app a correr)

| ID    | Passos                                                                  | Esperado                                                                       |
|-------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| IV-05 | POS › abrir caixa › adicionar **Açúcar** (isento)                      | Coluna **IVA = "Isento"**; Líquido = Total.                                     |
| IV-06 | Adicionar **Óleo Alimentar** (16%) na mesma venda                      | Coluna **IVA = valor (16%)**; Total = líquido + IVA.                            |
| IV-07 | Observar o rodapé do carrinho                                          | **Subtotal s/ IVA** e **IVA** discriminados; **TOTAL A PAGAR** = líquido + IVA. |
| IV-08 | Finalizar venda em numerário                                           | Valor a pagar/troco usa o total **com IVA**; recibo mostra IVA por linha.       |
| IV-09 | Stock › Cadastrar Produto                                              | Existe seletor **Taxa de IVA** (default IVA Normal 16%); novo produto respeita-o. |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # 157 testes (155 + IV-01/IV-02), 0 falhas
```
</content>
