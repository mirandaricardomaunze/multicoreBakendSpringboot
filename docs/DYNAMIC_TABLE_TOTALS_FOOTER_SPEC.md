# SPEC-DTT-001: Barra de Totais e Estatísticas Instantâneas no Rodapé das Tabelas

## 1. Princípio e Enquadramento

Em tabelas financeiras, comerciais e de stock, o utilizador frequentemente necessita de saber:
- Quantos registos existem no total e quantos estão seleccionados.
- Qual é o somatório dos valores monetários das linhas que seleccionou.
- Qual é o somatório das quantidades das linhas seleccionadas.

Sem esta funcionalidade, o utilizador teria de recorrer a calculadora externa ou exportar para folha de cálculo.

## 2. Comportamento e Apresentação Canónica

1. **Estado Neutro (Sem Selecção):**
   - Apresenta: `{N} registo(s)`.
2. **Estado com Selecção (1 ou Mais Linhas):**
   - Apresenta: `{N} registo(s) · {S} sel.`
   - Se as linhas seleccionadas contiverem valores monetários (`... MT`):
     - Acrescenta: `[Total: {Soma} MT]` formatado estritamente com **2 casas decimais**.
   - Se contiverem quantidades:
     - Acrescenta: `[Qtd: {Soma}]` formatado com **2 casas decimais**.
3. **Reatividade em Tempo Real:**
   - Atualiza instantaneamente via `ListSelectionListener` sempre que a seleção de linhas muda (clique, `Shift+Clique`, `Ctrl+Clique` ou selecção por teclado).
   - Atualiza na filtragem de texto ou alteração de página.

## 3. Harness e Verificação

- `DynamicTableTotalsFooterHarnessTest` verifica:
  1. Cálculo de registos e linhas seleccionadas.
  2. Somatório dinâmico de valores monetários a 2 casas decimais.
  3. Atualização automática sem bloqueio do Event Dispatch Thread (EDT).
