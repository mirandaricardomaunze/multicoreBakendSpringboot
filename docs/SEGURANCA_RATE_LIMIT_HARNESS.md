# Harness — Rate-limiting do login

Ver [SEGURANCA_RATE_LIMIT_SPEC.md](SEGURANCA_RATE_LIMIT_SPEC.md).

## Automáticos — `LoginRateLimiterTest`

| ID    | Cenário                                        | Esperado                          |
|-------|------------------------------------------------|-----------------------------------|
| RL-01 | N falhas seguidas (= limite)                   | bloqueado (`checkAllowed` lança)  |
| RL-02 | Sucesso limpa o contador                       | falhas subsequentes < limite → ok |
| RL-03 | Outro utilizador                               | não afectado                      |
| RL-04 | Chave insensível a maiúsculas/espaços          | "Bob"/" bob "/"BOB" = mesmo alvo  |

## Manuais

| ID     | Cenário                                                        | Esperado                                               |
|--------|----------------------------------------------------------------|--------------------------------------------------------|
| RL-50  | 5 logins com senha errada, depois a correcta                   | 6ª tentativa bloqueada com "Tente novamente em N min". |
| RL-51  | Esperar a janela de bloqueio e tentar de novo                  | Login volta a ser aceite.                              |
| RL-52  | Ajustar `security.login.max-attempts`/`lockout-minutes`        | Comportamento acompanha a config.                      |
