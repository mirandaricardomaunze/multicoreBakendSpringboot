# Performance de contas a receber — 2026-10-12

## Implementação

- Skill `swing-springboot-desktop-client`: optimização restrita ao backend, sem alterar contratos
  HTTP nem colocar regras no desktop.
- `ReceivablesService.outstandingTotalFor` deixa de materializar todas as facturas da empresa:
  uma consulta agrega dívida por empresa/cliente/estados cobráveis, mantendo saldo mínimo zero.
- `getAging` consulta apenas documentos cobráveis com saldo positivo e faz fetch do cliente na
  mesma consulta. Preserva escalões, datas e totais; ausência de empresa é recusada antes da query.
- V90 corrigida com autorização contextual: utilizador confirmou que só existe no projecto local.
  `invoice_date` e `order_date` substituídos por `created_at`; o índice de produtos passa à tabela
  `product_companies`; acrescentado índice de crédito `(company_id, client_id, status)`.
- SPEC: `docs/ROBUSTEZ_MULTIUTILIZADOR_SPEC.md`, secção de consultas de contas a receber.

## Evidência

- `ReceivablesServiceTest`: 14 testes aprovados.
- `ReceivablesQueryHarnessTest`: 4 testes aprovados em H2 isolado, não na base da aplicação.
  Agregado equivalente ao domínio em todos os estados, isolamento de empresa/cliente, documentos
  pagos/excedidos excluídos, ausência de dívida devolve zero. Agregado executa um statement e
  carrega zero entidades; lista com 12 clientes executa um statement sem fetch adicional.
  V90 aplicada e reaplicada ao schema JPA, verificando as colunas do índice de crédito.
- `MultiModuleArchitectureHarnessTest` aprovado; `mvn -q compile` aprovado nos três módulos.
- `mvn -q clean compile` bloqueado na remoção do JAR backend em uso. Aplicação não interrompida.

## Limites e continuidade

- Não houve empacotamento, reinício ou deploy; a aplicação aberta ainda não recebeu estas mudanças.
- O perfil de desenvolvimento desactiva Flyway. Validar a cadeia completa de migrations e planos
  de execução em PostgreSQL isolado antes de promover V90; não aplicar manualmente à BD real.
- Sem ensaio de carga, não há números de latência nem capacidade multiutilizador certificados.
- O relatório de antiguidade ainda cresce com a quantidade de documentos em aberto; a melhoria
  remove histórico liquidado e N+1, não transforma o relatório inteiro num agregado SQL.
- Continuar paginação com filtros globais/exportação consistente, idempotência e ensaios reais
  de concorrência conforme a SPEC. Locks parciais e testes de estrutura não provam ciclo completo.
