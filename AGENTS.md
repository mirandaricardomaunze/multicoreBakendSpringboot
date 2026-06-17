# AI Instructions - Multicore ERP

Este ficheiro e a porta de entrada para qualquer agente de codigo neste projecto. Antes de alterar codigo, ler os documentos canonicos abaixo e seguir a ordem indicada.

## Ordem de leitura

1. [README.md](README.md) - stack, entrypoints, comandos e estrutura base.
2. [ARCHITECTURE.md](ARCHITECTURE.md) - camadas obrigatorias, SOLID e separacao backend/desktop.
3. [CONVENTIONS.md](CONVENTIONS.md) - naming, Lombok, DTOs, transaccoes, UI Swing e idioma.
4. [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md) - ownership dos dominios e dependencias permitidas.
5. [docs/BUSINESS_FLOWS.md](docs/BUSINESS_FLOWS.md) - fluxos de negocio que nao podem ser quebrados.
6. [docs/DATABASE.md](docs/DATABASE.md) - regras de persistencia, tenant, migrations e integridade.
7. [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) - contratos REST, DTOs, erros e versionamento.
8. [docs/UI_DESIGN_SYSTEM.md](docs/UI_DESIGN_SYSTEM.md) - padroes de UI Swing.
9. [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) - expectativas de testes.
10. [tasks/current.md](tasks/current.md) - contexto operacional actual.

Se uma instrucao do utilizador colidir com estes documentos, perguntar antes de divergir.

## Regras nao negociaveis

- Controller nunca chama Repository directamente.
- Controller nunca recebe ou devolve Entity JPA.
- Service contem regras de negocio, validacoes semanticas e transaccoes.
- Repository contem apenas persistencia e queries.
- DTOs records em todas as fronteiras HTTP.
- Erros de negocio usam `BusinessRuleException` com mensagem clara para o utilizador.
- Injecao por construtor, campos `final`; nunca `@Autowired` em campo.
- Mensagens visiveis ao utilizador em portugues de Mocambique.
- Identificadores Java, pacotes, tabelas e colunas em ingles.
- UI Swing usa `UIHelper`, `ModernButton`, `ModernPanel` e `UIHelper.icon(...)`; nao usar emojis em botoes.
- Dinheiro e quantidades usam `BigDecimal`, nunca `double` ou `float`.
- Antes de terminar uma alteracao de codigo, correr `mvn clean compile` quando possivel.

## Como trabalhar

1. Identificar o dominio dono da regra antes de editar.
2. Ler classes vizinhas no mesmo modulo e seguir o padrao existente.
3. Fazer a menor alteracao que preserve arquitectura.
4. Adicionar ou ajustar testes quando a regra de negocio muda.
5. Actualizar documentacao apenas quando uma decisao, fluxo ou contrato muda.

## Quando parar e perguntar

- A mudanca exige quebrar uma regra em [ARCHITECTURE.md](ARCHITECTURE.md).
- Ha duvida sobre regra fiscal, contabilistica, stock, pagamento, permissao ou auditoria.
- Um dado parece pertencer a mais de um modulo e o ownership nao esta claro.
- A solucao exige migracao de base de dados destrutiva.
- A alteracao muda comportamento de faturacao, POS, stock, caixa ou salarios.

## Definicao de pronto

- Codigo compila.
- Camadas continuam separadas.
- DTOs e mensagens seguem as convencoes.
- Regras criticas tem teste ou justificacao clara para nao ter.
- UI nao bloqueia o EDT em operacoes longas.
- `tasks/current.md` foi actualizado se uma fase de trabalho fechou.
