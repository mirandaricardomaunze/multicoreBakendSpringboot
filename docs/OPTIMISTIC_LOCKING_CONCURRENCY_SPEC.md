# Especificação Técnica: Controlo Estrito de Concorrência & Bloqueio Otimista Transversal (SPEC-CONC-001)

## 1. Visão Geral e Objectivo

Num ambiente multi-operador e multi-terminal como o Multicore ERP, múltiplos utilizadores acedem e modificam dados em simultâneo (ex.: caixas no POS, comerciais a atualizar clientes/preços, gestores de armazém e compradores).
Sem controlo explícito de concorrência, o sistema sofre de **Lost Updates** (a última gravação sobrepõe silenciosamente as alterações de outro operador sem qualquer alerta).

O objectivo desta especificação é estabelecer uma regra não negociável e universal de **Bloqueio Otimista (@Version)** em todas as entidades mestras e transacionais críticas, acompanhado de tratamento centralizado de conflitos no `GlobalExceptionHandler`.

---

## 2. Princípios de Arquitetura e Engenharia

1. **Defesa contra Lost Updates (SOLID / Concurrency Control):**
   - Toda entidade mestra (`Client`, `Product`, `Supplier`) e documento transacional (`Order`, `Quotation`, `StockTransfer`, `PurchaseOrder`) possui um campo `@Version private Long version;`.
   - O Hibernate/JPA incrementa atomicamente a versão a cada mutação (`WHERE id = ? AND version = ?`).
   - Se a versão no momento do commit divergir da versão carregada na memória, o Hibernate lança `ObjectOptimisticLockingFailureException` / `OptimisticLockException`.

2. **Tratamento Centralizado e Amigável no Servidor (`GlobalExceptionHandler`):**
   - As exceções de concorrência otimista não podem cair no `handleGeneric` gerando erro 500 (*Internal Server Error*).
   - O `GlobalExceptionHandler` intercepta especificamente `ObjectOptimisticLockingFailureException` e `OptimisticLockException` e responde com `HTTP 409 Conflict` contendo mensagem semântica em português de Moçambique:
     `"Este registo foi alterado ou aprovado concorrentemente por outro utilizador. Por favor, actualize os dados antes de gravar."`

3. **Compatibilidade de Contratos (`contracts`):**
   - Os DTOs de dados mestres (`ClientDTO`, `ProductDTO`, `SupplierDTO`) transportam o campo `version` para a interface do utilizador.
   - Qualquer sobrecarga anterior é preservada para garantir retrocompatibilidade com clientes desktop e serviços legados.

---

## 3. Entidades Cobertas

| Entidade | Tabela | Mecanismo |
| :--- | :--- | :--- |
| `Client` | `clients` | `@Version private Long version;` |
| `Product` | `products` | `@Version private Long version;` |
| `Supplier` | `suppliers` | `@Version private Long version;` |
| `Order` | `orders` | `@Version private Long version;` (Já activo) |
| `Quotation` | `quotations` | `@Version private Long version;` (Já activo) |
| `StockTransfer` | `stock_transfers` | `@Version private Long version;` (Já activo) |
| `PurchaseOrder` | `purchase_orders` | `@Version private Long version;` (Já activo) |
| `Stock` / `ProductBatch` | `stocks` / `product_batches` | `@Version private Long version;` (Já activo) |

---

## 4. Migração de Base de Dados

Migration Flyway canónica: `V80__optimistic_locking_master_data.sql`:
```sql
ALTER TABLE clients ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
```
