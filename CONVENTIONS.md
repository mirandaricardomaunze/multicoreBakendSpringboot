# Convenções de Código — Multicore

Convenções diárias. Onde colidirem com [ARCHITECTURE.md](ARCHITECTURE.md), a arquitectura ganha.

---

## 1. Idioma

- **Mensagens ao utilizador** (UI, exceções, logs visíveis): **português de Moçambique**.
- **Identificadores Java** (classes, métodos, variáveis): **inglês**.
- **Tabelas SQL e colunas**: `snake_case` em inglês (`product_batches`, `expiration_date`).
- **Comentários**: PT quando expressam regra de negócio, EN quando explicam mecanismo técnico.

```java
// ✅ correcto
throw new BusinessRuleException("Quantidade da linha deve ser positiva.");

// ❌ errado — mistura idiomas e termos técnicos
throw new BusinessRuleException("Invalid qty: must be > 0 (NPE downstream)");
```

---

## 2. Naming

| Elemento                | Convenção                       | Exemplo                                   |
|-------------------------|----------------------------------|--------------------------------------------|
| Classe                  | `PascalCase`                     | `ProductBatchService`                      |
| Método                  | `camelCase`, verbo no infinitivo | `findNextFEFO`, `consumeFEFO`              |
| Constante               | `UPPER_SNAKE`                    | `LEGACY_EXPIRATION`                        |
| Variável local / campo  | `camelCase`                      | `expirationDate`                           |
| Pacote                  | `lowercase.singular`             | `com.phcpro.modules.inventory`             |
| Tabela JPA              | `snake_case`, **plural**         | `@Table(name = "product_batches")`         |
| DTO de input            | `Create<Entidade>Request`        | `CreateProductRequest`                     |
| DTO de output           | `<Entidade>DTO`                  | `ProductBatchDTO`                          |
| Endpoint REST           | `/api/<modulo>[/<recurso>]`      | `/api/inventory/batches/expiring`          |

Verbos típicos em Service: `create`, `find`, `update`, `delete`, `register`, `cancel`, `approve`, `consume`, `ensure`.

---

## 3. Lombok

Permitido **apenas**:
- `@Getter @Setter` em `@Entity` e na maioria dos `model/`.
- (raríssimo) `@RequiredArgsConstructor` quando o construtor seria literalmente igual ao gerado.

**Proibido** (forçado por `lombok.config` na raíz):
- `@Data` (gera `equals/hashCode/toString` em entidades JPA — perigo de equals com proxies). `ERROR`.
- `@AllArgsConstructor` (preferir construtor explícito ou record). `WARNING`.
- `@Builder` em entidades (induz a criar entidades inconsistentes). `WARNING`.

### Configuração do IDE (eliminar ruído `cannot find symbol`)

O language server do IDE não corre annotation processors por defeito, daí os falsos erros `cannot find symbol: getX()`. **`mvn compile` é a verdade.** Para silenciar o ruído:

**VS Code** (extensão `redhat.java`):
1. Instalar a extensão `vscjava.vscode-lombok` (ou `gabrielbb.vscode-lombok`).
2. Reiniciar o language server: `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`.
3. Verificar que `~/.vscode/extensions/.../lombok.jar` está a ser passado como `-javaagent` ao JDT-LS.

**IntelliJ IDEA**:
1. `File → Settings → Plugins` → instalar `Lombok`.
2. `File → Settings → Build → Compiler → Annotation Processors` → activar `Enable annotation processing`.
3. Reabrir o projecto.

**Eclipse**:
1. Descarregar `lombok.jar` em https://projectlombok.org/download.
2. Correr `java -jar lombok.jar`, apontar para a instalação do Eclipse, reiniciar.

---

## 4. Records vs classes

| Caso                                        | Usar                  |
|---------------------------------------------|-----------------------|
| DTO de input / output                        | `record`              |
| Resposta paginada / colecção                 | `record` ou `List<DTO>` |
| Entidade JPA                                 | classe + `@Getter @Setter` |
| Service / Controller / Repository            | classe                |
| Value object imutável dentro do domínio      | `record`              |

---

## 5. Injecção de dependências

**Sempre por construtor, campos `final`.** Nunca `@Autowired` em campo.

```java
@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProductBatchService productBatchService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                            ProductBatchService productBatchService) {
        this.purchaseRepository = purchaseRepository;
        this.productBatchService = productBatchService;
    }
    // …
}
```

Para Swing (`gui/`): mesma regra — o painel recebe Services no construtor.

---

## 6. Transacções

- Anotar **métodos de Service**, não classes (granularidade).
- Escrita: `@Transactional`. Leitura agregada/`findAll`: `@Transactional(readOnly = true)`.
- Nunca anotar Controller, Repository nem painéis Swing.
- Métodos que combinam várias escritas devem estar **dentro do mesmo Service** e numa única `@Transactional` — não dividir em métodos chamados externamente que perdem o contexto.

---

## 7. Validação

| Onde                          | Como                                                     |
|-------------------------------|----------------------------------------------------------|
| Input do Controller            | Bean Validation no DTO (`@Valid` no parâmetro)           |
| Regras de negócio              | `if (...) throw new BusinessRuleException("…")` no Service |
| Integridade da BD              | Constraints JPA (`nullable=false`, `@UniqueConstraint`) |

Não duplicar validação sintáctica nos dois sítios. Não confiar só nas constraints JPA para erros de utilizador (a mensagem fica feia).

```java
// DTO
public record CreatePurchaseLineRequest(
    @NotNull Long productId,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    String batchNumber,
    LocalDate expirationDate
) {}

// Service
if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
    throw new BusinessRuleException("Não é permitido receber lote já vencido.");
}
```

---

## 8. JPA

- `@ManyToOne` e `@OneToOne` sempre `fetch = FetchType.LAZY`. EAGER só quando provado necessário.
- `@OneToMany` por defeito LAZY; cuidado com N+1 — usar `JOIN FETCH` no Repository quando se for percorrer a colecção.
- `BigDecimal` para dinheiro e quantidades. Definir `precision`/`scale` explícitos:
  ```java
  @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
  private BigDecimal quantity = BigDecimal.ZERO;
  ```
- `LocalDate` para datas sem hora, `LocalDateTime` para timestamps locais, `Instant` para timestamps UTC.
- Índices: declarar em `@Table(indexes = …)` quando há finder que usa o campo.
- Constraints únicas: `@UniqueConstraint(columnNames = {…})`.

---

## 9. Repository

- Herdar `JpaRepository<Entity, Long>`.
- Métodos derivados (`findByXxx`) para casos simples; `@Query` JPQL para joins/agregados.
- **Não devolver entidades para fora do Service.** Repository devolve `Entity` ou `Optional<Entity>`; o Service converte para DTO.

```java
@Query("SELECT b FROM ProductBatch b " +
       "WHERE b.product.id = :productId AND b.warehouse.id = :warehouseId AND b.quantity > 0 " +
       "ORDER BY b.expirationDate ASC, b.entryDate ASC, b.id ASC")
List<ProductBatch> findAvailableFEFO(@Param("productId") Long productId,
                                      @Param("warehouseId") Long warehouseId);
```

---

## 10. Excepções

| Caso                            | Excepção                                  |
|---------------------------------|--------------------------------------------|
| Regra de negócio violada         | `BusinessRuleException`                    |
| Entidade não encontrada (404)    | `BusinessRuleException("X não encontrado.")` ou `EntityNotFoundException` |
| Permissão                        | `BusinessRuleException("Sem permissão para …")` (até haver `AccessDeniedException` próprio) |
| Input sintáctico                 | Bean Validation (automático)               |

**Nunca** apanhar `Exception` para esconder erro. Quando capturar é mesmo necessário:

```java
try {
    inventoryService.registerMovement(...);
} catch (BusinessRuleException ex) {
    JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
}
```

---

## 11. UI Swing

- **Painéis** ficam em `com.phcpro.gui/<Modulo>Panel.java` e injectam Services no construtor.
- **Estilos** via `UIHelper.style…(comp)`, `ModernButton`, `ModernPanel`. Não criar `new Color(...)` ad-hoc — usar as paletas de `UIHelper` (`ACCENT_BLUE`, `BG_DARK`, `TEXT_MUTED`, …).
- **Ícones**: `UIHelper.icon("fas-<nome>", 14)` (FontAwesome 5 Solid via Ikonli). **Nunca emojis em labels** de botões / tabuladores.
- **Diálogos**: `UIHelper.createDialogForm(...)` para criar forms label-campo; `UIHelper.makeDialogScrollable(...)` para diálogos altos.
- **Datas pelo utilizador**: hoje `JTextField` com placeholder `yyyy-MM-dd` e parsing via `LocalDate.parse(...)`. Não introduzir uma terceira biblioteca de date picker — manter o padrão.
- **Tabelas**: `DefaultTableModel` com `isCellEditable` definido. Render via `UIHelper.styleTable(tbl)`.
- **Tarefas longas**: executar em `SwingWorker`, não bloquear o EDT.

---

## 12. Mensagens e formatação

- Moeda: `String.format("%,.2f MT", valor)` (vírgula como milhares, ponto como decimal — locale PT).
- Datas: apresentação `dd/MM/yyyy`; input/parse `yyyy-MM-dd`.
- Quantidades: 3 casas decimais (`%,.3f`), salvo quando o módulo definir outra precisão.
- "Sem stock", "—", "Sucesso", "Erro", "Aviso" são convenções já em uso no UI — manter.

---

## 13. PDF (`modules/printing/`)

- Um Service por tipo de documento (`InvoicePrintService`, `ReceiptPrintService`, `StockTransferPrintService`, …). SRP.
- Reutilizar blocos do `printing/` para cabeçalho da empresa, tabela de linhas e bloco de totais.
- Não duplicar a montagem do header em cada serviço — extrair para helper partilhado.

---

## 14. Testes

- `src/test/java/...` reflecte a árvore de `main`.
- Tests unitários para Service (sem Spring). Tests `@SpringBootTest` só onde realmente houver wiring de container envolvido.
- Nome dos métodos: `metodoSobTeste_<cenário>_<resultadoEsperado>()`.
  ```java
  @Test void consumeFEFO_stockInsuficiente_lancaBusinessRuleException() { … }
  ```

---

## 15. Git

- Mensagens em **inglês**, imperativo, prefixos convencionais: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `test:`.
- Um commit = uma intenção. Não misturar refactor com feature.
- Nunca `--no-verify`. Se hook falha, corrigir a causa.

---

## 16. Comentários

- Por defeito **nenhum**. Bom naming substitui comentário.
- Quando justificado: explicar **o porquê** (constraint legal, decisão fiscal, workaround documentado).
- Nunca referenciar IDs de issues, autores, datas — isso vive no `git log`.

```java
// ✅ explica regra fiscal não-óbvia
// IVA Moçambique aplicado depois do desconto de linha, antes do desconto comercial global.
BigDecimal taxBase = lineSubtotal.subtract(lineDiscount);

// ❌ redundante
// soma os totais
total = total.add(amount);
```

---

## 17. Ficheiros novos

- Antes de criar um ficheiro, ver se há skill que o faça (`.claude/skills/phc-new-*`).
- Não criar `README.md`, `NOTES.md`, `TODO.md` dentro de módulos — usar [tasks/current.md](tasks/current.md).
- Não criar pacote novo dentro de um módulo sem que haja >2 classes a viver lá.
