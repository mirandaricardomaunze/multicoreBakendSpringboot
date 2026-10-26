# Especificação Técnica: Decomposição Modular do CRMService (Fase 104)

## 1. Contexto e Diagnóstico
O `CRMService.java` acumulava 474 linhas de código no backend (`mz.multicore.erp.modules.crm.service`), centralizando:
- Ciclo de vida e triagem de pedidos de suporte (`SupportTicket`), validação de estados, motivo de anulação e checagem de folhas de obra ativas.
- Gestão completa de folhas de obra técnicas (`WorkSheet`), edição com validações de fecho, anulação com reversão de estado do ticket e facturação direta (`billWorkSheet`) via `ComercialService`.
- Gestão do catálogo interno de produtos de suporte (`SERV-TEC` para mão de obra com tarifa horária e `PECAS-SUP` para peças com quantidade=valor), garantindo a criação sob medida sem controlo de stock para evitar falhas no FEFO.
- Configuração de tarifas horárias (`CrmSettingsDTO`), consultas analíticas e opções de enums para a interface desktop.

## 2. Decisão Arquitetural
Segregou-se a lógica em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`SupportTicketLifecycleService.java`** (~140 linhas $\le 160$):
   - Criação (`createTicket`), triagem e atribuição técnica (`updateTicket`).
   - Transição de estado com regras de negócio (`changeTicketStatus`), impedindo anulação de pedidos com folhas de obra abertas e limpando/carimbando fecho.
   - Parsing seguro de `TicketStatus` e `TicketPriority` com mensagens em pt-MZ.
   - Projeção DTO de tickets (`toDTO`).

2. **`WorkSheetService.java`** (~260 linhas $\le 265$):
   - Registo de trabalho técnico (`createWorkSheet`), fechando automaticamente o ticket associado.
   - Atualização (`updateWorkSheet`) e anulação com preservação de histórico (`voidWorkSheet`), reabrindo o ticket se não restar trabalho vivo.
   - Facturação comercial (`billWorkSheet`), gerando linhas de fatura com horas em `BigDecimal` e valor de peças.
   - Resolução e auto-criação de produtos de catálogo interno sem controlo de stock (`internalProduct`, `labourProduct`, `partsProduct`).
   - Projeção DTO de folhas de obra (`toDTO`).

3. **`CRMQueryService.java`** (~75 linhas $\le 95$):
   - Configurações corporativas de tarifa de assistência (`getSettings`, `updateSettings`) protegidas por `PermissionGuard.requireManagerOrAdmin`.
   - Listagens ordenadas de tickets e folhas de obra por tenant (`getAllTickets`, `getAllWorkSheets`, `getWorkSheet`).
   - Fornecimento de opções legíveis de prioridade e estado para os seletores do desktop.

4. **`CRMService.java`** (~145 linhas $\le 150$):
   - Fachada enxuta que orquestra as chamadas delegando aos 3 serviços.
   - Construtor canónico de 3 colaboradores (`@Autowired`) e construtor retrocompatível de 7 dependências preservado para testes legados.

## 3. Conformidade
- Limites físicos estritos de linhas rigorosamente cumpridos.
- 100% retrocompatível com `CRMServiceTest` e `TenantIsolationIntegrationTest`.
- Zero uso de `BigDecimal.ZERO.equals` (uso de `compareTo` e `signum`).
- Mensagens ao utilizador em português de Moçambique.
