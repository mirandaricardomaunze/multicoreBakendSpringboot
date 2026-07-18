# Harness — Cor de estado nas linhas

> Cenários para [COR_ESTADO_LINHAS_SPEC.md](COR_ESTADO_LINHAS_SPEC.md). Manuais (apresentação Swing).

**Última actualização:** 2026-07-03

| ID    | Passos | Esperado |
|-------|--------|----------|
| CE-01 | Stock → Níveis de Stock, com um produto a 0 e outro abaixo do mínimo. | Linha esgotada com tom **vermelho** (ESGOTADO), abaixo do mínimo **amarelo** (BAIXO), restantes neutro (EM STOCK verde). |
| CE-02 | Compras → Reposição. | Esgotados a **vermelho**, abaixo do mínimo a **amarelo**. |
| CE-03 | Faturas / Encomendas (têm coluna Estado). | Anuladas/canceladas a vermelho, pendentes a amarelo, pagas/aprovadas a verde — sem código novo. |
| CE-04 | Seleccionar uma linha colorida. | A selecção sobrepõe-se limpa (tom de estado não compete com a cor de selecção). |
| CE-05 | Alternar tema claro/escuro. | O tom mantém-se subtil e legível (é misturado com o fundo do tema). |

## Verificação

- `mvn clean test` → verde (mudança é só de apresentação; sem novos testes).
