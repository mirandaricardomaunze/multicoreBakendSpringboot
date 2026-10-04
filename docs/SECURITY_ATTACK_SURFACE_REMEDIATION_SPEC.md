# SPEC-SEC-2026-10: Fecho das falhas de autenticacao e isolamento

## Diagnostico

1. `AppUserService.authenticate` aceita duas senhas fixas para o nome `admin`.
2. `resetPassword` e `setManagerPin` procuram um username global sem confirmar que pertence a empresa do administrador. Assim, um administrador de tenant pode alterar ate a conta da plataforma.
3. `/api/monitoring/**` atribui `ADMIN` a qualquer token. A vista multi-tenant e o historico de auditoria podem revelar dados de outras empresas.
4. Estado da empresa e assinatura sao verificados no login, mas nao durante a sessao de oito horas.
5. O login distingue username desconhecido de senha errada; o bloqueio global por username permite negar servico a um utilizador por tentativas de terceiros. O mapa de tentativas nao tem limite.
6. O desktop aceita HTTP remoto; o perfil de desenvolvimento permite acesso remoto a consola H2.

## Regras obrigatorias

- Nao existe senha alternativa para qualquer conta. Senhas sem hash legadas so autenticam por igualdade exacta e sao convertidas em BCrypt apos sucesso. O seed novo grava sempre hash.
- O desktop nao apresenta atalhos com credenciais de demonstracao por omissao; apenas `-Dmulticore.demo-login=true` os activa num ambiente isolado.
- Respostas de credenciais invalidas sao identicas para username inexistente, inactivo ou senha errada. O limite de tentativas conserva mensagem propria.
- A API de utilizadores de tenant so altera contas com acesso a empresa activa. Contas `platformAdmin` nao podem ser alteradas por esta API. A reposicao de senha invalida imediatamente todas as sessoes da conta, incluindo pela API da plataforma.
- Uma chamada de monitorizacao exige papel real `ADMIN` na empresa activa ou `SUPERADMIN` da plataforma. Para `ADMIN`, a saude multi-tenant e os eventos de auditoria sao filtrados pela empresa activa. Historico de incidentes globais e envio de email de teste exigem `SUPERADMIN`.
- Cada chamada de tenant revalida utilizador activo, associacao, empresa activa e assinatura que permita acesso. A suspensao produz efeito no pedido seguinte.
- Tentativas de login sao limitadas por par `(username, origem)` e por origem. Uma origem nao bloqueia a conta noutras origens. Entradas expiradas sao removidas e o armazenamento tem limite fixo. `X-Forwarded-For` so e aceite quando o pedido veio de proxy confiavel configurado; a configuracao canonica do Caddy substitui esse cabecalho pelo IP da ligacao cliente.
- O desktop aceita HTTP apenas para loopback local (`localhost`, `127.0.0.1`, `::1`); qualquer destino remoto exige HTTPS. A consola H2 de desenvolvimento so aceita ligacoes locais.

## Casos de regressao

1. `admin` com hash de outra senha recusa `admin` e `password`.
2. `ADMIN` da empresa A nao altera senha ou PIN de conta exclusiva da empresa B ou de `superadmin`; reposicao valida da sua empresa revoga sessoes.
3. `SELLER` recebe 403 em monitorizacao; `ADMIN` ve apenas a sua empresa; `SUPERADMIN` ve todas e pode enviar teste de email.
4. Conta suspensa, empresa desactivada ou assinatura expirada perde acesso no pedido seguinte.
5. Origem A esgota limites sem bloquear a mesma conta na origem B; usernames aleatorios nao fazem o mapa crescer sem limite.
6. URL HTTP remota e rejeitada; HTTP loopback e HTTPS remoto continuam validos; H2 remoto fica desactivado.

## Limites operacionais

As sessoes e os contadores continuam em memoria e exigem uma instancia de backend. O deployment deve activar a confianca no proxy apenas quando o backend e acessivel exclusivamente pelo Caddy configurado nesta spec.
