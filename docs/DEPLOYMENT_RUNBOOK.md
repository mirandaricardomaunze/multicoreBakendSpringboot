# Deployment Runbook - Multicore ERP

Este documento resume comandos e verificacoes operacionais para desenvolvimento, build e diagnostico.

## Modulos

O repositorio e um monorepo Maven com tres artefactos — ver
[MULTI_MODULE_ARCHITECTURE_SPEC.md](MULTI_MODULE_ARCHITECTURE_SPEC.md).

```text
multicore-parent          packaging=pom, so agrega
├── contracts             DTOs e enums de contrato; sem Spring, JPA ou Swing
├── backend               API, servicos, entidades, Flyway
└── desktop               Swing e clientes HTTP; sem JPA nem JDBC
```

O `pom.xml` da raiz nao produz artefacto. Compilar e testar continuam a correr na raiz (o reactor
trata dos tres); **arrancar** uma aplicacao e que mudou — ver abaixo.

Antes de arrancar backend ou desktop pela primeira vez, e sempre que o `contracts` mudar:

```powershell
mvn install -DskipTests
```

Poe o `multicore-contracts` no repositorio local. Sem isso, `mvn -pl backend spring-boot:run` falha
com *Could not resolve dependencies ... multicore-contracts:jar:1.0.0*.

## Requisitos

- Java 21.
- Maven.
- Acesso a PostgreSQL para ambiente de producao alvo.
- H2 para desenvolvimento local.

## Correr backend

```powershell
mvn -pl backend spring-boot:run
```

Entrypoint:

```text
mz.multicore.erp.MulticoreApplication
```

> **Sem `-am`, e de proposito.** Com `-am` o pai entra no reactor e o `spring-boot:run` tenta correr
> tambem contra ele, falhando com *Unable to find a suitable main class* — o pai e `packaging=pom`
> e nao tem nenhuma. E por isso que o `mvn install` acima e necessario: sem `-am`, o `contracts`
> tem de vir do repositorio local.

## Correr desktop

```powershell
mvn -pl desktop spring-boot:run
```

O `mainClass` esta fixado em `desktop/pom.xml`, pelo que ja nao e preciso o
`-Dspring-boot.run.main-class` — que, alias, nunca sobrepunha um valor literal da configuracao.

Para apontar para backend remoto:

```powershell
$env:DESKTOP_API_BASE_URL="https://erp.exemplo.co.mz"
mvn -pl desktop spring-boot:run
```

O desktop arranca **sem base de dados**: nem `DataSource`, nem Hikari, nem JPA, nem Flyway. Se
algum deles aparecer no arranque, alguem voltou a por JPA no modulo errado — e o
`MultiModuleArchitectureHarnessTest` deve apanha-lo antes.

## Build e testes

```powershell
mvn clean compile      # os tres modulos, na ordem do reactor
mvn test
mvn -pl backend test   # so um modulo
```

> **`clean` falha enquanto houver um backend a correr a partir do `target/`.** Um processo
> lancado com `java -jar backend/target/multicore-backend-1.0.0.jar`, ou um `spring-boot:run`
> esquecido, segura o proprio ficheiro que o `maven-clean-plugin` tenta apagar. O erro nomeia
> sempre o ficheiro preso — nao e um defeito do build:
>
> ```
> Failed to clean project: Failed to delete ...ackend	arget\multicore-backend-1.0.0.jar
> ```
>
> Descobrir quem o segura e parar so esse processo:
>
> ```powershell
> Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
>     Select-Object ProcessId, CreationDate, CommandLine
> ```
>
> Instancias antigas podem estar a correr codigo anterior a uma migracao ou a separacao em
> modulos, o que e por si so razao para as reiniciar. Sem `clean`, o `verify` passa na mesma.

Nota: erros `cannot find symbol: getX()` no IDE podem ser ruido de Lombok. O Maven e a verdade.

## Perfis

- `desktop`: cliente Swing.
- `prod`: configuracao alvo de producao quando aplicavel.
- default: desenvolvimento local.

## Base de dados

- Migrations vivem em `backend/src/main/resources/db/migration`.
- Consultar [DATABASE.md](DATABASE.md) antes de mudar schema.
- Nunca editar migration ja aplicada em ambiente partilhado.

## Diagnostico rapido

| Sintoma | Verificar |
|---------|-----------|
| Desktop nao autentica | `DESKTOP_API_BASE_URL`, backend activo, token/sessao |
| Dados de outra empresa aparecem | tenant filter, `CurrentUserContext`, acesso do utilizador |
| Erro Lombok no IDE | annotation processing/plugin Lombok, confirmar com Maven |
| Factura/POS falha no stock | lote, armazem, quantidade, FEFO, transaccao |
| PDF falha | Service de `printing`, permissao de ficheiro, dados obrigatorios |
| Migration falha | ordem V*, SQL compativel H2/PostgreSQL, constraints existentes |
| `mvn` na raiz nao faz nada | a raiz e `packaging=pom`; compilar/testar corre na raiz, arrancar usa `-pl <modulo>` |
| `Unable to find a suitable main class` | usou `-am` com `spring-boot:run`; tirar o `-am` |
| `Could not resolve ... multicore-contracts` | falta `mvn install -DskipTests` uma vez |
| Desktop nao compila por falta de um DTO | o DTO pertence a `contracts`, nao ao `backend` |

## Antes de deploy

- [ ] `mvn clean verify` na raiz passa (os tres modulos).
- [ ] `MultiModuleArchitectureHarnessTest` passa — e o que impede o desktop de voltar a importar JPA.
- [ ] `mvn test` passa ou falhas estao explicadas.
- [ ] Migrations novas foram revistas.
- [ ] Configuracoes sensiveis nao estao hardcoded.
- [ ] Logs nao expoem tokens/passwords.
- [ ] Fluxos POS, venda, stock e login foram testados.
