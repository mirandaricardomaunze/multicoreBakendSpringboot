# Harness de Testes: Bloqueio Otimista e Concorrência Transversal (HARNESS-CONC-001)

## 1. Objectivo

Este harness audita e homologa a conformidade da especificação [OPTIMISTIC_LOCKING_CONCURRENCY_SPEC.md](OPTIMISTIC_LOCKING_CONCURRENCY_SPEC.md) (`SPEC-CONC-001`), exercitando o comportamento de concorrência, prevenção de *lost updates* e resposta HTTP 409 amigável no `GlobalExceptionHandler`.

---

## 2. Cenários de Teste

| ID | Cenário | Ação de Teste | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **CONC-01** | Tratamento Centralizado no GlobalExceptionHandler | Lançamento de `ObjectOptimisticLockingFailureException` e `OptimisticLockException`. | Resposta HTTP 409 Conflict estruturada com mensagem descritiva em português, sem erro 500 nem stack trace vazada. |
| **CONC-02** | Proteção contra Lost Update em Clientes (`Client`) | Simulação de conflito de versão em `Client`. | Falha de bloqueio otimista intercetada, garantindo integridade dos dados anteriores. |
| **CONC-03** | Proteção contra Lost Update em Produtos (`Product`) | Simulação de mutações paralelas de preço/stock em `Product`. | Versão incrementada atomicamente e rejeição de versão defasada. |
| **CONC-04** | Proteção contra Lost Update em Fornecedores (`Supplier`) | Edição de fornecedor com versão concorrente. | Exceção de bloqueio otimista disparada e rejeição de escrita desatualizada. |
| **CONC-05** | Retrocompatibilidade de DTOs e Construtores | Instanciação de `ClientDTO`, `ProductDTO` e `SupplierDTO` com e sem `version`. | Preservação integral de construtores existentes e novos campos mapeados. |

---

## 3. Execução Automatizada

Classe de teste canónica:
- [OptimisticLockingConcurrencyHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/concurrency/OptimisticLockingConcurrencyHarnessTest.java)

Comando de teste:
```powershell
mvn test -pl backend -Dtest=OptimisticLockingConcurrencyHarnessTest
```

Critério de aprovação:
- 100% de aprovação (5/5 cenários aprovados com 0 falhas).
