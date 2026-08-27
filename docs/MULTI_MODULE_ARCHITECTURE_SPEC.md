# SPEC — Separação física Backend, Desktop e Contratos

Estado: **obrigatório**  
Decisão: 2026-08-27

## 1. Objectivo

O Multicore é um monorepo Maven com três artefactos independentes:

```text
multicore-parent
├── contracts   biblioteca Java sem Spring/JPA/Swing
├── backend     Spring Boot headless, API e PostgreSQL
└── desktop     cliente Swing instalado, exclusivamente HTTP
```

Esta separação é uma fronteira de segurança e distribuição, não apenas organização de pastas.

## 2. Estrutura física obrigatória

```text
contracts/src/main/java/   DTOs, requests, responses, enums de contrato e value objects puros
backend/src/main/java/     controllers, services, repositories, entities e entrypoint do servidor
backend/src/main/resources/db/migration/   migrations Flyway
desktop/src/main/java/     Swing, clientes HTTP, sessão e integração com periféricos locais
deployment/                contentores, hospedagem e configuração de produção
```

O `pom.xml` raiz tem `packaging=pom` e agrega exactamente `contracts`, `backend` e `desktop`.

## 3. Dependências permitidas

```text
desktop ─────→ contracts ←───── backend
```

- `desktop` nunca depende de `backend`.
- `backend` nunca depende de `desktop`.
- `contracts` não conhece Spring, Swing, JPA, JDBC, Flyway, repositórios ou serviços.
- DTOs HTTP são records e vivem em `contracts`.
- Tipos internos JPA nunca atravessam a API.

## 4. Conteúdo dos artefactos

### Backend

- Entry point `mz.multicore.erp.MulticoreApplication`.
- Spring Web, Security, Validation, JPA, Flyway e PostgreSQL.
- Não contém `java.desktop`, `javax.swing`, `java.awt` ou classes `gui`/`desktop`.
- Produz `multicore-backend.jar` executável e imagem Docker headless.

### Desktop

- Entry point `mz.multicore.erp.desktop.DesktopApplication`.
- Swing, impressão local, balança, scanner e clientes HTTP.
- Não contém JPA, JDBC, Flyway, repositories, entities ou credenciais da base de dados.
- Produz `multicore-desktop.jar`; distribuição Windows é gerada com `jpackage` e runtime Java 21.

### Contracts

- Produz `multicore-contracts.jar`.
- Contém apenas estruturas de transporte imutáveis e utilitários puros necessários ao contrato.

## 5. Hospedagem

Somente o backend é hospedado. O desktop é instalado nos computadores Windows e recebe apenas:

```properties
api.base-url=https://api.multicore.co.mz
```

Topologia de produção:

```text
Desktop Windows → HTTPS/JWT → Backend → PostgreSQL privado
                                  └──→ object storage / backups
```

PostgreSQL nunca é público. O backend recebe segredos por variáveis de ambiente. Documentos não
ficam no instalador nem em pastas partilhadas do desktop.

## 6. Compatibilidade e versões

- API versionada e compatível pelo menos com a versão desktop anterior.
- Backend publica versão actual e versão mínima do cliente.
- O desktop recusa apenas versões abaixo da mínima, com mensagem de actualização accionável.
- Migrations executam antes de o novo backend receber tráfego.

## 7. Processo de entrega

1. CI executa harness, compilação e testes dos três módulos.
2. Publica imagem/`jar` do backend no ambiente de staging.
3. Executa health check e smoke tests.
4. Promove o backend para produção.
5. Gera instalador Windows assinado e publica-o no canal de actualizações.
6. Clientes actualizam de forma gradual dentro da janela de compatibilidade.

## 8. Definition of Done

- `mvn clean verify` no raiz passa.
- Os três módulos compilam isoladamente na ordem do reactor.
- O harness `MultiModuleArchitectureHarnessTest` passa.
- O desktop arranca sem `DataSource` e comunica com um backend real por HTTP.
- O backend arranca em modo headless e passa `/actuator/health`.
- Docker e instalador usam apenas os respectivos artefactos.
- `AGENTS.md`, `ARCHITECTURE.md`, README e documentação de deploy reflectem esta decisão.

