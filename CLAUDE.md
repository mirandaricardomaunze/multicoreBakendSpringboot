# Contexto da IA — Multicore ERP

Este ficheiro é lido automaticamente pelo Claude Code no início de cada sessão. Mantê-lo **curto** — para detalhes, apontar para os documentos canónicos.

---

## Spec-driven harness

A IA tem **uma fonte de verdade por tópico**. Antes de responder ou escrever código, consultar:

| Documento                                              | Para quê                                                                 |
|---------------------------------------------------------|--------------------------------------------------------------------------|
| [README.md](README.md)                                  | Stack, entrypoints, como correr, módulos                                  |
| [ARCHITECTURE.md](ARCHITECTURE.md)                      | Camadas (Controller→Service→Repository), SOLID, separação backend/desktop |
| [CONVENTIONS.md](CONVENTIONS.md)                        | Naming, Lombok, DTOs, exceções, transacções, UI Swing, idioma             |
| [MOVIMENTOS_COMERCIAIS.md](MOVIMENTOS_COMERCIAIS.md)    | Documentos de venda (POS, fatura, NC, ND), ledgers stock/caixa/tesouraria, lacunas |
| [tasks/current.md](tasks/current.md)                    | O que está em curso **agora**, decisões recentes, próximos passos         |
| [.claude/skills/](.claude/skills/)                      | Receitas accionáveis para tarefas frequentes (ver lista abaixo)           |

Se uma instrução do utilizador colide com estes ficheiros: **perguntar antes de divergir**.

---

## Skills disponíveis (`.claude/skills/`)

Quando o pedido encaixa numa skill, invocar a skill em vez de inventar:

| Skill                  | Quando usar                                                          |
|------------------------|----------------------------------------------------------------------|
| `phc-new-module`       | "cria um módulo de X", "novo domínio Y" — gera pasta completa        |
| `phc-new-endpoint`     | Adicionar endpoint REST a um módulo existente                         |
| `phc-pdf-document`     | Novo documento imprimível (factura, recibo, ordem, relatório)        |
| `phc-icons`            | Alterar/adicionar ícones em botões/tabs (`UIHelper.icon`)            |
| `phc-solid-review`     | Rever classe ou diff contra SOLID/SRP/DRY do projecto                |
| `phc-store-status`     | Relatório operacional rápido (vendas hoje, stock baixo, …)           |

---

## Regras de comportamento da IA

1. **Compilar é a verdade.** Diagnostics do IDE `cannot find symbol: getX()` são ruído de Lombok. Confiar em `mvn clean compile`.
2. **Mensagens ao utilizador em PT-MZ.** Identificadores Java em inglês. Ver [CONVENTIONS.md §1](CONVENTIONS.md#1-idioma).
3. **Não misturar camadas.** Controller não chama Repository, Service não devolve `@Entity` para fora. Ver [ARCHITECTURE.md §2](ARCHITECTURE.md#2-camadas-obrigatórias-por-módulo).
4. **Sempre DTO na fronteira** do Controller. Sempre `BusinessRuleException` para regras de negócio.
5. **Injecção por construtor**, campos `final`. Nunca `@Autowired` em campo.
6. **UI Swing**: `UIHelper.icon(...)` para ícones, **nunca emojis** em labels de botões.
7. **Antes de uma alteração arquitectural grande**, ler [tasks/current.md](tasks/current.md) e perguntar se está alinhado com a iteração actual.
8. **Updates a `tasks/current.md`**: actualizar quando uma fase fecha; manter ≤1 página. Histórico vive no `git log`.

---

## Stack curta

Java 21 · Spring Boot 3.2.5 · JPA/Hibernate · H2 (dev) / PostgreSQL (alvo) · Swing · Lombok · OpenPDF · Ikonli FontAwesome 5 · Maven.

Dois entrypoints num só codebase:
- Backend: `com.phcpro.MulticoreApplication`
- Desktop:  `com.phcpro.desktop.DesktopApplication` (perfil `desktop`)

---

## Estrutura mínima de um módulo

```
modules/<nome>/
├── controller/   (@RestController, só HTTP)
├── service/      (@Service, lógica + @Transactional)
├── repository/   (@Repository, JpaRepository)
├── model/        (@Entity extends BaseEntity)
└── dto/          (records: CreateXxxRequest / XxxDTO)
```

Quem não respeitar esta divisão tem bug. Ver [ARCHITECTURE.md §2](ARCHITECTURE.md).
