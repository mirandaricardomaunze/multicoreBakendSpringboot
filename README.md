# Multicore — ERP profissional em Java/Spring Boot + Swing

Multicore é um ERP modular (vendas, compras, stock, POS, fiscal, RH, CRM, financeira, aprovações, auditoria) com um reactor Maven de três módulos fisicamente separados:

- **`contracts`** — DTOs e tipos partilhados, sem Spring/JPA/Swing.
- **`backend`** — API Spring Boot, regras, JPA, Flyway e PostgreSQL; é o único componente hospedado.
- **`desktop`** — aplicação Swing instalada no Windows; usa HTTPS e nunca conhece a base de dados.

Esta separação é protegida pelo SPEC e pelo harness em [docs/MULTI_MODULE_ARCHITECTURE_SPEC.md](docs/MULTI_MODULE_ARCHITECTURE_SPEC.md).

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

```text
contracts/src/main/java/   # contratos HTTP puros
backend/src/main/java/     # controllers, services, repositories e entidades
backend/src/main/resources/# configuração e migrações Flyway
desktop/src/main/java/     # Swing, clientes HTTP e impressão local
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

O desktop tem POM e entrypoint próprios:

```powershell
mvn -pl desktop -am spring-boot:run
```

> O desktop não contém driver de BD nem credenciais. Configure apenas `DESKTOP_API_BASE_URL` com o endereço HTTPS do backend.

O login e a seleção de empresa do desktop comunicam com a API HTTP. Por defeito,
o modo desktop usa o backend local em `http://localhost:8080`. Para apontar para
um backend remoto:

```powershell
$env:DESKTOP_API_BASE_URL="https://erp.exemplo.co.mz"
mvn -pl desktop -am spring-boot:run
```

O token de autenticação fica apenas em memória durante a sessão do desktop.

### Backend isolado (sem janelas)
```powershell
mvn -pl backend -am spring-boot:run
```

### Compilar / verificar
```powershell
mvn clean compile           # build completo
mvn test                    # testes
```

### Console H2
Com o backend a correr: `http://localhost:8080/h2-console`

### CI e gate de merge
A CI ([.github/workflows/build.yml](.github/workflows/build.yml)) corre em **todas as branches e PRs**:
compila e corre a **suite completa** (unit + integração Spring, em H2; UI Swing via `xvfb`). Qualquer
teste que parta **reprova o build**.

Para a CI **travar o merge** (e não só sinalizar), é preciso ligar a **proteção de branch** em `main`
no GitHub — é uma definição do repositório, não do workflow. Em **Settings → Branches → Add rule
(ou ruleset)** para `main`:
- ✅ *Require a pull request before merging*
- ✅ *Require status checks to pass before merging* → escolher o check **`build`**
- ✅ *Require branches to be up to date before merging*
- (opcional) *Do not allow bypassing the above settings*

Sem isto, um build vermelho **não** impede o merge — foi assim que o bug de numeração multi-empresa
chegou a `main`. Com isto ligado, o fluxo passa a ser por **Pull Request** (deixa de se fazer push
directo para `main`).

## Deploy em produção (VPS)

O backend (`mz.multicore.erp.MulticoreApplication`, headless) é hospedável à parte com **Docker + PostgreSQL
privado + Caddy (HTTPS automático)**:

```bash
cp .env.example .env      # editar: DOMAIN, DB_PASSWORD, PG_MAJOR
docker compose up -d --build
./scripts/deploy-smoke.sh https://o-teu-dominio   # verificação pós-deploy
```

Guião completo, arquitetura e **checklist de hardening**: [docs/DEPLOY_VPS_SPEC.md](docs/DEPLOY_VPS_SPEC.md).
Segurança: `/api/**` exige token válido (Spring Security + `SecurityInterceptor`); só `/actuator/health`
e o login são públicos — ver [docs/SEGURANCA_HARDENING_SPEC.md](docs/SEGURANCA_HARDENING_SPEC.md).

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
