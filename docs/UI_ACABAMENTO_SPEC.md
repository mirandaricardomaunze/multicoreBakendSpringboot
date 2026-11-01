# Acabamento visual antes do piloto — Especificação e harness

**Última actualização:** 2026-11-01

## Objectivo

Tirar do sistema o que parece inacabado ou obriga a adivinhar: menus com nomes vagos, painéis vazios
sem indicação do passo seguinte e uma coluna sem função em tabelas sem linhas.

## Decisões

### Nomes dos menus de acções

Um menu diz o que contém, como "Gestão do Contrato". Ficam proibidos "Operações", "Decisão",
"Ações", "Acções" e "Opções", e "Mais acções" passa a ter uma só grafia.

"Mais acções" mantém-se: é o menu de excesso comum e tem significado claro.

| Ecrã | Antes | Depois |
|------|-------|--------|
| Cotações | Operações | Gerir Cotação |
| Cotações | Decisão | Decisão do Cliente |
| POS | Operações | Caixa e Turno |
| Salários | Operações | Folha Salarial |
| Conciliação bancária | Operações | Conciliação |
| Utilizadores | Operações | Gerir Utilizador |
| Passivos laborais | Ações | Apuramento |
| Unidades de negócio | Ações | Gerir Unidade |
| Fornecedores | Ações | Gerir Fornecedor |
| Qualidade, atribuições | Acções | Gerir Entrega |
| Qualidade, catálogo | Acções | Gerir Equipamento |
| Qualidade, radar | Acções | Documentos |
| Folha de ponto | Opções | Folha de Ponto |
| Prémios | Mais Ações | Mais acções |

### Painel sem dados

Os gráficos de vendas e da estrutura financeira dizem o que fazer: registar uma venda ou escolher outro
período. O texto vive nos gráficos partilhados, que recebem uma dica opcional.

### Coluna de selecção

A coluna estreita à esquerda das tabelas indica a linha seleccionada. Sem linhas não tem função, por
isso desaparece, junto do canto do cabeçalho. Volta com a primeira linha.

### Cor dos botões secundários

Não muda. `BrightButtonsHarnessTest` fixa esta cor de propósito.

## Cenários

| ID | Cenário | Resultado |
|----|---------|-----------|
| UA-01 | Menu com nome genérico em qualquer ecrã | Teste falha |
| UA-02 | "Mais acções" com outra grafia | Teste falha |
| UA-03 | Gráfico vazio com dica | Mostra a mensagem e a dica |
| UA-04 | Gráfico vazio sem dica | Mostra só a mensagem |
| UA-05 | Painel Inicial | Vendas e estrutura financeira têm dica |
| UA-06 | Tabela sem linhas | Sem coluna de selecção nem canto |
| UA-07 | Tabela recebe a primeira linha | A coluna volta |
| UA-08 | Tabela volta a ficar vazia | A coluna sai |

Teste: `UiFinishHarnessTest`.

## Limites

- A dica do Painel é texto, não um botão.
- Com filtros a esconder todas as linhas, a coluna fica, porque a tabela continua a ter dados.
- Sem revisão com dados reais, nomes longos e valores altos ainda podem mostrar outros problemas.
