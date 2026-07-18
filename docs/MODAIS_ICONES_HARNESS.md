# Harness — Ícones nos modais de formulário

> Validação da [spec](MODAIS_ICONES_SPEC.md). É UX/Swing — verificação manual. Critério técnico:
> `mvn clean compile` + app arranca; `mvn test` sem regressões.

**Última actualização:** 2026-06-28

| ID    | Passos                                                                      | Esperado                                                                                  |
|-------|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| MI-01 | Comercial › Emitir Nova Fatura                                              | Título com ícone `fas-file-invoice` (azul) à esquerda; ícone igual na barra de título.    |
| MI-02 | Compras › Novo Fornecedor                                                   | Título com ícone `fas-truck`.                                                              |
| MI-03 | Compras › Editar Fornecedor                                                 | Mostra `fas-truck` (domínio ganha ao verbo "Editar"), **não** o lápis.                     |
| MI-04 | Compras › Registar Compra (Entrada de Stock)                               | Título com ícone `fas-download`.                                                           |
| MI-05 | Compras › Nova Encomenda a Fornecedor                                       | Título com ícone `fas-file-signature`.                                                     |
| MI-06 | Stock › Nova Categoria / Editar Categoria                                   | Título com ícone `fas-tags`.                                                               |
| MI-07 | Qualquer modal `ModernFormDialog`: botões em baixo                          | Cancelar com `fas-times`; Gravar com `fas-save`. Ícones a 14 px, sem emojis no label.      |
| MI-08 | Modal legado `JOptionPane` (Stock › Cadastrar Produto)                      | Cabeçalho premium (badge `fas-boxes` + título + subtítulo + divisória); grava sem regressão. |
| MI-09 | Stock › **Nova Transferência de Stock**                                     | Cabeçalho premium com badge `fas-exchange-alt` + subtítulo "Mova stock entre armazéns".     |
| MI-10 | POS › Devolver/Trocar venda                                                 | Badge `fas-undo` + subtítulo "Devolução por nota de crédito".                                |
| MI-11 | RH › Novo Colaborador / Gerar Recibo / Falta / Férias                       | Cada um com badge de domínio (users / file-invoice-dollar / user-times / umbrella-beach).    |
| MI-12 | Compras › Pagar a Fornecedor · Comercial › NC/ND/Recibo/Receber Pagamento   | Todos com cabeçalho premium e badge coerente; gravam sem regressão.                          |
| MI-13 | Qualquer modal legado migrado (ex.: Cadastrar Produto, Transferência)       | Botões **estilizados com ícone** (Cancelar `fas-times` + confirmação), **não** os nativos do JOptionPane. |
| MI-14 | Modal alto (Cadastrar Produto)                                              | Botões no rodapé fixo, **sempre visíveis**; o conteúdo faz scroll no meio.                   |
| MI-15 | Botões de confirmação contextuais                                          | "Receber"/"Pagar" (`fas-money-bill-wave`), "Emitir"/"Confirmar" (`fas-check`), resto "Gravar" (`fas-save`). |
| MI-16 | Inputs em grelha (Cadastrar Produto, Colaborador)                          | Campos dispostos em **2 colunas** (label sobre campo), aspecto compacto/profissional.        |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
