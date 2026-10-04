# Harness de Testes: Segurança de Inputs e Validação no Backend (HARNESS-SEC-VAL-001)

## 1. Objectivo

O presente harness homologa a implementação da especificação [SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_SPEC.md](SEGURANCA_INPUTS_E_VALIDACAO_BACKEND_SPEC.md) (`SPEC-SEC-VAL-001`), exercitando o conjunto de validações declarativas, sanitização activa contra ataques de injeção, prevenção de Path Traversal e tratamento fail-safe de excepções no backend.

---

## 2. Cenários de Teste

| ID | Cenário | Validação Executada | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **SEC-01** | Validação Canónica de NUIT | `@ValidNuit` com 9 dígitos numéricos, consumidor final `999999999` e verificação estrita de Módulo 11 da AT. | NUITs válidos passam sem erros; caracteres alfabéticos, tamanhos divergentes e dígitos de controlo errados geram violações de validação. |
| **SEC-02** | Validação de Telefones Moçambicanos | `@ValidPhoneMZ` em redes móveis (82 a 87) e fixas (21 a 28), com/sem `+258`. | Operadoras moçambicanas aceites; prefixos inexistentes (ex.: 81) ou números malformados são rejeitados. |
| **SEC-03** | Validação de B.I. Moçambicano | `@ValidBiMZ` (12 algarismos numéricos seguidos de 1 letra maiúscula de controlo). | BI válido aceite; números sem letra de controlo ou caracteres especiais rejeitados. |
| **SEC-04** | Higienização de Texto e Script Tags | `InputSanitizer.stripControlCharacters`, `sanitizeNotes` e `sanitizeSearchQuery`. | Bytes nulos (`\0`) e caracteres de controlo removidos; acentuação e cedilhas preservadas; tags de script (`<script>`) neutralizadas. |
| **SEC-05** | Prevenção de Path Traversal | `InputSanitizer.sanitizeFileName` em nomes de ficheiro. | Tentativas de subida de directório (`../../etc/passwd`, `..\..\cmd.exe`) têm separadores eliminados de forma segura; entradas em branco devolvem `"documento"`. |
| **SEC-06** | Endurecimento de Erros do Servidor | `GlobalExceptionHandler` manipulando `MethodArgumentNotValidException`, `DataIntegrityViolationException` e `IllegalArgumentException`. | Retorno consistente em `HTTP 400 Bad Request` com mensagens descritivas para o utilizador, sem qualquer fuga de nomes de constraints, tabelas ou logs SQL internos. |
| **SEC-07** | Validação Declarativa dos DTOs | Validação de `SaveClientRequest`, `CreateSupplierRequest`, `CreateStockWasteRequest`, `CreateWarehouseRequest`, `CreateCompanyRequest` e `SetManagerPinRequest`. | DTOs rejeitam inputs inválidos automaticamente antes da execução dos serviços de negócio. |

---

## 3. Execução Automatizada dos Testes

O teste executável está localizado em:
- [InputSecurityAndBackendValidationHarnessTest.java](file:///c:/Users/miran/Desktop/manager/backend/src/test/java/mz/multicore/erp/architecture/security/InputSecurityAndBackendValidationHarnessTest.java)

Comando canónico de validação:
```powershell
mvn test -pl backend -Dtest=InputSecurityAndBackendValidationHarnessTest
```

Critério de aprovação:
- **7/7 testes aprovados** com 0 falhas, 0 erros e compilação limpa em todos os módulos do reator Maven.
