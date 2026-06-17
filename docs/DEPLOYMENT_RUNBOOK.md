# Deployment Runbook - Multicore ERP

Este documento resume comandos e verificacoes operacionais para desenvolvimento, build e diagnostico.

## Requisitos

- Java 21.
- Maven.
- Acesso a PostgreSQL para ambiente de producao alvo.
- H2 para desenvolvimento local.

## Correr backend

```powershell
mvn spring-boot:run
```

Entrypoint:

```text
com.phcpro.MulticoreApplication
```

## Correr desktop

```powershell
mvn spring-boot:run "-Dspring-boot.run.main-class=com.phcpro.desktop.DesktopApplication"
```

Para apontar para backend remoto:

```powershell
$env:DESKTOP_API_BASE_URL="https://erp.exemplo.co.mz"
mvn spring-boot:run "-Dspring-boot.run.main-class=com.phcpro.desktop.DesktopApplication"
```

## Build e testes

```powershell
mvn clean compile
mvn test
```

Nota: erros `cannot find symbol: getX()` no IDE podem ser ruido de Lombok. O Maven e a verdade.

## Perfis

- `desktop`: cliente Swing.
- `prod`: configuracao alvo de producao quando aplicavel.
- default: desenvolvimento local.

## Base de dados

- Migrations vivem em `src/main/resources/db/migration`.
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

## Antes de deploy

- [ ] `mvn clean compile` passa.
- [ ] `mvn test` passa ou falhas estao explicadas.
- [ ] Migrations novas foram revistas.
- [ ] Configuracoes sensiveis nao estao hardcoded.
- [ ] Logs nao expoem tokens/passwords.
- [ ] Fluxos POS, venda, stock e login foram testados.
