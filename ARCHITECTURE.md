# Arquitectura — Multicore ERP

Documento único e canónico das **regras de arquitectura**. Consolida o antigo `ARCHITECTURAL_GUIDELINES.md` e `ARCHITECTURE_SEPARATION.md`. Se uma regra aqui colide com código existente, **a regra prevalece** e o código tem bug a corrigir.

---

## 1. Visão geral

```text
┌─────────────────────────────────────────────────────────────────┐
│           CLIENTE DESKTOP (Swing)                                │
│  com.phcpro.desktop.DesktopApplication                          │
│  + com.phcpro.gui.*  (Painéis, UIHelper, componentes)           │
└────────────────────────┬────────────────────────────────────────┘
                         │  hoje: chamadas directas a @Service
                         │  meta: HTTPS contra backend
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│           BACKEND (Spring Boot)                                  │
│  com.phcpro.MulticoreApplication                                │
│  └── modules/<dominio>/                                          │
│       controller → service → repository → model                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
                   ┌──────────────┐
                   │  H2 / PG     │
                   └──────────────┘
```

Dois entrypoints, **uma única árvore Java**:

| Entrypoint                                       | Quando usar                                       |
|--------------------------------------------------|---------------------------------------------------|
| `com.phcpro.MulticoreApplication`                | Backend puro (API online, sem Swing)              |
| `com.phcpro.desktop.DesktopApplication`          | Cliente desktop — perfil `desktop`, abre Swing    |

---

## 2. Camadas obrigatórias por módulo

Para cada domínio em `com.phcpro.modules.<dom>/`:

```
controller/   ← @RestController         só HTTP
service/      ← @Service                lógica de negócio + @Transactional
repository/   ← @Repository             JpaRepository<Entity, Long>
model/        ← @Entity extends BaseEntity   tabela
dto/          ← record  CreateXxxRequest / XxxDTO    contrato externo
```

### Responsabilidade — quem faz o quê

| Camada      | **Faz**                                                                   | **Não faz**                                                          |
|-------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------|
| Controller  | Receber request, `@Valid`, delegar a Service, devolver `ResponseEntity<DTO>` | Lógica de negócio, regras, consultas SQL, transformações complexas    |
| Service     | Regras de negócio, validações semânticas, orquestrar transacções           | Conhecer `HttpServletRequest`, `ResponseEntity`, formatar JSON         |
| Repository  | Consultas (`@Query` JPQL), `findBy…`, persistência                          | Lógica de negócio, validações, transformações DTO                     |
| Model       | Mapear tabela, relações (`@ManyToOne(LAZY)`), auditoria via `BaseEntity`    | Conter lógica de negócio que ultrapasse o próprio agregado            |
| DTO         | Transportar dados na fronteira API ⇄ exterior; validar input               | Conter regras de cálculo ou conhecer JPA                              |

### Setas legais (quem pode chamar quem)

```
Controller  ──→  Service  ──→  Repository  ──→  Model
     │              │
     └──→ DTO       └──→ DTO ⇄ Model (mapear no Service ou MapperHelper)
```

**Proibido**:
- Controller → Repository (saltar a Service).
- Repository → Service.
- Service A → Repository do Service B.
- Devolver `@Entity` JPA para fora do Service (sempre converter para DTO).
- Entidade JPA usada como request body num Controller.

---

## 3. SOLID aplicado ao Multicore

### SRP — Single Responsibility
Cada classe tem **uma** razão para mudar.
- Mau: `InvoiceService` que também cria PDFs.
- Bom: `InvoiceService` (regras) + `InvoicePrintService` (geração PDF) injectados separadamente.

### OCP — Open/Closed
Abrir por extensão (novo módulo, nova estratégia), fechar por modificação. Ex.: FEFO é uma estratégia de consumo de stock; futuras estratégias (LIFO/FIFO) devem ser irmãs, não `if/else` espalhados.

### LSP — Liskov
Subtipos honram contratos do supertipo. `BusinessRuleException extends RuntimeException` mantém semântica de unchecked.

### ISP — Interface Segregation
Repositories herdam `JpaRepository` mas adicionam **só os finders que o domínio precisa**. Não criar repositórios "Deus" com métodos para todos os módulos.

### DIP — Dependency Inversion
Injecção por **construtor**, dependências apontam para abstracções (interface) sempre que houver mais que uma implementação.

```java
// ✅ certo
private final ProductRepository repo;
public ProductService(ProductRepository repo) { this.repo = repo; }

// ❌ errado
@Autowired private ProductRepository repo;
```

---

## 4. Padrão DTO

Todos os atravessamentos de fronteira (Controller ⇄ exterior) usam **records**.

```java
// Entrada
public record CreateProductRequest(
    @NotBlank @Size(max = 32)  String sku,
    @NotBlank @Size(max = 200) String name,
    @NotNull @Positive         BigDecimal unitPrice
) {}

// Saída
public record ProductDTO(
    Long id,
    String sku,
    String name,
    BigDecimal unitPrice
) {}
```

**Regras:**
1. Records, imutáveis. Sem setters, sem lógica.
2. Input traz validações Bean Validation (`@NotBlank`, `@Positive`, `@Size`, …).
3. Output **nunca** expõe campos sensíveis (passwords, hashes, dados auditoria internos).
4. Mapper vive no Service como método `toDTO(Entity)` ou numa classe utilitária dedicada.
5. Nunca usar `@Entity` directamente como `@RequestBody` nem em retornos de Controller.

---

## 5. Transacções e exceções

### Transacções

| Anotação                              | Onde                                                        |
|----------------------------------------|-------------------------------------------------------------|
| `@Transactional`                       | Métodos de escrita no Service (rollback automático em RuntimeException) |
| `@Transactional(readOnly = true)`      | Consultas que envolvem mais que um `findById` simples       |
| (sem anotação)                         | Apenas em métodos puramente utilitários sem acesso a BD     |

Nunca anotar Controller. Nunca anotar Repository (já é gerido pelo Spring Data).

### Exceções

- **Regra de negócio violada** → `throw new BusinessRuleException("mensagem clara em PT")`.
- **Sintaxe / validação de input** → automaticamente via Bean Validation, devolvido como 400.
- **Bug genuíno** → propaga; será capturado pelo handler global como 500.
- Resposta JSON uniforme:

```json
{
  "timestamp": "2026-06-01T17:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Quantidade da linha deve ser positiva.",
  "path": "/api/purchases"
}
```

Mensagens são **vistas pelo utilizador final** — sempre em português de Moçambique, claras, accionáveis. Evitar jargão técnico (`null`, `IllegalArgument`, etc.).

---

## 6. DRY

| Repetição típica                                | Solução                                                                |
|--------------------------------------------------|------------------------------------------------------------------------|
| Campos `createdAt/updatedAt/createdBy`           | Herdar `BaseEntity` (`@MappedSuperclass`)                              |
| `try/catch` em cada Controller                   | `@RestControllerAdvice` global                                          |
| Conversões Entity ⇄ DTO                          | Método `toDTO()` no Service ou `MapperHelper` central                  |
| Cabeçalho/rodapé de PDF                          | Building blocks em `modules/printing/`                                  |
| Estilos Swing                                    | `UIHelper.styleX(...)`, `ModernButton`, `ModernPanel`                  |

---

## 7. Migração desktop ⇄ backend

Hoje os painéis Swing (`com.phcpro.gui.*`) **injectam Services directamente** (`@Autowired` via construtor) — comodidade do monolito durante a migração. Meta:

```text
Swing instalado
  └─ HTTP client (`com.phcpro.desktop.client.*`)
       └─ HTTPS → backend online (Spring Boot)
            └─ PostgreSQL gerido
```

### Passos sequenciais

1. Criar `com.phcpro.desktop.client.ApiConfig` com base URL configurável.
2. Camada `com.phcpro.desktop.client.<dominio>Client` (um por módulo) que chama o backend via `RestClient` ou `WebClient`.
3. Substituir injecções directas de Service nos painéis pelos Clients.
4. Ordem de migração sugerida: **auth → produtos → stock → POS → vendas → compras → restantes**.
5. Quando todos os painéis usarem Clients, separar o desktop em módulo Maven independente.

Enquanto a migração está em curso:
- **Lógica vai sempre para Service**, mesmo que hoje seja chamada directamente pelo Swing. Quando o Client HTTP existir, o Service não muda.
- **Regras de negócio nunca em painéis** — se for puxado para o backend amanhã, perde-se.

---

## 8. Auditoria, segurança, contexto de utilizador

- `BaseEntity` traz `createdAt`, `updatedAt`, `createdBy` automaticamente.
- Identidade do operador propagada via `com.phcpro.architecture.security.CurrentUserContext` (`getCurrentCompanyId()`, …).
- Logs sensíveis (login, aprovações, anulações de fatura) escritos no módulo `audit/`.
- Permissões: módulo `users/` + `approvals/`. Controllers que mexam em dinheiro/fiscal devem ser guardados por permissão antes de delegar à Service.

---

## 9. Lista de verificação antes de commit

Para qualquer alteração que toque um módulo de negócio:

- [ ] Há DTO de entrada e saída? Nenhuma `@Entity` cruza a fronteira do Controller.
- [ ] Service tem `@Transactional` adequado (write vs readOnly).
- [ ] Regras de negócio lançam `BusinessRuleException` com mensagem em PT.
- [ ] Injecção por construtor, campos `final`.
- [ ] Não há SQL ou JPQL fora do Repository.
- [ ] `mvn clean compile` passa (ignorar diagnostics Lombok do IDE).
- [ ] Se há UI Swing nova: usa `UIHelper.icon(...)`, sem emojis em botões.

---

## 10. Quando estás na dúvida

> "Se este código for amanhã puxado para um microserviço, sobrevive?"

Se a resposta é **não**, está na camada errada.
