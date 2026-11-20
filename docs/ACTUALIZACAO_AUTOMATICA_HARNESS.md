# Harness — Actualização automática dos ecrãs

> Cenários que provam [ACTUALIZACAO_AUTOMATICA_SPEC.md](ACTUALIZACAO_AUTOMATICA_SPEC.md).

**Última actualização:** 2026-11-20

| ID | Garantia | Teste |
|---|---|---|
| AA-01 | Contador por empresa sobe só nessa empresa; `epoch` fixo durante a vida do servidor (§3) | `ChangeVersionRegistryTest` |
| AA-02 | Escrita 2xx sobe o contador; `GET` e escrita com erro não sobem (§3) | `ChangeTrackingHttpIntegrationTest` |
| AA-03 | `/api/changes` devolve só a empresa activa; outra empresa não muda; exige token (§3) | `ChangeTrackingHttpIntegrationTest` |
| AA-04 | Rota `/api/changes` classificada e aberta a qualquer perfil da empresa (§3) | `ApiRolePolicyEndpointCoverageHarnessTest` · `ApiRolePolicyTest` |
| AA-05 | Alteração = versão sobe ou epoch muda; 1.ª leitura e troca de empresa só memorizam (§4.1) | `AutoRefreshPolicyTest` |
| AA-06 | Ocupado (diálogo, tecla < 3 s, célula em edição, minimizada, a carregar) adia; livre recarrega (§4.3) | `AutoRefreshPolicyTest` |
| AA-07 | Todo o botão Actualizar fica registado; só os visíveis e activos são executados (§4.2) | `RefreshRegistryTest` |
| AA-08 | Página e linha seleccionada repostas pela chave depois de os dados mudarem; linha apagada fica sem selecção (§4.4) | `TableStateKeeperTest` |
| AA-09 | O botão Actualizar continua em todos os ecrãs que o tinham (BAU-01) | `RefreshButtonUnificationHarnessTest` |
