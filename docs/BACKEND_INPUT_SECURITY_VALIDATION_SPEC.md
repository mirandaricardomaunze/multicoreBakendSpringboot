# SPEC — Segurança e validação de inputs no backend

## Objectivo

O desktop pode antecipar erros para melhorar a experiência, mas nunca é uma fronteira de confiança.
Todo pedido HTTP deve ser autenticado, autorizado, isolado por empresa e validado novamente no backend
antes de consultar ou alterar persistência.

## Fase 1 — Qualidade

- `/api/quality/**` é uma área explicitamente classificada para `ADMIN` e `MANAGER`.
- `SELLER` permanece sem acesso.
- O `companyId` recebido por query string ou corpo nunca pode divergir do tenant autenticado em
  `CurrentUserContext`.
- A divergência é rejeitada antes de qualquer acesso a Repository.
- `EquipmentCreateRequest` e `EquipmentAssignmentRequest` usam Jakarta Bean Validation.
- Quantidades e prazos não positivos são rejeitados; não são corrigidos silenciosamente.
- Pesquisa, paginação, IDs e textos recebidos na URL têm limites declarativos no Controller.

## Harness

- `ApiRolePolicyEndpointCoverageHarnessTest`: nenhuma rota fica sem classificação.
- `QualityEquipmentServiceTest`: isolamento de tenant e validações semânticas.
- `QualityInputValidationTest`: constraints dos DTOs.
- `MultiModuleArchitectureHarnessTest`: contratos continuam independentes e nas fronteiras correctas.

## Fases seguintes

1. Substituir corpos `Map<String, ...>` por records validados.
2. Completar validação em listas aninhadas e uploads binários.
3. Padronizar constraints de `@RequestParam` e `@PathVariable` nos restantes domínios.
4. Adicionar harness transversal que falha quando um `@RequestBody` tipado não usa `@Valid`.

## Gate transversal concluído

`BackendInputValidationCoverageHarnessTest` inspecciona todos os Controllers compilados e reprova quando:

- um corpo HTTP estruturado não possui `@Valid`;
- um Controller usa `Map` como contrato de entrada;
- a excepção explícita é `byte[]`, validado no Service por `ImagePayloadValidator`.

## Fase 3 — Validação profunda

- Picking valida ordem, terminal, notas e cada linha/quantidade/lote; o ID da rota prevalece sobre o corpo.
- Devoluções validam factura, armazém, linhas e quantidades positivas.
- Planos prestacionais rejeitam números, intervalos e montantes inválidos sem normalização silenciosa.
- Manifestos validam viatura, motorista, rota e cada guia seleccionada.
- Facturas e lançamentos contabilísticos ganharam limites e constraints numéricas adicionais.
- `NestedRequestValidationTest` comprova a propagação recursiva das constraints.

## Fase 4 — Fronteira multiempresa transversal

- `TenantRequestBoundaryGuard` compara o tenant autenticado com `companyId` de query string,
  variáveis de rota e DTOs recebidos no corpo.
- `TenantRequestBodyAdvice` aplica a verificação automaticamente a qualquer contrato que exponha
  `companyId()`, antes da execução do Controller.
- Endpoints de plataforma sem empresa activa permanecem isentos e continuam protegidos pelo papel
  `SUPERADMIN`.
- Campos JSON desconhecidos são rejeitados para evitar pedidos ambíguos e mass assignment.
- Testes de ataque cobrem troca de tenant na query, troca no corpo e tentativa de controlar o estado
  de uma factura através de campo JSON não contratado.

## Fase 5 — Idempotência e protecção contra replay no POS

- Cada checkout iniciado pelo desktop recebe uma chave estável `POS-<UUID>`, reutilizada pelos
  retries do mesmo pedido.
- A autoridade permanece no backend: a factura guarda a chave e o SHA-256 de uma representação
  canónica do operador, tenant, cliente, armazém, cotação, linhas e pagamentos.
- Repetir a mesma chave com o mesmo conteúdo devolve a factura já emitida, sem nova cobrança,
  numeração fiscal ou saída de stock.
- Reutilizar a mesma chave com conteúdo diferente é rejeitado com `BusinessRuleException` antes de
  qualquer mutação.
- O índice único `(company_id, contingency_reference)` fecha a corrida entre pedidos concorrentes;
  chaves iguais continuam isoladas por empresa.
- Pedidos antigos sem chave continuam aceites durante a transição. O desktop actualizado gera a
  chave também para vendas online; referências `CONT-*` mantêm compatibilidade offline.
- `PosContingencyHarnessTest` cobre persistência do fingerprint, replay legítimo e conflito de payload.

## Fase 6 — Recepção idempotente de transferências

- O desktop atribui `RECV-<UUID>` a cada confirmação de recepção e conserva a chave no corpo enviado.
- O backend bloqueia pessimisticamente a guia antes de verificar estado ou movimentar stock, serializando
  duas confirmações simultâneas da mesma transferência.
- A guia guarda a chave e o SHA-256 canónico das observações, linhas, quantidades recebidas e motivos.
- O mesmo pedido repetido devolve a transferência já recebida sem repetir entrada no destino ou devolução
  à origem; a mesma chave com conteúdo diferente é rejeitada antes de qualquer movimento.
- O record mantém os construtores antigos para compatibilidade; clientes anteriores continuam com a regra
  terminal existente, enquanto o desktop actualizado beneficia de replay seguro.
- `StockTransferReceiptDiscrepancyHarnessTest` cobre replay legítimo, conflito de payload, recepção integral
  e recepção com divergência.

## Fase 7 — Pagamentos posteriores idempotentes

- O desktop gera `PAY-<UUID>` no momento em que o operador confirma a cobrança de uma factura em dívida.
- O backend bloqueia pessimisticamente a factura por `id + companyId` antes de ler o saldo ou registar
  qualquer entrada, impedindo duas cobranças concorrentes sobre o mesmo saldo.
- `PaymentEntry` guarda a chave e o SHA-256 canónico do método, valor, valor entregue, referência e conta.
- Repetir chave e pedido devolve a factura já actualizada sem nova entrada financeira, `PaymentEntry` ou
  alteração de `amountPaid`; reutilizar a chave com conteúdo diferente é rejeitado.
- O construtor original de `PosPaymentRequest` foi preservado e a chave permanece opcional para clientes
  antigos; o desktop actualizado envia-a sempre neste fluxo.
- `POSServiceTest` cobre replay legítimo e conflito de payload antes da tesouraria.

## Fase 8 — Devoluções POS idempotentes

- O desktop gera `RETURN-<UUID>` para cada confirmação de devolução.
- O backend bloqueia pessimisticamente a factura antes de procurar ou criar a nota de crédito, evitando
  duas devoluções concorrentes sobre as mesmas linhas.
- A nota de crédito guarda a chave e o SHA-256 canónico do operador, tenant, factura, armazém, motivo,
  método de reembolso, conta e linhas devolvidas.
- Um replay idêntico recupera a nota de crédito e o vale já emitido, quando aplicável, sem nova aprovação,
  reposição de stock, saída de gaveta/tesouraria ou emissão de vale.
- Reutilizar a chave com payload diferente é rejeitado antes de qualquer efeito financeiro ou de stock.
- O construtor anterior de `POSReturnRequest` permanece disponível para clientes antigos.
- `PosVoucherHarnessTest` cobre replay legítimo e conflito de payload.

## Fase 9 — Recepções parciais de compras idempotentes

- Cada confirmação no diálogo de recepção recebe `PURCHASE-RECV-<UUID>`.
- O backend bloqueia pessimisticamente a encomenda por empresa antes de verificar saldos e linhas.
- Como uma encomenda admite várias recepções, cada operação fica num registo imutável
  `PurchaseOrderReceipt`; não se guarda apenas a última chave na encomenda.
- O fingerprint SHA-256 cobre linhas, quantidades boas, danificadas, em falta e observações, com ordem
  canónica por linha.
- Um replay histórico devolve o estado actual da encomenda sem nova entrada de stock ou divergência;
  reutilizar a chave com conteúdo diferente é rejeitado.
- O construtor anterior de `ReceivePurchaseOrderRequest` permanece compatível.
- `PurchaseOrderServiceTest` cobre replay legítimo e conflito antes de stock/persistência.

## Fase 10 — Quebras e perdas de stock idempotentes

- Cada confirmação no diálogo de quebra recebe `WASTE-<UUID>`; o desktop apenas cria a chave e o
  backend continua dono da validação, aprovação e movimentação de stock.
- O backend confirma o tenant autenticado antes de consultar empresa, armazém, produto ou lote.
- `StockWaste` guarda a chave e o fingerprint SHA-256 canónico da empresa, armazém, produto, lote,
  quantidade, motivo e observações.
- A chave é reservada com `saveAndFlush` antes do abatimento; o índice único por empresa impede que
  pedidos concorrentes apliquem a mesma perda duas vezes.
- Um replay idêntico devolve o registo original sem novo movimento ou pedido de aprovação; reutilizar
  a chave com dados diferentes é rejeitado antes de qualquer efeito de stock.
- O construtor anterior de `CreateStockWasteRequest` permanece disponível para compatibilidade.
- `StockWasteHarnessTest`, `AuditTrailCriticalEventsHarnessTest` e
  `MultiModuleArchitectureHarnessTest` cobrem o fluxo e as fronteiras alteradas.

## Fase 11 — Ajustes manuais de stock idempotentes

- O desktop gera `ADJUST-<UUID>` apenas quando o operador confirma uma contagem; regras e efeitos
  permanecem exclusivamente no backend.
- O fingerprint SHA-256 cobre empresa, produto, armazém, quantidade contada e motivo normalizado.
- `StockAdjustmentOperation` reserva a chave com `saveAndFlush` antes de alterar lotes, movimentos ou
  saldos, e associa o movimento resultante ao terminar.
- A restrição única `(company_id, idempotency_key)` impede que dois pedidos concorrentes apliquem a
  mesma contagem; replay idêntico devolve o movimento original e payload divergente é rejeitado.
- O construtor antigo de `CreateStockAdjustmentRequest` permanece compatível com contagens físicas e
  clientes anteriores.
- A migração `V103` cria o histórico técnico das operações sem modificar a semântica do ajuste.

## Fase 12 — Concorrência em inventários físicos

- As mutações `guardar`, `aplicar` e `cancelar` carregam a sessão com lock pessimista, incluindo linhas,
  artigos, empresa e armazém necessários à decisão.
- Duas confirmações simultâneas da mesma contagem são serializadas: apenas a primeira pode observar
  `DRAFT`; a segunda encontra o estado terminal e é rejeitada antes de criar ajustes.
- Guardar contagens não pode correr em paralelo com aplicar ou cancelar a mesma sessão, evitando linhas
  parcialmente actualizadas e decisões baseadas numa fotografia inconsistente.
- A validação do tenant continua a ocorrer imediatamente depois do lock e antes de qualquer movimento.
- `InventoryCountServiceTest` e `MultiModuleArchitectureHarnessTest` validam o fluxo e as fronteiras.

## Fase 13 — Movimentos manuais de stock idempotentes

- Registos manuais iniciados no desktop recebem `MOVEMENT-<UUID>`; o contrato anterior permanece
  disponível para integrações existentes.
- O backend calcula fingerprint SHA-256 sobre tenant, produto, armazém, quantidade, tipo, lote, série,
  descrição e validade.
- `StockMovementOperation` reserva a chave antes de criar/consumir lotes ou actualizar o saldo e guarda
  o movimento final devolvido ao cliente.
- Replay idêntico devolve o movimento original; reutilização da chave com payload diferente é rejeitada.
- A restrição única por empresa fecha a corrida entre pedidos concorrentes e a validação explícita do
  armazém impede movimentos cruzados entre empresas.
- A migração `V104` cria o histórico técnico das operações.

## Fase 14 — Integridade do saldo sob movimentos concorrentes

- Toda soma/subtracção do saldo em `InventoryService`, `StockTransferService` e
  `StockTransferTransitService` carrega o registo com `PESSIMISTIC_WRITE`.
- Movimentos diferentes e legítimos sobre o mesmo produto/armazém são serializados, evitando que uma
  gravação apague o resultado da outra (`lost update`).
- Ajustes por contagem bloqueiam o saldo antes de ler a quantidade actual e calcular a diferença.
- A versão optimista de `Stock` permanece como segunda barreira de integridade.
- `MultiUserRobustnessHarnessTest` verifica permanentemente que os três serviços usam o lock canónico.

## Fase 15 — Limites dos contratos de movimento e ajuste

- IDs de empresa, produto e armazém devem ser positivos; quantidades respeitam a precisão persistida
  de nove dígitos inteiros e três casas decimais.
- Tipo de movimento é obrigatório, normalizado para maiúsculas e limitado; lote, série, descrição e
  motivo respeitam os tamanhos máximos aceites pela persistência.
- Campos textuais opcionais são aparados e valores vazios tornam-se `null`, estabilizando fingerprints e
  evitando estados semanticamente duplicados.
- Chaves de idempotência aceitam apenas caracteres seguros (`A-Z`, `a-z`, dígitos, ponto, sublinhado,
  dois-pontos e hífen), além do limite de 60 caracteres.
- `InventoryInputValidationTest` cobre rejeições e normalização; os gates transversais de validação,
  arquitetura e arranque desktop permanecem aprovados.

## Fase 16 — Autorização e isolamento de leituras de inventário

- Leituras por armazém resolvem primeiro o armazém e confirmam que pertence à empresa autenticada,
  antes de consultar saldos, lotes ou a próxima sugestão FEFO.
- O endpoint de lotes deixou de aceder directamente ao serviço de lotes quando recebe `warehouseId`;
  passa pela fronteira de segurança de `InventoryService`.
- O histórico global de movimentos e a listagem administrativa de todos os armazéns exigem perfil
  `MANAGER` ou `ADMIN`; o vendedor conserva apenas as consultas operacionais necessárias ao POS.
- `InventoryServiceTest` cobre tentativas de leitura de armazém de outra empresa antes de qualquer
  consulta de dados; `ApiRolePolicyTest` fixa a matriz de permissões por perfil.
- `ApiRolePolicyEndpointCoverageHarnessTest`, `SecurityApiIntegrationTest`,
  `TenantAccessServiceTest` e `MultiModuleArchitectureHarnessTest` mantêm a cobertura transversal.
