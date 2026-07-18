# Assinatura expirada — bloqueio automático da sessão aberta

**Última actualização:** 2026-07-09
**Estado:** feito.

## Objectivo

Quando a assinatura de uma empresa **expira ou é suspensa com a aplicação já aberta**, o sistema deve
**avisar com um popup** e depois **desativar o acesso automaticamente**, sem depender de o utilizador
reiniciar. Fecha a lacuna: até aqui só o **login** era bloqueado (`allowsLogin`) — uma sessão viva
continuava a operar até fechar a app.

## Comportamento

- **Ação ao expirar:** *logout forçado para o ecrã de login*. Mostra o popup de erro e volta ao login,
  que **recusa o re-login** enquanto a assinatura não for regularizada (política `allowsLogin` já
  existente). Reversível: assim que renovar, entra outra vez. Não encerra a aplicação nem perde o
  processo — só termina a sessão.
- **Frequência:** verificação **no arranque** (`checkSubscriptionOnStartup`) e **a cada 6 horas** com a
  app aberta (`javax.swing.Timer`, `SUB_WATCH_INTERVAL_MS`).
- **Severidade** (reusa `MainFrame.subscriptionSeverity`): `EXPIRED`/`SUSPENDED` ⇒ **bloqueia**;
  `≤7 dias` ⇒ só **avisa** (amarelo, não bloqueia); resto ⇒ nada. Empresa **sem assinatura** nunca é
  bloqueada (consistente com `allowsLogin`).

## Peças

- **`MainFrame`**
  - `startSubscriptionWatch()` — arranca o `Timer` de 6h (só para não-superadmin). Chamado no
    construtor (sobrevive à reconstrução por troca de tema).
  - `enforceExpiredSubscription(dto)` — popup de erro + invoca o hook de logout. **Idempotente**
    (`subscriptionEnforced`): dispara uma vez.
  - `checkSubscriptionOnStartup()` — no arranque: `-1` ⇒ enforce; `0` ⇒ aviso ≤7 dias.
  - `dispose()` — pára o `Timer` (evita fugas e duplo-disparo na reconstrução por tema).
- **`UIHelper.onForcedLogout`** — hook estático (espelho de `onThemeChanged`), registado pelo
  `DesktopLauncher`. Sem hook (testes/backend) não tem efeito.
- **`DesktopLauncher`**
  - `loginAndShow()` — extraído de `launch()`; mostra login + prepara contexto + abre janela. Reusável.
  - `logoutToLogin()` — invalida a sessão (`authApiClient.logout`, best-effort), limpa
    `DesktopSessionStore`/`CurrentUserContext`, faz `dispose()` da janela (**não** encerra a app) e
    volta a `loginAndShow()`.

## Notas / limites

- Superadmin não tem empresa/assinatura ⇒ nunca é vigiado nem bloqueado.
- O bloqueio é do lado do **cliente desktop**; a autoridade continua no servidor (`allowsLogin` no
  login e nas chamadas). A vigia apenas antecipa a expulsão da sessão viva em vez de esperar pelo fecho.
- Não mata a sessão a meio de uma ação atómica do servidor — o logout é despoletado na EDT após o
  popup; qualquer gravação já submetida completa normalmente.
