# Harness — Layout responsivo do POS

> Validação da [spec](POS_LAYOUT_RESPONSIVO_SPEC.md). Os cenários visuais devem ser executados no
> Windows real; os testes automatizados cobrem a regra de adaptação da tabela.

**Última actualização:** 2026-08-11

| ID | Passos | Esperado |
|---|---|---|
| LR-01 | Abrir Venda POS em 1366×768 | Catálogo ocupa aproximadamente 36% e carrinho 64%; ambos são utilizáveis. |
| LR-02 | Adicionar artigos suficientes para preencher 12 linhas | Tabela tem altura útil e scroll vertical apenas nas linhas. |
| LR-03 | Reduzir a largura da janela | Colunas preservam larguras legíveis e aparece scroll horizontal. |
| LR-04 | Aumentar até o viewport da tabela ultrapassar 900 px | Scroll horizontal desaparece e a tabela volta a preencher a largura. |
| LR-05 | Reduzir a altura da janela | Subtotal, IVA, total, Fiado e Finalizar Venda permanecem visíveis. |
| LR-06 | Arrastar o divisor para ambos os lados | Catálogo e carrinho respeitam os mínimos enquanto houver largura disponível. |
| LR-07 | Comparar Cliente, Código de barras e Mais opções | Controlos mantêm altura visual canónica; não ficaram maiores. |
| LR-08 | Abrir caixa, adicionar artigo e finalizar | Checkout, totais, stock e recibo não apresentam regressão. |

## Verificação técnica

```text
mvn clean compile
mvn -Dtest=PosLayoutTest,POSKeyboardShortcutTest,POSServiceTest test
```

