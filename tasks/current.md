# Tarefa actual

Estado: **estabilização para produção**
Actualizado em: 2026-10-31
Histórico: [até 2026-10-31](archive/current-history-through-2026-10-31.md) ·
[até 2026-10-10](archive/current-history-through-2026-10-10.md)

Este ficheiro contém apenas o contexto necessário para continuar o trabalho actual. Resultados
concluídos devem ser movidos para `tasks/archive/` no fecho de cada fase.

## Trabalho activo

- Sem fase de código em curso. Paridade PostgreSQL e formato numérico concluídos e integrados em
  `main`; ver `docs/POSTGRES_SCHEMA_PARITY_SPEC.md` e `docs/FORMATO_NUMERICO_SPEC.md`.
- Em 2026-10-31 o responsável confirmou que não existe base PostgreSQL real, por isso as migrações
  V66, V76, V89 e V96 puderam ser corrigidas no próprio ficheiro.

## Bloqueios antes de produção

- Protecção da branch `main` a exigir os checks `build` e `postgres-parity`.
- XSD oficial da AT-MZ para validar o SAF-T (`docs/FISCAL_SAFT_VALIDACAO_SPEC.md`).
- Homologação por jurista da conformidade legal e do contrato de licença.
- Execução assinada de `docs/GUIA_ACEITACAO_LOJA.md` numa loja piloto.
- Carga e concorrência em PostgreSQL por validar: não anunciar número de utilizadores suportados.

## Decisões permanentes relevantes

29. **Cobertura Integral da Política de Permissões HTTP:**
    - Todos os mappings `/api/**` passam a ser inventariados automaticamente pelo contexto Spring.
    - Endpoint sem área explicitamente classificada reprova o build.
    - Fallback de `MANAGER` alinhado com deny-by-default para rotas desconhecidas.
    - SPEC e harness em `docs/API_ROLE_POLICY_COVERAGE_SPEC.md` e `ApiRolePolicyEndpointCoverageHarnessTest`.

28. **Arranque Enxuto do Perfil Vendedor:**
    - O `MainFrame` deixa de construir quinze painéis de gestão quando a sessão activa é `SELLER`.
    - Permanecem apenas Painel do Vendedor, POS, Comercial, Clientes, CRM e Notificações.
    - Cards opcionais e callback de transferências protegidos contra `null`.
    - SPEC e harness em `docs/SELLER_LEAN_STARTUP_SPEC.md` e `SellerLeanStartupHarnessTest`.

27. **Validação Assíncrona de Vales no Pagamento POS:**
    - Removidas as chamadas HTTP síncronas do EDT no botão de verificação e na confirmação.
    - Feedback imediato `A VALIDAR VALE...`, bloqueio de duplo clique e recuperação do modal em erro.
    - Regra partilhada de estado, validade e saldo protegida por `PosVoucherUiHarnessTest`.
    - Especificação em `docs/POS_ASYNC_VOUCHER_VALIDATION_SPEC.md`.

- O backend é headless e é o único módulo que conhece JPA, Flyway e a base de dados.
- O desktop conhece apenas `contracts`, clientes HTTP e integrações locais.
- DTOs HTTP são records em `contracts`; alterações preservam compatibilidade com a versão desktop
  anterior.
- Mensagens visíveis ao utilizador usam português de Moçambique.
- UI Swing segue `docs/UI_DESIGN_SYSTEM.md` e as regras adicionais de `AGENTS.md`.
- Alterações fiscais, contabilísticas, de stock, POS, caixa ou salários exigem confirmação quando a
  regra de negócio não estiver inequivocamente documentada.

## Próximos passos

- Seleccionar uma única dívida técnica por fase e criar a respectiva SPEC quando a mudança afectar
  contrato, fluxo de negócio ou decisão duradoura.
- Executar os testes dirigidos da área alterada.
- Executar `mvn clean compile` antes de concluir alterações de código, quando possível.
- Actualizar este ficheiro apenas com trabalho activo, bloqueios e decisões ainda abertas.

## Arranque da aplicação no Windows

A regra permanente está em `.agents/rules/startup_procedure.md` e em `AGENTS.md`.

1. Encerrar instâncias anteriores de `javaw` que possam bloquear o JAR.
2. Confirmar `http://localhost:8080/actuator/health` com estado `UP`.
3. Iniciar o desktop na sessão interactiva através de `scripts/run_gui.bat` ou da tarefa agendada
   `MulticoreERP`.
4. Usar sempre `-Djava.awt.headless=false -Dspring.profiles.active=desktop`.

## Critério de fecho da fase

- Código compila e os testes dirigidos passam.
- Fronteiras entre módulos permanecem válidas.
- Contratos e mensagens respeitam as convenções.
- A documentação canónica foi actualizada apenas se houve decisão duradoura.
- O resumo da fase foi movido para `tasks/archive/`.
