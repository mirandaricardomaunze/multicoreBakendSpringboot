# Harness de Testes: Trilha de Auditoria Centralizada e Imutável (HARNESS-AUD-001)

## 1. Objectivo

Este harness audita e homologa a conformidade da especificação [AUDIT_TRAIL_CRITICAL_EVENTS_SPEC.md](AUDIT_TRAIL_CRITICAL_EVENTS_SPEC.md) (`SPEC-AUD-001`), exercitando o registo rigoroso e imutável de eventos críticos de inventário, crédito de clientes, segurança de utilizadores e anulações documentais.

---

## 2. Cenários de Teste

| ID | Cenário | Ação de Teste | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **AUD-01** | Auditoria de Registo de Quebra de Stock | Submissão de `registerWaste` com produto, armazém, quantidade e motivo. | Evento `STOCK_WASTE_REGISTER` gravado em `AuditLog` contendo produto, armazém, quantidade e custo financeiro. |
| **AUD-02** | Auditoria de Aprovação / Rejeição de Quebra | Invocação de `approveWaste` por supervisor/gerente. | Evento `STOCK_WASTE_APPROVE` (ou `STOCK_WASTE_REJECT`) persistido com ID da quebra, utilizador e notas. |
| **AUD-03** | Auditoria de Alteração de Limite de Crédito | Atualização do limite de crédito de cliente em `updateClient`. | Evento `CLIENT_CREDIT_LIMIT_CHANGE` gravado com valor anterior e novo valor em MT. |
| **AUD-04** | Auditoria de Mutação de Segurança e Permissões | Criação de utilizador, alteração de papel (`updateCompanyRole`), definição de PIN (`setManagerPin`), reset de password (`resetPassword`) e toggle de status (`toggleUserStatus`). | Eventos correspondentes (`USER_CREATE`, `USER_ROLE_CHANGE`, `USER_PIN_SET`, `USER_PASSWORD_RESET`, `USER_STATUS_CHANGE`) registados na trilha da empresa. |
| **AUD-05** | Auditoria de Anulação de Documentos | Anulação de faturas (`cancelInvoice`), recibos (`cancelReceipt`), encomendas (`cancelOrder`) e cotações (`cancelQuotation`). | Eventos `INVOICE_CANCEL`, `RECEIPT_CANCEL`, `ORDER_CANCEL`, `QUOTATION_CANCEL` gravados contendo os respetivos números e motivos obrigatórios. |
| **AUD-06** | Imutabilidade e Consulta por Tenant | Leitura via `AuditController` / `AuditLogService`. | Registos escopados pela empresa activa; sem endpoints de alteração ou eliminação. |

---

## 3. Execução Automatizada

Classe de teste canónica:
- [AuditTrailCriticalEventsHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/modules/audit/AuditTrailCriticalEventsHarnessTest.java)

Comando de teste:
```powershell
mvn test -pl backend -Dtest=AuditTrailCriticalEventsHarnessTest
```

Critério de aprovação:
- 100% de aprovação (6/6 cenários aprovados com 0 falhas).
