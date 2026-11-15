# Validação no próprio campo

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/FieldValidationHarnessTest.java`

## Problema

Quando um valor do formulário estava errado, a mensagem aparecia numa faixa no topo do diálogo
("Não foi possível gravar"). O operador tinha de procurar qual era o campo. Só 29 ecrãs usavam o
`FormField` com erro inline; os outros lançavam `IllegalArgumentException("…")` e, às vezes,
focavam o campo à mão.

## Regras

| ID    | Regra |
|-------|-------|
| VC-01 | Um campo solto com erro fica com borda vermelha e a mensagem logo por baixo; editar o campo limpa os dois. |
| VC-02 | Dentro de um `FormField`, a mensagem vai para a linha de erro do próprio `FormField` (sem duplicar). |
| VC-03 | Num `JSpinner` o erro fica no editor de texto, onde o operador escreve. |
| VC-04 | Um erro que não é de campo (regra de negócio do servidor, falha geral) não assinala nenhum campo. |
| VC-05 | O `ModernFormDialog` encaminha `FieldValidationException` para o campo; a faixa do topo passa a dizer "Corrija o campo assinalado". |
| VC-06 | Ninguém escreve `campo.requestFocus(); throw new IllegalArgumentException(...)`: usa-se `throw FieldValidationException.of(campo, mensagem)`. |
| VC-07 | Os formulários principais (activos, viaturas, contratos, transferências, fichas técnicas, utilizadores) assinalam o campo. |

## Como usar

```java
dialog.setOnSave(() -> {
    if (nameField.getText().isBlank()) {
        throw FieldValidationException.of(nameField, "O nome é obrigatório.");
    }
    ...
});
```

`FieldValidationException` continua a ser um `IllegalArgumentException`: quem a apanha fora de um
diálogo continua a mostrar a mensagem como antes.

## Fica de fora, de propósito

As validações de tabela ("Adicione pelo menos uma linha"), as que envolvem vários campos ao mesmo
tempo e os erros de programação (argumentos inválidos de componentes) continuam na faixa do topo.
Não há um campo único para assinalar.
