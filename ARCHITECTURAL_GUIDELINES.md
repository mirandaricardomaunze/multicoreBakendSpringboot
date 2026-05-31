# Diretrizes de Arquitetura de Software: Padrão Multicore Java

Este documento estabelece as regras de design e boas práticas de engenharia de software que guiarão o desenvolvimento do nosso sistema ERP. O objetivo é garantir um código limpo, de fácil manutenção, escalável e de nível profissional.

---

## 1. Princípios SOLID Aplicados

### Single Responsibility Principle (SRP) - Princípio da Responsabilidade Única
Cada classe e componente do sistema deve ter apenas **uma única responsabilidade** e, portanto, apenas uma razão para ser alterada.

*   **Controllers (`@RestController`):** Responsáveis apenas por receber requisições HTTP, validar sintaticamente a entrada (ex: `@Valid`), delegar o processamento à camada de serviço e formatar a resposta HTTP. Não contêm lógica de negócio.
*   **Services (`@Service`):** Responsáveis por encapsular toda a lógica de negócio, regras de validação semântica e coordenação de transações. Eles não interagem com conceitos de HTTP (como `HttpServletRequest`).
*   **Repositories (`@Repository`):** Responsáveis única e exclusivamente pelo acesso aos dados e execução de consultas ao banco de dados.
*   **DTOs (Data Transfer Objects):** Responsáveis por transportar dados entre a rede e as camadas da aplicação, sem possuir lógica de negócio ou detalhes de mapeamento do banco de dados.

---

## 2. Padrão DTO (Data Transfer Object)

Para isolar o modelo de banco de dados (Entidades JPA) da camada de apresentação (API REST), utilizaremos DTOs específicos para cada operação.

### Por que usar?
1.  **Segurança:** Evita a exposição acidental de colunas sensíveis (como senhas ou dados internos) ou injeções indesejadas de dados.
2.  **Desacoplamento:** O banco de dados pode mudar sua estrutura sem quebrar os contratos públicos da API (frontend).
3.  **Desempenho:** Transmite apenas os dados necessários pela rede.

### Exemplo de Estrutura
```java
// Entidade JPA (Banco de Dados)
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;
    
    private BigDecimal totalAmount;
    private String status; // PENDING_APPROVAL, APPROVED, PAID
}

// DTO de Entrada (Criação de Fatura)
public record CreateInvoiceRequest(
    @NotBlank String clientTaxId,
    @NotEmpty List<InvoiceItemDTO> items
) {}

// DTO de Saída (Detalhe de Fatura para o Frontend)
public record InvoiceResponse(
    Long id,
    String invoiceNumber,
    String clientName,
    BigDecimal totalAmount,
    String status
) {}
```

---

## 3. DRY (Don't Repeat Yourself)

Evitaremos a duplicação de lógica aplicando padrões comuns de reuso:

*   **Herança de Auditoria:** Entidades que necessitam de rastreio de criação/modificação (ex: documentos, transações, aprovações) herdarão de uma classe base `@MappedSuperclass` contendo campos como `createdAt`, `updatedAt` e `createdBy`.
*   **Tratamento de Exceções Global:** Em vez de blocos `try-catch` repetitivos em cada Controller, criaremos um interceptador global (`@RestControllerAdvice`). Qualquer erro de negócio lançará uma exceção customizada (ex: `BusinessRuleException`), capturada centralizadamente para gerar uma resposta HTTP consistente.
*   **Conversores (Mappers):** Centralização da conversão entre Entidades e DTOs usando classes auxiliares ou métodos estáticos dedicados (ex: `toDTO()`, `toEntity()`).

---

## 4. Camada de Serviços (Service Layer) e Transações

A lógica de negócios reside inteiramente nos serviços:

*   **Anotação `@Transactional`:** Usada nos métodos de escrita para garantir que todas as operações no banco ocorram dentro de uma transação ativa. Se ocorrer um erro, a transação sofre *rollback* automático.
*   **Encapsulamento de Regras:** Nenhuma operação de banco de dados deve ser executada sem passar pela validação de negócio dos serviços.
*   **Design Clean:** Os serviços devem ser altamente focados e injetados via construtor (Injeção de Dependências por Construtor, evitando a anotação `@Autowired` diretamente nos campos, facilitando testes unitários).

---

## 5. Tratamento de Exceções e Respostas Padronizadas

Todas as respostas de erro da API seguirão um formato JSON uniforme para facilitar a integração com o frontend:

```json
{
  "timestamp": "2026-05-22T21:07:33Z",
  "status": 400,
  "error": "Bad Request",
  "message": "A fatura não pode ser aprovada pois o valor excede o limite do aprovador.",
  "path": "/api/approvals/123/approve"
}
```
