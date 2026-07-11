# Harness — Balança / venda ao peso

Ver [BALANCA_PESO_VARIAVEL_SPEC.md](BALANCA_PESO_VARIAVEL_SPEC.md).

## Automáticos — `ScaleBarcodeParserTest` (lógica pura)

| ID    | Cenário                                                        | Esperado                              |
|-------|---------------------------------------------------------------|---------------------------------------|
| BV-01 | Etiqueta de peso `2000420015000`                              | PLU `00042`, 1500 g → **1.500 kg**    |
| BV-02 | Modo `PRICE`, `2000420123500`                                | preço **123.50 MT**, `embedsPrice`    |
| BV-03 | EAN de fabricante `5601234567890` (prefixo ≠ 2)              | vazio (não é balança)                 |
| BV-04 | Comprimento ≠ 13                                              | vazio                                 |
| BV-05 | Código não-numérico                                          | vazio                                 |
| BV-06 | `enabled=false`                                              | vazio (tudo tratado como normal)      |
| BV-07 | Config inválida (soma ≠ 13)                                  | `isValid()=false`, parse vazio        |
| BV-08 | Prefixo multi-dígito `20` + PLU 4 + medida 6                 | PLU `0042`, 1500                      |
| BV-09 | Espaços nas pontas                                           | tolerado                              |
| BV-10 | `null` / vazio                                               | vazio, sem excepção                   |

Correr: `mvn -o test -Dtest=ScaleBarcodeParserTest`.

## Manuais — POS + hardware

| ID     | Cenário                                                                                   | Esperado                                                            |
|--------|-------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| BV-50  | Produto ao peso com PLU `42` no campo Código de barras; caixa aberta; scan `2000420015000` | Carrinho: 1.500 kg × preço/kg; IVA e total corados; sem erro.      |
| BV-51  | Scan de duas etiquetas do mesmo artigo (pesos diferentes)                                  | Uma linha, peso somado.                                            |
| BV-52  | PLU inexistente                                                                            | Aviso "PLU não encontrado", nada adicionado.                      |
| BV-53  | Artigo com Tipo de Venda ≠ Peso                                                            | Aviso "defina Tipo de Venda = Peso".                              |
| BV-54  | `retail.scale.embedded=PRICE`: scan com preço embutido                                     | Peso derivado (preço ÷ preço/kg); total da linha = preço da etiqueta. |
| BV-55  | Balança física real → imprime etiqueta → leitor lê → venda fecha e baixa stock            | Fluxo ponta-a-ponta com hardware. *(pendente — loja real)*         |
| BV-56  | Confirmar formato real da balança do cliente vs defaults `retail.scale.*`                  | Ajustar prefixo/dígitos/divisor se diferente. *(pendente)*         |
