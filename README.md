# Multicore — ERP profissional em Java/Spring Boot + Swing

Multicore é um ERP modular (vendas, compras, stock, POS, fiscal, RH, CRM, financeira, aprovações, auditoria) com **um único codebase** que arranca em dois modos:

- **Backend HTTP/API** — `mz.multicore.erp.MulticoreApplication` (Spring Boot puro, sem janelas).
- **Cliente desktop Swing** — `mz.multicore.erp.desktop.DesktopApplication` (arranca Spring Boot com perfil `desktop` e abre a janela de login).

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
src/main/java/mz/multicore/erp/
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

O `pom.xml` fixa `<mainClass>mz.multicore.erp.MulticoreApplication</mainClass>`, pelo que
`mvn spring-boot:run` arranca **sempre o backend puro** (sem janela) — o
`-Dspring-boot.run.main-class` da linha de comando **não** sobrepõe um valor literal
da configuração. Para arrancar o cliente desktop, correr o `DesktopApplication` directamente:

```powershell
mvn -q compile
mvn -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
$cp = "target/classes;" + (Get-Content target/cp.txt -Raw)
java -cp $cp mz.multicore.erp.desktop.DesktopApplication
```

> 🗄️ **Base de dados:** o perfil `desktop` usa **PostgreSQL local** (`jdbc:postgresql://localhost:5432/multicore`),
> não H2 — os dados persistem. Requer um servidor PostgreSQL a correr, a BD `multicore` + role `multicore`,
> e a variável de ambiente **`DB_PASSWORD`** com a password da role. Flyway é dono do schema (`V1..V17`),
> Hibernate apenas valida. Detalhes em [docs/BD_POSTGRES_DESKTOP_SPEC.md](docs/BD_POSTGRES_DESKTOP_SPEC.md).
> Para criar a BD/role de raiz:
> ```sql
> CREATE ROLE multicore LOGIN PASSWORD 'a_tua_password';
> CREATE DATABASE multicore OWNER multicore;
> ```

O login e a seleção de empresa do desktop comunicam com a API HTTP. Por defeito,
o modo desktop usa o backend local em `http://localhost:8080`. Para apontar para
um backend remoto:

```powershell
$env:DESKTOP_API_BASE_URL="https://erp.exemplo.co.mz"
mvn spring-boot:run "-Dspring-boot.run.main-class=mz.multicore.erp.desktop.DesktopApplication"
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
