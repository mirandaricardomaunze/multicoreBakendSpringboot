# Especificação Canónica: Módulo de Qualidade, Equipamentos & Segurança dos Trabalhadores (QST / HST)

## 1. Visão Geral e Objectivos

O Módulo de Qualidade, Equipamentos e Segurança dos Trabalhadores gerencia o ciclo completo de conformidade, atribuição e rastreabilidade de equipamentos de proteção individual (EPIs), instrumentos técnicos e ferramentas de trabalho aos colaboradores da empresa, garantindo:
1. **Cadastro Padronizado de Equipamentos:** Cada equipamento possui código, categoria, número de série/modelo, fornecedor e prazo de validade/renovação pré-definido (em dias ou meses).
2. **Atribuição Direta a Trabalhadores do RH:** Reutiliza os colaboradores já cadastrados no módulo de Recursos Humanos (`Employee`), calculando automaticamente a data de expiração com base no prazo do equipamento e na data de entrega.
3. **Detecção e Agrupamento Automático de Colaboradores Expirados:** O sistema identifica em tempo real todos os colaboradores com equipamentos cuja validade já expirou ou está a expirar (dentro da janela de alerta preventivo).
4. **Notificações Pró-activas no Sistema:** Integração direta com o motor de alertas e com o sino do cabeçalho executivo (`NotificationFeed`), permitindo ação preventiva sem necessidade de consultas manuais.
5. **Paginação Executiva de Alta Performance:** Listagens de equipamentos e atribuições servidas por página através do componente corporativo `TablePager` com `PageResponse`, espelhando o padrão canónico de pesquisa de encomendas do sistema.

---

## 2. Invariantes de Domínio e Regras de Negócio

1. **Cálculo de Expiração:**
   - $\text{Data de Expiração} = \text{Data de Entrega} + \text{Prazo de Validade}$ (em dias ou meses).
   - Se o prazo de validade do equipamento for alterado no cadastro, as atribuições já ativas preservam a validade calculada na entrega, mantendo a integridade histórica.
2. **Estados da Atribuição (`AssignmentStatus`):**
   - `ACTIVE`: Atribuição em vigor, equipamento em uso e dentro do prazo de validade.
   - `EXPIRING_SOON`: Equipamento ativo, cuja data de expiração cai dentro da janela de alerta preventivo (padrão: 15 a 30 dias).
   - `EXPIRED`: Data atual posterior à data de expiração ($\text{hoje} > \text{dataExpiração}$).
   - `RENEWED`: Equipamento substituído/renovado por nova atribuição.
   - `RETURNED`: Equipamento devolvido ao almoxarifado/stock de proteção.
   - `DISCARDED`: Equipamento danificado ou descartado por motivo de segurança.
3. **Associação com Recursos Humanos:**
   - Todo registo de atribuição deve referenciar um `Employee` existente da empresa atual (`company_id`).
   - Se um colaborador for desvinculado (rescisão/demissão), o sistema sinaliza os equipamentos pendentes de devolução.
4. **Isolamento Multi-Empresa (`tenant`):**
   - Todos os registos (`Equipment`, `WorkerEquipmentAssignment`) possuem campo obrigatório `company_id`.
   - Nenhuma consulta ou mutação pode aceder a dados de outra empresa.

---

## 3. Modelo de Dados e Entidades JPA

### 3.1 `Equipment` (`quality_equipments`)
- `id`: `Long` (PK)
- `company_id`: `Long` (obrigatório, indexado)
- `code`: `String` (código único por empresa, ex.: `EPI-001`, `CAL-005`)
- `name`: `String` (nome descritivo do equipamento/instrumento)
- `category`: `EquipmentCategory` (`PPE_PROTECTION`, `CALIBRATION_TOOL`, `SAFETY_DEVICE`, `WORK_TOOL`)
- `period_value`: `int` (valor numérico do prazo, ex.: `6`, `12`, `30`)
- `period_unit`: `PeriodUnit` (`DAYS`, `MONTHS`)
- `warning_days_before`: `int` (dias de antecedência para alerta preventivo, default: `15`)
- `active`: `boolean` (ativo para novas atribuições)
- `description`: `String` (instruções de uso ou normas técnicas)

### 3.2 `WorkerEquipmentAssignment` (`quality_worker_assignments`)
- `id`: `Long` (PK)
- `company_id`: `Long` (obrigatório, indexado)
- `equipment_id`: `Equipment` (`@ManyToOne`, lazy)
- `employee_id`: `Employee` (`@ManyToOne`, lazy)
- `assigned_date`: `LocalDate` (data de entrega)
- `expiry_date`: `LocalDate` (data calculada de expiração)
- `quantity`: `int` (quantidade entregue, padrão: 1)
- `size_or_spec`: `String` (tamanho/calibre, ex.: `42`, `L`, `0-100 bar`)
- `serial_number`: `String` (opcional para ferramentas com número de série)
- `status`: `AssignmentStatus` (`ACTIVE`, `EXPIRING_SOON`, `EXPIRED`, `RENEWED`, `RETURNED`)
- `notes`: `String` (observações da entrega)

---

## 4. Contratos de API (`contracts`)

Os endpoints REST residem em `/api/quality/**`:
- `GET /api/quality/equipments?companyId=X&page=0&size=25&search=Y` -> `PageResponse<EquipmentDTO>`
- `POST /api/quality/equipments` -> `EquipmentDTO`
- `PUT /api/quality/equipments/{id}` -> `EquipmentDTO`
- `GET /api/quality/assignments?companyId=X&page=0&size=25&status=Y&employeeId=Z` -> `PageResponse<WorkerEquipmentAssignmentDTO>`
- `POST /api/quality/assignments` -> `WorkerEquipmentAssignmentDTO`
- `POST /api/quality/assignments/{id}/renew` -> `WorkerEquipmentAssignmentDTO`
- `POST /api/quality/assignments/{id}/return` -> `void`
- `GET /api/quality/expired-workers?companyId=X` -> `List<ExpiredWorkerAlertDTO>`
- `GET /api/quality/kpis?companyId=X` -> `QualityKpiDTO`

---

## 5. Interface Gráfica Swing (`QualityPanel`)

- **Componente Principal:** `QualityPanel` adicionado ao `MainFrame`.
- **Top KPIs (`KpiCard.createGrid(4, 8, 8)`):**
  1. `Equipamentos Atribuídos` (Azul)
  2. `A Expirar (Próximos Dias)` (Laranja Alerta)
  3. `Validade Expirada (Colaboradores)` (Vermelho Urgente)
  4. `Equipamentos Cadastrados` (Verde)
- **Abas Corporativas (`JTabbedPane` com `UIHelper.styleTabbedPaneMulticore`):**
  - **Aba 1: Atribuições a Trabalhadores:** Tabela com paginação remota `TablePager`, filtros por colaborador/equipamento/estado, botão `[+ Atribuir Equipamento]`.
  - **Aba 2: Radar de Colaboradores Expirados:** Tabela automática focada exclusivamente em quem tem itens vencidos, com botão `[Renovar Equipamento]` e acção de contacto/termo.
  - **Aba 3: Cadastro de Equipamentos:** Grelha de equipamentos, tipos e prazos, com formulário de cadastro/edição.
- **Modais Canónicos:**
  - `QualityAssignmentDialog` e `EquipmentFormDialog` usam `UIHelper.applyStandardModalBounds(this)` com altura máx. de 610px e botões de ação sempre visíveis.

---

## 6. Seletores de Data Interativos (DatePicker) & Impressão

### 6.1 Seletor de Data (`DateField`)
- O seletor de calendário corporativo (`DateField`) com botão popup trailing e atalho F4 / Alt+Down é obrigatório em todas as entradas de data:
  - No diálogo de atribuição de equipamento (`QualityAssignmentDialog`): calcula e pré-visualiza a expiração em tempo real.
  - No diálogo de renovação (`QualityRenewalDialog`): permite ao operador definir a nova data de entrega/vigência.

### 6.2 Impressão e Exportação de Listas e Documentos
- **Termo Oficial de Entrega e Responsabilidade de EPI:**
  - Renderizado pelo backend através de `QualityEquipmentPrintService` (`GET /api/quality/assignments/{id}/delivery-sheet`).
  - Inclui cabeçalho oficial da empresa (`CompanyHeaderRenderer`), identificação completa do trabalhador, tabela de especificações do equipamento e vigência, cláusulas de responsabilidade e segurança no trabalho (HST) e assinaturas (`SignatureBlockRenderer`).
  - Pode ser emitido automaticamente após gravação ou a partir das linhas de atribuição / radar.
- **Exportação de Listagens em PDF:**
  - Exportação oficial via `TableExportAction.export` integrada com `PrintApiClient` para as tabelas de Atribuições, Radar de Validades e Catálogo Geral de Equipamentos.

