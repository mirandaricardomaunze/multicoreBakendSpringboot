# SPEC-POS-PASSAGEM-TURNO-001 — Passagem de Turno e Reconciliação Parcial no POS

## 1. Contexto e Necessidade do Negócio

No ambiente comercial de retalho e atendimento contínuo ao balcão, a operação da caixa registadora estende-se frequentemente por múltiplos turnos de trabalho ao longo do mesmo dia (ex.: turno da manhã das 08h às 14h, turno da tarde das 14h às 21h).

### Problemas Anteriores:
1. **Sessão Monolítica de Caixa por Operador:** O modelo original associava estritamente uma única sessão a um único operador. Para mudar de operador, era mandatório fechar a sessão, emitir o fecho Z, abrir uma nova sessão com novo fundo de caixa, fragmentando a prestação de contas diária.
2. **Sem Rastreabilidade de Handover:** Não havia registo formal auditável de quem entregou a gaveta a quem, nem de quanto dinheiro físico estava presente no momento exacto da transição de responsabilidade.
3. **Z-Report Desintegrado:** Relatórios dispersos em múltiplos Z diários por cada mini-sessão, dificultando o fecho de dia e a conciliação contabilística/bancária da loja.

### Solução Arquitetural:
Introdução do conceito de **Passagem de Turno com Reconciliação Parcial (*Shift Reconciliation*)** dentro da mesma sessão de caixa diária (`TillSession`):
- A sessão de caixa diária permanece `OPEN` durante o handover, sem interrupção de serviço.
- O operador que sai realiza a contagem física (cega ou discriminada por notas e moedas MZN).
- O sistema calcula o saldo esperado até aquele instante e regista formalmente o handover (`ShiftReconciliation`).
- A responsabilidade da sessão é atomicamente transferida para o operador de entrada (`till_sessions.current_operator`).
- O operador entrante continua as vendas na mesma sessão de caixa.
- No fecho final do dia (Z-Report), todas as reconciliações e transições de turno constam discriminadas no relatório e documento PDF.

---

## 2. Requisitos Funcionais

### RF-01: Passagem de Turno Atómica (`POST /api/pos/sessions/{sessionId}/shift-handover`)
- **Pré-condições:** Sessão deve estar `OPEN` e pertencer à empresa do utilizador autenticado. O operador de saída deve corresponder ao operador actualmente responsável pela sessão (`current_operator` ou, se nulo, `operator`).
- **Validações:**
  - Operador de entrada não pode ser nulo, vazio nem igual ao operador de saída.
  - Valor contado deve ser não-nulo e não-negativo.
  - Se houver divergência entre o valor contado e o saldo esperado no momento (`difference != 0`), exige privilégio de gerente/administrador (`PermissionGuard.requireManagerOrAdmin`).
- **Efeitos:**
  - Cria e persiste registo `ShiftReconciliation` contendo: operador de saída, operador de entrada, timestamp, saldo esperado, saldo contado, diferença, JSON de notas/moedas e observações.
  - Atualiza `till_sessions.current_operator` com o operador de entrada.
  - Registo em log de auditoria: `POS_SHIFT_HANDOVER`.
  - Retorna `ShiftReconciliationDTO`.

### RF-02: Resolução Atómica de Operador Activo (`POSService.getActiveSession`)
- Garante que a consulta de caixa aberta encontra a sessão correcta para o operador que está no posto:
  - Condição SQL: `s.status = 'OPEN' AND s.company.id = :companyId AND ((s.currentOperator IS NOT NULL AND s.currentOperator = :operator) OR (s.currentOperator IS NULL AND s.operator = :operator))`.
  - Se o operador A passou a caixa ao operador B, o operador B encontra a sessão como sua, e o operador A deixa de ser o operador activo daquela caixa.

### RF-03: Consulta de Reconciliações de Turno (`GET /api/pos/sessions/{sessionId}/shift-reconciliations`)
- Lista todas as reconciliações parciais da sessão ordenadas cronologicamente por `reconciledAt ASC`.

### RF-04: Consolidação no Z-Report e PDF
- `PosZReportDTO` enriquecido com `currentOperator` e lista de `shiftReconciliations`.
- `POSZReportPrintService` inclui secção detalhada de "Passagem de Turno" na impressão do fecho Z em PDF quando existiram transições.

### RF-05: Interface Gráfica Desktop Ergonomicamente Segura
- Botão "Passar Turno" (`fas-people-arrows`) visível na barra de ações quando a caixa está aberta.
- Modal `PosShiftHandoverDialog` (540 linhas < 1000):
  - Seleção de operador de entrada.
  - Alternância entre contagem discriminada (todas as notas 1000 a 20 MT e moedas 10 a 0.50 MT) e valor total directo.
  - Cálculo automático e em tempo real de subtotais e totais contados.
  - Validação inline e confirmação não-bloqueante com feedback visual por cores semânticas (`APPROVED_GREEN`, `PENDING_YELLOW`, `REJECTED_RED`).

---

## 3. Modelo de Dados e Migração Flyway

Tabela criada na migração `V76__shift_reconciliations.sql`:
```sql
CREATE TABLE IF NOT EXISTS shift_reconciliations (
    id BIGSERIAL PRIMARY KEY,
    till_session_id BIGINT NOT NULL REFERENCES till_sessions(id),
    outgoing_operator VARCHAR(100) NOT NULL,
    incoming_operator VARCHAR(100) NOT NULL,
    reconciled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expected_cash DECIMAL(14,2) NOT NULL,
    counted_cash DECIMAL(14,2) NOT NULL,
    difference DECIMAL(14,2) NOT NULL,
    cash_breakdown_json TEXT,
    notes VARCHAR(500),
    created_by VARCHAR(100) NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id)
);

CREATE INDEX IF NOT EXISTS idx_shift_recon_session ON shift_reconciliations(till_session_id);
CREATE INDEX IF NOT EXISTS idx_shift_recon_company ON shift_reconciliations(company_id);

ALTER TABLE till_sessions ADD COLUMN IF NOT EXISTS current_operator VARCHAR(100);
```

---

## 4. Fronteiras de Módulos (Maven Multi-Module)

- `contracts`:
  - `ShiftHandoverRequest` (record imutável)
  - `ShiftReconciliationDTO` (record imutável)
  - `TillSessionDTO`: enriquecido com `currentOperator` e construtores retrocompatíveis.
  - `PosZReportDTO`: enriquecido com `currentOperator`, `shiftReconciliations` e construtores retrocompatíveis.
- `backend`:
  - `ShiftReconciliation` (entidade JPA)
  - `ShiftReconciliationRepository` (Spring Data JPA)
  - `TillSessionRepository.findActiveSessionForOperator`
  - `POSService.performShiftHandover`, `getShiftReconciliations`, `buildZReport`
  - `POSController`: endpoints REST
  - `POSZReportPrintService`: renderização PDF das transições de turno
- `desktop`:
  - `POSApiClient.performShiftHandover`, `getShiftReconciliations`
  - `PosShiftHandoverDialog` (modal moderno)
  - `PosCashSessionActions.shiftHandover`
  - `POSPanel`: botão "Passar Turno", indicador de operador actual no banner, painel em 990 linhas (< 1000).
