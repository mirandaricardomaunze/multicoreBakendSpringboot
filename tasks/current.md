# Tarefa actual

Estado: **estabilização para produção**
Actualizado em: 2026-11-01
Histórico: [até 2026-10-31](archive/current-history-through-2026-10-31.md) ·
[até 2026-10-10](archive/current-history-through-2026-10-10.md)

Este ficheiro contém apenas o contexto necessário para continuar o trabalho actual. Resultados
concluídos devem ser movidos para `tasks/archive/` no fecho de cada fase.

## Trabalho activo

- Sem fase de código em curso. Paridade PostgreSQL e formato numérico concluídos e integrados em
  `main`; ver `docs/POSTGRES_SCHEMA_PARITY_SPEC.md` e `docs/FORMATO_NUMERICO_SPEC.md`.
- Design do ecrã POS revisto em `docs/POS_DESIGN_ECRA_SPEC.md`. A balança real continua sem driver de
  porta série: só existe simulação.
- POS antes do piloto em `docs/POS_PRE_PILOTO_SPEC.md`: simulação de pagamento móvel desligada,
  venda suspensa, cliente limpo entre vendas e desconto F5 corrigido. Faltam o operador M-Pesa/e-Mola,
  a balança e a gaveta.
- Apresentação do Painel Inicial em `docs/DASHBOARD_APRESENTACAO_SPEC.md`.
- Auditoria de faturas, notas, guias e encomendas em `docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md`, com decisões em aberto.
- Segurança antes de produção em `docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md` (SC-25 a SC-32): código
  2FA de uso único, senhas de demonstração substituídas, backups antigos e lógicos cifrados. No VPS,
  correr `scripts/validar-producao.sh` antes de receber dados reais.
  Comparação de conteúdo e restauro PostgreSQL isolado validados; ver
  `tasks/archive/2026-11-01-backup-content-verification.md`.
- Em 2026-10-31 o responsável confirmou que não existe base PostgreSQL real, por isso as migrações
  V66, V76, V89 e V96 puderam ser corrigidas no próprio ficheiro.

## Bloqueios antes de produção

- Protecção da branch `main` a exigir os checks `build` e `postgres-parity`.
- XSD oficial da AT-MZ para validar o SAF-T (`docs/FISCAL_SAFT_VALIDACAO_SPEC.md`).
- Homologação por jurista da conformidade legal e do contrato de licença.
- Execução assinada de `docs/GUIA_ACEITACAO_LOJA.md` numa loja piloto.
- Carga medida só na venda POS e numa máquina de desenvolvimento (`docs/CARGA_POSTGRES_SPEC.md`):
  repetir no hardware final antes de anunciar capacidade.
- Deploy real no VPS por validar.

## Decisões permanentes relevantes

35. **Painel Executivo com Tema e Modais Próprios:**
    - O painel abre no tema da aplicação e acompanha a troca de tema sem fechar.
    - Os modais (Metas, Vendedores) abrem sempre no tema actual do painel, na moldura `ExecutiveTvDialog`.
    - SPEC em `docs/SALA_DE_REUNIOES_TV_SPEC.md` §7 (ET-01 a ET-07); harness em `ExecutiveThemeConsistencyHarnessTest`.

34. **Circuito da Encomenda Validado no Backend:**
    - A faturação directa não aceita pedidos de separação. A conferência exige todas as linhas.
      A folha A4 é `POST`.
    - Faturar e aprovar uma guia respeitam as reservas de outros pedidos. Os estados estão no enum
      `OrderStatus`. Quem pede uma aprovação não a aprova (excepto ADMIN).
    - SPEC em `docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md`, "Circuito da encomenda". Harness em
      `OrderStatusTest`, `OrderStockAvailabilityTest`, `DeliveryGuideApprovalReservationTest`,
      `OrderSearchQueryHarnessTest` e `OrderPickingConferenciaHarnessTest`.
    - POS, fatura directa e cotação convertida também respeitam reservas (`SaleStockIssuer`).
    - Crédito: uma encomenda A4 aprovada por gerente ou ADMIN fatura mesmo acima do limite; a Green Lane não.
    - Carga POS repetida em 2026-11-07 (`docs/CARGA_POSTGRES_SPEC.md`): 12.252 vendas, 0 erros. Encontrou e
      corrigiu as corridas do "Consumidor Final" e da série FT na primeira venda do ano (CG-06, CG-07).

33. **Acções Sobre a Linha Seleccionada:**
    - Acções de tabela usam `TableFilter.selectedModelRow(table)`. Com filtro ou da 2.ª página em
      diante, 16 acções agiam sobre outro registo (utilizadores, contratos, contagens, POS, backups).
    - SPEC e harness em `docs/SELECCAO_LINHA_MODELO_SPEC.md` e `SelectedRowModelIndexHarnessTest`.

32. **Números Iguais ao Ecrã Fora da Tabela:**
    - Export PDF e painel de espreitar usam `CellText` (texto do renderer); PDFs e mensagens do
      backend usam `DisplayNumbers` em `contracts`. Nunca `toPlainString()` para pessoas.
    - O painel de espreitar recebe a linha do modelo: 26 ecrãs convertiam-na outra vez e, com filtro,
      mostravam outra linha.
    - SPEC e harness em `docs/FORMATO_NUMERICO_SPEC.md` (FN-07 a FN-11).

31. **Histórico POS com Entregue, Troco e Cliente Não Cadastrado:**
    - A venda já gravava tudo; o histórico mostrava "Consumidor Final" e não tinha o pagamento.
    - Comprador via `InvoiceDTO.buyerName()`; entregue e troco via `GET /api/pos/sales/payments`.
    - SPEC e harness em `docs/POS_HISTORICO_PAGAMENTO_CLIENTE_SPEC.md`, `PosSalePaymentQueryServiceTest`
      e `PosSalesHistoryPaymentHarnessTest`.

30. **Botão Actualizar Unificado:**
    - Todo o "Actualizar" vem de `UIHelper.createRefreshButton(...)`: mesma cor, ícone, dica e nome acessível.
    - Nos cards com `tableCardTop`, fica no cabeçalho como primeira acção, como em Pedidos.
    - SPEC e harness em `docs/BOTAO_ACTUALIZAR_UNIFICADO_SPEC.md` e `RefreshButtonUnificationHarnessTest`.

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
