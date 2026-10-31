# Especificação Técnica: Decomposição Modular do QualityEquipmentService (Fase 108)

## 1. Contexto e Diagnóstico
O `QualityEquipmentService.java` acumulava 426 linhas de código no backend (`mz.multicore.erp.modules.quality.service`), acumulando:
- Cadastro e gestão de catálogo de equipamentos e EPIs (`createEquipment`, `updateEquipment`, `getEquipment`, `listActiveEquipments`, `searchEquipments`).
- Regras de validação de prazos de validade/calibração e unidades periódicas (`DAYS`, `MONTHS`, `YEARS`).
- Atribuição direta a colaboradores da empresa (`assignEquipment`), cálculo automático da data de expiração, renovações de equipamentos (`renewAssignment`), registo de devoluções com notas (`returnAssignment`) e pesquisas paginadas (`searchAssignments`).
- Radar de vigilância de colaboradores com EPIs/equipamentos expirados ou a expirar (`getExpiredWorkerAlerts`), consolidação por trabalhador e apuração de indicadores-chave de desempenho (`QualityKpiDTO`).

## 2. Decisão Arquitetural
A decomposição segregou as responsabilidades em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`EquipmentCatalogService.java`** (170 linhas $\le 180$):
   - Cadastro e atualização de equipamentos com validação de unicidade de código (`code`) e regras de prazo positivo.
   - Consultas ativas, detalhe individual e pesquisa paginada com ordenação.
   - Mapeamento determinístico para `EquipmentDTO`.

2. **`WorkerEquipmentAssignmentService.java`** (193 linhas $\le 205$):
   - Atribuição de equipamentos a colaboradores com cálculo da data de expiração a partir do prazo e unidade periódica.
   - Ciclo de renovação periódica (`renewAssignment`) marcando a anterior como `RENEWED` e gerando nova atribuição atómica.
   - Devolução de equipamentos (`returnAssignment`) com anotações de motivo e conservação.
   - Pesquisa paginada com filtros por estado (`AssignmentStatus`), colaborador e texto livre.
   - Mapeamento determinístico para `WorkerEquipmentAssignmentDTO`.

3. **`QualityMonitoringService.java`** (104 linhas $\le 120$):
   - Radar analítico de vigilância de trabalhadores com equipamentos expirados (`EXPIRED`) ou em risco iminente (`EXPIRING_SOON`).
   - Apuração em tempo real de KPIs de qualidade (`QualityKpiDTO`): equipamentos ativos, a expirar em 30 dias, expirados, colaboradores afetados e total cadastrado.

4. **`QualityEquipmentService.java`** (130 linhas $\le 140$):
   - Fachada executiva que orquestra as chamadas delegando aos 3 serviços especializados.
   - Helper centralizado de validação de tenant `resolveCompanyId`.
   - Construtor canónico `@Autowired` de 3 colaboradores e construtor retrocompatível de 4 dependências preservado para compatibilidade integral com `QualityEquipmentServiceTest`.

## 3. Conformidade
- Redução de 426 para 130 linhas no orquestrador (redução de 296 linhas / 69.5%).
- 100% retrocompatível com `QualityEquipmentServiceTest`.
- Zero uso de `BigDecimal.ZERO.equals`.
- Mensagens ao utilizador em português de Moçambique.
