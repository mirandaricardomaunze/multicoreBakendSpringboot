# Especificação: Trilha de Auditoria Centralizada e Imutável de Eventos Críticos (SPEC-AUD-001)

## 1. Visão Geral e Princípios Fundamentais

Esta especificação define o padrão canónico de **Trilha de Auditoria (*Audit Trail & Forensics*)** do Multicore ERP, assegurando conformidade estrita com os requisitos de governação corporativa, transparência fiscal e os princípios **SOLID** e **DRY**.

Nenhuma ação com impacto financeiro, tributário, de inventário ou de segurança de privilégios pode ocorrer sem registo correspondente na trilha de auditoria:
1. **Imutabilidade Absoluta:** Registos de auditoria são do tipo *append-only*. Não existem operações de `UPDATE` ou `DELETE` para a entidade `AuditLog`.
2. **Rastreabilidade Completa:** Todo o evento de auditoria grava:
   - Data e hora precisas (`event_time`).
   - Identificador do operador responsável (`username`).
   - Identificador da empresa / tenant (`company_id`).
   - Código semântico da ação (`action`).
   - Detalhe explicativo com valores anteriores e novos (`details`).
   - Endereço IP de origem (`ip_address`).
3. **Fronteiras Auditadas Obrigatoriamente:**
   - **Gestão de Stock:** Registo, aprovação e rejeição de quebras de inventário (`STOCK_WASTE_REGISTER`, `STOCK_WASTE_APPROVE`, `STOCK_WASTE_REJECT`).
   - **Gestão de Crédito:** Alteração de limites de crédito de clientes (`CLIENT_CREDIT_LIMIT_CHANGE`).
   - **Segurança de Utilizadores:** Criação, alteração de permissões/papéis, definição de PIN de gerente, redefinição de palavras-passe e ativação/desativação de contas (`USER_CREATE`, `USER_ROLE_CHANGE`, `USER_PIN_SET`, `USER_PASSWORD_RESET`, `USER_STATUS_CHANGE`).
   - **Anulações Documentais:** Faturas, recibos, encomendas e cotações (`INVOICE_CANCEL`, `RECEIPT_CANCEL`, `ORDER_CANCEL`, `QUOTATION_CANCEL`).

---

## 2. Catálogo Canónico de Ações de Auditoria

| Domínio | Ação de Auditoria | Descrição do Detalhe Gravado |
| :--- | :--- | :--- |
| **Inventário** | `STOCK_WASTE_REGISTER` | Produto, armazém, quantidade, custo total (MT) e justificação. |
| **Inventário** | `STOCK_WASTE_APPROVE` | ID da quebra aprovada, valor financeiro e notas de aprovação. |
| **Inventário** | `STOCK_WASTE_REJECT` | ID da quebra rejeitada, operador e justificação da rejeição. |
| **Comercial** | `CLIENT_CREDIT_LIMIT_CHANGE` | Nome e ID do cliente, limite anterior em MT e novo limite em MT. |
| **Comercial** | `INVOICE_CANCEL` | Número da fatura anulada e motivo obrigatório da anulação. |
| **Comercial** | `RECEIPT_CANCEL` | Número do recibo anulado e motivo do cancelamento. |
| **Comercial** | `ORDER_CANCEL` | Número da encomenda cancelada e motivo da anulação. |
| **Comercial** | `QUOTATION_CANCEL` | Número da cotação cancelada e motivo. |
| **Segurança / RH** | `USER_CREATE` | Username do novo utilizador, nome completo e papel inicial atribuído. |
| **Segurança / RH** | `USER_ROLE_CHANGE` | Username do utilizador afetado, papel anterior e novo papel atribuído. |
| **Segurança / RH** | `USER_PIN_SET` | Username do utilizador que recebeu/atualizou o PIN de gerente. |
| **Segurança / RH** | `USER_PASSWORD_RESET` | Username do utilizador com palavra-passe redefinida. |
| **Segurança / RH** | `USER_STATUS_CHANGE` | Username do utilizador e estado atualizado (Ativado / Desativado). |

---

## 3. Modelo de Dados e Migração

Tabela `audit_logs` atualizada com suporte a IP de origem:
```sql
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50);
```
Dicionário de dados da entidade:
* `id`: Chave primária sequencial (`BIGSERIAL`).
* `company_id`: ID da empresa/tenant isolado (`BIGINT`).
* `event_time`: Carimbo de data/hora (`TIMESTAMP`).
* `username`: Utilizador autor da mutação (`VARCHAR(255)`).
* `action`: Código padronizado da ação (`VARCHAR(255)`).
* `details`: Explicação com contexto semântico (`VARCHAR(1000)`).
* `ip_address`: Endereço IP do posto emissor (`VARCHAR(50)`).
