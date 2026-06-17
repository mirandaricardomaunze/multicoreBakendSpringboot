# Multicore — ERP profissional em Java/Spring Boot + Swing

Multicore é um ERP modular (vendas, compras, stock, POS, fiscal, RH, CRM, financeira, aprovações, auditoria) com **um único codebase** que arranca em dois modos:

- **Backend HTTP/API** — `com.phcpro.MulticoreApplication` (Spring Boot puro, sem janelas).
- **Cliente desktop Swing** — `com.phcpro.desktop.DesktopApplication` (arranca Spring Boot com perfil `desktop` e abre a janela de login).

A meta de migração é o desktop falar **só HTTPS** com o backend; ver [ARCHITECTURE.md](ARCHITECTURE.md).

## Stack

| Camada            | Tecnologia                                              |
|-------------------|---------------------------------------------------------|
| Linguagem         | Java 21                                                 |
| Framework         | Spring Boot 3.2.5 (Web + Data JPA + Validation)         |
| Persistência      | JPA/Hibernate                                            |
| BD local          | H2 (in-memory, zero-setup)                              |
| BD alvo produção  | PostgreSQL (via HTTPS contra backend online)            |
| UI desktop        | Java Swing + componentes próprios (`gui/components`)    |
| Ícones            | Ikonli + FontAwesome 5 Solid (`UIHelper.icon(...)`)      |
| PDF               | OpenPDF (LGPL/MPL fork do iText)                        |
| Boilerplate       | Lombok (`@Getter`, `@Setter`)                           |
| Build             | Maven                                                    |

> ⚠️ **Nota Lombok:** o language server do IDE não corre annotation processors por defeito e marca falsos erros `cannot find symbol: getX()`. Confiar sempre no `mvn compile` para a verdade.

## Estrutura

```
src/main/java/com/phcpro/
├── MulticoreApplication.java        # entrypoint backend (sem Swing)
├── architecture/                     # base classes: BaseEntity, exceções, security context
├── desktop/
│   └── DesktopApplication.java      # entrypoint Swing
├── gui/                              # painéis Swing (StockPanel, POSPanel, ComercialPanel, …)
│   └── components/                   # UIHelper, ModernButton, ModernPanel, …
└── modules/                          # módulos de negócio (uma pasta = um domínio)
    ├── approvals/
    ├── audit/
    ├── backup/
    ├── comercial/                    # produtos, clientes, faturas, encomendas
    ├── company/
    ├── crm/
    ├── financeira/
    ├── fiscal/
    ├── hr/
    ├── inventory/                    # armazéns, stock, lotes, validades, FEFO
    ├── pos/
    ├── printing/
    ├── purchases/
    ├── reports/
    └── users/
```

Cada módulo segue a mesma sub-estrutura **obrigatória**:

```
modules/<nome>/
├── controller/    # @RestController — só HTTP, sem lógica
├── service/       # @Service — toda a lógica e @Transactional
├── repository/    # @Repository — interfaces JpaRepository
├── model/         # @Entity — entidades JPA (extends BaseEntity)
└── dto/           # records — input (CreateXxxRequest) e output (XxxDTO)
```

## Como correr

### Desktop (uso diário)
```powershell
mvn spring-boot:run "-Dspring-boot.run.main-class=com.phcpro.desktop.DesktopApplication"
```

O login e a seleção de empresa do desktop comunicam com a API HTTP. Por defeito,
o modo desktop usa o backend local em `http://localhost:8080`. Para apontar para
um backend remoto:

```powershell
$env:DESKTOP_API_BASE_URL="https://erp.exemplo.co.mz"
mvn spring-boot:run "-Dspring-boot.run.main-class=com.phcpro.desktop.DesktopApplication"
```

O token de autenticação fica apenas em memória durante a sessão do desktop.

### Backend isolado (sem janelas)
```powershell
mvn spring-boot:run
```

### Compilar / verificar
```powershell
mvn clean compile           # build completo
mvn test                    # testes
```

### Console H2
Com o backend a correr: `http://localhost:8080/h2-console`

## Documentação

| Ficheiro | Para quê |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md)     | Como **não misturar camadas** (Controller→Service→Repository), separação backend/desktop, princípios SOLID |
| [CONVENTIONS.md](CONVENTIONS.md)       | Convenções de código: naming, Lombok, DTOs, exceções, transações, mensagens em PT-PT/PT-BR |
| [.claude/skills/](.claude/skills/)     | Receitas accionáveis (novo módulo, novo endpoint, novo PDF, ícones, revisão SOLID, status da loja) |
| [tasks/current.md](tasks/current.md)   | Contexto operacional actual — o que está em curso, decisões recentes, próximos passos |

## Princípios não-negociáveis

1. **SRP rígido** — Controller não chama Repository directamente; Service não devolve entidade JPA fora do módulo.
2. **DTOs em todas as fronteiras** — entrada (`@Valid CreateXxxRequest`) e saída (`XxxDTO`). Nunca expor `@Entity` na API.
3. **Erros de negócio = `BusinessRuleException`** — capturados centralmente, resposta JSON uniforme.
4. **`@Transactional` em escrita; `@Transactional(readOnly = true)` em leitura agregada.**
5. **Injecção por construtor**, nunca `@Autowired` em campo.
6. **Ícones Swing via `UIHelper.icon("fas-…", size)`** — nunca emojis em labels de botões.

Detalhes em [ARCHITECTURE.md](ARCHITECTURE.md) e [CONVENTIONS.md](CONVENTIONS.md).
