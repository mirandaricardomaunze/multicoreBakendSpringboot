# HARNESS-ICF-001: Matriz de Testes e Conformidade do Inventário Físico

## Matriz de Conformidade

| ID | Requisito | Critério de Aceitação |
|---|---|---|
| **ICF-01** | Criação de Sessão | Nova sessão inicia no estado `DRAFT` com numeração `INV-YYYY/N` e itens carregados do catálogo de produtos. |
| **ICF-02** | Contagem Cega | Em modo `blindCounting = true`, os DTOs para operadores não expõem a quantidade esperada nem a variação até à conclusão. |
| **ICF-03** | Registo por Código de Barras | Scanner de código de barras localiza o produto e incrementa ou define a quantidade contada com precisão de 3 casas decimais. |
| **ICF-04** | Apuramento Financeiro | Cálculo correto de `totalSurplusValue`, `totalDeficitValue` e `netFinancialImpact` com base no custo médio/histórico do produto. |
| **ICF-05** | Reconciliação de Stock | Ao fechar a sessão (`CLOSED`), os stocks dos produtos são atualizados exatamente para as quantidades contadas e são registados movimentos `INVENTARIO_AJUSTE`. |
| **ICF-06** | Dossiê PDF A4 | Emissão limpa do PDF em A4 com cabeçalho da empresa, tabela de divergências e termo de responsabilidade. |
| **ICF-07** | UI Swing Responsiva | Painel `PhysicalInventoryPanel` responsivo, em conformidade com o tema escuro (`UIHelper`), com atalhos de barra de pesquisa e tamanho $\le 1000$ linhas. |
