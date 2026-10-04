# Especificação: Segurança de Inputs e Validação no Backend (SPEC-SEC-VAL-001)

## 1. Visão Geral e Princípios Fundamentais

Esta especificação estabelece o padrão canónico e arquitetural de **Segurança de Inputs e Validação no Backend** em todo o sistema Multicore ERP, assegurando a conformidade estrita com o princípio de **Defesa em Profundidade (*Defense-in-Depth*)**.

Nenhuma entrada de utilizador ou cliente HTTP é considerada confiável, mesmo que já tenha sido submetida a validação prévia na interface Desktop Swing:
1. **Fronteira Física Maven Limpa:** DTOs de contrato residem no módulo `contracts` com validações declarativas baseadas na especificação Jakarta Bean Validation (`jakarta.validation.*`), sem acoplamento a Spring, JPA ou Swing.
2. **Validações Canónicas Moçambicanas:** Regras fiscais e dados de identificação nacional (NUIT com Módulo 11 da AT, Telefones moçambicanos de redes móveis e fixas, Bilhete de Identidade com letra de controlo) utilizam anotações declarativas e algoritmos centrais compartilhados.
3. **Higienização Activa (*Sanitization*):** Eliminação de caracteres de controlo invisíveis (null bytes `\0`, `\x00-\x1F`), bloqueio de scripts/XSS em textos livres e imunização contra Path Traversal em nomes de ficheiro.
4. **Respostas Seguras e Sem Fuga de Detalhes Internos (*Fail-Safe Error Responses*):** O [GlobalExceptionHandler.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/architecture/exception/GlobalExceptionHandler.java) formata violações com mensagens claras em português de Moçambique sob status `HTTP 400 Bad Request`, suprimindo estritamente stack traces, nomes de constraints e esquemas de base de dados para o cliente.

---

## 2. Anotações Canónicas de Validação (`contracts`)

As anotações declarativas residem no pacote `mz.multicore.erp.architecture.validation` do módulo `contracts`:

| Anotação | Alvo | Descrição e Regras | Exemplo Aceite | Exemplo Rejeitado |
| :--- | :--- | :--- | :--- | :--- |
| **`@ValidNuit`** | `FIELD`, `RECORD_COMPONENT` | Exatamente 9 dígitos numéricos. Suporta flag opcional `checkModulo11 = true` para verificação de dígito da Autoridade Tributária. O NUIT de Consumidor Final `999999999` é sempre aceite. | `"400123456"`, `"999999999"` | `"12345678A"`, `"12345"`, `"1234567890"` |
| **`@ValidPhoneMZ`** | `FIELD`, `RECORD_COMPONENT` | Números nacionais móveis (82/83 Tmcel, 84/85 Vodacom, 86/87 Movitel) e redes fixas (21-28). Suporta prefixo internacional opcional `+258`. | `"841234567"`, `"+258 84 123 4567"`, `"21123456"` | `"811234567"`, `"12345"`, `"84123456A"` |
| **`@ValidBiMZ`** | `FIELD`, `RECORD_COMPONENT` | Bilhete de Identidade moçambicano: 12 dígitos seguidos de 1 letra maiúscula de controlo. | `"110100234567B"` | `"1101002345678"`, `"110100234567@"`, `"123B"` |

---

## 3. Sanitização de Entradas (`InputSanitizer`)

O utilitário central [InputSanitizer.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/architecture/validation/InputSanitizer.java) oferece métodos atómicos e seguros:

1. **`stripControlCharacters(String input)`**:
   - Remove bytes nulos (`\0`) e caracteres de controlo ASCII (`\x00` a `\x1F` e `\x7F`), preservando quebras de linha padrão (`\n`, `\r`) e tabulações (`\t`).
   - Preserva integralmente acentos e caracteres ortográficos em português (`ç`, `ã`, `é`, `ô`, `Á`, etc.) e símbolos monetários (`MT`, `MZN`).
2. **`sanitizeNotes(String notes, int maxLength)`**:
   - Neutraliza injeções de script (`<script>`, `</script>`, `javascript:`, `onload=`, `onerror=`) em campos de texto livre (observações de facturas, notas de devolução, etc.).
   - Trunca o texto ao limite estipulado pelo contrato.
3. **`sanitizeSearchQuery(String query, int maxLength)`**:
   - Limpa espaços supérfluos e caracteres de controlo em termos de pesquisa antes da passagem para métodos de repositório.
4. **`sanitizeFileName(String fileName)`**:
   - Previne Path Traversal e injeção de cabeçalho `Content-Disposition`.
   - Substitui separadores (`/`, `\`), dois pontos (`:`), e sequências de travessia (`..`) por `_`.
   - Devolve o valor padrão seguro `"documento"` se a entrada for vazia ou inválida.

---

## 4. Endurecimento do `GlobalExceptionHandler` (`backend`)

O tratador global de excepções [GlobalExceptionHandler.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/architecture/exception/GlobalExceptionHandler.java) foi enriquecido para interceptar e mapear adequadamente falhas de validação:

* **`MethodArgumentNotValidException` (Bean Validation de DTOs):**
  - Status: `400 BAD REQUEST`
  - Erro: `"Validation Error"`
  - Mensagem: Lista legível consolidada dos campos violados (ex.: `"Validação falhou: taxId: NUIT moçambicano inválido; email: O email deve ter um formato válido."`).
* **`ConstraintViolationException` (Jakarta Validation em Serviços/Parâmetros):**
  - Status: `400 BAD REQUEST`
  - Erro: `"Constraint Violation"`
  - Mensagem: Campo e motivo da violação de restrição.
* **`DataIntegrityViolationException` (Violação de FK, Unique no Banco):**
  - Status: `400 BAD REQUEST`
  - Erro: `"Data Integrity Violation"`
  - Mensagem: `"Operação não permitida por conflito de integridade de dados (registo duplicado ou dependência activa)."`
  - **Segurança:** O log interno grava o detalhe do erro da base de dados, mas o cliente HTTP nunca recebe nomes de tabelas, índices internos ou mensagens técnicas do driver JDBC.
* **`IllegalArgumentException`:**
  - Status: `400 BAD REQUEST`
  - Erro: `"Bad Request"` com a mensagem de negócio do argumento.

---

## 5. DTOs e Endpoints Reforçados

1. **Comercial / Vendas:**
   - [SaveClientRequest.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/comercial/dto/SaveClientRequest.java): Validação `@ValidNuit` no `taxId`, `@Email` no `email`, limites de tamanho em nomes e morada.
   - [ComercialController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/comercial/controller/ComercialController.java): `@Valid` activo em criação e actualização de clientes.
2. **Compras / Fornecedores:**
   - [CreateSupplierRequest.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/purchases/dto/CreateSupplierRequest.java): Validação `@ValidNuit` no `taxId`, `@ValidPhoneMZ` no `phone`, limites de tamanho.
3. **Inventário & Quebras de Stock:**
   - [CreateStockWasteRequest.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/inventory/dto/CreateStockWasteRequest.java): Obrigatoriedade de empresa, armazém, produto, motivo e `@Positive` na quantidade.
   - [StockWasteController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/inventory/controller/StockWasteController.java): Adição de `@Valid` em `register` e `approve`.
   - [CreateWarehouseRequest.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/inventory/dto/CreateWarehouseRequest.java): Validação `@ValidPhoneMZ` no telefone do armazém e tamanhos máximos.
   - [InventoryPhysicalCountingController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/inventory/controller/InventoryPhysicalCountingController.java): Adição de `@Valid` em abertura de sessão e registo de contagens físicas.
4. **Utilizadores e Segurança:**
   - [UserSecurityRequestsDTOs.java](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/users/dto/UserSecurityRequestsDTOs.java): Validação `@NotBlank` e limites de tamanho em PINs e palavras-passe.
   - [UserController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/users/controller/UserController.java): Adição de `@Valid` em todos os endpoints mutantes (`create`, `updateName`, `updateRole`, `setManagerPin`, `verifyManagerPin`, `resetPassword`, `toggleStatus`).
5. **Geração de Documentos e Impressão:**
   - [PrintController.java](file:///c:/Users/miran/Desktop/manager/backend/src/main/java/mz/multicore/erp/modules/printing/PrintController.java): Sanitização ativa do nome de ficheiro através de `InputSanitizer.sanitizeFileName(fileBase)`.
