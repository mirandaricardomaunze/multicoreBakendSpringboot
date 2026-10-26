# SPEC — Cobertura Integral da Política de Permissões HTTP

## Problema

Testes por exemplos confirmavam as permissões principais, mas não provavam que todos os mappings
Spring tinham uma decisão na `ApiRolePolicy`. Um novo controller podia depender do fallback genérico
de gestor e escapar à revisão da matriz.

## Decisão

- `ApiRolePolicy.isExplicitlyClassified(method, uri)` identifica se a rota pertence a uma área
  conscientemente registada.
- Rotas desconhecidas deixam de herdar acesso genérico de `MANAGER` e são negadas por omissão.
- `ApiRolePolicyEndpointCoverageHarnessTest` obtém todos os mappings dos `@RestController` através do
  `RequestMappingHandlerMapping` e falha quando qualquer rota `/api/**` não está classificada.
- A classificação não substitui as regras finas por método, sufixo, ownership ou guardas do Service.

## Fluxo para novos endpoints

1. Criar o endpoint e executar o harness.
2. Classificar conscientemente a área na `ApiRolePolicy`.
3. Definir os perfis permitidos e acrescentar testes positivos e negativos quando a operação for sensível.
4. Actualizar a matriz canónica se a autorização funcional tiver mudado.

## Critérios de aceitação

1. Todos os endpoints `/api/**` actuais possuem classificação explícita.
2. Uma rota fictícia desconhecida é negada a `MANAGER`.
3. O harness apresenta método e path de cada mapping esquecido.
4. `ApiRolePolicyTest`, `SecurityApiIntegrationTest` e `MultiModuleArchitectureHarnessTest` passam.
