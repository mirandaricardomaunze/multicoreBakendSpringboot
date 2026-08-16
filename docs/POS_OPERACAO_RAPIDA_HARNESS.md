# Harness — Operação rápida e acabamento profissional do POS

**Última actualização:** 2026-08-05

| ID | Passos | Esperado |
|---|---|---|
| OR-01 | Abrir Venda POS | Cliente e código de barras visíveis; armazém/conta recolhidos. |
| OR-02 | Premir Mais opções duas vezes | Armazém/conta aparecem e voltam a recolher sem perder selecção. |
| OR-03 | Premir F2 e F4 | Foco passa para pesquisa de produto e cliente, respectivamente. |
| OR-04 | Com carrinho preenchido, seleccionar linha e premir F6 | Abre Alterar Quantidade. |
| OR-05 | Fazer duplo clique numa linha | Abre o mesmo editor; quantidade decimal positiva actualiza totais. |
| OR-06 | Tentar quantidade vazia, inválida, zero ou negativa | Modal permanece aberto e apresenta mensagem clara. |
| OR-07 | Com tabela focada, premir Delete | Linha seleccionada é removida; Delete num campo de texto não remove linha. |
| OR-08 | Premir F9 com caixa/carrinho válidos | Abre pagamento; com estado inválido mostra a validação existente. |
| OR-09 | Escolher Numerário e clicar Exacto/100/200/500/1000 | Valor entregue actualiza e troco/falta recalcula imediatamente. |
| OR-10 | Escolher método electrónico | Recebimento rápido oculta-se; referência fica disponível. |
| OR-11 | Concluir venda e imprimir recibo | Checkout, totais, stock e recibo mantêm comportamento anterior. |

## Verificação técnica

```text
mvn clean compile
mvn -Dtest=POSKeyboardShortcutTest,POSServiceTest test
```
