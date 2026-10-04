# Harness — Composição Caixa → Embalagem → Unidade

| ID | Cenário | Resultado esperado |
|---|---|---|
| PC-01 | Configurar 12 embalagens e 6 unidades | Total derivado de 72 unidades por caixa |
| PC-02 | Criar composição com zero/negativo | Regra recusada com mensagem clara |
| PC-03 | Total que excede `int` | Regra recusada sem overflow silencioso |
| PC-04 | Converter 2 cx + 3 emb + 4 un, em 12×6 | 166 unidades |
| PC-05 | Decompor 166 unidades, em 12×6 | 2 cx + 3 emb + 4 un |
| PC-06 | Pedido antigo envia só `unitsPerBox=24` | Composição compatível 24×1 |
| PC-07 | Criar/editar produto pelo desktop | Mostra os dois factores e total somente leitura |
| PC-08 | Reabrir produto existente | Composição regressa preenchida sem alterar stock |
| PC-09 | Suite de arquitectura | `MultiModuleArchitectureHarnessTest` verde |
| PC-10 | Build do reactor | `mvn clean compile` verde |
| PC-11 | Documento com 18 unidades de produto 12×6 | Colunas mostram `3` embalagens, `0.25` caixas e `25%` da caixa |
| PC-12 | Documento com 90 unidades de produto 12×6 | Colunas mostram `15` embalagens, `1.25` caixas e `125%` da caixa |
| PC-13 | Conferir totais antes/depois das colunas | Quantidade, subtotal, IVA e stock permanecem iguais |
