# Especificação Canónica — Expansão dos Módulos Empresariais (Tier-1)
## Multicore Enterprise ERP

Este documento define a especificação arquitectural canónica para os 4 novos módulos de alta escala do Multicore ERP:
1. **Activos Fixos & Património (Fixed Assets & Depreciações PGC-NIRF / IRPC)**
2. **Produção, Transformação & Ficha Técnica (Manufacturing & Bill of Materials - BOM)**
3. **Gestão de Frotas, Combustível & Viaturas (Fleet Management)**
4. **Contratos & Facturação Recorrente (Recurring Billing & Subscriptions)**

Esta especificação segue rigorosamente as regras não negociáveis de [ARCHITECTURE.md](../ARCHITECTURE.md), [CONVENTIONS.md](../CONVENTIONS.md), [docs/MULTI_MODULE_ARCHITECTURE_SPEC.md](MULTI_MODULE_ARCHITECTURE_SPEC.md) e [docs/UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md).

---

## 1. Princípios Arquitecturais Mandatórios (SOLID & DRY)

1. **Separação Física em 3 Módulos Maven:**
   - `contracts`: Records imutáveis (DTOs), Enums e Value Objects. **Zero dependências** de Spring, JPA, Hibernate ou Swing.
   - `backend`: Lógica de domínio, transacções, entidades JPA, migrações Flyway e endpoints REST `@RestController`. **Zero importações** de Swing, AWT ou classes de interface gráfica (`gui`).
   - `desktop`: Interface Swing moderna (FlatLaf, `ModernPanel`, `UIHelper`). Comunica com o backend exclusivamente via HTTPS REST DTOs. **Zero JPA**, sem Repositories ou Services backend.

2. **SOLID Aplicado:**
   - **SRP (Single Responsibility):** Cada Service possui uma única razão para mudar. Motores de cálculo (depreciações fiscais, custo industrial de produção, consumo médio de combustível) são componentes isolados injetados nos serviços de orquestração.
   - **OCP (Open/Closed):** Motores de depreciação e estratégias de cálculo de custo são extensíveis por interface.
   - **LSP (Liskov Substitution):** Exceções de negócio herdam de `BusinessRuleException` com mensagens claras em português de Moçambique.
   - **ISP (Interface Segregation):** Repositórios JPA expõem apenas métodos de busca específicos da agregação.
   - **DIP (Dependency Inversion):** Injeção estritamente por construtor com campos `final`. Proibido `@Autowired` em campo.

3. **Multi-Tenancy & Segurança:**
   - Todas as entidades herdam de `BaseEntity` e filtram obrigatoriamente por `companyId`.
   - Autorização controlada centralmente pelo servidor via RBAC.

4. **Valores Monetários e Quantidades:**
   - Sempre `BigDecimal` com arredondamento explícito (`RoundingMode.HALF_UP`). Proibido `double` ou `float`.

---

## 2. Módulo 1: Activos Fixos & Património (`assets`)

### 2.1 Objectivo de Negócio
Gerir o ciclo de vida completo do património corpóreo e incorpóreo da empresa segundo as normas moçambicanas do PGC-NIRF e o Regulamento do IRPC:
- Cadastro e etiquetagem com código patrimonial (código de barras / QR Code).
- Alocação a colaboradores, departamentos e locais físicos.
- Cálculo automático mensal e anual das quotas de depreciação (método das quotas constantes).
- Integração directa com a Contabilidade (contas da classe 3 - Investimentos de Capital e 6.5 - Imparidades e Amortizações).
- Gestão de abates, perdas extraordinárias e alienações com apuramento de mais-valias/menos-valias fiscais.

### 2.2 Estrutura de Pacotes
- `contracts`: `mz.multicore.erp.modules.assets.dto` & `mz.multicore.erp.modules.assets.model`
  - `AssetDTO`, `CreateAssetRequest`, `AssetCategoryRateDTO`, `DepreciationScheduleDTO`, `AssetWriteOffRequest`
  - Enums: `AssetCategory` (BUILDINGS, VEHICLES, MACHINERY, IT_EQUIPMENT, FURNITURE, OTHER), `AssetStatus` (ACTIVE, DEPRECIATING, FULLY_DEPRECIATED, WRITTEN_OFF, SOLD)
- `backend`: `mz.multicore.erp.modules.assets`
  - `model`: `Asset`, `AssetDepreciationRecord`
  - `repository`: `AssetRepository`, `AssetDepreciationRecordRepository`
  - `service`: `AssetService`, `AssetDepreciationEngine`
  - `controller`: `AssetController` (`/api/assets`)
- `desktop`: `mz.multicore.erp.gui` & `mz.multicore.erp.desktop.client`
  - `AssetApiClient`, `AssetsPanel`

### 2.3 Taxas Oficiais de Depreciação (IRPC Moçambique)
| Categoria | Descrição | Taxa Anual Padrão | Vida Útil (Anos) |
| :--- | :--- | :--- | :--- |
| `BUILDINGS` | Edifícios e construções industriais | 2% - 4% | 25 a 50 |
| `VEHICLES` | Viaturas ligeiras e pesadas | 20% - 25% | 4 a 5 |
| `MACHINERY` | Equipamento industrial e máquinas | 12.5% - 20% | 5 a 8 |
| `IT_EQUIPMENT` | Computadores, servidores e periféricos | 33.33% | 3 |
| `FURNITURE` | Mobiliário de escritório e balcões | 10% - 14.28% | 7 a 10 |

---

## 3. Módulo 2: Produção & Ficha Técnica / Transformação (`manufacturing`)

### 3.1 Objectivo de Negócio
Gerir processos de transformação industrial ligeira, panificação, marcenarias e linhas de montagem:
- Fichas Técnicas / Receitas (Bill of Materials - BOM) definindo componentes, quantidades líquidas e percentagem de desperdício tolerável.
- Ordens de Produção (OP) com estados: `DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
- Execução com consumo automático de matérias-primas via FEFO/FIFO do armazém e entrada do produto acabado no armazém de destino.
- Apuramento do Custo Industrial de Produção (CIP): Matéria-prima + Mão de obra direta estimada + Custos gerais indiretos.

### 3.2 Estrutura de Pacotes
- `contracts`: `mz.multicore.erp.modules.manufacturing.dto` & `mz.multicore.erp.modules.manufacturing.model`
  - `BillOfMaterialsDTO`, `CreateBomRequest`, `BomItemDTO`, `ProductionOrderDTO`, `CreateProductionOrderRequest`, `CompleteProductionOrderRequest`
  - Enums: `ProductionOrderStatus`, `BomType`
- `backend`: `mz.multicore.erp.modules.manufacturing`
  - `model`: `BillOfMaterials`, `BomItem`, `ProductionOrder`, `ProductionOrderItem`
  - `repository`: `BillOfMaterialsRepository`, `ProductionOrderRepository`
  - `service`: `BillOfMaterialsService`, `ProductionOrderService`
  - `controller`: `ManufacturingController` (`/api/manufacturing`)
- `desktop`: `mz.multicore.erp.gui` & `mz.multicore.erp.desktop.client`
  - `ManufacturingApiClient`, `ManufacturingPanel`

---

## 4. Módulo 3: Gestão de Frotas & Viaturas (`fleet`)

### 4.1 Objectivo de Negócio
Controlar custos de transporte, consumo de combustível e conservação mecânica da frota automóvel da empresa:
- Ficha de viatura: matrícula, marca, modelo, motorista atribuído, odómetro atual, seguro e licenças de transporte.
- Registo de abastecimentos com cálculo automático de autonomia e consumo médio ($Km/L$ ou $L/100Km$) para detecção imediata de desvios e anomalias.
- Alertas de manutenção preventiva programada por quilometragem (óleo, filtros, calços de travão, pneus).
- Registo de avarias, oficinas autorizadas e histórico de despesas por veículo.

### 4.2 Estrutura de Pacotes
- `contracts`: `mz.multicore.erp.modules.fleet.dto` & `mz.multicore.erp.modules.fleet.model`
  - `VehicleDTO`, `CreateVehicleRequest`, `FuelLogDTO`, `RecordFuelLogRequest`, `VehicleMaintenanceDTO`, `ScheduleMaintenanceRequest`
  - Enums: `VehicleStatus`, `FuelType` (DIESEL, PETROL, HYBRID, ELECTRIC), `MaintenanceType`
- `backend`: `mz.multicore.erp.modules.fleet`
  - `model`: `Vehicle`, `FuelLog`, `VehicleMaintenance`
  - `repository`: `VehicleRepository`, `FuelLogRepository`, `VehicleMaintenanceRepository`
  - `service`: `VehicleService`, `FuelConsumptionEngine`, `VehicleMaintenanceService`
  - `controller`: `FleetController` (`/api/fleet`)
- `desktop`: `mz.multicore.erp.gui` & `mz.multicore.erp.desktop.client`
  - `FleetApiClient`, `FleetPanel`

---

## 5. Módulo 4: Contratos & Facturação Recorrente (`recurring`)

### 5.1 Objectivo de Negócio
Automatizar faturamento periódico para prestadores de serviços, segurança privada, limpeza, TI e telecomunicações:
- Cadastro de contratos com cliente, valor fixo, periodicidade (mensal, trimestral, anual) e dia fixo de emissão.
- Disparo de facturação em lote (`Batch Billing`) gerando faturas comerciais integradas no módulo Comercial.
- Controlo de vencimento de contratos, reajustes automáticos por taxa de inflação e alertas de renovação.

### 5.2 Estrutura de Pacotes
- `contracts`: `mz.multicore.erp.modules.recurring.dto` & `mz.multicore.erp.modules.recurring.model`
  - `RecurringContractDTO`, `CreateRecurringContractRequest`, `BatchBillingExecutionDTO`, `BatchBillingResultDTO`
  - Enums: `ContractStatus` (ACTIVE, SUSPENDED, EXPIRED, CANCELLED), `BillingFrequency` (MONTHLY, QUARTERLY, BIANNUAL, ANNUAL)
- `backend`: `mz.multicore.erp.modules.recurring`
  - `model`: `RecurringContract`, `RecurringContractItem`
  - `repository`: `RecurringContractRepository`
  - `service`: `RecurringContractService`, `RecurringBillingRunner`
  - `controller`: `RecurringContractController` (`/api/recurring-contracts`)
- `desktop`: `mz.multicore.erp.gui` & `mz.multicore.erp.desktop.client`
  - `RecurringContractApiClient`, `RecurringContractsPanel`

---

## 6. Padrões de Interface Gráfica Swing (Tier-1)

Todos os novos painéis no módulo `desktop` devem respeitar sem exceção:
1. **Cartões de KPI:** Usar exclusivamente `KpiCard.createGrid(...)` com altura uniforme obrigatória `KpiCard.STANDARD_CARD_HEIGHT = 96px`.
2. **Container Principal de Tabela:** Um único `ModernPanel(16)` contendo:
   - `BorderLayout.NORTH`: Cabeçalho construído com `UIHelper.tableCardTop(...)` (título obrigatoriamente no `CENTER` para proteção anti-colisão) e linha única de filtros (`TableFilter.bar`).
   - `BorderLayout.CENTER`: Tabela estilizada com `UIHelper.styleTable(...)`, larguras auto-ajustadas via `UIHelper.ensureHeadersFit(...)` e suporte a espreitadela rápida (`TableQuickPeekController` com tecla Espaço).
3. **Botões de Ação:** Máximo de 2-3 botões abertos no cabeçalho; ações secundárias agrupadas em `ActionMenuButton` (máximo de 5 opções por menu).
4. **Sem Emojis Unicode:** Proibido o uso de caracteres especiais Unicode (como ⭐, 🚗, 🏭); usar exclusivamente ícones vetoriais FontAwesome via `UIHelper.icon(...)` ou `UIHelper.semanticIcon(...)`.
5. **Idioma:** Todos os textos, títulos, filtros e mensagens voltados ao utilizador final redigidos em português de Moçambique (`pt-MZ`).
