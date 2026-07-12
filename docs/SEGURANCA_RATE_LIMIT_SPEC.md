# Rate-limiting do login (anti força-bruta)

**Última actualização:** 2026-07-12
**Estado:** feito (limiter + wiring + testes).

## Objectivo

Fechar o gap identificado na revisão de sistema: `/api/auth/login` não tinha limite de tentativas —
exposto a força-bruta. Passa a bloquear um utilizador após várias falhas seguidas.

## Como funciona

- **`LoginRateLimiter`** (memória): por utilizador, ao atingir `security.login.max-attempts` falhas
  seguidas, bloqueia durante `security.login.lockout-minutes`. Um login com sucesso limpa o contador.
- **`AuthController.login`**: `checkAllowed(username)` antes de autenticar; `recordFailure` em qualquer
  falha (senha errada, inativo, inexistente — não revela qual, e trava enumeração de utilizadores);
  `recordSuccess` ao entrar.
- **Config** (`application.properties`): `security.login.max-attempts=5`, `security.login.lockout-minutes=15`.

## Regras / limites

- Chave por **utilizador** (case/espaço-insensível). Consequência conhecida: um atacante pode forçar o
  bloqueio temporário de um utilizador (lockout-DoS) — mitigado pela janela curta (15 min). Um limite
  adicional **por IP** é o passo seguinte natural.
- Estado em **memória** (como as sessões) — reinicia com a app; adequado ao backend em processo do
  desktop. Num backend central escalável, mover para um store partilhado (ex.: Redis).
