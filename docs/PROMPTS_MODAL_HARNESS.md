# Harness — Prompts de dados em modal

> Cenários de validação manual da [spec](PROMPTS_MODAL_SPEC.md). UI/Swing — verificação manual.
> Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-30

| ID    | Passos                                                                       | Esperado                                                                                   |
|-------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| PM-01 | POS › **Abrir Caixa**                                                        | Modal (`fas-lock-open`) com campo de saldo; valor não numérico → erro e **fica aberto**.   |
| PM-02 | POS › Abrir Caixa com valor válido                                           | Sessão abre; estado da caixa actualiza.                                                    |
| PM-03 | POS › **Fechar Caixa**                                                       | Modal (`fas-lock`) pede saldo físico; depois resumo de fecho como antes.                   |
| PM-04 | Comercial › **Anular Fatura** / **Anular Recibo**                           | Modal (`fas-ban`); motivo vazio → erro e fica aberto; com motivo → anula e recarrega.      |
| PM-05 | Comercial › **Rejeitar NC** / **Rejeitar ND**                               | Modal (`fas-times-circle`); motivo obrigatório; rejeita e recarrega.                       |
| PM-06 | Compras › **Cancelar Encomenda**                                            | Modal (`fas-ban`) com nº da encomenda; motivo obrigatório.                                 |
| PM-07 | Stock › **Rejeitar Guia** de transferência                                  | Modal (`fas-times-circle`); rejeita sem mover stock.                                       |
| PM-08 | RH › Férias › **Rejeitar**                                                   | Modal (`fas-times-circle`) com colaborador; motivo obrigatório.                            |
| PM-09 | Qualquer prompt → **Cancelar**                                              | Nada acontece (operação abortada).                                                         |
| PM-10 | Stock › **Criar Armazém**                                                    | Continua a abrir o modal V2 (`fas-warehouse`); V1 morto removido, sem regressão.           |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
