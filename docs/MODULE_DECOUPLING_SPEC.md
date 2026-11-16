# Desacoplamento entre Módulos — Lookup no Módulo Dono

Status: **aprovada** (responsável, 2026-11-10)
Harness: [MODULE_DECOUPLING_HARNESS.md](MODULE_DECOUPLING_HARNESS.md) · regra de base: CA-07 em
[CLEAN_ARCHITECTURE_RULES_SPEC.md](CLEAN_ARCHITECTURE_RULES_SPEC.md)
Execução: goals `GOAL-001` a `GOAL-005` no Ralph Loop.

## 1. Problema

Em 2026-11-10 havia **395 imports** de um Repository de outro módulo, em 182 ficheiros e sobre 46
repositórios. Um módulo que lê directamente as tabelas de outro fica preso ao schema e às regras
internas desse módulo: qualquer mudança no dono parte consumidores que ele nem conhece.

| Alvo | Ligações | Goal |
|---|---|---|
| `company.repository.*` (106 em `CompanyRepository`, 64 só `findById`) | 106 | GOAL-001 |
| `ProductRepository`, `ProductCategoryRepository` | 50 | GOAL-002 |
| Resto de `comercial.repository.*` (facturas, clientes, encomendas, notas, guias) | 79 | GOAL-003 |
| `inventory.repository.*` (armazéns, stock, quebras, transferências, lotes) | 65 | GOAL-004 |
| Os restantes (users, hr, purchases, pos, financeira, fleet, fiscal, ...) | 95 | GOAL-005 |

## 2. Decisão

Cada módulo dono expõe a sua **porta de leitura** e os outros só falam com ele por serviços:

- **Leitura:** `<Agregado>Lookup` no pacote `service` do módulo dono (ex.: `company.service.CompanyLookup`,
  `comercial.service.ProductLookup`). É um `@Service` com injecção por construtor e
  `@Transactional(readOnly = true)`, e é o único sítio do módulo que outro módulo usa para ler.
- **Escrita:** nunca pelo Repository alheio. O consumidor chama um método de comando do Service dono
  (existente ou novo). Movimentos de stock passam **sempre** pelo serviço de movimentos de stock (ledger)
  do inventário, nunca por `stockRepository.save` vindo de fora.
- **Entidades:** o Lookup pode devolver a entidade dentro do backend (as relações JPA `@ManyToOne` entre
  módulos precisam dela), mas a entidade nunca sai para um Controller (CA-02) nem para o desktop.
- **Não encontrado:** `BusinessRuleException` com mensagem PT-MZ normalizada no Lookup:
  `require(id)` → "Empresa não encontrada." / "Produto não encontrado." etc. As variantes actuais
  ("Empresa ativa não encontrada.", com grafia do Brasil) passam a "Empresa activa não encontrada.".
  Uma mensagem específica só se mantém quando um teste existente a verifica.
- **Granularidade:** um Lookup por agregado (SRP). Métodos pouco usados não justificam um Lookup próprio:
  ficam no Lookup do agregado mais próximo do mesmo módulo.
- **Sem excepções de plataforma:** `architecture/DataLoader*`, `backup` e `printing` também passam a
  usar Lookups/Services. Se uma operação (ex.: restauro de backup) precisar de acesso em massa que um
  Lookup não cubra, a iteração pára com `NEEDS_HUMAN`.

## 3. Comportamento

Refactorização pura: nenhum endpoint, resultado, código HTTP, transacção ou regra de negócio muda.
A suite completa tem de continuar verde no fecho de cada goal.

## 4. Fecho de cada onda

O `ModuleDecouplingHarnessTest` lê os goals e impõe em dois níveis: cada critério de migração assim que
é marcado `[x]` (ex.: `MD-C02` exige zero ligações a `company.repository.*` a partir de `hr`), e a onda
inteira quando o goal passa a `DONE`. Os critérios de criação de Lookups são provados pelos seus testes. As linhas resolvidas são apagadas da baseline do CA-07 (o diff-guard aceita apagar linhas da
baseline, mas nunca acrescentá-las). Progresso em `backend/target/module-decoupling-report.md`.

## 5. Fora de âmbito

- Repositórios usados **dentro** do próprio módulo (não são acoplamento).
- Trocar relações JPA entre módulos por IDs ou dividir a base de dados (opção "só DTOs", recusada).
- Integração das branches: o loop trabalha em `ralph/goals`; o merge é revisto pelo responsável.
