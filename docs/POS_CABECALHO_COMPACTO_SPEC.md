# Spec — Cabeçalho compacto do POS (barcode na linha dos selects)

> Melhoria de layout do [POSPanel](../src/main/java/mz/multicore/erp/gui/POSPanel.java), alinhada com
> [UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md) e [CONVENTIONS.md §11](../CONVENTIONS.md). **Só
> apresentação** — sem mexer em `POSService`, DTOs nem cálculos.

**Última actualização:** 2026-08-16

> Revisão final: pesquisa do cliente, cliente, armazém, conta e código de barras ficam todos numa
> única linha. O antigo botão "Mais opções" e o diálogo associado foram removidos.

## Problema

Na aba **Venda POS**, o cabeçalho ocupa **duas linhas** empilhadas acima do workspace
(`salesTop`):

1. `topSelectsBar` (3 colunas): **Cliente** (pesquisa empilhada sobre o combo → coluna alta,
   `weightx=0.5`), **Armazém Expedição** (combo) e **Conta Tesouraria** (combo).
2. `scannerBar` (linha própria, largura total): ícone `fas-barcode` + campo de código de barras.

Resultado: o campo de código de barras está numa linha **isolada por baixo** dos selects, e o
Cliente é largo de mais. As duas linhas roubam altura ao **catálogo de produtos em cards**
(`leftPanel`/`productGridScroll`), que é a área onde o operador selecciona e adiciona ao carrinho.

## Decisões

- **Cinco campos sempre visíveis:** Pesquisar cliente (20%), Cliente (22%), Armazém (16%), Conta
  (18%) e Código de barras (24%). Os pesos totalizam 100% e adaptam-se à largura disponível.
- **Uma única altura canónica:** inputs, combos e botão Novo usam 38 px e partilham a mesma base.
- **Sem Mais opções:** armazém e conta deixam de ficar escondidos; não existe expansão vertical.

- **Código de barras sobe para a linha dos selects.** O campo deixa de ter uma `scannerBar`
  própria e passa a ser a **4.ª coluna** de `topSelectsBar`, alinhado (mesma linha) com o combo
  **Armazém Expedição**. Liberta-se a linha inteira do scanner.
- **Reduzir a largura do Cliente.** O peso do Cliente baixa de `0.5` para `~0.34`; Armazém e Conta
  ficam compactos (`~0.16` cada) e o código de barras leva `~0.34`. Isto cria espaço horizontal
  para a 4.ª coluna, como pedido pelo utilizador.
- **Campo de código de barras a altura única** (ícone `fas-barcode` **dentro** do input, padrão
  visual de `searchRow`), 38px (`FORM_CONTROL_HEIGHT`), para alinhar com os combos.
- **Sem perda de funcionalidade.** Mantém-se a pesquisa de cliente (`clientSearchField` empilhado
  sobre o combo na coluna Cliente), o botão **+ Novo**, e o `handleBarcodeScan()` no Enter do campo.
  Os campos da linha ancoram a NORTE, pelo que o código de barras alinha com o **combo** de
  Armazém Expedição (a pesquisa de cliente continua por cima do combo de Cliente).
- **Altura ganha vai para o catálogo.** Como `leftPanel` ocupa o `CENTER` do workspace, remover a
  linha do scanner aumenta automaticamente a altura visível do grid de cards.

## Não-objetivos

- Não remover a pesquisa de cliente nem torná-la um combo editável (fora de âmbito; seria outra
  iteração se for preciso ainda mais altura).
- Não tocar no carrinho, checkout, histórico de vendas, nem em `POSService`/DTOs/cálculos.
- Não introduzir FlatLaf nem nova biblioteca.
