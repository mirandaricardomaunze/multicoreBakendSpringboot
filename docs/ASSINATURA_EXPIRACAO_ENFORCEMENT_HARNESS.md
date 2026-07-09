# Harness — Assinatura expirada bloqueia sessão aberta

> Cenários para [ASSINATURA_EXPIRACAO_ENFORCEMENT_SPEC.md](ASSINATURA_EXPIRACAO_ENFORCEMENT_SPEC.md).
> Todos manuais (comportamento de integração UI + sessão). A lógica de severidade já é coberta
> indiretamente pelo chip/aviso existentes.

## Preparação

Para forçar a expiração sem esperar: no superadmin (`PlataformaPanel` → Assinaturas) **Suspender** a
empresa, **ou** definir a validade para uma data passada. Reverter no fim (Reactivar / registar
pagamento estende a validade).

## Manuais

| ID    | Passos | Esperado |
|-------|--------|----------|
| AE-50 | Com a empresa **activa**, entrar e usar a app normalmente. | Sem popup de bloqueio; app funciona. |
| AE-51 | Com a app aberta, o superadmin **suspende** a empresa; disparar a vigia (esperar o ciclo ou reiniciar para simular o arranque). | Popup de erro "Assinatura … suspensa/expirada" e, ao confirmar, **volta ao ecrã de login**. |
| AE-52 | No ecrã de login pós-bloqueio, tentar **re-entrar** com a empresa ainda suspensa. | Login **recusado** (mensagem de acesso/assinatura). |
| AE-53 | Superadmin **reactiva** / regista pagamento; voltar a fazer login. | Login **aceite**; app abre normalmente. |
| AE-54 | Assinatura a **≤7 dias** (não expirada), abrir a app. | Só **aviso** amarelo (arranque/chip); **não** bloqueia nem faz logout. |
| AE-55 | **Superadmin** (sem empresa) com a app aberta durante muito tempo. | Nunca é bloqueado nem expulso (não é vigiado). |
| AE-56 | Trocar de **tema** com a app aberta (reconstrói a janela). | A vigia continua activa (novo `Timer`); sem duplo-popup nem fuga do `Timer` antigo. |
| AE-57 | Bloqueio dispara enquanto há um popup/modal aberto ou logo após gravar um documento. | A gravação submetida completa; o logout ocorre depois do popup, sem corromper dados. |

## Verificação

- `mvn -o compile` limpo.
- Sem teste automático dedicado (comportamento de sessão/UI); severidade reusa a lógica já existente.
