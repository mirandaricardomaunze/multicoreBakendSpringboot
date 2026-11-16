# Mensagens de erro com uma só frase

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/UserErrorsHarnessTest.java`

## Problema

Cada ecrã montava a sua frase: "Erro ao carregar radar: …", "Falha ao…", "Não foi possível…",
"Erro: …". E um erro de programação (`NullPointerException`) podia chegar ao operador como "null" ou
como texto técnico em inglês.

## Regras

| ID    | Regra |
|-------|-------|
| ER-01 | Uma só frase: "Não foi possível {acção}." seguida do motivo, quando o motivo é para pessoas (resposta do servidor, regra de negócio, validação). |
| ER-02 | Um erro de programação não é mostrado: aparece "Tente novamente; se o problema continuar, contacte o suporte." e o detalhe vai para o log. |
| ER-03 | Se o invólucro não tem texto, usa-se a mensagem da causa; sem acção indicada diz "concluir a operação". |
| ER-04 | Nenhum toast ou diálogo monta `"Erro ao …: " + e.getMessage()` à mão: usa `UserErrors.toast(dono, "acção", erro)` ou `UserErrors.show(...)`. |

65 mensagens foram convertidas. As que já tinham texto próprio sem o detalhe técnico ficaram como estavam.
